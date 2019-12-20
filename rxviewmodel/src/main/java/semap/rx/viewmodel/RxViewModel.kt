package semap.rx.viewmodel

import android.os.Handler
import android.os.Looper
import androidx.lifecycle.ViewModel
import io.reactivex.Observable
import io.reactivex.ObservableSource
import io.reactivex.Observer
import io.reactivex.Scheduler
import io.reactivex.functions.BiFunction
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.ReplaySubject

abstract class RxViewModel<A, S>: ViewModel() {

//    private val TAG = RxViewModel::class.java.simpleName

    /**
     * The outputs (Observables) of the RxViewModel
     */
    val currentState: S
        get() = stateBehaviorSubject.value ?: createInitialState()


    /**
     * Observable of State, it will replay
     */
    val stateObservable: Observable<S>

    /**
     * Observable of loading, it will replay
     */
    val loadingObservable: Observable<Boolean>


    /**
     * Observable of ActionAndState, it does NOT replay
     */
    val actionAndStateObservable: Observable<ActionAndState<A, S>>

    /**
     * Observable of ActionAndState, it does NOT replay
     */
    val actionCompleteObservable: Observable<ActionAndState<A, S>>

    /**
     * Observable of Error, it does NOT replay
     */
    val errorObservable: Observable<Throwable>

    /**
     * Observable of ActionAndState, it does NOT replay
     */
    val actionAndErrorObservable: Observable<ActionAndError<A>>

    /**
     * The inputs (the publishEvent subjects that accepts actions) of the RxViewModel
     */
    private val concurrentActionSubject = PublishSubject.create<A>()
    private val sequentialActionSubject = PublishSubject.create<A>()
    private val switchMapLatestActionSubject = PublishSubject.create<A>()

    /**
     * Internal data
     */
    private val stateBehaviorSubject = BehaviorSubject.create<S>()
    private val errorSubject = PublishSubject.create<ActionAndError<A>>()
    private val loadingCount = ReplaySubject.create<Int>()
    private val actionComplete = PublishSubject.create<ActionAndState<A, S>>()

    init {

        val sequential = sequentialActionSubject.concatMap(this::executeAndCombine)

        val concurrent = concurrentActionSubject.flatMap(this::executeAndCombine)

        val flatMapLatest = switchMapLatestActionSubject.switchMap(this::executeAndCombine)

        actionAndStateObservable = Observable
                .merge(sequential, concurrent, flatMapLatest)
                .share()

        stateObservable = actionAndStateObservable
                .map { it.state }
                .startWith(Observable.fromCallable { createInitialState() })
                .doOnNext { stateBehaviorSubject.onNext(it) }
                .replay(1)
                .autoConnect()

        loadingObservable = loadingCount
                .scan { x, y -> x + y }
                .map { count -> count > 0 }
                .startWith(false)
                .distinctUntilChanged()
                .replay(1)
                .autoConnect()

        actionAndErrorObservable = errorSubject.hide()

        errorObservable = actionAndErrorObservable
                .map { (_, error) -> error }

        actionCompleteObservable = actionComplete.hide()
    }

    /**
     *
     * @return the initial state of the ViewModel
     */
    abstract fun createInitialState(): S

    /**
     * This is the method to tell the RxViewModel "how to change the current state" for a given action.
     * @param action the action needed to be executed
     * @return the StateMapper (how to change the current state)
     */
    abstract fun createObservable(action: A): Observable<StateMapper<S>>?

    /**
     *
     * @param action
     * @return ture if the app needs to show the spinner for the action.
     */
    open fun showSpinner(action: A): Boolean {
        return false
    }

    /**
     * Default scheduler for observables
     * @return
     */
    open fun defaultScheduler(): Scheduler {
        return Schedulers.computation()
    }

    // if the return value is true, it means the error has been handled, and it will not be in the errorSubject
    open fun handleError(action: A?, error: Throwable): Boolean {
        // you can put a breakpoint here so you will get notified whenever there is an error.
        return false
    }

    fun <T> toLiveData(observable: Observable<T>): RxLiveData<T> {
        val observableWithoutError = observable
                .onErrorResumeNext { error: Throwable ->
                    if (!handleError(null, error)) {
                        errorSubject.onNext(ActionAndError(null, error))
                    }
                    Observable.empty<T>()
                }
        return RxLiveData(observableWithoutError)
    }

    /**
     * Execute the action in parallel. The View classes create Actions and call this method to run them in parallel.
     * @param action
     */
    fun executeAction(action: A) {
        if (!concurrentActionSubject.hasObservers()) {
            Handler(Looper.getMainLooper()).post { execute(action, concurrentActionSubject) }
        } else {
            concurrentActionSubject.onNext(action)
        }
    }

    /**
     * Execute the action in sequence. The View classes create Actions and call this method to run them in sequence.
     * @param action
     */
    fun executeActionInSequence(action: A) {
        if (!concurrentActionSubject.hasObservers()) {
            Handler(Looper.getMainLooper()).post { execute(action, sequentialActionSubject) }
        } else {
            sequentialActionSubject.onNext(action)
        }
    }

