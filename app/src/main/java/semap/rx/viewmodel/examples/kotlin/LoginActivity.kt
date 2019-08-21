package semap.rx.viewmodel.examples.kotlin

import android.arch.lifecycle.ViewModelProvider
import android.content.Context
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import com.jakewharton.rxbinding3.view.clicks
import com.jakewharton.rxbinding3.widget.textChanges
import semap.rx.R
import semap.rx.viewmodel.asLiveData
import semap.rx.viewmodel.examples.kotlin.LoginAction.*
import semap.rx.viewmodel.observe
import semap.rx.viewmodel.toNonNullLiveData

class LoginActivity: AppCompatActivity() {

    private lateinit var usernameView: EditText
    private lateinit var passwordView: EditText
    private lateinit var progressView: View
    private lateinit var loginFormView: View
    private lateinit var signInButton: Button

    private val viewModel: LoginViewModel by lazy {
        val viewModelProvider = ViewModelProvider(this, ViewModelProvider.NewInstanceFactory())
        viewModelProvider.get(LoginViewModel::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        usernameView = findViewById<View>(R.id.email) as EditText
        passwordView = findViewById<View>(R.id.password) as EditText
        signInButton = findViewById<View>(R.id.email_sign_in_button) as Button
        loginFormView = findViewById(R.id.login_form)
        progressView = findViewById(R.id.login_progress)

        // bind the LiveData in the ViewModel to the views.
        bindViewModelToView()
        // bind users inputs to the data in the ViewModel
        bindViewToViewModel()
    }

    private fun bindViewToViewModel() {

        signInButton.clicks()
                .map<LoginAction> { Login }
                .doAfterNext { _ -> this.closeKeyboard()}
                .asLiveData()
                .observe(this, viewModel.concurrentActionLiveDataObserver)

        usernameView.textChanges()
                .skipInitialValue()
                .map { it.toString() }
                .map<LoginAction> (::SetUsername)
                .asLiveData()
                .observe(this, viewModel.concurrentActionLiveDataObserver)

        passwordView.textChanges()
                .skipInitialValue()
                .map { it.toString() }
                .map<LoginAction> (::SetPassword)
                .asLiveData()
                .observe(this, viewModel.concurrentActionLiveDataObserver)

    }

    private fun bindViewModelToView() {

        viewModel.isLoadingObservable
                .asLiveData(viewModel)
                .observe(this, ::showProgress)

        viewModel.isFormValidObservable
                .asLiveData(viewModel)
                .observe(this, signInButton::setEnabled)

        viewModel.errorObservable
                .asLiveData(viewModel)
                .observe(this, ::showError)

        viewModel.loginActionObservable
                .asLiveData(viewModel)
                .observe(this) { showLoginStatus()}
    }

    private fun showProgress(show: Boolean) {
        progressView.setVisibility(if (show) View.VISIBLE else View.GONE)
        loginFormView.setVisibility(if (show) View.GONE else View.VISIBLE)
    }

    private fun showLoginStatus() {
        Toast.makeText(this, R.string.sign_in_successfully, Toast.LENGTH_LONG).show()
    }

    private fun showError(throwable: Throwable) {
        AlertDialog.Builder(this)
                .setTitle(R.string.error_login_failed_title)
                .setMessage(throwable.message)
                .setPositiveButton(android.R.string.yes, null)
                .setNegativeButton(android.R.string.no, null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show()
    }

    protected fun closeKeyboard() {
        val view = this.currentFocus
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager?
        if (view != null && imm != null) {
            imm.hideSoftInputFromWindow(view.windowToken, 0)
        }
    }
}