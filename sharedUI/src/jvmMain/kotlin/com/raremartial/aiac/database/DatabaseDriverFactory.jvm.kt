package com.raremartial.aiac.database

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import java.io.File

actual class DatabaseDriverFactory {
    actual fun createDriver(): SqlDriver {
        val databasePath = File(System.getProperty("user.home"), ".aiac")
        databasePath.mkdirs()
        
        val dbFile = File(databasePath, "chat_database.db")
        val driver = JdbcSqliteDriver("jdbc:sqlite:${dbFile.absolutePath}")
        
        // Создаем схему только если база данных не существует или пустая
        val databaseExists = dbFile.exists() && dbFile.length() > 0L
        
        if (!databaseExists) {
            // База данных не существует или пустая - создаем схему
            ChatDatabase.Schema.create(driver)
        }
        // Если база уже существует, просто используем её без создания схемы
        
        return driver
    }
}