package org.partiql.runner.schema

import com.amazon.ion.IonStruct

data class Namespace(
    var env: IonStruct,
    val namespaces: MutableList<Namespace>,
    val testCases: MutableList<TestCase>,
    val equivClasses: MutableMap<String, List<String>>
)
