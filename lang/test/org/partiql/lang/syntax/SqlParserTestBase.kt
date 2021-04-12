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
        pigBuilder: PartiqlAst.Builder.() -> PartiqlAst.PartiqlAstNode
    ) {
        val parsedExprNode = parse(source)

        val expectedPartiQlAst = PartiqlAst.build { pigBuilder() }.toIonElement().toString()
        // Convert the query to ExprNode

        val partiqlAst = loadIonSexp(expectedPartiQlAst)
        partiqlAssert(parsedExprNode, partiqlAst, source)

        pigDomainAssert(parsedExprNode, partiqlAst.toIonElement().asSexp())
        pigExprNodeTransformAsserts(parsedExprNode)
    }

    // TODO: refactor the signature with pig builder
    protected fun assertExpression(
            source: String,
            expectedSexpAstAsString: String
    ) {
        val parsedExprNode = parse(source)
        val expectedSexpAst = loadSingleElement(
            expectedSexpAstAsString,
            IonElementLoaderOptions(includeLocationMeta = false)
        ).asSexp()

        val parsedExprNodeIonElement = when (parsedExprNode) {
            is DataManipulation -> parsedExprNode.toAstStatement().toIonElement()
            else -> parsedExprNode.toAstExpr().toIonElement()
        }
        assertRoundTripIonElementToPartiQlAst(parsedExprNodeIonElement, expectedSexpAst)
        assertRoundTripPartiQlAstToExprNode(parsedExprNode.toAstStatement(), expectedSexpAst, parsedExprNode)
        pigExprNodeTransformAsserts(parsedExprNode)
    }

    protected fun assertExpression(
        source: String,
        expectedSexpAstV0String: String,
        pigBuilder: PartiqlAst.Builder.() -> PartiqlAst.PartiqlAstNode
    ) {
        val expectedPartiQlAst = PartiqlAst.build { pigBuilder() }
        assertExpression(source, expectedSexpAstV0String, expectedPartiQlAst.toIonElement().toString())
    }

    protected fun assertExpression(
        source: String,
        expectedSexpAstV0String: String,
        expectedPartiqlAstString: String = expectedSexpAstV0String
    ) {
        // Convert the query to ExprNode
        val parsedExprNode = parse(source)

        val v0SexpAst = loadIonSexp(expectedSexpAstV0String)
        serializeAssert(AstVersion.V0, parsedExprNode, v0SexpAst, source)

        val partiqlAst = loadIonSexp(expectedPartiqlAstString)
        partiqlAssert(parsedExprNode, partiqlAst, source)

        pigDomainAssert(parsedExprNode, partiqlAst.toIonElement().asSexp())

        pigExprNodeTransformAsserts(parsedExprNode)
    }

    private fun serializeAssert(astVersion: AstVersion, parsedExprNode: ExprNode, expectedSexpAst: IonSexp, source: String) {

        val actualSexpAstWithoutMetas = AstSerializer.serialize(parsedExprNode, astVersion, ion).filterMetaNodes()

        val deserializer = AstDeserializerBuilder(ion).build()
        assertSexpEquals(expectedSexpAst, actualSexpAstWithoutMetas, "$astVersion AST, $source")

        val deserializedExprNodeFromSexp = deserializer.deserialize(expectedSexpAst, astVersion)

        assertEquals(
            "Parsed ExprNodes must match deserialized s-exp $astVersion AST",
            parsedExprNode.stripMetas(),
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
     * a [PartiqlAst] and to an IonValue Sexp equals the [expectedSexpAst]. Next checks that converting this IonValue
     * Sexp to an ExprNode equals the [parsedExprNode].
     */
    private fun partiqlAssert(parsedExprNode: ExprNode, expectedSexpAst: IonSexp, source: String) {
        val actualSexpAstStatment = parseToAst(source)
        val actualSexpQuery = unwrapQuery(actualSexpAstStatment)
        val actualSexpAst = actualSexpQuery.toIonElement().asAnyElement().toIonValue(ion)

        assertSexpEquals(expectedSexpAst, actualSexpAst, "AST, $source")

        val exprNodeFromSexp = actualSexpAstStatment.toExprNode(ion)

        assertEquals(
            "Parsed ExprNodes must match the expected PartiqlAst",
            parsedExprNode.stripMetas(),
            exprNodeFromSexp.stripMetas())
    }

    private fun pigDomainAssert(parsedExprNode: ExprNode, expectedSexpAst: SexpElement) {
        // Convert ExprNode into a PartiqlAst statement
        val statement = parsedExprNode.toAstStatement()

        // Test cases are missing (query <expr>) wrapping, so extract <expr>
        val parsedElement = unwrapQuery(statement)

        assertRoundTripIonElementToPartiQlAst(parsedElement, expectedSexpAst)

        // Run the parsedExprNode through the conversion to PIG domain instance for all tests
        // to detect conditions which will cause the conversion to throw an exception.
        assertRoundTripPartiQlAstToExprNode(statement, expectedSexpAst, parsedExprNode)
    }

    private fun assertRoundTripPartiQlAstToExprNode(statement: PartiqlAst.Statement, expectedSexpAst: IonElement, parsedExprNode: ExprNode) {
        // Run additional checks on the resulting PartiqlAst instance

        // None of our test cases are wrapped in (query <expr>), so extract <expr> from that out
        val element = unwrapQuery(statement)
        assertEquals(expectedSexpAst, element)

        // Convert the the IonElement back to the PartiqlAst instance and assert equivalence
        val transformedPig = PartiqlAst.transform(statement.toIonElement()) as PartiqlAst.Statement
        assertEquals(statement, transformedPig)

        // Convert from the PIG instance back to ExprNode and assert the result is the same as parsedExprNode.
        val exprNode2 = MetaStrippingRewriter.stripMetas(transformedPig.toExprNode(ion))
        assertEquals(MetaStrippingRewriter.stripMetas(parsedExprNode), exprNode2)
    }

    private fun assertRoundTripIonElementToPartiQlAst(parsedElement: SexpElement, expectedSexpAst: SexpElement) {
        // #1 We can transform the parsed PartiqlAst element.
        val transformedParsedElement = PartiqlAst.transform(parsedElement)

        // #2 We can transform the expected PartiqlAst element.
        val transformedExpectedElement = PartiqlAst.transform(expectedSexpAst)

        // #3 The results of both transformations match.
        assertEquals(transformedExpectedElement, transformedParsedElement)

        // #4 Re-transforming the parsed PartiqlAst element and check if it matches the expected AST.
        val reserializedAst = transformedParsedElement.toIonElement()
        assertEquals(expectedSexpAst, reserializedAst)

        // #5 Re-serializing the expected PartiqlAst element matches the expected AST.
        // Note:  because of #3 above, no need for #5.
    }

    /**
     * Strips metas from the [parsedExprNode] so they are not included in equivalence checks
     * and round-trip the resulting [parsedExprNode] AST through [toAstStatement] and [toExprNode].
     *
     * Verify that the result matches the original without metas.
     */
    private fun pigExprNodeTransformAsserts(parsedExprNode: ExprNode) {
        val parsedExprNodeNoMetas = MetaStrippingRewriter.stripMetas(parsedExprNode)
        val statement = parsedExprNodeNoMetas.toAstStatement()
        val exprNode2 = statement.toExprNode(ion)
        assertEquals(parsedExprNodeNoMetas, exprNode2)
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
