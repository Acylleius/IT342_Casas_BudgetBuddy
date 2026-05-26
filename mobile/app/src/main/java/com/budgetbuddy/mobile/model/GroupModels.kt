package com.budgetbuddy.mobile.model

data class InboxNotification(
    val id: Long,
    val groupId: Long?,
    val invitationId: Long?,
    val invitationStatus: String?,
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
    val description: String?
)

data class GroupHistoryItem(
    val id: Long,
    val actorUsername: String,
    val actionType: String,
    val description: String,
    val createdAt: String
)
