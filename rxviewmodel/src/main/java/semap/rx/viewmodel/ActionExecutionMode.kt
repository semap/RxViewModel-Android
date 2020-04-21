package semap.rx.viewmodel

sealed class ActionExecutionMode {
    /**
     * The default parallel mode.
     *
     * Execute the action in parallel. And keep the order of the stateMappers be the same with the actions.
     * Notice: The action's stateMapperObservable will not complete if the stateMapperObservable from the previous
     * action does not complete.
     */
    object ParallelDefault: ActionExecutionMode()

    /**
     * Execute the action in parallel.
     * The order of the StateMappers is NOT guaranteed to be the same with the order of actions
     */
    object Parallel: ActionExecutionMode()

    /**
     * Postpone the execution of the action until all the stateMappers of the previous actions in parallel complete.
     */
    object ParallelDefer: ActionExecutionMode()

    /**
     * Execute the action in sequence. The View classes create Actions and call this method to run them in sequence.
     */
    object Sequence: ActionExecutionMode()

    /**
     * Execute the action and dispose the previous action (if not finished).
     * The View classes create Action and call this method to run it. It will dispose the previous
     * action if the previous action is not finished.
     */
    object SwitchMap: ActionExecutionMode()
}