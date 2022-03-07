package org.partiql.testscript.parser.ast

import com.amazon.ion.IonSexp
import com.amazon.ion.IonStruct
import org.partiql.testscript.parser.ScriptLocation

sealed class AstNode {
    abstract val scriptLocation: ScriptLocation
}

data class ModuleNode(val nodes: List<AstNode>, override val scriptLocation: ScriptLocation) : AstNode()

data class TestNode(
    val id: String,
    val description: String?,
    val statement: String,
    val environment: IonStruct?,
    val expected: IonSexp,
    override val scriptLocation: ScriptLocation
) : AstNode()

sealed class SetDefaultEnvironmentNode : AstNode()

data class FileSetDefaultEnvironmentNode(
    val environmentRelativeFilePath: String,
    override val scriptLocation: ScriptLocation
) : SetDefaultEnvironmentNode()

data class InlineSetDefaultEnvironmentNode(
    val environment: IonStruct,
    override val scriptLocation: ScriptLocation
) : SetDefaultEnvironmentNode()

data class SkipListNode(val patterns: List<String>, override val scriptLocation: ScriptLocation) : AstNode()

data class AppendTestNode(
    val pattern: String,
    val additionalData: IonStruct,
    override val scriptLocation: ScriptLocation
) : AstNode()
