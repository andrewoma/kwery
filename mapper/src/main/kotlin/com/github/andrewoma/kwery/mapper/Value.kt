package com.github.andrewoma.kwery.mapper

/**
 * Value allows extraction of column values by column.
 */
interface Value<C> {
    infix fun <T> of(column: Column<C, T>): T
}