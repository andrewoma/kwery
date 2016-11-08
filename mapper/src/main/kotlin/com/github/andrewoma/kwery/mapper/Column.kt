package com.github.andrewoma.kwery.mapper

/**
 * Column defines a how to map an SQL column to and from an object property of type `T`
 * within a containing class `C`.
 *
 * While columns can be added directly it is more common to use the `col` methods on `Table`
 * to provide sensible defaults.
 */
data class Column<C, T>(
        /**
         * A function to extract the property value from the containing object
         */
        val property: (C) -> T,

        /**
         * If a value is not `nullable` a default value must be provided to allow construction
         * of partially selected objects
         */
        val defaultValue: T,

        /**
         * A converter between the SQL type and `T`
         */
        val converter: Converter<T>,

        /**
         * The name of the SQL column
         */
        val name: String,

        /**
         * True if the column is part of the primary key
         */
        val id: Boolean,

        /**
         * True if the column is used for versioning using optimistic locking
         */
        val version: Boolean,

        /**
         * True if the column is selected in queries by default.
         * Generally true, but is useful to exclude `BLOBs` and `CLOBs` in some cases.
         */
        val selectByDefault: Boolean,

        /**
         * True if the column is nullable
         */
        val isNullable: Boolean
) {
    /**
     * A type-safe variant of `to`
     */
    infix fun of(value: T): Pair<Column<C, T>, T> = Pair(this, value)

    /**
     * A type-safe variant of `to` with an optional value
     */
    @Suppress("BASE_WITH_NULLABLE_UPPER_BOUND")
    infix fun optional(value: T?): Pair<Column<C, T>, T?> = Pair(this, value)

    override fun toString(): String {
        return "Column($name id=$id version=$version nullable=$isNullable)" // Prevent NPE in debugger on "property"
    }
}