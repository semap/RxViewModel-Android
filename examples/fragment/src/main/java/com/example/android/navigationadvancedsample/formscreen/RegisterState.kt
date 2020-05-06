package com.example.android.navigationadvancedsample.formscreen

data class RegisterState(
        val name: String = "",
        val email: String = "",
        val password: String = "",
        val registered: Boolean = false)