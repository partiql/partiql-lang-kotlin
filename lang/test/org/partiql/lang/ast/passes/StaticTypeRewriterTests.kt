/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates.  All rights reserved.
 */

package org.partiql.lang.ast.passes

import junitparams.Parameters
import org.junit.Test
import org.partiql.lang.ast.CallAgg
import org.partiql.lang.ast.CaseSensitivity
import org.partiql.lang.ast.CreateIndex
import org.partiql.lang.ast.ExprNode
import org.partiql.lang.ast.Literal
import org.partiql.lang.ast.MetaContainer
import org.partiql.lang.ast.NAry
import org.partiql.lang.ast.NAryOp
import org.partiql.lang.ast.Path
import org.partiql.lang.ast.PathComponentExpr
import org.partiql.lang.ast.ScopeQualifier
import org.partiql.lang.ast.SourceLocationMeta
import org.partiql.lang.ast.StaticTypeMeta
import org.partiql.lang.ast.VariableReference
import org.partiql.lang.ast.emptyMetaContainer
import org.partiql.lang.ast.metaContainerOf
import org.partiql.lang.ast.plus
import org.partiql.lang.ast.sourceLocation
import org.partiql.lang.ast.staticType
import org.partiql.lang.errors.ErrorCode
import org.partiql.lang.errors.Property
import org.partiql.lang.errors.Property.BINDING_NAME
import org.partiql.lang.errors.Property.COLUMN_NUMBER
import org.partiql.lang.errors.Property.FEATURE_NAME
import org.partiql.lang.errors.Property.LINE_NUMBER
import org.partiql.lang.eval.Bindings
import org.partiql.lang.types.StaticType
import java.io.PrintWriter
import java.io.StringWriter

class StaticTypeRewriterTests : RewriterTestBase() {

    data class STRTestCase(val originalSql: String,
                           val globals: Map<String, StaticType>,
                           val handler: (ResolveTestResult) -> Unit,
                           val constraints: Set<StaticTypeRewriterConstraints> = setOf(),
                           val expectedAst: String? = null) {

        override fun toString(): String =
            "originalSql=$originalSql, globals=$globals, constraints=$constraints, expectedSql=$expectedAst"
    }

    private data class VarExpectation(
        val id: String,
        val line: Long,
        val charOffset: Long,
        val staticType: StaticType,
        val scopeQualifier: ScopeQualifier
    )

