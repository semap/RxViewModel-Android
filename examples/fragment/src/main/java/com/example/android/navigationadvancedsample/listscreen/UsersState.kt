package com.example.android.navigationadvancedsample.listscreen

data class UsersState(
        val users: List<User> = emptyList(),
        val selected: User? = null
)