package org.reactive

import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import java.util.concurrent.atomic.AtomicInteger


fun main() {

    fun threadInformation() = "[ThreadId ${Thread.currentThread().id}, ThreadName ${Thread.currentThread().name}]"
    fun printThreadInformation(additionalInformation: String) = println("$additionalInformation ${threadInformation()}")

    fun publishExample() {
        val publisher = Flowable.fromCallable {
            printThreadInformation("Thread 1 inside callable")
            Thread.sleep(1000) //  imitate expensive computation
            printThreadInformation("Thread 2 inside callable")
            "Done without both"
        }
            .subscribeOn(Schedulers.io()) // callable executed on the main thread scheduler for Observable
        //  .observeOn(Schedulers.from()) //subcribed


        // <--- wait for the flow to finish
        printThreadInformation("Thread out side callable")
        publisher.subscribe({
            printThreadInformation("Thread inside subscriber callable")
            println(it)
        }) {
            printThreadInformation("Thread inside subscriber throwable")
            it.printStackTrace()
        }
        /*
            publisher.blockingSubscribe({ // if you need to run on main thread you can use blockingSubscriber
            printThreadInformation("Thread inside subscriber callable")
            println(it)
        }) {
            printThreadInformation("Thread inside subscriber throwable")
            it.printStackTrace()
        }
        */

        Thread.sleep(2000)
    }

    fun notConcurrentFlow() {
        Flowable.range(1, 10)
            .observeOn(Schedulers.computation())
            .map { v: Int ->
                printThreadInformation("map $v * $v = ${v * v}")
                v * v
            }
            .blockingSubscribe { x: Int? -> println(x) }
    }

    fun runInParallel() {
        Flowable.range(1, 10)
            .flatMap { v: Int ->
                printThreadInformation("flat map $v * $v = ${v * v}")
                Flowable.just(v)
                    .subscribeOn(Schedulers.computation())
                    .map { w: Int ->
                        printThreadInformation("Flowable.just $w * $w = ${w * w}")
                        w * w
                    }
            }
            .blockingSubscribe { x: Int? -> println(x) }
    }

    fun runInParallelUsingParallelFlowable() {
        Flowable.range(1, 10)
            .parallel()
            .runOn(Schedulers.computation())
            .map { v: Int ->
                printThreadInformation("map $v * $v = ${v * v}")
                v * v
            }
            .sequential()
            .blockingSubscribe { x: Int? -> println(x) }
    }

    fun dependantSubFlows() {
        data class Demand(val id: Int)
        data class Inventory(val id: Int) {
            fun getInventoryAsync(inventoryId: Int) = Flowable.range(10 * inventoryId, 2).map { Demand(it) }.subscribeOn(Schedulers.io())

        }


        val inventorySource: Flowable<Inventory> = Flowable.range(1, 10)
            .subscribeOn(Schedulers.io())
            .map {

                Inventory(it)
            }

        inventorySource
            .flatMap { inventoryItem ->
                    inventoryItem.getInventoryAsync(inventoryItem.id).map { demand -> "Item $inventoryItem has demand $demand" }
            }.blockingSubscribe { println(it) }

    }

    fun continuations() {
        class Service {
            fun apiCall(): Flowable<Int> = Flowable.range(1, 10)
            fun anotherApiCall(i: Int): Flowable<Int> = Flowable.range(i * 10, 2)

            fun finalCall(i: Int): Flowable<Int> = Flowable.range(i * 100, 3)

        }

        val service = Service()
        service.apiCall()
            .flatMap { value -> service.anotherApiCall(value) }
            .flatMap { next -> service.finalCall(next) }
            .blockingSubscribe { println(it)  }
    }

    fun nonDependant() {
        //Non-dependant on https://github.com/ReactiveX/RxJava#Non-dependent
        /*
        Observable continued = sourceObservable.flatMapSingle(ignored -> someSingleSource)
            continued.map(v -> v.toString())
            .subscribe(System.out::println, Throwable::printStackTrace);

        sourceObservable
            .ignoreElements()           // returns Completable
            .andThen(someSingleSource)
            .map(v -> v.toString())
         */

    }

    fun deferredDependant() {
        val count = AtomicInteger()

        Observable.range(1, 10)
            .doOnNext { _ -> count.incrementAndGet() }
            .ignoreElements()
            .andThen(Single.just(count.get()))
            .subscribe(System.out::println) //This prints zero because Single.just(count.get()) is evaluated in assembly time

        val count2 = AtomicInteger()

        Observable.range(1, 10)
            .doOnNext { _: Int? -> count2.incrementAndGet() }
            .ignoreElements()
            .andThen(Single.defer {
                Single.just(
                    count2.get()
                )
            })
            .subscribe { x: Int? -> println(x) }

        val count3 = AtomicInteger()

        Observable.range(1, 10)
            .doOnNext { _: Int? -> count3.incrementAndGet() }
            .ignoreElements()
            .andThen(Single.fromCallable { count3.get() })
            .subscribe { x: Int? -> println(x) }
    }

    fun customObservable() {
        fun createCustomObservable() = Observable.create { subscriber ->
            printThreadInformation("running observable")
            for (i in 1..50) {
                subscriber.onNext("value $i")
            }

            subscriber.onComplete()
        }

        fun createCustomAsyncObservable() = Observable.create { subscriber ->
            printThreadInformation("running observable")
            for (i in 1..50) {
                subscriber.onNext("value $i")
            }

            subscriber.onComplete()
        }.subscribeOn(Schedulers.io())

        val asyncObservable = createCustomAsyncObservable()
        asyncObservable.subscribe(::println)
        println("running")
        Thread.sleep(2000)
        val observable = createCustomObservable()
        observable.subscribe(::println)

    }


    publishExample()
    notConcurrentFlow()
    runInParallel()
    runInParallelUsingParallelFlowable()
    dependantSubFlows()
    continuations()
    nonDependant()
    deferredDependant()
    customObservable()

}

