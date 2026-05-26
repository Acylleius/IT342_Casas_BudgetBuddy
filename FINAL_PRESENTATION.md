# BudgetBuddy Finals Presentation

## System Overview

BudgetBuddy is a user-centered shared finance app for normal registered users. It supports personal transactions, group invitations, shared group income and expenses, group-only history, in-app inbox notifications, mobile access, and realtime update awareness.

## Architecture

Backend uses Spring Boot with Vertical Slice Architecture:

- `features/auth`
- `features/transactions`
- `features/groups`
- `features/grouptransactions`
- `features/invitations`
- `features/inbox`
- `features/sharedexpenses`
- `features/notifications`
- `features/realtime`
- `features/users`
- `shared/store`
- `shared/utils`

Frontend uses feature-based folders:

- `features/auth`
- `features/dashboard`
- `features/groups`
- `features/inbox`
- `features/profile`
- `shared/js`
- `shared/styles`

Mobile uses Android Kotlin screens for login/register, dashboard, groups, group detail, inbox, and invitation responses.

## Implemented Features

- Registered user auth with access token and refresh token support.
- Persistent web sessions through `/auth/refresh`.
- Personal income/expense transactions.
- Group creation for registered users.
- Registered-user-only group invitations.
- Invitation accept/decline flow.
- In-app inbox with welcome messages, group invites, and group updates.
- Inbox unread badges and read/unread visual states.
- Accepted and declined invitations become inactive and show their final status.
- Group transactions with `INCOME` and `EXPENSE`.
- Group balances for expense splitting.
- Group-only history with actor display names.
- Server-Sent Events for live group/inbox awareness.
- Spring Mail notification hook, with inbox as the primary fallback.
- Supabase proof schema and Postman collection.
- Optional Supabase/PostgreSQL persistence using `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD`.
- Stacked pastel toast notifications for success/error feedback.

## Data Flow

1. User registers or logs in.
2. Backend returns access and refresh tokens.
3. Web stores the tokens and restores sessions with `/auth/refresh`.
4. User creates a group.
5. User invites another registered user by email.
6. Invitee receives an inbox notification and can accept or decline.
7. Accepting creates a group member record.
8. Group income/expense actions create group transactions, group history rows, inbox updates, and realtime events for accepted members.

## Demo Flow

1. Register User A.
2. Register User B.
3. User A creates group `Roommates`.
4. User A invites User B.
5. User B opens Inbox and sees the invitation.
6. User B accepts.
7. User A adds group expense `Rent ₱6000`.
8. User B sees the update in Inbox and Group History.
9. User B adds income/contribution `₱2000`.
10. User A sees the update.
11. Show Postman API requests for auth, invite, inbox, group transactions, and history.
12. Show Supabase SQL proof tables including `group_invitations`, `inbox_notifications`, and `group_activity_logs`.

## Screenshot Checklist

- Backend vertical slice folders.
- Web feature folders.
- Android project screens.
- Login/register success.
- Dashboard personal transaction summary.
- Groups page.
- Group detail summary, members, transactions, balances, and history.
- Inbox invitation with Accept/Decline.
- Postman successful API requests.
- Supabase schema/tables.

## Validation Commands

```powershell
.\mvnw.cmd test
cd mobile
.\gradlew.bat :app:assembleDebug
```
