package com.amazon.ionsql.eval

import org.junit.*

class EvaluatingCompilerExceptionsTest : EvaluatorBase() {
    private val globalListOfNumbers = mapOf("numbers" to "[1, 2.0, 3e0, 4, 5d0]")
    private val animals = mapOf("animals" to """
        [
          {name: "Kumo", type: "dog"},
          {name: "Mochi", type: "dog"},
          {name: "Lilikoi", type: "unicorn"},
        ]
        """, "animal_types" to """
        [
          {id: "dog", is_magic: false},
          {id: "cat", is_magic: false},
          {id: "unicorn", is_magic: true},
        ]
        """)

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
        voidEval("""SELECT VALUE COUNT(*) FROM numbers""", globalListOfNumbers)
    }

    @Test
    fun selectListNestedAggregateCall() = assertThrows("No such syntax handler for call_agg", NodeMetadata(1, 12)) {
        voidEval("""SELECT SUM(AVG(n)) FROM <<numbers, numbers>> AS n""", globalListOfNumbers)
    }


    @Test
    fun badAlias() = assertThrows("No such binding: y", NodeMetadata(1, 14)) {
        voidEval("SELECT VALUE y FROM << 'el1' >> AS x")
    }

    @Test
    fun wrongArityExists() = assertThrows("Expected a single argument for exists but found: 0", NodeMetadata(1, 1)) {
        voidEval("exists()")
    }

    @Test
    fun unknownFunction() = assertThrows("No such function: unknownFunction", NodeMetadata(1, 1)) {
        voidEval("unknownFunction()")
    }

    @Test
    fun rightJoin() = assertThrows("RIGHT and FULL JOIN not supported", NodeMetadata(1, 28)) {
        voidEval("SELECT * FROM animals AS a RIGHT JOIN animal_types AS a_type WHERE a.type = a_type.id", animals)
    }

    @Test
    fun outerJoin() = assertThrows("RIGHT and FULL JOIN not supported", NodeMetadata(1, 28)) {
        voidEval("SELECT * FROM animals AS a OUTER JOIN animal_types AS a_type WHERE a.type = a_type.id", animals)
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
    fun badCast() = assertThrows("Internal error, For input string: \"a\"", NodeMetadata(1, 18),
                                 cause = NumberFormatException::class) {
        voidEval("CAST('a' as int) > 0")
    }

    @Test
    fun badCastInSelect() = assertThrows("Internal error, For input string: \"a\"", NodeMetadata(1, 91),
                                 cause = NumberFormatException::class) {
        voidEval("SELECT *  FROM `[{_1: a, _2: 1}, {_1: a, _2: 'a'}, {_1: a, _2: 3}]` WHERE CAST(_2 as INT) > 0")
    }

    @Test
    fun divideByZero() = assertThrows("Internal error, / by zero", NodeMetadata(1, 3),
                                         cause = ArithmeticException::class) {
        voidEval("1 / 0")
    }

    @Test
    fun divideByZeroInSelect() = assertThrows("Internal error, / by zero", NodeMetadata(1, 76),
                                         cause = ArithmeticException::class) {
        voidEval("SELECT *  FROM `[{_1: a, _2: 1}, {_1: a, _2: 2}, {_1: a, _2: 3}]` WHERE _2 / 0 > 0")
    }


}
