package com.example.wizard.ui.wizard

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import arrow.optics.Lens
import arrow.optics.optics
import com.example.wizard.BuildConfig
import com.example.wizard.R
import com.example.wizard.core.ui.util.*
import com.example.wizard.core.util.EmailUtil
import com.example.wizard.core.util.PhoneUtil
import com.example.wizard.core.util.PhoneUtil.formattedCallingCode
import com.example.wizard.core.util.toUnit
import com.example.wizard.data.Init
import com.example.wizard.domain.errors.SignInError
import com.example.wizard.domain.model.Country
import com.example.wizard.domain.model.DeliveryMethod
import com.example.wizard.domain.model.Dictionaries
import com.example.wizard.domain.model.User
import com.example.wizard.domain.repository.WizardRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class WizardViewModel @Inject constructor(
    private val repository: WizardRepository,
    init: Init
) : ViewModel() {

    private val user: StateFlow<User?> = init.user
    private val dictionaries: Dictionaries = init.dictionaries

    private val _state = MutableStateFlow(WizardState())
    val state = _state.asStateFlow()

    val userMessage = state
        .map { it.userMessage }
        .stateIn(viewModelScope, SharingStarted.Eagerly, initialValue = null)

    init {
        if (BuildConfig.DEBUG) {
            _state
                .onEach { Timber.tag("wizard state").d(it.toString()) }
                .launchIn(viewModelScope)
        }

        user.onEach { user ->
            _state.update { state ->
                WizardState.recipientData.modify(state) {
                    when (user) {
                        null -> it.copy(isSignedIn = false)
                        else -> {
                            it.copy(
                                isSignedIn = true,
                                name = user.name.validate(),
                                surname = user.surname.validate(),
                                email = user.email.validate(),
                                phoneNumber = PhoneNumber(
                                    country = user.country,
                                    countryInput = user.country.toValidInput(),
                                    number = user.phoneNumber.validate()
                                )
                            )
                        }
                    }
                }
            }
        }.launchIn(viewModelScope)

        viewModelScope.launch {
            val response = repository.initWizard().getOrElse { return@launch }
            _state.update { state ->
                state.copy(
                    userMessage = null,
                    deliveryDetails = DeliveryDetails(
                        deliveryMethods = response.deliveryMethods
                    ),
                )
            }
        }
    }

    val countries: List<Country> by lazy {
        dictionaries.countriesDictionary.values.sortedBy { it.callingCode }
    }

    fun emit(event: ProgressEvent) {
        Timber.tag("wizard event").d(event.toString())
        when (event) {
            ProgressEvent.NextButtonClicked -> onNextButtonClicked()
        }
    }

    fun emit(event: UserMessageEvent) {
        Timber.tag("wizard event").d(event.toString())
        when (event) {
            UserMessageEvent.UserMessageSeen -> _state.update {
                it.copy(userMessage = null)
            }
        }
    }

    fun emit(event: RecipientDataEvent) {
        Timber.tag("wizard event").d(event.toString())
        when (event) {
            is RecipientDataEvent.NameChanged -> onRecipientNameOrSurnameChanged(
                value = event.name,
                lens = RecipientData.name
            )
            is RecipientDataEvent.SurnameChanged -> onRecipientNameOrSurnameChanged(
                value = event.surname,
                lens = RecipientData.surname
            )
            is RecipientDataEvent.CountryChanged -> onRecipientCountryChanged(event.country)
            is RecipientDataEvent.EmailChanged -> onRecipientEmailChanged(event.email)
            is RecipientDataEvent.PhoneNumberChanged -> onRecipientPhoneNumberChanged(event.phoneNumber)
            is RecipientDataEvent.SignInButtonClicked -> onSignInButtonClicked()
            is UserSignInEvent.NameChanged -> onSignInNameChanged(event.name)
            is UserSignInEvent.PasswordChanged -> onSignInPasswordChanged(event.pwd)
            UserSignInEvent.Canceled -> onSignInCanceled()
            UserSignInEvent.Confirmed -> onSignInConfirmed()
        }
    }

    fun emit(event: DeliveryDetailsEvent) {
        Timber.tag("wizard event").d(event.toString())
        when (event) {
            is DeliveryDetailsEvent.DeliveryMethodSelected -> onDeliveryMethodSelected(event.method)
        }
    }

    private fun onNextButtonClicked() {
        when (state.value.currentStage) {
            WizardStage.RecipientData -> {
                if (validateRecipientData().isValid) {
                    progressStage()
                }
            }
            WizardStage.DeliveryMethod -> TODO()
        }
    }

    private fun progressStage() = WizardState.currentStage.modify(WizardStage::next)

    private fun validateRecipientData() =
        WizardState.recipientData.modifyAndGet(RecipientData::validate)

    private fun onRecipientNameOrSurnameChanged(
        value: String,
        lens: Lens<RecipientData, ValidatedInput>
    ) = (WizardState.recipientData + lens).modify {
        value
            .trimStart()
            .replaceFirstChar(Char::uppercaseChar)
            .validate(it.validator)
    }

    private fun onRecipientEmailChanged(email: String) = WizardState.recipientData.email.modify {
        email
            .trim()
            .lowercase()
            .validate(it.validator)
    }

    private fun onRecipientCountryChanged(country: Country) =
        WizardState.recipientData.phoneNumber.modify {
            it.copy(
                country = country,
                countryInput = country.toValidInput()
            )
        }

    private fun onRecipientPhoneNumberChanged(phoneNumber: String) =
        WizardState.recipientData.phoneNumber.modify {
            it.validate(phoneNumber)
        }

    private fun onSignInButtonClicked() = WizardState.recipientData.isSignInDialogVisible.modify {
        true
    }

    private fun onSignInNameChanged(name: String) = WizardState.recipientData.user.name.modify {
        name.validate(it.validator)
    }

    private fun onSignInPasswordChanged(pwd: String) =
        WizardState.recipientData.user.password.modify {
            pwd.validate(it.validator)
        }

    private fun onSignInCanceled() = WizardState.recipientData.modify {
        it.copy(
            isSignInDialogVisible = false,
            user = UserSignIn()
        )
    }

    private fun onSignInConfirmed(): Unit = viewModelScope.launch {
        val user = validateSignInData()
        if (!user.isValid) return@launch

        _state.update {
            it.copy(
                userMessage = UserMessage.LoadingData,
                recipientData = it.recipientData.copy(
                    isSignInDialogVisible = false
                )
            )
        }

        val error = repository.signIn(user.name.value, user.password.value)
        _state.update {
            it.copy(
                userMessage = when (error) {
                    null -> null
                    SignInError.ConnectionError -> UserMessage.ConnectionError
                    SignInError.UserNotFound -> UserMessage.UserNotFound
                },
                currentStage = when (error) {
                    null -> it.currentStage.next()
                    else -> it.currentStage
                }
            )
        }
    }.toUnit()

    private fun onDeliveryMethodSelected(method: DeliveryMethod) =
        WizardState.deliveryDetails.nullableSelectedDeliveryMethod.modify { method }

    private fun validateSignInData() = WizardState.recipientData.user
        .modifyAndGet(UserSignIn::validate)

    private fun <T> Lens<WizardState, T>.modify(
        map: (focus: T) -> T
    ) = _state.update { this@modify.modify(it, map) }

    private fun <T> Lens<WizardState, T>.modifyAndGet(
        map: (focus: T) -> T
    ) = get(_state.updateAndGet { this@modifyAndGet.modify(it, map) })
}

