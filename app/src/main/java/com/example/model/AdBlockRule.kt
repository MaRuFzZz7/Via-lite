package com.example.model

data class AdBlockRule(
    val id: String,
    val pattern: String,
    val isRegex: Boolean = false,
    val isEnabled: Boolean = true,
    val description: String = ""
)
