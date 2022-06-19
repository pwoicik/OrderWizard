package com.example.wizard.core.ui.components

import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable

@Composable
fun SnackbarHost(
    state: SnackbarHostState
) {
    SnackbarHost(hostState = state) {
        when (val visuals = it.visuals as SnackbarVisuals) {
            is SnackbarVisuals.Error -> {
                Snackbar(snackbarData = it)
            }
            is SnackbarVisuals.Loading -> {
                LoadingDialog(visuals.message)
            }
        }
    }
}

sealed interface SnackbarVisuals : androidx.compose.material3.SnackbarVisuals {

    data class Error(
        override val message: String
    ) : SnackbarVisuals {
        override val actionLabel: String? = null
        override val duration: SnackbarDuration = SnackbarDuration.Short
        override val withDismissAction: Boolean = false
    }

    data class Loading(
        override val message: String
    ) : SnackbarVisuals {
        override val actionLabel: String? = null
        override val duration: SnackbarDuration = SnackbarDuration.Indefinite
        override val withDismissAction: Boolean = false
    }
}
