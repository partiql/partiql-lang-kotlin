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
 * Provides a minimal interface for an AST rewriter implementation.
 */
public interface AstRewriter {
    fun rewriteExprNode(node: ExprNode): ExprNode
}

/**
 * This is the base-class for an AST rewriter which simply makes an exact copy of the original AST.
 * Simple rewrites can be performed by inheritors.
 */
public open class AstRewriterBase : AstRewriter {

    override fun rewriteExprNode(node: ExprNode): ExprNode =
        when (node) {
            is Literal           -> rewriteLiteral(node)
            is LiteralMissing    -> rewriteLiteralMissing(node)
            is VariableReference -> rewriteVariableReference(node)
            is NAry              -> rewriteNAry(node)
            is CallAgg           -> rewriteCallAgg(node)
            is Typed             -> rewriteTyped(node)
            is Path              -> rewritePath(node)
            is SimpleCase        -> rewriteSimpleCase(node)
            is SearchedCase      -> rewriteSearchedCase(node)
            is Struct            -> rewriteStruct(node)
            is ListExprNode      -> rewriteListExprNode(node)
            is Bag               -> rewriteBag(node)
            is Select            -> rewriteSelect(node)
        }

    open fun rewriteMetas(itemWithMetas: HasMetas): MetaContainer = itemWithMetas.metas

    open fun rewriteLiteral(node: Literal): ExprNode =
        Literal(node.ionValue, rewriteMetas(node))

    open fun rewriteLiteralMissing(node: LiteralMissing): ExprNode =
        LiteralMissing(rewriteMetas(node))

    open fun rewriteVariableReference(node: VariableReference): ExprNode =
        VariableReference(
            id = node.id,
            case = node.case,
            scopeQualifier = node.scopeQualifier,
            metas = rewriteMetas(node))

    protected open fun rewriteBag(node: Bag): Bag =
        Bag(
            node.bag.map { rewriteExprNode(it) },
            rewriteMetas(node))

    open fun rewriteListExprNode(node: ListExprNode): ExprNode =
        ListExprNode(
            node.values.map { rewriteExprNode(it) },
            rewriteMetas(node))

    open fun rewriteStruct(node: Struct): ExprNode =
        Struct(
            node.fields.mapIndexed { index, field -> rewriteStructField(field, index) },
            rewriteMetas(node))

     open fun rewriteStructField(field: StructField, index: Int): StructField =
        StructField(
            rewriteExprNode(field.name),
            rewriteExprNode(field.expr))

    open fun rewriteSearchedCase(node: SearchedCase): ExprNode {
        return SearchedCase(
            node.whenClauses.map { rewriteSearchedCaseWhen(it) },
            node.elseExpr?.let { rewriteExprNode(it) },
            rewriteMetas(node))
    }

    open fun rewriteSimpleCase(node: SimpleCase): ExprNode {
        return SimpleCase(
            rewriteExprNode(node.valueExpr),
            node.whenClauses.map { rewriteSimpleCaseWhen(it) },
            node.elseExpr?.let { rewriteExprNode(it) },
            rewriteMetas(node))
    }

    open fun rewritePath(node: Path): ExprNode {
        return Path(
            rewriteExprNode(node.root),
            node.components.map { rewritePathComponent(it) },
            rewriteMetas(node))
    }

    open fun rewriteTyped(node: Typed): ExprNode {
        return Typed(
            node.op,
            rewriteExprNode(node.expr),
            rewriteDataType(node.type),
            rewriteMetas(node))
    }

    open fun rewriteCallAgg(node: CallAgg): ExprNode {
        return CallAgg(
            rewriteExprNode(node.funcExpr),
            node.setQuantifier,
            rewriteExprNode(node.arg),
            rewriteMetas(node))
    }

    open fun rewriteNAry(node: NAry): ExprNode {
        return NAry(
            node.op,
            node.args.map { rewriteExprNode(it) },
            rewriteMetas(node))
    }

    open fun rewriteSelect(selectExpr: Select): ExprNode =
        innerRewriteSelect(selectExpr)

    /**
     * Many subtypes of [AstRewriterBase] need to override [rewriteSelect] to selectively apply a different nested
     * instance of themselves to [Select] nodes.  These subtypes can invoke this method instead of [rewriteSelect]
     * to avoid infinite recursion.  They can also override this function if they need to customize how the new
     * [Select] node is instantiated.
     */
    protected open fun innerRewriteSelect(selectExpr: Select): Select {
        val projection = rewriteSelectProjection(selectExpr.projection)
        val from = rewriteFromSource(selectExpr.from)
        val where = selectExpr.where?.let { rewriteSelectWhere(it) }
        val groupBy = selectExpr.groupBy?.let { rewriteGroupBy(it) }
        val having = selectExpr.having?.let { rewriteSelectHaving(it) }
        val limit = selectExpr.limit?.let { rewriteSelectLimit(it) }
        val metas = rewriteSelectMetas(selectExpr)

        return Select(
            setQuantifier = selectExpr.setQuantifier,
            projection = projection,
            from = from,
            where = where,
            groupBy = groupBy,
            having = having,
            limit = limit,
            metas = metas)
    }

    open fun rewriteSelectWhere(node: ExprNode):ExprNode = rewriteExprNode(node)

    open fun rewriteSelectHaving(node: ExprNode): ExprNode = rewriteExprNode(node)

