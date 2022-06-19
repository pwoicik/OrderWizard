package com.example.wizard.ui.wizard

import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.wizard.core.ui.BaseActivity
import com.example.wizard.core.ui.components.Column
import com.example.wizard.core.ui.components.InputField
import com.example.wizard.core.ui.components.ScrollableColumn
import com.example.wizard.core.ui.components.Section
import com.example.wizard.core.ui.components.SnackbarVisuals
import com.example.wizard.core.ui.util.stringResource
import com.example.wizard.domain.model.Country
import com.example.wizard.domain.model.DeliveryMethod
import com.example.wizard.util.Globals
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlin.random.Random
import kotlin.time.toJavaDuration

private val horizontalPadding = 12.dp
private val contentPadding = PaddingValues(horizontal = horizontalPadding)

@AndroidEntryPoint
class WizardActivity : BaseActivity() {

    private val viewModel by viewModels<WizardViewModel>()

    @Composable
    override fun ColumnScope.Content() {
        val state by viewModel.state.collectAsState()

        AnimatedVisibility(
            visible = state.userMessage != UserMessage.Initializing,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            ScrollableColumn(
                verticalArrangement = Arrangement.spacedBy(24.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                RecipientDataSection(
                    recipientData = state.recipientData,
                    onSignInButtonClick = { viewModel.emit(RecipientDataEvent.SignInButtonClicked) },
                    onUsernameChange = { viewModel.emit(UserSignInEvent.NameChanged(it)) },
                    onPasswordChange = { viewModel.emit(UserSignInEvent.PasswordChanged(it)) },
                    onSignInCancel = { viewModel.emit(UserSignInEvent.Canceled) },
                    onSignInConfirm = { viewModel.emit(UserSignInEvent.Confirmed) },
                    onNameChange = { viewModel.emit(RecipientDataEvent.NameChanged(it)) },
                    onSurnameChange = { viewModel.emit(RecipientDataEvent.SurnameChanged(it)) },
                    onEmailChange = { viewModel.emit(RecipientDataEvent.EmailChanged(it)) },
                    onCountryChange = { viewModel.emit(RecipientDataEvent.CountryChanged(it)) },
                    onPhoneNumberChange = {
                        viewModel.emit(RecipientDataEvent.PhoneNumberChanged(it))
                    },
                    countries = viewModel.countries,
                    onNext = { viewModel.emit(ProgressEvent.NextButtonClicked) }
                )

                AnimatedVisibility(
                    visible = state.currentStage >= WizardStage.DeliveryMethod,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    DeliveryMethodSection(
                        isLatestStage = state.currentStage == WizardStage.DeliveryMethod,
                        deliveryDetails = state.deliveryDetails,
                        onDeliveryMethodSelect = {
                            viewModel.emit(DeliveryDetailsEvent.DeliveryMethodSelected(it))
                        }
                    )
                }
            }
        }
    }

    @Composable
    override fun TopBar() {
        CenterAlignedTopAppBar(
            title = {
                Text("Wizard")
            },
            navigationIcon = {
                IconButton(onClick = { finish() }) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = null
                    )
                }
            },
            actions = {
                Switch(
                    checked = Globals.toggleFailure,
                    onCheckedChange = { Globals.toggleFailure = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.error,
                        checkedTrackColor = MaterialTheme.colorScheme.errorContainer,
                        checkedBorderColor = MaterialTheme.colorScheme.error,
                        uncheckedThumbColor = Color(0xFF04BF8A),
                        uncheckedTrackColor = Color.Green.copy(alpha = 0.1f),
                        uncheckedBorderColor = Color(0xFF04BF8A)
                    ),
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
            },
            modifier = Modifier.statusBarsPadding()
        )
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun SnackbarHost(state: SnackbarHostState) {
        com.example.wizard.core.ui.components.SnackbarHost(state = state)

        val userMessage by viewModel.userMessage.collectAsState()
        val context = LocalContext.current
        LaunchedEffect(userMessage) {
            launch {
                val um = userMessage
                if (um == null) {
                    viewModel.emit(UserMessageEvent.UserMessageSeen)
                    return@launch
                }

                val message = context.stringResource(um.body)
                when (um) {
                    UserMessage.Initializing,
                    UserMessage.LoadingData -> {
                        state.showSnackbar(SnackbarVisuals.Loading(message))
                    }
                    UserMessage.UserNotFound,
                    UserMessage.ConnectionError -> {
                        state.showSnackbar(SnackbarVisuals.Error(message))
                        viewModel.emit(UserMessageEvent.UserMessageSeen)
                    }
                }
            }
        }
    }

    @Composable
    override fun BottomBar() {
        Surface(shadowElevation = 8.dp) {
            FilledTonalButton(
                onClick = { viewModel.emit(ProgressEvent.NextButtonClicked) },
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(
                        WindowInsets.ime.union(WindowInsets.navigationBars)
                    )
                    .padding(horizontal = 24.dp, vertical = 8.dp)
            ) {
                Text("Next")
            }
        }
    }
}

@Composable
private fun RecipientDataSection(
    recipientData: RecipientData,
    onSignInButtonClick: () -> Unit,
    onUsernameChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onSignInCancel: () -> Unit,
    onSignInConfirm: () -> Unit,
    onNameChange: (String) -> Unit,
    onSurnameChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onCountryChange: (Country) -> Unit,
    countries: List<Country>,
    onPhoneNumberChange: (String) -> Unit,
    onNext: () -> Unit
) {
    if (recipientData.isSignInDialogVisible) {
        SignInDialog(
            user = recipientData.user,
            onUsernameChange = onUsernameChange,
            onPasswordChange = onPasswordChange,
            onConfirm = onSignInConfirm,
            onDismissRequest = onSignInCancel
        )
    }

    Section(
        title = "Recipient data",
        action = {
            if (!recipientData.isSignedIn) {
                TextButton(onClick = onSignInButtonClick) {
                    Text("Sign in")
                }
            }
        },
        modifier = Modifier.padding(contentPadding)
    ) {
        InputField(
            input = recipientData.name,
            onInputChange = onNameChange,
            label = "Name",
            autofocus = true
        )
        InputField(
            input = recipientData.surname,
            onInputChange = onSurnameChange,
            label = "Last Name"
        )
        val phoneNumberFocusRequester = remember { FocusRequester() }
        InputField(
            input = recipientData.email,
            onInputChange = onEmailChange,
            label = "Email",
            keyboardType = KeyboardType.Email,
            onImeAction = phoneNumberFocusRequester::requestFocus
        )
        RecipientPhoneNumberInput(
            countries = countries,
            phoneNumber = recipientData.phoneNumber,
            onCountryChange = onCountryChange,
            onPhoneNumberChange = onPhoneNumberChange,
            phoneNumberFocusRequester = phoneNumberFocusRequester,
            onNext = onNext
        )
    }
}

@Composable
private fun SignInDialog(
    user: UserSignIn,
    onUsernameChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismissRequest: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(dismissOnClickOutside = false)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxHeight(0.6f)
                .clip(MaterialTheme.shapes.medium)
                .background(MaterialTheme.colorScheme.surface)
        ) {
            Column(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)
            ) {
                Box(
                    contentAlignment = Alignment.CenterStart,
                    modifier = Modifier.heightIn(min = ButtonDefaults.MinHeight)
                ) {
                    Text(
                        text = "Sign in",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                InputField(
                    input = user.name,
                    onInputChange = onUsernameChange,
                    label = "Username or email"
                )
                InputField(
                    input = user.password,
                    onInputChange = onPasswordChange,
                    label = "Password",
                    keyboardType = KeyboardType.Password,
                    visualTransformation = PasswordVisualTransformation(),
                    imeAction = ImeAction.Done,
                    onImeAction = onConfirm
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = onDismissRequest) {
                        Text("Cancel")
                    }
                    TextButton(onClick = onConfirm) {
                        Text("Sign in")
                    }
                }
            }
        }
    }
}

