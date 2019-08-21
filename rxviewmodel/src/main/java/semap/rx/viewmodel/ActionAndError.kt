package semap.rx.viewmodel

data class ActionAndError<A>(val action: A?, val error: Throwable)