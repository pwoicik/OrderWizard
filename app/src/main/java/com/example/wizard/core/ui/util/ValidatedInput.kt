@file:Suppress("UNUSED")

package com.example.wizard.core.ui.util

typealias ValidInput = ValidatedInput.Valid
typealias InvalidInput = ValidatedInput.Invalid

sealed interface ValidatedInput {

    val value: String
    val validator: Validator<String>

    data class Valid(
        override val value: String = "",
        override val validator: Validator<String> = { null }
    ) : ValidatedInput
    data class Invalid(
        override val value: String,
        val errorMessage: StringRes,
        override val validator: Validator<String> = { null }
    ) : ValidatedInput

    fun validate() = when(val err = validator(value)) {
        null -> Valid(value = value, validator = validator)
        else -> Invalid(value = value, errorMessage = err, validator = validator)
    }

    companion object {
        operator fun invoke(
            value: String = "",
            errorMessage: StringRes? = null,
            validator: Validator<String> = { null }
        ): ValidatedInput = when (errorMessage) {
            null -> Valid(value, validator)
            else -> Invalid(value, errorMessage, validator)
        }
    }
}

fun <T> ValidatedInput.fold(
    ifValid: (ValidInput) -> T,
    ifInvalid: (InvalidInput) -> T
): T = when (this) {
    is ValidInput -> ifValid(this)
    is InvalidInput -> ifInvalid(this)
}

inline val ValidatedInput.isValid: Boolean
    get() = this is ValidInput

inline val ValidatedInput.isInvalid: Boolean
    get() = this is InvalidInput

inline fun <T> ValidatedInput.ifInvalid(
    action: (InvalidInput) -> T
): T? = when (this) {
    is InvalidInput -> action(this)
    is ValidInput -> null
}

fun ValidInput.toInvalid(errorMessage: StringRes) = InvalidInput(value, errorMessage, validator)
fun InvalidInput.toValid() = ValidInput(value, validator)

fun String.validate(
    validator: (String) -> StringRes? = { null }
) = ValidatedInput(this, validator(this), validator)

fun ValidatedInput.validate(
    validator: (String) -> StringRes? = this.validator
) = value.validate(validator)
