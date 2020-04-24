package semap.rx.viewmodel

import android.os.Handler
import android.os.Looper
import androidx.lifecycle.ViewModel
import io.reactivex.Notification
import io.reactivex.Observable
import io.reactivex.Scheduler
import io.reactivex.functions.BiFunction
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.ReplaySubject
import io.reactivex.subjects.Subject
import semap.rx.viewmodel.ActionExecutionMode.*

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
    private val wrapperObservable: Observable<AnsWrapper>

    init {
        val sequential = sequentialActionSubject
                .concatMap { list ->
                    Observable.fromIterable(list)
                        .concatMap { executeAndCombine(it, true) }
                        .onErrorResumeNext { _: Throwable ->
                            Observable.empty<AnsWrapper>()
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
     * @return the Reducer (how to change the current state)
     */
    abstract fun createReducerObservable(action: A): Observable<Reducer<S>>?

    /**
     *
     * @param action
     * @return ture if the app needs to show the spinner for the action.
     */
    open fun showSpinner(action: A): Boolean {
        return false
    }

    open fun executeMode(action: A): ActionExecutionMode {
        return ParallelDefault
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
     * Execute an action.
     * @param action
     */
    fun execute(action: A) {
        execute(action, executeMode(action))
    }

    /**
     * Execute an action with a specific executionMode.
     * @param action
     * @param mode
     */
    fun execute(action: A, mode: ActionExecutionMode) {
        when(mode) {
            Parallel ->  enqueueInParallel(action, stateMapInOrder = false, deferred = false)
            ParallelDefer -> enqueueInParallel(action, stateMapInOrder = true, deferred = true)
            Sequence -> enqueueInSequence(action)
            SwitchMap -> enqueueWithSwitchMap(action)
            else -> enqueueInParallel(action, stateMapInOrder = true, deferred = false)
        }
    }

    /**
     * Execute the actions in sequence. The View classes create Actions and call this method to run them in sequence.
     * If an action fails, the rest of the actions will not be executed.
     * @param actions
     */
    fun executeInSequence(vararg actions: A) {
        if (!concatEagerActionSubject.hasObservers()) {
            Handler(Looper.getMainLooper()).post { execute(actions.asList(), this.sequentialActionSubject) }
        } else {
            sequentialActionSubject.onNext(actions.asList())
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

    private fun <O> execute(action: O, subject: Subject<O>) {
        if (!subject.hasObservers()) {
            // TODO: Log
        }
        subject.onNext(action)
    }

    private fun executeAndCombine(action: A, throwError: Boolean = false, deferredAction: Boolean = false): Observable<AnsWrapper> {
        return Observable.combineLatest(
                    Observable.just(action),
                    if (deferredAction) Observable.empty() else toReducerObservable(action),
                    BiFunction<A, Reducer<S>, Pair<A, Reducer<S>>> { a, m -> Pair(a, m) })
                .observeOn(Schedulers.single())     // use Schedulers.single() to avoid race condition
                .map { ActionAndState(it.first, it.second.invoke(currentState)) }
                .doOnNext { stateBehaviorSubject.onNext(it.state) }
                .observeOn(defaultScheduler())
                .map { AnsWrapper(it) }
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
                                Notification.createOnNext(AnsWrapper(ActionAndState(action, currentState), true)),
                                Notification.createOnComplete<AnsWrapper>()
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
                        Observable.empty<AnsWrapper>()
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

    private fun enqueueInParallel(action: A, stateMapInOrder: Boolean, deferred: Boolean) {

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

    private fun enqueueInSequence(action: A) {
        if (!concatEagerActionSubject.hasObservers()) {
            Handler(Looper.getMainLooper()).post { execute(listOf(action), sequentialActionSubject) }
        } else {
            sequentialActionSubject.onNext(listOf(action))
        }
    }

    private fun enqueueWithSwitchMap(action: A) {
        if (!switchMapLatestActionSubject.hasObservers()) {
            Handler(Looper.getMainLooper()).post { execute(action, switchMapLatestActionSubject) }
        } else {
            switchMapLatestActionSubject.onNext(action)
        }
    }

    private fun toReducerObservable(action: A): Observable<Reducer<S>> {
        return Observable.just(action)
                .flatMap { a ->
                    val stateMapperObservable: Observable<Reducer<S>> =
                            createReducerObservable(a) ?: Observable.just { s -> s }

                    stateMapperObservable
                            .defaultIfEmpty { s -> s }
                }
    }

    private inner class AnsWrapper(val actionAndState: ActionAndState<A, S>, val isComplete: Boolean = false)
    private inner class DeferrableAction(val action: A, val isDeferred: Boolean = false)
}