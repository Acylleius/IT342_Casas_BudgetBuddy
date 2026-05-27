package edu.casas.budgetbuddy.shared.utils;

import java.util.Set;
import org.springframework.http.HttpStatus;

public final class CategoryUtils {
    public static final Set<String> ALLOWED = Set.of(
            "GENERAL",
            "FOOD_AND_DINING",
            "GROCERIES",
            "TRANSPORTATION",
            "UTILITIES",
            "RENT",
            "HOUSING",
            "HEALTHCARE",
            "MEDICINE",
            "EDUCATION",
            "ENTERTAINMENT",
            "SHOPPING",
            "TRAVEL",
            "SUBSCRIPTIONS",
            "PERSONAL_CARE",
            "FAMILY",
            "GIFTS_AND_DONATIONS",
            "SAVINGS",
            "DEBT_PAYMENT",
            "INSURANCE",
            "TAXES",
            "SALARY",
            "ALLOWANCE",
            "BUSINESS",
            "OTHER"
    );

    private CategoryUtils() {
    }

    public static String require(String category) {
        String normalized = normalize(category);
        if (normalized == null || !ALLOWED.contains(normalized)) {
            throw new DomainException(HttpStatus.BAD_REQUEST,
                    "Invalid category. Use one of: " + String.join(", ", ALLOWED));
        }
        return normalized;
    }

    public static String optional(String category) {
        if (category == null || category.isBlank()) {
            return "GENERAL";
        }
        return require(category);
    }

    public static boolean matches(String budgetCategory, String transactionCategory) {
        String budget = optional(budgetCategory);
        if ("GENERAL".equals(budget)) {
            return true;
        }
        return budget.equals(require(transactionCategory));
    }

    private static String normalize(String category) {
        if (category == null) {
            return null;
        }
        String normalized = category.trim().toUpperCase()
                .replace("&", "AND")
                .replaceAll("[\\s-]+", "_");
        return switch (normalized) {
            case "FOOD" -> "FOOD_AND_DINING";
            case "GROCERY" -> "GROCERIES";
            case "DONATION", "DONATIONS", "GIFTS" -> "GIFTS_AND_DONATIONS";
            case "DEBT" -> "DEBT_PAYMENT";
            case "CONTRIBUTION" -> "ALLOWANCE";
            default -> normalized;
        };
    }
}
