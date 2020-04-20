package semap.rx.viewmodel

import android.os.Handler
import android.os.Looper
import androidx.lifecycle.ViewModel
import io.reactivex.*
import io.reactivex.functions.BiFunction
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.ReplaySubject
import io.reactivex.subjects.Subject

abstract class RxViewModel<A, S>: ViewModel() {

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
    val actionOnNextObservable: Observable<ActionAndState<A, S>>

    /**
     * Observable of ActionAndState, it does NOT replay
     */
    val actionOnCompleteObservable: Observable<ActionAndState<A, S>>

    /**
     * Observable of Error, it does NOT replay
     */
    val errorObservable: Observable<Throwable>

    /**
     * Observable of ActionAndState, it does NOT replay
     */
    val actionErrorObservable: Observable<ActionAndError<A>>

    /**
     * The inputs (the publishEvent subjects that accepts actions) of the RxViewModel
     */
    private val concatEagerActionSubject = PublishSubject.create<DeferrableAction>().toSerialized()
    private val concurrentActionSubject = PublishSubject.create<A>().toSerialized()
    private val sequentialActionSubject = PublishSubject.create<List<A>>().toSerialized()
    private val switchMapLatestActionSubject = PublishSubject.create<A>().toSerialized()
    private val deferredActionSubject = PublishSubject.create<A>().toSerialized()

    /**
     * Internal data
     */
    private val stateBehaviorSubject = BehaviorSubject.create<S>()
    private val errorSubject = PublishSubject.create<ActionAndError<A>>().toSerialized()
    private val loadingCount = ReplaySubject.create<Int>().toSerialized()
    private val wrapperObservable: Observable<Wrapper>

