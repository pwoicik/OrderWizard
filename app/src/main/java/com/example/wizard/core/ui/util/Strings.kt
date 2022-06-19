package com.example.wizard.core.ui.util

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource

@JvmInline
value class StringRes(@androidx.annotation.StringRes val id: Int)

@Composable
fun stringResource(res: StringRes): String {
    return stringResource(res.id)
}

fun Context.stringResource(res: StringRes) = resources.getString(res.id)
