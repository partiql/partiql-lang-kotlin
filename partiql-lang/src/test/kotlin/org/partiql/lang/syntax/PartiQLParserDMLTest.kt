package org.partiql.lang.syntax

import com.amazon.ion.Decimal
import com.amazon.ionelement.api.ionDecimal
import com.amazon.ionelement.api.ionInt
import com.amazon.ionelement.api.ionString
import org.junit.Ignore
import org.junit.Test
import org.partiql.errors.ErrorCode
import org.partiql.errors.Property
import org.partiql.lang.ION
import org.partiql.lang.domains.id
import org.partiql.lang.util.getAntlrDisplayString
import org.partiql.parser.internal.antlr.PartiQLParser

/**
 * Tests parsing of DML using just the PIG parser.
 */
class PartiQLParserDMLTest : PartiQLParserTestBase() {
    override val targets: Array<ParserTarget> = arrayOf(ParserTarget.DEFAULT)

    // ****************************************
    // DML
    // ****************************************

    @Test
    fun fromInsertValuesDml() = assertExpression(
        "FROM x INSERT INTO foo VALUES (1, 2), (3, 4)",
        """
        (dml
            (operations
                (dml_op_list
                    (insert
                        (id foo (case_insensitive) (unqualified))
                        null
                        (bag
                            (list
                                (lit 1)
                                (lit 2))
                            (list
                                (lit 3)
                                (lit 4)))
                        null)))
            (from
                (scan
                    (id x (case_insensitive) (unqualified))
                    null
                    null
                    null)))
        """
    )

    @Test
    fun fromInsertValueAtDml() = assertExpression(
        "FROM x INSERT INTO foo VALUE 1 AT bar",
        """
          (dml
            (operations
              (dml_op_list
                (insert_value
                  (id foo (case_insensitive) (unqualified))
                  (lit 1)
                  (id bar (case_insensitive) (unqualified))
                  null
                )
              )
            )
            (from (scan (id x (case_insensitive) (unqualified)) null null null))
          )
        """
    )

    @Test
    @Ignore
    fun fromInsertValueAtReturningDml() = assertExpression(
        "FROM x INSERT INTO foo VALUE 1 AT bar RETURNING ALL OLD foo",
        """
          (dml
            (operations
              (dml_op_list
                (insert_value
                  (id foo (case_insensitive) (unqualified))
                  (lit 1)
                  (id bar (case_insensitive) (unqualified))
                )
              )
            )
            (from (scan (id x (case_insensitive) (unqualified)) null null null))
          )
        """
    )

    @Test
    fun fromInsertValueDml() = assertExpression(
        "FROM x INSERT INTO foo VALUE 1",
        """
          (dml
            (operations
              (dml_op_list
                (insert_value
                  (id foo (case_insensitive) (unqualified))
                  (lit 1)
                  null null)))
            (from (scan (id x (case_insensitive) (unqualified)) null null null))
          )
        """
    )

    @Test
    @Ignore
    fun fromInsertValueReturningDml() = assertExpression(
        "FROM x INSERT INTO foo VALUE 1 RETURNING ALL OLD foo",
        """
          (dml
            (dml_op_list
              (insert_value
                (id foo case_insensitive)
                (lit 1))
            )
            (from (id x case_insensitive))
          )
          """
    )

    @Test
    fun fromInsertQueryDml() = assertExpression(
        "FROM x INSERT INTO foo SELECT y FROM bar",
        """
        (dml
            (operations
                (dml_op_list
                    (insert
                        (id foo (case_insensitive) (unqualified))
                        null
                        (select
                            (project
                                (project_list
                                    (project_expr
                                        (id y (case_insensitive) (unqualified))
                                        null)))
                            (from
                                (scan
                                    (id bar (case_insensitive) (unqualified))
                                    null
                                    null
                                    null)))
                        null)))
            (from
                (scan
                    (id x (case_insensitive) (unqualified))
                    null
                    null
                    null)))
        """
    )

    @Test
    fun insertValueDml() = assertExpression(
        "INSERT INTO foo VALUE 1",
        """
          (dml
            (operations
              (dml_op_list
                (insert_value
                  (id foo (case_insensitive) (unqualified))
                  (lit 1)
                  null null
                )
              )
            )
          )
        """
    )

    @Test
    fun insertValueReturningDml() = assertExpression(
        "INSERT INTO foo VALUE 1 RETURNING MODIFIED OLD foo",
        """
        (dml 
            (operations 
                (dml_op_list (insert_value (id foo (case_insensitive) (unqualified)) (lit 1) null null)))
                (returning 
                    (returning_expr 
                        (returning_elem 
                            (modified_old) 
                            (returning_column (id foo (case_insensitive) (unqualified)))))))
        """
    )

    @Test
    fun insertValueReturningStarDml() = assertExpression(
        "INSERT INTO foo VALUE 1 RETURNING ALL OLD *",
        """
        (dml 
            (operations 
                (dml_op_list (insert_value (id foo (case_insensitive) (unqualified)) (lit 1) null null)))
                (returning 
                    (returning_expr 
                        (returning_elem 
                            (all_old) 
                            (returning_wildcard)))))
        """
    )

    @Test
    fun insertValuesDml() = assertExpression(
        "INSERT INTO foo VALUES (1, 2), (3, 4)",
        """
        (dml
            (operations
                (dml_op_list
                    (insert
                        (id foo (case_insensitive) (unqualified))
                        null
                        (bag
                            (list
                                (lit 1)
                                (lit 2))
                            (list
                                (lit 3)
                                (lit 4)))
                        null))))
        """
    )

    @Test
    fun insertValueAtDml() = assertExpression(
        "INSERT INTO foo VALUE 1 AT bar",
        """
          (dml
            (operations
              (dml_op_list
                (insert_value
                  (id foo (case_insensitive) (unqualified))
                  (lit 1)
                  (id bar (case_insensitive) (unqualified))
                  null
                )
              )
            )
          )
        """
    )

    @Test
    fun insertValueAtReturningDml() = assertExpression(
        "INSERT INTO foo VALUE 1 AT bar RETURNING ALL OLD foo",
        """
        (dml 
            (operations 
                (dml_op_list (insert_value (id foo (case_insensitive) (unqualified)) 
                (lit 1) (id bar (case_insensitive) (unqualified)) null)))
                (returning 
                    (returning_expr 
                        (returning_elem 
                            (all_old) 
                            (returning_column (id foo (case_insensitive) (unqualified)))))))
        """
    )

    @Test
    fun insertValueAtMultiReturningTwoColsDml() = assertExpression(
        "INSERT INTO foo VALUE 1 AT bar RETURNING ALL OLD a",
        """
        (dml 
            (operations 
                (dml_op_list (insert_value (id foo (case_insensitive) (unqualified)) 
                (lit 1) (id bar (case_insensitive) (unqualified)) null)))
                (returning 
                    (returning_expr 
                        (returning_elem 
                            (all_old) 
                            (returning_column (id a (case_insensitive) (unqualified)))))))
        """
    )

