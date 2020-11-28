package com.example.android.navigationadvancedsample

import io.reactivex.rxjava3.android.plugins.RxAndroidPlugins
import io.reactivex.rxjava3.plugins.RxJavaPlugins
import io.reactivex.rxjava3.schedulers.Schedulers
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

class RxSchedulerRule: TestRule {
    override fun apply(base: Statement?, description: Description?): Statement {

        return object : Statement() {
            @Throws(Throwable::class)
            override fun evaluate() {
                RxJavaPlugins.setComputationSchedulerHandler { scheduler -> Schedulers.trampoline() }
                RxJavaPlugins.setIoSchedulerHandler { scheduler -> Schedulers.trampoline() }
                RxJavaPlugins.setSingleSchedulerHandler { scheduler -> Schedulers.trampoline() }
                RxJavaPlugins.setNewThreadSchedulerHandler { scheduler -> Schedulers.trampoline() }
                RxAndroidPlugins.setMainThreadSchedulerHandler { scheduler -> Schedulers.trampoline() }

                try {
                    base?.evaluate()
                } finally {
                    RxJavaPlugins.reset()
                    RxAndroidPlugins.reset()
                }
            }
        }
    }
}