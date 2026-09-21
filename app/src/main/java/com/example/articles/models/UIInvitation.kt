package com.example.articles.models

data class UIInvitation(
    val invitationId: String,
    val fromUid: String,
    val fromEmail: String,
    val toEmail: String,
    val toUid: String,
    val groupId: String,
    val groupName: String,
    val status: InvitationStatus,
    val createdAt: Long,
    val existingSharedGroupId: String? = null
)

enum class InvitationStatus {
    PENDING,
    ACCEPTED,
    DECLINED
}
