package com.airbnb.mvrx.dogs

import android.app.Application
import com.airbnb.mvrx.MvRx
import com.airbnb.mvrx.dogs.data.DogRepository

/**
 * Launcher icon made by Freepik at flaticon.com.
 */
class DogApplication : Application() {
    val dogsRepository = DogRepository()

    override fun onCreate() {
        super.onCreate()
        MvRx.install(this)
    }
}