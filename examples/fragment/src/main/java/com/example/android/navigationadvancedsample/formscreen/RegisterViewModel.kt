package com.example.android.navigationadvancedsample.formscreen

import android.view.View
import com.example.android.navigationadvancedsample.formscreen.RegisterAction.*
import io.reactivex.Observable
import semap.rx.viewmodel.Reducer
import semap.rx.viewmodel.RxViewModel
import java.util.concurrent.TimeUnit

class RegisterViewModel: RxViewModel<RegisterAction, RegisterState>() {

    val signUpComplete
        get() = actionOnCompleteObservable(SignUp::class.java)
            .asLiveData()

    val loadingIndicatorVisibility
        get() = loadingObservable
                .map { if (it) View.VISIBLE else View.GONE }
                .asLiveData()

    override fun createInitialState(): RegisterState {
        return RegisterState()
    }

    override fun createReducerObservable(action: RegisterAction): Observable<Reducer<RegisterState>>? {
        return when(action) {
            is SetName -> Observable.just { oldState: RegisterState -> oldState.copy(name = action.name) }
            is SetEmail -> Observable.just { oldState: RegisterState -> oldState.copy(email = action.email) }
            is SetPassword -> Observable.just { oldState: RegisterState -> oldState.copy(password = action.password) }
            SignUp -> Observable.just(action)
                    .delay(3, TimeUnit.SECONDS)
                    .map { _ -> {state: RegisterState -> state.copy(registered = true)} }
        }
    }

    override fun showSpinner(action: RegisterAction): Boolean {
        return when(action) {
            SignUp -> true
            else -> false
        }
    }
}