    open fun rewriteSelectLimit(node: ExprNode): ExprNode = rewriteExprNode(node)

    open fun rewriteSelectMetas(selectExpr: Select): MetaContainer = rewriteMetas(selectExpr)

    open fun rewriteSelectProjection(projection: SelectProjection): SelectProjection =
        when (projection) {
            is SelectProjectionList  -> rewriteSelectProjectionList(projection)
            is SelectProjectionValue -> rewriteSelectProjectionValue(projection)
            is SelectProjectionPivot -> rewriteSelectProjectionPivot(projection)
        }

    open fun rewriteSelectProjectionList(projection: SelectProjectionList): SelectProjection =
        SelectProjectionList(
            projection.items.map { it -> rewriteSelectListItem(it) })

    open fun rewriteSelectProjectionValue(projection: SelectProjectionValue): SelectProjection =
        SelectProjectionValue(rewriteExprNode(projection.expr))


    open fun rewriteSelectProjectionPivot(projection: SelectProjectionPivot): SelectProjection =
        SelectProjectionPivot(
            rewriteExprNode(projection.valueExpr),
            rewriteExprNode(projection.nameExpr))

    open fun rewriteSelectListItem(item: SelectListItem): SelectListItem =
        when(item) {
            is SelectListItemStar       -> rewriteSelectListItemStar(item)
            is SelectListItemExpr       -> rewriteSelectListItemExpr(item)
            is SelectListItemProjectAll -> rewriteSelectListItemProjectAll(item)
        }

    open fun rewriteSelectListItemProjectAll(item: SelectListItemProjectAll): SelectListItem =
        SelectListItemProjectAll(
            rewriteExprNode(item.expr))

    open fun rewriteSelectListItemExpr(item: SelectListItemExpr): SelectListItem =
        SelectListItemExpr(
            rewriteExprNode(item.expr),
            item.asName?.let { rewriteSymbolicName(it) })

    open fun rewriteSelectListItemStar(item: SelectListItemStar): SelectListItem =
        SelectListItemStar(rewriteMetas(item))

    open fun rewritePathComponent(pathComponent: PathComponent): PathComponent =
        when(pathComponent) {
            is PathComponentUnpivot -> rewritePathComponentUnpivot(pathComponent)
            is PathComponentWildcard -> rewritePathComponentWildcard(pathComponent)
            is PathComponentExpr     -> rewritePathComponentExpr(pathComponent)
        }

    open fun rewritePathComponentUnpivot(pathComponent: PathComponent): PathComponent = pathComponent

    open fun rewritePathComponentWildcard(pathComponent: PathComponent): PathComponent = pathComponent

    open fun rewritePathComponentExpr(pathComponent: PathComponentExpr): PathComponent =
        PathComponentExpr(rewriteExprNode(pathComponent.expr), pathComponent.case)

    open fun rewriteFromSource(fromSource: FromSource): FromSource =
        when(fromSource) {
            is FromSourceExpr    -> rewriteFromSourceExpr(fromSource)
            is FromSourceJoin    -> rewriteFromSourceJoin(fromSource)
            is FromSourceUnpivot -> rewriteFromSourceUnpivot(fromSource)
        }

    open fun rewriteFromSourceUnpivot(fromSource: FromSourceUnpivot): FromSource =
        FromSourceUnpivot(
            rewriteExprNode(fromSource.expr),
            fromSource.asName?.let { rewriteSymbolicName(it) },
            fromSource.atName?.let { rewriteSymbolicName(it) },
            rewriteMetas(fromSource))

    open fun rewriteFromSourceJoin(fromSource: FromSourceJoin): FromSource =
        FromSourceJoin(
            fromSource.joinOp,
            rewriteFromSource(fromSource.leftRef),
            rewriteFromSource(fromSource.rightRef),
            rewriteExprNode(fromSource.condition),
            rewriteMetas(fromSource))


    open fun rewriteFromSourceExpr(fromSource: FromSourceExpr): FromSource =
        FromSourceExpr(
            rewriteExprNode(fromSource.expr),
            fromSource.asName?.let { rewriteSymbolicName(it) },
            fromSource.atName?.let { rewriteSymbolicName(it) })

    open fun rewriteGroupBy(groupBy: GroupBy): GroupBy =
        GroupBy(
            groupBy.grouping,
            groupBy.groupByItems.map { rewriteGroupByItem(it)},
            groupBy.groupName?.let { rewriteSymbolicName(it)})

    open fun rewriteGroupByItem(item: GroupByItem): GroupByItem =
        GroupByItem(
            rewriteExprNode(item.expr),
            item.asName?.let { rewriteSymbolicName(it) } )

    open fun rewriteDataType(dataType: DataType) = dataType

    open fun rewriteSimpleCaseWhen(case: SimpleCaseWhen): SimpleCaseWhen =
        SimpleCaseWhen(
            rewriteExprNode(case.valueExpr),
            rewriteExprNode(case.thenExpr))

    open fun rewriteSearchedCaseWhen(case: SearchedCaseWhen): SearchedCaseWhen =
        SearchedCaseWhen(
            rewriteExprNode(case.condition),
            rewriteExprNode(case.thenExpr))

    open fun rewriteSymbolicName(symbolicName: SymbolicName): SymbolicName =
        SymbolicName(
            symbolicName.name,
            rewriteMetas(symbolicName))
}
