package com.example.wizard.core.ui

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import com.example.wizard.core.ui.components.Column
import com.example.wizard.core.ui.theme.WizardTheme

abstract class BaseActivity : AppCompatActivity() {

    @Composable
    abstract fun ColumnScope.Content()

    @Composable
    open fun TopBar() = Unit

    @Composable
    open fun SnackbarHost(state: SnackbarHostState) = Unit

    @Composable
    open fun BottomBar() = Unit

    final override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            WizardTheme {
                @OptIn(ExperimentalMaterial3Api::class)
                Scaffold(
                    topBar = { TopBar() }
                ) { innerPadding ->
                    Column(
                        modifier = Modifier
                            .padding(innerPadding)
                            .navigationBarsPadding()
                    ) {
                        SnackbarHost(state = remember { SnackbarHostState() })
                        Column(Modifier.weight(1f)) {
                            Content()
                        }
                        BottomBar()
                    }
                }
            }
        }
    }
}
