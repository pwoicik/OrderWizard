package com.example.wizard.core.util

object EmailUtil {

    fun validateEmail(email: String) = when {
        ".." in email -> false
        email.matches(emailRegex) -> true
        else -> false
    }

    private val emailRegex = """^[a-z\d][a-z\d.]*[a-z\d]@[a-z\d][a-z\d.]*[a-z\d]\.[a-z\d]{2,}$""".toRegex()
}
