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

import com.amazon.ion.*
import org.partiql.lang.*
import org.partiql.lang.ast.*
import org.partiql.lang.ast.passes.V0AstSerializer
import org.partiql.lang.util.*

abstract class SqlParserTestBase : TestBase() {
    protected val parser = SqlParser(ion)

    protected fun parse(source: String): ExprNode = parser.parseExprNode(source)


    protected fun assertExpression(expectedLegacyAstStr: String, source: String) {
        assertExpression(expectedLegacyAstStr, source, null)
    }

    protected fun assertExpression(expectedSexpAstV0String: String, source: String, expectedSexpAstV1String: String?) {
        val deserializer = AstDeserializerBuilder(ion).build()

        // Convert the query to ExprNode
        val parsedExprNode = parse(source)

        // Serialize to V0 s-exp and assert that it matches the expected v0 s-exp AST
        val expectedSexpAstV0 = ion.singleValue(expectedSexpAstV0String)
        val sexpAstV0 = V0AstSerializer.serialize(parsedExprNode, ion)

        val sexpAstV0WithoutMetas = sexpAstV0.filterMetaNodes()
        assertSexpEquals(expectedSexpAstV0, sexpAstV0WithoutMetas, "V0 AST, $source")

        // Deserialize the v0 ast and assert that it matches the parsed ExprNode
        val deserializedExprNodeFromSexpV0 = deserializer.deserialize(sexpAstV0)
        assertEquals("Parsed ExprNodes must match deserialized s-exp V0 AST", parsedExprNode, deserializedExprNodeFromSexpV0)

        if (expectedSexpAstV1String != null) {

            // Serialize the ExprNodes originating from the query to s-exp AS v1 and assert that they match
            val expectedSexpAstV1 = ion.singleValue(expectedSexpAstV1String)
            val sexpAstV1 = AstSerializer.serialize(parsedExprNode, ion)

            //Our expected s-exp v1 values do not have the versioning wrapper so remove it
            assertEquals("ast", sexpAstV1.tagText)
            assertEquals(2, sexpAstV1.arity)
            assertEquals(1L, (sexpAstV1.tail
                .filterIsInstance<IonSexp>()
                .first { it.tagText == "version" }.tail.head as IonInt).longValue())

            val rootSexp = (sexpAstV1.args.find { (it as IonSexp).tagText == "root" } as IonSexp).tail.head as IonSexp

            // Our expected s-exp v1 values are wrapped in terms but do not have the `meta` node, so remove those
            val actualSexpAstV1 = rootSexp.filterTermMetas()

            // Finally, make sure the expected v1 s-exp matches the actual deserialized v1 s-exp
            assertSexpEquals(expectedSexpAstV1, actualSexpAstV1, "V1 AST, $source")


            // Deserialize the v1 ast and assert that it matches the parsed exprNode
            val deserializedExprNodeFromSexpV1 = deserializer.deserialize(sexpAstV1)
            assertEquals("Parsed ExprNodes must match deserialized s-exp V1 AST", parsedExprNode, deserializedExprNodeFromSexpV1)


            // As a final check, assert that the ExprNodes deserialized from the serialized v0
            // and v1 s-exp ASTs are equivalent
            assertEquals("ExprNode instances deserialized from s-exp V0 and V1 must match",
                         deserializedExprNodeFromSexpV0, deserializedExprNodeFromSexpV1)

            assertRewrite(source, parsedExprNode)
        }
    }

}

/**
 * Given:
 * ```
 * (term
 *    (exp
 *      (not
 *        (term
 *          (exp lit 1)
 *          (meta ...))
 *    (meta ...))
 *  ```
 *
 *  Returns an s-exp without the meta nodes, i.e.:
 *
 *  ```
 * (term
 *    (exp
 *      (not
 *        (term
 *          (exp lit 1))))
 *  ```
 */
private fun IonSexp.filterTermMetas(): IonSexp {
    val newSexp = system.newSexp()
    forEach { child ->
        if(child is IonSexp) {
            val tag = child[0]
            if (tag is IonSymbol && tag.stringValue() == "meta") {
                //Skip this node
            }
            else {
                newSexp.add(child.filterTermMetas())
            }
        } else {
            newSexp.add(child.clone())
        }
    }
    return newSexp
}

