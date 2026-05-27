package com.budgetbuddy.mobile.model

data class InboxNotification(
    val id: Long,
    val groupId: Long?,
    val invitationId: Long?,
    val invitationStatus: String?,
    val entityType: String?,
    val entityId: Long?,
    val actionStatus: String?,
    val type: String,
    val title: String,
    val message: String,
    val isRead: Boolean,
    val unreadCount: Long,
    val createdAt: String
)

data class Invitation(
    val id: Long,
    val groupId: Long,
    val groupName: String,
    val status: String
)

data class Group(
    val id: Long,
    val name: String,
    val description: String?,
    val role: String
)

data class GroupTransaction(
    val id: Long,
    val actorUsername: String,
    val type: String,
    val amount: Double,
    val category: String,
    val description: String?,
    val verificationStatus: String?
)

data class GroupHistoryItem(
    val id: Long,
    val actorUsername: String,
    val actionType: String,
    val description: String,
    val createdAt: String
)

data class BudgetRequest(
    val name: String,
    val limitAmount: Double,
    val period: String,
    val category: String? = null
)

data class Budget(
    val id: Long,
    val scope: String,
    val name: String,
    val limitAmount: Double,
    val period: String,
    val category: String?,
    val spentAmount: Double,
    val remainingAmount: Double,
    val percentageUsed: Double,
    val status: String
)

data class BudgetTracking(
    val budget: Budget
)

data class SavingGoalRequest(
    val title: String,
    val targetAmount: Double,
    val currentAmount: Double
)

data class SavingGoal(
    val id: Long,
    val scope: String,
    val title: String,
    val targetAmount: Double,
    val currentAmount: Double,
    val remainingAmount: Double,
    val percentageUsed: Double,
    val status: String
)
