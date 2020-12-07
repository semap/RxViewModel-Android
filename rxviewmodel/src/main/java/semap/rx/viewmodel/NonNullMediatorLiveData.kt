package semap.rx.viewmodel

import androidx.lifecycle.LifecycleOwner
import io.reactivex.rxjava3.core.Observable

inline fun <T> RxLiveData<T>.observe(owner: LifecycleOwner, crossinline observer: (t: T) -> Unit) {
    this.observe(owner, androidx.lifecycle.Observer {
        it?.let(observer)
    })
}

inline fun <T> Observable<T>.asLiveData(viewModel: RxViewModel<*,*>): RxLiveData<T> = viewModel.toLiveData(this)


inline fun <T, R> Observable<T>.skipNull(crossinline mapper: (t: T) -> R?): Observable<R> {
    return this.flatMap {
                val nullable = mapper.invoke(it)
                if (nullable == null) Observable.empty() else Observable.just(nullable)
            }
}

inline fun <A>Observable<out A>.execute(viewModel: RxViewModel<A, *>, lifecycleOwner: LifecycleOwner) {
    return asLiveData(viewModel)
            .observe(lifecycleOwner) {
                viewModel.execute(it)
            }
}

inline fun <A>Observable<out A>.execute(viewModel: RxViewModel<A, *>, lifecycleOwner: LifecycleOwner, executionMode: ActionExecutionMode) {
    return asLiveData(viewModel)
            .observe(lifecycleOwner) {
                viewModel.execute(it, executionMode)
            }
}
