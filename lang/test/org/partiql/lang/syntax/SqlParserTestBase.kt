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
import org.partiql.lang.CUSTOM_TEST_TYPES_MAP
import org.partiql.lang.checkErrorAndErrorContext
import org.partiql.lang.domains.PartiqlAst
import org.partiql.lang.errors.ErrorCode
import org.partiql.lang.errors.Property
import org.partiql.lang.util.asIonSexp
import org.partiql.lang.util.filterMetaNodes
import org.partiql.lang.util.softAssert
import org.partiql.pig.runtime.toIonElement

abstract class SqlParserTestBase : TestBase() {
    protected val parser = SqlParser(ion, CUSTOM_TEST_TYPES_MAP)

    protected fun parse(source: String): PartiqlAst.Statement = parser.parseAstStatement(source)
    protected fun parseToExprNode(source: String): ExprNode = parser.parseExprNode(source)

    /**
     * This method is used by test cases for parsing a string.
     * The test are performed with only PIG AST.
     * The expected PIG AST is a string.
     */
    protected fun assertExpression(
        source: String,
        expectedPigAst: String
    ) {
        val actualStatement = parse(source)
        val expectedIonSexp = loadIonSexp(expectedPigAst)

        // Check equals for actual value and expected value in IonSexp format
        checkEqualInIonSexp(actualStatement, expectedIonSexp, source)

        val expectedElement = expectedIonSexp.toIonElement().asSexp()

        // Perform checks for Pig AST. See the comments inside the function to see what checks are performed.
        pigDomainAssert(actualStatement, expectedElement)

        // Check equals for actual value after round trip transformation: astStatement -> ExprNode -> astStatement
        assertRoundTripPigAstToExprNode(actualStatement)
    }

    /**
     * This method is used by test cases for parsing a string.
     * The test are performed with only PIG AST.
     * The expected PIG AST is a PIG builder.
     */
    protected fun assertExpression(
        source: String,
        expectedPigBuilder: PartiqlAst.Builder.() -> PartiqlAst.PartiqlAstNode
    ) {
        val expectedPigAst = PartiqlAst.build { expectedPigBuilder() }.toIonElement().toString()

        // Refer to comments inside the main body of the following function to see what checks are performed.
        assertExpression(source, expectedPigAst)
    }

    /**
     * This method is used by test cases for parsing a string.
     * The test are performed with both PIG AST and V0 AST.
     * The expected PIG AST is a string.
     */
    protected fun assertExpression(
        source: String,
        expectedV0Ast: String,
        expectedPigAst: String
    ) {
        // Check for V0 Ast
        val actualStatement = parse(source)
        val expectedV0AstSexp = loadIonSexp(expectedV0Ast)
        serializeAssert(AstVersion.V0, actualStatement.toExprNode(ion), expectedV0AstSexp, source)

        // Check for PIG Ast
        assertExpression(source, expectedPigAst)
    }

    /**
     * This method is used by test cases for parsing a string.
     * The test are performed with both PIG AST and V0 AST.
     * The expected PIG AST is a PIG builder.
     */
    protected fun assertExpression(
        source: String,
        expectedSexpAstV0: String,
        expectedPigBuilder: PartiqlAst.Builder.() -> PartiqlAst.PartiqlAstNode
    ) {
        val expectedPigAst = PartiqlAst.build { expectedPigBuilder() }.toIonElement().toString()

        // Refer to comments inside the main body of the following function to see what checks are performed.
        assertExpression(source, expectedSexpAstV0, expectedPigAst)
    }

    private fun serializeAssert(astVersion: AstVersion, actualExprNode: ExprNode, expectedIonSexp: IonSexp, source: String) {
        // Check equals for actual value and expected value after transformation: ExprNode -> IonSexp
        val actualSexpAstWithoutMetas = AstSerializer.serialize(actualExprNode, astVersion, ion).filterMetaNodes()
        assertSexpEquals(expectedIonSexp, actualSexpAstWithoutMetas, "$astVersion AST, $source")

        // Check equals for actual value and expected value after transformation: IonSexp -> ExprNode
        val deserializer = AstDeserializerBuilder(ion).build()
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
    private fun checkEqualInIonSexp(actualStatement: PartiqlAst.Statement, expectedIonSexp: IonSexp, source: String) {
        val actualElement = unwrapQuery(actualStatement)
        val actualIonSexp = actualElement.toIonElement().asAnyElement().toIonValue(ion)

        assertSexpEquals(expectedIonSexp, actualIonSexp, "AST, $source")
    }

    private fun pigDomainAssert(actualStatement: PartiqlAst.Statement, expectedElement: SexpElement) {
        // Check equal for actual and expected in SexpElement format
        val actualElement = unwrapQuery(actualStatement)
        assertEquals(expectedElement, actualElement)

        // Check equal after transformation: PIG AST -> SexpElement -> PIG AST
        assertRoundTripPigAstToSexpElement(actualStatement)

        // Check equal for actual and expected in transformed astStatement: astStatement -> SexpElement -> astStatement
        val transformedActualStatement = PartiqlAst.transform(actualElement)
        val transformedExpectedStatement = PartiqlAst.transform(expectedElement)
        assertEquals(transformedExpectedStatement, transformedActualStatement)

        // Check round trip for actual: SexpElement -> astStatement -> SexpElement
        val reserializedActualElement = transformedActualStatement.toIonElement()
        assertEquals(expectedElement, reserializedActualElement)
    }

    /**
     * Check equal after transformation: PIG AST -> SexpElement -> PIG AST
     */
    private fun assertRoundTripPigAstToSexpElement(actualStatement: PartiqlAst.Statement) =
        assertEquals(actualStatement, PartiqlAst.transform(actualStatement.toIonElement()) as PartiqlAst.Statement)

    /**
     * Check equal after transformation: PIG AST -> ExprNode -> PIG AST
     */
    private fun assertRoundTripPigAstToExprNode(actualStatement: PartiqlAst.Statement) =
        assertEquals(actualStatement, actualStatement.toExprNode(ion).toAstStatement())

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
