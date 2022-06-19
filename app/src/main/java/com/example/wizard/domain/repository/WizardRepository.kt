package com.example.wizard.domain.repository

import com.example.wizard.domain.errors.SignInError
import com.example.wizard.domain.model.WizardInit

interface WizardRepository {

    suspend fun initWizard(): Result<WizardInit>

    suspend fun signIn(usernameOrEmail: String, password: String): SignInError?
}
