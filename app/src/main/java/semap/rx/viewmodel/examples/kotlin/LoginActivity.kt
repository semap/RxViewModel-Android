package semap.rx.viewmodel.examples.kotlin

import android.arch.lifecycle.ViewModelProvider
import android.content.Context
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import com.jakewharton.rxbinding3.view.clicks
import com.jakewharton.rxbinding3.widget.textChanges
import kotlinx.android.synthetic.main.activity_login.*
import semap.rx.R
import semap.rx.viewmodel.asLiveData
import semap.rx.viewmodel.examples.kotlin.LoginAction.*
import semap.rx.viewmodel.observe

class LoginActivity: AppCompatActivity() {

    private val viewModel: LoginViewModel by lazy {
        val viewModelProvider = ViewModelProvider(this, ViewModelProvider.NewInstanceFactory())
        viewModelProvider.get(LoginViewModel::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // bind the LiveData in the ViewModel to the views.
        bindViewModelToView()
        // bind users inputs to the data in the ViewModel
        bindViewToViewModel()
    }

    private fun bindViewToViewModel() {

        signInButton.clicks()
                .map<LoginAction> { Login }
                .doAfterNext { this.closeKeyboard() }
                .asLiveData()
                .observe(this, viewModel.concurrentActionLiveDataObserver)

        email.textChanges()
                .skipInitialValue()
                .map { it.toString() }
                .map<LoginAction> (::SetUsername)
                .asLiveData()
                .observe(this, viewModel.concurrentActionLiveDataObserver)

        password.textChanges()
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
                .observe(this) { showLoginStatus() }
    }

    private fun showProgress(show: Boolean) {
        loginProgress.setVisibility(if (show) View.VISIBLE else View.GONE)
        loginForm.setVisibility(if (show) View.GONE else View.VISIBLE)
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