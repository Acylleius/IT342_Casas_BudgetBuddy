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

create index if not exists idx_transactions_user_id on transactions(user_id);
create index if not exists idx_group_members_user_id on group_members(user_id);
create index if not exists idx_group_invitations_user_status on group_invitations(invited_user_id, status);
create index if not exists idx_group_transactions_group_id on group_transactions(group_id);
create index if not exists idx_group_activity_group_created on group_activity_logs(group_id, created_at desc);
create index if not exists idx_inbox_recipient_created on inbox_notifications(recipient_user_id, created_at desc);
