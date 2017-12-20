package com.amazon.ionsql.eval

import com.amazon.ionsql.errors.*
import org.junit.*

class EvaluatingCompilerExceptionsTest : EvaluatorBase() {

    @Test
    fun notOnOne() = assertThrows("Expected boolean: 1", NodeMetadata(1, 1), cause = IllegalArgumentException::class) {
        voidEval("not 1")
    }

    @Test
    fun betweenIncompatiblePredicate() = assertThrows("Cannot compare values: \"APPLE\", 2", NodeMetadata(4, 19)) {
        voidEval("""
          SELECT VALUE x
          FROM << 'APPLE', 'ZOE', 'YOYO' >> AS x
          WHERE x BETWEEN 'A' AND 2
        """)
    }

    @Test
    fun notBetweenIncompatiblePredicate() = assertThrows("Cannot compare values: \"APPLE\", 1", NodeMetadata(4, 19)) {
        voidEval("""
          SELECT VALUE x
          FROM << 'APPLE', 'ZOE', 'YOYO' >> AS x
          WHERE x NOT BETWEEN 1 AND 'Y'
        """)
    }

    @Test
    fun shadowedVariables() = assertThrows("a is ambiguous: [5, 5]", NodeMetadata(1, 14)) {
        voidEval("""SELECT VALUE a FROM `[{v:5}]` AS item, @item.v AS a, @item.v AS a""")
    }

    @Test
    fun topLevelCountStar() = assertThrows("No such syntax handler for call_agg_wildcard",
                                           NodeMetadata(1, 1)) { voidEval("""COUNT(*)""") }


    @Test
    fun selectValueCountStar() = assertThrows("No such syntax handler for call_agg_wildcard", NodeMetadata(1, 14)) {
        voidEval("""SELECT VALUE COUNT(*) FROM numbers""")
    }

    @Test
    fun selectListNestedAggregateCall() = assertThrows("No such syntax handler for call_agg", NodeMetadata(1, 12)) {
        voidEval("""SELECT SUM(AVG(n)) FROM <<numbers, numbers>> AS n""")
    }

    private val BAD_ALIAS_SQL = "SELECT VALUE y FROM << 'el1' >> AS x"
    @Test fun badAlias() {
        //Note that the current default for CompileOptions.undefinedVariable is UndefinedVariableBehavior.ERROR
        checkInputThrowingEvaluationException(
            BAD_ALIAS_SQL,
            ErrorCode.EVALUATOR_BINDING_DOES_NOT_EXIST,
            mapOf(
                Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 14L,
                Property.BINDING_NAME to "y"))
    }

    @Test fun missingAlias() =
        //Same SQL as previous test--but DO NOT throw exception this time because of UndefinedVariableBehavior.MISSING
        assertEval(BAD_ALIAS_SQL, "[null]",
                   compileOptions = CompileOptions.build { undefinedVariable(UndefinedVariableBehavior.MISSING) })

    @Test
    fun wrongArityExists() = assertThrows("Expected a single argument for exists but found: 0", NodeMetadata(1, 1)) {
        voidEval("exists()")
    }

    @Test
    fun unknownFunction() = assertThrows("No such function: unknownfunction", NodeMetadata(1, 1)) {
        voidEval("unknownFunction()")
    }

    @Test
    fun rightJoin() = assertThrows("RIGHT and FULL JOIN not supported", NodeMetadata(1, 28)) {
        voidEval("SELECT * FROM animals AS a RIGHT JOIN animal_types AS a_type WHERE a.type = a_type.id")
    }

    @Test
    fun outerJoin() = assertThrows("RIGHT and FULL JOIN not supported", NodeMetadata(1, 28)) {
        voidEval("SELECT * FROM animals AS a OUTER JOIN animal_types AS a_type WHERE a.type = a_type.id")
    }

    @Test
    fun substringWrongType() = assertThrows("Argument 2 of substring was not numeric", NodeMetadata(1, 1)) {
        voidEval("substring('abcdefghi' from '1')")
    }

    @Test
    fun addingWrongTypes() = assertThrows("Expected number: \"a\"", NodeMetadata(1, 11)) {
        voidEval("1 + 2 + 4 + 'a' + 5")
    }

    @Test
    fun badCastToInt() = checkInputThrowingEvaluationException(
        "CAST('a' as int) > 0",
        ErrorCode.EVALUATOR_CAST_FAILED,
        SourceLocationProperties(1, 18) + mapOf(Property.CAST_FROM to "STRING", Property.CAST_TO to "INT"))

    @Test
    fun badCastInSelectToInt() = checkInputThrowingEvaluationException(
        "SELECT *  FROM `[{_1: a, _2: 1}, {_1: a, _2: 'a'}, {_1: a, _2: 3}]` WHERE CAST(_2 as INT) > 0",
        ErrorCode.EVALUATOR_CAST_FAILED,
        SourceLocationProperties(1, 91) + mapOf(Property.CAST_FROM to "SYMBOL", Property.CAST_TO to "INT"))

    @Test
    fun badCastToDecimal() = checkInputThrowingEvaluationException(
        "CAST('a' as DECIMAL) > 0",
        ErrorCode.EVALUATOR_CAST_FAILED,
        SourceLocationProperties(1, 22) + mapOf(Property.CAST_FROM to "STRING", Property.CAST_TO to "DECIMAL"),
        NumberFormatException::class)

    @Test
    fun badCastToTimestamp() = checkInputThrowingEvaluationException(
        "CAST('2010-01-01T10' as TIMESTAMP) > 0",
        ErrorCode.EVALUATOR_CAST_FAILED,
        SourceLocationProperties(1, 36) + mapOf(Property.CAST_FROM to "STRING", Property.CAST_TO to "TIMESTAMP"),
        IllegalArgumentException::class)

    @Test // https://i.amazon.com/issues/IONSQL-163
    fun failedCastWithoutLocation() = checkInputThrowingEvaluationException("SELECT CAST('foo' as INT) FROM <<1>>",
                                                                            ErrorCode.EVALUATOR_CAST_FAILED_NO_LOCATION,
                                                                            mapOf(Property.CAST_FROM to "STRING",
                                                                                  Property.CAST_TO to "INT"),
                                                                            ClassCastException::class)

    @Test // https://i.amazon.com/issues/IONSQL-163
    fun invalidCastWithoutLocation() = checkInputThrowingEvaluationException("SELECT CAST(`2010T` as INT) FROM <<1>>",
                                                                             ErrorCode.EVALUATOR_INVALID_CAST_NO_LOCATION,
                                                                             mapOf(Property.CAST_FROM to "TIMESTAMP",
                                                                                   Property.CAST_TO to "INT"))

    @Test
    fun divideByZero() = assertThrows("/ by zero", NodeMetadata(1, 3)) {
        voidEval("1 / 0")
    }

    @Test
    fun divideByZeroDecimal() = assertThrows("/ by zero", NodeMetadata(1, 5)) {
        voidEval("1.0 / 0.0")
    }

    @Test
    fun divideByZeroInSelect() = assertThrows("/ by zero", NodeMetadata(1, 76)) {
        voidEval("SELECT *  FROM `[{_1: a, _2: 1}, {_1: a, _2: 2}, {_1: a, _2: 3}]` WHERE _2 / 0 > 0")
    }

    @Test
    fun utcnowWithArgument() = assertThrows("utcnow() takes no arguments", NodeMetadata(1, 1)) {
        voidEval("utcnow(1)")
    }
}
