package semap.rx.viewmodel;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.ViewModel;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.v4.util.Pair;

import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.Observer;
import io.reactivex.Scheduler;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.ReplaySubject;

/**
 * The base class of the ViewModel.
 * @param <A> action class of the ViewModel
 * @param <S> state class of the ViewModel. It is recommended that the state class is immutable, if a property of the state is changed, a new state instance will be created.
 */
public abstract class RxViewModel<A, S> extends ViewModel {
    private static final String TAG = RxViewModel.class.getSimpleName();
    /**
     * The outputs (Observables) of the RxViewModel
     */
    private S currentState;
    private Observable<S> stateObservable;
    private Observable<ActionAndState<A, S>> actionAndStateObservable;
    private Observable<ActionAndState<A, S>> nextActionAndStateObservable;  // similar to actionAndStateObservable, but it does NOT replay
    private Observable<Throwable> errorObservable;
    private Observable<ActionAndError<A>> actionAndErrorObservable;
    private Observable<Boolean> loadingObservable;

    /**
     * The inputs (the publishEvent subjects that accepts actions) of the RxViewModel
     */
    private PublishSubject<A> concurrentActionSubject = PublishSubject.create();
    private PublishSubject<A> sequentialActionSubject = PublishSubject.create();
    private PublishSubject<A> switchMapLatestActionSubject = PublishSubject.create();

    /**
     * Internal data
     */
    protected PublishSubject<ActionAndError<A>> errorSubject = PublishSubject.create();
    private ReplaySubject<Integer> loadingCount = ReplaySubject.create();

    public RxViewModel() {
        setupRx();
    }

    private void setupRx() {
        Observable<Pair<A, StateMapper<S>>> sequential = sequentialActionSubject
                .concatMap(this::executeAndCombine);

        Observable<Pair<A, StateMapper<S>>> concurrent = concurrentActionSubject
                .flatMap(this::executeAndCombine);

        Observable<Pair<A, StateMapper<S>>> flatMapLatest = switchMapLatestActionSubject
                .switchMap(this::executeAndCombine);

        nextActionAndStateObservable = Observable.merge(sequential, concurrent, flatMapLatest)
                .observeOn(Schedulers.single())     // use trampoline to avoid race condition
                .flatMap(anm -> {
                    A action  = anm.first;
                    StateMapper<S> mapper = anm.second;
                    try {
                        S newState = mapper.map(currentState);
                        currentState = newState;
                        return Observable.just(new ActionAndState<A, S>(action, newState));
                    } catch (Throwable throwable) {
                        if (!handleError(action, throwable)) {
                            errorSubject.onNext(new ActionAndError<>(action, throwable));
                        }
                        return Observable.empty();
                    }
                })
                .observeOn(defaultScheduler())
                .share();

        actionAndStateObservable = nextActionAndStateObservable
                .replay(1)
                .autoConnect();

        currentState = createInitialState();

        stateObservable = actionAndStateObservable
                .map(ActionAndState::getState)
                .startWith(Observable.fromCallable(() -> getCurrentState()))
                .replay(1)
                .autoConnect();

        loadingObservable = loadingCount
                .scan((x, y) -> x + y)
                .map(count -> count > 0)
                .startWith(false)
                .replay(1)
                .autoConnect();

        actionAndErrorObservable = errorSubject.hide();

        errorObservable = actionAndErrorObservable
                .map(event -> event.getError());

    }

    private Observable<Pair<A, StateMapper<S>>> executeAndCombine(@NonNull A action) {
        Observable<StateMapper<S>> nonMapperObservable = Observable.just(action)
                .flatMap(a -> {
                    Observable<StateMapper<S>> stateMapperObservable = createObservable(a);
                    if (stateMapperObservable == null) {
                        stateMapperObservable = Observable.just(s -> s);
                    }

                    return stateMapperObservable
                            .defaultIfEmpty(s -> s)
                            .doOnSubscribe(__ -> {
                                if (showSpinner(a)) {
                                    loadingCount.onNext(1);
                                }
                            })
                            .doFinally(() -> {
                                if (showSpinner(a)) {
                                    loadingCount.onNext(-1);
                                }
                            })
                            .subscribeOn(defaultScheduler());
                })
                .onErrorResumeNext(throwable -> {
                    if (!handleError(action, throwable)) {
                        errorSubject.onNext(new ActionAndError<>(action, throwable));
                    }
                    return Observable.empty();
                });

        return Observable
                .combineLatest(Observable.just(action), nonMapperObservable, (a, m) -> new Pair(a, m));
    }

    /**
     * This is the method to tell the RxViewModel "how to change the current state" for a given action.
     * @param action the action needed to be executed
     * @return the StateMapper (how to change the current state)
     */
    protected abstract Observable<StateMapper<S>> createObservable(A action);

    /**
     *
     * @param action
     * @return ture if the app needs to show the spinner for the action.
     */
    protected abstract boolean showSpinner(A action);

    /**
     *
     * @return the initial state of the ViewModel
     */
    public abstract S createInitialState();