@Composable
private fun RecipientPhoneNumberInput(
    countries: List<Country>,
    phoneNumber: PhoneNumber,
    onCountryChange: (Country) -> Unit,
    onPhoneNumberChange: (String) -> Unit,
    phoneNumberFocusRequester: FocusRequester,
    onNext: () -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        CountryCodeSelection(
            countries = countries,
            phoneNumber = phoneNumber,
            onSelectionChange = onCountryChange
        )
        InputField(
            input = phoneNumber.number,
            onInputChange = onPhoneNumberChange,
            label = "Phone No.",
            keyboardType = KeyboardType.Phone,
            onImeAction = onNext,
            modifier = Modifier
                .weight(1f)
                .focusRequester(phoneNumberFocusRequester)
        )
    }
}

@OptIn(FlowPreview::class)
private fun filteredCountries(
    countries: List<Country>,
    searchText: State<String>
) = snapshotFlow { searchText.value }
    .debounce(300)
    .distinctUntilChanged()
    .map {
        countries.filter { country ->
            country.name.contains(it, ignoreCase = true)
                    || country.callingCode.toString().contains(it)
        }
    }
    .conflate()

@Composable
private fun CountryCodeSelection(
    countries: List<Country>,
    phoneNumber: PhoneNumber,
    onSelectionChange: (Country) -> Unit
) {
    val searchText = rememberSaveable { mutableStateOf("") }
    val filteredCountries by filteredCountries(countries, searchText)
        .collectAsState(countries, Dispatchers.Default)

    var isDialogVisible by rememberSaveable { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    fun closeDialog() {
        isDialogVisible = false
        focusManager.moveFocus(FocusDirection.Next)
    }

    val textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace)
    val fontSize = textStyle.fontSize
    InputField(
        input = phoneNumber.countryInput,
        onInputChange = {},
        label = "Ext.",
        textStyle = textStyle,
        readOnly = true,
        onFocusChange = { isFocused ->
            if (!isFocused) return@InputField
            isDialogVisible = true
        },
        modifier = Modifier.width(with(LocalDensity.current) { (fontSize * 7).toDp() })
    )
    if (isDialogVisible) {
        Dialog(onDismissRequest = ::closeDialog) {
            Column(
                modifier = Modifier
                    .fillMaxHeight(0.8f)
                    .clip(MaterialTheme.shapes.medium)
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                CountryCodeList(
                    countries = filteredCountries,
                    selectedCountryCallingCode = phoneNumber.country.callingCode,
                    onSelectionChange = {
                        onSelectionChange(it)
                        closeDialog()
                    },
                    modifier = Modifier.weight(1f)
                )

                val focusRequester = remember { FocusRequester() }
                LaunchedEffect(Unit) {
                    delay(200)
                    coroutineContext.job.invokeOnCompletion {
                        focusRequester.requestFocus()
                    }
                }
                TextField(
                    value = searchText.value,
                    onValueChange = { searchText.value = it },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    colors = TextFieldDefaults.textFieldColors(
                        containerColor = Color.Transparent
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CountryCodeList(
    countries: List<Country>,
    selectedCountryCallingCode: Int,
    onSelectionChange: (Country) -> Unit,
    modifier: Modifier = Modifier
) {
    val firstVisibleItemIndex = when (
        val selectedItemIndex = countries.indexOfFirst {
            it.callingCode == selectedCountryCallingCode
        }
    ) {
        -1 -> 0
        else -> selectedItemIndex
    }
    val state = rememberLazyListState(firstVisibleItemIndex)

    LazyColumn(
        state = state,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .selectableGroup()
            .then(modifier)
    ) {
        items(
            items = countries,
            key = Country::tag
        ) {
            CountryCode(
                country = it,
                isSelected = it.callingCode == selectedCountryCallingCode,
                onClick = {
                    if (it.callingCode == selectedCountryCallingCode) return@CountryCode
                    onSelectionChange(it)
                },
                modifier = Modifier.animateItemPlacement()
            )
        }
    }
}

@Composable
private fun CountryCode(
    country: Country,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        tonalElevation = if (isSelected) 4.dp else 0.dp,
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = isSelected,
                onClick = onClick
            )
            .then(modifier)
    ) {
        Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Text(
                text = buildAnnotatedString {
                    appendInlineContent("")
                    append("${country.unicodeFlag} ${country.name}")
                },
                inlineContent = mapOf(
                    "" to InlineTextContent(
                        placeholder = Placeholder(
                            width = 50.sp,
                            height = 20.sp,
                            placeholderVerticalAlign = PlaceholderVerticalAlign.Center
                        ),
                        children = {
                            Text(
                                text = "+${country.callingCode}",
                                fontFamily = FontFamily.Monospace,
                                style = MaterialTheme.typography.labelLarge.copy(fontSize = 16.sp)
                            )
                        }
                    )
                )
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DeliveryMethodSection(
    isLatestStage: Boolean,
    deliveryDetails: DeliveryDetails,
    onDeliveryMethodSelect: (DeliveryMethod) -> Unit
) {
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    LaunchedEffect(isLatestStage) {
        if (isLatestStage) {
            bringIntoViewRequester.bringIntoView()
        }
    }

    Section(
        title = "Delivery method",
        titleContentPadding = contentPadding,
        modifier = Modifier.bringIntoViewRequester(bringIntoViewRequester)
    ) {
        DeliveryMethodSelect(
            deliveryDetails = deliveryDetails,
            onDeliveryMethodSelect = onDeliveryMethodSelect
        )
        Column(
            contentPadding = contentPadding
        ) {
            TODO()
        }
    }
}

@Composable
private fun DeliveryMethodSelect(
    deliveryDetails: DeliveryDetails,
    onDeliveryMethodSelect: (DeliveryMethod) -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .horizontalScroll(rememberScrollState())
            .selectableGroup()
    ) {
        Spacer(Modifier.width(horizontalPadding))
        deliveryDetails.deliveryMethods.forEach { method ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .clip(MaterialTheme.shapes.small)
                    .background(
                        brush = Brush.linearGradient(
                            listOf(Color(0xffee9ca7), Color(0xffffdde1)),
                            start = remember { Offset(Random.nextFloat(), Random.nextFloat()) },
                            end = remember { Offset(Random.nextFloat(), Random.nextFloat()) }
                        )
                    )
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .widthIn(min = 100.dp)
                    .selectable(deliveryDetails.selectedDeliveryMethod?.id == method.id) {
                        onDeliveryMethodSelect(method)
                    }
            ) {
                Icon(imageVector = method.icon, contentDescription = null)
                Column(
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    ProvideTextStyle(MaterialTheme.typography.bodySmall) {
                        Text(
                            text = method.name,
                            style = MaterialTheme.typography.labelMedium
                        )
                        Text(
                            text = remember {
                                val dateTime =
                                    LocalDateTime.now() + method.estimatedTime.toJavaDuration()
                                val date = dateTime.toLocalDate().run {
                                    when (dateTime.hour > 16) {
                                        true -> plusDays(1)
                                        else -> this
                                    }
                                }
                                date.format(dateFormatter)
                            }
                        )
                        Text(method.cost)
                    }
                }
            }
        }
        Spacer(Modifier.width(horizontalPadding))
    }
}

private val dateFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT)
