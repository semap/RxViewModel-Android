package semap.rx.viewmodel

import androidx.lifecycle.LifecycleOwner
import io.reactivex.Observable

fun <T> RxLiveData<T>.observe(owner: LifecycleOwner, observer: (t: T) -> Unit) {
    this.observe(owner, androidx.lifecycle.Observer {
        it?.let(observer)
    })
}

fun <T> Observable<T>.asLiveData(viewModel: RxViewModel<*,*>): RxLiveData<T> = viewModel.toLiveData(this)


fun <T, R> Observable<T>.skipNull(mapper: (t: T) -> R?): Observable<R> {
    return this.flatMap {
                val nullable = mapper.invoke(it)
                if (nullable == null) Observable.empty() else Observable.just(nullable)
            }
}
