package org.partiql.lang.eval

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsSource
import org.partiql.lang.eval.evaluatortestframework.CompilerPipelineFactory
import org.partiql.lang.eval.evaluatortestframework.EvaluatorTestAdapter
import org.partiql.lang.eval.evaluatortestframework.EvaluatorTestCase
import org.partiql.lang.eval.evaluatortestframework.PipelineEvaluatorTestAdapter
import org.partiql.lang.util.ArgumentsProviderBase

class EvaluatingCompilerExcludeTests : EvaluatorTestBase() {

    private val testHarness: EvaluatorTestAdapter = PipelineEvaluatorTestAdapter(CompilerPipelineFactory())

    class ExcludeTests : ArgumentsProviderBase() {
        override fun getParameters(): List<Any> = listOf(
            EvaluatorTestCase(
                "SELECT t.* EXCLUDE t.a FROM <<{'a': {'b': 2}, 'foo': 'bar', 'foo2': 'bar2'}>> AS t",
                """<<{'foo': 'bar', 'foo2': 'bar2'}>>"""
            ),
            EvaluatorTestCase( // EXCLUDE tuple attr using bracket syntax; same output as above
                "SELECT t.* EXCLUDE t['a'] FROM <<{'a': {'b': 2}, 'foo': 'bar', 'foo2': 'bar2'}>> AS t",
                """<<{'foo': 'bar', 'foo2': 'bar2'}>>"""
            ),
            EvaluatorTestCase( // multiple binding tuples; select star
                """
                SELECT * 
                EXCLUDE t.a FROM 
                <<
                    {'a': {'b': 1}, 'foo': 'bar', 'foo2': 'bar1'},
                    {'a': {'b': 2}, 'foo': 'bar', 'foo2': 'bar2'},
                    {'a': {'b': 3}, 'foo': 'bar', 'foo2': 'bar3'}
                >> AS t
                """.trimIndent(),
                """<<
                    {'foo': 'bar', 'foo2': 'bar1'},
                    {'foo': 'bar', 'foo2': 'bar2'},
                    {'foo': 'bar', 'foo2': 'bar3'}
                >>""".trimMargin()
            ),
            EvaluatorTestCase( // multiple binding tuples; select list
                """
                SELECT t.* 
                EXCLUDE t.a FROM 
                <<
                    {'a': {'b': 1}, 'foo': 'bar', 'foo2': 'bar1'},
                    {'a': {'b': 2}, 'foo': 'bar', 'foo2': 'bar2'},
                    {'a': {'b': 3}, 'foo': 'bar', 'foo2': 'bar3'}
                >> AS t
                """.trimIndent(),
                """<<
                    {'foo': 'bar', 'foo2': 'bar1'},
                    {'foo': 'bar', 'foo2': 'bar2'},
                    {'foo': 'bar', 'foo2': 'bar3'}
                >>""".trimMargin()
            ),
            EvaluatorTestCase( // multiple binding tuples;
                """
                SELECT VALUE t
                EXCLUDE t.a FROM 
                <<
                    {'a': {'b': 1}, 'foo': 'bar', 'foo2': 'bar1'},
                    {'a': {'b': 2}, 'foo': 'bar', 'foo2': 'bar2'},
                    {'a': {'b': 3}, 'foo': 'bar', 'foo2': 'bar3'}
                >> AS t
                """.trimIndent(),
                """<<
                    {'foo': 'bar', 'foo2': 'bar1'},
                    {'foo': 'bar', 'foo2': 'bar2'},
                    {'foo': 'bar', 'foo2': 'bar3'}
                >>""".trimMargin()
            ),
            EvaluatorTestCase( // EXCLUDE deeper nested field; no fields remaining
                "SELECT t.* EXCLUDE t.a.b FROM <<{'a': {'b': 2}, 'foo': 'bar', 'foo2': 'bar2'}>> AS t",
                """<<{'a': {}, 'foo': 'bar', 'foo2': 'bar2'}>>"""
            ),
            EvaluatorTestCase( // EXCLUDE deeper nested field; other field remaining
                "SELECT t.* EXCLUDE t.a.b FROM <<{'a': {'b': 2, 'c': 3}, 'foo': 'bar', 'foo2': 'bar2'}>> AS t",
                """<<{'a': {'c': 3}, 'foo': 'bar', 'foo2': 'bar2'}>>"""
            ),
            EvaluatorTestCase( // EXCLUDE multiple nested paths
                "SELECT t.* EXCLUDE t.a.c, t.a.d, t.foo FROM <<{'a': {'b': 2, 'c': 3, 'd': 4}, 'foo': 'bar', 'foo2': 'bar2'}>> AS t",
                """<<{'a': {'b': 2}, 'foo2': 'bar2'}>>"""
            ),
            EvaluatorTestCase( // EXCLUDE overlapping paths
                """
                SELECT t.*
                EXCLUDE t.a.c, t.a -- `t.a` and `t.a.c` overlap; still exclude `t.a`
                FROM <<{'a': {'b': 2, 'c': 3, 'd': 4}, 'foo': 'bar', 'foo2': 'bar2'}>> AS t
                """.trimIndent(),
                """<<{'foo': 'bar', 'foo2': 'bar2'}>>"""
            ),
            EvaluatorTestCase( // EXCLUDE select star
                """SELECT * EXCLUDE c.ssn FROM [
                    {
                        'name': 'Alan',
                        'custId': 1,
                        'address': {
                            'city': 'Seattle',
                            'zipcode': 98109,
                            'street': '123 Seaplane Dr.'
                        },
                        'ssn': 123456789
                    }
                ] AS c
                """.trimIndent(),
                """
                <<
                    {
                        'name': 'Alan',
                        'custId': 1,
                        'address': {
                            'city': 'Seattle',
                            'zipcode': 98109,
                            'street': '123 Seaplane Dr.'
                        }
                    }
                >>"""
            ),
            EvaluatorTestCase( // EXCLUDE select star with FROM source list
                """SELECT * EXCLUDE c.ssn FROM [
                    {
                        'name': 'Alan',
                        'custId': 1,
                        'address': {
                            'city': 'Seattle',
                            'zipcode': 98109,
                            'street': '123 Seaplane Dr.'
                        },
                        'ssn': 123456789
                    }
                ] AS c
                """.trimIndent(),
                """
                <<
                    {
                        'name': 'Alan',
                        'custId': 1,
                        'address': {
                            'city': 'Seattle',
                            'zipcode': 98109,
                            'street': '123 Seaplane Dr.'
                        }
                    }
                >>"""
            ),
            EvaluatorTestCase( // EXCLUDE select star with multiple paths and FROM source list
                """
                SELECT * EXCLUDE c.ssn, c.address.street FROM [
                    {
                        'name': 'Alan',
                        'custId': 1,
                        'address': {
                            'city': 'Seattle',
                            'zipcode': 98109,
                            'street': '123 Seaplane Dr.'
                        },
                        'ssn': 123456789
                    }
                ] AS c
                """.trimIndent(),
                """
                <<
                    {
                        'name': 'Alan',
                        'custId': 1,
                        'address': {
                            'city': 'Seattle',
                            'zipcode': 98109
                        }
                    }
                >>"""
            ),
            EvaluatorTestCase( // EXCLUDE select star list index and list index field
                """
                SELECT *
                EXCLUDE
                    t.a.b.c[0],
                    t.a.b.c[1].field
                FROM [{
                    'a': {
                        'b': {
                            'c': [
                                {
                                    'field': 0,    -- c[0]; entire struct to be excluded
                                    'index': 0
                                },
                                {
                                    'field': 1,    -- c[1]; `field` to be excluded
                                    'index': 1
                                },
                                {
                                    'field': 2,    -- c[2]; field unchanged
                                    'index': 2
                                }
                            ]
                        }
                    },
                    'foo': 'bar'
                }] AS t
                """,
                """<<{'a': {'b': {'c': [{'index': 1}, {'field': 2, 'index': 2}]}}, 'foo': 'bar'}>>"""
            ),
            EvaluatorTestCase( // EXCLUDE select star collection index as last step
                """
                SELECT *
                EXCLUDE
                    t.a.b.c[0]
                FROM [{
                    'a': {
                        'b': {
                            'c': [0, 1, 2]
                        }
                    },
                    'foo': 'bar'
                }] AS t
                """,
                """<<{'a': {'b': {'c': [1, 2]}}, 'foo': 'bar'}>>"""
            ),
            EvaluatorTestCase( // EXCLUDE select star collection wildcard as last step on list
                """
                SELECT *
                EXCLUDE
                    t.a[*]
                FROM [{
                    'a': [0, 1, 2]
                }] AS t
                """,
                """<<{'a': []}>>"""
            ),
            EvaluatorTestCase( // EXCLUDE select star collection wildcard and tuple path on list
                """
                SELECT *
                EXCLUDE
                    t.a.b.c[*].field_x
                FROM [{
                    'a': {
                        'b': {
                            'c': [
                                {                    -- c[0]; field_x to be removed
                                    'field_x': 0, 
                                    'field_y': 0
                                },
                                {                    -- c[1]; field_x to be removed
                                    'field_x': 1,
                                    'field_y': 1
                                },
                                {                    -- c[2]; field_x to be removed
                                    'field_x': 2,
                                    'field_y': 2
                                }
                            ]
                        }
                    },
                    'foo': 'bar'
                }] AS t
                """,
                """<<{'a': {'b': {'c': [{'field_y': 0}, {'field_y': 1}, {'field_y': 2}]}}, 'foo': 'bar'}>>"""
            ),
            EvaluatorTestCase( // EXCLUDE select star tuple wildcard as final step
                """
                SELECT *
                EXCLUDE
                    t.a.b.c[*].*
                FROM [{
                    'a': {
                        'b': {
                            'c': [
                                {                    -- c[0]
                                    'field_x': 0, 
                                    'field_y': 0
                                },
                                {                    -- c[1]
                                    'field_x': 1,
                                    'field_y': 1
                                },
                                {                    -- c[2]
                                    'field_x': 2,
                                    'field_y': 2
                                }
                            ]
                        }
                    },
                    'foo': 'bar'
                }] AS t
                """,
                """<<{'a': {'b': {'c': [{}, {}, {}]}}, 'foo': 'bar'}>>"""
            ),
            EvaluatorTestCase( // EXCLUDE select star order by
                """
                SELECT *
                EXCLUDE
                    t.a
                FROM [
                    {
                        'a': 2,
                        'foo': 'bar2'
                    },
                    {
                        'a': 1,
                        'foo': 'bar1'
                    },
                    {
                        'a': 3,
                        'foo': 'bar3'
                    }
                ] AS t
                ORDER BY t.a
                """,
                """[{'foo': 'bar1'}, {'foo': 'bar2'}, {'foo': 'bar3'}]"""
            ),
            EvaluatorTestCase( // EXCLUDE select star with JOIN
                """
                SELECT *
                EXCLUDE bar.d
                FROM 
                <<
                    {'a': 1, 'b': 11}, 
                    {'a': 2, 'b': 22}
                >> AS foo,
                <<
                    {'c': 3, 'd': 33},
                    {'c': 4, 'd': 44}
                >> AS bar
                """,
                """<<{'a': 1, 'b': 11, 'c': 3}, {'a': 1, 'b': 11, 'c': 4}, {'a': 2, 'b': 22, 'c': 3}, {'a': 2, 'b': 22, 'c': 4}>>"""
            ),
            EvaluatorTestCase( // EXCLUDE select list with multiple fields in FROM source struct
                """
                SELECT t.b EXCLUDE t.b[*].b_1
                FROM <<
                {
                    'a': {'a_1':1,'a_2':2},
                    'b': [ {'b_1':3,'b_2':4}, {'b_1':5,'b_2':6} ],  -- every `b_1` to be excluded
                    'c': 7,
                    'd': 8
                } >> AS t
                """,
                """<<{'b': [{'b_2': 4}, {'b_2': 6}]}>>"""
            ),
            EvaluatorTestCase( // EXCLUDE select star with multiple fields in FROM source struct
                """
                SELECT * EXCLUDE t.b[*].b_1
                FROM <<
                {
                    'a': {'a_1':1,'a_2':2},
                    'b': [ {'b_1':3,'b_2':4}, {'b_1':5,'b_2':6} ],  -- every `b_1` to be excluded
                    'c': 7,
                    'd': 8
                } >> AS t
                """,
                """<<{'a': {'a_1': 1, 'a_2': 2}, 'b': [{'b_2': 4}, {'b_2': 6}], 'c': 7, 'd': 8}>>"""
            ),
            EvaluatorTestCase( // EXCLUDE select value with multiple fields in FROM source struct
                """
                SELECT VALUE t.b EXCLUDE t.b[*].b_1
                FROM <<
                {
                    'a': {'a_1':1,'a_2':2},
                    'b': [ {'b_1':3,'b_2':4}, {'b_1':5,'b_2':6} ],
                    'c': 7,
                    'd': 8
                } >> AS t
                """,
                """<<[{'b_2': 4}, {'b_2': 6}]>>"""
            ),
            EvaluatorTestCase( // EXCLUDE select star collection wildcard and nested tuple attr
                """
                SELECT * EXCLUDE t.a[*].b.c
                FROM <<
                    {
                        'a': [                                  -- `c` attr to be excluded from each element of `a`
                            { 'b': { 'c': 0, 'd': 'zero' } },
                            { 'b': { 'c': 1, 'd': 'one' } },
                            { 'b': { 'c': 2, 'd': 'two' } }
                        ]
                    }
                >> AS t
                """,
                """<<{'a': [{'b': {'d': 'zero'}}, {'b': {'d': 'one'}}, {'b': {'d': 'two'}}]}>>"""
            ),
            EvaluatorTestCase( // EXCLUDE select star collection index and nested tuple attr
                """
                SELECT * EXCLUDE t.a[1].b.c
                FROM <<
                    {
                        'a': [
                            { 'b': { 'c': 0, 'd': 'zero' } },
                            { 'b': { 'c': 1, 'd': 'one' } },    -- exclude `c` from just this index
                            { 'b': { 'c': 2, 'd': 'two' } }
                        ]
                    }
                >> AS t
                """,
                """<<{'a': [{'b': {'c': 0, 'd': 'zero'}}, {'b': {'d': 'one'}}, {'b': {'c': 2, 'd': 'two'}}]}>>"""
            ),
            EvaluatorTestCase( // EXCLUDE select star collection wildcard and nested tuple wildcard
                """
                SELECT * EXCLUDE t.a[*].b.*
                FROM <<
                    {
                        'a': [                                  -- exclude all of `b`'s attrs from each element of `a`
                            { 'b': { 'c': 0, 'd': 'zero' } },
                            { 'b': { 'c': 1, 'd': 'one' } },
                            { 'b': { 'c': 2, 'd': 'two' } }
                        ]
                    }
                >> AS t
                """,
                """<<{'a': [{'b': {}}, {'b': {}}, {'b': {}}]}>>"""
            ),
            EvaluatorTestCase( // EXCLUDE select star collection index and nested tuple wildcard
                """
                SELECT * EXCLUDE t.a[1].b.*
                FROM <<
                    {
                        'a': [
                            { 'b': { 'c': 0, 'd': 'zero' } },
                            { 'b': { 'c': 1, 'd': 'one' } },    -- exclude all of `b`'s attrs from just this index
                            { 'b': { 'c': 2, 'd': 'two' } }
                        ]
                    }
                >> AS t
                """,
                """<<{'a': [{'b': {'c': 0, 'd': 'zero'}}, {'b': {}}, {'b': {'c': 2, 'd': 'two'}}]}>>"""
            ),
            EvaluatorTestCase( // EXCLUDE select star collection wildcard and nested collection wildcard
                """
                SELECT * EXCLUDE t.a[*].b.d[*].e
                FROM <<
                    {
                        'a': [
                            { 'b': { 'c': 0, 'd': [{'e': 'zero0', 'f': true}, {'e': 'zero1', 'f': false}] } },  -- all `e` to be excluded
                            { 'b': { 'c': 1, 'd': [{'e': 'one0', 'f': true}, {'e': 'one1', 'f': false}] } },    -- all `e` to be excluded
                            { 'b': { 'c': 2, 'd': [{'e': 'two0', 'f': true}, {'e': 'two1', 'f': false}] } }     -- all `e` to be excluded
                        ]
                    }
                >> AS t
                """,
                """
                <<
                    {
                        'a': [
                            { 'b': { 'c': 0, 'd': [ { 'f': true }, { 'f': false } ] } }, 
                            { 'b': { 'c': 1, 'd': [ { 'f': true }, { 'f': false } ] } }, 
                            { 'b': { 'c': 2, 'd': [ { 'f': true }, { 'f': false } ] } }
                        ]
                    }
                >>
                """
            ),
            EvaluatorTestCase( // EXCLUDE select star collection index and nested collection wildcard
                """
                SELECT * EXCLUDE t.a[1].b.d[*].e
                FROM <<
                    {
                        'a': [
                            { 'b': { 'c': 0, 'd': [{'e': 'zero0', 'f': true}, {'e': 'zero1', 'f': false}] } },
                            { 'b': { 'c': 1, 'd': [{'e': 'one0', 'f': true}, {'e': 'one1', 'f': false}] } },    -- only `e` from this index to be excluded
                            { 'b': { 'c': 2, 'd': [{'e': 'two0', 'f': true}, {'e': 'two1', 'f': false}] } }
                        ]
                    }
                >> AS t
                """,
                """
                <<
                    {
                        'a': [
                            { 'b': { 'c': 0, 'd': [ { 'e': 'zero0', 'f': true }, { 'e': 'zero1', 'f': false } ] } }, 
                            { 'b': { 'c': 1, 'd': [ { 'f': true }, { 'f': false } ] } }, 
                            { 'b': { 'c': 2, 'd': [ { 'e': 'two0', 'f': true }, { 'e': 'two1', 'f': false } ] } }
                        ]
                    }
                >>
                """
            ),
            EvaluatorTestCase( // EXCLUDE select star collection index and nested collection index
                """
                SELECT * EXCLUDE t.a[1].b.d[0].e
                FROM <<
                    {
                        'a': [
                            { 'b': { 'c': 0, 'd': [{'e': 'zero0', 'f': true}, {'e': 'zero1', 'f': false}] } },
                            { 'b': { 'c': 1, 'd': [{'e': 'one0', 'f': true}, {'e': 'one1', 'f': false}] } },    -- `e` from 0-th index of `d` to be excluded
                            { 'b': { 'c': 2, 'd': [{'e': 'two0', 'f': true}, {'e': 'two1', 'f': false}] } }
                        ]
                    }
                >> AS t
                """,
                """
                <<
                    {
                        'a': [
                            { 'b': { 'c': 0, 'd': [ { 'e': 'zero0', 'f': true }, { 'e': 'zero1', 'f': false } ] } }, 
                            { 'b': { 'c': 1, 'd': [ { 'f': true }, { 'e': 'one1', 'f': false } ] } }, 
                            { 'b': { 'c': 2, 'd': [ { 'e': 'two0', 'f': true }, { 'e': 'two1', 'f': false } ] } }
                        ]
                    }
                >>
                """
            ),
            EvaluatorTestCase( // EXCLUDE select star tuple wildcard and subsequent tuple attr
                """
                SELECT *
                EXCLUDE
                    t.a.*.bar
                FROM [
                    {
                        'a': {
                            'b': { 'foo': 1, 'bar': 2 },
                            'c': { 'foo': 11, 'bar': 22 },
                            'd': { 'foo': 111, 'bar': 222 }
                        },
                        'foo': 'bar'
                    }
                ] AS t
                """,
                """
                <<
                    {
                        'a': {
                            'b': { 'foo': 1 },
                            'c': { 'foo': 11 },
                            'd': { 'foo': 111 }
                        }, 
                        'foo': 'bar'
                    }
                >>
                """
            ),
            EvaluatorTestCase( // EXCLUDE select star with ORDER BY
                """
                SELECT *
                EXCLUDE
                    t.a
                FROM [
                    {
                        'a': 2,
                        'foo': 'bar2'
                    },
                    {
                        'a': 1,
                        'foo': 'bar1'
                    },
                    {
                        'a': 3,
                        'foo': 'bar3'
                    }
                ] AS t
                ORDER BY t.a
                """,
                """
                [
                    {
                        'foo': 'bar1'
                    },
                    {
                        'foo': 'bar2'
                    },
                    {
                        'foo': 'bar3'
                    }
                ]
                """
            ),
            EvaluatorTestCase( // exclude select star with GROUP BY
                """
                SELECT *
                EXCLUDE g[*].t.c
                FROM 
                <<
                    { 'a': 1, 'b': 11, 'c': 111 },
                    { 'a': 1, 'b': 22, 'c': 222 },
                    { 'a': 2, 'b': 33, 'c': 333 }
                >> AS t 
                GROUP BY t.a AS a GROUP AS g
                """,
                """
                <<
                    {
                        'a': 1,
                        'g': <<
                            { 't': { 'a': 1, 'b': 11 } },
                            { 't': { 'a': 1, 'b': 22 } }
                        >>
                    },
                    {
                        'a': 2,
                        'g': <<
                            { 't': { 'a': 2, 'b': 33 } }
                        >>
                    }
                >>
                """
            ),
            EvaluatorTestCase( // EXCLUDE select star with DISTINCT
                """
                SELECT DISTINCT *
                EXCLUDE t.a
                FROM 
                <<
                    { 'a': 1, 'b': 11, 'c': 111 },
                    { 'a': 2, 'b': 11, 'c': 111 },  -- `b` and `c` same as above; `a` is different but will be excluded
                    { 'a': 1, 'b': 22, 'c': 222 }   -- `b` and `c` different from above two rows; will be kept
                >> AS t
                """,
                """
                <<
                    {'b': 11, 'c': 111},
                    {'b': 22, 'c': 222}
                >>
                """
            ),
            EvaluatorTestCase( // EXCLUDE select star with ORDER BY, LIMIT, OFFSET
                """
                SELECT DISTINCT *
                EXCLUDE t.a
                FROM 
                <<
                    { 'a': 1, 'b': 11, 'c': 111 },
                    { 'a': 2, 'b': 22, 'c': 222 },
                    { 'a': 3, 'b': 33, 'c': 333 },  -- kept
                    { 'a': 4, 'b': 44, 'c': 444 },  -- kept
                    { 'a': 5, 'b': 55, 'c': 555 }
                >> AS t
                ORDER BY a
                LIMIT 2
                OFFSET 2
                """,
                """
                [
                    {'b': 33, 'c': 333},
                    {'b': 44, 'c': 444}
                ]
                """
            ),
            EvaluatorTestCase( // EXCLUDE PIVOT
                """
                PIVOT t.v AT t.attr
                EXCLUDE t.v[*].excludeValue
                FROM 
                <<
                    { 'attr': 'a', 'v': [{'keepValue': 1, 'excludeValue': 11}, {'keepValue': 4, 'excludeValue': 44}] },
                    { 'attr': 'b', 'v': [{'keepValue': 2, 'excludeValue': 22}, {'keepValue': 5, 'excludeValue': 55}] },
                    { 'attr': 'c', 'v': [{'keepValue': 3, 'excludeValue': 33}, {'keepValue': 6, 'excludeValue': 66}] }
                >> AS t
                """,
                """
                {
                    'a': [{'keepValue': 1}, {'keepValue': 4}], 
                    'b': [{'keepValue': 2}, {'keepValue': 5}], 
                    'c': [{'keepValue': 3}, {'keepValue': 6}]
                }
                """
            ),
            EvaluatorTestCase( // EXCLUDE UNPIVOT
                """
                SELECT v, attr
                EXCLUDE v.foo
                FROM UNPIVOT 
                {
                    'a': {'foo': 1, 'bar': 11}, 
                    'a': {'foo': 2, 'bar': 22}, 
                    'b': {'foo': 3, 'bar': 33}
                } AS v AT attr
                """,
                """
                <<
                    {'v': {'bar': 11}, 'attr': 'a'}, 
                    {'v': {'bar': 22}, 'attr': 'a'}, 
                    {'v': {'bar': 33}, 'attr': 'b'}
                >>
                """
            ),
            EvaluatorTestCase( // EXCLUDE collection index on list
                """
                SELECT *
                EXCLUDE t.a[1]
                FROM 
                <<
                    {'a': [0, 1, 2]}  -- index `1` to be excluded
                >> AS t
                """,
                """<<{'a': [0, 2]}>>"""
            ),
            EvaluatorTestCase( // EXCLUDE collection index on bag -- nothing gets excluded; no error
                """
                SELECT *
                EXCLUDE t.a[1]
                FROM 
                <<
                    {'a': <<0, 1, 2>>}
                >> AS t
                """,
                """<<{'a': <<0, 1, 2>>}>>"""
            ),
            EvaluatorTestCase( // EXCLUDE collection index on sexp
                """
                SELECT *
                EXCLUDE t.a[1]
                FROM 
                <<
                    {'a': `(0 1 2)`}    -- index `1` to be excluded
                >> AS t
                """,
                """<<{'a': `(0 2)`}>>"""
            ),
            EvaluatorTestCase( // EXCLUDE collection wildcard on list
                """
                SELECT *
                EXCLUDE t.a[*]
                FROM 
                <<
                    {'a': [0, 1, 2]}    -- all indexes to be excluded; empty list as result
                >> AS t
                """,
                """<<{'a': []}>>"""
            ),
            EvaluatorTestCase( // EXCLUDE collection wildcard on bag
                """
                SELECT *
                EXCLUDE t.a[*]
                FROM 
                <<
                    {'a': <<0, 1, 2>>}  -- all indexes to be excluded; empty bag as result
                >> AS t
                """,
                """<<{'a': <<>>}>>"""
            ),
            EvaluatorTestCase( // EXCLUDE collection wildcard on sexp
                """
                SELECT *
                EXCLUDE t.a[*]
                FROM 
                <<
                    {'a': `(0 1 2)`}    -- all indexes to be excluded; empty sexp as result
                >> AS t
                """,
                """<<{'a': `()`}>>"""
            ),
            EvaluatorTestCase( // EXCLUDE with duplicates
                """
                SELECT *
                EXCLUDE t.a
                FROM 
                <<
                    {
                        'a': 1, -- to be excluded
                        'a': 2, -- to be excluded
                        'a': 3, -- to be excluded
                        'b': 4,
                        'c': 5
                    }
                >> AS t
                """,
                """<<{'b': 4, 'c': 5}>>"""
            ),
            EvaluatorTestCase( // EXCLUDE with non-existent paths; no error
                """
                SELECT *
                EXCLUDE t.path_does_not_exist, t.path_does_not_exist.*.foo, t.a['does not exist'], t.a
                FROM 
                <<{'a': 1, 'b': 2}>> AS t   -- only exclude `a`
                """,
                """<<{'b': 2}>>"""
            ),
            EvaluatorTestCase( // EXCLUDE with different FROM source bindings
                """
                SELECT *
                EXCLUDE t.a[*].bar, t.a.bar, t.a.*.bar  -- EXCLUDE all `bar`
                FROM 
                <<
                    {'a': [{'foo': 0, 'bar': 1, 'baz': 2}, {'foo': 3, 'bar': 4, 'baz': 5}]},
                    {'a': {'foo': 6, 'bar': 7, 'baz': 8}},
                    {'a': {'a1': {'foo': 9, 'bar': 10, 'baz': 11}, 'a2': {'foo': 12, 'bar': 13, 'baz': 14}}}
                >> AS t
                """,
                """
                <<
                    {'a': [{'foo': 0, 'baz': 2}, {'foo': 3, 'baz': 5}]}, 
                    {'a': {'foo': 6, 'baz': 8}}, 
                    {'a': {'a1': {'foo': 9, 'baz': 11}, 'a2': {'foo': 12, 'baz': 14}}}
                >>
                """
            ),
        )
    }

    @ParameterizedTest
    @ArgumentsSource(ExcludeTests::class)
    fun validExcludeTests(tc: EvaluatorTestCase) = testHarness.runEvaluatorTestCase(
        tc,
        EvaluationSession.standard()
    )
}
