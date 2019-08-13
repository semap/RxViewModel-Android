package semap.rx.viewmodel

import android.arch.lifecycle.LifecycleOwner
import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MediatorLiveData
import android.arch.lifecycle.Observer
import io.reactivex.Observable

class NonNullMediatorLiveData<T> : MediatorLiveData<T>()

fun <T> LiveData<T>.nonNull(): NonNullMediatorLiveData<T> {
    val mediator: NonNullMediatorLiveData<T> = NonNullMediatorLiveData()
    mediator.addSource(this, Observer { it?.let { mediator.value = it } })
    return mediator
}

fun <T> NonNullMediatorLiveData<T>.observe(owner: LifecycleOwner, observer: (t: T) -> Unit) {
    this.observe(owner, android.arch.lifecycle.Observer {
        it?.let(observer)
    })
}

fun <A, S, T> RxViewModel<A, S>.toNonNullLiveData(observable: Observable<T>): NonNullMediatorLiveData<T> {
    return this.toLiveData(observable).nonNull();
}

fun <T> Observable<T>.asLiveData(viewModel: RxViewModel<*,*>): NonNullMediatorLiveData<T> = viewModel.toLiveData(this).nonNull<T>()
