package com.example.wizard.core.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.unit.dp

@Composable
fun Section(
    title: String,
    modifier: Modifier = Modifier,
    titleContentPadding: PaddingValues = PaddingValues(),
    action: @Composable () -> Unit = {},
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(
                    min = LocalViewConfiguration.current.minimumTouchTargetSize.height
                )
                .padding(titleContentPadding)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium
            )
            action()
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            content()
        }
    }
}
