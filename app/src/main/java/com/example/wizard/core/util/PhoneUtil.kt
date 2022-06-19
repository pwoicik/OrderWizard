package com.example.wizard.core.util

import com.example.wizard.domain.model.Country
import com.google.i18n.phonenumbers.PhoneNumberUtil

object PhoneUtil {

    private val phoneUtil = PhoneNumberUtil.getInstance()

    fun isValidNumber(number: String, country: Country): Boolean {
        return try {
            phoneUtil.parse(number, country.tag)
            return true
        } catch (_: Exception) {
            false
        }
    }

    val Country.formattedCallingCode: String
        get() = "+$callingCode $unicodeFlag"
}
