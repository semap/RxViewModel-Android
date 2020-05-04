package semap.rx.viewmodel.examples.kotlin

sealed class LoginAction {
    data class SetUsername(val username: String): LoginAction()
    data class SetPassword(val password: String): LoginAction()
    object Login: LoginAction()
}