    /**
     * Default scheduler for observables
     * @return
     */
    protected Scheduler defaultScheduler() {
        return Schedulers.computation();
    }

    public <T> LiveData<T> toLiveData(Observable<T> observable) {
        Observable<T> observableWithoutError = observable
                .onErrorResumeNext(error -> {
                    if (!handleError(null, error)) {
                        errorSubject.onNext(new ActionAndError<>(null, error));
                    }
                    return Observable.empty();
                });
        return new RxLiveData<>(observableWithoutError);
    }

    /**
     * Execute the action in parallel. The View classes create Actions and call this method to run them in parallel.
     * @param action
     */
    public void executeAction(A action) {
        if (!concurrentActionSubject.hasObservers()) {
            new Handler(Looper.getMainLooper()).post(() -> execute(action, concurrentActionSubject));
        } else {
            concurrentActionSubject.onNext(action);
        }
    }

    /**
     * Execute the action in sequence. The View classes create Actions and call this method to run them in sequence.
     * @param action
     */
    public void executeActionInSequence(A action) {
        if (!concurrentActionSubject.hasObservers()) {
            new Handler(Looper.getMainLooper()).post(() -> execute(action, sequentialActionSubject));
        } else {
            sequentialActionSubject.onNext(action);
        }
    }

    // The previous action will be disposed.
    /**
     * Execute the action and dispose the previous action (if not finished).
     * The View classes create Action and call this method to run it. It will dispose the previous
     * action if the previous action is not finished.
     * @param action
     */
    public void executeActionSwitchMap(A action) {
        if (!switchMapLatestActionSubject.hasObservers()) {
            new Handler(Looper.getMainLooper()).post(() -> execute(action, switchMapLatestActionSubject));
        } else {
            switchMapLatestActionSubject.onNext(action);
        }
    }

    private void execute(A action, PublishSubject<A> publishSubject) {
        if (!publishSubject.hasObservers()) {
            // TODO Logger.error(TAG, "No observer, the action will not be executed. You need to create at least one observer of stateObservable/actionAndStateObserver or the observables from them.");
        }
        publishSubject.onNext(action);
    }

    // if the return value is true, it means the error has been handled, and it will not be in the errorSubject
    public boolean handleError(A action, Throwable error)  {
        // TODO Logger.error BBLogger.error(TAG, "Error in ViewModel " + error.getMessage());
        if (error instanceof Exception) {
            // TODO Logger.error BBLogger.error(TAG, (Exception)error);
        }
        // you can put a breakpoint here so you will get notified whenever there is an error.
        return false;
    }

    public S getCurrentState() {
        return currentState;
    }

    public Observable<S> getStateObservable() {
        return stateObservable;
    }

    public Observable<ActionAndState<A, S>> getActionAndStateObservable() {
        return getActionAndStateObservable(true);
    }

    public Observable<ActionAndState<A, S>> getActionAndStateObservable(boolean skipCurrentValue) {
        return skipCurrentValue ? nextActionAndStateObservable : actionAndStateObservable;
    }

    public Observable<ActionAndError<A>> getActionAndErrorObservable() {
        return actionAndErrorObservable;
    }

    public Observer<A> getConcurrentActionObserver() {
        return concurrentActionSubject;
    }

    public android.arch.lifecycle.Observer<A> getConcurrentActionLiveDataObserver() {
        return action -> { getConcurrentActionObserver().onNext(action);};
    }

    public Observer<A> getSequentialActionObserver() {
        return sequentialActionSubject;
    }

    public android.arch.lifecycle.Observer<A> getSequentialActionLiveDataObserver() {
        return action -> { getSequentialActionObserver().onNext(action);};
    }

    public Observer<A> getSwitchActionObserver() {
        return switchMapLatestActionSubject;
    }

    public android.arch.lifecycle.Observer<A> getSwitchActionLiveDataObserver() {
        return action -> { getSwitchActionObserver().onNext(action);};
    }

    public Observable<Throwable> getErrorObservable() {
        return errorObservable;
    }

    public Observable<Boolean> isLoadingObservable() {
        return loadingObservable;
    }

    // A util method to skip the null values.
    protected <T> Observable<T> skipNull(T object) {
        if (object == null) {
            return Observable.empty();
        }
        return Observable.just(object);
    }

    // A util method to wrap a nullable object with a Optional
    protected <T> Optional<T> toOptional(T object) {
        return new Optional<T>(object);
    }

    /**
     * Given an observable, make it showing the "spinner".
     * @param observable
     * @param <T>
     * @return
     */
    protected <T> ObservableSource<T> enableLoading(Observable<T> observable) {
        return observable
                .doOnSubscribe(__ -> {
                    increaseLoadingCount();
                })
                .doFinally(() -> {
                    decreaseLoadingCount();;
                });
    }

    /**
     * Manually increase the loading count.
     */
    public void increaseLoadingCount() {
        loadingCount.onNext(1);
    }

    /**
     * Manually decrease the loading count.
     */
    public void decreaseLoadingCount() {
        loadingCount.onNext(-1);
    }

}

