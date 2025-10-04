package com.glacier.survivalgames.infrastructure.repository

import com.glacier.survivalgames.domain.model.Participant
import com.glacier.survivalgames.domain.repository.ParticipantRepository
import com.glacier.survivalgames.infrastructure.manager.DatabaseManager
import io.fairyproject.container.InjectableComponent
import java.sql.ResultSet
import java.util.UUID

@InjectableComponent
class ParticipantRepositoryImpl(private val databaseManager: DatabaseManager) : ParticipantRepository {
    /**
     * 指定したUniqueIdを持つParticipantを取得する
     */
    override fun findByUniqueId(uniqueId: UUID): Participant? {
        val results = databaseManager.executeQuery(
            sql = "SELECT * FROM participants WHERE unique_id = ?",
            params = listOf(uniqueId.toString())
        ) { getParticipant(it) }

        return results.firstOrNull()
    }

    /**
     * DB上にある全てのParticipantを取得する
     */
    override fun findAll(): List<Participant>
            = databaseManager.executeQuery(
        sql = "SELECT * FROM participants"
    ) { getParticipant(it) }

    /**
     * 引数で渡したParticipantを保存する
     */
    override fun save(participant: Participant): Participant {
        val exist = findByUniqueId(participant.uniqueId)
        return if (exist == null) {
            // INSERT: Participantを新規作成
            databaseManager.executeUpdate(
                sql = """
                    INSERT INTO participants (unique_id, points, wins, played, kills, chests, lifespan, previous_map) 
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """.trimIndent(),
                params = listOf(
                    participant.uniqueId.toString(),
                    participant.points,
                    participant.wins,
                    participant.played,
                    participant.kills,
                    participant.chests,
                    participant.lifespan,
                    participant.previousMap
                )
            )
            participant // 新規作成されたParticipantをそのまま返す
        } else {
            // UPDATE: 既存レコードを更新する
            databaseManager.executeUpdate(
                sql = """
                    UPDATE participants
                    SET points = ?, wins = ?, played = ?, kills = ?, chests = ?, lifespan = ?, previous_map = ?
                    WHERE unique_id = ?
                """.trimIndent(),
                params = listOf(
                    participant.points,
                    participant.wins,
                    participant.played,
                    participant.kills,
                    participant.chests,
                    participant.lifespan,
                    participant.previousMap,
                    participant.uniqueId.toString()
                )
            )
            participant // 更新されたParticipantを返す
        }
    }

    /**
     * 指定したUniqueIdを持つレコードを削除する
     */
    override fun remove(uniqueId: UUID): Boolean {
        val affectedRows = databaseManager.executeUpdate(
            sql = "DELETE FROM participants WHERE unique_id = ?",
            params = listOf(uniqueId.toString())
        )

        // 影響を受けた行数が１以上なら削除成功
        return affectedRows > 0
    }

    /**
     * 指定したUniqueIdを持つParticipantがあるかを返す
     */
    override fun exists(uniqueId: UUID): Boolean {
        val results = databaseManager.executeQuery(
            sql = "SELECT COUNT(*) as count FROM participants WHERE unique_id = ?",
            params = listOf(uniqueId.toString())
        ) { resultSet ->
            resultSet.getInt("count")
        }

        // COUNT結果が1以上なら存在する
        return results.firstOrNull()?.let { it > 0 } ?: false
    }

    override fun getRanking(uniqueId: UUID): Int {
        val results = databaseManager.executeQuery(
            sql = """
                SELECT 
                    ROW_NUMBER() OVER (
                        ORDER BY 
                            (CAST(wins AS DECIMAL) / CAST(played AS DECIMAL)) DESC,
                            wins DESC,
                            points DESC
                    ) as rank,
                    unique_id
                FROM participants
                WHERE played > 0
            """.trimIndent()
        ) { resultSet ->
            resultSet.getInt("rank") to resultSet.getString("unique_id")
        }

        return results.find { it.second == uniqueId.toString() }?.first ?: -1
    }

    /**
     * ResultSetからParticipantを作成する
     */
    private fun getParticipant(result: ResultSet): Participant = Participant(
        uniqueId = UUID.fromString(result.getString("unique_id")),
        points = result.getInt("points"),
        wins = result.getInt("wins"),
        played = result.getInt("played"),
        kills = result.getInt("kills"),
        chests = result.getInt("chests"),
        lifespan = result.getInt("lifespan"),
        previousMap = result.getString("previous_map")
    )
}