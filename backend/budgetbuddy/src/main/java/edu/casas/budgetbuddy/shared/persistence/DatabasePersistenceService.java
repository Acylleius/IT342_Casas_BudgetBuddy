package edu.casas.budgetbuddy.shared.persistence;

import edu.casas.budgetbuddy.shared.store.BudgetBuddyStore;
import edu.casas.budgetbuddy.shared.store.BudgetBuddyStore.GroupActivityLogRecord;
import edu.casas.budgetbuddy.shared.store.BudgetBuddyStore.GroupInvitationRecord;
import edu.casas.budgetbuddy.shared.store.BudgetBuddyStore.GroupMemberRecord;
import edu.casas.budgetbuddy.shared.store.BudgetBuddyStore.GroupRecord;
import edu.casas.budgetbuddy.shared.store.BudgetBuddyStore.GroupTransactionRecord;
import edu.casas.budgetbuddy.shared.store.BudgetBuddyStore.InboxNotificationRecord;
import edu.casas.budgetbuddy.shared.store.BudgetBuddyStore.TransactionRecord;
import edu.casas.budgetbuddy.shared.store.BudgetBuddyStore.UserRecord;
import java.time.LocalDateTime;
import java.sql.Date;
import java.sql.Timestamp;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.stereotype.Service;

@Service
public class DatabasePersistenceService {
    private static final Logger log = LoggerFactory.getLogger(DatabasePersistenceService.class);

    private final BudgetBuddyStore store;
    private final JdbcTemplate jdbcTemplate;
    private final boolean enabled;
    private final boolean datasourceUrlConfigured;
    private final boolean datasourceUsernameConfigured;
    private final boolean datasourcePasswordConfigured;

