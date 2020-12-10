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

import com.amazon.ionelement.api.toIonElement
import org.junit.Test
import org.partiql.lang.TestBase
import org.partiql.lang.domains.PartiqlAst
import org.partiql.lang.errors.ErrorCode

class PartiqlAstSanityValidatorTests : TestBase() {
    private fun litInt(value: Int) = PartiqlAst.build { lit(ion.newInt(value).toIonElement()) }

    @Test
    fun groupPartial() {
        assertThrowsSqlException(ErrorCode.EVALUATOR_FEATURE_NOT_SUPPORTED_YET) {
            PartiqlAstSanityValidator.validate(
                PartiqlAst.build {
                    query(
                        select(
                            project = projectValue(litInt(1)),
                            from = scan(litInt(1)),
                            group = groupBy(
                                strategy =  groupPartial(),
                                keyList =  groupKeyList(emptyList()))))
                }
            )
        }
    }

    @Test
    fun groupPartialInSubquery() {
        assertThrowsSqlException(ErrorCode.EVALUATOR_FEATURE_NOT_SUPPORTED_YET) {
            PartiqlAstSanityValidator.validate(
                PartiqlAst.build {
                    query(
                        select(
                            project = projectValue(litInt(1)),
                            from = scan(
                                select(
                                    project = projectValue(litInt(1)),
                                    from = scan(litInt(1)),
                                    group = groupBy(
                                        strategy =  groupPartial(),
                                        keyList =  groupKeyList(emptyList()))))))
                }
            )
        }
    }

    @Test
    fun groupPartialInSubSubquery() {
        assertThrowsSqlException(ErrorCode.EVALUATOR_FEATURE_NOT_SUPPORTED_YET) {
            PartiqlAstSanityValidator.validate(
                PartiqlAst.build {
                    query(
                        select(
                            project = projectValue(litInt(1)),
                            from = scan(
                                select(
                                    project = projectValue(litInt(1)),
                                    from = scan(
                                        select(
                                            project = projectValue(litInt(1)),
                                            from = scan(litInt(1)),
                                            group = groupBy(
                                                strategy =  groupPartial(),
                                                keyList =  groupKeyList(emptyList()))))))))
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
                            from = scan(litInt(1)),
                            project = projectPivot(litInt(1), litInt(1)),
                            group = groupBy(
                                strategy = groupFull(),
                                keyList = groupKeyList(emptyList()))))
                }
            )
        }
    }

    @Test
    fun pivotWithGroupByInSubquery() {
        assertThrowsSqlException(ErrorCode.EVALUATOR_FEATURE_NOT_SUPPORTED_YET) {
            PartiqlAstSanityValidator.validate(
                PartiqlAst.build {
                    query(
                        select(
                            project = projectValue(select(
                                    from = scan(litInt(1)),
                                    project = projectPivot(litInt(1), litInt(1)),
                                    group = groupBy(
                                        strategy = groupFull(),
                                        keyList = groupKeyList(emptyList())))),
                            from = scan(litInt(1))))
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
                            from = scan(litInt(1)),
                            project = projectValue(litInt(1)),
                            // The error should occur when `groupBy` is null but `having` is not
                            group = null,
                            having = litInt(1)))
                }
            )
        }
    }

    @Test
    fun havingWithoutGroupByGroupByIsNullInSubquery() {
        assertThrowsSqlException(ErrorCode.SEMANTIC_HAVING_USED_WITHOUT_GROUP_BY) {
            PartiqlAstSanityValidator.validate(
                PartiqlAst.build {
                    query(
                        select (
                            from = scan(select(
                                from = scan(litInt(1)),
                                project = projectValue(litInt(1)),
                                // The error should occur when `groupBy` is null but `having` is not
                                group = null,
                                having = litInt(1))),
                            project = projectValue(litInt(1))))
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
                            having = litInt(1)))
                }
            )
        }
    }

    @Test
    fun havingWithoutGroupByNoGroupByItemsInSubquery() {
        assertThrowsSqlException(ErrorCode.SEMANTIC_HAVING_USED_WITHOUT_GROUP_BY) {
            PartiqlAstSanityValidator.validate(
                PartiqlAst.build {
                    query(
                        select(
                            from = scan(select(
                                from = scan(litInt(1)),
                                project = projectValue(litInt(1)),
                                // The error should occur when `groupBy.groupByItems` is empty and `having` is not null
                                group = groupBy(
                                    strategy = groupFull(),
                                    keyList = groupKeyList(emptyList())),
                                having = litInt(1))),
                            project = projectValue(litInt(1))))
                }
            )
        }
    }

    @Test
    fun literalIntOverflow() {
        val literalOverflowInt = PartiqlAst.build { query(lit(ion.singleValue("${Long.MAX_VALUE}0").toIonElement())) }
        assertThrowsSqlException(ErrorCode.EVALUATOR_INTEGER_OVERFLOW) {
            PartiqlAstSanityValidator.validate(literalOverflowInt)
        }
    }

    @Test
    fun literalIntOverflowInQuery() {
        assertThrowsSqlException(ErrorCode.EVALUATOR_INTEGER_OVERFLOW) {
            PartiqlAstSanityValidator.validate(
                PartiqlAst.build {
                    query(
                        select(
                            from = scan(lit(ion.singleValue("${Long.MAX_VALUE}0").toIonElement())),
                            project = projectValue(litInt(1))))
                }
            )
        }
    }

    @Test
    fun literalIntOverflowInSubquery() {
        assertThrowsSqlException(ErrorCode.EVALUATOR_INTEGER_OVERFLOW) {
            PartiqlAstSanityValidator.validate(
                PartiqlAst.build {
                    query(
                        select(
                            from = scan(litInt(1)),
                            project = projectValue(select(
                                from = scan(lit(ion.singleValue("${Long.MAX_VALUE}0").toIonElement())),
                                project = projectValue(litInt(1))))))
                }
            )
        }
    }
}
