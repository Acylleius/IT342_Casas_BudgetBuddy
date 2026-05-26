package edu.casas.budgetbuddy.features.auth;

import edu.casas.budgetbuddy.features.auth.AuthDtos.AuthData;
import edu.casas.budgetbuddy.features.auth.AuthDtos.AuthUser;
import edu.casas.budgetbuddy.features.activity.ActivityService;
import edu.casas.budgetbuddy.features.inbox.InboxService;
import edu.casas.budgetbuddy.shared.store.BudgetBuddyStore;
import edu.casas.budgetbuddy.shared.store.BudgetBuddyStore.UserRecord;
import edu.casas.budgetbuddy.shared.persistence.DatabasePersistenceService;
import edu.casas.budgetbuddy.shared.utils.DomainException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    private final BudgetBuddyStore store;
    private final ActivityService activityService;
    private final InboxService inboxService;
    private final DatabasePersistenceService databasePersistenceService;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final Map<String, Long> tokens = new ConcurrentHashMap<>();
    private final Map<String, Long> refreshTokens = new ConcurrentHashMap<>();
    private final String jwtSecret;
    private final long accessTokenSeconds;
    private final long refreshTokenSeconds;

    public AuthService(BudgetBuddyStore store,
                       ActivityService activityService,
                       InboxService inboxService,
                       DatabasePersistenceService databasePersistenceService,
                       @Value("${budgetbuddy.jwt.secret}") String jwtSecret,
                       @Value("${budgetbuddy.jwt.access-token-minutes}") long accessTokenMinutes,
                       @Value("${budgetbuddy.jwt.refresh-token-days}") long refreshTokenDays) {
        this.store = store;
        this.activityService = activityService;
        this.inboxService = inboxService;
        this.databasePersistenceService = databasePersistenceService;
        this.jwtSecret = jwtSecret;
        this.accessTokenSeconds = accessTokenMinutes * 60;
        this.refreshTokenSeconds = refreshTokenDays * 24 * 60 * 60;
    }

    public synchronized AuthData register(String email, String password, String firstname, String lastname) {
        String normalizedEmail = normalizeEmail(email);
        if (findByEmail(normalizedEmail).isPresent()) {
            throw new DomainException(HttpStatus.CONFLICT, "Email already exists");
        }
        UserRecord user = new UserRecord(store.userIds.getAndIncrement(), normalizedEmail,
                passwordEncoder.encode(password), firstname, lastname, "USER", "local", null,
                LocalDateTime.now());
        store.users.add(user);
        databasePersistenceService.saveUser(user);
        activityService.log(user.id(), "REGISTER", "USER", user.id(),
                user.firstname() + " " + user.lastname() + " registered");
        inboxService.welcome(user.id(), user.firstname());
        return issueToken(user);
    }

    public AuthData login(String email, String password) {
        UserRecord user = findByEmail(normalizeEmail(email))
                .orElseThrow(() -> new DomainException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));
        if (!"local".equals(user.authProvider()) || user.passwordHash() == null
                || !passwordEncoder.matches(password, user.passwordHash())) {
            throw new DomainException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }
        activityService.log(user.id(), "LOGIN", "USER", user.id(), user.email() + " logged in");
        return issueToken(user);
    }

    public AuthData refresh(String refreshToken) {
        Long userId = refreshTokens.get(refreshToken);
        if (userId == null) {
            userId = verifyToken(refreshToken, "refresh")
                    .map(claims -> Long.valueOf(claims.get("sub")))
                    .orElseThrow(() -> new DomainException(HttpStatus.UNAUTHORIZED, "Invalid refresh token"));
        }
        Long finalUserId = userId;
        UserRecord user = store.users.stream()
                .filter(candidate -> candidate.id().equals(finalUserId))
                .findFirst()
                .orElseThrow(() -> new DomainException(HttpStatus.UNAUTHORIZED, "Invalid refresh token"));
        return issueToken(user);
    }

    public synchronized AuthData loginWithGoogle(String email, String firstname, String lastname, String googleId) {
        String normalizedEmail = normalizeEmail(email);
        UserRecord user = findByEmail(normalizedEmail).orElseGet(() -> {
            UserRecord created = new UserRecord(store.userIds.getAndIncrement(), normalizedEmail, null,
                    blankToDefault(firstname, "Google"), blankToDefault(lastname, "User"),
                    "USER", "google", googleId, LocalDateTime.now());
            store.users.add(created);
            databasePersistenceService.saveUser(created);
            activityService.log(created.id(), "REGISTER_GOOGLE", "USER", created.id(),
                    created.email() + " registered with Google");
            inboxService.welcome(created.id(), created.firstname());
            return created;
        });
        return issueToken(user);
    }

    public synchronized void changePassword(String authorization, String currentPassword, String newPassword) {
        UserRecord current = requireUser(authorization);
        if (!"local".equals(current.authProvider())) {
            throw new DomainException(HttpStatus.BAD_REQUEST, "Google users manage passwords with Google");
        }
        if (!passwordEncoder.matches(currentPassword, current.passwordHash())) {
            throw new DomainException(HttpStatus.BAD_REQUEST, "Current password is incorrect");
        }
        replaceUser(new UserRecord(current.id(), current.email(), passwordEncoder.encode(newPassword),
                current.firstname(), current.lastname(), current.role(), current.authProvider(),
                current.googleId(), current.createdAt()));
    }

    public UserRecord requireUser(String authorization) {
        String token = parseToken(authorization);
        if (token == null) {
            throw new DomainException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        Long userId = tokens.get(token);
        if (userId == null) {
            userId = verifyToken(token, "access")
                    .map(claims -> Long.valueOf(claims.get("sub")))
                    .orElseThrow(() -> new DomainException(HttpStatus.UNAUTHORIZED, "Authentication required"));
        }
        Long finalUserId = userId;
        return store.users.stream()
                .filter(user -> user.id().equals(finalUserId))
                .findFirst()
                .orElseThrow(() -> new DomainException(HttpStatus.UNAUTHORIZED, "Authentication required"));
    }

    public AuthUser toAuthUser(UserRecord user) {
        return new AuthUser(user.id(), user.email(), user.firstname(), user.lastname(),
                user.role(), user.authProvider());
    }

    public void logout(String authorization) {
        String token = parseToken(authorization);
        if (token != null) {
            tokens.remove(token);
        }
    }

    public void clearSessions() {
        tokens.clear();
        refreshTokens.clear();
    }

    public Optional<UserRecord> findByEmail(String email) {
        String normalizedEmail = normalizeEmail(email);
        return store.users.stream()
                .filter(user -> user.email().equalsIgnoreCase(normalizedEmail))
                .findFirst();
    }

    private AuthData issueToken(UserRecord user) {
        Instant accessExpiresAt = Instant.now().plusSeconds(accessTokenSeconds);
        String accessToken = signedToken(user, "access", accessExpiresAt);
        Instant refreshExpiresAt = Instant.now().plusSeconds(refreshTokenSeconds);
        String refreshToken = signedToken(user, "refresh", refreshExpiresAt);
        tokens.put(accessToken, user.id());
        refreshTokens.put(refreshToken, user.id());
        databasePersistenceService.saveRefreshToken(user.id(), refreshToken,
                LocalDateTime.ofInstant(refreshExpiresAt, ZoneId.systemDefault()));
        return new AuthData(accessToken, accessToken, refreshToken,
                LocalDateTime.ofInstant(accessExpiresAt, ZoneId.systemDefault()), toAuthUser(user));
    }

    private String parseToken(String authorization) {
        if (authorization == null || authorization.isBlank()) {
            return null;
        }
        return authorization.startsWith("Bearer ") ? authorization.substring(7) : authorization;
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }

    private String blankToDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private void replaceUser(UserRecord replacement) {
        for (int index = 0; index < store.users.size(); index++) {
            if (store.users.get(index).id().equals(replacement.id())) {
                store.users.set(index, replacement);
                return;
            }
        }
    }

    private String signedToken(UserRecord user, String type, Instant expiresAt) {
        String header = base64Url("{\"alg\":\"HS256\",\"typ\":\"JWT\"}");
        String payload = base64Url("""
                {"sub":"%d","email":"%s","type":"%s","exp":%d,"jti":"%s"}
                """.formatted(user.id(), user.email(), type, expiresAt.getEpochSecond(), UUID.randomUUID()));
        return header + "." + payload + "." + hmac(header + "." + payload);
    }

    private Optional<Map<String, String>> verifyToken(String token, String expectedType) {
        String[] parts = token.split("\\.");
        if (parts.length != 3 || !hmac(parts[0] + "." + parts[1]).equals(parts[2])) {
            return Optional.empty();
        }
        String payload = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
        Map<String, String> claims = parseFlatJson(payload);
        if (!expectedType.equals(claims.get("type"))) {
            return Optional.empty();
        }
        long expiresAt = Long.parseLong(claims.getOrDefault("exp", "0"));
        if (Instant.now().getEpochSecond() >= expiresAt) {
            return Optional.empty();
        }
        return Optional.of(claims);
    }

    private String hmac(String value) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(jwtSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to sign token", ex);
        }
    }

    private String base64Url(String value) {
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private Map<String, String> parseFlatJson(String json) {
        Map<String, String> values = new HashMap<>();
        String body = json.trim().replaceAll("^\\{|}$", "");
        if (body.isBlank()) {
            return values;
        }
        for (String part : body.split(",")) {
            String[] keyValue = part.split(":", 2);
            if (keyValue.length == 2) {
                String key = keyValue[0].trim().replace("\"", "");
                String value = keyValue[1].trim().replace("\"", "");
                values.put(key, value);
            }
        }
        return values;
    }
}
