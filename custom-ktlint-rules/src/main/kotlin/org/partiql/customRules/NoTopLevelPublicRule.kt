
package org.partiql.customRules

import com.pinterest.ktlint.core.Rule
import com.pinterest.ktlint.core.ast.ElementType
import com.pinterest.ktlint.core.ast.children
import org.jetbrains.kotlin.com.intellij.lang.ASTNode

public class NoTopLevelPublicRule : Rule("no-top-level-public") {

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
        val grandParent = parent.treeParent
        if (grandParent.elementType != ElementType.FILE) {
            return
        }
        val modifiers = parent.findChildByType(ElementType.MODIFIER_LIST)?.children()
        if (modifiers == null || modifiers.none { it.elementType == ElementType.PUBLIC_KEYWORD }) {
            return
        }

        val annotationEntry = grandParent.findChildByType(ElementType.FILE_ANNOTATION_LIST)?.findChildByType(ElementType.ANNOTATION_ENTRY)
        if (annotationEntry != null) {
            val annotationTarget = annotationEntry.findChildByType(ElementType.ANNOTATION_TARGET)
            if (annotationTarget != null && annotationTarget.text.lowercase() == "file") {
                val constructorCallee = annotationEntry.findChildByType(ElementType.CONSTRUCTOR_CALLEE)
                if (constructorCallee != null && constructorCallee.text.lowercase() == "jvmname") {
                    return
                }
            }
        }
        emit(
            node.startOffset,
            "Top level public declaration found: ${node.text}",
            false
        )
    }
}
