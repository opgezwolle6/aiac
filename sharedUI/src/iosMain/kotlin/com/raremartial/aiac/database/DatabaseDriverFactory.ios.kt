package com.raremartial.aiac.database

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver

actual class DatabaseDriverFactory {
    actual fun createDriver(): SqlDriver {
        return NativeSqliteDriver(
            schema = com.raremartial.aiac.database.ChatDatabase.Schema,
            name = "chat_database.db"
        )
    }
}

