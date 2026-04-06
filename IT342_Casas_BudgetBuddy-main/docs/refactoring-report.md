# BudgetBuddy Refactoring Report

## Objective

Refactor the existing BudgetBuddy backend and mobile modules by applying suitable design patterns that improve maintainability, reduce coupling, and better separate responsibilities.

## Initial Issues

### Backend

- `AuthController` handled endpoint logic and authentication flow too directly.
- `UserService` was concrete and used an in-memory list instead of the existing repository layer.
- Responses were plain strings, which did not match the structured mobile client expectations.
- There was no extensible authentication mechanism.

### Mobile

- Activities called Retrofit directly.
- `SessionManager` existed but did not manage any real state.
- UI screens depended too much on raw API responses.
- Login state changes were not observable by multiple screens.

## Patterns Applied

## 1. Strategy Pattern

- Location: Backend authentication flow
- Added Classes:
  - `AuthStrategy`
  - `EmailPasswordAuthStrategy`
  - `AuthContext`
- Reason: Encapsulate login behavior and prepare the codebase for future authentication strategies.
- Result: `AuthController` delegates authentication instead of hardcoding the logic.

## 2. Factory Pattern

- Location: Backend response creation
- Added Class:
  - `ResponseFactory`
- Reason: Standardize creation of success and error responses.
- Result: Login and registration now return consistent `AuthResponse` objects.

## 3. Repository Pattern

- Location: Backend persistence
- Existing Class:
  - `UserRepository`
- Refactor:
  - `UserService` became an interface.
  - `UserServiceImpl` now uses `UserRepository`.
- Reason: Use the repository layer properly and separate persistence from service logic.

## 4. Singleton Pattern

- Location: Mobile networking and session management
- Classes:
  - `RetrofitClient`
  - `SessionManager`
- Reason: Keep shared infrastructure centralized.
- Result: The app uses a single API client and a single session state manager.

## 5. Adapter Pattern

- Location: Mobile authentication mapping
- Added Class:
  - `AuthResponseAdapter`
- Reason: Convert backend `AuthResponse` into a mobile-friendly `SessionUser`.
- Result: The UI no longer depends on the raw transport model.

## 6. Observer Pattern

- Location: Mobile session flow
- Refactor:
  - `SessionManager` now stores observers and notifies them on session changes.
- Reason: Allow screens to react automatically when the session updates.
- Result: Login and dashboard screens respond to authentication state changes without tightly coupled direct calls.

## Key Code Changes

### Backend

- Replaced string-based auth responses with structured DTOs:
  - `AuthResponse`
  - `AuthData`
  - `ApiError`
  - `UserDto`
- Standardized auth route to `/api/v1/auth` to match the mobile client.
- Moved user lookup and save logic into `UserServiceImpl` with `UserRepository`.

### Mobile

- Added `AuthRepository` so activities no longer call Retrofit directly.
- Added `AuthResponseAdapter` for response-to-session conversion.
- Implemented observer-based `SessionManager`.
- Updated `MainActivity` to route users based on session state.
- Updated `LoginActivity` and `DashboardActivity` to react to observed session changes.
- Added the `INTERNET` permission in the Android manifest.

## Before and After

### Before

- Tight coupling between controllers, activities, and lower-level logic
- Inconsistent backend response structure
- Limited reuse of business logic
- No reactive session handling in the mobile app

### After

- Cleaner separation of concerns
- Consistent API response model
- Extensible authentication design
- Reusable mapping and session infrastructure
- Better alignment between backend and mobile layers

## Outcome

The refactoring introduced six relevant design patterns without overengineering the project. The changes are practical for the current BudgetBuddy scope and also provide a better foundation for future features such as alternative login methods, token refresh handling, and more modular mobile screens.
