package org.partiql.transpiler.block

/**
 * BlockWriter Layout configuration.
 */
public data class Layout(
    val indent: Indent,
) {

    public companion object {

        public val DEFAULT = Layout(
            indent = Indent(
                count = 2,
                type = Indent.Type.SPACE,
            )
        )
    }
}

class Indent(
    val count: Int,
    val type: Type,
) {

    enum class Type(val char: Char) {
        TAB(Char(9)),
        SPACE(Char(32)), ;
    }

    override fun toString() = type.char.toString().repeat(count)
}
