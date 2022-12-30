package org.partiql.sprout.generator.target.kotlin

/**
 * Generator options are entirely independent of the type definitions
 *
 * @property packageRoot
 */
class KotlinOptions(
    val packageRoot: String,
    val node: KotlinNodeOptions = KotlinNodeOptions(),
)

/**
 * Consider other options as this is Kotlin specific
 */
class KotlinNodeOptions(
    val modifier: Modifier = Modifier.FINAL,
) {

    enum class Modifier {
        FINAL,
        DATA,
        OPEN,
    }
}
