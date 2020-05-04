package semap.rx.viewmodel.examples.kotlin

import io.reactivex.Observable
import semap.rx.viewmodel.ActionExecutionMode
import semap.rx.viewmodel.ActionExecutionMode.*
import semap.rx.viewmodel.RxViewModel
import semap.rx.viewmodel.Reducer
import semap.rx.viewmodel.examples.LoginService
import semap.rx.viewmodel.examples.kotlin.LoginAction.*

class LoginViewModel(private val loginService: LoginService = LoginService()): RxViewModel<LoginAction, LoginState>() {

    // *** Begin of LiveData ***
    val isFormValid by lazy {
        isFormValidObservable
                .asLiveData()
    }

    val isLoading by lazy {
        loadingObservable
                .asLiveData()
    }

    val loginAction
        get() = actionOnCompleteObservable(Login::class.java)
                .asLiveData()

    // *** End of LiveData ***

    override fun createReducerObservable(action: LoginAction): Observable<Reducer<LoginState>>? {
        return when (action) {
        is SetUsername -> Observable.just { oldState -> oldState.copy(username = action.username) }

        is SetPassword -> Observable.just { oldState -> oldState.copy(password = action.password) }

        is Login -> Observable.fromCallable { currentState }
                .flatMap { loginService.loginToServer(it.username, it.password) }
                .map { token -> { oldState: LoginState -> oldState.copy(token = token) } }
        }
    }

    override fun showSpinner(action: LoginAction): Boolean {
        return when (action) {
            is Login -> true
            else -> false
        }
    }

    override fun executeMode(action: LoginAction): ActionExecutionMode {
        return when (action) {
            is Login -> ParallelDefer
            else -> ParallelDefault
        }
    }

    override fun createInitialState(): LoginState =
            LoginState()

    private val isFormValidObservable: Observable<Boolean>
        get() = stateObservable
                .map { it.isFormValid }
                .distinctUntilChanged()

}