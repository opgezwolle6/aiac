package com.raremartial.aiac.di

import android.content.Context

object ContextProvider {
    private var context: Context? = null

    fun initialize(context: Context) {
        this.context = context.applicationContext
    }

    fun getApplicationContext(): Context {
        return context ?: throw IllegalStateException("Context not initialized. Call ContextProvider.initialize() first.")
    }
}

