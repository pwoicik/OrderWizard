package com.example.wizard.domain.model

typealias CountryTag = String

data class Dictionaries(
    val countriesDictionary: Map<CountryTag, Country>
)

data class Country(
    val tag: String,
    val name: String,
    val callingCode: Int,
    val unicodeFlag: String
)
