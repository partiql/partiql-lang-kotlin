package org.partiql.lang.eval.builtins

/**
 * TODO replace this internal value once we have function libraries
 */
internal val SCALAR_BUILTINS_DEFAULT =
    SCALAR_BUILTINS_SQL + SCALAR_BUILTINS_EXT + SCALAR_BUILTINS_COLL_AGG + SYSTEM_BUILTINS_SQL
