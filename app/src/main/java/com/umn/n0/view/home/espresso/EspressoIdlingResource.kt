@file:JvmName("EspressoIdlingResource")

package com.umn.n0.view.home.espresso

import androidx.test.espresso.idling.CountingIdlingResource
import com.umn.n0.view.constant.AppBuild

object EspressoIdlingResource {

    private const val RESOURCE = "GLOBAL"

    val idlingResource by lazy { CountingIdlingResource(RESOURCE) }

    @JvmStatic
    fun increment() {
        if (AppBuild.IS_DEBUG)
            idlingResource.increment()
    }

    @JvmStatic
    fun decrement() {
        if (AppBuild.IS_DEBUG)
            idlingResource.decrement()
    }
}