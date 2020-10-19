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

package org.partiql.lang.eval.visitors

import com.amazon.ionelement.api.emptyMetaContainer
import com.amazon.ionelement.api.toIonElement
import org.junit.Test
import org.partiql.lang.TestBase
import org.partiql.lang.domains.PartiqlAst
import org.partiql.lang.errors.ErrorCode

class PartiqlAstSanityValidatorTests : TestBase() {
    private val dummyMetas = emptyMetaContainer()
    private fun litInt(value: Int) = PartiqlAst.build{ lit(ion.newInt(value).toIonElement(), dummyMetas) }

    @Test
    fun groupPartial() {
        assertThrowsSqlException(ErrorCode.EVALUATOR_FEATURE_NOT_SUPPORTED_YET) {
            PartiqlAstSanityValidator.validate(
                PartiqlAst.build {
                    query(
                        select(
                            setq = all(),
                            project = projectValue(litInt(1)),
                            from = scan(litInt(1)),
                            group = groupBy(
                                strategy =  groupPartial(),
                                keyList =  groupKeyList(emptyList())),
                            metas = dummyMetas))
                }
            )
        }
    }

    @Test
    fun pivotWithGroupBy() {
        assertThrowsSqlException(ErrorCode.EVALUATOR_FEATURE_NOT_SUPPORTED_YET) {
            PartiqlAstSanityValidator.validate(
                PartiqlAst.build {
                    query(
                        select(
                            setq = all(),
                            from = scan(litInt(1)),
                            project = projectPivot(litInt(1), litInt(1)),
                            group = groupBy(
                                strategy = groupFull(),
                                keyList = groupKeyList(emptyList())),
                            metas = dummyMetas))
                }
            )
        }
    }

    @Test
    fun havingWithoutGroupByGroupByIsNull() {
        assertThrowsSqlException(ErrorCode.SEMANTIC_HAVING_USED_WITHOUT_GROUP_BY) {
            PartiqlAstSanityValidator.validate(
                PartiqlAst.build {
                    query(
                        select(
                            setq = all(),
                            from = scan(litInt(1)),
                            project = projectValue(litInt(1)),
                            // The error should occur when `groupBy` is null but `having` is not
                            group = null,
                            having = litInt(1),
                            metas = dummyMetas))
                }
            )
        }
    }

    @Test
    fun havingWithoutGroupByNoGroupByItems() {
        assertThrowsSqlException(ErrorCode.SEMANTIC_HAVING_USED_WITHOUT_GROUP_BY) {
            PartiqlAstSanityValidator.validate(
                PartiqlAst.build {
                    query(
                        select(
                            setq = all(),
                            from = scan(litInt(1)),
                            project = projectValue(litInt(1)),
                            // The error should occur when `groupBy.groupByItems` is empty and `having` is not null
                            group = groupBy(
                                strategy = groupFull(),
                                keyList = groupKeyList(emptyList())),
                            having = litInt(1),
                            metas = dummyMetas))
                }
            )
        }
    }

    @Test
    fun literalIntOverflow() {
        val literalInt = PartiqlAst.build { query(lit(ion.singleValue("${Long.MAX_VALUE}0").toIonElement(), dummyMetas)) }
        assertThrowsSqlException(ErrorCode.EVALUATOR_INTEGER_OVERFLOW) {
            PartiqlAstSanityValidator.validate(literalInt)
        }
    }
}
