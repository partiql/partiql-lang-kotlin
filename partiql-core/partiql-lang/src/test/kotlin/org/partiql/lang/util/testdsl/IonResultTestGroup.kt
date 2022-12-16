package org.partiql.lang.util.testdsl

/** Defines a group of related tests. */
data class IonResultTestGroup(val name: String, val tests: List<IonResultTestCase>)
