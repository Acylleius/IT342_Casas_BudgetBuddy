# BudgetBuddy Database Setup

## Purpose

BudgetBuddy can run in two modes:

- Database persistence mode when `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD` are configured.
- In-memory fallback mode when those variables are missing.

On startup the backend logs one of these messages:

```text
Database persistence enabled.
```

or:

```text
Supabase/PostgreSQL not configured. Using in-memory fallback store.
```

## Supabase Setup

1. Create a Supabase project.
2. Open SQL Editor.
3. Run:

   ```text
   database/supabase_finals_schema.sql
   ```

4. Copy the PostgreSQL connection values from Supabase.

## Required Environment Variables

```powershell
$env:DB_URL="jdbc:postgresql://<host>:5432/postgres?sslmode=require"
$env:DB_USERNAME="postgres.<project-ref>"
$env:DB_PASSWORD="<database-password>"
```

Optional:

```powershell
$env:SPRING_PROFILES_ACTIVE="prod"
```

## Running

From the repository root:

```powershell
.\mvnw.cmd spring-boot:run
```

## Verify Records Are Saving

After registering users and using the app, run these in Supabase SQL Editor:

```sql
select * from users order by id desc;
select * from transactions order by id desc;
select * from groups order by id desc;
select * from group_members order by joined_at desc;
select * from group_invitations order by id desc;
select * from group_transactions order by id desc;
select * from group_activity_logs order by created_at desc;
select * from inbox_notifications order by created_at desc;
```

## Common Errors

- `connection timed out`: check Supabase host, password, and network access.
- `relation does not exist`: restart the backend so it can auto-create the presentation tables, or run `database/supabase_finals_schema.sql` manually in Supabase SQL Editor.
- `password authentication failed`: verify `DB_USERNAME` and `DB_PASSWORD`.
- App logs fallback mode: one or more required `DB_*` variables are missing.

## Notes

The current implementation keeps the existing vertical-slice services and mirrors records to PostgreSQL when configured. If database credentials are missing, the app remains usable with the in-memory fallback store for local testing.
