package com.raremartial.aiac.database

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.raremartial.aiac.di.ContextProvider

actual class DatabaseDriverFactory {
    actual fun createDriver(): SqlDriver {
        return AndroidSqliteDriver(
            schema = com.raremartial.aiac.database.ChatDatabase.Schema,
            context = ContextProvider.getApplicationContext(),
            name = "chat_database.db"
        )
    }
}

