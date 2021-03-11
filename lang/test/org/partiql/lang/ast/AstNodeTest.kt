package org.partiql.lang.ast

import com.amazon.ion.*
import com.amazon.ion.system.*
import junitparams.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.*
import org.partiql.lang.syntax.*

// TODO:  add tests for DDL & DML ExprNodes.

@RunWith(JUnitParamsRunner::class)
class AstNodeTest {
    // This test builds some invalid AST nodes but since we are focusing on testing AstNode interface we only care
    // about the node and it's children so it's ok

    private val ion: IonSystem = IonSystemBuilder.standard().build()
    private val emptyMeta = metaContainerOf()

    private fun <T> Iterator<T>.toList() = this.asSequence().toList()
    private fun literal(value: String) = Literal(ion.singleValue(value), emptyMeta)

    @Test
    fun leafNodeNoPotentialChildren() {
        // A Literal node can't have any children
        val leafNode = literal("1")

        assertTrue(leafNode.children.isEmpty())
        assertEquals(listOf(leafNode), leafNode.iterator().toList())
    }

    @Test
    fun leafNodeWithPotentialChildren() {
        // An NAry node can have any children.
        val leafNode = NAry(NAryOp.ADD, listOf(), emptyMeta)

        assertTrue(leafNode.children.isEmpty())
        assertEquals(listOf(leafNode), leafNode.iterator().toList())
    }

    @Test
    fun nodeWithSingleChildrenLeaf() {
        val childNode = NAry(NAryOp.ADD, listOf(), emptyMeta)
        val rootNode = NAry(NAryOp.ADD, listOf(childNode), emptyMeta)

        assertEquals(listOf(childNode), rootNode.children)
        assertEquals(listOf(rootNode, childNode), rootNode.iterator().toList())
    }

    @Test
    fun nodeWithMultipleChildren() {
        val childNode1 = NAry(NAryOp.ADD, listOf(), emptyMeta)
        val childNode2 = NAry(NAryOp.ADD, listOf(), emptyMeta)
        val rootNode = NAry(NAryOp.ADD, listOf(childNode1, childNode2), emptyMeta)

        assertEquals(listOf(childNode1, childNode2), rootNode.children)
        assertEquals(listOf(rootNode, childNode1, childNode2), rootNode.iterator().toList())
    }

    class IteratorTestCase(val sql: String, val expectedTrace: String)

    @Test
    @Parameters
    fun iteratorTests(testCase: IteratorTestCase) {
        val ast = SqlParser(ion).parseExprNode(testCase.sql)

        val actual = ast.joinToString("|") { it.javaClass.simpleName }

        assertEquals(testCase.expectedTrace, actual)
    }

