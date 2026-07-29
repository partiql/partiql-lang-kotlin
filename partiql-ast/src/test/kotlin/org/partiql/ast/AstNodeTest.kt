package org.partiql.ast

import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.partiql.ast.ddl.AttributeConstraint
import org.partiql.ast.ddl.TableConstraint
import org.partiql.ast.expr.Expr
import org.partiql.ast.expr.WindowFunction
import org.partiql.ast.graph.GraphDirection
import java.io.File
import java.lang.reflect.Modifier

/**
 * Guards the `getChildren()` null-safety contract across the [AstNode] hierarchy.
 *
 * ## The contract
 * `getChildren()` must never return a list containing `null`: traversals such as
 * [AstVisitor.defaultVisit] call `child.accept(...)` unchecked. A node with a `@Nullable` child
 * field must guard it (`if (field != null) kids.add(field);`) rather than add it unconditionally.
 * Violations have already shipped: LEAD/LAG ([WindowFunctionType]), DDL [AttributeConstraint], and
 * [TableConstraint.Unique].
 *
 * ## How this test works
 * - Per-node tests: for each node with a nullable child field, build it with that field `null` and
 *   assert via [assertNullSafe] that `getChildren()` has no null element and a full visitor
 *   traversal does not throw.
 * - Completeness guard: [astNodeSetIsUnchanged] fails when a concrete [AstNode] subclass is
 *   added/removed, forcing the author to decide whether the new node needs a per-node test above.
 */
class AstNodeTest {

    @Test
    fun astNodeSetIsUnchanged() {
        val discovered = discoverConcreteAstNodes().toSortedSet()

        val added = discovered - KNOWN_AST_NODES
        val removed = KNOWN_AST_NODES - discovered

        assertTrue(
            added.isEmpty() && removed.isEmpty(),
            buildString {
                appendLine("The set of concrete AstNode subclasses changed.")
                appendLine()
                if (added.isNotEmpty()) {
                    appendLine("ADDED nodes — for EACH, check its source for @Nullable child fields. If it has any,")
                    appendLine("make getChildren() guard them (`if (field != null) kids.add(field);`) and add a")
                    appendLine("dedicated null-child @Test to AstNodeTest; otherwise a null child will NPE traversal:")
                    added.forEach { appendLine("  + $it") }
                    appendLine()
                }
                if (removed.isNotEmpty()) {
                    appendLine("REMOVED nodes:")
                    removed.forEach { appendLine("  - $it") }
                    appendLine()
                }
                appendLine("After verifying null-safety, update KNOWN_AST_NODES in AstNodeTest to match.")
            },
        )
    }

    // region per-node null-child tests

    @Test
    fun fromExpr() = assertNullSafe(Ast.fromExpr(lit(), FromType.SCAN(), asAlias = null, atAlias = null))

    @Test
    fun fromJoin() = assertNullSafe(Ast.fromJoin(fromRef(), fromRef(), joinType = null, condition = null))

    @Test
    fun groupBy() = assertNullSafe(Ast.groupBy(GroupByStrategy.FULL(), listOf(Ast.groupByKey(lit(), null)), asAlias = null))

    @Test
    fun groupByKey() = assertNullSafe(Ast.groupByKey(lit(), asAlias = null))

    @Test
    fun queryBodySFW() = assertNullSafe(Ast.queryBodySFW(select(), from())) // exclude/let/where/groupBy/having/window all null

    @Test
    fun selectItemExpr() = assertNullSafe(Ast.selectItemExpr(lit(), asAlias = null))

    @Test
    fun windowSpecification() = assertNullSafe(
        @Suppress("DEPRECATION")
        Ast.windowSpecification(existingName = null, partitionClause = null, orderByClause = null),
    )

    @Test
    fun withListElement() = assertNullSafe(Ast.withListElement(simple(), Ast.exprQuerySet(body()), columnList = null))

    @Test
    fun exprCase() = assertNullSafe(Ast.exprCase(expr = null, branches = listOf(Ast.exprCaseBranch(lit(), lit())), defaultExpr = null))

