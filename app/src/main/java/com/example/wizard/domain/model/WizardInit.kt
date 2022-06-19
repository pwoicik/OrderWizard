package com.example.wizard.domain.model

import androidx.compose.ui.graphics.vector.ImageVector
import kotlin.time.Duration

data class WizardInit(
    val deliveryMethods: List<DeliveryMethod>
)

data class DeliveryMethod(
    val id: Long,
    val icon: ImageVector,
    val name: String,
    val estimatedTime: Duration,
    val cost: String
)
