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

package org.partiql.lang.syntax

import com.amazon.ion.IonSexp
import com.amazon.ionelement.api.IonElement
import com.amazon.ionelement.api.SexpElement
import com.amazon.ionelement.api.toIonElement
import com.amazon.ionelement.api.toIonValue
import org.partiql.lang.TestBase
import org.partiql.lang.ast.AstDeserializerBuilder
import org.partiql.lang.ast.AstSerializer
import org.partiql.lang.ast.AstVersion
import org.partiql.lang.ast.ExprNode
import org.partiql.lang.ast.passes.MetaStrippingRewriter
import org.partiql.lang.ast.toAstStatement
import org.partiql.lang.ast.toExprNode
import org.partiql.lang.domains.PartiqlAst
import org.partiql.lang.errors.ErrorCode
import org.partiql.lang.errors.Property
import org.partiql.lang.util.asIonSexp
import org.partiql.lang.util.filterMetaNodes
import org.partiql.lang.util.softAssert
import org.partiql.pig.runtime.toIonElement

abstract class SqlParserTestBase : TestBase() {
    protected val parser = SqlParser(ion)

    protected fun parse(source: String): PartiqlAst.Statement = parser.parseAstStatement(source)

    /**
     * This method is used by test cases, to test with PIG AST, while the expected PIG AST is a string
     */
    protected fun assertExpression(
        source: String,
        expectedSexpAst: String
    ) {
        val actualStatement = parse(source)
        val expectedIonSexp = loadIonSexp(expectedSexpAst)
        val expectedElement = expectedIonSexp.toIonElement().asSexp()

        // Check equal for actual and expected in IonSexp format
        partiqlAssert(actualStatement, expectedIonSexp, source)

        // Check equal for actual and expected in transformed astStatement: astStatment -> SexpElement -> astStatement
        // Check equal for actual and expected in SexpElement format
        // Check equal for actual and expected in deprecated ExprNode format
        // Check round trip for actual: SexpElement -> astStatement -> SexpElement
        // Check round trip for actual: astStatement -> SexpElement -> astStatement
        pigDomainAssert(actualStatement, expectedElement)

        // Check round trip for actual: astStatement -> ExprNode -> astStatement
        roundTripAstStatementToExprNode(actualStatement)
    }

    /**
     * This method is used by test cases, to test with PIG AST, while the expected PIG AST is a PIG builder
     */
    protected fun assertExpression(
        source: String,
        expectedPigBuilder: PartiqlAst.Builder.() -> PartiqlAst.PartiqlAstNode
    ) {
        val expectedPigAst = PartiqlAst.build { expectedPigBuilder() }.toIonElement().toString()

        assertExpression(source, expectedPigAst)
    }

    /**
     * This method is used by test cases, to test with PIG AST and V0 AST, while the expected PIG AST is a string
     */
    protected fun assertExpression(
        source: String,
        expectedV0Ast: String,
        expectedPigAst: String
    ) {
        // Perform checks with V0 AST
        val actualStatement = parse(source)
        val expectedV0IonSexp = loadIonSexp(expectedV0Ast)
        serializeAssert(AstVersion.V0, actualStatement.toExprNode(ion), expectedV0IonSexp, source)

        // Perform checks with PIG AST
        assertExpression(source, expectedPigAst)
    }

    /**
     * This method is used by test cases, to test with PIG AST and V0 AST, where the expected PIG AST is a PIG builder
     */
    protected fun assertExpression(
        source: String,
        expectedSexpAstV0: String,
        expectedPigBuilder: PartiqlAst.Builder.() -> PartiqlAst.PartiqlAstNode
    ) {
        val expectedPigAst = PartiqlAst.build { expectedPigBuilder() }.toIonElement().toString()

        assertExpression(source, expectedSexpAstV0, expectedPigAst)
    }

    /**
     * Check equal for expected and actual in format of V0 AST
     */
    private fun serializeAssert(astVersion: AstVersion, actualExprNode: ExprNode, expectedSexpAst: IonSexp, source: String) {

        val actualSexpAstWithoutMetas = AstSerializer.serialize(actualExprNode, astVersion, ion).filterMetaNodes()

        val deserializer = AstDeserializerBuilder(ion).build()
        assertSexpEquals(expectedSexpAst, actualSexpAstWithoutMetas, "$astVersion AST, $source")

        val deserializedExprNodeFromSexp = deserializer.deserialize(expectedSexpAst, astVersion)

        assertEquals(
            "Parsed ExprNodes must match deserialized s-exp $astVersion AST",
            actualExprNode.stripMetas(),
            deserializedExprNodeFromSexp.stripMetas())
    }

