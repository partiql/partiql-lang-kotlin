package com.amazon.ionsql.eval.builtins

import com.amazon.ionsql.eval.*
import com.amazon.ionsql.syntax.*
import org.junit.*


class TrimEvaluationTest : EvaluatorBase() {

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

    @Test // regression for https://issues.amazon.com/IONSQL-160
    fun trimMultiple4() = assertEval("trim(both ' ' from '            ')", "\"\"")

    @Test // regression for https://issues.amazon.com/IONSQL-160
    fun trimMultiple5() = assertEval("trim(leading ' ' from '            ')", "\"\"")

    @Test // regression for https://issues.amazon.com/IONSQL-160
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
}