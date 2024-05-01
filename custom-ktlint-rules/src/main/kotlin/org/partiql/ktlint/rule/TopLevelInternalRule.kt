package org.partiql.ktlint.rule

import com.pinterest.ktlint.core.Rule
import com.pinterest.ktlint.core.ast.ElementType
import com.pinterest.ktlint.core.ast.children
import org.jetbrains.kotlin.com.intellij.lang.ASTNode

public class TopLevelInternalRule : Rule("top-level-internal") {

    override fun visit(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit
    ) {
        if (node.elementType != ElementType.IDENTIFIER) {
            return
        }

        // Focus on just functions and values
        val parent = node.treeParent
        if (parent.elementType != ElementType.FUN && parent.elementType != ElementType.PROPERTY) {
            return
        }

        // Check grandparent of node is FILE; if so, is top-level declaration
        if (parent.treeParent.elementType != ElementType.FILE) {
            return
        }
        val modifiers = parent.findChildByType(ElementType.MODIFIER_LIST)?.children()
        if (modifiers != null && modifiers.any { it.elementType == ElementType.INTERNAL_KEYWORD }) {
            emit(
                node.startOffset,
                "Top-level internal declaration found: ${node.text}",
                false
            )
        }
    }
}