    init {
        val sequential = sequentialActionSubject
                .concatMap { list ->
                    Observable.fromIterable(list)
                        .concatMap { executeAndCombine(it, true) }
                        .onErrorResumeNext { _: Throwable ->
                            Observable.empty<Wrapper>()
                        }
                }
        val concurrent = concurrentActionSubject.flatMap { executeAndCombine(it) }
        val concatEager = concatEagerActionSubject.concatMapEager { executeAndCombine(it.action, throwError = false, deferredAction = it.isDeferred) }
        val flatMapLatest = switchMapLatestActionSubject.switchMap { executeAndCombine(it) }
        val deferred = deferredActionSubject.concatMapEager { executeAndCombine(it) }

        this.wrapperObservable = Observable
                .merge(listOf(sequential, concatEager, concurrent, flatMapLatest, deferred))
                .share()

        actionOnCompleteObservable = this.wrapperObservable
                .filter { it.isComplete }
                .map { it.actionAndState }

        this.actionOnNextObservable = this.wrapperObservable
                .filter { !it.isComplete }
                .map { it.actionAndState }

        this.stateObservable = actionOnNextObservable
                .map { it.state }
                .startWith(Observable.fromCallable { createInitialState() })
                .replay(1)
                .autoConnect()

        this.loadingObservable = loadingCount
                .scan { x, y -> x + y }
                .map { count -> count > 0 }
                .startWith(false)
                .distinctUntilChanged()
                .replay(1)
                .autoConnect()

        this.actionErrorObservable = errorSubject.hide()

        this.errorObservable = this.actionErrorObservable
                .map { (_, error) -> error }
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
    abstract fun createActionObservable(action: A): Observable<StateMapper<S>>?

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
     * Execute the action in parallel. And keep the order of the stateMappers be the same with the actions.
     * Notice: The action's stateMapperObservable will not complete if the stateMapperObservable from the previous
     * action does not complete.
     * The View classes create Actions and call this method to run them in parallel.
     * @param action
     */
    fun executeInParallel(action: A) {
        executeInParallel(action, stateMapInOrder = true, deferred = false)
    }

    /**
     * Postpone the execution of the action until all the stateMappers of the previous actions in parallel complete.
     * @param action
     */
    fun executeInParallelWithDefer(action: A) {
        executeInParallel(action, stateMapInOrder = true, deferred = true)
    }

    /**
     * Execute the action in parallel. The order of the stateMapper is not guaranteed.
     */
    fun executeInParallelWithoutOrder(action: A) {
        executeInParallel(action, stateMapInOrder = false, deferred = false)
    }
    /**
     * Execute the action in parallel. And keep the order of the stateMapper. (concatMapEager)
     * The View classes create Actions and call this method to run them in parallel.
     * @param action
     * @param stateMapInOrder true if keep the order of the stateMapper the same with the actions.
     */
    private fun executeInParallel(action: A, stateMapInOrder: Boolean, deferred: Boolean) {

        if (stateMapInOrder) {
            if (!concatEagerActionSubject.hasObservers()) {
                Handler(Looper.getMainLooper()).post { execute(DeferrableAction(action, deferred), concatEagerActionSubject) }
            } else {
                concatEagerActionSubject.onNext(DeferrableAction(action, deferred))
            }
        } else {
            if (!concurrentActionSubject.hasObservers()) {
                Handler(Looper.getMainLooper()).post { execute(action, concurrentActionSubject) }
            } else {
                concurrentActionSubject.onNext(action)
            }
        }
    }

    /**
     * Execute the action in sequence. The View classes create Actions and call this method to run them in sequence.
     * @param action
     */
    fun executeInSequence(action: A) {
        if (!concatEagerActionSubject.hasObservers()) {
            Handler(Looper.getMainLooper()).post { execute(listOf(action), sequentialActionSubject) }
        } else {
            sequentialActionSubject.onNext(listOf(action))
        }
    }

    /**
     * Execute the action in sequence. The View classes create Actions and call this method to run them in sequence.
     * @param actions
     */
    fun executeInSequence(actions: List<A>) {
        if (!concatEagerActionSubject.hasObservers()) {
            Handler(Looper.getMainLooper()).post { execute(actions, this.sequentialActionSubject) }
        } else {
            sequentialActionSubject.onNext(actions)
        }
    }

    /**
     * Execute the action and dispose the previous action (if not finished).
     * The View classes create Action and call this method to run it. It will dispose the previous
     * action if the previous action is not finished.
     * @param action
     */
    fun executeWithSwitchMap(action: A) {
        if (!switchMapLatestActionSubject.hasObservers()) {
            Handler(Looper.getMainLooper()).post { execute(action, switchMapLatestActionSubject) }
        } else {
            switchMapLatestActionSubject.onNext(action)
        }
    }

    fun isLoadingObservable(): Observable<Boolean> {
        return loadingObservable
    }

    fun <T> Observable<T>.asLiveData(): RxLiveData<T> {
        return this.asLiveData(this@RxViewModel)
    }

    fun <T: A> actionOnNextObservable(clazz: Class<T>): Observable<T> {
        return this.actionOnNextObservable
                .map { it.action }
                .ofType(clazz)
    }

    fun <T: A, R> actionOnNextObservable(clazz: Class<T>, mapper: (S) -> R?): Observable<R> {
        return this.actionOnNextObservable
                .filter { clazz.isInstance(it.action) }
                .skipNull { mapper.invoke(it.state) }
    }

    fun <T: A> actionOnCompleteObservable(clazz: Class<T>): Observable<T> {
        return this.actionOnCompleteObservable
                .map { it.action }
                .ofType(clazz)
    }

    fun <T: A, R> actionOnCompleteObservable(clazz: Class<T>, mapper: (S) -> R?): Observable<R> {
        return this.actionOnCompleteObservable
                .filter { clazz.isInstance(it.action) }
                .skipNull { mapper.invoke(it.state) }
    }

    fun <T: A> actionErrorObservable(clazz: Class<T>): Observable<Throwable> {
        return this.actionErrorObservable
                .filter { clazz.isInstance(it.action) }
                .map { it.error }
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

    private fun <O> execute(action: O, subject: Subject<O>) {
        if (!subject.hasObservers()) {
            // TODO: Log
        }
        subject.onNext(action)
    }

    private fun executeAndCombine(action: A, throwError: Boolean = false, deferredAction: Boolean = false): Observable<Wrapper> {
        return Observable.combineLatest(
                    Observable.just(action),
                    if (deferredAction) Observable.empty() else toStateMapperObservable(action),
                    BiFunction<A, StateMapper<S>, Pair<A, StateMapper<S>>> { a, m -> Pair(a, m) })
                .observeOn(Schedulers.single())     // use Schedulers.single() to avoid race condition
                .map { ActionAndState(it.first, it.second.map(currentState)) }
                .doOnNext { stateBehaviorSubject.onNext(it.state) }
                .observeOn(defaultScheduler())
                .map { Wrapper(it) }
                .materialize()
                .doOnNext { if (deferredAction && it.isOnComplete) deferredActionSubject.onNext(action) }
                .filter { !deferredAction }
                .doOnNext {
                    it.error?.let {throwable ->
                        if (!handleError(action, throwable)) {
                            errorSubject.onNext(ActionAndError(action, throwable))
                        }
                    }
                }
                .flatMap {
                    if (it.isOnComplete) {
                        Observable.just(
                                Notification.createOnNext(Wrapper(ActionAndState(action, currentState), true)),
                                Notification.createOnComplete<Wrapper>()
                        )
                    }  else {
                        Observable.just(it)
                    }
                }
                .dematerialize { it }
                .onErrorResumeNext { throwable: Throwable ->
                    if (throwError)
                        throw throwable
                    else
                        Observable.empty<Wrapper>()
                }
                .doOnSubscribe {
                    if (!deferredAction && showSpinner(action)) {
                        loadingCount.onNext(1)
                    }
                }
                .doFinally {
                    if (!deferredAction && showSpinner(action)) {
                        loadingCount.onNext(-1)
                    }
                }
                .subscribeOn(defaultScheduler())
    }

    private fun toStateMapperObservable(action: A): Observable<StateMapper<S>> {
        return Observable.just(action)
                .flatMap { a ->
                    val stateMapperObservable: Observable<StateMapper<S>> =
                            createActionObservable(a) ?: Observable.just(StateMapper { it })

                    stateMapperObservable
                            .defaultIfEmpty(StateMapper { it })
                }
    }

    private inner class Wrapper(val actionAndState: ActionAndState<A, S>, val isComplete: Boolean = false)
    private inner class DeferrableAction(val action: A, val isDeferred: Boolean = false)
}