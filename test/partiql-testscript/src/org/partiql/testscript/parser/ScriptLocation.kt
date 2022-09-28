package org.partiql.testscript.parser

data class ScriptLocation(val inputName: String, val lineNum: Long) {
    override fun toString(): String {
        return "$inputName:$lineNum"
    }
}
