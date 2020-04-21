package semap.rx.viewmodel

import io.reactivex.Observable
import org.hamcrest.CoreMatchers.*
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.powermock.modules.junit4.PowerMockRunner
import java.util.concurrent.TimeUnit

@RunWith(PowerMockRunner::class)
class ViewModelTest {
    @Rule
    val schedulerRule = RxSchedulerRule()

    @Test
    fun testActionOnCompleteObservable() {
        val viewModel = FooViewModel()

        val setFirstNameActionObserver1 = viewModel
                .actionOnCompleteObservable(FooAction.SetFirstName::class.java)
                .test()

        val setFirstNameActionObserver2 = viewModel
                .actionOnCompleteObservable(FooAction.SetFirstName::class.java) { it.firstName }
                .test()

        val firstNameObserver = viewModel
                .stateObservable
                .skipNull { it.firstName }
                .distinctUntilChanged()
                .test()

        viewModel.executeInParallel(FooAction.SetFirstName("John"))

        assertThat(setFirstNameActionObserver1.lastValue(), `is`(FooAction.SetFirstName("John")))

        assertThat(setFirstNameActionObserver2.lastValue(), `is`("John"))

        assertThat(firstNameObserver.lastValue(), `is`("John"))

    }


    @Test
    fun testViewModel() {
        val viewModel = FooViewModel()

        val stateObserver = viewModel.stateObservable.test()
        val errorObserver = viewModel.errorObservable.test()
        val loadingObserver = viewModel.loadingObservable.test()
        val setFirstNameActionOnNextObserver = viewModel
                .actionOnNextObservable(FooAction.SetFirstName::class.java)
                .test()
        val setFirstNameActionOnCompleteObserver = viewModel
                .actionOnCompleteObservable(FooAction.SetFirstName::class.java)
                .test()

        val firstNameChangedObservable =
                viewModel.actionOnNextObservable(FooAction.SetFirstName::class.java) { it.firstName }
                .test()

        val setLastNameActionOnNextObserver = viewModel
                .actionOnCompleteObservable(FooAction.SetLastName::class.java)
                .test()
        val setLastNameActionOnCompleteObserver = viewModel
                .actionOnCompleteObservable(FooAction.SetLastName::class.java)
                .test()

        viewModel.executeInParallel(FooAction.SetFirstName("Kehuan"))
        assertThat(stateObserver.lastValue().firstName, `is`("Kehuan"))
        assertThat(firstNameChangedObservable.lastValue(), `is`("Kehuan"))
        errorObserver.assertEmpty()

        viewModel.executeInParallel(FooAction.SetLastName("Wang"))
        assertThat(stateObserver.lastValue().firstName, not("Wang"))
        assertThat(errorObserver.lastValue().message, `is`("error"))


        viewModel.executeInParallelWithDefer(FooAction.Submit)
        loadingObserver.assertValueCount(3) // false, true, false
        assertThat(loadingObserver.lastValue(), `is`(false))


        setFirstNameActionOnNextObserver.assertValueCount(1)
        setFirstNameActionOnCompleteObserver.assertValueCount(1)

        setLastNameActionOnNextObserver.assertValueCount(0)
        setLastNameActionOnCompleteObserver.assertValueCount(0)

        stateObserver.dispose()
        errorObserver.dispose()
        loadingObserver.dispose()
        setFirstNameActionOnNextObserver.dispose()
        setFirstNameActionOnCompleteObserver.dispose()
        firstNameChangedObservable.dispose()
        setLastNameActionOnNextObserver.dispose()
        setLastNameActionOnCompleteObserver.dispose()
    }

    @Test
    fun sequenceTasks() {
        val viewModel = FooViewModel()

        val actions = listOf(
                FooAction.AddScore(1),
                FooAction.AddScore(3),
                FooAction.SetLastName("Bob"),   // this action will fail
                FooAction.AddScore(5))

        val errorObserver = viewModel.errorObservable.test()
        val addScore5OnNextObserver = viewModel
                .actionOnNextObservable(FooAction.AddScore::class.java)
                .filter { it.num == 5 }
                .test()
        val scoreObserver = viewModel.stateObservable
                .map { it.score }
                .distinctUntilChanged()
                .test()
        val setLastNameErrorObserver = viewModel.actionErrorObservable(FooAction.SetLastName::class.java)
                .test()

        viewModel.executeInSequence(actions)
        assertThat(scoreObserver.lastValue(), `is`(4))
        assertThat(setLastNameErrorObserver.valueCount(), `is`(1))

        errorObserver.dispose()
        addScore5OnNextObserver.dispose()
        scoreObserver.dispose()
        setLastNameErrorObserver.dispose()

    }


}

sealed class FooAction {
    data class SetFirstName(val name: String): FooAction()
    data class SetLastName(val name: String): FooAction()   // This action will fails (on purpose)
    data class AddScore(val num: Int): FooAction()
    object Submit: FooAction()
}

data class FooState(
        val firstName: String? = null,
        val lastName: String? = null,
        val score: Int = 0)

class FooViewModel: RxViewModel<FooAction, FooState>() {
    override fun createInitialState() = FooState()

    override fun createStateMapperObservable(action: FooAction): Observable<StateMapper<FooState>>? {
        return when (action) {
            is FooAction.SetFirstName ->
                    Observable.just(action.name)
                        .map { name -> StateMapper<FooState> { it.copy(firstName = name) } }

            is FooAction.SetLastName ->
                    Observable.just(action.name)
                        .doOnNext { throw Exception("error") }
                        .map { name -> StateMapper<FooState> { it.copy(lastName = name) } }
            FooAction.Submit ->
                Observable.just(action)
                        .delay(100, TimeUnit.MILLISECONDS)
                        .map { StateMapper<FooState> { it } }

            is FooAction.AddScore ->
                Observable.just(action.num)
                        .map { num ->  StateMapper<FooState> { it.copy(score = it.score + num) } }

        }
    }

    override fun showSpinner(action: FooAction): Boolean {
        return when(action) {
            FooAction.Submit -> true
            else -> false
        }
    }
}