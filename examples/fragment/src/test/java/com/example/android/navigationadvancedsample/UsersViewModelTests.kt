package com.example.android.navigationadvancedsample

import android.view.View
import androidx.test.espresso.matcher.ViewMatchers
import com.example.android.navigationadvancedsample.formscreen.RegisterAction
import com.example.android.navigationadvancedsample.listscreen.UsersAction
import com.example.android.navigationadvancedsample.listscreen.UsersViewModel
import org.hamcrest.CoreMatchers.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.powermock.modules.junit4.PowerMockRunner


@RunWith(PowerMockRunner::class)
class UsersViewModelTests {
    @Rule
    val schedulerRule = RxSchedulerRule()

    @Test
    fun init() {
        val viewModel = createViewModel()
        val loading = viewModel.loadingIndicatorVisibility.test()
        val users = viewModel.users.test()
        val errObserver = viewModel.errorObservable.test()

        ViewMatchers.assertThat(loading.values(), `is`(listOf(View.GONE, View.VISIBLE, View.GONE)))

        ViewMatchers.assertThat(users.lastValue().isEmpty(), `is`(false))
        ViewMatchers.assertThat(errObserver.valueCount(), `is`(0))

        loading.dispose()
        users.dispose()
        errObserver.dispose()
    }

    @Test
    fun loadUsers() {
        val viewModel = createViewModel()
        val stateObserver = viewModel.stateObservable.test()
        val errObserver = viewModel.errorObservable.test()

        viewModel.execute(UsersAction.LoadUsers)

        ViewMatchers.assertThat(stateObserver.lastValue().users.isEmpty(), `is`(false))
        ViewMatchers.assertThat(errObserver.valueCount(), `is`(0))

        stateObserver.dispose()
        errObserver.dispose()
    }

    @Test
    fun loadSelectUser() {
        val viewModel = createViewModel()
        val selectedUser = viewModel.selectedUser.test()
        val userSelected = viewModel.userSelected.test()
        val errObserver = viewModel.errorObservable.test()

        val id = viewModel.currentState.users[3].id
        viewModel.execute(UsersAction.SelectUser(id))

        ViewMatchers.assertThat(selectedUser.lastValue().id, `is`(id))
        ViewMatchers.assertThat(userSelected.valueCount(), `is`(1))

        ViewMatchers.assertThat(errObserver.valueCount(), `is`(0))

        selectedUser.dispose()
        userSelected.dispose()
        errObserver.dispose()
    }


    private fun createViewModel() = UsersViewModel()


}