    @Test
    fun insertValueAtMultiReturningThreeColsDml() = assertExpression(
        "INSERT INTO foo VALUE 1 AT bar RETURNING MODIFIED OLD bar, MODIFIED NEW bar, ALL NEW *",
        """
            (dml 
                (operations 
                    (dml_op_list (insert_value (id foo (case_insensitive) (unqualified)) 
                    (lit 1) (id bar (case_insensitive) (unqualified)) null)))
                (returning 
                    (returning_expr 
                        (returning_elem 
                            (modified_old) 
                            (returning_column (id bar (case_insensitive) (unqualified)))) 
                        (returning_elem 
                            (modified_new) 
                            (returning_column (id bar (case_insensitive) (unqualified)))) 
                        (returning_elem 
                            (all_new) 
                            (returning_wildcard)))))
        """
    )

    @Test
    fun insertValueAtOnConflictDml() = assertExpression(
        "INSERT INTO foo VALUE 1 AT bar ON CONFLICT WHERE a DO NOTHING",
        """
          (dml
            (operations (dml_op_list
              (insert_value
                (id foo (case_insensitive) (unqualified))
                (lit 1)
                (id bar (case_insensitive) (unqualified))
                (on_conflict
                    (id a (case_insensitive) (unqualified))
                    (do_nothing)
                )
              )
            ))
          )
        """
    )

    @Test
    fun insertValueAtOnConflictReturningDml() = assertExpression(
        "INSERT INTO foo VALUE 1 AT bar ON CONFLICT WHERE a DO NOTHING RETURNING ALL OLD foo",
        """
      (dml
        (operations (dml_op_list
          (insert_value
            (id foo (case_insensitive) (unqualified))
            (lit 1)
            (id bar (case_insensitive) (unqualified))
            (on_conflict
                (id a (case_insensitive) (unqualified))
                (do_nothing)))))
        (returning 
            (returning_expr 
                (returning_elem 
                    (all_old) 
                    (returning_column (id foo (case_insensitive) (unqualified)))))))
        """
    )

    @Test
    fun insertValueOnConflictDml() = assertExpression(
        "INSERT INTO foo VALUE 1 ON CONFLICT WHERE bar DO NOTHING",
        """
          (dml
            (operations (dml_op_list
              (insert_value
                (id foo (case_insensitive) (unqualified))
                (lit 1)
                null
                (on_conflict
                    (id bar (case_insensitive) (unqualified))
                    (do_nothing)
                )
              )
            ))
          )
        """
    )

    @Test
    fun insertValueOnConflictExpr1Dml() = assertExpression(
        "INSERT INTO foo VALUE 1 ON CONFLICT WHERE hk=1 DO NOTHING",
        """
          (dml
            (operations (dml_op_list
              (insert_value
                (id foo (case_insensitive) (unqualified))
                (lit 1)
                null
                (on_conflict
                    (eq (id hk (case_insensitive) (unqualified)) (lit 1))
                    (do_nothing)
                )
              )
            ))
          )
        """
    )

    @Test
    fun insertValueOnConflictExpr2Dml() = assertExpression(
        "INSERT INTO foo VALUE 1 ON CONFLICT WHERE hk=1 and rk=1 DO NOTHING",
        """
          (dml
            (operations (dml_op_list
              (insert_value
                (id foo (case_insensitive) (unqualified))
                (lit 1)
                null
                (on_conflict
                    (and (eq (id hk (case_insensitive) (unqualified)) (lit 1)) (eq (id rk (case_insensitive) (unqualified)) (lit 1)))
                    (do_nothing)
                )
              )
            ))
          )
        """
    )

    @Test
    fun insertValueOnConflictExpr3Dml() = assertExpression(
        "INSERT INTO foo VALUE 1 ON CONFLICT WHERE hk BETWEEN 'a' and 'b' or rk = 'c' DO NOTHING",
        """
          (dml
            (operations (dml_op_list
              (insert_value
                (id foo (case_insensitive) (unqualified))
                (lit 1)
                null
                (on_conflict
                    (or (between (id hk (case_insensitive) (unqualified)) (lit "a") (lit "b")) (eq (id rk (case_insensitive) (unqualified)) (lit "c")))
                    (do_nothing)
                )
              )
            ))
          )
        """
    )

    @Test
    fun insertValueOnConflictExpr4Dml() = assertExpression(
        "INSERT INTO foo VALUE 1 ON CONFLICT WHERE not hk = 'a' DO NOTHING",
        """
          (dml
            (operations (dml_op_list
              (insert_value
                (id foo (case_insensitive) (unqualified))
                (lit 1)
                null
                (on_conflict
                    (not (eq (id hk (case_insensitive) (unqualified)) (lit "a")))
                    (do_nothing)
                )
              )
            ))
          )
        """
    )

    @Test
    fun insertValueOnConflictExpr5Dml() = assertExpression(
        "INSERT INTO foo VALUE 1 ON CONFLICT WHERE attribute_exists(hk) DO NOTHING",
        """
          (dml
            (operations (dml_op_list 
              (insert_value
                (id foo (case_insensitive) (unqualified))
                (lit 1)
                null
                (on_conflict
                    (call attribute_exists (id hk (case_insensitive) (unqualified)))
                    (do_nothing)
                )
              )
            ))
          )
        """
    )

    @Test
    fun insertValueOnConflictExpr6Dml() = assertExpression(
        "INSERT INTO foo VALUE 1 ON CONFLICT WHERE not attribute_exists(hk) DO NOTHING",
        """
          (dml
            (operations (dml_op_list
              (insert_value
                (id foo (case_insensitive) (unqualified))
                (lit 1)
                null
                (on_conflict
                    (not (call attribute_exists (id hk (case_insensitive) (unqualified))))
                    (do_nothing)
                )
              )
            ))
          )
        """
    )

    @Test
    fun insertQueryDml() = assertExpression(
        "INSERT INTO foo SELECT y FROM bar",
        """
        (dml
            (operations
                (dml_op_list
                    (insert
                        (id foo (case_insensitive) (unqualified))
                        null
                        (select
                            (project
                                (project_list
                                    (project_expr
                                        (id y (case_insensitive) (unqualified))
                                        null)))
                            (from
                                (scan
                                    (id bar (case_insensitive) (unqualified))
                                    null
                                    null
                                    null)))
                        null))))
        """
    )

    @Test
    fun insertWithOnConflictReplaceExcludedWithLiteralValue() = assertExpression(
        source = "INSERT into foo VALUES (1, 2), (3, 4) ON CONFLICT DO REPLACE EXCLUDED",
        expectedPigAst = """
        (dml
            (operations
                (dml_op_list
                    (insert
                        (id foo (case_insensitive) (unqualified))
                        null
                        (bag
                            (list
                                (lit 1)
                                (lit 2))
                            (list
                                (lit 3)
                                (lit 4)))
                        (do_replace
                            (excluded)
                            null
                        )))))
        """
    )

    @Test
    fun insertWithOnConflictReplaceExcludedWithLiteralValueAndCondition() = assertExpression(
        source = "INSERT into foo VALUES (1, 2), (3, 4) ON CONFLICT DO REPLACE EXCLUDED WHERE foo.id > 2",
        expectedPigAst = """
        (dml
            (operations
                (dml_op_list
                    (insert
                        (id foo (case_insensitive) (unqualified))
                        null
                        (bag
                            (list
                                (lit 1)
                                (lit 2))
                            (list
                                (lit 3)
                                (lit 4)))
                        (do_replace
                            (excluded)
                            (gt
                                (path
                                    (id foo (case_insensitive) (unqualified))
                                    (path_expr
                                        (lit "id")
                                        (case_insensitive)))
                                (lit 2)
                            )
                        )))))
        """
    )

