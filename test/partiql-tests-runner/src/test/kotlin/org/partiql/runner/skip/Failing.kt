package org.partiql.runner.skip

import org.partiql_v0_14_8.lang.eval.CompileOptions
import org.partiql_v0_14_8.lang.eval.TypingMode

private val COERCE_EVAL_MODE_COMPILE_OPTIONS = CompileOptions.build { typingMode(TypingMode.PERMISSIVE) }
private val ERROR_EVAL_MODE_COMPILE_OPTIONS = CompileOptions.build { typingMode(TypingMode.LEGACY) }

/*
The fail lists defined in this file show how the current Kotlin implementation diverges from the PartiQL spec. Most of
the divergent behavior is due to `partiql-lang-kotlin` not having a STRICT typing mode/ERROR eval mode.  The
[LEGACY typing mode](https://github.com/partiql/partiql-lang-kotlin/blob/main/lang/src/org/partiql/lang/eval/CompileOptions.kt#L53-L62)
(which is closer to STRICT typing mode/ERROR eval mode but not a complete match) was used for testing the STRICT typing
mode/ERROR eval mode.

A lot of the other behavior differences is due to not supporting some syntax mentioned in the spec (like `COLL_*`
aggregation functions) and due to not supporting coercions.

The remaining divergent behavior causing certain conformance tests to fail are likely bugs. Tracking issue:
https://github.com/partiql/partiql-lang-kotlin/issues/804.
 */