    @Test
    @Parameters
    fun sfwTest(tc: STRTestCase) = runSTRTest(tc)
    
    
    // In the test cases below there exists comments consisting of a bunch of numbers.  They can be 
    // used to quickly determine the column number of the text immediately beneath it.  For example, 
    // it's easy to see the token "fiftyFive" starts at character 55:
    //        1         2         3         4         5         6         7         8
    //2345678901234567890123456789012345678901234567890123456789012345678901234567890
    //                                                     fiftyFive
    // The first line is the 10's place of the column number, while the second line is the 1's place.
    // This helps to speed up the finding of the column number when it is used as part of the 
    // expectation.
    fun parametersForSfwTest() = listOf(
        STRTestCase(
            "b",
            mapOf("b" to StaticType.BAG),
            expectVariableReferences(
                VarExpectation("b", 1, 1, StaticType.BAG, ScopeQualifier.UNQUALIFIED)
            )
        ),
        STRTestCase(
            //        1         2         3         4         5         6         7         8
            //2345678901234567890123456789012345678901234567890123456789012345678901234567890
            "SELECT x FROM b AS x",
            mapOf("b" to StaticType.BAG),
            expectVariableReferences(
                VarExpectation("x", 1, 8, StaticType.ANY, ScopeQualifier.LEXICAL),
                VarExpectation("b", 1, 15, StaticType.BAG, ScopeQualifier.UNQUALIFIED)
            )
        ),
        STRTestCase(
            //        1         2         3         4         5         6         7         8
            //2345678901234567890123456789012345678901234567890123456789012345678901234567890
            "SELECT a FROM b",
            mapOf(
                "B" to StaticType.LIST,
                "a" to StaticType.INT
            ),
            expectVariableReferences(
                VarExpectation("b", 1, 8, StaticType.ANY, ScopeQualifier.LEXICAL),
                VarExpectation("b", 1, 15, StaticType.LIST, ScopeQualifier.UNQUALIFIED)
                // There is no variable reference to `a` here since `SELECT a FROM b`
                // is rewritten to `SELECT b.a FROM b`
            )
        ),
        STRTestCase(
            //        1         2         3         4         5         6         7         8
            //2345678901234567890123456789012345678901234567890123456789012345678901234567890
            "SELECT COUNT(*) FROM b",
            mapOf("B" to StaticType.BAG),
            expectVariableReferences(
                VarExpectation("b", 1, 22, StaticType.BAG, ScopeQualifier.UNQUALIFIED)
            )
        ),
        STRTestCase(
            //        1         2         3         4         5         6         7         8
            //2345678901234567890123456789012345678901234567890123456789012345678901234567890
            "SELECT y FROM b AS x",
            mapOf("B" to StaticType.BAG),
            expectSubNode(
                Path(
                    VariableReference(
                        "x",
                        CaseSensitivity.SENSITIVE,
                        ScopeQualifier.LEXICAL,
                        StaticType.ANY.toMetas() + metas(1, 8)
                    ),
                    listOf(
                        PathComponentExpr(
                            Literal(ion.newString("y"), metas(1, 8)),
                            CaseSensitivity.INSENSITIVE
                        )
                    ),
                    metas(1, 8)
                )
            )
        ),
        STRTestCase(
            //        1         2         3         4         5         6         7         8
            //2345678901234567890123456789012345678901234567890123456789012345678901234567890
            "SELECT y.z FROM b AS x",
            mapOf("B" to StaticType.BAG),
            expectSubNode(
                Path(
                    VariableReference(
                        "x",
                        CaseSensitivity.SENSITIVE,
                        ScopeQualifier.LEXICAL,
                        StaticType.ANY.toMetas() + metas(1, 8)
                    ),
                    listOf(
                        PathComponentExpr(
                            Literal(ion.newString("y"), metas(1, 8)),
                            CaseSensitivity.INSENSITIVE
                        ),
                        PathComponentExpr(
                            Literal(ion.newString("z"), metas(1, 10)),
                            CaseSensitivity.INSENSITIVE
                        )
                    ),
                    emptyMetaContainer
                )
            )
        ),
        STRTestCase(
            //        1         2         3         4         5         6         7         8
            //2345678901234567890123456789012345678901234567890123456789012345678901234567890
            "SELECT x FROM b AT x",
            mapOf("b" to StaticType.BAG),
            expectVariableReferences(
                VarExpectation("x", 1, 8, StaticType.ANY, ScopeQualifier.LEXICAL),
                VarExpectation("b", 1, 15, StaticType.BAG, ScopeQualifier.UNQUALIFIED)
            )
        ),
        STRTestCase(
            //        1         2         3         4         5         6         7         8
            //2345678901234567890123456789012345678901234567890123456789012345678901234567890
            "SELECT y FROM b AS x, x AS whatever AT y",
            mapOf("b" to StaticType.BAG),
            expectVariableReferences(
                VarExpectation("y", 1, 8, StaticType.ANY, ScopeQualifier.LEXICAL),
                VarExpectation("b", 1, 15, StaticType.BAG, ScopeQualifier.UNQUALIFIED),
                VarExpectation("x", 1, 23, StaticType.ANY, ScopeQualifier.LEXICAL)
            )
        ),
        STRTestCase(
            //        1         2         3         4         5         6         7         8
            //2345678901234567890123456789012345678901234567890123456789012345678901234567890
            "SELECT a FROM b",
            emptyMap(),
            expectErr(ErrorCode.SEMANTIC_UNBOUND_BINDING,
                BINDING_NAME to "b",
                LINE_NUMBER to 1L,
                COLUMN_NUMBER to 15L)
        ),
        STRTestCase(
            //        1         2         3         4         5         6         7         8
            //2345678901234567890123456789012345678901234567890123456789012345678901234567890
            "SELECT a FROM b,c",
            mapOf("B" to StaticType.ANY,
                  "C" to StaticType.ANY),
            expectErr(ErrorCode.SEMANTIC_UNBOUND_BINDING,
                BINDING_NAME to "a",
                LINE_NUMBER to 1L,
                COLUMN_NUMBER to 8L)
        ),
        STRTestCase(
            //        1         2         3         4         5         6         7         8
            //2345678901234567890123456789012345678901234567890123456789012345678901234567890
            "SELECT a FROM \"b\"",
            mapOf("B" to StaticType.ANY),
            expectErr(ErrorCode.SEMANTIC_UNBOUND_BINDING,
                BINDING_NAME to "b",
                LINE_NUMBER to 1L,
                COLUMN_NUMBER to 15L)
        ),
        STRTestCase(
            //        1         2         3         4         5         6         7         8
            //2345678901234567890123456789012345678901234567890123456789012345678901234567890
            "SELECT a FROM b, c",
            mapOf("B" to StaticType.ANY,
                  "C" to StaticType.ANY),
            expectErr(ErrorCode.SEMANTIC_UNBOUND_BINDING,
                BINDING_NAME to "a",
                LINE_NUMBER to 1L,
                COLUMN_NUMBER to 8L)
        ),
        // variable scoping within SELECT should resolve implicit lexical alias over global
        STRTestCase(
            //        1         2         3         4         5         6         7         8
            //2345678901234567890123456789012345678901234567890123456789012345678901234567890
            "SELECT a FROM b",
            mapOf(
                "b" to StaticType.BAG,
                "a" to StaticType.BAG),
            expectSubNode(Path(
                VariableReference(
                    "b",
                    CaseSensitivity.SENSITIVE,
                    ScopeQualifier.LEXICAL,
                    StaticType.ANY.toMetas() + metas(1, 8)
                ),
                listOf(
                    PathComponentExpr(
                        Literal(ion.newString("a"), metas(1, 8)),
                        CaseSensitivity.INSENSITIVE
                    )
                ),
                metas(1, 8)
            ))
        ),
        // ambiguous binding introduced in FROM clause (same AS-binding introduced twice)
        STRTestCase(
            //        1         2         3         4         5         6         7         8
            //2345678901234567890123456789012345678901234567890123456789012345678901234567890
            "SELECT * FROM b AS b, b AS B",
            mapOf("B" to StaticType.ANY),
            expectErr(ErrorCode.SEMANTIC_AMBIGUOUS_BINDING,
                BINDING_NAME to "B",
                LINE_NUMBER to 1L,
                COLUMN_NUMBER to 28L)
        ),
        // ambiguous binding introduced in FROM clause (AS binding given same name as AT binding)
        STRTestCase(
            //        1         2         3         4         5         6         7         8
            //2345678901234567890123456789012345678901234567890123456789012345678901234567890
            "SELECT * FROM b AS b AT b",
            mapOf("B" to StaticType.ANY),
            expectErr(ErrorCode.SEMANTIC_AMBIGUOUS_BINDING,
                BINDING_NAME to "b",
                LINE_NUMBER to 1L,
                COLUMN_NUMBER to 20L)
        ),
        // ambiguous binding introduced in FROM clause (AS binding given same name as BY binding)
        STRTestCase(
            //        1         2         3         4         5         6         7         8
            //2345678901234567890123456789012345678901234567890123456789012345678901234567890
            "SELECT * FROM b AS b BY b",
            mapOf("B" to StaticType.ANY),
            expectErr(ErrorCode.SEMANTIC_AMBIGUOUS_BINDING,
                BINDING_NAME to "b",
                LINE_NUMBER to 1L,
                COLUMN_NUMBER to 20L)
        ),
        // ambiguous binding introduced in FROM clause (AT binding given same name as BY binding)
        STRTestCase(
            //        1         2         3         4         5         6         7         8
            //2345678901234567890123456789012345678901234567890123456789012345678901234567890
            "SELECT * FROM b AS x AT b BY b",
            mapOf("B" to StaticType.ANY),
            expectErr(ErrorCode.SEMANTIC_AMBIGUOUS_BINDING,
                BINDING_NAME to "b",
                LINE_NUMBER to 1L,
                COLUMN_NUMBER to 30L)
        ),
        // join should not allow implicit attribute without schema
        STRTestCase(
            //        1         2         3         4         5         6         7         8
            //2345678901234567890123456789012345678901234567890123456789012345678901234567890
            "SELECT a FROM b AS x, B AS y",
            mapOf("B" to StaticType.ANY),
            expectErr(ErrorCode.SEMANTIC_UNBOUND_BINDING,
                BINDING_NAME to "a",
                LINE_NUMBER to 1L,
                COLUMN_NUMBER to 8L)
        ),
        // nested query should not allow implicit attribute as variable without schema
        STRTestCase(
            //        1         2         3         4         5         6         7         8
            //2345678901234567890123456789012345678901234567890123456789012345678901234567890
            "SELECT a FROM b AS x WHERE EXISTS (SELECT y FROM x)",
            mapOf("B" to StaticType.ANY),
            expectErr(ErrorCode.SEMANTIC_UNBOUND_BINDING,
                BINDING_NAME to "y",
                LINE_NUMBER to 1L,
                COLUMN_NUMBER to 43L)
        ),

        // local variable with same name as global should not shadow global in from source
        STRTestCase(
            //        1         2         3         4         5         6         7         8
            //2345678901234567890123456789012345678901234567890123456789012345678901234567890
            "SELECT 1 FROM a AS b, b AS c, c as d",
            mapOf("a" to StaticType.ANY,
                  "b" to StaticType.ANY),
            expectVariableReferences(
                VarExpectation("a", 1, 15, StaticType.ANY, ScopeQualifier.UNQUALIFIED),
                // The [VarExpectation] below proves that the "b" in the "b AS c" from source was resolved in the global scope.
                VarExpectation("b", 1, 23, StaticType.ANY, ScopeQualifier.UNQUALIFIED),
                VarExpectation("c", 1, 31, StaticType.ANY, ScopeQualifier.LEXICAL))
        ),

        // @ causes the local b to be resolved instead of the global b.
        STRTestCase(
            //        1         2         3         4         5         6         7         8
            //2345678901234567890123456789012345678901234567890123456789012345678901234567890
            "SELECT 1 FROM b as b, @b.c",
            mapOf(
                "B" to StaticType.BAG
            ),
            expectVariableReferences(
                VarExpectation("b", 1, 15, StaticType.BAG, ScopeQualifier.UNQUALIFIED),
                VarExpectation("b", 1, 24, StaticType.ANY, ScopeQualifier.LEXICAL)
            )
        ),
        // Group By should not be allowed
        STRTestCase(
            //        1         2         3         4         5         6         7         8
            //2345678901234567890123456789012345678901234567890123456789012345678901234567890
            "SELECT * FROM b as x GROUP BY x.name",
            mapOf("b" to StaticType.ANY),
            expectErr(ErrorCode.UNIMPLEMENTED_FEATURE, FEATURE_NAME to "GROUP BY")
        )
    )