    @Test
    fun exprLike() = assertNullSafe(Ast.exprLike(lit(), lit(), escape = null, not = false))

    @Test
    fun exprOperator() = assertNullSafe(Ast.exprOperator("+", lhs = null, rhs = lit()))

    @Test
    fun exprOverlay() = assertNullSafe(Ast.exprOverlay(lit(), lit(), lit(), forLength = null))

    @Test
    fun exprQuerySet() = assertNullSafe(Ast.exprQuerySet(body())) // orderBy/limit/offset/with all null

    @Test
    fun exprSubstring() = assertNullSafe(Ast.exprSubstring(lit(), start = null, length = null))

    @Test
    fun exprTrim() = assertNullSafe(Ast.exprTrim(lit(), chars = null, trimSpec = null))

    @Test
    fun exprWindow() = assertNullSafe(
        @Suppress("DEPRECATION")
        Ast.exprWindow(WindowFunction.LEAD(), lit(), offset = null, defaultValue = null, over = Ast.exprWindowOver(emptyList(), emptyList())),
    )

    @Test
    fun windowFunctionTypeLead() = assertNullSafe(
        @Suppress("DEPRECATION")
        WindowFunctionType.Lead(lit(), null, null, null),
    ) // defaultValue null

    @Test
    fun windowFunctionTypeLag() = assertNullSafe(
        @Suppress("DEPRECATION")
        WindowFunctionType.Lag(lit(), null, null, null),
    ) // defaultValue null

    @Test
    fun attributeConstraintNull() = assertNullSafe(Ast.columnConstraintNullable(name = null, isNullable = false))

    @Test
    fun attributeConstraintUnique() = assertNullSafe(Ast.columnConstraintUnique(name = null, isPrimaryKey = false))

    @Test
    fun attributeConstraintCheck() = assertNullSafe(Ast.columnConstraintCheck(name = null, searchCondition = lit()))

    @Test
    fun tableConstraintUnique() = assertNullSafe(Ast.tableConstraintUnique(name = null, columns = emptyList(), isPrimaryKey = false))

    @Test
    fun createTable() = assertNullSafe(Ast.createTable(ident(), emptyList(), emptyList(), partitionBy = null, tableProperties = emptyList()))

    @Test
    fun conflictActionDoReplace() = assertNullSafe(Ast.doReplace(Ast.doReplaceActionExcluded(), condition = null))

    @Test
    fun conflictActionDoUpdate() = assertNullSafe(Ast.doUpdate(Ast.doUpdateActionExcluded(), condition = null))

    @Test
    fun delete() = assertNullSafe(Ast.delete(ident(), condition = null))

    @Test
    fun insert() = assertNullSafe(Ast.insert(ident(), asAlias = null, source = Ast.insertSourceDefault(), onConflict = null))

    @Test
    fun insertSourceFromExpr() = assertNullSafe(Ast.insertSourceExpr(columns = null, expr = lit()))

    @Test
    fun onConflict() = assertNullSafe(Ast.onConflict(Ast.doNothing(), target = null))

    @Test
    fun replace() = assertNullSafe(Ast.replace(ident(), asAlias = null, source = Ast.insertSourceDefault()))

    @Test
    fun update() = assertNullSafe(Ast.update(ident(), setClauses = emptyList(), condition = null))

    @Test
    fun upsert() = assertNullSafe(Ast.upsert(ident(), asAlias = null, source = Ast.insertSourceDefault()))

    @Test
    fun graphMatch() = assertNullSafe(Ast.graphMatch(listOf(Ast.graphPattern(parts = listOf(Ast.graphMatchNode()))), selector = null))

    @Test
    fun graphMatchNode() = assertNullSafe(Ast.graphMatchNode(prefilter = null, variable = null, label = null))

    @Test
    fun graphMatchEdge() = assertNullSafe(Ast.graphMatchEdge(GraphDirection.LEFT(), quantifier = null, prefilter = null, variable = null, label = null))

    @Test
    fun graphPattern() = assertNullSafe(Ast.graphPattern(restrictor = null, prefilter = null, variable = null, quantifier = null, parts = listOf(Ast.graphMatchNode())))