    /**
     * Converts the given PartiqlAst.Statement into an IonElement. If the given [statement] is a query, extracts
     * just the expr component to be compatible with the SqlParser tests.
     */
    private fun unwrapQuery(statement: PartiqlAst.Statement) : SexpElement {
       return when (statement) {
           is PartiqlAst.Statement.Query -> statement.expr.toIonElement()
           is PartiqlAst.Statement.Dml -> statement.toIonElement()
           is PartiqlAst.Statement.Ddl -> statement.toIonElement()
           is PartiqlAst.Statement.Exec -> statement.toIonElement()
        }
    }

    /**
     * Performs checks similar to that of [serializeAssert]. First checks that parsing the [source] query string to
     * a [PartiqlAst] and to an IonValue Sexp equals the [expectedIonSexp].
     */
    private fun partiqlAssert(actualStatement: PartiqlAst.Statement, expectedIonSexp: IonSexp, source: String) {
        val actualElement = unwrapQuery(actualStatement)
        val actualIonSexp = actualElement.toIonElement().asAnyElement().toIonValue(ion)

        assertSexpEquals(expectedIonSexp, actualIonSexp, "AST, $source")
    }

    private fun pigDomainAssert(actualStatement: PartiqlAst.Statement, expectedElement: SexpElement) {
        // Test cases are missing (query <expr>) wrapping, so extract <expr>
        val actualElement = unwrapQuery(actualStatement)

        // Check equals for actual and expected in transformed statement: astStatement -> SexpElement -> astStatement
        // Check round trip for actual: SexpElement -> astStatement -> SexpElement
        assertRoundTripIonElementToPartiQlAst(actualElement, expectedElement)

        // Check equals for actual and expected in SexpElement format
        // Check round trip for actual: astStatement -> SexpElement -> astStatement
        // Check equals in deprecated ExprNode format
        assertRoundTripPartiQlAstToExprNode(actualStatement, expectedElement)
    }

    private fun assertRoundTripPartiQlAstToExprNode(actualStatement: PartiqlAst.Statement, expectedElement: IonElement) {
        // Run additional checks on the resulting PartiqlAst instance

        // None of our test cases are wrapped in (query <expr>), so extract <expr> from that out
        val actualElement = unwrapQuery(actualStatement)
        assertEquals(expectedElement, actualElement)

        // Convert the IonElement back to the PartiqlAst instance and assert equivalence
        val transformedActualStatement = PartiqlAst.transform(actualStatement.toIonElement()) as PartiqlAst.Statement
        assertEquals(actualStatement, transformedActualStatement)

        // Check them in deprecated ExprNode format
        val actualExprNode = actualStatement.toExprNode(ion).stripMetas()
        val transformedActualExprNode = transformedActualStatement.toExprNode(ion).stripMetas()
        assertEquals(actualExprNode, transformedActualExprNode)
    }

    private fun assertRoundTripIonElementToPartiQlAst(actualElement: SexpElement, expectedElement: SexpElement) {
        // #1 We can transform the actual PartiqlAst element.
        val transformedActualStatement = PartiqlAst.transform(actualElement)

        // #2 We can transform the expected PartiqlAst element.
        val transformedExpectedStatement = PartiqlAst.transform(expectedElement)

        // #3 The results of both transformations match.
        assertEquals(transformedExpectedStatement, transformedActualStatement)

        // #4 Re-transforming the parsed PartiqlAst element and check if it matches the expected AST.
        val reserializedActualElement = transformedActualStatement.toIonElement()
        assertEquals(expectedElement, reserializedActualElement)
    }

    /**
     * Round-trip the resulting [statement] AST through [toExprNode] and [toAstStatement].
     *
     * Verify that the result matches the original without metas.
     */
    private fun roundTripAstStatementToExprNode(statement: PartiqlAst.Statement) =
        assertEquals(statement, statement.toExprNode(ion).toAstStatement())

    private fun loadIonSexp(expectedSexpAst: String) = ion.singleValue(expectedSexpAst).asIonSexp()
    private fun ExprNode.stripMetas() = MetaStrippingRewriter.stripMetas(this)

    protected fun checkInputThrowingParserException(input: String,
                                                  errorCode: ErrorCode,
                                                  expectErrorContextValues: Map<Property, Any>) {

        softAssert {
            try {
                parser.parseAstStatement(input)
                fail("Expected ParserException but there was no Exception")
            }
            catch (pex: ParserException) {
                checkErrorAndErrorContext(errorCode, pex, expectErrorContextValues)
            }
            catch (ex: Exception) {
                fail("Expected ParserException but a different exception was thrown \n\t  $ex")
            }
        }
    }
}
