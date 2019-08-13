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

        if ("admin".equals(username) && "admin".equals(password)) {
            return Observable.just("mock_token")  // mock token
                    .delay(3, TimeUnit.SECONDS);  // delay 3 secs to make it like a real API call
        } else if ("error".equals(username) && "error".equals(password)) {
            // exception happens in a unexpected place
            throw new RuntimeException("Something unexpected came up");

        } else {
            // The API returns an exception
            return Observable.error(new LoginException("Wrong username password"));
        }
    }
}