    fun parametersForIteratorTests() = listOf(
        IteratorTestCase(
            "MISSING",
            "LiteralMissing"),
        IteratorTestCase(
            "1",
            "Literal"),
        IteratorTestCase(
            "1 + 1",
            "NAry|Literal|Literal"),
        IteratorTestCase(
            "[1, 2]",
            "Seq|Literal|Literal"),
        IteratorTestCase(
            "{ 'fooField': 1 }",
            "Struct|StructField|Literal|Literal"),
        IteratorTestCase(
            "a.b.c",
            "Path|VariableReference|PathComponentExpr|Literal|PathComponentExpr|Literal"),
        IteratorTestCase(
            "a[b].c",
            "Path|VariableReference|PathComponentExpr|VariableReference|PathComponentExpr|Literal"),
        IteratorTestCase(
            "a[1].c",
            "Path|VariableReference|PathComponentExpr|Literal|PathComponentExpr|Literal"),
        IteratorTestCase(
            "a[*].c",
            "Path|VariableReference|PathComponentWildcard|PathComponentExpr|Literal"),
        IteratorTestCase(
            "a.*.c",
            "Path|VariableReference|PathComponentUnpivot|PathComponentExpr|Literal"),
        IteratorTestCase(
            "fcall(var1, var2)",
            "NAry|VariableReference|VariableReference|VariableReference"),
        IteratorTestCase(
            "CASE foo WHEN 1 THEN 10 ELSE 11 END",
            "SimpleCase|VariableReference|SimpleCaseWhen|Literal|Literal|Literal"),
        IteratorTestCase(
            "CASE WHEN 1 THEN 10 ELSE 11 END",
            "SearchedCase|SearchedCaseWhen|Literal|Literal|Literal"),
        IteratorTestCase(
            "SELECT * FROM foo",
            "Select|SelectProjectionList|SelectListItemStar|FromSourceExpr|VariableReference"),
        IteratorTestCase(
            "SELECT * FROM foo, bar",
            //Reminder:  this yields the same AST as: ... FROM foo INNER JOIN bar ON true
            "Select|SelectProjectionList|SelectListItemStar|FromSourceJoin|FromSourceExpr|VariableReference|FromSourceExpr|VariableReference|Literal"),
        IteratorTestCase(
            "SELECT * FROM foo, bar",
            "Select|SelectProjectionList|SelectListItemStar|FromSourceJoin|FromSourceExpr|VariableReference|FromSourceExpr|VariableReference|Literal"),
        IteratorTestCase(
            "SELECT * FROM foo WHERE bar",
            "Select|SelectProjectionList|SelectListItemStar|FromSourceExpr|VariableReference|VariableReference"),
        IteratorTestCase(
            "SELECT * FROM foo INNER JOIN bar ON condition",
            "Select|SelectProjectionList|SelectListItemStar|FromSourceJoin|FromSourceExpr|VariableReference|FromSourceExpr|VariableReference|VariableReference"),
        IteratorTestCase(
            "SELECT f.* FROM foo AS f",
            "Select|SelectProjectionList|SelectListItemProjectAll|VariableReference|FromSourceExpr|VariableReference"),
        IteratorTestCase(
            "SELECT VALUE foo FROM bar",
            "Select|SelectProjectionValue|VariableReference|FromSourceExpr|VariableReference"),
        IteratorTestCase(
            "PIVOT 1 AT 2 FROM 3",
            "Select|SelectProjectionPivot|Literal|Literal|FromSourceExpr|Literal"),
        IteratorTestCase(
            "INSERT INTO foo VALUES (1)",
            "DataManipulation|InsertOp|VariableReference|Seq|Seq|Literal|DmlOpList|InsertOp|VariableReference|Seq|Seq|Literal"),
        IteratorTestCase(
            "UPDATE foo SET x.y = bar WHERE n",
            "DataManipulation|AssignmentOp|Assignment|Path|VariableReference|PathComponentExpr|Literal|VariableReference|FromSourceExpr|VariableReference|VariableReference|DmlOpList|AssignmentOp|Assignment|Path|VariableReference|PathComponentExpr|Literal|VariableReference"),
        IteratorTestCase(
            "FROM x IN Y REMOVE p",
            "DataManipulation|RemoveOp|VariableReference|FromSourceExpr|NAry|VariableReference|VariableReference|DmlOpList|RemoveOp|VariableReference"),
        IteratorTestCase(
            "DELETE FROM foo WHERE bar",
            "DataManipulation|DeleteOp|FromSourceExpr|VariableReference|VariableReference|DmlOpList|DeleteOp"),
        IteratorTestCase(
            "CREATE TABLE foo",
            "CreateTable"),
        IteratorTestCase(
            "DROP TABLE foo",
            "DropTable"),

        IteratorTestCase("MISSING", "LiteralMissing"))

    @Test
    fun nodeWithMultipleNonLeafChildren() {
        val depth2_1 = NAry(NAryOp.ADD, listOf(), emptyMeta)
        val depth2_2 = NAry(NAryOp.ADD, listOf(), emptyMeta)

        val depth1_1 = NAry(NAryOp.ADD, listOf(depth2_1), emptyMeta)
        val depth1_2 = NAry(NAryOp.ADD, listOf(), emptyMeta)
        val depth1_3 = NAry(NAryOp.ADD, listOf(depth2_2), emptyMeta)
        val depth0 = NAry(NAryOp.ADD, listOf(depth1_1, depth1_2, depth1_3), emptyMeta)

        assertEquals(listOf(depth1_1, depth1_2, depth1_3), depth0.children)
        assertEquals(listOf(depth0, depth1_1, depth2_1, depth1_2, depth1_3, depth2_2), depth0.iterator().toList())
    }

    @Test
    fun literalChildren() = assertTrue(Literal(ion.newNull(), emptyMeta).children.isEmpty())

