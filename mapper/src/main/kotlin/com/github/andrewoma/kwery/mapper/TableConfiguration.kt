package com.github.andrewoma.kwery.mapper

import com.github.andrewoma.kwery.mapper.util.camelToLowerUnderscore
import kotlin.reflect.KType

/**
 * TableConfiguration defines configuration common to a set of tables.
 */
class TableConfiguration(
        /**
         * Defines default values for types when the column is not null, but is not selected.
         * Defaults to `standardDefaults`
         */
        val defaults: Map<KType, *> = standardDefaults + timeDefaults,

        /**
         * Defines converters from JDBC types to arbitrary Kotlin types.
         * Defaults to `standardConverters` + `timeConverters`
         */
        val converters: Map<Class<*>, Converter<*>> = standardConverters + timeConverters,

        /**
         * Defines the naming convention for converting `Column` names to SQL column names.
         * Defaults to `camelToLowerUnderscore`
         */
        val namingConvention: (String) -> String = camelToLowerUnderscore)