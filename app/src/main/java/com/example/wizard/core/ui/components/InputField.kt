package com.example.wizard.core.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalTextToolbar
import androidx.compose.ui.platform.TextToolbar
import androidx.compose.ui.platform.TextToolbarStatus
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.wizard.core.ui.util.ValidatedInput
import com.example.wizard.core.ui.util.ifInvalid
import com.example.wizard.core.ui.util.isInvalid
import com.example.wizard.core.ui.util.stringResource
import kotlinx.coroutines.delay
import kotlinx.coroutines.job
import kotlin.time.Duration.Companion.milliseconds

@Suppress("UNUSED")
typealias InputFieldDefaults = TextFieldDefaults
typealias InputFieldColors = TextFieldColors

@OptIn(ExperimentalLayoutApi::class, ExperimentalAnimationApi::class)
@Composable
fun InputField(
    input: ValidatedInput,
    onInputChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    isRequired: Boolean = true,
    singleLine: Boolean = true,
    textStyle: TextStyle = LocalTextStyle.current,
    keyboardType: KeyboardType = KeyboardType.Text,
    readOnly: Boolean = false,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    imeAction: ImeAction = ImeAction.Next,
    onImeAction: (() -> Unit)? = null,
    autofocus: Boolean = false,
    clearFocusOnImeHide: Boolean = !readOnly,
    onFocusChange: ((Boolean) -> Unit)? = null,
    isTextToolbarEnabled: Boolean = !readOnly,
    colors: InputFieldColors = InputFieldDefaults.outlinedTextFieldColors()
) {
    val interactionSource = remember { MutableInteractionSource() }

    val focusRequester = remember { FocusRequester() }
    if (autofocus) {
        LaunchedEffect(Unit) {
            delay(200.milliseconds)
            coroutineContext.job.invokeOnCompletion {
                focusRequester.requestFocus()
            }
        }
    }

    var isFocused by remember {
        mutableStateOf(autofocus)
    }
    val isImeVisible = WindowInsets.isImeVisible
    val focusManager = LocalFocusManager.current
    LaunchedEffect(isImeVisible) {
        if (clearFocusOnImeHide && isFocused && !isImeVisible) {
            focusManager.clearFocus()
        }
    }

    val toolbar = when (isTextToolbarEnabled) {
        true -> LocalTextToolbar.current
        false -> remember {
            object : TextToolbar {
                override val status: TextToolbarStatus
                    get() = TextToolbarStatus.Hidden

                override fun hide() = Unit

                override fun showMenu(
                    rect: Rect,
                    onCopyRequested: (() -> Unit)?,
                    onPasteRequested: (() -> Unit)?,
                    onCutRequested: (() -> Unit)?,
                    onSelectAllRequested: (() -> Unit)?
                ) = Unit

            }
        }
    }
    Column(
        modifier = modifier.animateContentSize()
    ) {
        CompositionLocalProvider(LocalTextToolbar provides toolbar) {

            var textFieldValueState by remember {
                mutableStateOf(
                    TextFieldValue(
                        text = input.value,
                        selection = TextRange(Int.MAX_VALUE)
                    )
                )
            }
            val textFieldValue = textFieldValueState.copy(text = input.value)
            var lastTextValue by remember(input.value) { mutableStateOf(input.value) }
            OutlinedTextField(
                value = textFieldValue,
                onValueChange = { newTextFieldValueState ->
                    textFieldValueState = newTextFieldValueState

                    val stringChangedSinceLastInvocation =
                        lastTextValue != newTextFieldValueState.text
                    lastTextValue = newTextFieldValueState.text

                    if (stringChangedSinceLastInvocation) {
                        onInputChange(newTextFieldValueState.text)
                    }
                },
                label = {
                    Row {
                        Text(label)
                        if (isRequired) {
                            Text(
                                text = "*",
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier
                                    .offset(x = 2.dp, y = 0.dp)
                                    .scale(1.4f)
                            )
                        }
                    }
                },
                singleLine = singleLine,
                textStyle = textStyle,
                readOnly = readOnly,
                isError = input.isInvalid,
                visualTransformation = visualTransformation,
                interactionSource = interactionSource,
                keyboardOptions = KeyboardOptions.Default.copy(
                    keyboardType = keyboardType,
                    imeAction = imeAction
                ),
                keyboardActions = KeyboardActions {
                    onImeAction?.invoke() ?: defaultKeyboardAction(imeAction)
                },
                colors = colors,
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
                    .onFocusEvent {
                        val isNowFocused = it.isFocused
                        if (isNowFocused != isFocused) {
                            isFocused = isNowFocused
                            onFocusChange?.invoke(isNowFocused)
                        }
                    }
            )
        }
        input.ifInvalid {
            AnimatedContent(it.errorMessage) { errorMessageRes ->
                Text(
                    text = stringResource(errorMessageRes),
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.labelColor(
                        isError = true,
                        enabled = true,
                        interactionSource = interactionSource
                    ).value,
                    modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                )
            }
        }
    }
}
