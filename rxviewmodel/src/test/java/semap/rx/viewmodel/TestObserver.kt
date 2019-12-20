package semap.rx.viewmodel

import io.reactivex.observers.TestObserver

fun <T> TestObserver<T>.lastValue(): T = values().last()

fun <T> TestObserver<T>.lastValue(predicate: (T) -> Boolean): T = values().last(predicate)