package semap.rx.viewmodel

import androidx.annotation.MainThread
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.observers.TestObserver

class RxLiveData<T>(val observable: Observable<T>): MutableLiveData<T>() {
    private var disposable: Disposable? = null

    override fun removeObserver(observer: Observer<in T>) {
        super.removeObserver(observer)

        // dispose the subscription if there is no observers (including active and in-active)
        // there is no race condition here, because it is running in the MainThread
        if (!hasObservers()) {
            disposable?.dispose()
            disposable = null
        }
    }

    @MainThread
    override fun observe(owner: LifecycleOwner, observer: Observer<in T>) {
        val hadObservers = this.hasObservers()
        super.observe(owner, observer)
        if (!hadObservers) {
            // there is no race condition here, because it is running in the MainThread
            subscribeToObservable()
        }
    }

    @MainThread
    override fun observeForever(observer: Observer<in T>) {
        val hadObservers = hasObservers()
        super.observeForever(observer)
        if (!hadObservers) { // there is no race condition here, because it is running in the MainThread
            subscribeToObservable()
        }
    }

    private inline fun subscribeToObservable() {
        disposable = observable
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { value: T -> setValue(value) }
    }

    fun test(): TestObserver<T> {
        return observable.test()
    }

}