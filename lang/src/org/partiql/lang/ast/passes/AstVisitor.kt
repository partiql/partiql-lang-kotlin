/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates.  All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 *  You may not use this file except in compliance with the License.
 * A copy of the License is located at:
 *
 *      http://aws.amazon.com/apache2.0/
 *
 *  or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific
 *  language governing permissions and limitations under the License.
 */

package org.partiql.lang.ast.passes

import org.partiql.lang.ast.*

/**
 * Used in conjunction with [AstWalker], implementors of this interface can easily inspect an AST.
 *
 * Implementations of this interface might be:
 *
 *  - Structural AST validators
 *  - Semantic passes
 *  - Anything that examines the AST but does not evaluate or rewrite.
 *
 *  One `visit*` function is included for each base type in the AST.
 */
@Deprecated("Use org.lang.partiql.domains.PartiqlAst.Visitor instead")
interface AstVisitor {
    /**
     * Invoked by [AstWalker] for every instance of [ExprNode] encountered.
     *
     * If the node has children, this is invoked before the children are walked.
     */
    fun visitExprNode(expr: ExprNode) {
        // Default implementation does nothing.
    }

    /**
     * Invoked by [AstWalker] for every instance of [SelectProjection] encountered.
     *
     * If the node has children, this is invoked before the children are walked.
     */
    fun visitSelectProjection(projection: SelectProjection) {
        // Default implementation does nothing.
    }

    /**
     * Invoked by [AstWalker]for every instance of [SelectListItem] encountered.
     *
     * If the node has children, this is invoked before the children are walked.
     */
    fun visitSelectListItem(selectListItem: SelectListItem) {
        // Default implementation does nothing.
    }

    /**
     * Invoked by [AstWalker] for every instance of [FromSource] encountered.
     *
     * If the node has children, this is invoked before the children are walked.
     */
    fun visitFromSource(fromSource: FromSource) {
        // Default implementation does nothing.
    }

    /**
     * Invoked by [AstWalker] for every instance of [PathComponent] encountered.
     *
     * If the node has children, this is invoked before the children are walked.
     */
    fun visitPathComponent(pathComponent: PathComponent) {
        // Default implementation does nothing.
    }

    /**
     * Invoked by [AstWalker] for every instance of [DataType] encountered.
     *
     * If the node has children, this is invoked before the children are walked.
     */
    fun visitDataType(dataType: DataType) {
        // Default implementation does nothing.
    }

    /**
     * Invoked by [AstWalker] for every instance of [DataManipulationOperation].
     *
     * The operation's children are visited after this method is invoked.
     */
    fun visitDataManipulationOperation(dmlOp: DataManipulationOperation) {
        // Default implementation does nothing.
    }

    /**
     * Invoked by [AstWalker] for every instance of [OnConflict].
     *
     * The operation's children are visited after this method is invoked.
     */
    fun visitOnConflict(onConflict: OnConflict) {
        // Default implementation does nothing.
    }
}

/**
 * Provides default implementations of [AstVisitor] methods which do nothing.
 * This is needed because the Kotlin compiler doesn't expose the default
 * implementations to java.  (Or at least that feature is currently
 * experimental.)
 */
@Deprecated("Use AstNode#iterator() or AstNode#children()")
open class AstVisitorBase : AstVisitor {
    override fun visitExprNode(expr: ExprNode) {
        // Default implementation does nothing.
    }

    override fun visitSelectProjection(projection: SelectProjection) {
        // Default implementation does nothing.
    }

    override fun visitSelectListItem(selectListItem: SelectListItem) {
        // Default implementation does nothing.
    }

    override fun visitFromSource(fromSource: FromSource) {
        // Default implementation does nothing.
    }

    override fun visitPathComponent(pathComponent: PathComponent) {
        // Default implementation does nothing.
    }

    override fun visitDataType(dataType: DataType) {
        // Default implementation does nothing.
    }

    override fun visitDataManipulationOperation(dmlOp: DataManipulationOperation) {
        // Default implementation does nothing.
    }

    override fun visitOnConflict(onConflict: OnConflict) {
        // Default implementation does nothing.
    }
}