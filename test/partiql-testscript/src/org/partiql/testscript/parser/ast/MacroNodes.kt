package org.partiql.testscript.parser.ast

import com.amazon.ion.IonStruct
import com.amazon.ion.IonValue
import org.partiql.testscript.parser.ScriptLocation

internal data class VariableSet(val variables: IonStruct, val scriptLocation: ScriptLocation)

internal data class TestTemplate(
    val id: String,
    val description: IonValue?,
    val statement: IonValue,
    val environment: IonValue?,
    val expected: IonValue,
    val scriptLocation: ScriptLocation
)
