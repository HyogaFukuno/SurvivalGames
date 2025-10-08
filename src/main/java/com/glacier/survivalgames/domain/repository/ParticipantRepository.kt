package com.glacier.survivalgames.domain.repository

import com.glacier.survivalgames.domain.model.Participant
import java.util.UUID

interface ParticipantRepository {
    fun findByUniqueId(uniqueId: UUID): Participant?
    fun findAll(): List<Participant>
    fun save(participant: Participant): Participant
    fun remove(uuid: UUID): Boolean
    fun exists(uuid: UUID): Boolean
    fun getRanking(uuid: UUID): Int
}