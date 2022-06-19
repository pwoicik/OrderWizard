package com.example.wizard.core.ui.util

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import arrow.optics.optics

@Immutable
@optics
data class InputValue(

    val text: String = "",

    @StringRes
    val errorMessage: Int? = null,

    val isRequired: Boolean = true,

    val isEnabled: Boolean = true
) {
    companion object
}