    @Test
    fun insertWithOnConflictReplaceExcludedWithExcludedInCondition() = assertExpression(
        source = "INSERT into foo VALUES (1, 2), (3, 4) ON CONFLICT DO REPLACE EXCLUDED WHERE excluded.id > 2",
        expectedPigAst = """
        (dml
            (operations
                (dml_op_list
                    (insert
                        (id foo (case_insensitive) (unqualified))
                        null
                        (bag
                            (list
                                (lit 1)
                                (lit 2))
                            (list
                                (lit 3)
                                (lit 4)))
                        (do_replace
                            (excluded)
                            (gt
                                (path
                                    (id excluded (case_insensitive) (unqualified))
                                    (path_expr
                                        (lit "id")
                                        (case_insensitive)))
                                (lit 2)
                            )
                        )))))
        """
    )

    @Test
    fun insertWithOnConflictReplaceExcludedWithLiteralValueWithAlias() = assertExpression(
        source = "INSERT into foo AS f <<{'id': 1, 'name':'bob'}>> ON CONFLICT DO REPLACE EXCLUDED",
        expectedPigAst = """
            (dml
                (operations
                    (dml_op_list
                        (insert
                            (id foo (case_insensitive) (unqualified))
                            f
                            (bag
                                (struct
                                    (expr_pair
                                        (lit "id")
                                        (lit 1))
                                    (expr_pair
                                        (lit "name")
                                        (lit "bob"))))
                            (do_replace
                                (excluded)
                                null
                            )))))
        """
    )

    @Test
    fun insertWithOnConflictReplaceExcludedWithLiteralValueWithAliasAndCondition() = assertExpression(
        source = "INSERT into foo AS f <<{'id': 1, 'name':'bob'}>> ON CONFLICT DO REPLACE EXCLUDED WHERE f.id > 2",
        expectedPigAst = """
            (dml
                (operations
                    (dml_op_list
                        (insert
                            (id foo (case_insensitive) (unqualified))
                            f
                            (bag
                                (struct
                                    (expr_pair
                                        (lit "id")
                                        (lit 1))
                                    (expr_pair
                                        (lit "name")
                                        (lit "bob"))))
                            (do_replace
                                (excluded)
                            (gt
                                (path
                                    (id f (case_insensitive) (unqualified))
                                    (path_expr
                                        (lit "id")
                                        (case_insensitive)))
                                (lit 2)
                            )
                            )))))
        """
    )

    @Test
    fun insertWithOnConflictReplaceExcludedWithAliasAndExcludedInCondition() = assertExpression(
        source = "INSERT into foo AS f <<{'id': 1, 'name':'bob'}>> ON CONFLICT DO REPLACE EXCLUDED WHERE excluded.id > 2",
        expectedPigAst = """
            (dml
                (operations
                    (dml_op_list
                        (insert
                            (id foo (case_insensitive) (unqualified))
                            f
                            (bag
                                (struct
                                    (expr_pair
                                        (lit "id")
                                        (lit 1))
                                    (expr_pair
                                        (lit "name")
                                        (lit "bob"))))
                            (do_replace
                                (excluded)
                            (gt
                                (path
                                    (id excluded (case_insensitive) (unqualified))
                                    (path_expr
                                        (lit "id")
                                        (case_insensitive)))
                                (lit 2)
                            )
                            )))))
        """
    )

    @Test
    fun insertWithOnConflictReplaceExcludedWithSelect() = assertExpression(
        source = "INSERT into foo SELECT bar.id, bar.name FROM bar ON CONFLICT DO REPLACE EXCLUDED",
        expectedPigAst = """
            (dml
                (operations
                    (dml_op_list
                        (insert
                            (id foo (case_insensitive) (unqualified))
                            null
                            (select
                                (project
                                    (project_list
                                        (project_expr
                                            (path
                                                (id bar (case_insensitive) (unqualified))
                                                (path_expr
                                                    (lit "id")
                                                    (case_insensitive)))
                                            null)
                                        (project_expr
                                            (path
                                                (id bar (case_insensitive) (unqualified))
                                                (path_expr
                                                    (lit "name")
                                                    (case_insensitive)))
                                            null)))
                                (from
                                    (scan
                                        (id bar (case_insensitive) (unqualified))
                                        null
                                        null
                                        null)))
                            (do_replace
                                (excluded)
                                null
                            )))))
        """
    )

    @Test
    fun insertWithOnConflictUpdateExcludedWithLiteralValue() = assertExpression(
        source = "INSERT into foo VALUES (1, 2), (3, 4) ON CONFLICT DO UPDATE EXCLUDED",
        expectedPigAst = """
        (dml
            (operations
                (dml_op_list
                    (insert
                        (id foo (case_insensitive) (unqualified))
                        null
                        (bag
                            (list
                                (lit 1)
                                (lit 2))
                            (list
                                (lit 3)
                                (lit 4)))
                        (do_update
                            (excluded)
                            null
                        )))))
        """
    )

    @Test
    fun insertWithOnConflictUpdateExcludedWithLiteralValueAndCondition() = assertExpression(
        source = "INSERT into foo VALUES (1, 2), (3, 4) ON CONFLICT DO UPDATE EXCLUDED WHERE foo.id > 2",
        expectedPigAst = """
        (dml
            (operations
                (dml_op_list
                    (insert
                        (id foo (case_insensitive) (unqualified))
                        null
                        (bag
                            (list
                                (lit 1)
                                (lit 2))
                            (list
                                (lit 3)
                                (lit 4)))
                        (do_update
                            (excluded)
                            (gt
                                (path
                                    (id foo (case_insensitive) (unqualified))
                                    (path_expr
                                        (lit "id")
                                        (case_insensitive)))
                                (lit 2)
                            )
                        )))))
        """
    )

    @Test
    fun insertWithOnConflictUpdateExcludedWithExcludedCondition() = assertExpression(
        source = "INSERT into foo VALUES (1, 2), (3, 4) ON CONFLICT DO UPDATE EXCLUDED WHERE excluded.id > 2",
        expectedPigAst = """
        (dml
            (operations
                (dml_op_list
                    (insert
                        (id foo (case_insensitive) (unqualified))
                        null
                        (bag
                            (list
                                (lit 1)
                                (lit 2))
                            (list
                                (lit 3)
                                (lit 4)))
                        (do_update
                            (excluded)
                            (gt
                                (path
                                    (id excluded (case_insensitive) (unqualified))
                                    (path_expr
                                        (lit "id")
                                        (case_insensitive)))
                                (lit 2)
                            )
                        )))))
        """
    )

    @Test
    fun insertWithOnConflictUpdateExcludedWithLiteralValueWithAlias() = assertExpression(
        source = "INSERT into foo AS f <<{'id': 1, 'name':'bob'}>> ON CONFLICT DO UPDATE EXCLUDED",
        expectedPigAst = """
            (dml
                (operations
                    (dml_op_list
                        (insert
                            (id foo (case_insensitive) (unqualified))
                            f
                            (bag
                                (struct
                                    (expr_pair
                                        (lit "id")
                                        (lit 1))
                                    (expr_pair
                                        (lit "name")
                                        (lit "bob"))))
                            (do_update
                                (excluded)
                                null
                            )))))
        """
    )

