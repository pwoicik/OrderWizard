package com.example.wizard.domain.model

import java.util.UUID

data class User(
    val id: UUID,
    val name: String,
    val surname: String,
    val email: String,
    val phoneNumber: String,
    val country: Country
)