@Immutable
@optics
data class WizardState(
    val currentStage: WizardStage = WizardStage.RecipientData,
    val userMessage: UserMessage? = UserMessage.Initializing,
    val recipientData: RecipientData = RecipientData(),
    val deliveryDetails: DeliveryDetails = DeliveryDetails()
) {
    companion object
}

enum class WizardStage {

    RecipientData,
    DeliveryMethod
    ;

    fun next(): WizardStage = values()[ordinal + 1]
}

enum class UserMessage(val body: StringRes) {

    Initializing(StringRes(R.string.wizard_initializing)),
    LoadingData(StringRes(R.string.wizard_loading_data)),
    ConnectionError(StringRes(R.string.error_no_connection)),
    UserNotFound(StringRes(R.string.error_signin_user_not_found))
}

@Immutable
@optics
data class RecipientData(
    val isSignedIn: Boolean = false,
    val isSignInDialogVisible: Boolean = false,
    val user: UserSignIn = UserSignIn(),
    val name: ValidatedInput = ValidInput(validator = Validators::requiredFieldValidator),
    val surname: ValidatedInput = ValidInput(validator = Validators::requiredFieldValidator),
    val email: ValidatedInput = ValidInput(validator = Validators::emailValidator),
    val phoneNumber: PhoneNumber = PhoneNumber(),
) {
    companion object

    val isValid: Boolean
        inline get() = user.isValid
                && name.isValid
                && surname.isValid
                && email.isValid
                && phoneNumber.isValid

    fun validate() = copy(
        name = name.validate(),
        surname = surname.validate(),
        email = email.validate(),
        phoneNumber = phoneNumber.validate()
    )
}

