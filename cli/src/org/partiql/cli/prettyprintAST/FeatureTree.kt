package org.partiql.cli.prettyprintAST

import org.partiql.lang.domains.PartiqlAst

/**
 * PIG AST is not a recursive data structure, thus it is not easy to transform it directly to a pretty printed string.
 * So we need to first transform it into FeatureTree, which is a recursive tree structure (it has a list of children which are also Feature Tree),
 * then we can recursively pretty print the FeatureTree as we want.
 *
 * @param astType is a string of the PIG AST node type
 * @param value is the value in case node type is [PartiqlAst.Expr.Lit]
 * @param attrOfParent is a string which represents which attribute it belongs to its parent
 * @param children is a list of child FeatureTree
 *
 * The first 3 parameters are chosen as they represent the main feature of a PIG AST node.
 *
 */
class FeatureTree (astType: String, value: String? = null, attrOfParent: String? = null, children: List<FeatureTree>? = null) {
    private val astType = astType
    private val value = value
    private val attrOfParent = attrOfParent
    private val children = children

    fun convertToString (): String {
        val result = StringBuilder()
        recurseToResult(0, result)
        return result.toString().dropLast(1) // Drop last line separator \n
    }

    private fun recurseToResult (indent: Int, result: StringBuilder) {
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