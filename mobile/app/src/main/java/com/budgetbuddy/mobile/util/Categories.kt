package com.budgetbuddy.mobile.util

object Categories {
    val values = listOf(
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
    )

    fun label(value: String?): String = value.orEmpty()
        .lowercase()
        .split("_")
        .joinToString(" ") { word -> word.replaceFirstChar { it.uppercase() } }
        .replace("And", "&")
}