    @Test
    fun insertWithOnConflictUpdateExcludedWithLiteralValueWithAliasAndCondition() = assertExpression(
        source = "INSERT into foo AS f <<{'id': 1, 'name':'bob'}>> ON CONFLICT DO UPDATE EXCLUDED WHERE f.id > 2",
        expectedPigAst = """
            (dml
                (operations
                    (dml_op_list
                        (insert
                            (id foo (case_insensitive) (unqualified))
                            f
                            (bag
                                (struct
                                    (expr_pair
                                        (lit "id")
                                        (lit 1))
                                    (expr_pair
                                        (lit "name")
                                        (lit "bob"))))
                            (do_update
                                (excluded)
                            (gt
                                (path
                                    (id f (case_insensitive) (unqualified))
                                    (path_expr
                                        (lit "id")
                                        (case_insensitive)))
                                (lit 2)
                            )
                            )))))
        """
    )

    @Test
    fun insertWithOnConflictUpdateExcludedWithAliasAndExcludedCondition() = assertExpression(
        source = "INSERT into foo AS f <<{'id': 1, 'name':'bob'}>> ON CONFLICT DO UPDATE EXCLUDED WHERE excluded.id > 2",
        expectedPigAst = """
            (dml
                (operations
                    (dml_op_list
                        (insert
                            (id foo (case_insensitive) (unqualified))
                            f
                            (bag
                                (struct
                                    (expr_pair
                                        (lit "id")
                                        (lit 1))
                                    (expr_pair
                                        (lit "name")
                                        (lit "bob"))))
                            (do_update
                                (excluded)
                            (gt
                                (path
                                    (id excluded (case_insensitive) (unqualified))
                                    (path_expr
                                        (lit "id")
                                        (case_insensitive)))
                                (lit 2)
                            )
                            )))))
        """
    )

    @Test
    fun insertWithOnConflictUpdateExcludedWithSelect() = assertExpression(
        source = "INSERT into foo SELECT bar.id, bar.name FROM bar ON CONFLICT DO UPDATE EXCLUDED",
        expectedPigAst = """
            (dml
                (operations
                    (dml_op_list
                        (insert
                            (id foo (case_insensitive) (unqualified))
                            null
                            (select
                                (project
                                    (project_list
                                        (project_expr
                                            (path
                                                (id bar (case_insensitive) (unqualified))
                                                (path_expr
                                                    (lit "id")
                                                    (case_insensitive)))
                                            null)
                                        (project_expr
                                            (path
                                                (id bar (case_insensitive) (unqualified))
                                                (path_expr
                                                    (lit "name")
                                                    (case_insensitive)))
                                            null)))
                                (from
                                    (scan
                                        (id bar (case_insensitive) (unqualified))
                                        null
                                        null
                                        null)))
                            (do_update
                                (excluded)
                                null
                            )))))
        """
    )

    @Test
    fun insertWithOnConflictDoNothing() = assertExpression(
        source = "INSERT into foo <<{'id': 1, 'name':'bob'}>> ON CONFLICT DO NOTHING",
        expectedPigAst = """
            (dml
                (operations
                    (dml_op_list
                        (insert
                            (id foo (case_insensitive) (unqualified))
                            null
                            (bag
                                (struct
                                    (expr_pair
                                        (lit "id")
                                        (lit 1))
                                    (expr_pair
                                        (lit "name")
                                        (lit "bob"))))
                            (do_nothing)))))
        """
    )

    @Test
    fun attemptConditionWithInsertDoNothing() = checkInputThrowingParserException(
        "INSERT into foo <<{'id': 1, 'name':'bob'}>> ON CONFLICT DO NOTHING WHERE foo.id > 2",
        ErrorCode.PARSE_UNEXPECTED_TOKEN,
        expectErrorContextValues = mapOf(
            Property.LINE_NUMBER to 1L,
            Property.COLUMN_NUMBER to 68L,
            Property.TOKEN_DESCRIPTION to PartiQLParser.WHERE.getAntlrDisplayString(),
            Property.TOKEN_VALUE to ION.newSymbol("WHERE")
        )
    )

    @Test
    fun mixAndMatchInsertWithLegacy() = checkInputThrowingParserException(
        "INSERT INTO foo <<{'id': 1, 'name':'bob'}>> ON CONFLICT WHERE TRUE DO NOTHING",
        ErrorCode.PARSE_UNEXPECTED_TOKEN,
        expectErrorContextValues = mapOf(
            Property.LINE_NUMBER to 1L,
            Property.COLUMN_NUMBER to 57L,
            Property.TOKEN_DESCRIPTION to PartiQLParser.WHERE.getAntlrDisplayString(),
            Property.TOKEN_VALUE to ION.newSymbol("WHERE")
        )
    )

    @Test
    fun mixAndMatchInsertLegacyWithCurrent() = checkInputThrowingParserException(
        "INSERT INTO foo VALUE {'id': 1, 'name':'bob'} ON CONFLICT DO UPDATE EXCLUDED",
        ErrorCode.PARSE_UNEXPECTED_TOKEN,
        expectErrorContextValues = mapOf(
            Property.LINE_NUMBER to 1L,
            Property.COLUMN_NUMBER to 59L,
            Property.TOKEN_DESCRIPTION to PartiQLParser.DO.getAntlrDisplayString(),
            Property.TOKEN_VALUE to ION.newSymbol("DO")
        )
    )

    @Test
    fun insertWithOnConflictDoNothingWithLiteralValueWithAlias() = assertExpression(
        source = "INSERT into foo AS f <<{'id': 1, 'name':'bob'}>> ON CONFLICT DO NOTHING",
        expectedPigAst = """
            (dml
                (operations
                    (dml_op_list
                        (insert
                            (id foo (case_insensitive) (unqualified))
                            f
                            (bag
                                (struct
                                    (expr_pair
                                        (lit "id")
                                        (lit 1))
                                    (expr_pair
                                        (lit "name")
                                        (lit "bob"))))
                            (do_nothing)))))
        """
    )

    @Test
    fun insertWithOnConflictDoNothingWithSelect() = assertExpression(
        source = "INSERT into foo SELECT bar.id, bar.name FROM bar ON CONFLICT DO NOTHING",
        expectedPigAst = """
            (dml
                (operations
                    (dml_op_list
                        (insert
                            (id foo (case_insensitive) (unqualified))
                            null
                            (select
                                (project
                                    (project_list
                                        (project_expr
                                            (path
                                                (id bar (case_insensitive) (unqualified))
                                                (path_expr
                                                    (lit "id")
                                                    (case_insensitive)))
                                            null)
                                        (project_expr
                                            (path
                                                (id bar (case_insensitive) (unqualified))
                                                (path_expr
                                                    (lit "name")
                                                    (case_insensitive)))
                                            null)))
                                (from
                                    (scan
                                        (id bar (case_insensitive) (unqualified))
                                        null
                                        null
                                        null)))
                            (do_nothing)))))
        """
    )

    @Test
    fun replaceCommand() = assertExpression(
        source = "REPLACE INTO foo <<{'id': 1, 'name':'bob'}>>",
        expectedPigAst = """
            (dml
                (operations
                    (dml_op_list
                        (insert
                            (id foo (case_insensitive) (unqualified))
                            null
                            (bag
                                (struct
                                    (expr_pair
                                        (lit "id")
                                        (lit 1))
                                    (expr_pair
                                        (lit "name")
                                        (lit "bob"))))
                            (do_replace
                                (excluded)
                                null
                            )))))
        """
    )

