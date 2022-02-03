package org.partiql.lang.util

/**
 * Simplifies the syntax of adding the `$partiql_bag::` annotation inside of a raw string literal.
 *
 * Instead of `"""${'$'}partiql_bag::[1, 2, 3] """` we can simply type `"""$partiql_bag::[1, 2, 3]"""`.
 */
internal const val partiql_bag = "\$partiql_bag"

/**
 * Simplifies the syntax of adding the `partiql_missing::` annotation inside of a raw string literal.
 *
 * Instead of `"""${'$'}partiql_missing::null """` we can simply type `"""$partiql_missing::null"""`.
 */
internal const val partiql_missing = "\$partiql_missing"
