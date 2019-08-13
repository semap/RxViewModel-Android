package semap.rx.viewmodel.examples.java;

import android.support.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public class LoginAction {
    public static final int SET_USERNAME = 1;
    public static final int SET_PASSWORD = 2;
    public static final int LOGIN = 3;

    @IntDef({SET_USERNAME, SET_PASSWORD, LOGIN})
    @Retention(RetentionPolicy.SOURCE)
    public @interface ActionType{};

    private int type;
    private Object payload;

    public LoginAction(@ActionType int type) {
        this(type, null);
    }

    public LoginAction(@ActionType int type, Object payload) {
        this.type = type;
        this.payload = payload;
    }

    public int getType() {
        return type;
    }

    public <T> T getPayload() {
        return (T)payload;
    }
}