    @Test
    fun replaceCommandWithAsAlias() = assertExpression(
        source = "REPLACE INTO foo As f <<{'id': 1, 'name':'bob'}>>",
        expectedPigAst = """
            (dml
                (operations
                    (dml_op_list
                        (insert
                            (id foo (case_insensitive) (unqualified))
                            f
                            (bag
                                (struct
                                    (expr_pair
                                        (lit "id")
                                        (lit 1))
                                    (expr_pair
                                        (lit "name")
                                        (lit "bob"))))
                            (do_replace
                                (excluded)
                                null
                            )))))
        """
    )

    @Test
    fun upsertCommand() = assertExpression(
        source = "UPSERT INTO foo <<{'id': 1, 'name':'bob'}>>",
        expectedPigAst = """
            (dml
                (operations
                    (dml_op_list
                        (insert
                            (id foo (case_insensitive) (unqualified))
                            null
                            (bag
                                (struct
                                    (expr_pair
                                        (lit "id")
                                        (lit 1))
                                    (expr_pair
                                        (lit "name")
                                        (lit "bob"))))
                            (do_update
                                (excluded)
                                null
                            )))))
        """
    )

    @Test
    fun upsertCommandWithAsAlias() = assertExpression(
        source = "UPSERT INTO foo As f <<{'id': 1, 'name':'bob'}>>",
        expectedPigAst = """
            (dml
                (operations
                    (dml_op_list
                        (insert
                            (id foo (case_insensitive) (unqualified))
                            f
                            (bag
                                (struct
                                    (expr_pair
                                        (lit "id")
                                        (lit 1))
                                    (expr_pair
                                        (lit "name")
                                        (lit "bob"))))
                            (do_update
                                (excluded)
                            null
                            )))))
        """
    )

    @Test
    fun replaceCommandWithSelect() = assertExpression(
        source = "REPLACE INTO foo SELECT bar.id, bar.name FROM bar",
        expectedPigAst = """
            (dml
                (operations
                    (dml_op_list
                        (insert
                            (id foo (case_insensitive) (unqualified))
                            null
                            (select
                                (project
                                    (project_list
                                        (project_expr
                                            (path
                                                (id bar (case_insensitive) (unqualified))
                                                (path_expr
                                                    (lit "id")
                                                    (case_insensitive)))
                                            null)
                                        (project_expr
                                            (path
                                                (id bar (case_insensitive) (unqualified))
                                                (path_expr
                                                    (lit "name")
                                                    (case_insensitive)))
                                            null)))
                                (from
                                    (scan
                                        (id bar (case_insensitive) (unqualified))
                                        null
                                        null
                                        null)))
                            (do_replace
                                (excluded)
                                null
                            )))))
        """
    )

    @Test
    fun upsertCommandWithSelect() = assertExpression(
        source = "UPSERT INTO foo SELECT bar.id, bar.name FROM bar",
        expectedPigAst = """
        (dml
            (operations
                (dml_op_list
                    (insert
                        (id foo (case_insensitive) (unqualified))
                        null
                        (select
                            (project
                                (project_list
                                    (project_expr
                                        (path
                                            (id bar (case_insensitive) (unqualified))
                                            (path_expr
                                                (lit "id")
                                                (case_insensitive)))
                                        null)
                                    (project_expr
                                        (path
                                            (id bar (case_insensitive) (unqualified))
                                            (path_expr
                                                (lit "name")
                                                (case_insensitive)))
                                        null)))
                            (from
                                (scan
                                    (id bar (case_insensitive) (unqualified))
                                    null
                                    null
                                    null)))
                        (do_update
                            (excluded)
                            null
                        )
                        ))))
        """
    )

    @Test
    fun fromSetSingleDml() = assertExpression(
        "FROM x WHERE a = b SET k = 5",
        """
          (dml
            (operations
              (dml_op_list
                (set
                  (assignment
                    (id k (case_insensitive) (unqualified))
                    (lit 5)
                  )
                )
              )
            )
            (from (scan (id x (case_insensitive) (unqualified)) null null null))
            (where (eq (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))))
          )
        """
    )

    @Test
    fun fromSetSingleReturningDml() = assertExpression(
        "FROM x WHERE a = b SET k = 5 RETURNING ALL OLD x",
        """
        (dml (operations (dml_op_list (set (assignment (id k (case_insensitive) (unqualified)) (lit 5)))))
        (from (scan (id x (case_insensitive) (unqualified)) null null null))
        (where (eq (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))))
        (returning 
            (returning_expr 
                (returning_elem 
                    (all_old) 
                    (returning_column (id x (case_insensitive) (unqualified)))))))
        """
    )

    @Test
    fun fromSetSinglePathFieldDml() = assertExpression(
        "FROM x WHERE a = b SET k.m = 5",
        """
          (dml
            (operations
              (dml_op_list
                (set
                  (assignment
                    (path (id k (case_insensitive) (unqualified)) (path_expr (lit "m") (case_insensitive)))
                    (lit 5)
                  )
                )
              )
            )
            (from (scan (id x (case_insensitive) (unqualified)) null null null))
            (where (eq (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))))
          )
        """
    )

    @Test
    fun fromSetSinglePathStringIndexDml() = assertExpression(
        "FROM x WHERE a = b SET k['m'] = 5",
        """
          (dml
            (operations
              (dml_op_list
                (set
                  (assignment
                    (path (id k (case_insensitive) (unqualified)) (path_expr (lit "m") (case_sensitive)))
                    (lit 5)
                  )
                )
              )
            )
            (from (scan (id x (case_insensitive) (unqualified)) null null null))
            (where (eq (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))))
          )
        """
    )

    @Test
    fun fromSetSinglePathOrdinalDml() = assertExpression(
        "FROM x WHERE a = b SET k[3] = 5",
        """
          (dml
            (operations
              (dml_op_list
                (set
                  (assignment
                    (path (id k (case_insensitive) (unqualified)) (path_expr (lit 3) (case_sensitive)))
                    (lit 5)
                  )
                )
              )
            )
            (from (scan (id x (case_insensitive) (unqualified)) null null null))
            (where (eq (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))))
          )
        """
    )

    @Test
    fun fromSetMultiDml() = assertExpression(
        "FROM x WHERE a = b SET k = 5, m = 6",
        """
          (dml
            (operations
              (dml_op_list
                (set
                  (assignment
                    (id k (case_insensitive) (unqualified))
                    (lit 5)
                  )
                )
                (set
                  (assignment
                    (id m (case_insensitive) (unqualified))
                    (lit 6)
                  )
                )
              )
            )
            (from (scan (id x (case_insensitive) (unqualified)) null null null))
            (where (eq (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))))
          )
        """
    )

    @Test
    fun fromSetMultiReturningDml() = assertExpression(
        "FROM x WHERE a = b SET k = 5, m = 6 RETURNING ALL OLD x",
        """
        (dml (operations 
            (dml_op_list 
                (set (assignment (id k (case_insensitive) (unqualified)) (lit 5))) 
                (set (assignment (id m (case_insensitive) (unqualified)) (lit 6)))))
        (from (scan (id x (case_insensitive) (unqualified)) null null null))
        (where (eq (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))))
        (returning 
            (returning_expr 
                (returning_elem 
                    (all_old) 
                    (returning_column (id x (case_insensitive) (unqualified)))))))
        """
    )

