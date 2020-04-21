package semap.rx.viewmodel.examples.kotlin

import androidx.test.espresso.matcher.ViewMatchers.assertThat
import io.reactivex.Observable
import org.hamcrest.CoreMatchers.`is`
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mock
import org.mockito.Mockito
import org.powermock.modules.junit4.PowerMockRunner
import semap.rx.viewmodel.examples.LoginService
import semap.rx.viewmodel.examples.kotlin.LoginAction.*

@RunWith(PowerMockRunner::class)
class LoginViewModelTests {
    @Rule
    val schedulerRule = RxSchedulerRule()

    @Mock
    lateinit var loginService: LoginService

    @Test
    fun setUsername() {
        val viewModel = createViewModel()
        val stateObserver = viewModel.stateObservable.test()
        val errObserver = viewModel.errorObservable.test()

        viewModel.executeInParallel(SetUsername("Jennifer"))

        assertThat(stateObserver.lastValue().username, `is`("Jennifer"))
        assertThat(errObserver.valueCount(), `is`(0))

        stateObserver.dispose()
        errObserver.dispose()
    }

    @Test
    fun setPassword() {
        val viewModel = createViewModel()
        val stateObserver = viewModel.stateObservable.test()
        val errObserver = viewModel.errorObservable.test()

        viewModel.executeInParallel(SetUsername("st@yHome"))

        assertThat(stateObserver.lastValue().username, `is`("st@yHome"))
        assertThat(errObserver.valueCount(), `is`(0))

        stateObserver.dispose()
        errObserver.dispose()
    }


    @Test
    fun isFormValid() {
        val viewModel = createViewModel()
        val isFormValidObserver = viewModel.isFormValid.test()

        assertThat(isFormValidObserver.lastValue(), `is`(false))
        viewModel.executeInParallel(SetUsername("st@yHome"))
        viewModel.executeInParallel(SetPassword("Pa"))
        assertThat(isFormValidObserver.lastValue(), `is`(false))

        viewModel.executeInParallel(SetPassword("Pas"))
        assertThat(isFormValidObserver.lastValue(), `is`(true))

    }


    @Test
    fun loginSuccessfully() {
        val viewModel = createViewModel()
        val mockToken = "token_1872382967"

        Mockito.`when`(loginService.loginToServer(anyString(), anyString()))
                .thenReturn(Observable.just(mockToken))

        val stateObserver = viewModel.stateObservable.test()
        val errObserver = viewModel.errorObservable.test()
        val loadingObserver = viewModel.isLoading.test()
        val loginActionObserver = viewModel.loginAction.test()

        viewModel.executeInParallel(SetUsername("joseph"))

        viewModel.executeInParallel(SetPassword("p@ssWord"))

        viewModel.executeInParallelWithDefer(Login)

        assertThat(loadingObserver.values(), `is`(listOf(false, true, false)))
        assertThat(stateObserver.lastValue().token, `is`(mockToken))
        assertThat(errObserver.valueCount(), `is`(0))
        assertThat(loginActionObserver.valueCount(), `is`(1))

        stateObserver.dispose()
        errObserver.dispose()
        loadingObserver.dispose()
        loginActionObserver.dispose()

    }

    @Test
    fun loginWhenServiceError() {
        val viewModel = createViewModel()

        val errMsg = "Something went wrong."
        Mockito.`when`(loginService.loginToServer(anyString(), anyString()))
                .thenReturn(Observable.error(Exception(errMsg)))

        val stateObserver = viewModel.stateObservable.test()
        val errObserver = viewModel.errorObservable.test()

        viewModel.executeInParallel(SetUsername("joseph"))

        viewModel.executeInParallel(SetPassword("p@ssWord"))

        viewModel.executeInParallelWithDefer(Login)

        assertThat(errObserver.valueCount(), `is`(1))
        assertThat(errObserver.lastValue().message, `is`(errMsg))

        stateObserver.dispose()
        errObserver.dispose()
    }

    private fun createViewModel() = LoginViewModel(loginService)
}