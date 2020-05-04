package com.example.android.navigationadvancedsample.listscreen

sealed class UsersAction {
    object LoadUsers: UsersAction()
    data class SelectUser(val userId: String): UsersAction()
}