val LANG_KOTLIN_EVAL_FAIL_LIST = setOf(
    // from the spec: no explicit CAST to string means the query is "treated as an array navigation with wrongly typed
    // data" and will return `MISSING`
    Pair("tuple navigation with array notation without explicit CAST to string", COERCE_EVAL_MODE_COMPILE_OPTIONS),
    // same as above, but since in error mode, should give an error
    Pair("tuple navigation with array notation without explicit CAST to string", ERROR_EVAL_MODE_COMPILE_OPTIONS),

    // for the following, partiql-lang-kotlin doesn't have a STRICT typing mode/ERROR eval mode. tested using
    // partiql-lang-kotlin's LEGACY typing mode, which has some semantic differences from STRICT typing mode/ERROR eval
    // mode.
    Pair("path on string", ERROR_EVAL_MODE_COMPILE_OPTIONS),
    Pair("tuple navigation missing attribute dot notation", ERROR_EVAL_MODE_COMPILE_OPTIONS),
    Pair("tuple navigation missing attribute array notation", ERROR_EVAL_MODE_COMPILE_OPTIONS),
    Pair("single source FROM with bag and AT clause", ERROR_EVAL_MODE_COMPILE_OPTIONS),
    Pair("single source FROM with scalar and AT clause", ERROR_EVAL_MODE_COMPILE_OPTIONS),
    Pair("single source FROM with tuple and AT clause", ERROR_EVAL_MODE_COMPILE_OPTIONS),
    Pair("single source FROM with absent value null and AT clause", ERROR_EVAL_MODE_COMPILE_OPTIONS),
    Pair("single source FROM with absent value missing and AT clause", ERROR_EVAL_MODE_COMPILE_OPTIONS),
    Pair("cast and operations with missing argument", ERROR_EVAL_MODE_COMPILE_OPTIONS),
    Pair("missing value in arithmetic expression", ERROR_EVAL_MODE_COMPILE_OPTIONS),
    Pair("equality of scalar missing", ERROR_EVAL_MODE_COMPILE_OPTIONS),
    Pair("arithmetic with null/missing", ERROR_EVAL_MODE_COMPILE_OPTIONS),

    // TODO: clarify behavior. spec (section 8) says it should return NULL based on 3-value logic
    Pair("missing and true", COERCE_EVAL_MODE_COMPILE_OPTIONS),

    // partiql-lang-kotlin doesn't currently implement subquery coercion. The inner SFW query returns a bag of two
    // elements that when coerced to a scalar should return MISSING in COERCE mode. As a result, `customerName` should
    // be missing from the first tuple.
    Pair("inner select evaluating to collection with more than one element", COERCE_EVAL_MODE_COMPILE_OPTIONS),

    // coll_* aggregate functions not supported in partiql-lang-kotlin -- results in parser error. coll_* functions
    // will be supported in https://github.com/partiql/partiql-lang-kotlin/issues/222
    Pair("coll_count without group by", COERCE_EVAL_MODE_COMPILE_OPTIONS),
    Pair("coll_count without group by", ERROR_EVAL_MODE_COMPILE_OPTIONS),
    Pair("coll_count with result of subquery", COERCE_EVAL_MODE_COMPILE_OPTIONS),
    Pair("coll_count with result of subquery", ERROR_EVAL_MODE_COMPILE_OPTIONS),

    // WITH keyword not supported resulting in parse error
    Pair("windowing simplified with grouping", COERCE_EVAL_MODE_COMPILE_OPTIONS),
    Pair("windowing simplified with grouping", ERROR_EVAL_MODE_COMPILE_OPTIONS),

    // partiql-lang-kotlin doesn't have STRICT typing mode/ERROR eval mode. LEGACY mode used which doesn't error when
    // RHS of `IN` expression is not a bag, list, or sexp
    Pair("notInPredicateSingleExpr", ERROR_EVAL_MODE_COMPILE_OPTIONS),

    // PartiQL Test Suite (PTS, https://github.com/partiql/partiql-lang-kotlin/tree/main/test/partiql-pts) tests:
    // partiql-lang-kotlin doesn't have STRICT typing mode/ERROR eval mode. LEGACY mode used which propagates NULL
    // rather than missing
    Pair("""char_length null and missing propagation{in:"missing",result:(success missing::null)}""", ERROR_EVAL_MODE_COMPILE_OPTIONS),
    Pair("""character_length null and missing propagation{in:"missing",result:(success missing::null)}""", ERROR_EVAL_MODE_COMPILE_OPTIONS),
    Pair("""trim null and missing propagation{sql:"trim(missing)"}""", ERROR_EVAL_MODE_COMPILE_OPTIONS),
    Pair("""trim null and missing propagation{sql:"trim(leading from missing)"}""", ERROR_EVAL_MODE_COMPILE_OPTIONS),
    Pair("""trim null and missing propagation{sql:"trim(trailing from missing)"}""", ERROR_EVAL_MODE_COMPILE_OPTIONS),
    Pair("""trim null and missing propagation{sql:"trim(both from missing)"}""", ERROR_EVAL_MODE_COMPILE_OPTIONS),
    Pair("""trim null and missing propagation{sql:"trim(leading '' from missing)"}""", ERROR_EVAL_MODE_COMPILE_OPTIONS),
    Pair("""trim null and missing propagation{sql:"trim(trailing '' from missing)"}""", ERROR_EVAL_MODE_COMPILE_OPTIONS),
    Pair("""trim null and missing propagation{sql:"trim(both '' from missing)"}""", ERROR_EVAL_MODE_COMPILE_OPTIONS),
    Pair("""trim null and missing propagation{sql:"trim(leading missing from '')"}""", ERROR_EVAL_MODE_COMPILE_OPTIONS),
    Pair("""trim null and missing propagation{sql:"trim(trailing missing from '')"}""", ERROR_EVAL_MODE_COMPILE_OPTIONS),
    Pair("""trim null and missing propagation{sql:"trim(both missing from '')"}""", ERROR_EVAL_MODE_COMPILE_OPTIONS),
    Pair("""trim null and missing propagation{sql:"trim(leading null from missing)"}""", ERROR_EVAL_MODE_COMPILE_OPTIONS),
    Pair("""trim null and missing propagation{sql:"trim(trailing null from missing)"}""", ERROR_EVAL_MODE_COMPILE_OPTIONS),
    Pair("""trim null and missing propagation{sql:"trim(both null from missing)"}""", ERROR_EVAL_MODE_COMPILE_OPTIONS),
    Pair("""trim null and missing propagation{sql:"trim(leading missing from null)"}""", ERROR_EVAL_MODE_COMPILE_OPTIONS),
    Pair("""trim null and missing propagation{sql:"trim(trailing missing from null)"}""", ERROR_EVAL_MODE_COMPILE_OPTIONS),
    Pair("""trim null and missing propagation{sql:"trim(both missing from null)"}""", ERROR_EVAL_MODE_COMPILE_OPTIONS),
    Pair("""trim null and missing propagation{sql:"trim(leading missing from missing)"}""", ERROR_EVAL_MODE_COMPILE_OPTIONS),
    Pair("""trim null and missing propagation{sql:"trim(trailing missing from missing)"}""", ERROR_EVAL_MODE_COMPILE_OPTIONS),
    Pair("""trim null and missing propagation{sql:"trim(both missing from missing)"}""", ERROR_EVAL_MODE_COMPILE_OPTIONS),
    Pair("""substring null and missing propagation 2 arguments{target:"missing",start_pos:"1"}""", ERROR_EVAL_MODE_COMPILE_OPTIONS),
    Pair("""substring null and missing propagation 2 arguments{target:"''",start_pos:"missing"}""", ERROR_EVAL_MODE_COMPILE_OPTIONS),
    Pair("""substring null and missing propagation 2 arguments{target:"missing",start_pos:"missing"}""", ERROR_EVAL_MODE_COMPILE_OPTIONS),
    Pair("""substring null and missing propagation 2 arguments{target:"null",start_pos:"missing"}""", ERROR_EVAL_MODE_COMPILE_OPTIONS),
    Pair("""substring null and missing propagation 2 arguments{target:"missing",start_pos:"null"}""", ERROR_EVAL_MODE_COMPILE_OPTIONS),
    Pair("""substring null and missing propagation 3 arguments{target:"null",start_pos:"1",quantity:"missing"}""", ERROR_EVAL_MODE_COMPILE_OPTIONS),
    Pair("""substring null and missing propagation 3 arguments{target:"null",start_pos:"null",quantity:"missing"}""", ERROR_EVAL_MODE_COMPILE_OPTIONS),
    Pair("""substring null and missing propagation 3 arguments{target:"null",start_pos:"missing",quantity:"1"}""", ERROR_EVAL_MODE_COMPILE_OPTIONS),
    Pair("""substring null and missing propagation 3 arguments{target:"null",start_pos:"missing",quantity:"null"}""", ERROR_EVAL_MODE_COMPILE_OPTIONS),
    Pair("""substring null and missing propagation 3 arguments{target:"null",start_pos:"missing",quantity:"missing"}""", ERROR_EVAL_MODE_COMPILE_OPTIONS),
    Pair("""substring null and missing propagation 3 arguments{target:"missing",start_pos:"1",quantity:"1"}""", ERROR_EVAL_MODE_COMPILE_OPTIONS),
    Pair("""substring null and missing propagation 3 arguments{target:"missing",start_pos:"1",quantity:"null"}""", ERROR_EVAL_MODE_COMPILE_OPTIONS),
    Pair("""substring null and missing propagation 3 arguments{target:"missing",start_pos:"1",quantity:"missing"}""", ERROR_EVAL_MODE_COMPILE_OPTIONS),
    Pair("""substring null and missing propagation 3 arguments{target:"missing",start_pos:"null",quantity:"1"}""", ERROR_EVAL_MODE_COMPILE_OPTIONS),
    Pair("""substring null and missing propagation 3 arguments{target:"missing",start_pos:"null",quantity:"null"}""", ERROR_EVAL_MODE_COMPILE_OPTIONS),
    Pair("""substring null and missing propagation 3 arguments{target:"missing",start_pos:"null",quantity:"missing"}""", ERROR_EVAL_MODE_COMPILE_OPTIONS),
    Pair("""substring null and missing propagation 3 arguments{target:"missing",start_pos:"missing",quantity:"1"}""", ERROR_EVAL_MODE_COMPILE_OPTIONS),
    Pair("""substring null and missing propagation 3 arguments{target:"missing",start_pos:"missing",quantity:"null"}""", ERROR_EVAL_MODE_COMPILE_OPTIONS),
    Pair("""substring null and missing propagation 3 arguments{target:"missing",start_pos:"missing",quantity:"missing"}""", ERROR_EVAL_MODE_COMPILE_OPTIONS),
    Pair("""substring null and missing propagation 3 arguments{target:"''",start_pos:"1",quantity:"missing"}""", ERROR_EVAL_MODE_COMPILE_OPTIONS),
    Pair("""substring null and missing propagation 3 arguments{target:"''",start_pos:"null",quantity:"missing"}""", ERROR_EVAL_MODE_COMPILE_OPTIONS),
    Pair("""substring null and missing propagation 3 arguments{target:"''",start_pos:"missing",quantity:"1"}""", ERROR_EVAL_MODE_COMPILE_OPTIONS),
    Pair("""substring null and missing propagation 3 arguments{target:"''",start_pos:"missing",quantity:"null"}""", ERROR_EVAL_MODE_COMPILE_OPTIONS),
    Pair("""substring null and missing propagation 3 arguments{target:"''",start_pos:"missing",quantity:"missing"}""", ERROR_EVAL_MODE_COMPILE_OPTIONS),
    Pair("""upper null and missing propagation{param:"missing"}""", ERROR_EVAL_MODE_COMPILE_OPTIONS),
    Pair("""lower null and missing propagation{param:"missing"}""", ERROR_EVAL_MODE_COMPILE_OPTIONS),
    Pair("""extract null and missing propagation{time_part:"year",timestamp:"missing"}""", ERROR_EVAL_MODE_COMPILE_OPTIONS),
    Pair("""|| valid cases{lparam:"null",rparam:"missing",result:missing::null}""", ERROR_EVAL_MODE_COMPILE_OPTIONS),
    Pair("""|| valid cases{lparam:"missing",rparam:"null",result:missing::null}""", ERROR_EVAL_MODE_COMPILE_OPTIONS),
    Pair("""|| valid cases{lparam:"missing",rparam:"'b'",result:missing::null}""", ERROR_EVAL_MODE_COMPILE_OPTIONS),
    Pair("""|| valid cases{lparam:"'a'",rparam:"missing",result:missing::null}""", ERROR_EVAL_MODE_COMPILE_OPTIONS),
    Pair("""|| valid cases{lparam:"missing",rparam:"missing",result:missing::null}""", ERROR_EVAL_MODE_COMPILE_OPTIONS),
    Pair("""concatenation with null values{left:"MISSING",right:"MISSING"}""", ERROR_EVAL_MODE_COMPILE_OPTIONS),
    Pair("""concatenation with null values{left:"''",right:"MISSING"}""", ERROR_EVAL_MODE_COMPILE_OPTIONS),
    Pair("""concatenation with null values{left:"MISSING",right:"''"}""", ERROR_EVAL_MODE_COMPILE_OPTIONS),
    Pair("""concatenation with null values{left:"'a'",right:"MISSING"}""", ERROR_EVAL_MODE_COMPILE_OPTIONS),
    Pair("""concatenation with null values{left:"MISSING",right:"'b'"}""", ERROR_EVAL_MODE_COMPILE_OPTIONS),
    // similar to above partiql-lang-kotlin doesn't have an STRICT typing mode/ERROR eval mode; its LEGACY mode
    // propagates NULL rather than MISSING
    Pair("""null comparison{sql:"MISSING = NULL",result:missing::null}""", ERROR_EVAL_MODE_COMPILE_OPTIONS),
    Pair("""null comparison{sql:"NULL = MISSING",result:missing::null}""", ERROR_EVAL_MODE_COMPILE_OPTIONS),
    Pair("""null comparison{sql:"`null.null` = MISSING",result:missing::null}""", ERROR_EVAL_MODE_COMPILE_OPTIONS),
    Pair("""null comparison{sql:"`null.bool` = MISSING",result:missing::null}""", ERROR_EVAL_MODE_COMPILE_OPTIONS),
    Pair("""null comparison{sql:"`null.int` = MISSING",result:missing::null}""", ERROR_EVAL_MODE_COMPILE_OPTIONS),
    Pair("""null comparison{sql:"`null.decimal` = MISSING",result:missing::null}""", ERROR_EVAL_MODE_COMPILE_OPTIONS),
    Pair("""null comparison{sql:"`null.string` = MISSING",result:missing::null}""", ERROR_EVAL_MODE_COMPILE_OPTIONS),
    Pair("""null comparison{sql:"`null.symbol` = MISSING",result:missing::null}""", ERROR_EVAL_MODE_COMPILE_OPTIONS),
    Pair("""null comparison{sql:"`null.clob` = MISSING",result:missing::null}""", ERROR_EVAL_MODE_COMPILE_OPTIONS),
    Pair("""null comparison{sql:"`null.blob` = MISSING",result:missing::null}""", ERROR_EVAL_MODE_COMPILE_OPTIONS),
    Pair("""null comparison{sql:"`null.list` = MISSING",result:missing::null}""", ERROR_EVAL_MODE_COMPILE_OPTIONS),
    Pair("""null comparison{sql:"`null.struct` = MISSING",result:missing::null}""", ERROR_EVAL_MODE_COMPILE_OPTIONS),
    Pair("""null comparison{sql:"`null.sexp` = MISSING",result:missing::null}""", ERROR_EVAL_MODE_COMPILE_OPTIONS),

    // `partiql-lang-kotlin` does not implement STRICT/ERROR mode. LEGACY typing mode outputs `NULL` rather than `MISSING`
    Pair("""MISSING LIKE 'some pattern'""", ERROR_EVAL_MODE_COMPILE_OPTIONS),
    Pair("""'some value' LIKE MISSING""", ERROR_EVAL_MODE_COMPILE_OPTIONS),
    Pair("""MISSING LIKE MISSING""", ERROR_EVAL_MODE_COMPILE_OPTIONS),
    Pair("""NULL LIKE MISSING""", ERROR_EVAL_MODE_COMPILE_OPTIONS),
    Pair("""MISSING LIKE NULL""", ERROR_EVAL_MODE_COMPILE_OPTIONS),
    Pair("""MISSING LIKE 'some pattern' ESCAPE '/'""", ERROR_EVAL_MODE_COMPILE_OPTIONS),
    Pair("""'some value' LIKE MISSING ESCAPE '/'""", ERROR_EVAL_MODE_COMPILE_OPTIONS),
    Pair("""'some value' LIKE 'some pattern' ESCAPE MISSING""", ERROR_EVAL_MODE_COMPILE_OPTIONS),
    Pair("""NULL LIKE 'some pattern' ESCAPE MISSING""", ERROR_EVAL_MODE_COMPILE_OPTIONS),
    Pair("""'some value' LIKE NULL ESCAPE MISSING""", ERROR_EVAL_MODE_COMPILE_OPTIONS),
    // Bad types for arguments should result in `MISSING` when run in PERMISSIVE/COERCE mode
    Pair("""LIKE bad value type""", COERCE_EVAL_MODE_COMPILE_OPTIONS),
    Pair("""LIKE bad pattern type""", COERCE_EVAL_MODE_COMPILE_OPTIONS),
    Pair("""LIKE bad escape type""", COERCE_EVAL_MODE_COMPILE_OPTIONS),
    // `NULLIF(MISSING, MISSING)` should output `MISSING` rather than `NULL` https://github.com/partiql/partiql-lang-kotlin/issues/973
    Pair("""nullif valid cases{first:"missing",second:"missing",result:missing}""", COERCE_EVAL_MODE_COMPILE_OPTIONS),
    Pair("""nullif valid cases{first:"missing",second:"missing",result:missing}""", ERROR_EVAL_MODE_COMPILE_OPTIONS),
    // `POSITION` not yet implemented
    Pair("""POSITION empty string in string""", COERCE_EVAL_MODE_COMPILE_OPTIONS),
    Pair("""POSITION empty string in string""", ERROR_EVAL_MODE_COMPILE_OPTIONS),
    Pair("""POSITION string at start""", COERCE_EVAL_MODE_COMPILE_OPTIONS),
    Pair("""POSITION string at start""", ERROR_EVAL_MODE_COMPILE_OPTIONS),
    Pair("""POSITION string in middle""", COERCE_EVAL_MODE_COMPILE_OPTIONS),
    Pair("""POSITION string in middle""", ERROR_EVAL_MODE_COMPILE_OPTIONS),
    Pair("""POSITION string at end""", COERCE_EVAL_MODE_COMPILE_OPTIONS),
    Pair("""POSITION string at end""", ERROR_EVAL_MODE_COMPILE_OPTIONS),
    Pair("""POSITION string not in string""", COERCE_EVAL_MODE_COMPILE_OPTIONS),
    Pair("""POSITION string not in string""", ERROR_EVAL_MODE_COMPILE_OPTIONS),
    Pair("""POSITION NULL in string""", COERCE_EVAL_MODE_COMPILE_OPTIONS),
    Pair("""POSITION NULL in string""", ERROR_EVAL_MODE_COMPILE_OPTIONS),
    Pair("""POSITION MISSING in string""", COERCE_EVAL_MODE_COMPILE_OPTIONS),
    Pair("""POSITION MISSING in string""", ERROR_EVAL_MODE_COMPILE_OPTIONS),
    Pair("""POSITION string in NULL""", COERCE_EVAL_MODE_COMPILE_OPTIONS),
    Pair("""POSITION string in NULL""", ERROR_EVAL_MODE_COMPILE_OPTIONS),
    Pair("""POSITION string in MISSING""", COERCE_EVAL_MODE_COMPILE_OPTIONS),
    Pair("""POSITION string in MISSING""", ERROR_EVAL_MODE_COMPILE_OPTIONS),
    Pair("""POSITION NULL in MISSING""", COERCE_EVAL_MODE_COMPILE_OPTIONS),
    Pair("""POSITION NULL in MISSING""", ERROR_EVAL_MODE_COMPILE_OPTIONS),
    Pair("""POSITION MISSING in NULL""", COERCE_EVAL_MODE_COMPILE_OPTIONS),
    Pair("""POSITION MISSING in NULL""", ERROR_EVAL_MODE_COMPILE_OPTIONS),
    Pair("""POSITION NULL in NULL""", COERCE_EVAL_MODE_COMPILE_OPTIONS),
    Pair("""POSITION NULL in NULL""", ERROR_EVAL_MODE_COMPILE_OPTIONS),
    Pair("""POSITION MISSING in MISSING""", COERCE_EVAL_MODE_COMPILE_OPTIONS),
    Pair("""POSITION MISSING in MISSING""", ERROR_EVAL_MODE_COMPILE_OPTIONS),
    Pair("""POSITION invalid type in string""", COERCE_EVAL_MODE_COMPILE_OPTIONS),
    Pair("""POSITION string in invalid type""", COERCE_EVAL_MODE_COMPILE_OPTIONS),
    // `BIT_LENGTH` not yet implemented
    Pair("""BIT_LENGTH empty string""", COERCE_EVAL_MODE_COMPILE_OPTIONS),
    Pair("""BIT_LENGTH empty string""", ERROR_EVAL_MODE_COMPILE_OPTIONS),
    Pair("""BIT_LENGTH string""", COERCE_EVAL_MODE_COMPILE_OPTIONS),
    Pair("""BIT_LENGTH string""", ERROR_EVAL_MODE_COMPILE_OPTIONS),
    Pair("""BIT_LENGTH NULL""", COERCE_EVAL_MODE_COMPILE_OPTIONS),
    Pair("""BIT_LENGTH NULL""", ERROR_EVAL_MODE_COMPILE_OPTIONS),
    Pair("""BIT_LENGTH MISSING""", COERCE_EVAL_MODE_COMPILE_OPTIONS),
    Pair("""BIT_LENGTH MISSING""", ERROR_EVAL_MODE_COMPILE_OPTIONS),
    Pair("""BIT_LENGTH invalid type""", COERCE_EVAL_MODE_COMPILE_OPTIONS),
    Pair("""BIT_LENGTH special character""", COERCE_EVAL_MODE_COMPILE_OPTIONS),
    Pair("""BIT_LENGTH special character""", ERROR_EVAL_MODE_COMPILE_OPTIONS),
    // `OCTET_LENGTH` not yet implemented
    Pair("""OCTET_LENGTH empty string""", COERCE_EVAL_MODE_COMPILE_OPTIONS),
    Pair("""OCTET_LENGTH empty string""", ERROR_EVAL_MODE_COMPILE_OPTIONS),
    Pair("""OCTET_LENGTH string""", COERCE_EVAL_MODE_COMPILE_OPTIONS),
    Pair("""OCTET_LENGTH string""", ERROR_EVAL_MODE_COMPILE_OPTIONS),
    Pair("""OCTET_LENGTH NULL""", COERCE_EVAL_MODE_COMPILE_OPTIONS),
    Pair("""OCTET_LENGTH NULL""", ERROR_EVAL_MODE_COMPILE_OPTIONS),
    Pair("""OCTET_LENGTH MISSING""", COERCE_EVAL_MODE_COMPILE_OPTIONS),
    Pair("""OCTET_LENGTH MISSING""", ERROR_EVAL_MODE_COMPILE_OPTIONS),
    Pair("""OCTET_LENGTH invalid type""", COERCE_EVAL_MODE_COMPILE_OPTIONS),
    Pair("""OCTET_LENGTH special character""", COERCE_EVAL_MODE_COMPILE_OPTIONS),
    Pair("""OCTET_LENGTH special character""", ERROR_EVAL_MODE_COMPILE_OPTIONS),
)