    public DatabasePersistenceService(BudgetBuddyStore store,
                                      @Value("${spring.datasource.url:}") String datasourceUrl,
                                      @Value("${spring.datasource.username:}") String datasourceUsername,
                                      @Value("${spring.datasource.password:}") String datasourcePassword) {
        this.store = store;
        this.datasourceUrlConfigured = datasourceUrl != null && !datasourceUrl.isBlank();
        this.datasourceUsernameConfigured = datasourceUsername != null && !datasourceUsername.isBlank();
        this.datasourcePasswordConfigured = datasourcePassword != null && !datasourcePassword.isBlank();
        if (datasourceUrlConfigured && datasourceUsernameConfigured && datasourcePasswordConfigured) {
            DriverManagerDataSource dataSource = new DriverManagerDataSource();
            dataSource.setDriverClassName("org.postgresql.Driver");
            dataSource.setUrl(datasourceUrl);
            dataSource.setUsername(datasourceUsername);
            dataSource.setPassword(datasourcePassword);
            this.jdbcTemplate = new JdbcTemplate(dataSource);
        } else {
            this.jdbcTemplate = null;
        }
        this.enabled = this.jdbcTemplate != null;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void announceModeAndLoad() {
        if (!enabled) {
            log.warn("Supabase/PostgreSQL not configured. Using in-memory fallback store. "
                            + "jdbcTemplateAvailable={}, datasourceUrlConfigured={}, "
                            + "datasourceUsernameConfigured={}, datasourcePasswordConfigured={}",
                    jdbcTemplate != null, datasourceUrlConfigured, datasourceUsernameConfigured,
                    datasourcePasswordConfigured);
            return;
        }
        try {
            jdbcTemplate.queryForObject("select 1", Integer.class);
            log.info("Database persistence enabled.");
            ensureSchema();
            loadDatabaseState();
        } catch (RuntimeException ex) {
            log.warn("Supabase/PostgreSQL connection failed. Using in-memory fallback store. Cause: {}", ex.getMessage());
        }
    }

    public boolean enabled() {
        return enabled;
    }

    public void saveUser(UserRecord user) {
        execute("save user", """
                insert into users (id, email, password_hash, firstname, lastname, role, created_at)
                values (?, ?, ?, ?, ?, ?, ?)
                on conflict (id) do update set email = excluded.email,
                  password_hash = excluded.password_hash,
                  firstname = excluded.firstname, lastname = excluded.lastname, role = excluded.role
                """, user.id(), user.email(), user.passwordHash(), user.firstname(), user.lastname(), user.role(),
                Timestamp.valueOf(user.createdAt()));
    }

    public void saveRefreshToken(Long userId, String refreshToken, java.time.LocalDateTime expiresAt) {
        execute("save refresh token", """
                insert into refresh_tokens (user_id, token_hash, expires_at)
                values (?, ?, ?)
                """, userId, refreshToken, Timestamp.valueOf(expiresAt));
    }

    public void saveTransaction(TransactionRecord transaction) {
        execute("save transaction", """
                insert into transactions (id, user_id, type, amount, category, description, transaction_date)
                values (?, ?, ?, ?, ?, ?, ?)
                on conflict (id) do update set type = excluded.type, amount = excluded.amount,
                  category = excluded.category, description = excluded.description,
                  transaction_date = excluded.transaction_date
                """, transaction.id(), transaction.userId(), transaction.type(), transaction.amount(),
                transaction.category(), transaction.description(), Date.valueOf(transaction.transactionDate()));
    }

    public void saveGroup(GroupRecord group) {
        execute("save group", """
                insert into groups (id, name, description, created_by)
                values (?, ?, ?, ?)
                on conflict (id) do update set name = excluded.name, description = excluded.description
                """, group.id(), group.name(), group.description(), group.createdBy());
    }

    public void saveGroupMember(GroupMemberRecord member) {
        execute("save group member", """
                insert into group_members (group_id, user_id, role)
                values (?, ?, ?)
                on conflict (group_id, user_id) do update set role = excluded.role
                """, member.groupId(), member.userId(), member.role());
    }

    public void saveInvitation(GroupInvitationRecord invitation) {
        execute("save group invitation", """
                insert into group_invitations (id, group_id, invited_user_id, invited_by_user_id, status, created_at, responded_at)
                values (?, ?, ?, ?, ?, ?, ?)
                on conflict (id) do update set status = excluded.status, responded_at = excluded.responded_at
                """, invitation.id(), invitation.groupId(), invitation.invitedUserId(), invitation.invitedByUserId(),
                invitation.status(), Timestamp.valueOf(invitation.createdAt()),
                invitation.respondedAt() == null ? null : Timestamp.valueOf(invitation.respondedAt()));
    }

    public void saveInbox(InboxNotificationRecord notification) {
        execute("save inbox notification", """
                insert into inbox_notifications (id, recipient_user_id, group_id, invitation_id, type, title, message, is_read, created_at)
                values (?, ?, ?, ?, ?, ?, ?, ?, ?)
                on conflict (id) do update set is_read = excluded.is_read
                """, notification.id(), notification.recipientUserId(), notification.groupId(), notification.invitationId(),
                notification.type(), notification.title(), notification.message(), notification.read(),
                Timestamp.valueOf(notification.createdAt()));
    }

    public void saveGroupActivity(GroupActivityLogRecord activity) {
        execute("save group activity", """
                insert into group_activity_logs (id, group_id, actor_user_id, actor_username, action_type,
                  entity_type, entity_id, old_value, new_value, description, created_at)
                values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                on conflict (id) do nothing
                """, activity.id(), activity.groupId(), activity.actorUserId(), activity.actorUsername(),
                activity.actionType(), activity.entityType(), activity.entityId(), activity.oldValue(),
                activity.newValue(), activity.description(), Timestamp.valueOf(activity.createdAt()));
    }

    public void saveGroupTransaction(GroupTransactionRecord transaction) {
        execute("save group transaction", """
                insert into group_transactions (id, group_id, created_by_user_id, type, amount, category,
                  description, transaction_date, created_at, updated_at)
                values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                on conflict (id) do update set type = excluded.type, amount = excluded.amount,
                  category = excluded.category, description = excluded.description,
                  transaction_date = excluded.transaction_date, updated_at = excluded.updated_at
                """, transaction.id(), transaction.groupId(), transaction.createdByUserId(), transaction.type(),
                transaction.amount(), transaction.category(), transaction.description(),
                Date.valueOf(transaction.transactionDate()), Timestamp.valueOf(transaction.createdAt()),
                Timestamp.valueOf(transaction.updatedAt()));
    }

    private void loadDatabaseState() {
        loadUsers();
        loadTransactions();
        loadGroups();
        loadGroupMembers();
        loadInvitations();
        loadInbox();
        loadGroupTransactions();
        loadGroupActivity();
    }

    private void ensureSchema() {
        String[] statements = {
                """
                create table if not exists users (
                  id bigserial primary key,
                  email text unique not null,
                  password_hash text,
                  firstname text not null default '',
                  lastname text not null default '',
                  role text not null default 'USER',
                  created_at timestamptz not null default now()
                )
                """,
                "alter table users add column if not exists password_hash text",
                "alter table users add column if not exists firstname text not null default ''",
                "alter table users add column if not exists lastname text not null default ''",
                "alter table users add column if not exists role text not null default 'USER'",
                "alter table users add column if not exists created_at timestamptz not null default now()",
                """
                create table if not exists refresh_tokens (
                  id bigserial primary key,
                  user_id bigint not null references users(id) on delete cascade,
                  token_hash text not null,
                  expires_at timestamptz not null,
                  revoked_at timestamptz,
                  created_at timestamptz not null default now()
                )
                """,
                """
                create table if not exists transactions (
                  id bigserial primary key,
                  user_id bigint not null references users(id) on delete cascade,
                  type text not null,
                  amount numeric(12,2) not null,
                  category text not null,
                  description text,
                  transaction_date date not null default current_date,
                  created_at timestamptz not null default now(),
                  updated_at timestamptz
                )
                """,
                """
                create table if not exists groups (
                  id bigserial primary key,
                  name text not null,
                  description text,
                  created_by bigint not null references users(id),
                  created_at timestamptz not null default now()
                )
                """,
                """
                create table if not exists group_members (
                  group_id bigint not null references groups(id) on delete cascade,
                  user_id bigint not null references users(id) on delete cascade,
                  role text not null default 'MEMBER',
                  joined_at timestamptz not null default now(),
                  primary key (group_id, user_id)
                )
                """,
                """
                create table if not exists group_invitations (
                  id bigserial primary key,
                  group_id bigint not null references groups(id) on delete cascade,
                  invited_user_id bigint not null references users(id) on delete cascade,
                  invited_by_user_id bigint not null references users(id),
                  status text not null,
                  created_at timestamptz not null default now(),
                  responded_at timestamptz
                )
                """,
                """
                create table if not exists group_transactions (
                  id bigserial primary key,
                  group_id bigint not null references groups(id) on delete cascade,
                  created_by_user_id bigint not null references users(id),
                  type text not null,
                  amount numeric(12,2) not null,
                  category text not null,
                  description text,
                  transaction_date date not null default current_date,
                  created_at timestamptz not null default now(),
                  updated_at timestamptz
                )
                """,
                """
                create table if not exists expense_splits (
                  id bigserial primary key,
                  group_transaction_id bigint not null references group_transactions(id) on delete cascade,
                  user_id bigint not null references users(id),
                  amount numeric(12,2) not null,
                  settled boolean not null default false,
                  settled_at timestamptz
                )
                """,
                """
                create table if not exists group_activity_logs (
                  id bigserial primary key,
                  group_id bigint not null references groups(id) on delete cascade,
                  actor_user_id bigint not null references users(id),
                  actor_username text not null,
                  action_type text not null,
                  entity_type text not null,
                  entity_id bigint,
                  old_value text,
                  new_value text,
                  description text not null,
                  created_at timestamptz not null default now()
                )
                """,
                """
                create table if not exists inbox_notifications (
                  id bigserial primary key,
                  recipient_user_id bigint not null references users(id) on delete cascade,
                  group_id bigint references groups(id) on delete cascade,
                  invitation_id bigint references group_invitations(id) on delete cascade,
                  type text not null,
                  title text not null,
                  message text not null,
                  is_read boolean not null default false,
                  created_at timestamptz not null default now()
                )
                """,
                "create index if not exists idx_transactions_user_id on transactions(user_id)",
                "create index if not exists idx_group_members_user_id on group_members(user_id)",
                "create index if not exists idx_group_invitations_user_status on group_invitations(invited_user_id, status)",
                "create index if not exists idx_group_transactions_group_id on group_transactions(group_id)",
                "create index if not exists idx_group_activity_group_created on group_activity_logs(group_id, created_at desc)",
                "create index if not exists idx_inbox_recipient_created on inbox_notifications(recipient_user_id, created_at desc)"
        };
        for (String statement : statements) {
            jdbcTemplate.execute(statement);
        }
    }

    private void loadUsers() {
        List<UserRecord> users = jdbcTemplate.query("""
                select id, email, password_hash, firstname, lastname, role, 'local' as auth_provider,
                  null as google_id, created_at
                from users
                order by id
                """, (rs, rowNum) -> new UserRecord(rs.getLong("id"), rs.getString("email"),
                rs.getString("password_hash"), rs.getString("firstname"), rs.getString("lastname"),
                rs.getString("role"), rs.getString("auth_provider"), rs.getString("google_id"),
                rs.getTimestamp("created_at").toLocalDateTime()));
        if (!users.isEmpty() && store.users.isEmpty()) {
            store.users.addAll(users);
            long nextId = users.stream().mapToLong(UserRecord::id).max().orElse(0L) + 1;
            store.userIds.set(nextId);
            log.info("Loaded {} users from database into BudgetBuddy store.", users.size());
        }
    }

    private void loadTransactions() {
        List<TransactionRecord> records = jdbcTemplate.query("""
                select id, user_id, type, amount, category, description, transaction_date
                from transactions
                order by id
                """, (rs, rowNum) -> new TransactionRecord(rs.getLong("id"), rs.getLong("user_id"),
                rs.getString("type"), rs.getBigDecimal("amount"), rs.getString("category"),
                rs.getString("description"), rs.getDate("transaction_date").toLocalDate(), false));
        if (!records.isEmpty() && store.transactions.isEmpty()) {
            store.transactions.addAll(records);
            store.transactionIds.set(records.stream().mapToLong(TransactionRecord::id).max().orElse(0L) + 1);
        }
    }

    private void loadGroups() {
        List<GroupRecord> records = jdbcTemplate.query("""
                select id, name, description, created_by
                from groups
                order by id
                """, (rs, rowNum) -> new GroupRecord(rs.getLong("id"), rs.getString("name"),
                rs.getString("description"), rs.getLong("created_by"), false));
        if (!records.isEmpty() && store.groups.isEmpty()) {
            store.groups.addAll(records);
            store.groupIds.set(records.stream().mapToLong(GroupRecord::id).max().orElse(0L) + 1);
        }
    }

    private void loadGroupMembers() {
        List<GroupMemberRecord> records = jdbcTemplate.query("""
                select group_id, user_id, role
                from group_members
                """, (rs, rowNum) -> new GroupMemberRecord(rs.getLong("group_id"),
                rs.getLong("user_id"), rs.getString("role"), false));
        if (!records.isEmpty() && store.members.isEmpty()) {
            store.members.addAll(records);
        }
    }

    private void loadInvitations() {
        List<GroupInvitationRecord> records = jdbcTemplate.query("""
                select id, group_id, invited_user_id, invited_by_user_id, status, created_at, responded_at
                from group_invitations
                order by id
                """, (rs, rowNum) -> new GroupInvitationRecord(rs.getLong("id"), rs.getLong("group_id"),
                rs.getLong("invited_user_id"), rs.getLong("invited_by_user_id"), rs.getString("status"),
                rs.getTimestamp("created_at").toLocalDateTime(),
                rs.getTimestamp("responded_at") == null ? null : rs.getTimestamp("responded_at").toLocalDateTime()));
        if (!records.isEmpty() && store.groupInvitations.isEmpty()) {
            store.groupInvitations.addAll(records);
            store.invitationIds.set(records.stream().mapToLong(GroupInvitationRecord::id).max().orElse(0L) + 1);
        }
    }

    private void loadInbox() {
        List<InboxNotificationRecord> records = jdbcTemplate.query("""
                select id, recipient_user_id, group_id, invitation_id, type, title, message, is_read, created_at
                from inbox_notifications
                order by id
                """, (rs, rowNum) -> new InboxNotificationRecord(rs.getLong("id"), rs.getLong("recipient_user_id"),
                nullableLong(rs, "group_id"), nullableLong(rs, "invitation_id"), rs.getString("type"),
                rs.getString("title"), rs.getString("message"), rs.getBoolean("is_read"),
                rs.getTimestamp("created_at").toLocalDateTime()));
        if (!records.isEmpty() && store.inboxNotifications.isEmpty()) {
            store.inboxNotifications.addAll(records);
            store.inboxIds.set(records.stream().mapToLong(InboxNotificationRecord::id).max().orElse(0L) + 1);
        }
    }

    private void loadGroupTransactions() {
        List<GroupTransactionRecord> records = jdbcTemplate.query("""
                select id, group_id, created_by_user_id, type, amount, category, description,
                  transaction_date, created_at, updated_at
                from group_transactions
                order by id
                """, (rs, rowNum) -> new GroupTransactionRecord(rs.getLong("id"), rs.getLong("group_id"),
                rs.getLong("created_by_user_id"), rs.getString("type"), rs.getBigDecimal("amount"),
                rs.getString("category"), rs.getString("description"), rs.getDate("transaction_date").toLocalDate(),
                rs.getTimestamp("created_at").toLocalDateTime(),
                rs.getTimestamp("updated_at") == null ? rs.getTimestamp("created_at").toLocalDateTime()
                        : rs.getTimestamp("updated_at").toLocalDateTime(),
                null, false));
        if (!records.isEmpty() && store.groupTransactions.isEmpty()) {
            store.groupTransactions.addAll(records);
            store.groupTransactionIds.set(records.stream().mapToLong(GroupTransactionRecord::id).max().orElse(0L) + 1);
        }
    }

    private void loadGroupActivity() {
        List<GroupActivityLogRecord> records = jdbcTemplate.query("""
                select id, group_id, actor_user_id, actor_username, action_type, entity_type,
                  entity_id, old_value, new_value, description, created_at
                from group_activity_logs
                order by id
                """, (rs, rowNum) -> new GroupActivityLogRecord(rs.getLong("id"), rs.getLong("group_id"),
                rs.getLong("actor_user_id"), rs.getString("actor_username"), rs.getString("action_type"),
                rs.getString("entity_type"), nullableLong(rs, "entity_id"), rs.getString("old_value"),
                rs.getString("new_value"), rs.getString("description"),
                rs.getTimestamp("created_at").toLocalDateTime()));
        if (!records.isEmpty() && store.groupActivityLogs.isEmpty()) {
            store.groupActivityLogs.addAll(records);
            store.groupActivityIds.set(records.stream().mapToLong(GroupActivityLogRecord::id).max().orElse(0L) + 1);
        }
    }

    private Long nullableLong(java.sql.ResultSet rs, String column) throws java.sql.SQLException {
        long value = rs.getLong(column);
        return rs.wasNull() ? null : value;
    }

    private void execute(String action, String sql, Object... args) {
        if (!enabled) {
            return;
        }
        try {
            jdbcTemplate.update(sql, args);
        } catch (RuntimeException ex) {
            log.warn("Database persistence skipped for {}: {}", action, ex.getMessage());
        }
    }
}
