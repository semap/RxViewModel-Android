package semap.rx.viewmodel.examples;

import java.util.concurrent.TimeUnit;

import javax.security.auth.login.LoginException;

import io.reactivex.Observable;

/**
 * Mock Login Service
 */
public class LoginService {


    // returns an observable of token
    public Observable<String> loginToServer(String username, String password) {

        if ("error".equals(username) && "error".equals(password)) {
            // exception happens in a unexpected place
            throw new RuntimeException("Something unexpected came up");
        }

        return Observable.just("mock_token")
                .delay(3, TimeUnit.SECONDS)  // delay 3 secs to make it like a real API call
                .doOnNext(__ -> {
                   if (!"admin".equals(username) || !"admin".equals(password)) {
                       throw new LoginException("Wrong username password");
                   }
                });

    }
}
