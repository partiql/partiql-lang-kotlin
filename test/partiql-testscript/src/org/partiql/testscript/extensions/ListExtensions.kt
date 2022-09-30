package org.partiql.testscript.extensions

internal fun <A, B> List<A>.crossProduct(other: List<B>): List<Pair<A, B>> {
    val r = mutableListOf<Pair<A, B>>()
    for (thisEl in this) {
        for (otherEl in other) {
            r.add(thisEl to otherEl)
        }
    }

    return r
}
