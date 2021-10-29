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
import com.amazon.ionelement.api.IonElementLoaderOptions
import com.amazon.ionelement.api.SexpElement
import com.amazon.ionelement.api.toIonElement
import com.amazon.ionelement.api.loadSingleElement
import com.amazon.ionelement.api.toIonValue
import org.partiql.lang.TestBase
import org.partiql.lang.ast.AstDeserializerBuilder
import org.partiql.lang.ast.AstSerializer
import org.partiql.lang.ast.AstVersion
import org.partiql.lang.ast.DataManipulation
import org.partiql.lang.ast.ExprNode
import org.partiql.lang.ast.passes.MetaStrippingRewriter
import org.partiql.lang.ast.toAstExpr
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

    protected fun parse(source: String): ExprNode = parser.parseExprNode(source)
    protected fun parseToAst(source: String): PartiqlAst.Statement = parser.parseAstStatement(source)

    protected fun assertExpression(
        source: String,
        expectedPigBuilder: PartiqlAst.Builder.() -> PartiqlAst.PartiqlAstNode
    ) {
        val expectedPigAst = PartiqlAst.build { expectedPigBuilder() }.toIonElement().toString()

        assertExpression(source, expectedPigAst)
    }

    protected fun assertExpression(
        source: String,
        expectedPigAst: String
    ) {
        val actualExprNode = parse(source)
        val expectedIonSexp = loadIonSexp(expectedPigAst)

        checkEqualInIonSexp(actualExprNode, expectedIonSexp, source)

        val expectedElement = expectedIonSexp.toIonElement().asSexp()

        pigDomainAssert(actualExprNode, expectedElement)
        pigExprNodeTransformAsserts(actualExprNode)
    }

    protected fun assertExpression(
        source: String,
        expectedSexpAstV0: String,
        expectedPigBuilder: PartiqlAst.Builder.() -> PartiqlAst.PartiqlAstNode
    ) {
        val expectedPigAst = PartiqlAst.build { expectedPigBuilder() }.toIonElement().toString()

        assertExpression(source, expectedSexpAstV0, expectedPigAst)
    }

    protected fun assertExpression(
        source: String,
        expectedV0Ast: String,
        expectedPigAst: String
    ) {
        // Check for V0 Ast
        val actualExprNode = parse(source)
        val expectedV0AstSexp = loadIonSexp(expectedV0Ast)

        serializeAssert(AstVersion.V0, actualExprNode, expectedV0AstSexp, source)

        // Check for PIG Ast
        assertExpression(source, expectedPigAst)
    }

    private fun serializeAssert(astVersion: AstVersion, actualExprNode: ExprNode, expectedIonSexp: IonSexp, source: String) {

        val actualSexpAstWithoutMetas = AstSerializer.serialize(actualExprNode, astVersion, ion).filterMetaNodes()

        val deserializer = AstDeserializerBuilder(ion).build()
        assertSexpEquals(expectedIonSexp, actualSexpAstWithoutMetas, "$astVersion AST, $source")

        val deserializedExprNodeFromSexp = deserializer.deserialize(expectedIonSexp, astVersion)

        assertEquals(
            "actual ExprNodes must match deserialized s-exp $astVersion AST",
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
           is PartiqlAst.Statement.Dml,
           is PartiqlAst.Statement.Ddl,
           is PartiqlAst.Statement.Exec -> statement.toIonElement()
        }
    }

    /**
     * Performs checks similar to that of [serializeAssert]. First checks that parsing the [source] query string to
     * a [PartiqlAst] and to an IonValue Sexp equals the [expectedIonSexp].
     */
    private fun checkEqualInIonSexp(actualExprNode: ExprNode, expectedIonSexp: IonSexp, source: String) {
        val actualStatment = actualExprNode.toAstStatement()
        val actualElement = unwrapQuery(actualStatment)
        val actualIonSexp = actualElement.toIonElement().asAnyElement().toIonValue(ion)

        assertSexpEquals(expectedIonSexp, actualIonSexp, "AST, $source")
    }

    private fun pigDomainAssert(actualExprNode: ExprNode, expectedSexpAst: SexpElement) {
        // Convert ExprNode into a PartiqlAst statement
        val actualStatement = actualExprNode.toAstStatement()

        // Test cases are missing (query <expr>) wrapping, so extract <expr>
        val actualElement = unwrapQuery(actualStatement)

        assertRoundTripIonElementToPartiQlAst(actualElement, expectedSexpAst)

        // Run the actualExprNode through the conversion to PIG domain instance for all tests
        // to detect conditions which will cause the conversion to throw an exception.
        assertRoundTripPartiQlAstToExprNode(actualStatement, expectedSexpAst, actualExprNode)
    }

    private fun assertRoundTripPartiQlAstToExprNode(actualStatement: PartiqlAst.Statement, expectedSexpAst: IonElement, actualExprNode: ExprNode) {
        // Run additional checks on the resulting PartiqlAst instance

        // None of our test cases are wrapped in (query <expr>), so extract <expr> from that out
        val actualElement = unwrapQuery(actualStatement)
        assertEquals(expectedSexpAst, actualElement)

        // Convert the the IonElement back to the PartiqlAst instance and assert equivalence
        val transformedActualStatement = PartiqlAst.transform(actualStatement.toIonElement()) as PartiqlAst.Statement
        assertEquals(actualStatement, transformedActualStatement)

        // Convert from the PIG instance back to ExprNode and assert the result is the same as actualExprNode.
        val transformedActualExprNode = MetaStrippingRewriter.stripMetas(transformedActualStatement.toExprNode(ion))
        assertEquals(MetaStrippingRewriter.stripMetas(actualExprNode), transformedActualExprNode)
    }

    private fun assertRoundTripIonElementToPartiQlAst(actualElement: SexpElement, expectedElement: SexpElement) {
        // #1 We can transform the actual PartiqlAst element.
        val transformedActualElement = PartiqlAst.transform(actualElement)

        // #2 We can transform the expected PartiqlAst element.
        val transformedExpectedElement = PartiqlAst.transform(expectedElement)

        // #3 The results of both transformations match.
        assertEquals(transformedExpectedElement, transformedActualElement)

        // #4 Re-transforming the actual PartiqlAst element and check if it matches the expected AST.
        val reserializedActualElement = transformedActualElement.toIonElement()
        assertEquals(expectedElement, reserializedActualElement)

        // #5 Re-serializing the expected PartiqlAst element matches the expected AST.
        // Note:  because of #3 above, no need for #5.
    }

    /**
     * Strips metas from the [actualExprNode] so they are not included in equivalence checks
     * and round-trip the resulting [actualExprNode] AST through [toAstStatement] and [toExprNode].
     *
     * Verify that the result matches the original without metas.
     */
    private fun pigExprNodeTransformAsserts(actualExprNode: ExprNode) {
        val actualExprNodeNoMetas = MetaStrippingRewriter.stripMetas(actualExprNode)
        val actualStatement = actualExprNodeNoMetas.toAstStatement()
        val transformedActualExprNode = actualStatement.toExprNode(ion)

        assertEquals(actualExprNodeNoMetas, transformedActualExprNode)
    }

    private fun loadIonSexp(expectedSexpAst: String) = ion.singleValue(expectedSexpAst).asIonSexp()
    private fun ExprNode.stripMetas() = MetaStrippingRewriter.stripMetas(this)

    protected fun checkInputThrowingParserException(input: String,
                                                  errorCode: ErrorCode,
                                                  expectErrorContextValues: Map<Property, Any>) {

        softAssert {
            try {
                parser.parseExprNode(input)
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
