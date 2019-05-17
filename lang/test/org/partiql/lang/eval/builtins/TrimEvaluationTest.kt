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

package org.partiql.lang.eval.builtins

import org.partiql.lang.eval.*
import org.partiql.lang.syntax.*
import org.junit.*


class TrimEvaluationTest : EvaluatorTestBase() {

    @Test
    fun trimDefault1() = assertEval("trim('   string   ')", "\"string\"")

    @Test
    fun trimDefault2() = assertEval("trim('   string')", "\"string\"")

    @Test
    fun trimDefault3() = assertEval("trim('string   ')", "\"string\"")

    @Test
    fun trimOtherWhitespaces() = assertEval("trim('\tstring\t')", "\"\tstring\t\"")

    @Test
    fun trimSimpleCaseWithFrom1() = assertEval("trim(from '   string   ')", "\"string\"")

    @Test
    fun trimSimpleCaseWithFrom2() = assertEval("trim(from '   string')", "\"string\"")

    @Test
    fun trimSimpleCaseWithFrom3() = assertEval("trim(from 'string   ')", "\"string\"")

    @Test
    fun trimBoth1() = assertEval("trim(both from '   string   ')", "\"string\"")

    @Test
    fun trimBoth2() = assertEval("trim(both from '   string')", "\"string\"")

    @Test
    fun trimBoth3() = assertEval("trim(both from 'string   ')", "\"string\"")

    @Test
    fun trimLeading1() = assertEval("trim(leading from '   string   ')", "\"string   \"")

    @Test
    fun trimLeading2() = assertEval("trim(leading from '   string')", "\"string\"")

    @Test
    fun trimLeading3() = assertEval("trim(leading from 'string   ')", "\"string   \"")

    @Test
    fun trimTrailing1() = assertEval("trim(trailing from '   string   ')", "\"   string\"")

    @Test
    fun trimTrailing2() = assertEval("trim(trailing from '   string')", "\"   string\"")

    @Test
    fun trimTrailing3() = assertEval("trim(trailing from 'string   ')", "\"string\"")

    @Test
    fun trimMultiple1() = assertEval("trim(both ' -=' from '- =string =-  ')", "\"string\"")

    @Test
    fun trimMultiple2() = assertEval("trim(both ' -=' from '--===    -= -= -=   string')", "\"string\"")

    @Test
    fun trimMultiple3() = assertEval("trim(both ' -=' from 'string ==- = -=- - ----------  ')", "\"string\"")

    @Test
    fun trimMultiple4() = assertEval("trim(both ' ' from '            ')", "\"\"")

    @Test
    fun trimMultiple5() = assertEval("trim(leading ' ' from '            ')", "\"\"")

    @Test
    fun trimMultiple6() = assertEval("trim(trailing ' ' from '            ')", "\"\"")

    @Test
    fun trimEmoji1() = assertEval("trim(both '💩' from  '💩💩💩💩💩💩💩💩💩💩😁😞😸😸💩💩💩💩💩💩💩💩💩💩💩💩💩💩')",
                                  "\"😁😞😸😸\"")

    @Test
    fun trimEmoji2() = assertEval("trim('               😁😞😸😸             ')", "\"😁😞😸😸\"")

    @Test
    fun trimChinese() = assertEval("trim(both '話 ' from '話話 話話話話話話費谷料村能話話話話 話話話話    ')", "\"費谷料村能\"")

    @Test
    fun trimExpression() = assertEval("trim('   a'||'b  ')", "\"ab\"")

    @Test
    fun trimToRemoveExpression() = assertEval("""
            SELECT
                trim(both el from '   1ab1  ') as trimmed
            FROM
                <<' 1'>> AS el
            """, "[{trimmed:\"ab\"}]")

    @Test
    fun trimWrongToRemoveType() = assertThrows("Expected text: 1", NodeMetadata(1, 1)) {
        voidEval("trim(trailing 1 from '')")
    }

    @Test
    fun trimWrongStringType() = assertThrows("Expected text: true", NodeMetadata(1, 1)) {
        voidEval("trim(true)")
    }

    @Test
    fun trimWrongStringType2() = assertThrows("Expected text: true", NodeMetadata(1, 1)) {
        voidEval("trim(trailing from true)")
    }

    @Test
    fun trimDefaultSpecification1() = assertEval("trim('12' from '1212b1212')", "\"b\"")

    @Test
    fun trimDefaultSpecification2() = assertEval("trim('12' from '1212b')", "\"b\"")

    @Test
    fun trimDefaultSpecification3() = assertEval("trim('12' from 'b1212')", "\"b\"")

    @Test
    fun trimNull01() = assertEval("trim(both null from '')", "null")

    @Test
    fun trimNull02() = assertEval("trim(both '' from null)", "null")

    @Test
    fun trimNull03() = assertEval("trim(null from '')", "null")

    @Test
    fun trimNull04() = assertEval("trim('' from null)", "null")

    @Test
    fun trimNull05() = assertEval("trim(null)", "null")

    @Test
    fun trimMissing01() = assertEval("trim(both missing from '')", "null")

    @Test
    fun trimMissing02() = assertEval("trim(both '' from missing)", "null")

    @Test
    fun trimMissing03() = assertEval("trim(missing from '')", "null")

    @Test
    fun trimMissing04() = assertEval("trim('' from missing)", "null")

    @Test
    fun trimMissing05() = assertEval("trim(missing)", "null")
}