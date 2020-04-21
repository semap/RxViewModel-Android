package semap.rx.viewmodel.examples.java;

import org.jetbrains.annotations.NotNull;

import io.reactivex.Observable;
import semap.rx.viewmodel.ActionExecutionMode;
import semap.rx.viewmodel.ActionExecutionMode.ParallelDefer;
import semap.rx.viewmodel.ActionExecutionMode.ParallelDefault;
import semap.rx.viewmodel.RxViewModel;
import semap.rx.viewmodel.StateMapper;
import semap.rx.viewmodel.examples.LoginService;

import static semap.rx.viewmodel.examples.java.LoginAction.LOGIN;
import static semap.rx.viewmodel.examples.java.LoginAction.SET_PASSWORD;
import static semap.rx.viewmodel.examples.java.LoginAction.SET_USERNAME;

public class LoginViewModel extends RxViewModel<LoginAction, LoginState> {
    private LoginService loginService;

    public LoginViewModel() {
        this.loginService = new LoginService();
    }

    @Override
    public Observable<StateMapper<LoginState>> createStateMapperObservable(LoginAction action) {
        switch (action.getType()) {
            case SET_USERNAME:
                return Observable.just(action)
                        .map(LoginAction::<String>getPayload)
                        .map(username -> state -> state.setUsername(username));
            case SET_PASSWORD:
                return Observable.just(action)
                        .map(LoginAction::<String>getPayload)
                        .map(password -> state -> state.setPassword(password));
            case LOGIN:
                return Observable.fromCallable(() -> getCurrentState())
                        .flatMap(state -> loginService.loginToServer(state.getUsername(), state.getPassword()))
                        .map(loginResult -> state -> state);
        }
        return null;
    }

    @Override
    public boolean showSpinner(LoginAction action) {
        switch (action.getType()) {
            case LOGIN:
                return true;
            default:
                return false;
        }
    }

    @NotNull
    @Override
    public ActionExecutionMode executeMode(LoginAction action) {
        switch (action.getType()) {
            case LOGIN:
                return ParallelDefer.INSTANCE;
            default:
                return ParallelDefault.INSTANCE;
        }
    }

    @Override
    public LoginState createInitialState() {
        return new LoginState();
    }

    public Observable<Boolean> getIsFormValidObservable() {
        return getStateObservable()
                .map (state -> state.isFormValid())
                .distinctUntilChanged();
    }

    public Observable<LoginState> getLoginActionObservable() {
        return getActionOnNextObservable()
                .filter(ans -> ans.getAction().getType() == LOGIN)
                .map(ans -> ans.getState());
    }

}