@Immutable
@optics
data class UserSignIn(
    val name: ValidatedInput = ValidInput(validator = Validators::requiredFieldValidator),
    val password: ValidatedInput = ValidInput(validator = Validators::passwordValidator)
) {
    companion object

    val isValid: Boolean
        inline get() = name.isValid
                && password.isValid

    fun validate() = copy(
        name = name.validate(),
        password = password.validate()
    )
}

@Immutable
@optics
data class DeliveryDetails(
    val deliveryMethods: List<DeliveryMethod> = emptyList(),
    val selectedDeliveryMethod: DeliveryMethod? = null
) {
    companion object
}

@Immutable
@optics
data class PhoneNumber(
    val country: Country = Init.countryFromLocale,
    val countryInput: ValidatedInput = ValidInput(),
    val number: ValidatedInput = ValidInput()
) {
    companion object

    val isValid: Boolean
        inline get() = number.isValid

    fun validate(number: String = this.number.value): PhoneNumber {
        val errorMessage =
            Validators.requiredFieldValidator(number)
                ?: when {
                    PhoneUtil.isValidNumber(number, country) -> null
                    else -> StringRes(R.string.error_phone_no_invalid)
                }
        return copy(number = ValidatedInput(number, errorMessage))
    }
}

private fun Country.toValidInput() = ValidInput(formattedCallingCode)

private object Validators {

    fun requiredFieldValidator(value: String): StringRes? = when (value.isBlank()) {
        false -> null
        true -> StringRes(R.string.error_field_required)
    }

    fun emailValidator(email: String) =
        requiredFieldValidator(email)
            ?: when (EmailUtil.validateEmail(email)) {
                false -> null
                true -> StringRes(R.string.error_email_invalid)
            }

    fun passwordValidator(pwd: String) =
        requiredFieldValidator(pwd)
            ?: when (!pwd.matches("""[a-zA-Z\d]+""".toRegex())) {
                false -> null
                true -> StringRes(R.string.error_password_illegal_characters)
            }
            ?: when (pwd.length < 8) {
                false -> null
                true -> StringRes(R.string.error_password_too_short)
            }
}

sealed interface WizardEvent

sealed interface ProgressEvent : WizardEvent {

    object NextButtonClicked : ProgressEvent
}

sealed interface UserMessageEvent : WizardEvent {

    object UserMessageSeen : UserMessageEvent
}

sealed interface RecipientDataEvent : WizardEvent {

    data class NameChanged(val name: String) : RecipientDataEvent
    data class SurnameChanged(val surname: String) : RecipientDataEvent
    data class EmailChanged(val email: String) : RecipientDataEvent
    data class CountryChanged(val country: Country) : RecipientDataEvent
    data class PhoneNumberChanged(val phoneNumber: String) : RecipientDataEvent

    object SignInButtonClicked : RecipientDataEvent
}

sealed interface UserSignInEvent : RecipientDataEvent {

    data class NameChanged(val name: String) : UserSignInEvent
    data class PasswordChanged(val pwd: String) : UserSignInEvent

    object Canceled : UserSignInEvent
    object Confirmed : UserSignInEvent
}

sealed interface DeliveryDetailsEvent : WizardEvent {

    data class DeliveryMethodSelected(val method: DeliveryMethod) : DeliveryDetailsEvent
}
