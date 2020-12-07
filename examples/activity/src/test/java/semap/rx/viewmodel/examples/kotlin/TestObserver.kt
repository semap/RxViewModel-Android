package semap.rx.viewmodel.examples.kotlin

import io.reactivex.rxjava3.observers.TestObserver

fun <T> TestObserver<T>.lastValue(): T = values().last()

fun <T> TestObserver<T>.lastValue(predicate: (T) -> Boolean): T = values().last(predicate)

fun <T> TestObserver<T>.valueCount(): Int = values().size
