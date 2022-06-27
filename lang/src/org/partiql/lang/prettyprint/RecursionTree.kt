package org.partiql.lang.prettyprint

import org.partiql.lang.domains.PartiqlAst

/**
 * PIG AST is not a recursive data structure, thus it is not easy to transform it directly to a pretty printed string.
 * So we need to first transform it into RecursionTree, which is a recursive tree structure (it has a list of children which are also RecursionTree),
 * then we can recursively pretty print the RecursionTree as we want.
 *
 * @param astType is a string of the PIG AST node type
 * @param value is the value in case node type is [PartiqlAst.Expr.Lit]
 * @param attrOfParent is a string which represents which attribute it belongs to its parent
 * @param children is a list of child RecursionTree
 *
 * Take the [PartiqlAst.Expr.Eq] node in the WHERE clause in `SELECT a FROM b WHERE c = d` as example.
 * [astType] is '=', [value] is null, [attrOfParent] is 'where', [children] is a list of [PartiqlAst.Expr.Id] c and d.
 */
class RecursionTree(
    private val astType: String,
    private val value: String? = null,
    private val attrOfParent: String? = null,
    private val children: List<RecursionTree>? = null
) {
    fun convertToString(): String {
        val result = StringBuilder()
        recurseToResult(0, result)
        return result.toString().dropLast(1) // Drop last line separator \n
    }

    private fun recurseToResult(indent: Int, result: StringBuilder) {
        val prefix = when (attrOfParent) {
            null -> ""
            else -> "$attrOfParent: "
        }

        val displayedValue = when (value) {
            null -> ""
            else -> " $value"
        }

        result.append("\t".repeat(indent))
            .append(prefix)
            .append(astType)
            .append(displayedValue)
            .append('\n')

        children?.forEach {
            it.recurseToResult(indent + 1, result)
        }
    }
}
