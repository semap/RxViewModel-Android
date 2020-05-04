package com.example.android.navigationadvancedsample.formscreen

sealed class RegisterAction {
    data class SetName(val name: String): RegisterAction()
    data class SetEmail(val email: String): RegisterAction()
    data class SetPassword(val password: String): RegisterAction()
    object SignUp: RegisterAction()
}