    @Test
    fun dataTypeScalar() = assertNullSafe(DataType.INTEGER()) // name/keyType/elementType/fields all null

    // endregion

    /** Asserts [node]'s children contain no null and a full visitor traversal does not throw. */
    private fun assertNullSafe(node: AstNode) {
        node.children.forEachIndexed { i, child ->
            assertNotNull(
                child,
                "${node::class.java.name}.getChildren() leaked a null at index $i — guard the " +
                    "nullable field with `if (field != null) kids.add(field);`.",
            )
        }
        assertDoesNotThrow(
            {
                object : AstVisitor<Unit, Unit>() {
                    override fun defaultReturn(n: AstNode, ctx: Unit) = Unit
                }.let { node.accept(it, Unit) }
            },
            "Visitor traversal of ${node::class.java.name} threw — a null child likely leaked into getChildren().",
        )
    }

    // region shared leaf builders

    private fun lit(): Expr = Ast.exprLit(Literal.intNum(1))
    private fun simple(): Identifier.Simple = Ast.identifierSimple("x", true)
    private fun ident(): Identifier = Ast.identifier(emptyList(), simple())
    private fun fromRef(): FromExpr = Ast.fromExpr(lit(), FromType.SCAN())
    private fun select(): Select = Ast.selectStar()
    private fun from(): From = Ast.from(listOf(fromRef()))
    private fun body(): QueryBody = Ast.queryBodySFW(select(), from())

    // endregion

    // region completeness guard

    /**
     * Enumerates concrete (non-abstract/interface/enum) [AstNode] subclasses by walking this
     * module's compiled class output, located via AstNode.class on the classpath.
     */
    private fun discoverConcreteAstNodes(): List<String> {
        val markerPath = AstNode::class.java.name.replace('.', '/') + ".class"
        val markerUrl = AstNode::class.java.classLoader.getResource(markerPath)
            ?: error("Could not locate AstNode.class on the classpath")
        val astDir = File(markerUrl.toURI()).parentFile // .../org/partiql/ast
        val classesRoot = astDir.parentFile.parentFile.parentFile // .../classes/java/main

        return classesRoot.walkTopDown()
            .filter { it.isFile && it.extension == "class" }
            .mapNotNull { file ->
                val binaryName = file.relativeTo(classesRoot).path
                    .removeSuffix(".class")
                    .replace(File.separatorChar, '.')
                if (binaryName.endsWith("package-info")) return@mapNotNull null
                val simpleName = binaryName.substringAfterLast('.')
                // Skip Lombok builders and synthetic/anonymous classes.
                if (simpleName == "Builder" || simpleName.startsWith("Builder") || simpleName.matches(Regex(".*\\$\\d+"))) {
                    return@mapNotNull null
                }
                val cls = runCatching {
                    Class.forName(binaryName, false, AstNode::class.java.classLoader)
                }.getOrNull() ?: return@mapNotNull null
                val mods = cls.modifiers
                val isConcreteNode = AstNode::class.java.isAssignableFrom(cls) &&
                    !Modifier.isAbstract(mods) &&
                    !cls.isInterface &&
                    !cls.isEnum
                if (isConcreteNode) binaryName.replace('$', '.') else null
            }
            .toList()
    }

