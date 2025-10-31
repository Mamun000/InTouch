package com.example.intouch

import android.graphics.Bitmap

// Data class for Connection Request
data class ConnectionRequest(
    val requestId: String, // requestId format: "requesterId_requestedId"
    val requesterId: String, // User who sent the request
    val requestedId: String, // User who received the request
    val requesterName: String,
    val requesterProfession: String?,
    val requesterEmail: String?,
    val requesterProfileImage: Bitmap?,
    val timestamp: Long,
    val status: String // "pending", "accepted", "rejected"
)
