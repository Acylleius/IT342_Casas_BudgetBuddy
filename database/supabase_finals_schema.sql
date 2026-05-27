-- BudgetBuddy Finals Supabase/PostgreSQL schema.
-- Purpose: presentation-ready database proof for normal registered users,
-- personal transactions, group invitations, group income/expense tracking,
-- group-only history, inbox notifications, refresh tokens, and settlements.

-- Registered application users.
create table if not exists users (
  id bigserial primary key,
  email text unique not null,
  password_hash text,
  firstname text not null,
  lastname text not null,
  role text not null default 'USER',
  created_at timestamptz not null default now()
);

-- Refresh tokens used to restore user sessions after browser/app reopen.
create table if not exists refresh_tokens (
  id bigserial primary key,
  user_id bigint not null references users(id) on delete cascade,
  token_hash text not null,
  expires_at timestamptz not null,
  revoked_at timestamptz,
  created_at timestamptz not null default now()
);

-- Personal user transactions outside groups.
create table if not exists transactions (
  id bigserial primary key,
  user_id bigint not null references users(id) on delete cascade,
  type text not null check (type in ('INCOME', 'EXPENSE')),
  amount numeric(12,2) not null check (amount > 0),
  category text not null,
  description text,
  transaction_date date not null default current_date,
  created_at timestamptz not null default now(),
  updated_at timestamptz
);

-- Shared finance groups.
create table if not exists groups (
  id bigserial primary key,
  name text not null,
  description text,
  created_by bigint not null references users(id),
  created_at timestamptz not null default now()
);

-- Accepted group members only. Pending invited users are not inserted here yet.
create table if not exists group_members (
  group_id bigint not null references groups(id) on delete cascade,
  user_id bigint not null references users(id) on delete cascade,
  role text not null default 'MEMBER',
  joined_at timestamptz not null default now(),
  primary key (group_id, user_id)
);

-- Group invitations for registered users.
create table if not exists group_invitations (
  id bigserial primary key,
  group_id bigint not null references groups(id) on delete cascade,
  invited_user_id bigint not null references users(id) on delete cascade,
  invited_by_user_id bigint not null references users(id),
  status text not null check (status in ('PENDING', 'ACCEPTED', 'DECLINED')),
  created_at timestamptz not null default now(),
  responded_at timestamptz,
  unique (group_id, invited_user_id, status)
);

-- Group income and expense records.
create table if not exists group_transactions (
  id bigserial primary key,
  group_id bigint not null references groups(id) on delete cascade,
  created_by_user_id bigint not null references users(id),
  type text not null check (type in ('INCOME', 'EXPENSE')),
  amount numeric(12,2) not null check (amount > 0),
  category text not null,
  description text,
  transaction_date date not null default current_date,
  created_at timestamptz not null default now(),
  updated_at timestamptz
);

-- Split records for group expenses and settlement proof.
create table if not exists expense_splits (
  id bigserial primary key,
  group_transaction_id bigint not null references group_transactions(id) on delete cascade,
  user_id bigint not null references users(id),
  amount numeric(12,2) not null check (amount >= 0),
  settled boolean not null default false,
  settled_at timestamptz
);

-- Group-only audit trail. Members can view only their own group's rows.
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
);

-- In-app notifications for welcome messages, invites, and group updates.
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
);

-- Weekly/monthly budget limits for personal and group spending.
create table if not exists budgets (
  id bigserial primary key,
  scope text not null check (scope in ('PERSONAL', 'GROUP')),
  user_id bigint references users(id) on delete cascade,
  group_id bigint references groups(id) on delete cascade,
  created_by_user_id bigint not null references users(id),
  name text not null,
  limit_amount numeric(12,2) not null check (limit_amount > 0),
  period text not null check (period in ('WEEKLY', 'MONTHLY')),
  category text,
  start_date date,
  end_date date,
  warning_sent boolean not null default false,
  exceeded_sent boolean not null default false,
  deleted boolean not null default false,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  check (
    (scope = 'PERSONAL' and user_id is not null and group_id is null)
    or
    (scope = 'GROUP' and group_id is not null)
  )
);

-- Per-period alert guard so warning/exceeded inbox notifications are not spammed.
create table if not exists budget_alerts (
  id bigserial primary key,
  budget_id bigint not null references budgets(id) on delete cascade,
  alert_type text not null check (alert_type in ('WARNING', 'EXCEEDED')),
  period_start date not null,
  period_end date not null,
  created_at timestamptz not null default now(),
  unique (budget_id, alert_type, period_start, period_end)
);

-- Personal and group saving goals.
create table if not exists saving_goals (
  id bigserial primary key,
  scope text not null check (scope in ('PERSONAL', 'GROUP')),
  user_id bigint references users(id) on delete cascade,
  group_id bigint references groups(id) on delete cascade,
  created_by_user_id bigint not null references users(id),
  title text not null,
  target_amount numeric(12,2) not null check (target_amount > 0),
  current_amount numeric(12,2) not null default 0 check (current_amount >= 0),
  deadline date,
  status text not null default 'IN_PROGRESS' check (status in ('IN_PROGRESS', 'COMPLETED', 'OVERDUE')),
  deleted boolean not null default false,
  completion_notified boolean not null default false,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  check (
    (scope = 'PERSONAL' and user_id is not null and group_id is null)
    or
    (scope = 'GROUP' and group_id is not null)
  )
);

-- Group saving goal contribution history.
create table if not exists saving_goal_contributions (
  id bigserial primary key,
  saving_goal_id bigint not null references saving_goals(id) on delete cascade,
  user_id bigint not null references users(id),
  amount numeric(12,2) not null check (amount > 0),
  note text,
  created_at timestamptz not null default now()
);

create index if not exists idx_transactions_user_id on transactions(user_id);
create index if not exists idx_group_members_user_id on group_members(user_id);
create index if not exists idx_group_invitations_user_status on group_invitations(invited_user_id, status);
create index if not exists idx_group_transactions_group_id on group_transactions(group_id);
create index if not exists idx_group_activity_group_created on group_activity_logs(group_id, created_at desc);
create index if not exists idx_inbox_recipient_created on inbox_notifications(recipient_user_id, created_at desc);
create index if not exists idx_budgets_user on budgets(user_id);
create index if not exists idx_budgets_group on budgets(group_id);
create index if not exists idx_budget_alerts_budget_period on budget_alerts(budget_id, period_start, period_end);
create index if not exists idx_saving_goals_user on saving_goals(user_id);
create index if not exists idx_saving_goals_group on saving_goals(group_id);
create index if not exists idx_saving_goal_contributions_goal on saving_goal_contributions(saving_goal_id);
