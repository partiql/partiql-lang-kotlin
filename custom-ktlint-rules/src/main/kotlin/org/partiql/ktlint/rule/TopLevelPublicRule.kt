package org.partiql.ktlint.rule

import com.pinterest.ktlint.core.Rule
import com.pinterest.ktlint.core.ast.ElementType
import com.pinterest.ktlint.core.ast.children
import org.jetbrains.kotlin.com.intellij.lang.ASTNode

public class TopLevelPublicRule : Rule("top-level-public") {
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

        val modifiers = parent.findChildByType(ElementType.MODIFIER_LIST)
        if (modifiers != null && modifiers.isNotPublic()) {
            return
        }

        val annotationEntry = grandParent.findChildByType(ElementType.FILE_ANNOTATION_LIST)?.findChildByType(ElementType.ANNOTATION_ENTRY)
        if (annotationEntry == null || !annotationEntry.containsFileJvmName()) {
            emit(
                node.startOffset,
                "Top-level public declaration found without `@file:JvmName` annotation: ${node.text}",
                false
            )
        }
    }

    // returns true iff modifiers is not one of `PRIVATE_KEYWORD`, `INTERNAL_KEYWORD` or `PROTECTED_KEYWORD`
    private fun ASTNode.isNotPublic(): Boolean {
        val modifiers = this.children().map { it.elementType }
        return modifiers.any { it == ElementType.PRIVATE_KEYWORD || it == ElementType.INTERNAL_KEYWORD || it == ElementType.PROTECTED_KEYWORD }
    }

    // returns true iff node is `@file:JvmName(<some name>)`
    private fun ASTNode.containsFileJvmName(): Boolean {
        val annotationTarget = this.findChildByType(ElementType.ANNOTATION_TARGET)
        if (annotationTarget == null || annotationTarget.text.lowercase() != "file") {
            return false
        }
        val constructorCallee = this.findChildByType(ElementType.CONSTRUCTOR_CALLEE)
        if (constructorCallee == null || constructorCallee.text.lowercase() != "jvmname") {
            return false
        }
        return true
    }
}
