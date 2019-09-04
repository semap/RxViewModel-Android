package semap.rx.viewmodel.examples.java;

// If you are using Kotlin, use data class
public class LoginState implements Cloneable {
    private String username;
    private String password;

    public String getUsername() {
        return username;
    }

    // return a new instance with the new property, to make the state immutable
    public LoginState setUsername(String username) {
        LoginState clone = (LoginState)this.clone();
        clone.username = username;
        return clone;
    }

    public String getPassword() {
        return password;
    }

    // return a new instance with the new property, to make the state immutable
    public LoginState setPassword(String password) {
        LoginState clone = (LoginState)this.clone();
        clone.password = password;
        return clone;
    }

    public boolean isFormValid() {
        return username != null && username.length() > 4 &&
            password != null && password.length() > 2;
    }

    @Override
    protected Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            return null;
        }
    }

}
