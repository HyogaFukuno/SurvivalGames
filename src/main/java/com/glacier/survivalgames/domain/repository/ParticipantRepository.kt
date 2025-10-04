package com.glacier.survivalgames.domain.repository

import com.glacier.survivalgames.domain.model.Participant
import java.util.UUID

interface ParticipantRepository {
    fun findByUniqueId(uniqueId: UUID): Participant?
    fun findAll(): List<Participant>
    fun save(participant: Participant): Participant
    fun remove(uniqueId: UUID): Boolean
    fun exists(uniqueId: UUID): Boolean
    fun getRanking(uniqueId: UUID): Int
}