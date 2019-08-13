package semap.rx.viewmodel.examples.kotlin

data class LoginState(
        val username: String,
        val password: String) {

    val isFormValid: Boolean
        get() = username.length >= 5 && password.length >= 3
}