val LANG_KOTLIN_EVAL_EQUIV_FAIL_LIST = setOf(
    // partiql-lang-kotlin gives a parser error for tuple path navigation in which the path expression is a string
    // literal
    // e.g. { 'a': 1, 'b': 2}.'a' -> 1 (see section 4 of spec)
    Pair("equiv tuple path navigation with array notation", COERCE_EVAL_MODE_COMPILE_OPTIONS),
    Pair("equiv tuple path navigation with array notation", ERROR_EVAL_MODE_COMPILE_OPTIONS),

    // partiql-lang-kotlin doesn't support a STRICT typing mode/ERROR eval mode.
    Pair("equiv attribute value pair unpivot non-missing", ERROR_EVAL_MODE_COMPILE_OPTIONS),
    Pair("equiv attribute value pair unpivot missing", ERROR_EVAL_MODE_COMPILE_OPTIONS),

    // partiql-lang-kotlin doesn't support `LATERAL` keyword which results in a parser error
    Pair("equiv of comma, cross join, and join", COERCE_EVAL_MODE_COMPILE_OPTIONS),
    Pair("equiv of comma, cross join, and join", ERROR_EVAL_MODE_COMPILE_OPTIONS),

    // partiql-lang-kotlin doesn't support `TUPLEUNION` function which results in an evaluation error
    Pair("equiv tupleunion with select list", COERCE_EVAL_MODE_COMPILE_OPTIONS),
    Pair("equiv tupleunion with select list", ERROR_EVAL_MODE_COMPILE_OPTIONS),

    // partiql-lang-kotlin doesn't support coercion of subqueries which results in different outputs
    Pair("equiv coercion of a SELECT subquery into a scalar", COERCE_EVAL_MODE_COMPILE_OPTIONS),
    Pair("equiv coercion of a SELECT subquery into a scalar", ERROR_EVAL_MODE_COMPILE_OPTIONS),
    Pair("equiv coercion of a SELECT subquery into an array", COERCE_EVAL_MODE_COMPILE_OPTIONS),
    Pair("equiv coercion of a SELECT subquery into an array", ERROR_EVAL_MODE_COMPILE_OPTIONS),
    Pair("equiv coercions with explicit literals", COERCE_EVAL_MODE_COMPILE_OPTIONS),
    Pair("equiv coercions with explicit literals", ERROR_EVAL_MODE_COMPILE_OPTIONS),

    // partiql-lang-kotlin doesn't support `GROUP ALL` and `COLL_*` aggregate functions. Currently, results in a parser
    // error
    Pair("equiv group_all", COERCE_EVAL_MODE_COMPILE_OPTIONS),
    Pair("equiv group_all", ERROR_EVAL_MODE_COMPILE_OPTIONS),

    // partiql-lang-kotlin doesn't support `COLL_*` aggregate functions. Currently, results in an evaluation error
    Pair("equiv group by with aggregates", COERCE_EVAL_MODE_COMPILE_OPTIONS),
    Pair("equiv group by with aggregates", ERROR_EVAL_MODE_COMPILE_OPTIONS),

    // partiql-lang-kotlin doesn't support using aliases created in select list in `GROUP BY` (and `ORDER BY`). GH
    // issue to track:
    // https://github.com/partiql/partiql-lang-kotlin/issues/571
    Pair("equiv aliases from select clause", COERCE_EVAL_MODE_COMPILE_OPTIONS),
    Pair("equiv aliases from select clause", ERROR_EVAL_MODE_COMPILE_OPTIONS),
)
