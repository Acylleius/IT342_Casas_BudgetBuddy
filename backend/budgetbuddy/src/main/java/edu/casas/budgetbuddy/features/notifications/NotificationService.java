package edu.casas.budgetbuddy.features.notifications;

import edu.casas.budgetbuddy.shared.store.BudgetBuddyStore;
import edu.casas.budgetbuddy.shared.store.BudgetBuddyStore.EmailNotificationRecord;
import edu.casas.budgetbuddy.shared.store.BudgetBuddyStore.UserRecord;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {
    private static final String SUBJECT = "BudgetBuddy Notification";

    private final BudgetBuddyStore store;
    private final Optional<JavaMailSender> mailSender;
    private final String smtpHost;

    public NotificationService(BudgetBuddyStore store,
                               ObjectProvider<JavaMailSender> mailSender,
                               @Value("${spring.mail.host:}") String smtpHost) {
        this.store = store;
        this.mailSender = Optional.ofNullable(mailSender.getIfAvailable());
        this.smtpHost = smtpHost == null ? "" : smtpHost;
    }

    public void notifyGroupMembers(Long actorId, Long groupId, String message) {
        List<Long> recipientIds = store.members.stream()
                .filter(member -> member.groupId().equals(groupId) && !member.deleted()
                        && !member.userId().equals(actorId))
                .map(BudgetBuddyStore.GroupMemberRecord::userId)
                .toList();
        recipientIds.forEach(userId -> findUser(userId).ifPresent(user -> send(user, SUBJECT, message)));
    }

    public synchronized void send(UserRecord recipient, String subject, String message) {
        boolean sent = false;
        String status = "queued";
        if (mailSender.isPresent() && !smtpHost.isBlank()) {
            try {
                SimpleMailMessage mail = new SimpleMailMessage();
                mail.setTo(recipient.email());
                mail.setSubject(subject);
                mail.setText(message);
                mailSender.get().send(mail);
                sent = true;
                status = "sent";
            } catch (RuntimeException ex) {
                status = "failed: " + ex.getMessage();
            }
        } else {
            status = "smtp_not_configured";
        }
        store.emailNotifications.add(new EmailNotificationRecord(store.notificationIds.getAndIncrement(),
                recipient.id(), recipient.email(), subject, message, sent, status, LocalDateTime.now()));
    }

    private Optional<UserRecord> findUser(Long userId) {
        return store.users.stream().filter(user -> user.id().equals(userId)).findFirst();
    }
}