    // The previous action will be disposed.
    /**
     * Execute the action and dispose the previous action (if not finished).
     * The View classes create Action and call this method to run it. It will dispose the previous
     * action if the previous action is not finished.
     * @param action
     */
    fun executeActionSwitchMap(action: A) {
        if (!switchMapLatestActionSubject.hasObservers()) {
            Handler(Looper.getMainLooper()).post { execute(action, switchMapLatestActionSubject) }
        } else {
            switchMapLatestActionSubject.onNext(action)
        }
    }

    fun getConcurrentActionObserver(): Observer<A> {
        return concurrentActionSubject
    }

    fun getSequentialActionObserver(): Observer<A> {
        return sequentialActionSubject
    }

    fun getSwitchActionObserver(): Observer<A> {
        return switchMapLatestActionSubject
    }

    fun getConcurrentActionLiveDataObserver(): androidx.lifecycle.Observer<A> {
        return androidx.lifecycle.Observer { concurrentActionSubject.onNext(it) }
    }

    fun getSequentialActionLiveDataObserver(): androidx.lifecycle.Observer<A> {
        return androidx.lifecycle.Observer { sequentialActionSubject.onNext(it) }
    }

    fun getSwitchActionLiveDataObserver(): androidx.lifecycle.Observer<A> {
        return androidx.lifecycle.Observer { switchMapLatestActionSubject.onNext(it) }
    }

    fun isLoadingObservable(): Observable<Boolean> {
        return loadingObservable
    }

    fun <T> Observable<T>.asLiveData(): RxLiveData<T> {
        return this.asLiveData(this@RxViewModel)
    }

    fun <T: A> getActionObservable(clazz: Class<T>): Observable<T> {
        return actionAndStateObservable
                .map { it.action }
                .ofType(clazz)
    }

    fun <T: A, R> getActionObservable(clazz: Class<T>, mapper: (S) -> R?): Observable<R> {
        return actionAndStateObservable
                .filter { clazz.isInstance(it.action) }
                .skipNull { mapper.invoke(it.state) }
    }

    fun <T: A> getActionCompleteObservable(clazz: Class<T>): Observable<T> {
        return actionCompleteObservable
                .map { it.action }
                .ofType(clazz)
    }

    fun <T: A, R> getActionCompleteObservable(clazz: Class<T>, mapper: (S) -> R?): Observable<R> {
        return actionCompleteObservable
                .filter { clazz.isInstance(it.action) }
                .skipNull { mapper.invoke(it.state) }
    }

    // A util method to skip the null values.
    protected fun <T> skipNull(obj: T?): Observable<T> {
        return if (obj == null) {
            Observable.empty()
        } else Observable.just(obj)
    }

    // A util method to wrap a nullable object with a Optional
    protected fun <T> toOptional(obj: T): Optional<T> {
        return Optional(obj)
    }

    /**
     * Given an observable, make it showing the "spinner".
     * @param observable
     * @param <T>
     * @return
    </T> */
    protected fun <T> enableLoading(observable: Observable<T>): ObservableSource<T> {
        return observable
                .doOnSubscribe { increaseLoadingCount() }
                .doFinally { decreaseLoadingCount() }
    }

    /**
     * Manually increase the loading count.
     */
    fun increaseLoadingCount() {
        loadingCount.onNext(1)
    }

    /**
     * Manually decrease the loading count.
     */
    fun decreaseLoadingCount() {
        loadingCount.onNext(-1)
    }

    private fun execute(action: A, publishSubject: PublishSubject<A>) {
        if (!publishSubject.hasObservers()) {
            // TODO: Log
        }
        publishSubject.onNext(action)
    }

    private fun executeAndCombine(action: A): Observable<ActionAndState<A, S>> {
        return Observable.combineLatest(Observable.just(action),
                    toStateMapperObservable(action),
                    BiFunction<A, StateMapper<S>, Pair<A, StateMapper<S>>> { a, m -> Pair(a, m) })
                .observeOn(Schedulers.single())     // use Schedulers.single() to avoid race condition
                .flatMap(this::toActionAndStateObservable)
                .doOnComplete {
                    actionComplete.onNext(ActionAndState(action, currentState))
                }
                .doOnSubscribe {
                    if (showSpinner(action)) {
                        loadingCount.onNext(1)
                    }
                }
                .doFinally {
                    if (showSpinner(action)) {
                        loadingCount.onNext(-1)
                    }
                }
                .subscribeOn(defaultScheduler())
    }

    private fun toStateMapperObservable(action: A): Observable<StateMapper<S>> {
        return Observable.just(action)
                .flatMap { a ->
                    val stateMapperObservable: Observable<StateMapper<S>> =
                            createObservable(a) ?: Observable.just(StateMapper { it })

                    stateMapperObservable
                            .defaultIfEmpty(StateMapper { it })
                }
                .onErrorResumeNext { throwable: Throwable ->
                    if (!handleError(action, throwable)) {
                        errorSubject.onNext(ActionAndError(action, throwable))
                    }
                    Observable.empty<StateMapper<S>>()
                }
    }

    private fun toActionAndStateObservable(anm: Pair<A, StateMapper<S>>): Observable<ActionAndState<A, S>> {
        val act = anm.first
        val mapper = anm.second
        return try {
            val newState = mapper.map(currentState)
            Observable.just(ActionAndState(act, newState))
        } catch (throwable: Throwable) {
            if (!handleError(act, throwable)) {
                errorSubject.onNext(ActionAndError(act, throwable))
            }
            Observable.empty()
        }
    }
}