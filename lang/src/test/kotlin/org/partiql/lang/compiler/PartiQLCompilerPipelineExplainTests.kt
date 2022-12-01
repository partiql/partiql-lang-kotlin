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

package org.partiql.lang.compiler

import com.amazon.ionelement.api.ionInt
import org.junit.Assert.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsSource
import org.partiql.annotation.PartiQLExperimental
import org.partiql.lang.domains.PartiqlAst
import org.partiql.lang.domains.PartiqlLogical
import org.partiql.lang.domains.PartiqlLogicalResolved
import org.partiql.lang.domains.PartiqlPhysical
import org.partiql.lang.eval.EvaluationSession
import org.partiql.lang.eval.PartiQLResult
import org.partiql.lang.planner.transforms.PLAN_VERSION_NUMBER
import org.partiql.lang.util.ArgumentsProviderBase

@PartiQLExperimental
class PartiQLCompilerPipelineExplainTests {

    val compiler = PartiQLCompilerPipeline.standard()

    data class ExplainTestCase(
        val description: String? = null,
        val query: String,
        val expected: PartiQLResult,
        val session: EvaluationSession = EvaluationSession.standard()
    )

    @ArgumentsSource(SuccessTestProvider::class)
    @ParameterizedTest
    fun successTests(tc: ExplainTestCase) = runSuccessTest(tc)

    private fun runSuccessTest(tc: ExplainTestCase) {
        val statement = compiler.compile(tc.query)
        val result = statement.eval(tc.session)
        assertEquals(tc.expected, result)
    }

    class SuccessTestProvider : ArgumentsProviderBase() {
        override fun getParameters(): List<Any> = listOf(
            ExplainTestCase(
                description = "Simple explain implicit AST",
                query = "EXPLAIN 1",
                expected = PartiQLResult.Explain.Domain(
                    format = null,
                    value = PartiqlAst.build {
                        query(
                            lit(
                                ionInt(1)
                            )
                        )
                    }
                )
            ),
            ExplainTestCase(
                description = "Simple explain explicit type AST",
                query = "EXPLAIN (TYPE AST) 1",
                expected = PartiQLResult.Explain.Domain(
                    format = null,
                    value = PartiqlAst.build {
                        query(
                            lit(
                                ionInt(1)
                            )
                        )
                    }
                )
            ),
            ExplainTestCase(
                description = "Simple explain explicit type and format",
                query = "EXPLAIN (TYPE AST, FORMAT ion_SEXP) 1",
                expected = PartiQLResult.Explain.Domain(
                    format = "ion_SEXP",
                    value = PartiqlAst.build {
                        query(
                            lit(
                                ionInt(1)
                            )
                        )
                    }
                )
            ),
            ExplainTestCase(
                description = "Simple explain explicit format",
                query = "EXPLAIN (FORMAT ion_SEXP) 1",
                expected = PartiQLResult.Explain.Domain(
                    format = "ion_SEXP",
                    value = PartiqlAst.build {
                        query(
                            lit(
                                ionInt(1)
                            )
                        )
                    }
                )
            ),
            ExplainTestCase(
                description = "Simple explain explicit type AST NORMALIZED",
                query = "EXPLAIN (TYPE ast_normalized) 1",
                expected = PartiQLResult.Explain.Domain(
                    format = null,
                    value = PartiqlAst.build {
                        query(
                            lit(
                                ionInt(1)
                            )
                        )
                    }
                )
            ),
            ExplainTestCase(
                description = "Simple explain explicit type LOGICAL",
                query = "EXPLAIN (TYPE logical) 1",
                expected = PartiQLResult.Explain.Domain(
                    format = null,
                    value = PartiqlLogical.build {
                        plan(
                            stmt = query(
                                lit(
                                    ionInt(1)
                                )
                            ),
                            version = PLAN_VERSION_NUMBER
                        )
                    }
                )
            ),
            ExplainTestCase(
                description = "Simple explain explicit type LOGICAL RESOLVED",
                query = "EXPLAIN (TYPE logical_resolved) 1",
                expected = PartiQLResult.Explain.Domain(
                    format = null,
                    value = PartiqlLogicalResolved.build {
                        plan(
                            stmt = query(
                                lit(
                                    ionInt(1)
                                )
                            ),
                            version = PLAN_VERSION_NUMBER
                        )
                    }
                )
            ),
            ExplainTestCase(
                description = "Simple explain explicit type PHYSICAL",
                query = "EXPLAIN (TYPE physical) 1",
                expected = PartiQLResult.Explain.Domain(
                    format = null,
                    value = PartiqlPhysical.build {
                        plan(
                            stmt = query(
                                lit(
                                    ionInt(1)
                                )
                            ),
                            version = PLAN_VERSION_NUMBER
                        )
                    }
                )
            ),
            ExplainTestCase(
                description = "Simple explain explicit type PHYSICAL",
                query = "EXPLAIN (TYPE physical_transformed) 1",
                expected = PartiQLResult.Explain.Domain(
                    format = null,
                    value = PartiqlPhysical.build {
                        plan(
                            stmt = query(
                                lit(
                                    ionInt(1)
                                )
                            ),
                            version = PLAN_VERSION_NUMBER
                        )
                    }
                )
            ),
        )
    }
}
