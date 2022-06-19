package com.example.wizard.core.ui.components

import android.content.Context
import android.view.View
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewTreeLifecycleOwner
import androidx.lifecycle.ViewTreeViewModelStoreOwner
import androidx.savedstate.findViewTreeSavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.google.android.material.bottomsheet.BottomSheetDialog
import java.util.*

@Composable
fun BottomSheet(
    onDismissRequest: () -> Unit,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    val density = LocalDensity.current
    val composition = rememberCompositionContext()
    val currentContent by rememberUpdatedState(content)
    val sheetId = remember { UUID.randomUUID() }

    val sheet = remember(view, density) {
        BottomSheetDialog(view, sheetId).apply {
            setContent(composition) {
                Surface(
                    color = containerColor,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 100.dp),
                    content = currentContent
                )
            }
        }
    }

    DisposableEffect(view, density) {
        sheet.show()

        onDispose {
            sheet.dismiss()
            sheet.disposeComposition()
        }
    }

    SideEffect {
        sheet.update(containerColor, onDismissRequest)
    }
}

private class BottomSheetDialog(
    view: View,
    sheetId: UUID
) : BottomSheetDialog(view.context) {

    val bottomSheetView: BottomSheetView

    private lateinit var onDismissRequest: () -> Unit

    init {
        bottomSheetView = BottomSheetView(view.context).apply {
            setTag(androidx.compose.ui.R.id.compose_view_saveable_id_tag, "Dialog:$sheetId")
        }

        setContentView(bottomSheetView)
        ViewTreeLifecycleOwner.set(bottomSheetView, ViewTreeLifecycleOwner.get(view))
        ViewTreeViewModelStoreOwner.set(bottomSheetView, ViewTreeViewModelStoreOwner.get(view))
        bottomSheetView.setViewTreeSavedStateRegistryOwner(
            view.findViewTreeSavedStateRegistryOwner()
        )
    }

    fun update(
        navBarColor: Color,
        onDismissRequest: () -> Unit
    ) {
        window.navigationBarColor = navBarColor.toArgb()
        this.onDismissRequest = onDismissRequest
    }

    fun disposeComposition() {
        bottomSheetView.disposeComposition()
    }

    fun setContent(parentComposition: CompositionContext, content: @Composable () -> Unit) {
        bottomSheetView.setContent(parentComposition, content)
    }

    override fun onBackPressed() {
        onDismissRequest()
    }

    override fun cancel() {
        onDismissRequest()
    }

    override fun getWindow() = super.getWindow()!!
}

class BottomSheetView(context: Context) : AbstractComposeView(context) {

    private var content: @Composable () -> Unit by mutableStateOf({})

    override var shouldCreateCompositionOnAttachedToWindow: Boolean = false
        private set

    fun setContent(parentComposition: CompositionContext, content: @Composable () -> Unit) {
        setParentCompositionContext(parentComposition)
        this.content = content
        shouldCreateCompositionOnAttachedToWindow = true
        createComposition()
    }

    @Composable
    override fun Content() {
        content()
    }
}
