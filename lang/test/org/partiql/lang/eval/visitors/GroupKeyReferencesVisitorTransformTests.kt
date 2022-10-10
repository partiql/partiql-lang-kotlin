/*
 * Copyright 2022 Amazon.com, Inc. or its affiliates.  All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at:
 *
 *      http://aws.amazon.com/apache2.0/
 *
 * or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific
 * language governing permissions and limitations under the License.
 */

package org.partiql.lang.eval.visitors

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsSource
import org.partiql.lang.ast.UniqueNameMeta
import org.partiql.lang.domains.PartiqlAst
import org.partiql.lang.domains.metaContainerOf
import org.partiql.lang.util.ArgumentsProviderBase
import org.partiql.pig.runtime.SymbolPrimitive

class GroupKeyReferencesVisitorTransformTests : VisitorTransformTestBase() {

    @ParameterizedTest
    @ArgumentsSource(ArgsProvider::class)
    fun transform(tc: TransformTestCase) = runTestForIdempotentTransform(tc, GroupKeyReferencesVisitorTransform())

    class ArgsProvider : ArgumentsProviderBase() {
        override fun getParameters(): List<Any> {
            return listOf(

                // SELECT a AS a FROM t GROUP BY a AS a
                TransformTestCase(
                    original = PartiqlAst.build {
                        query(
                            select(
                                project = projectList(
                                    projectExpr(
                                        id("a", caseInsensitive(), unqualified()),
                                        asAlias = "a"
                                    )
                                ),
                                from = scan(
                                    id("t", caseInsensitive(), unqualified())
                                ),
                                group = groupBy(
                                    groupFull(),
                                    keyList = groupKeyList(
                                        groupKey_(
                                            id(
                                                "a",
                                                caseInsensitive(),
                                                unqualified()
                                            ),
                                            asAlias = SymbolPrimitive(
                                                text = "a",
                                                metas = metaContainerOf(UniqueNameMeta("someUniqueName"))
                                            )
                                        )
                                    )
                                )
                            )
                        )
                    },
                    expected = "SELECT \"someUniqueName\" AS a FROM t GROUP BY a AS a"
                ),

                // SELECT someKey AS someProjection FROM t GROUP BY a AS someKey
                TransformTestCase(
                    original = PartiqlAst.build {
                        query(
                            select(
                                project = projectList(
                                    projectExpr(
                                        id("someKey", caseInsensitive(), unqualified()),
                                        asAlias = "someProjection"
                                    )
                                ),
                                from = scan(
                                    id("t", caseInsensitive(), unqualified())
                                ),
                                group = groupBy(
                                    groupFull(),
                                    keyList = groupKeyList(
                                        groupKey_(
                                            id(
                                                "a",
                                                caseInsensitive(),
                                                unqualified()
                                            ),
                                            asAlias = SymbolPrimitive(
                                                text = "someKey",
                                                metas = metaContainerOf(UniqueNameMeta("someUniqueName"))
                                            )
                                        )
                                    )
                                )
                            )
                        )
                    },
                    expected = "SELECT \"someUniqueName\" AS someProjection FROM t GROUP BY a AS someKey"
                ),
            )
        }
    }
}
