package semap.rx.viewmodel.examples.java;

import static semap.rx.viewmodel.examples.java.LoginAction.LOGIN;
import static semap.rx.viewmodel.examples.java.LoginAction.SET_PASSWORD;
import static semap.rx.viewmodel.examples.java.LoginAction.SET_USERNAME;

import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import semap.rx.R;
import semap.rx.viewmodel.examples.AbstractTextWatcher;

/**
 * A login screen that offers login via username/password.
 */
public class LoginActivity extends AppCompatActivity {

    private EditText usernameView;
    private EditText passwordView;
    private View progressView;
    private View loginFormView;
    private Button signInButton;

    private LoginViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        usernameView = (EditText) findViewById(R.id.email);
        passwordView = (EditText) findViewById(R.id.password);
        signInButton = (Button) findViewById(R.id.signInButton);
        loginFormView = findViewById(R.id.loginForm);
        progressView = findViewById(R.id.loginProgress);

        viewModel = ViewModelProviders.of(this).get(LoginViewModel.class);

        // bind the LiveData in the ViewModel to the views.
        bindViewModelToView();
        // bind users inputs to the data in the ViewModel
        bindViewToViewModel();
    }

    private void bindViewModelToView() {
        viewModel.toLiveData(viewModel.isLoadingObservable())
                .observe(this, this::showProgress);

        viewModel.toLiveData(viewModel.getIsFormValidObservable())
                .observe(this, this.signInButton::setEnabled);

        viewModel.toLiveData(viewModel.getErrorObservable())
                .observe(this, this::showError);

        viewModel.toLiveData(viewModel.getLoginActionObservable())
                .observe(this, __ -> this.showLoginStatus());
    }

    private void bindViewToViewModel() {

        // You can use use RxBinding to create button clicks observable
        signInButton.setOnClickListener(__ -> {
            closeKeyboard();
            viewModel.executeAction(new LoginAction(LOGIN));
        });

        // You can use use RxBinding to create textChanges observable
        usernameView.addTextChangedListener(new AbstractTextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                viewModel.executeAction(new LoginAction(SET_USERNAME, usernameView.getText().toString()));
            }
        });

        // You can use use RxBinding to create textChanges observable
        passwordView.addTextChangedListener(new AbstractTextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                viewModel.executeAction(new LoginAction(SET_PASSWORD, passwordView.getText().toString()));
            }
        });

    }

    private void showProgress(final boolean show) {
        progressView.setVisibility(show ? View.VISIBLE : View.GONE);
        loginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
    }

    private void showLoginStatus() {
        Toast.makeText(this, R.string.sign_in_successfully, Toast.LENGTH_LONG).show();
    }

    private void showError(Throwable throwable) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.error_login_failed_title)
                .setMessage(throwable.getMessage())
                .setPositiveButton(android.R.string.yes, null)
                .setNegativeButton(android.R.string.no, null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    protected void closeKeyboard() {
        View view = this.getCurrentFocus();
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (view != null && imm != null) {
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

}

