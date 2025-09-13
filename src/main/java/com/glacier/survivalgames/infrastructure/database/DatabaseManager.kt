package com.glacier.survivalgames.infrastructure.database

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.fairyproject.container.InjectableComponent
import io.fairyproject.container.PreInitialize
import java.sql.ResultSet

@InjectableComponent
class DatabaseManager {

    private val source: HikariDataSource by lazy {
        val config = HikariConfig().apply {
            jdbcUrl = "jdbc:mysql://mariadb:3306/survivalgames"
            username = "minecraft"
            password = "ilchepo0521"
            maximumPoolSize = 10

            // 接続タイムアウト設定
            connectionTimeout = 30000
            idleTimeout = 600000
            maxLifetime = 1800000
        }
        HikariDataSource(config)
    }

    @PreInitialize
    fun onPreInitialize() {
        if (!existsParticipantTable("participants")) {
            createParticipantsTable()
        }
    }

    /**
     * Participantテーブルが存在するかを返す処理
     */
    fun existsParticipantTable(name: String): Boolean {
        val results = executeQuery(
            sql = """
                SELECT COUNT(*) as count
                FROM information_schema.tables
                WHERE table_schema = ? AND table_name = ?
            """.trimIndent(),
            params = listOf("survivalgames", name)
        ) { it.getInt("count") }

        return results.firstOrNull()?.let { it > 0 } ?: false
    }

    /**
     * Participant テーブルの作成メソッド
     */
    fun createParticipantsTable() {
        val sql = """
            CREATE TABLE IF NOT EXISTS participants (
                unique_id VARCHAR(36) PRIMARY KEY,
                points INT NOT NULL DEFAULT 100,
                wins INT NOT NULL DEFAULT 0,
                played INT NOT NULL DEFAULT 0,
                kills INT NOT NULL DEFAULT 0,
                chests INT NOT NULL DEFAULT 0,
                lifespan INT NOT NULL DEFAULT 0,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                
                INDEX idx_points (points DESC),
                INDEX idx_wins (wins DESC)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
        """.trimIndent()

        executeUpdate(sql)
    }

    /**
     * クエリ実行用のメソッド
     */
    fun <T> executeQuery(sql: String, params: List<Any> = emptyList(), mapper: (ResultSet) -> T): List<T>
            = source.connection.use { connection ->
        connection.prepareStatement(sql).use { statement ->
            params.forEachIndexed { index, param ->
                statement.setObject(index + 1, param)
            }

            statement.executeQuery().use { resultSet ->
                val results = mutableListOf<T>()
                while (resultSet.next()) {
                    results.add(mapper(resultSet))
                }
                results
            }
        }
    }

    /**
     * DB更新用メソッド
     */
    fun executeUpdate(sql: String, params: List<Any> = emptyList()): Int
            = source.connection.use { connection ->
        connection.prepareStatement(sql).use { statement ->
            params.forEachIndexed { index, param ->
                statement.setObject(index + 1, param)
            }
            statement.executeUpdate()
        }
    }

    fun close() {
        if (source.isClosed) {
            source.close()
        }
    }
}