    @Test
    @Parameters
    fun dmlTest(tc: STRTestCase) = runSTRTest(tc)

    fun parametersForDmlTest() = listOf(
        // DML happy paths
        STRTestCase(
            //        1         2         3         4         5         6         7         8
            //2345678901234567890123456789012345678901234567890123456789012345678901234567890
            "FROM x INSERT INTO y << 'doesnt matter' >>",
            mapOf("x" to StaticType.BAG,
                  "y" to StaticType.BOOL),
            expectVariableReferences(
                VarExpectation("x", 1, 6, StaticType.BAG, ScopeQualifier.UNQUALIFIED),
                VarExpectation("x", 1, 20, StaticType.ANY, ScopeQualifier.LEXICAL))
            //No expectation for y because `FROM x INSERT INTO y ...` is rewritten to `FROM x INSERT INTO x.y ...`
        ),
        STRTestCase(
            //        1         2         3         4         5         6         7         8
            //2345678901234567890123456789012345678901234567890123456789012345678901234567890
            "INSERT INTO x VALUE 5",
            mapOf("x" to StaticType.BAG),
            expectVariableReferences(
                VarExpectation("x", 1, 13, StaticType.BAG, ScopeQualifier.UNQUALIFIED)
            )
        ),
        STRTestCase(
            //        1         2         3         4         5         6         7         8
            //2345678901234567890123456789012345678901234567890123456789012345678901234567890
            "FROM x INSERT INTO y VALUE 5",
            mapOf(
                "x" to StaticType.BAG,
                "y" to StaticType.BOOL),
            expectVariableReferences(
                VarExpectation("x", 1, 6, StaticType.BAG, ScopeQualifier.UNQUALIFIED),
                VarExpectation("x", 1, 20, StaticType.ANY, ScopeQualifier.LEXICAL))
            //No expectation for y because `FROM x INSERT INTO y ...` is rewritten to `FROM x INSERT INTO x.y ...`
        ),
        STRTestCase(
            //        1         2         3         4         5         6         7         8
            //2345678901234567890123456789012345678901234567890123456789012345678901234567890
            "FROM x AS y SET doesntmatter = 1",
            mapOf("x" to StaticType.BAG),
            expectVariableReferences(
                VarExpectation("x", 1, 6, StaticType.BAG, ScopeQualifier.UNQUALIFIED),
                VarExpectation("y", 1, 17, StaticType.ANY, ScopeQualifier.LEXICAL)
            )
        ),
        STRTestCase(
            //        1         2         3         4         5         6         7         8
            //2345678901234567890123456789012345678901234567890123456789012345678901234567890
            "DELETE FROM x WHERE y",
            mapOf("x" to StaticType.BAG,
                  "y" to StaticType.BOOL),
            expectVariableReferences(
                VarExpectation("x", 1, 13, StaticType.BAG, ScopeQualifier.UNQUALIFIED),
                VarExpectation("x", 1, 21, StaticType.ANY, ScopeQualifier.LEXICAL)
            )
        ),
        STRTestCase(
            //        1         2         3         4         5         6         7         8
            //2345678901234567890123456789012345678901234567890123456789012345678901234567890
            "FROM x WHERE z REMOVE y",
            mapOf("x" to StaticType.BAG,
                  "y" to StaticType.BOOL,
                  "z" to StaticType.INT),
            expectVariableReferences(
                VarExpectation("x", 1, 6, StaticType.BAG, ScopeQualifier.UNQUALIFIED),
                VarExpectation("x", 1, 14, StaticType.ANY, ScopeQualifier.LEXICAL),
                VarExpectation("x", 1, 23, StaticType.ANY, ScopeQualifier.LEXICAL)
            )
        ),
        STRTestCase(
            //        1         2         3         4         5         6         7         8
            //2345678901234567890123456789012345678901234567890123456789012345678901234567890
            "FROM canines AS dogs, dogs AS d WHERE d.name = 'Timmy' SET d.colour = 'blue merle'",
            mapOf("canines" to StaticType.BAG),
            expectVariableReferences(
                VarExpectation("canines", 1, 6, StaticType.BAG, ScopeQualifier.UNQUALIFIED),
                VarExpectation("dogs", 1, 23, StaticType.ANY, ScopeQualifier.LEXICAL),
                VarExpectation("d", 1, 39, StaticType.ANY, ScopeQualifier.LEXICAL),
                VarExpectation("d", 1, 60, StaticType.ANY, ScopeQualifier.LEXICAL)
            )
        ),
        STRTestCase(
            //        1         2         3         4         5         6         7         8
            //2345678901234567890123456789012345678901234567890123456789012345678901234567890
            "FROM animals AS a, a.dogs AS d WHERE d.name = 'Timmy' SET d.colour = 'blue merle'",
            mapOf("animals" to StaticType.BAG),
            expectVariableReferences(
                VarExpectation("animals", 1, 6, StaticType.BAG, ScopeQualifier.UNQUALIFIED),
                VarExpectation("a", 1, 20, StaticType.ANY, ScopeQualifier.LEXICAL),
                VarExpectation("d", 1, 38, StaticType.ANY, ScopeQualifier.LEXICAL),
                VarExpectation("d", 1, 59, StaticType.ANY, ScopeQualifier.LEXICAL)
            )
        ),
        // DML undefined variables.
        STRTestCase(
            //        1         2         3         4         5         6         7         8
            //2345678901234567890123456789012345678901234567890123456789012345678901234567890
            "FROM y SET doesntmatter = 1",
            mapOf(),
            expectErr(ErrorCode.SEMANTIC_UNBOUND_BINDING,
                BINDING_NAME to "y",
                LINE_NUMBER to 1L,
                COLUMN_NUMBER to 6L)
        ),
        STRTestCase(
            //        1         2         3         4         5         6         7         8
            //2345678901234567890123456789012345678901234567890123456789012345678901234567890
            "DELETE FROM x as y WHERE z",
            mapOf("x" to StaticType.BAG),
            expectVariableReferences(
                VarExpectation("x", 1, 13, StaticType.BAG, ScopeQualifier.UNQUALIFIED),
                VarExpectation("y", 1, 26, StaticType.ANY, ScopeQualifier.LEXICAL)
            )
        ),
        STRTestCase(
            //        1         2         3         4         5         6         7         8
            //2345678901234567890123456789012345678901234567890123456789012345678901234567890
            "UPDATE dogs as d SET name = 'Timmy' WHERE color = 'Blue merle'",
            mapOf("dogs" to StaticType.BAG),
            expectVariableReferences(
                VarExpectation("dogs", 1, 8, StaticType.BAG, ScopeQualifier.UNQUALIFIED),
                VarExpectation("d", 1, 22, StaticType.ANY, ScopeQualifier.LEXICAL),
                VarExpectation("d", 1, 43, StaticType.ANY, ScopeQualifier.LEXICAL))
        )
    )