    companion object {
        /** Baseline set of concrete [AstNode] subclasses; see [astNodeSetIsUnchanged]. */
        private val KNOWN_AST_NODES: Set<String> = sortedSetOf(
            "org.partiql.ast.DataType",
            "org.partiql.ast.DataType.StructField",
            "org.partiql.ast.DatetimeField",
            "org.partiql.ast.Exclude",
            "org.partiql.ast.ExcludePath",
            "org.partiql.ast.ExcludeStep.CollIndex",
            "org.partiql.ast.ExcludeStep.CollWildcard",
            "org.partiql.ast.ExcludeStep.StructField",
            "org.partiql.ast.ExcludeStep.StructWildcard",
            "org.partiql.ast.Explain",
            "org.partiql.ast.From",
            "org.partiql.ast.FromExpr",
            "org.partiql.ast.FromJoin",
            "org.partiql.ast.FromType",
            "org.partiql.ast.GroupBy",
            "org.partiql.ast.GroupBy.Key",
            "org.partiql.ast.GroupByStrategy",
            "org.partiql.ast.Identifier",
            "org.partiql.ast.Identifier.Simple",
            "org.partiql.ast.IntervalQualifier.Range",
            "org.partiql.ast.IntervalQualifier.Single",
            "org.partiql.ast.JoinType",
            "org.partiql.ast.Let",
            "org.partiql.ast.Let.Binding",
            "org.partiql.ast.Literal",
            "org.partiql.ast.Nulls",
            "org.partiql.ast.Order",
            "org.partiql.ast.OrderBy",
            "org.partiql.ast.Query",
            "org.partiql.ast.QueryBody.SFW",
            "org.partiql.ast.QueryBody.SetOp",
            "org.partiql.ast.SelectItem.Expr",
            "org.partiql.ast.SelectItem.Star",
            "org.partiql.ast.SelectList",
            "org.partiql.ast.SelectPivot",
            "org.partiql.ast.SelectStar",
            "org.partiql.ast.SelectValue",
            "org.partiql.ast.SetOp",
            "org.partiql.ast.SetOpType",
            "org.partiql.ast.SetQuantifier",
            "org.partiql.ast.Sort",
            "org.partiql.ast.WindowClause",
            "org.partiql.ast.WindowClause.Definition",
            "org.partiql.ast.WindowFunctionNullTreatment",
            "org.partiql.ast.WindowFunctionType.CumeDist",
            "org.partiql.ast.WindowFunctionType.DenseRank",
            "org.partiql.ast.WindowFunctionType.Lag",
            "org.partiql.ast.WindowFunctionType.Lead",
            "org.partiql.ast.WindowFunctionType.PercentRank",
            "org.partiql.ast.WindowFunctionType.Rank",
            "org.partiql.ast.WindowFunctionType.RowNumber",
            "org.partiql.ast.WindowPartition",
            "org.partiql.ast.WindowSpecification",
            "org.partiql.ast.With",
            "org.partiql.ast.WithListElement",
            "org.partiql.ast.ddl.AttributeConstraint.Check",
            "org.partiql.ast.ddl.AttributeConstraint.Null",
            "org.partiql.ast.ddl.AttributeConstraint.Unique",
            "org.partiql.ast.ddl.ColumnDefinition",
            "org.partiql.ast.ddl.CreateTable",
            "org.partiql.ast.ddl.KeyValue",
            "org.partiql.ast.ddl.PartitionBy",
            "org.partiql.ast.ddl.TableConstraint.Unique",
            "org.partiql.ast.dml.ConflictAction.DoNothing",
            "org.partiql.ast.dml.ConflictAction.DoReplace",
            "org.partiql.ast.dml.ConflictAction.DoUpdate",
            "org.partiql.ast.dml.ConflictTarget.Constraint",
            "org.partiql.ast.dml.ConflictTarget.Index",
            "org.partiql.ast.dml.Delete",
            "org.partiql.ast.dml.DoReplaceAction.Excluded",
            "org.partiql.ast.dml.DoUpdateAction.Excluded",
            "org.partiql.ast.dml.Insert",
            "org.partiql.ast.dml.InsertSource.FromDefault",
            "org.partiql.ast.dml.InsertSource.FromExpr",
            "org.partiql.ast.dml.OnConflict",
            "org.partiql.ast.dml.Replace",
            "org.partiql.ast.dml.SetClause",
            "org.partiql.ast.dml.Update",
            "org.partiql.ast.dml.UpdateTarget",
            "org.partiql.ast.dml.UpdateTargetStep.Element",
            "org.partiql.ast.dml.UpdateTargetStep.Field",
            "org.partiql.ast.dml.Upsert",
            "org.partiql.ast.expr.ExprAnd",
            "org.partiql.ast.expr.ExprArray",
            "org.partiql.ast.expr.ExprBag",
            "org.partiql.ast.expr.ExprBetween",
            "org.partiql.ast.expr.ExprBoolTest",
            "org.partiql.ast.expr.ExprCall",
            "org.partiql.ast.expr.ExprCase",
            "org.partiql.ast.expr.ExprCase.Branch",
            "org.partiql.ast.expr.ExprCast",
            "org.partiql.ast.expr.ExprCoalesce",
            "org.partiql.ast.expr.ExprError",
            "org.partiql.ast.expr.ExprExtract",
            "org.partiql.ast.expr.ExprInCollection",
            "org.partiql.ast.expr.ExprIsType",
            "org.partiql.ast.expr.ExprLike",
            "org.partiql.ast.expr.ExprLit",
            "org.partiql.ast.expr.ExprMap",
            "org.partiql.ast.expr.ExprMap.Entry",
            "org.partiql.ast.expr.ExprMatch",
            "org.partiql.ast.expr.ExprMissingPredicate",
            "org.partiql.ast.expr.ExprNot",
            "org.partiql.ast.expr.ExprNullIf",
            "org.partiql.ast.expr.ExprNullPredicate",
            "org.partiql.ast.expr.ExprOperator",
            "org.partiql.ast.expr.ExprOr",
            "org.partiql.ast.expr.ExprOverlaps",
            "org.partiql.ast.expr.ExprOverlay",
            "org.partiql.ast.expr.ExprParameter",
            "org.partiql.ast.expr.ExprPath",
            "org.partiql.ast.expr.ExprPosition",
            "org.partiql.ast.expr.ExprQuerySet",
            "org.partiql.ast.expr.ExprRowValue",
            "org.partiql.ast.expr.ExprSessionAttribute",
            "org.partiql.ast.expr.ExprStruct",
            "org.partiql.ast.expr.ExprStruct.Field",
            "org.partiql.ast.expr.ExprSubstring",
            "org.partiql.ast.expr.ExprTrim",
            "org.partiql.ast.expr.ExprValues",
            "org.partiql.ast.expr.ExprVarRef",
            "org.partiql.ast.expr.ExprVariant",
            "org.partiql.ast.expr.ExprWindow",
            "org.partiql.ast.expr.ExprWindow.Over",
            "org.partiql.ast.expr.ExprWindowFunction",
            "org.partiql.ast.expr.PathStep.AllElements",
            "org.partiql.ast.expr.PathStep.AllFields",
            "org.partiql.ast.expr.PathStep.Element",
            "org.partiql.ast.expr.PathStep.Field",
            "org.partiql.ast.expr.SessionAttribute",
            "org.partiql.ast.expr.TrimSpec",
            "org.partiql.ast.expr.TruthValue",
            "org.partiql.ast.expr.WindowFunction",
            "org.partiql.ast.graph.GraphDirection",
            "org.partiql.ast.graph.GraphLabel.Conj",
            "org.partiql.ast.graph.GraphLabel.Disj",
            "org.partiql.ast.graph.GraphLabel.Name",
            "org.partiql.ast.graph.GraphLabel.Negation",
            "org.partiql.ast.graph.GraphLabel.Wildcard",
            "org.partiql.ast.graph.GraphMatch",
            "org.partiql.ast.graph.GraphPart.Edge",
            "org.partiql.ast.graph.GraphPart.Node",
            "org.partiql.ast.graph.GraphPart.Pattern",
            "org.partiql.ast.graph.GraphPattern",
            "org.partiql.ast.graph.GraphQuantifier",
            "org.partiql.ast.graph.GraphRestrictor",
            "org.partiql.ast.graph.GraphSelector.AllShortest",
            "org.partiql.ast.graph.GraphSelector.Any",
            "org.partiql.ast.graph.GraphSelector.AnyK",
            "org.partiql.ast.graph.GraphSelector.AnyShortest",
            "org.partiql.ast.graph.GraphSelector.ShortestK",
            "org.partiql.ast.graph.GraphSelector.ShortestKGroup",
        )
    }

    // endregion
}
