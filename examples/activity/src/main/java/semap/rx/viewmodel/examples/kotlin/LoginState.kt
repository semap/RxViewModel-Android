package semap.rx.viewmodel.examples.kotlin

data class LoginState(
        val username: String = "",
        val password: String = "",
        val token: String? = null) {

    val isFormValid: Boolean
        get() = username.length > 4 && password.length > 2
}