    @Test
    fun literalMissingChildren() = assertTrue(LiteralMissing(emptyMeta).children.isEmpty())

    @Test
    fun variableReferenceChildren() = assertTrue(VariableReference("",
                                                                   CaseSensitivity.INSENSITIVE,
                                                                   ScopeQualifier.LEXICAL,
                                                                   emptyMeta).children.isEmpty())

    @Test
    fun nAryChildren() {
        val expected = listOf(literal("1"), literal("2"))

        assertEquals(expected, NAry(NAryOp.ADD, expected, emptyMeta).children)
    }

    @Test
    fun callAggChildren() {
        val child1 = literal("1")
        val child2 = literal("2")

        assertEquals(listOf(child1, child2), CallAgg(child1, SetQuantifier.ALL, child2, emptyMeta).children)
    }

    @Test
    fun typedChildren() {
        val child1 = literal("1")
        val child2 = DataType(SqlDataType.BAG, listOf(), emptyMeta)

        assertEquals(listOf(child1, child2), Typed(TypedOp.CAST, child1, child2, emptyMeta).children)
    }

    @Test
    fun pathChildren() {
        val root = literal("1")
        val component1 = PathComponentExpr(literal("2"), CaseSensitivity.INSENSITIVE)
        val component2 = PathComponentExpr(literal("3"), CaseSensitivity.INSENSITIVE)

        assertEquals(listOf(root, component1, component2),
                     Path(root, listOf(component1, component2), emptyMeta).children)
    }

    @Test
    fun simpleCaseChildren() {
        val value = literal("1")
        val whenClause1 = SimpleCaseWhen(literal("21"), literal("22"))
        val whenClause2 = SimpleCaseWhen(literal("31"), literal("32"))
        assertEquals(listOf(value, whenClause1, whenClause2),
                     SimpleCase(value, listOf(whenClause1, whenClause2), null, emptyMeta).children)
    }

    @Test
    fun simpleCaseWithElseChildren() {
        val value = literal("1")
        val whenClause1 = SimpleCaseWhen(literal("21"), literal("22"))
        val whenClause2 = SimpleCaseWhen(literal("31"), literal("32"))
        val elseExpr = literal("4")
        assertEquals(listOf(value, whenClause1, whenClause2, elseExpr),
                     SimpleCase(value, listOf(whenClause1, whenClause2), elseExpr, emptyMeta).children)
    }

    @Test
    fun simpleCaseWhenChildren() {
        val child1 = literal("1")
        val child2 = literal("2")

        assertEquals(listOf(child1, child2), SimpleCaseWhen(child1, child2).children)
    }

    @Test
    fun searchedCaseChildren() {
        val searchedCaseWhen1 = SearchedCaseWhen(literal("11"), literal("12"))
        val searchedCaseWhen2 = SearchedCaseWhen(literal("21"), literal("22"))

        assertEquals(listOf(searchedCaseWhen1, searchedCaseWhen2),
                     SearchedCase(listOf(searchedCaseWhen1, searchedCaseWhen2), null, emptyMeta).children)
    }

    @Test
    fun searchedCaseWithElseChildren() {
        val searchedCaseWhen1 = SearchedCaseWhen(literal("11"), literal("12"))
        val searchedCaseWhen2 = SearchedCaseWhen(literal("21"), literal("22"))
        val elseExpr = literal("3")

        assertEquals(listOf(searchedCaseWhen1, searchedCaseWhen2, elseExpr),
                     SearchedCase(listOf(searchedCaseWhen1, searchedCaseWhen2), elseExpr, emptyMeta).children)
    }

    @Test
    fun searchedCaseWhenChildren() {
        val child1 = literal("1")
        val child2 = literal("2")

        assertEquals(listOf(child1, child2), SearchedCaseWhen(child1, child2).children)
    }

    @Test
    fun selectChildren() {
        val projection = SelectProjectionValue(literal("1"))
        val from = FromSourceExpr(literal("2"), LetVariables())

        assertEquals(listOf(projection, from),
                     Select(SetQuantifier.ALL, projection, from, null, null, null, null, null, null, emptyMeta).children)
    }

