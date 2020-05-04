package com.example.android.navigationadvancedsample


import android.view.View
import androidx.test.espresso.matcher.ViewMatchers.assertThat
import com.example.android.navigationadvancedsample.formscreen.RegisterAction.*
import com.example.android.navigationadvancedsample.formscreen.RegisterViewModel
import org.hamcrest.CoreMatchers.`is`
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.powermock.modules.junit4.PowerMockRunner

@RunWith(PowerMockRunner::class)
class RegisterViewModelTests {
    @Rule
    val schedulerRule = RxSchedulerRule()

    @Test
    fun setName() {
        val viewModel = createViewModel()
        val stateObserver = viewModel.stateObservable.test()
        val errObserver = viewModel.errorObservable.test()

        viewModel.execute(SetName("Jennifer"))


        assertThat(stateObserver.lastValue().name, `is`("Jennifer"))
        assertThat(errObserver.valueCount(), `is`(0))

        stateObserver.dispose()
        errObserver.dispose()
    }

    @Test
    fun setEmail() {
        val viewModel = createViewModel()
        val stateObserver = viewModel.stateObservable.test()
        val errObserver = viewModel.errorObservable.test()

        viewModel.execute(SetEmail("matt@gmail.com"))


        assertThat(stateObserver.lastValue().email, `is`("matt@gmail.com"))
        assertThat(errObserver.valueCount(), `is`(0))

        stateObserver.dispose()
        errObserver.dispose()
    }

    @Test
    fun setPassword() {
        val viewModel = createViewModel()
        val stateObserver = viewModel.stateObservable.test()
        val errObserver = viewModel.errorObservable.test()

        viewModel.execute(SetPassword("sEcret1234"))


        assertThat(stateObserver.lastValue().password, `is`("sEcret1234"))
        assertThat(errObserver.valueCount(), `is`(0))

        stateObserver.dispose()
        errObserver.dispose()
    }

    @Test
    fun signUp() {
        val viewModel = createViewModel()
        val stateObserver = viewModel.stateObservable.test()
        val errObserver = viewModel.errorObservable.test()
        val loading = viewModel.loadingIndicatorVisibility.test()
        val signUpComplete = viewModel.signUpComplete.test()

        viewModel.execute(SetName("Jennifer"))
        viewModel.execute(SetEmail("matt@gmail.com"))
        viewModel.execute(SetPassword("sEcret1234"))
        viewModel.execute(SignUp)

        assertThat(loading.values(), `is`(listOf(View.GONE, View.VISIBLE, View.GONE)))

        assertThat(signUpComplete.valueCount(), `is`(1))
        assertThat(errObserver.valueCount(), `is`(0))

        loading.dispose()
        signUpComplete.dispose()
        stateObserver.dispose()
        errObserver.dispose()
    }

    private fun createViewModel() = RegisterViewModel()

}