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

import com.amazon.ion.system.*
import org.partiql.lang.ast.*
import org.partiql.lang.syntax.*
import junitparams.*
import org.junit.*
import org.junit.Test
import org.junit.runner.*
import kotlin.test.*

/**
 * [AstWalker] simply traverses each node in the [ExprNode] instance but performs no transformations.
 *
 * This test class simply ensures that the expected node types are visited given some arbitrary query.
 */
@RunWith(JUnitParamsRunner::class)
class AstWalkerTests {

    // these tests are duplicated on AstNodeTest but kept here until we delete AstWalker
    
    companion object {

        /** A dummy visitor that simply appends the type name of each node to a StringBuilder to that
         * a "trace" of every node can be recorded. */
        class TestVisitor : AstVisitor {
            private val walkTrace = StringBuilder()

            override fun visitExprNode(expr: ExprNode) {
                appendNodeType(expr)
            }

            override fun visitSelectProjection(projection: SelectProjection) {
                appendNodeType(projection)
            }

            override fun visitSelectListItem(selectListItem: SelectListItem) {
                appendNodeType(selectListItem)
            }

            override fun visitFromSource(fromSource: FromSource) {
                appendNodeType(fromSource)
            }

            override fun visitPathComponent(pathComponent: PathComponent) {
                appendNodeType(pathComponent)
            }

            override fun visitDataType(dataType: DataType) {
                appendNodeType(dataType)
            }

            private fun appendNodeType(node: Any) {
                walkTrace.append(node.javaClass.simpleName)
                walkTrace.append("|")
            }

            override fun visitDataManipulationOperation(dmlOp: DataManipulationOperation) {
                appendNodeType(dmlOp)
            }

            val trace: String get() = walkTrace.toString()
        }
    }

    private val ion = IonSystemBuilder.standard().build()
    private val parser = SqlParser(ion)

    class WalkerTestCase(val sql: String, val expectedTrace: String)

    @Test
    @Parameters
    fun walkerTest(testCase: WalkerTestCase) {
        val ast = parser.parseExprNode(testCase.sql)

        val visitor = TestVisitor()
        val walker = AstWalker(visitor)
        walker.walk(ast)

        assertEquals(testCase.expectedTrace, visitor.trace, "Query was: ${testCase.sql}\n")
    }

    fun parametersForWalkerTest() = listOf(
        WalkerTestCase(
            "MISSING",
            "LiteralMissing|"),
        WalkerTestCase(
            "1",
            "Literal|"),
        WalkerTestCase(
            "1 + 1",
            "NAry|Literal|Literal|"),
        WalkerTestCase(
            "[1, 2]",
            "Seq|Literal|Literal|"),
        WalkerTestCase(
            "{ 'fooField': 1 }",
            "Struct|Literal|Literal|"),
        WalkerTestCase(
            "a.b.c",
            "Path|VariableReference|PathComponentExpr|Literal|PathComponentExpr|Literal|"),
        WalkerTestCase(
            "a[b].c",
            "Path|VariableReference|PathComponentExpr|VariableReference|PathComponentExpr|Literal|"),
        WalkerTestCase(
            "a[1].c",
            "Path|VariableReference|PathComponentExpr|Literal|PathComponentExpr|Literal|"),
        WalkerTestCase(
            "a[*].c",
            "Path|VariableReference|PathComponentWildcard|PathComponentExpr|Literal|"),
        WalkerTestCase(
            "a.*.c",
            "Path|VariableReference|PathComponentUnpivot|PathComponentExpr|Literal|"),
        WalkerTestCase(
            "fcall(var1, var2)",
            "NAry|VariableReference|VariableReference|VariableReference|"),
        WalkerTestCase(
            "CASE foo WHEN 1 THEN 10 ELSE 11 END",
            "SimpleCase|VariableReference|Literal|Literal|Literal|"),
        WalkerTestCase(
            "CASE WHEN 1 THEN 10 ELSE 11 END",
            "SearchedCase|Literal|Literal|Literal|"),
        WalkerTestCase(
            "SELECT * FROM foo",
            "Select|SelectProjectionList|SelectListItemStar|FromSourceExpr|VariableReference|"),
        WalkerTestCase(
            "SELECT * FROM foo, bar",
            //Reminder:  this yields the same AST as: ... FROM foo INNER JOIN bar ON true
            "Select|SelectProjectionList|SelectListItemStar|FromSourceJoin|FromSourceExpr|VariableReference|FromSourceExpr|VariableReference|Literal|"),
        WalkerTestCase(
            "SELECT * FROM foo, bar",
            "Select|SelectProjectionList|SelectListItemStar|FromSourceJoin|FromSourceExpr|VariableReference|FromSourceExpr|VariableReference|Literal|"),
        WalkerTestCase(
            "SELECT * FROM foo INNER JOIN bar ON condition",
            "Select|SelectProjectionList|SelectListItemStar|FromSourceJoin|FromSourceExpr|VariableReference|FromSourceExpr|VariableReference|VariableReference|"),
        WalkerTestCase(
            "SELECT f.* FROM foo AS f",
            "Select|SelectProjectionList|SelectListItemProjectAll|VariableReference|FromSourceExpr|VariableReference|"),
        WalkerTestCase(
            "SELECT VALUE foo FROM bar",
            "Select|SelectProjectionValue|VariableReference|FromSourceExpr|VariableReference|"),
        WalkerTestCase(
            "PIVOT 1 AT 2 FROM 3",
            "Select|SelectProjectionPivot|Literal|Literal|FromSourceExpr|Literal|"),
        WalkerTestCase(
            "CREATE TABLE FOO",
            "CreateTable|"),
        WalkerTestCase(
            "?",
            "Parameter|"),

        WalkerTestCase("MISSING", "LiteralMissing|"),

        WalkerTestCase("SELECT a FROM tb WHERE hk = 1 ORDER BY rk DESC", "Select|SelectProjectionList|SelectListItemExpr|VariableReference|FromSourceExpr|VariableReference|NAry|VariableReference|Literal|VariableReference|"),
        WalkerTestCase("INSERT INTO foo VALUE 1 ON CONFLICT WHERE bar DO NOTHING", "DataManipulation|VariableReference|Literal|VariableReference|")

    )

}