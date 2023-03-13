package org.partiql.pig.runtime

fun kotlin.collections.List<com.amazon.ionelement.api.IonElement>.asAnyElement() = map { it.asAnyElement() }