    @Test
    @Parameters
    fun ddlTest(tc: STRTestCase) = runSTRTest(tc)

    fun parametersForDdlTest() = listOf(
        // Regression test:  do not attempt to resolve any variables for a DROP INDEX statement.
        // DropIndex incorrectly models the index identifier as a [VariableReference]
        // and this test ensures we don't treat [identifier] as if it were a normal variable.
        STRTestCase(
            //        1         2         3         4         5         6         7         8
            //2345678901234567890123456789012345678901234567890123456789012345678901234567890
            "DROP INDEX IDX_foo ON SomeTable",
            mapOf(),
            expectVariableReferences()
        ),
        // Regression test:  do not attempt to resolve any variables for a CREATE INDEX statement.
        // CreateIndex incorrectly models the index identifier as a [List<ExprNode>]
        // and this test ensures we don't treat [keys] as if it were a normal variable.
        STRTestCase(
            //        1         2         3         4         5         6         7         8
            //2345678901234567890123456789012345678901234567890123456789012345678901234567890
            "CREATE INDEX ON SomeTable (SomeColumn)",
            mapOf(),
            expectVariableReferences()
        )
    )

    @Test
    @Parameters
    fun constraints(tc: STRTestCase) = runSTRTest(tc)

    fun parametersForConstraints() = listOf(
        // with PREVENT_GLOBALS_EXCEPT_IN_FROM option
        STRTestCase(
            // even though a global 'a' is defined, it is not accessible.
            // (need the JOIN because a is rewritten to b.a when there is only one from source)
            //        1         2         3         4         5         6         7         8
            //2345678901234567890123456789012345678901234567890123456789012345678901234567890
            "SELECT a FROM b, c",
            mapOf(
                "B" to StaticType.BAG,
                "A" to StaticType.BAG,
                "C" to StaticType.BAG
            ),
            expectErr(ErrorCode.SEMANTIC_ILLEGAL_GLOBAL_VARIABLE_ACCESS,
                BINDING_NAME to "a",
                LINE_NUMBER to 1L,
                COLUMN_NUMBER to 8L),
            setOf(StaticTypeRewriterConstraints.PREVENT_GLOBALS_EXCEPT_IN_FROM)
        ),
        STRTestCase(
            // Verify that a shadowed global ("b") doesn't get resolved instead of illegal global access error.
            //        1         2         3         4         5         6         7         8
            //2345678901234567890123456789012345678901234567890123456789012345678901234567890
            "select * from a as b, (select * from b)",
            mapOf(
                "a" to StaticType.BAG,
                "b" to StaticType.BAG
            ),
            expectErr(ErrorCode.SEMANTIC_ILLEGAL_GLOBAL_VARIABLE_ACCESS,
                BINDING_NAME to "b",
                LINE_NUMBER to 1L,
                COLUMN_NUMBER to 38L),
            setOf(StaticTypeRewriterConstraints.PREVENT_GLOBALS_IN_NESTED_QUERIES)
        ),
        STRTestCase(
            // basic happy path with PREVENT_GLOBALS_EXCEPT_IN_FROM
            //        1         2         3         4         5         6         7         8
            //2345678901234567890123456789012345678901234567890123456789012345678901234567890
            "SELECT b1.a FROM b AS b1",
            mapOf(
                "B" to StaticType.BAG
            ),
            expectVariableReferences(
                VarExpectation("b1", 1, 8, StaticType.ANY, ScopeQualifier.LEXICAL),
                VarExpectation("b", 1, 18, StaticType.BAG, ScopeQualifier.UNQUALIFIED)
            ),
            setOf(StaticTypeRewriterConstraints.PREVENT_GLOBALS_EXCEPT_IN_FROM)
        ),
        STRTestCase(
            // nested happy case with PREVENT_GLOBALS_EXCEPT_IN_FROM
            //        1         2         3         4         5         6         7         8
            //2345678901234567890123456789012345678901234567890123456789012345678901234567890
            "SELECT 1, (SELECT c1.d FROM c as c1) FROM b, (SELECT 2 FROM c)",
            mapOf(
                "B" to StaticType.BAG,
                "C" to StaticType.BAG
            ),
            expectVariableReferences(
                VarExpectation("c1", 1, 19, StaticType.ANY, ScopeQualifier.LEXICAL),
                VarExpectation("c", 1, 29, StaticType.BAG, ScopeQualifier.UNQUALIFIED),
                VarExpectation("b", 1, 43, StaticType.BAG, ScopeQualifier.UNQUALIFIED),
                VarExpectation("c", 1, 61, StaticType.BAG, ScopeQualifier.UNQUALIFIED)
            ),
            setOf(StaticTypeRewriterConstraints.PREVENT_GLOBALS_EXCEPT_IN_FROM)
        ),
        // multiple joins
        STRTestCase(
            //        1         2         3         4         5         6         7         8
            //2345678901234567890123456789012345678901234567890123456789012345678901234567890
            "select 1 from actors a, a.movies am, movies m",
            mapOf("movies" to StaticType.BAG,
                  "actors" to StaticType.BAG),
            expectVariableReferences(
                VarExpectation("actors", 1, 15, StaticType.BAG, ScopeQualifier.UNQUALIFIED),
                VarExpectation("a", 1, 25, StaticType.ANY, ScopeQualifier.LEXICAL),
                VarExpectation("movies", 1, 38, StaticType.BAG, ScopeQualifier.UNQUALIFIED)
            ),
            setOf(StaticTypeRewriterConstraints.PREVENT_GLOBALS_EXCEPT_IN_FROM)),
        STRTestCase(
            // failure case with PREVENT_GLOBALS_IN_NESTED_QUERIES though
            //        1         2         3         4         5         6         7         8
            //2345678901234567890123456789012345678901234567890123456789012345678901234567890
            "SELECT 1, (SELECT c1.d FROM c as c1) FROM b",
            mapOf(
                "B" to StaticType.BAG,
                "C" to StaticType.BAG
            ),
            expectErr(ErrorCode.SEMANTIC_ILLEGAL_GLOBAL_VARIABLE_ACCESS,
                BINDING_NAME to "c",
                LINE_NUMBER to 1L,
                COLUMN_NUMBER to 29L),
            setOf(StaticTypeRewriterConstraints.PREVENT_GLOBALS_IN_NESTED_QUERIES)
        ),
        STRTestCase(
            // checking PREVENT_GLOBALS_IN_NESTED_QUERIES failure within outer FROM
            //        1         2         3         4         5         6         7         8
            //2345678901234567890123456789012345678901234567890123456789012345678901234567890
            "SELECT 1 FROM b, (SELECT c1.d FROM c as c1)",
            mapOf(
                "B" to StaticType.BAG,
                "C" to StaticType.BAG
            ),
            expectErr(ErrorCode.SEMANTIC_ILLEGAL_GLOBAL_VARIABLE_ACCESS,
                BINDING_NAME to "c",
                LINE_NUMBER to 1L,
                COLUMN_NUMBER to 36L),
            setOf(StaticTypeRewriterConstraints.PREVENT_GLOBALS_IN_NESTED_QUERIES)
        ),
        STRTestCase(
            // nested happy case with PREVENT_GLOBALS_IN_NESTED_QUERIES
            //        1         2         3         4         5         6         7         8
            //2345678901234567890123456789012345678901234567890123456789012345678901234567890
            "SELECT 1, (SELECT c1.d FROM c as c1) FROM b as b, @b.c as c1, (SELECT 2 FROM c1)",
            mapOf(
                "B" to StaticType.BAG,
                "C" to StaticType.BAG
            ),
            expectVariableReferences(
                // No scope qualifier below because `b` is referenced twice: once with the lexical scope
                // qualifier, and once without.
                VarExpectation("c1", 1, 19, StaticType.ANY, ScopeQualifier.LEXICAL),
                VarExpectation("c", 1, 29, StaticType.BAG, ScopeQualifier.UNQUALIFIED),
                VarExpectation("b", 1, 43, StaticType.BAG, ScopeQualifier.UNQUALIFIED),
                VarExpectation("b", 1, 52, StaticType.ANY, ScopeQualifier.LEXICAL),
                VarExpectation("c1", 1, 78, StaticType.ANY, ScopeQualifier.LEXICAL)
            ),
            setOf(StaticTypeRewriterConstraints.PREVENT_GLOBALS_EXCEPT_IN_FROM)
        ),
        STRTestCase(
            //        1         2         3         4         5         6         7         8
            //2345678901234567890123456789012345678901234567890123456789012345678901234567890
            "select collies FROM dogs d",
            mapOf("dogs" to StaticType.BAG,
                  "collies" to StaticType.BAG),
            expectSubNode(
                Path(
                    VariableReference(
                        "d",
                        CaseSensitivity.SENSITIVE,
                        ScopeQualifier.LEXICAL,
                        StaticType.ANY.toMetas() + metas(1, 8)
                    ),
                    listOf(
                        PathComponentExpr(
                            Literal(ion.newString("collies"), metas(1, 8)),
                            CaseSensitivity.INSENSITIVE
                        )
                    ),
                    metas(1, 8)
                )),
            setOf(StaticTypeRewriterConstraints.PREVENT_GLOBALS_EXCEPT_IN_FROM)
        ),
        // DML with PREVENT_GLOBALS_EXCEPT_IN_FROM
        STRTestCase(
            //        1         2         3         4         5         6         7         8
            //2345678901234567890123456789012345678901234567890123456789012345678901234567890
            "FROM dogs d INSERT INTO collies VALUE 'Timmy'",
            mapOf("dogs" to StaticType.BAG,
                  "collies" to StaticType.BAG),
            expectSubNode(
                Path(
                    VariableReference(
                        "d",
                        CaseSensitivity.SENSITIVE,
                        ScopeQualifier.LEXICAL,
                        StaticType.ANY.toMetas() + metas(1, 25)
                    ),
                    listOf(
                        PathComponentExpr(
                            Literal(ion.newString("collies"), metas(1, 8)),
                            CaseSensitivity.INSENSITIVE
                        )
                    ),
                    metas(1, 25)
                )
            ),
            setOf(StaticTypeRewriterConstraints.PREVENT_GLOBALS_EXCEPT_IN_FROM)),
        STRTestCase(
            //        1         2         3         4         5         6         7         8
            //2345678901234567890123456789012345678901234567890123456789012345678901234567890
            "INSERT INTO dogs VALUE 'Timmy'",
            mapOf("dogs" to StaticType.BAG),
            expectVariableReferences(
                VarExpectation("dogs", 1, 13, StaticType.BAG, ScopeQualifier.UNQUALIFIED)
            ),
            setOf(StaticTypeRewriterConstraints.PREVENT_GLOBALS_EXCEPT_IN_FROM)),
        // captures the var ranging over dogs and implicitly prefixes id and owner
        STRTestCase(
            //        1         2         3         4         5         6         7         8
            //2345678901234567890123456789012345678901234567890123456789012345678901234567890
            "UPDATE dogs d SET name = 'Timmy' WHERE owner = 'Margaret'",
            mapOf("dogs" to StaticType.BAG),
            expectVariableReferences(
                VarExpectation("dogs", 1, 8, StaticType.BAG, ScopeQualifier.UNQUALIFIED),
                VarExpectation("d", 1, 19, StaticType.ANY, ScopeQualifier.LEXICAL),
                VarExpectation("d", 1, 40, StaticType.ANY, ScopeQualifier.LEXICAL)),
            setOf(StaticTypeRewriterConstraints.PREVENT_GLOBALS_EXCEPT_IN_FROM))
    )

