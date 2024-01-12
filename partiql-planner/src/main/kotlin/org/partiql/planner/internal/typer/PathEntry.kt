package org.partiql.planner.internal.typer

/**
 *
 *
 * @param T
 * @property catalog    The resolved entity's catalog name in case-normal-form.
 * @property path       The resolved entity's path (within its catalog) in case-normal-form.
 * @property metadata   The resolved entity's type information.
 */
internal data class PathEntry<T>(
    val catalog: String,
    val path: List<String>,
    val metadata: T,
)