    @Test
    fun selectWithAllChildren() {
        val projection = SelectProjectionValue(literal("1"))
        val from = FromSourceExpr(literal("2"), LetVariables())
        val fromLet = LetSource(emptyList())
        val where = literal("3")
        val groupBy = GroupBy(GroupingStrategy.FULL, listOf())
        val having = literal("4")
        val orderBy = OrderBy(listOf())
        val limit = literal("5")

        assertEquals(listOf(projection, from, fromLet, where, groupBy, having, orderBy, limit),
                     Select(SetQuantifier.ALL, projection, from, fromLet, where, groupBy, having, orderBy, limit, emptyMeta).children)
    }

    @Test
    fun symbolicNameChildren() = assertTrue(SymbolicName("", emptyMeta).children.isEmpty())

    @Test
    fun pathComponentExprChildren() {
        val child = literal("2")
        assertEquals(listOf(child), PathComponentExpr(child, CaseSensitivity.INSENSITIVE).children)
    }

    @Test
    fun pathComponentUnpivotChildren() = assertTrue(PathComponentUnpivot(emptyMeta).children.isEmpty())

    @Test
    fun pathComponentWildcardChildren() = assertTrue(PathComponentWildcard(emptyMeta).children.isEmpty())

    @Test
    fun selectProjectionListChildren() {
        val child1 = SelectListItemExpr(literal("1"))
        val child2 = SelectListItemExpr(literal("2"))
        assertEquals(listOf(child1, child2), SelectProjectionList(listOf(child1, child2)).children)
    }

    @Test
    fun selectProjectionValueChildren() {
        val child = literal("1")

        assertEquals(listOf(child), SelectProjectionValue(child).children)
    }

    @Test
    fun selectProjectionPivotChildren() {
        val child1 = literal("1")
        val child2 = literal("2")

        assertEquals(listOf(child1, child2), SelectProjectionPivot(child1, child2).children)
    }

    @Test
    fun selectListItemExprChildren() {
        val child = literal("1")

        assertEquals(listOf(child), SelectListItemExpr(child).children)
    }

    @Test
    fun selectListItemProjectAllChildren() {
        val child = literal("1")

        assertEquals(listOf(child), SelectListItemProjectAll(child).children)
    }

    @Test
    fun selectListItemStarChildren() = assertTrue(SelectListItemStar(emptyMeta).children.isEmpty())

    @Test
    fun fromSourceExprChildren() {
        val child = literal("1")

        assertEquals(listOf(child), FromSourceExpr(child, LetVariables()).children)
    }

    @Test
    fun fromSourceJoinChildren() {
        val child1 = FromSourceExpr(literal("1"), LetVariables())
        val child2 = FromSourceExpr(literal("2"), LetVariables())
        val child3 = literal("3")

        assertEquals(listOf(child1, child2, child3),
                     FromSourceJoin(JoinOp.INNER, child1, child2, child3, emptyMeta).children)
    }

    @Test
    fun fromSourceUnpivotChildren() {
        val child = literal("1")

        assertEquals(listOf(child), FromSourceUnpivot(child, LetVariables(), emptyMeta).children)
    }

    @Test
    fun groupByChildren() {
        val child1 = GroupByItem(literal("1"))
        val child2 = GroupByItem(literal("2"))

        assertEquals(listOf(child1, child2), GroupBy(GroupingStrategy.FULL, listOf(child1, child2), null).children)
    }

    @Test
    fun groupByItemChildren() {
        val child = literal("1")

        assertEquals(listOf(child), GroupByItem(child).children)
    }

    @Test
    fun structFieldChildren() {
        val child1 = literal("1")
        val child2 = literal("2")

        assertEquals(listOf(child1, child2), StructField(child1, child2).children)
    }

    @Test
    fun structChildren() {
        val child1 = StructField(literal("11"), literal("12"))
        val child2 = StructField(literal("21"), literal("22"))

        assertEquals(listOf(child1, child2), Struct(listOf(child1, child2), emptyMeta).children)
    }

    @Test
    fun seqExprNodeChildren() {
        val child1 = literal("2")
        val child2 = literal("2")

        assertEquals(listOf(child1, child2), Seq(SeqType.LIST, listOf(child1, child2), emptyMeta).children)
    }

    @Test
    fun dataTypeChildren() = assertTrue(DataType(SqlDataType.BAG, listOf(), emptyMeta).children.isEmpty())
}