    sealed class ResolveTestResult {
        data class Value(val testCase: STRTestCase, val node: ExprNode) : ResolveTestResult()
        data class Error(val testCase: STRTestCase, val error: SemanticException) : ResolveTestResult()
    }

    private fun expectErr(code: ErrorCode, vararg properties: Pair<Property, Any>): (ResolveTestResult) -> Unit = {
        when (it) {
            is ResolveTestResult.Value -> fail("Expected id error for: ${it.testCase.originalSql}")
            is ResolveTestResult.Error -> {
                assertEquals("Error code in error doesn't match", code, it.error.errorCode)
                properties.forEach { (property, expectedValue) ->
                    assertEquals(
                        "${property.propertyName} in error doesn't match",
                        expectedValue, it.error.errorContext?.get(property)?.value)

                }
            }
        }
    }

    private fun Exception.stackTraceAsString(): String =
        StringWriter().use { sw ->
            PrintWriter(sw).use { pw ->
                this.printStackTrace(pw)
                return sw.toString()
            }
        }

    private fun expectVariableReferences(vararg varExpectations: VarExpectation): (ResolveTestResult) -> Unit = { result: ResolveTestResult ->
        when (result) {
            is ResolveTestResult.Error -> fail("Expected value, not failure.  Stack trace: \n${result.error.stackTraceAsString()}")
            is ResolveTestResult.Value -> {
                val remainingExpectations = varExpectations.toMutableList()

                // all variable reference nodes should be tagged
                val visitor = object : AstVisitorBase() {
                    override fun visitExprNode(expr: ExprNode) {
                        when (expr) {
                            is VariableReference -> {
                                val sourceLocationMeta = expr.metas.sourceLocation
                                                         ?: error("VariableReference '${expr.id}' had no SourceLocationMeta")

                                // Find a VarExpectation that matches the given VariableReference
                                val matchingExpectation = remainingExpectations.firstOrNull {
                                    it.id == expr.id &&
                                    it.line == sourceLocationMeta.lineNum &&
                                    it.charOffset == sourceLocationMeta.charOffset
                                } ?: error("No expectation found for VariableReference ${expr.id} at $sourceLocationMeta")

                                remainingExpectations.remove(matchingExpectation)

                                assertEquals(
                                    "VariableReference '${expr.id}' at $sourceLocationMeta scope qualifier must match expectation",
                                    matchingExpectation.scopeQualifier,
                                    expr.scopeQualifier
                                )

                                val staticTypeMeta = expr.metas.staticType ?:
                                                     error("VariableReference '${expr.id}' at $sourceLocationMeta had no StaticTypeMeta")

                                assertEquals(
                                    "VariableReference ${expr.id} at $sourceLocationMeta static type must match expectation",
                                    staticTypeMeta.type,
                                    matchingExpectation.staticType
                                )
                            }
                            else -> { /* do nothing */ }
                        }
                    }
                }

                object : AstWalker(visitor) {
                    override fun walkExprNode(vararg exprs: ExprNode?) {
                        exprs.filterNotNull().forEach { expr: ExprNode ->
                            //do not walk the name of a function call this should be a symbolic name in another namespace (AST is over generalized here)
                            when {
                                expr is NAry && expr.op == NAryOp.CALL -> {
                                    visitor.visitExprNode(expr)
                                    expr.args.drop(1).forEach { this.walkExprNode(it) }
                                    return
                                }
                                expr is CallAgg                        -> {
                                    // same for CallAgg
                                    this.walkExprNode(expr.arg)
                                    return
                                }
                                expr is CreateIndex -> {
                                    // Ignore variable references within CREATE INDEX.
                                    return
                                }
                            }
                            // For everything else, rely on the super AstWalker.
                            super.walkExprNode(expr)
                        }
                    }
                }.walk(result.node)

                if(remainingExpectations.any()) {
                    println("Unmet expectations:")
                    remainingExpectations.forEach {
                        println(it)
                    }

                    fail("${remainingExpectations.size} variable expectations were not met.\n" +
                         "The first was: ${remainingExpectations.first()}\n" +
                         "See standard output for a complete list")
                }
            }
        }
    }


