package com.github.andrewoma.kwery.example.film.dao

import com.github.andrewoma.kwery.mapper.*
import com.github.andrewoma.kwery.core.*

import com.github.andrewoma.kwery.example.film.model.FilmActor as FA
import com.github.andrewoma.kwery.example.film.model.FilmActor


object filmActorTable : Table<FA, FA.Id>("film_actor", tableConfig), VersionedWithTimestamp {
    // @formatter:off
    val FilmId     by col(FA.Id::filmId,  path = { it.id }, id = true)
    val ActorId    by col(FA.Id::actorId, path = { it.id }, id = true)
    // @formatter:on

    override fun idColumns(id: FA.Id) = setOf(FilmId of id.filmId, ActorId of id.actorId)
    override fun create(value: Value<FA>) = FA(FA.Id(value of FilmId, value of ActorId))
}

class FilmActorDao(session: Session) : AbstractDao<FA, FA.Id>(session, filmActorTable, { it.id }, null, IdStrategy.Explicit) {

    fun findByFilmIds(ids: Collection<Int>): List<FilmActor> {
        val name = "findByFilmIds"
        val sql = sql(name) { "select $columns from ${table.name} where film_id in(unnest(:ids))" }
        val idsArray = session.connection.createArrayOf("int", ids.copyToArray<Any>())
        return session.select(sql, mapOf("ids" to idsArray), selectOptions(name), table.rowMapper())
    }

    fun findByActorIds(ids: Collection<Int>): List<FilmActor> {
        val name = "findByActorIds"
        val sql = sql(name) { "select $columns from ${table.name} where actor_id in(unnest(:ids))" }
        val idsArray = session.connection.createArrayOf("int", ids.copyToArray<Any>())
        return session.select(sql, mapOf("ids" to idsArray), selectOptions(name), table.rowMapper())
    }
}