    @Test
    fun fromComplexDml() = assertExpression(
        "FROM x WHERE a = b SET k = 5, m = 6 INSERT INTO c VALUE << 1 >> REMOVE a SET l = 3 REMOVE b",
        // Note that this query cannot be represented with the V0 AST.
        """ 
        (dml
            (operations
                (dml_op_list
                    (set
                        (assignment
                            (id k (case_insensitive) (unqualified))
                            (lit 5)))
                    (set
                        (assignment
                            (id m (case_insensitive) (unqualified))
                            (lit 6)))
                    (insert_value
                        (id c (case_insensitive) (unqualified))
                        (bag
                            (lit 1))
                        null null)
                    (remove
                        (id a (case_insensitive) (unqualified)))
                    (set
                        (assignment
                            (id l (case_insensitive) (unqualified))
                            (lit 3)))
                    (remove
                        (id b (case_insensitive) (unqualified)))))
            (from
                (scan
                    (id x (case_insensitive) (unqualified))
                    null
                    null
                    null))
            (where
                (eq
                    (id a (case_insensitive) (unqualified))
                    (id b (case_insensitive) (unqualified)))))
    """
    )

    @Test
    fun legacyUpdateComplexDml() = assertExpression(
        "UPDATE x SET k = 5, m = 6 INSERT INTO c VALUE << 1 >> REMOVE a SET l = 3 REMOVE b WHERE a = b",
        // Note that this query cannot be represented with the V0 AST.
        """
            (dml
                (operations
                    (dml_op_list
                        (set
                            (assignment
                                (id k (case_insensitive) (unqualified))
                                (lit 5)))
                        (set
                            (assignment
                                (id m (case_insensitive) (unqualified))
                                (lit 6)))
                        (insert_value
                            (id c (case_insensitive) (unqualified))
                            (bag
                                (lit 1))
                            null null)
                        (remove
                            (id a (case_insensitive) (unqualified)))
                        (set
                            (assignment
                                (id l (case_insensitive) (unqualified))
                                (lit 3)))
                        (remove
                            (id b (case_insensitive) (unqualified)))))
                (from
                    (scan
                        (id x (case_insensitive) (unqualified))
                        null
                        null
                        null))
                (where
                    (eq
                        (id a (case_insensitive) (unqualified))
                        (id b (case_insensitive) (unqualified)))))
        """
    )

    @Test
    fun legacyUpdateReturningComplexDml() = assertExpression(
        "UPDATE x SET k = 5, m = 6 INSERT INTO c VALUE << 1 >> REMOVE a SET l = 3 REMOVE b WHERE a = b RETURNING MODIFIED OLD a",
        """
        (dml
            (operations
                (dml_op_list
                    (set
                        (assignment
                            (id k (case_insensitive) (unqualified))
                            (lit 5)))
                    (set
                        (assignment
                            (id m (case_insensitive) (unqualified))
                            (lit 6)))
                    (insert_value
                        (id c (case_insensitive) (unqualified))
                        (bag
                            (lit 1))
                        null null)
                    (remove
                        (id a (case_insensitive) (unqualified)))
                    (set
                        (assignment
                            (id l (case_insensitive) (unqualified))
                            (lit 3)))
                    (remove
                        (id b (case_insensitive) (unqualified)))))
            (from
                (scan
                    (id x (case_insensitive) (unqualified))
                    null
                    null
                    null))
            (where
                (eq
                    (id a (case_insensitive) (unqualified))
                    (id b (case_insensitive) (unqualified))))
            (returning 
                (returning_expr 
                    (returning_elem 
                        (modified_old) 
                        (returning_column (id a (case_insensitive) (unqualified)))))))        
        """
    )

    @Test
    fun setSingleDml() = assertExpression(
        "SET k = 5",
        """
          (dml
            (operations
              (dml_op_list
                (set
                  (assignment
                    (id k (case_insensitive) (unqualified))
                    (lit 5)
                  )
                )
              )
            )
          )
        """
    )

    @Test
    fun setSingleDmlWithQuotedIdentifierAtHead() = assertExpression(
        "SET \"k\" = 5",
        """
          (dml
            (operations
              (dml_op_list
                (set
                  (assignment
                    (id k (case_sensitive) (unqualified))
                    (lit 5)
                  )
                )
              )
            )
          )
        """
    )

    @Test
    fun setMultiDml() = assertExpression(
        "SET k = 5, m = 6",
        """
          (dml
            (operations
              (dml_op_list 
                (set
                  (assignment
                    (id k (case_insensitive) (unqualified))
                    (lit 5)
                  )
                )
                (set
                  (assignment
                    (id m (case_insensitive) (unqualified))
                    (lit 6)
                  )
                )
              )
            )
          )
        """
    )

    @Test
    fun fromRemoveDml() = assertExpression(
        "FROM x WHERE a = b REMOVE y",
        """
          (dml
            (operations
              (dml_op_list
                (remove
                  (id y (case_insensitive) (unqualified))
                )
              )
            )
            (from (scan (id x (case_insensitive) (unqualified)) null null null))
            (where (eq (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))))
          )
        """
    )

    @Test
    fun fromRemoveReturningDml() = assertExpression(
        "FROM x WHERE a = b REMOVE y RETURNING MODIFIED NEW a",
        """
        (dml (operations 
            (dml_op_list (remove (id y (case_insensitive) (unqualified))))) 
            (from (scan (id x (case_insensitive) (unqualified)) null null null)) 
            (where (eq (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified)))) 
            (returning 
                (returning_expr 
                    (returning_elem 
                        (modified_new) 
                        (returning_column (id a (case_insensitive) (unqualified)))))))
        """
    )

    @Test
    fun fromMultipleRemoveDml() = assertExpression(
        "FROM x WHERE a = b REMOVE y REMOVE z",
        """
          (dml
            (operations
              (dml_op_list
                (remove
                  (id y (case_insensitive) (unqualified))
                )
                (remove
                  (id z (case_insensitive) (unqualified))
                )
              )
            )
            (from (scan (id x (case_insensitive) (unqualified)) null null null))
            (where (eq (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))))
          )
        """
    )

    @Test
    fun fromMultipleRemoveReturningDml() = assertExpression(
        "FROM x WHERE a = b REMOVE y REMOVE z RETURNING MODIFIED OLD a",
        """
        (dml
        (operations
          (dml_op_list
            (remove
              (id y (case_insensitive) (unqualified))
            )
            (remove
              (id z (case_insensitive) (unqualified))
            )
          )
        )
        (from (scan (id x (case_insensitive) (unqualified)) null null null))
        (where (eq (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))))
        (returning 
            (returning_expr 
                (returning_elem 
                    (modified_old) 
                    (returning_column (id a (case_insensitive) (unqualified)))))))
        """
    )

    @Test
    fun removeDml() = assertExpression(
        "REMOVE y",
        """
          (dml
            (operations
              (dml_op_list
                (remove
                  (id y (case_insensitive) (unqualified))
                )
              )
            )
          )
        """
    )

    @Test
    fun removeDmlPath() = assertExpression(
        "REMOVE a.b['c'][2]",
        """
          (dml
            (operations
              (dml_op_list
                (remove
                  (path
                    (id a (case_insensitive) (unqualified))
                    (path_expr (lit "b") (case_insensitive))
                    (path_expr (lit "c") (case_sensitive))
                    (path_expr (lit 2) (case_sensitive))
                  )
                )
              )
            )
          )
        """
    )

