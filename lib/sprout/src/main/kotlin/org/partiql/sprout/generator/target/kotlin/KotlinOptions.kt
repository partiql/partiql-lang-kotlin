package org.partiql.sprout.generator.target.kotlin

/**
 * Generator options are entirely independent of the type definitions
 *
 * @property packageRoot
 */
class KotlinOptions(
    val packageRoot: String,
    val poems: List<String>,
    val optIns: List<String> = emptyList(),
)
