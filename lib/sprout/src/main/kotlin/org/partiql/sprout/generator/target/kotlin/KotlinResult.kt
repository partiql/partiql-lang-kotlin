package org.partiql.sprout.generator.target.kotlin

import org.partiql.sprout.generator.target.kotlin.spec.KotlinFileSpec

class KotlinResult(private val specs: List<KotlinFileSpec>) {

    fun write(action: (KotlinFileSpec) -> Unit) = specs.forEach(action)
}
