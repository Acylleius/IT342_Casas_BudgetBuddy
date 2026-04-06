# BudgetBuddy Design Pattern Research

## Project Context

BudgetBuddy is a cross-platform budgeting application with a Spring Boot backend and an Android Kotlin mobile client. The system includes authentication, user persistence, API communication, and session-aware UI navigation. These characteristics make the project a suitable candidate for several object-oriented design patterns.

## 1. Factory Pattern

- Category: Creational
- Intent: Centralize and simplify object creation.
- Problem Solved: Repeated manual construction of success and error responses can create duplication and inconsistent API output.
- Project Fit: The backend now uses `ResponseFactory` to build standardized `AuthResponse` objects for registration and login.
- Benefit: Response creation is reusable, cleaner, and easier to extend.

## 2. Singleton Pattern

- Category: Creational
- Intent: Ensure that only one shared instance exists.
- Problem Solved: Certain infrastructure objects should not be recreated repeatedly across the application.
- Project Fit: The mobile app uses `object RetrofitClient` and `object SessionManager`.
- Benefit: Network access and session state remain centralized and consistent.

## 3. Adapter Pattern

- Category: Structural
- Intent: Convert one interface or object structure into another that the client needs.
- Problem Solved: Backend authentication responses are not the same shape as the mobile UI session model.
- Project Fit: The mobile app now uses `AuthResponseAdapter` to convert `AuthResponse` into `SessionUser`.
- Benefit: The UI no longer depends directly on raw API payload structure.

## 4. Repository Pattern

- Category: Structural
- Intent: Separate data access from business logic.
- Problem Solved: Services should not directly manage query logic or persistence details.
- Project Fit: The backend uses `UserRepository` to manage database access for `User`.
- Benefit: Business logic stays cleaner and persistence can change independently.

## 5. Strategy Pattern

- Category: Behavioral
- Intent: Encapsulate interchangeable algorithms behind a common interface.
- Problem Solved: Authentication logic can grow to support multiple login flows.
- Project Fit: The backend now uses `AuthStrategy`, `EmailPasswordAuthStrategy`, and `AuthContext`.
- Benefit: New authentication methods such as Google login can be added with minimal controller changes.

## 6. Observer Pattern

- Category: Behavioral
- Intent: Notify subscribed objects when state changes.
- Problem Solved: Multiple screens need to react when login state changes.
- Project Fit: The mobile `SessionManager` keeps observers and notifies them when session data is saved or cleared.
- Benefit: Login and dashboard screens respond automatically to session updates.

## Summary

The chosen patterns are suitable because they match the actual responsibilities of the BudgetBuddy system:

- Factory standardizes backend API responses.
- Singleton centralizes shared mobile services.
- Adapter bridges backend response models to UI state.
- Repository isolates data access in the backend.
- Strategy makes authentication extensible.
- Observer keeps the mobile interface reactive to session changes.

These patterns improve maintainability, readability, and scalability without making the current project unnecessarily complex.
