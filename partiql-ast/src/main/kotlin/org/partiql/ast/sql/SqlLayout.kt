package org.partiql.ast.sql

/**
 * [SqlWriter] layout configuration.
 */
public data class SqlLayout(
    public val indent: Indent,
) {

    public companion object {

        public val DEFAULT = SqlLayout(
            indent = Indent(
                count = 2,
                type = Indent.Type.SPACE,
            )
        )

        public val ONELINE = SqlLayout(
            indent = Indent(
                count = 0,
                type = Indent.Type.SPACE,
            )
        )
    }
}

/**
 * [SqlLayout] indent configuration.
 *
 * @property count
 * @property type
 */
public class Indent(
    public val count: Int,
    public val type: Type,
) {

    enum class Type(val char: Char) {
        TAB(Char(9)),
        SPACE(Char(32)), ;
    }

    override fun toString() = type.char.toString().repeat(count)
}
