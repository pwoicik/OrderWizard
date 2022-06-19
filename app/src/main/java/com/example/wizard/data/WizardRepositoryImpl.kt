package com.example.wizard.data

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalPostOffice
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.MarkunreadMailbox
import androidx.compose.material.icons.filled.Store
import com.example.wizard.domain.errors.SignInError
import com.example.wizard.domain.model.DeliveryMethod
import com.example.wizard.domain.model.WizardInit
import com.example.wizard.domain.repository.WizardRepository
import com.example.wizard.util.Globals
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.seconds

@Singleton
class WizardRepositoryImpl @Inject constructor(
    private val dispatcher: CoroutineDispatcher,
    private val init: Init
) : WizardRepository {

    private suspend fun standardDelay() {
        delay(1.seconds)
    }

    private suspend fun <T> standardResponse(data: T): Result<T> {
        standardDelay()
        return when (Globals.toggleFailure) {
            true -> Result.failure(Exception())
            false -> Result.success(data)
        }
    }

    override suspend fun initWizard(): Result<WizardInit> = withContext(dispatcher) {
        standardResponse(
            WizardInit(
                deliveryMethods = listOf(
                    DeliveryMethod(
                        id = 3,
                        icon = Icons.Default.Store,
                        name = "Pickup in person",
                        estimatedTime = 0.days,
                        cost = "0zł"
                    ),
                    DeliveryMethod(
                        id = 0,
                        icon = Icons.Default.LocalShipping,
                        name = "Local Shipping",
                        estimatedTime = 1.days,
                        cost = "25zł"
                    ),
                    DeliveryMethod(
                        id = 1,
                        icon = Icons.Default.LocalPostOffice,
                        name = "Local Post Office",
                        estimatedTime = 3.days,
                        cost = "17zł"
                    ),
                    DeliveryMethod(
                        id = 2,
                        icon = Icons.Default.MarkunreadMailbox,
                        name = "Paczkomat",
                        estimatedTime = 2.days,
                        cost = "8zł"
                    )
                )
            )
        )
    }

    override suspend fun signIn(
        usernameOrEmail: String,
        password: String
    ): SignInError? = withContext(dispatcher) {
        standardDelay()
        when {
            Globals.toggleFailure -> SignInError.ConnectionError
            usernameOrEmail.length != 1 -> SignInError.UserNotFound
            else -> {
                init.updateUser(exampleUser)
                null
            }
        }
    }
}
