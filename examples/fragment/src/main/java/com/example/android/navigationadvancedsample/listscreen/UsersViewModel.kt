package com.example.android.navigationadvancedsample.listscreen

import android.view.View
import com.example.android.navigationadvancedsample.R
import com.example.android.navigationadvancedsample.listscreen.UsersAction.LoadUsers
import com.example.android.navigationadvancedsample.listscreen.UsersAction.SelectUser
import io.reactivex.Observable
import semap.rx.viewmodel.Reducer
import semap.rx.viewmodel.RxViewModel
import semap.rx.viewmodel.skipNull
import java.util.concurrent.TimeUnit

class UsersViewModel: RxViewModel<UsersAction, UsersState>() {
    init {
        execute(LoadUsers)
    }

    val loadingIndicatorVisibility
        get() = loadingObservable
                .map { if (it) View.VISIBLE else View.GONE }
                .asLiveData()

    val users
        get() = stateObservable
                .map { it.users }
                .distinctUntilChanged()
                .asLiveData()

    val userSelected
        get() = actionOnCompleteObservable(SelectUser::class.java) { it.selected }
                .asLiveData()

    val selectedUser
        get() = stateObservable
                .skipNull { it.selected }
                .distinctUntilChanged()
                .asLiveData()

    override fun createInitialState(): UsersState {
        return UsersState()
    }

    override fun createReducerObservable(action: UsersAction): Observable<Reducer<UsersState>>? {
        return when(action) {
            LoadUsers -> loadUsers()
                    .delay(2, TimeUnit.SECONDS)
                    .map { users -> {state: UsersState -> state.copy(users = users)} }

            is SelectUser -> Observable.fromCallable { currentState.users }
                    .skipNull { users -> users.firstOrNull { it.id == action.userId} }
                    .map { user -> { state: UsersState -> state.copy(selected = user) } }
        }
    }

    override fun showSpinner(action: UsersAction): Boolean {
        return when(action) {
            is LoadUsers -> true
            else -> false
        }
    }

    private fun loadUsers(): Observable<List<User>> {
        return Observable.fromCallable { listOf(
                User("100", "John", 20000, 10, 12, R.drawable.avatar_1_raster),
                User("101", "Riz", 77200, 2, 2, R.drawable.avatar_2_raster),
                User("102", "Sunny", 98400, 9, 2008, R.drawable.avatar_3_raster),
                User("103", "Liv", 88240, 3, 308, R.drawable.avatar_4_raster),
                User("104", "Adrian", 87999, 8, 100, R.drawable.avatar_5_raster),
                User("105", "Chris", 7777, 11, 90, R.drawable.avatar_6_raster),
                User("106", "Leo", 4545, 7, 9993, R.drawable.avatar_1_raster),
                User("107", "Ben", 200, 5, 13, R.drawable.avatar_2_raster),
                User("108", "Joseph", 99932, 4, 3112, R.drawable.avatar_3_raster),
                User("109", "Angel", 3009, 1, 38, R.drawable.avatar_4_raster),
                User("110", "Luke", 887622, 6, 200, R.drawable.avatar_5_raster),
                User("111", "Bob", 46600, 12, 766, R.drawable.avatar_6_raster)).shuffled() }
    }
}