    @Test
    fun updateDml() = assertExpression(
        "UPDATE x AS y SET k = 5, m = 6 WHERE a = b",
        """
          (dml
            (operations
              (dml_op_list
                (set
                  (assignment
                    (id k (case_insensitive) (unqualified))
                    (lit 5)
                  )
                )
                (set
                  (assignment
                    (id m (case_insensitive) (unqualified))
                    (lit 6)
                  )
                )
              )
            )
            (from (scan (id x (case_insensitive) (unqualified)) y null null))
            (where (eq (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))))
          )
        """
    )

    @Test
    fun updateReturningDml() = assertExpression(
        "UPDATE x AS y SET k = 5, m = 6 WHERE a = b RETURNING MODIFIED OLD a",
        """
      (dml
        (operations
          (dml_op_list
            (set
              (assignment
                (id k (case_insensitive) (unqualified))
                (lit 5)
              )
            )
            (set
              (assignment
                (id m (case_insensitive) (unqualified))
                (lit 6)
              )
            )
          )
        )
        (from (scan (id x (case_insensitive) (unqualified)) y null null))
        (where (eq (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))))
        (returning 
            (returning_expr 
                (returning_elem 
                    (modified_old) 
                    (returning_column (id a (case_insensitive) (unqualified)))))))
        """
    )

    @Test
    fun updateWithInsert() = assertExpression(
        "UPDATE x AS y INSERT INTO k << 1 >> WHERE a = b",
        """
        (dml
            (operations
                (dml_op_list
                    (insert
                        (id k (case_insensitive) (unqualified))
                        null
                        (bag
                            (lit 1))
                        null)))
            (from
                (scan
                    (id x (case_insensitive) (unqualified))
                    y
                    null
                    null))
            (where
                (eq
                    (id a (case_insensitive) (unqualified))
                    (id b (case_insensitive) (unqualified)))))
        """
    )

    @Test
    fun updateWithInsertReturningDml() = assertExpression(
        "UPDATE x AS y INSERT INTO k << 1 >> WHERE a = b RETURNING MODIFIED OLD a",
        """
        (dml
            (operations
                (dml_op_list
                    (insert
                        (id k (case_insensitive) (unqualified))
                        null
                        (bag
                            (lit 1))
                        null)))
            (from
                (scan
                    (id x (case_insensitive) (unqualified))
                    y
                    null
                    null))
            (where
                (eq
                    (id a (case_insensitive) (unqualified))
                    (id b (case_insensitive) (unqualified))))
            (returning
                (returning_expr
                    (returning_elem
                        (modified_old)
                        (returning_column
                            (id a (case_insensitive) (unqualified)))))))
        """
    )

    @Test
    fun updateWithInsertValueAt() = assertExpression(
        "UPDATE x AS y INSERT INTO k VALUE 1 AT 'j' WHERE a = b",
        """
          (dml
            (operations
              (dml_op_list
                (insert_value
                  (id k (case_insensitive) (unqualified))
                  (lit 1)
                  (lit "j")
                  null
                )
              )
            )    
            (from
              (scan (id x (case_insensitive) (unqualified)) y null null))
            (where
              (eq
                (id a (case_insensitive) (unqualified))
                (id b (case_insensitive) (unqualified)))))
        """
    )

    @Test
    fun updateWithRemove() = assertExpression(
        "UPDATE x AS y REMOVE y.a WHERE a = b",
        """
          (dml  
            (operations
              (dml_op_list
                (remove
                  (path
                    (id y (case_insensitive) (unqualified))
                    (path_expr (lit "a") (case_insensitive))))))
            (from (scan (id x (case_insensitive) (unqualified)) y null null))
            (where (eq (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))))
          )
        """
    )

    @Test
    fun updateDmlWithImplicitAs() = assertExpression(
        "UPDATE zoo z SET z.kingdom = 'Fungi'",
        """
          (dml
            (operations
              (dml_op_list
                (set
                  (assignment
                    (path (id z (case_insensitive) (unqualified)) (path_expr (lit "kingdom") (case_insensitive)))
                    (lit "Fungi")))))
            (from
              (scan (id zoo (case_insensitive) (unqualified)) z null null)))
        """
    )

    @Test
    fun updateDmlWithAt() = assertExpression(
        "UPDATE zoo AT z_ord SET z.kingdom = 'Fungi'",
        """
          (dml
            (operations
              (dml_op_list
                (set
                  (assignment
                    (path (id z (case_insensitive) (unqualified)) (path_expr (lit "kingdom") (case_insensitive)))
                    (lit "Fungi")))))
            (from
              (scan (id zoo (case_insensitive) (unqualified)) null z_ord null)))
        """
    )

    @Test
    fun updateDmlWithBy() = assertExpression(
        "UPDATE zoo BY z_id SET z.kingdom = 'Fungi'",
        """
          (dml
            (operations
              (dml_op_list
                (set
                  (assignment
                    (path (id z (case_insensitive) (unqualified)) (path_expr (lit "kingdom") (case_insensitive)))
                    (lit "Fungi")))))
            (from
              (scan (id zoo (case_insensitive) (unqualified)) null null z_id)))
        """
    )

    @Test
    fun updateDmlWithAtAndBy() = assertExpression(
        "UPDATE zoo AT z_ord BY z_id SET z.kingdom = 'Fungi'",
        """
          (dml
            (operations
              (dml_op_list
                (set
                  (assignment
                    (path (id z (case_insensitive) (unqualified)) (path_expr (lit "kingdom") (case_insensitive)))
                    (lit "Fungi")))))
            (from
              (scan (id zoo (case_insensitive) (unqualified)) null z_ord z_id)))
        """
    )

    @Test
    fun updateWhereDml() = assertExpression(
        "UPDATE x SET k = 5, m = 6 WHERE a = b",
        """
          (dml
            (operations
              (dml_op_list
                (set
                  (assignment
                    (id k (case_insensitive) (unqualified))
                    (lit 5)
                  )
                )
                (set
                  (assignment
                    (id m (case_insensitive) (unqualified))
                    (lit 6)
                  )
                )
              )
            )
            (from (scan (id x (case_insensitive) (unqualified)) null null null))
            (where (eq (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))))
          )
        """
    )

    @Test
    fun updateWhereReturningDml() = assertExpression(
        "UPDATE x SET k = 5, m = 6 WHERE a = b RETURNING MODIFIED OLD a, MODIFIED OLD b",
        """(dml 
        (operations 
            (dml_op_list 
                (set (assignment (id k (case_insensitive) (unqualified)) (lit 5))) 
                (set (assignment (id m (case_insensitive) (unqualified)) (lit 6))))) 
        (from (scan (id x (case_insensitive) (unqualified)) null null null)) 
        (where (eq (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))))
        (returning 
            (returning_expr 
                (returning_elem 
                    (modified_old) 
                    (returning_column (id a (case_insensitive) (unqualified)))) 
                (returning_elem 
                    (modified_old) 
                    (returning_column (id b (case_insensitive) (unqualified)))))))
        """
    )

