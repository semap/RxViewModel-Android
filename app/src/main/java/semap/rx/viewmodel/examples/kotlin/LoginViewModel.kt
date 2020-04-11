package semap.rx.viewmodel.examples.kotlin

import io.reactivex.Observable
import semap.rx.viewmodel.RxViewModel
import semap.rx.viewmodel.StateMapper
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

    override fun createObservable(action: LoginAction): Observable<StateMapper<LoginState>>? {
        return when (action) {
        is SetUsername -> Observable.just(action)
                .map { it.username }
                .map { username -> StateMapper<LoginState> { it.copy(username = username) } }

        is SetPassword -> Observable.just(action)
                .map { it.password }
                .map { password -> StateMapper<LoginState> { it.copy(password = password) } }

        is Login -> Observable.fromCallable { currentState }
                .flatMap{ loginService.loginToServer(it.username, it.password) }
                .map { _ -> StateMapper<LoginState> { it }}
        }
    }

    override fun showSpinner(action: LoginAction): Boolean {
        return when (action) {
            is Login -> true
            else -> false
        }
    }

    override fun createInitialState(): LoginState =
            LoginState(username = "", password = "")

    private val isFormValidObservable: Observable<Boolean>
        get() = stateObservable
                .map { it.isFormValid }
                .distinctUntilChanged()

}