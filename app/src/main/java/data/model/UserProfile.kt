package com.example.runbuddy.data.model

data class UserProfile(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val targetKm: Double? = null,
    val createdAt: Long = System.currentTimeMillis()
)
