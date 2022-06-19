package com.example.wizard.data

import com.example.wizard.domain.model.Country
import com.example.wizard.domain.model.Dictionaries
import com.example.wizard.domain.model.User
import com.google.i18n.phonenumbers.PhoneNumberUtil
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.seconds

@Singleton
class Init @Inject constructor(
    private val dispatcher: CoroutineDispatcher
) {

    private val _user: MutableStateFlow<User?> = MutableStateFlow(null)
    val user = _user.asStateFlow()

    lateinit var dictionaries: Dictionaries
        private set

    companion object {
        private val phoneUtil = PhoneNumberUtil.getInstance()
        private const val UNICODE_FLAG_OFFSET = 127397

        val countryFromLocale: Country
            get() = Locale.getDefault().run {
                Country(
                    tag = country,
                    name = displayCountry,
                    unicodeFlag = country.toUnicodeFlag(),
                    callingCode = phoneUtil.getCountryCodeForRegion(country)
                )
            }


        private fun String.toUnicodeFlag(): String = buildString {
            this@toUnicodeFlag.forEach {
                appendCodePoint(it.code + UNICODE_FLAG_OFFSET)
            }
        }
    }

    private var fetchJob: Job? = null
    suspend operator fun invoke(): Boolean = withContext(dispatcher) inv@{
        if (fetchJob?.isActive == true) fetchJob?.join()
        fetchJob = coroutineContext.job

        if (::dictionaries.isInitialized) return@inv false

        delay(2.seconds)
        _user.value = null

        val locales = Locale.getAvailableLocales()
        dictionaries = Dictionaries(
            countriesDictionary = phoneUtil.supportedCallingCodes
                .associateBy(phoneUtil::getRegionCodeForCountryCode)
                .filterKeys { it != "001" }
                .mapValues { (tag, callingCode) ->
                    Country(
                        tag = tag,
                        callingCode = callingCode,
                        unicodeFlag = tag.toUnicodeFlag(),
                        name = locales.find { it.country == tag }?.displayCountry ?: tag
                    )
                }
        )
        true
    }

    fun updateUser(user: User) {
        _user.update { user }
    }
}

@Suppress("UNUSED")
val exampleUser = User(
    id = UUID.randomUUID(),
    name = "Lorem",
    surname = "Ipsum",
    email = "lorem@ipsum.com",
    phoneNumber = "11111111",
    country = Init.countryFromLocale
)
