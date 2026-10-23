package com.marveloushost.betterrank.viewModel

import androidx.annotation.MainThread
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import java.util.concurrent.atomic.AtomicBoolean

/**
 * A lifecycle-aware observable that sends only new updates after subscription, used for events like
 * navigation and Snackbar messages.
 *
 * This avoids a common problem with LiveData where updates can be emitted after configuration
 * changes (e.g. rotation) if the LiveData has a sticky value.
 *
 * Note: Only one observer is supported.
 */
class SingleLiveEvent<T> : MutableLiveData<T>() {

    private val pending = AtomicBoolean(false)

    @MainThread
    override fun observe(owner: LifecycleOwner, observer: Observer<in T>) {
        // Optional: warn if multiple observers
        if (hasActiveObservers()) {
//            android.util.Log.w("SingleLiveEvent", "Multiple observers registered but only one will be notified.")
        }

        super.observe(owner) { t ->
            if (pending.compareAndSet(true, false)) {
                observer.onChanged(t)
            }
        }
    }

    @MainThread
    override fun setValue(value: T?) {
        pending.set(true)
        super.setValue(value)
    }

    /**
     * Used for cases where T is Void or you don't want to pass data.
     */
    @MainThread
    fun call() {
        value = null
    }
}