    @Test
    fun updateWhereReturningPathDml() = assertExpression(
        "UPDATE x SET k = 5, m = 6 WHERE a = b RETURNING MODIFIED OLD a.b",
        """(dml 
            (operations 
                (dml_op_list 
                    (set (assignment (id k (case_insensitive) (unqualified)) (lit 5))) 
                    (set (assignment (id m (case_insensitive) (unqualified)) (lit 6))))) 
            (from (scan (id x (case_insensitive) (unqualified)) null null null)) 
            (where (eq (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))))
            (returning 
                (returning_expr 
                    (returning_elem 
                        (modified_old) 
                        (returning_column 
                            (path (id a (case_insensitive) (unqualified)) 
                            (path_expr (lit "b") (case_insensitive))))))))
        """
    )

    @Test
    fun updateWhereReturningPathAsteriskDml() = assertExpression(
        "UPDATE x SET k = 5, m = 6 WHERE a = b RETURNING MODIFIED OLD '1234'.*",
        """(dml 
        (operations 
            (dml_op_list 
                (set (assignment (id k (case_insensitive) (unqualified)) (lit 5))) 
                (set (assignment (id m (case_insensitive) (unqualified)) (lit 6))))) 
        (from (scan (id x (case_insensitive) (unqualified)) null null null)) 
        (where (eq (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))))
        (returning 
            (returning_expr 
                (returning_elem 
                    (modified_old) 
                    (returning_column (path (lit "1234") (path_unpivot)))))))
        """
    )

    @Test
    fun updateMultipleSetsWhereDml() = assertExpression(
        "UPDATE x SET k = 5 SET m = 6 WHERE a = b",
        """
          (dml
            (operations
              (dml_op_list
                  (set
                    (assignment
                      (id k (case_insensitive) (unqualified))
                      (lit 5)
                    )
                  )
                  (set
                    (assignment
                      (id m (case_insensitive) (unqualified))
                      (lit 6)
                    )
                  )
               )
            )
            (from (scan (id x (case_insensitive) (unqualified)) null null null))
            (where (eq (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))))
          )
        """
    )

    @Test
    fun updateMultipleSetsWhereReturningDml() = assertExpression(
        "UPDATE x SET k = 5 SET m = 6 WHERE a = b RETURNING ALL OLD x.*",
        """
        (dml (operations (dml_op_list 
            (set (assignment (id k (case_insensitive) (unqualified)) (lit 5))) 
            (set (assignment (id m (case_insensitive) (unqualified)) (lit 6)))))
        (from (scan (id x (case_insensitive) (unqualified)) null null null))
        (where (eq (id a (case_insensitive) (unqualified)) (id b (case_insensitive) (unqualified))))
        (returning 
            (returning_expr 
                (returning_elem 
                    (all_old) 
                    (returning_column 
                        (path (id x (case_insensitive) (unqualified)) 
                        (path_unpivot)))))))
        """
    )

    @Test
    fun deleteDml() = assertExpression(
        "DELETE FROM y",
        """
          (dml
            (operations (dml_op_list (delete)))
            (from (scan (id y (case_insensitive) (unqualified)) null null null))
          )
        """
    )

    @Test
    fun deleteReturningDml() = assertExpression(
        "DELETE FROM y RETURNING MODIFIED NEW a",
        """
      (dml
        (operations (dml_op_list (delete)))
        (from (scan (id y (case_insensitive) (unqualified)) null null null))
        (returning 
            (returning_expr 
                (returning_elem 
                    (modified_new) 
                    (returning_column (id a (case_insensitive) (unqualified)))))))
        """
    )

    @Test
    fun deleteDmlAliased() = assertExpression(
        "DELETE FROM x AS y",
        """
          (dml
            (operations (dml_op_list (delete)))
            (from (scan (id x (case_insensitive) (unqualified)) y null null))
          )
        """
    )

    @Test
    fun canParseADeleteQueryWithAPositionClause() = assertExpression(
        "DELETE FROM x AT y",
        """
            (dml
              (operations ( dml_op_list (delete)))
              (from (scan (id x (case_insensitive) (unqualified)) null y null)))
        """
    )

    @Test
    fun canParseADeleteQueryWithAliasAndPositionClause() = assertExpression(
        "DELETE FROM x AS y AT z",
        """
            (dml
               (operations (dml_op_list (delete)))
               (from (scan (id x (case_insensitive) (unqualified)) y z null)))
        """
    )

    @Test
    fun canParseADeleteQueryWithPath() = assertExpression(
        "DELETE FROM x.n",
        """
            (dml
                (operations (dml_op_list (delete)))
                (from
                    (scan
                        (path (id x (case_insensitive) (unqualified)) (path_expr (lit "n") (case_insensitive)))
                        null 
                        null
                        null)))
        """
    )

    @Test
    fun canParseADeleteQueryWithNestedPath() = assertExpression(
        "DELETE FROM x.n.m",
        """
            (dml
                (operations (dml_op_list(delete)))
                (from
                    (scan 
                        (path
                            (id x (case_insensitive) (unqualified))
                            (path_expr (lit "n") (case_insensitive))
                            (path_expr (lit "m") (case_insensitive)))
                        null
                        null
                        null)))
        """
    )

    @Test
    fun canParseADeleteQueryWithNestedPathAndAlias() = assertExpression(
        "DELETE FROM x.n.m AS y",
        """
            (dml
                (operations (dml_op_list (delete)))
                (from
                    (scan
                        (path
                            (id x (case_insensitive) (unqualified))
                            (path_expr (lit "n") (case_insensitive))
                            (path_expr (lit "m") (case_insensitive)))
                        y
                        null    
                        null)))
        """
    )

    @Test
    fun canParseADeleteQueryWithNestedPathAndAliasAndPosition() = assertExpression(
        "DELETE FROM x.n.m AS y AT z",
        """
            (dml
                (operations (dml_op_list (delete)))
                (from
                    (scan 
                        (path
                            (id x (case_insensitive) (unqualified))
                            (path_expr (lit "n") (case_insensitive))
                            (path_expr (lit "m") (case_insensitive)))
                        y
                        z
                        null)))
        """
    )

    // ****************************************
    // EXEC clause parsing
    // ****************************************
    @Test
    fun execNoArgs() = assertExpression(
        "EXEC foo"
    ) {
        exec("foo", emptyList())
    }

    @Test
    fun execOneStringArg() = assertExpression(
        "EXEC foo 'bar'"
    ) {
        exec("foo", listOf(lit(ionString("bar"))))
    }

    @Test
    fun execOneIntArg() = assertExpression(
        "EXEC foo 1"
    ) {
        exec("foo", listOf(lit(ionInt(1))))
    }

    @Test
    fun execMultipleArg() = assertExpression(
        "EXEC foo 'bar0', `1d0`, 2, [3]"
    ) {
        exec(
            "foo",
            listOf(lit(ionString("bar0")), lit(ionDecimal(Decimal.valueOf(1))), lit(ionInt(2)), list(lit(ionInt(3))))
        )
    }

    @Test
    fun execWithMissing() = assertExpression(
        "EXEC foo MISSING"
    ) {
        exec("foo", listOf(missing()))
    }

    @Test
    fun execWithBag() = assertExpression(
        "EXEC foo <<1>>"
    ) {
        exec("foo", listOf(bag(lit(ionInt(1)))))
    }

    @Test
    fun execWithSelectQuery() = assertExpression(
        "EXEC foo SELECT baz FROM bar"
    ) {
        exec(
            "foo",
            listOf(
                select(
                    project = projectList(projectExpr(id("baz"))),
                    from = scan(id("bar"))
                )
            )
        )
    }
}