    fun expectSubNode(expectedNode: ExprNode): (ResolveTestResult) -> Unit = {
        when (it) {
            is ResolveTestResult.Error -> fail("Expected value, not failure ${it.error}")
            is ResolveTestResult.Value -> {
                var found = false
                val visitor = object : AstVisitorBase() {
                    override fun visitExprNode(expr: ExprNode) {
                        if (expr == expectedNode) {
                            found = true
                        }
                    }
                }

                AstWalker(visitor).walk(it.node)
                assertTrue("Could not find $expectedNode in ${it.node}", found)
            }
        }
    }

    private fun metas(line: Long, column: Long, type: StaticType? = null): MetaContainer =
        metaContainerOf(SourceLocationMeta(line, column)) +
        (type?.let { metaContainerOf(StaticTypeMeta(it)) } ?: emptyMetaContainer)

    private fun StaticType.toMetas(): MetaContainer = metaContainerOf(StaticTypeMeta(this))

    private fun runSTRTest(
        tc: STRTestCase
    ) {
        val globalBindings = Bindings.ofMap(tc.globals)
        val rewriter = StaticTypeRewriter(ion, globalBindings, tc.constraints)

        // We always pass the query under test through all of the basic rewriters primarily because we need
        // FromSourceAliasRewriter to execute first but also to help ensure the queries we're testing
        // make sense when they're all run.
        val defaultRewriters = basicRewriters(ion)
        val originalExprNode = defaultRewriters.rewriteExprNode(parse(tc.originalSql))

        val rewrittenExprNode = try {
            rewriter.rewriteExprNode(originalExprNode)
        }
        catch (e: SemanticException) {
            tc.handler(ResolveTestResult.Error(tc, e))
            return
        }

        tc.handler(ResolveTestResult.Value(tc, rewrittenExprNode))
    }
}
