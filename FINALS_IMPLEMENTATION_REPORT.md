# BudgetBuddy User-Centered Finals Enhancement Report

## Branch

Current local branch: `feature/finals-system-enhancement`

No push was performed.

## Removed Admin Surface

- Removed backend admin controller/routes.
- Removed web admin page.
- Removed dashboard admin navigation link.
- Removed first-user admin behavior.
- All registered users are now normal `USER` accounts.

## Backend Additions

- Added startup persistence mode logs:
  - `Database persistence enabled.`
  - `Supabase/PostgreSQL not configured. Using in-memory fallback store.`
- Added database persistence bridge for configured Supabase/PostgreSQL runs.
- Mirrored users, refresh tokens, personal transactions, groups, members, invitations, inbox notifications, group transactions, and group activity logs to PostgreSQL when `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD` are set.
- Group invitation flow:
  - `POST /api/v1/groups/{groupId}/invitations`
  - `GET /api/v1/invitations`
  - `POST /api/v1/invitations/{invitationId}/accept`
  - `POST /api/v1/invitations/{invitationId}/decline`
- Inbox:
  - `GET /api/v1/inbox`
  - `POST /api/v1/inbox/{id}/read`
  - `POST /api/v1/inbox/read-all`
  - Inbox DTOs now include `isRead`, `unreadCount`, and `invitationStatus`.
- Accept/decline invitation now marks the original invitation notification as read.
- Group history:
  - `GET /api/v1/groups/{groupId}/history`
- Group income/expense:
  - `POST /api/v1/groups/{groupId}/transactions`
  - `GET /api/v1/groups/{groupId}/transactions`
  - `PUT /api/v1/groups/{groupId}/transactions/{transactionId}`
  - `DELETE /api/v1/groups/{groupId}/transactions/{transactionId}`
  - `GET /api/v1/groups/{groupId}/transactions/summary`
- Realtime:
  - SSE remains available at `/api/v1/realtime/stream?token=...`
  - User-scoped events are used for inbox updates.
  - Group transaction events are sent to accepted group members.

## Web Additions

- Added Inbox page.
- Added unread inbox indicators in protected navigation.
- Added unread/read inbox visual states.
- Accepted/declined invitations now hide active buttons and show status labels.
- Replaced overlapping toast messages with a stacked toast container, close button, auto-dismiss timing, and success/error/warning/info variants.
- Removed admin link.
- Group detail page now has:
  - Group Summary
  - Add Group Transaction with Income/Expense type
  - Group Transactions
  - Member Balances
  - Invite Member
  - Group History
- Navigation now focuses on:
  - Dashboard
  - Groups
  - Inbox
  - Profile
  - Logout

## Android Additions

- Added Android API models for inbox, groups, group transactions, invitations, and group history.
- Added Inbox screen with invitation accept/decline actions.
- Added Groups screen.
- Added Group Detail screen showing transactions and history.
- Dashboard links to Groups and Inbox.
- Added pastel gradient background and softer card styling to match the web visual direction.

## Database / Supabase

Updated `database/supabase_finals_schema.sql` with:

- users
- refresh_tokens
- transactions
- groups
- group_members
- group_invitations
- group_transactions
- expense_splits
- group_activity_logs
- inbox_notifications

Added `DATABASE_SETUP.md` with environment variables, setup steps, verification queries, and troubleshooting.

## Postman

Updated `postman/BudgetBuddy_Finals.postman_collection.json`.

Removed admin endpoints and added invitations, inbox, group transactions, group history, and realtime.
Added a collection-level test that fails on server errors.

## Tests Added

Added finals user-flow regression tests for:

- users are normal `USER` accounts
- unregistered invite rejection
- accept invitation adds member
- decline invitation does not add member
- non-member cannot view group history
- member can view group history
- group history is group-specific
- inbox is scoped to current user
- group income creation
- group expense creation
- username appears in group history
- admin dashboard endpoint unavailable
- invitation accept marks invite notification read and exposes accepted status

## Known Limitations

- Current runtime still keeps the existing `BudgetBuddyStore` as the working cache, but mirrors records to Supabase/PostgreSQL when `DB_*` environment variables are configured and loads database records on startup.
- Android provides lightweight inbox/groups/group-detail support and builds successfully, but the web remains the richer demo client.
- Email delivery still requires SMTP environment variables; inbox is the reliable notification fallback.
