package semap.rx.viewmodel;

import androidx.annotation.NonNull;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.observers.TestObserver;

public class RxLiveData<T> extends MutableLiveData<T> {

    private Observable<T> observable;
    private Disposable disposable;

    public RxLiveData(Observable<T> observable) {
        this.observable = observable;
    }

    @Override
    public void removeObserver(@NonNull Observer<? super T> observer) {
        super.removeObserver(observer);
        // dispose the subscription if there is no observers (including active and in-active)
        // there is no race condition here, because it is running in the MainThread
        if (!this.hasObservers()) {
            if (disposable != null) {
                disposable.dispose();
                disposable = null;
            }
        }
    }

    @Override
    public void observe(@NonNull LifecycleOwner owner, @NonNull Observer<? super T> observer) {
        boolean hasObservables = this.hasObservers();
        super.observe(owner, observer);

        if (!hasObservables) {
            // there is no race condition here, because it is running in the MainThread
            subscribeToObservable();
        }
    }

    @Override
    public void observeForever(@NonNull Observer<? super T> observer) {
        boolean hasObservables = this.hasObservers();
        super.observeForever(observer);
        if (!hasObservables) {
            // there is no race condition here, because it is running in the MainThread
            subscribeToObservable();
        }
    }

    private void subscribeToObservable() {
        disposable = observable
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(value -> setValue(value));
    }

    public TestObserver<T> test() {
        return observable.test();
    }
}
