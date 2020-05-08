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

import com.amazon.ionelement.api.IonElement
import com.amazon.ionelement.api.toIonElement
import com.amazon.ion.IonSexp
import org.partiql.lang.TestBase
import org.partiql.lang.ast.AstDeserializerBuilder
import org.partiql.lang.ast.AstSerializer
import org.partiql.lang.ast.AstVersion
import org.partiql.lang.ast.ExprNode
import org.partiql.lang.ast.passes.MetaStrippingRewriter
import org.partiql.lang.ast.toAstStatement
import org.partiql.lang.ast.toExprNode
import org.partiql.lang.domains.partiql_ast
import org.partiql.lang.util.asIonSexp
import org.partiql.lang.util.filterMetaNodes
import org.partiql.pig.runtime.MalformedDomainDataException

abstract class SqlParserTestBase : TestBase() {
    protected val parser = SqlParser(ion)

    protected fun parse(source: String): ExprNode = parser.parseExprNode(source)

    protected fun assertExpression(
        source: String,
        expectedSexpAstV0String: String,
        skipPig: Boolean = true,
        pigBuilder: partiql_ast.builder.() -> partiql_ast.partiql_ast_node
    ) {
        val expectedPartiQlAst = partiql_ast.build { pigBuilder() }
        assertExpression(source, expectedSexpAstV0String, expectedPartiQlAst.toIonElement().toString(), skipPig)
    }

    protected fun assertExpression(
        source: String,
        expectedSexpAstV0String: String,
        expectedSexpAstV2String: String = expectedSexpAstV0String,
        skipPig: Boolean = true
    ) {
        // Convert the query to ExprNode
        val parsedExprNode = parse(source)

        val v0SexpAst = loadIonSexp(expectedSexpAstV0String)
        serializeAssert(AstVersion.V0, parsedExprNode, v0SexpAst, source)

        val v2SexpAst = loadIonSexp(expectedSexpAstV2String)
        serializeAssert(AstVersion.V2, parsedExprNode, v2SexpAst, source)

        pigDomainAssert(parsedExprNode, v2SexpAst.toIonElement(), skipPig)

        pigExprNodeTransformAsserts(parsedExprNode)
    }

    private fun serializeAssert(astVersion: AstVersion, parsedExprNode: ExprNode, expectedSexpAst: IonSexp, source: String) {

        val actualSexpAstWithoutMetas = AstSerializer.serialize(parsedExprNode, astVersion, ion).filterMetaNodes() as IonSexp

        val deserializer = AstDeserializerBuilder(ion).build()
        assertSexpEquals(expectedSexpAst, actualSexpAstWithoutMetas, "$astVersion AST, $source")

        val deserializedExprNodeFromSexp = deserializer.deserialize(expectedSexpAst, astVersion)

        assertEquals(
            "Parsed ExprNodes must match deserialized s-exp $astVersion AST",
            parsedExprNode.stripMetas(),
            deserializedExprNodeFromSexp.stripMetas())
    }

    private fun pigDomainAssert(parsedExprNode: ExprNode, expectedSexpAst: IonElement, skipPigTransformerTests: Boolean) {

        // Serialize ExprNode to V2 IonValue
        val sexpAst = AstSerializer.serialize(parsedExprNode, AstVersion.V2, ion).filterMetaNodes() as IonSexp

        // Convert the V2 IonValue to IonElement
        val parsedV2Element = sexpAst.toIonElement()

        if (skipPigTransformerTests) {
            assertPigTransformerFails(parsedV2Element)
        } else {
            assertRoundTripIonElementToPartiQlAst(parsedV2Element, expectedSexpAst)
        }

        // Run the parsedExprNode through the conversion to PIG domain instance for all tests
        // to detect conditions which will cause the conversion to throw an exception.
        val statement = parsedExprNode.toAstStatement()

        // If !skipPigTransformerTests, we can skip the following additional checks.
        if(!skipPigTransformerTests) {
            assertRoundTripPartiQlAstToExprNode(statement, expectedSexpAst, parsedExprNode)
        }
    }

    private fun assertRoundTripPartiQlAstToExprNode(statement: partiql_ast.statement, expectedSexpAst: IonElement, parsedExprNode: ExprNode) {
        // Run additional checks on the resulting partiql_ast instance

        // None of our test cases are wrapped in (query <expr>), so extract <expr> from that out
        val element = when (statement) {
            is partiql_ast.statement.query -> statement.expr0.toIonElement()
            is partiql_ast.statement.dml,
            is partiql_ast.statement.ddl -> statement.toIonElement()
        }
        assertEquals(expectedSexpAst, element)

        // Convert the the IonElement back to the partiql_ast instance and assert equivalence
        val transformedPig = partiql_ast.transform(statement.toIonElement()) as partiql_ast.statement
        assertEquals(statement, transformedPig)

        // Convert from the PIG instance back to ExprNode and assert the result is the same as parsedExprNode.
        val exprNode2 = MetaStrippingRewriter.stripMetas(transformedPig.toExprNode(ion))
        assertEquals(MetaStrippingRewriter.stripMetas(parsedExprNode), exprNode2)
    }

    private fun assertRoundTripIonElementToPartiQlAst(parsedV2Element: IonElement, expectedSexpAst: IonElement) {
        // #1 We can transform the parsed V2 element.
        val transformedParsedV2Element = partiql_ast.transform(parsedV2Element)

        // #2 We can transform the expected V2 element.
        val transformedExpectedV2Element = partiql_ast.transform(expectedSexpAst)

        // #3 The results of both transformations match.
        assertEquals(transformedExpectedV2Element, transformedParsedV2Element)

        // #4 Re-transforming the parsed V2 element matches the expected AST
        val reserializedV2Ast = transformedParsedV2Element.toIonElement()
        assertEquals(expectedSexpAst, reserializedV2Ast)

        // #5 Re-serializing the expected V2 element matches the expected AST.
        // Note:  because of #3 above, no need for #5.
    }

    private fun assertPigTransformerFails(parsedV2Element: IonElement) {

        // TODO: remove this method once V2 is fully complete.
        // Migration to PIG domain partially completed--expect failure.
        // (If the test starts succeeding suddenly, we want to know so we can mark the expected test to succeed!)
        // Doing so will also cause the additional assertions above to execute.
        try {
            partiql_ast.transform(parsedV2Element)
            fail("Transform to PIG domain unexpectedly succeeded! :)")
        } catch (ex: MalformedDomainDataException) {
            // OK!
        }
    }

    /**
     * Strips metas from the [parsedExprNode] so they are not included in equivalence checks
     * (it is a known fact that conversion from partiql_ast and ExprNode can be lossy) and
     * round-trip the resulting [parsedExprNode] AST through [toAstStatement] and [toExprNode].
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

}
