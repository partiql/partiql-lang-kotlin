parser grammar PartiQLParser;

options {
    tokenVocab=PartiQLTokens;
    caseInsensitive = true;
}

/**
 *
 * TOP LEVEL
 *
 */

statements
    : statement EOF
    | statement COLON_SEMI (statement COLON_SEMI)* EOF
    ;

statement
    : dql           # QueryDql
    | dml           # QueryDml
    | ddl           # QueryDdl
    | execCommand   # QueryExec  // TODO delete in `v1` release
    | EXPLAIN (PAREN_LEFT explainOption (COMMA explainOption)* PAREN_RIGHT)? statement # Explain
    ;

/**
 *
 * COMMON STRUCTURES
 *
 */

explainOption
    : param=IDENTIFIER value=IDENTIFIER;

asIdent
    : AS symbolPrimitive;

atIdent
    : AT symbolPrimitive;

byIdent
    : BY symbolPrimitive;

symbolPrimitive
    : IDENTIFIER            # IdentifierUnquoted
    | IDENTIFIER_QUOTED     # IdentifierQuoted
    | nonReserved           # IdentifierUnquoted
    ;

/**
 *
 * DATA QUERY LANGUAGE (DQL)
 *
 */

dql
    : expr;

/**
 *
 * EXECUTE
 *
 */

// FIXME #002: This is a slight deviation from SqlParser, as the old parser allows ANY token after EXEC. Realistically,
//  we probably need to determine the formal rule for this. I'm assuming we shouldn't allow any token, but I've
//  left it as an expression (which allows strings). See https://github.com/partiql/partiql-lang-kotlin/issues/707
execCommand
    : EXEC name=expr ( args+=expr ( COMMA args+=expr )* )?;

/**
 *
 * DATA DEFINITION LANGUAGE (DDL)
 * Experimental, towards #36 https://github.com/partiql/partiql-docs/issues/36
 * Currently, this is a small subset of SQL DDL that is likely to make sense for PartiQL as well.
 */

// <qualified name> ::= [ <schema name> <period> ] <qualified identifier>
qualifiedName : (qualifier+=symbolPrimitive PERIOD)* name=symbolPrimitive;

tableName : symbolPrimitive;
tableConstraintName : symbolPrimitive;
columnName : symbolPrimitive;
columnConstraintName : symbolPrimitive;

ddl
    : createCommand
    | dropCommand
    ;

createCommand
    : CREATE TABLE qualifiedName ( PAREN_LEFT tableDef PAREN_RIGHT )?                           # CreateTable
    | CREATE INDEX ON symbolPrimitive PAREN_LEFT pathSimple ( COMMA pathSimple )* PAREN_RIGHT   # CreateIndex
    ;

dropCommand
    : DROP TABLE qualifiedName                                  # DropTable
    | DROP INDEX target=symbolPrimitive ON on=symbolPrimitive   # DropIndex
    ;

tableDef
    : tableDefPart ( COMMA tableDefPart )*
    ;

tableDefPart
    : columnName type columnConstraint*                             # ColumnDeclaration
    ;

columnConstraint
    : ( CONSTRAINT columnConstraintName )?  columnConstraintDef
    ;

columnConstraintDef
    : NOT NULL                                  # ColConstrNotNull
    | NULL                                      # ColConstrNull
    ;

/**
 *
 * DATA MANIPULATION LANGUAGE (DML)
 *
 */
// TODO delete / rewrite rules ahead of `v1` release. Legacy DML rules can be deleted. Spec'd rules (e.g. PartiQL RFC
//  and SQL spec) should be easier to rewrite once the legacy DML rules are deleted.
dml
    : updateClause dmlBaseCommand+ whereClause? returningClause?  # DmlBaseWrapper
    | fromClause whereClause? dmlBaseCommand+ returningClause?    # DmlBaseWrapper
    | deleteCommand                                               # DmlDelete
    | insertCommandReturning                                      # DmlInsertReturning
    | dmlBaseCommand                                              # DmlBase
    ;

dmlBaseCommand
    : insertStatement
    | insertStatementLegacy
    | setCommand
    | replaceCommand
    | removeCommand
    | upsertCommand
    ;

pathSimple
    : symbolPrimitive pathSimpleSteps*;

pathSimpleSteps
    : BRACKET_LEFT key=literal BRACKET_RIGHT             # PathSimpleLiteral
    | BRACKET_LEFT key=symbolPrimitive BRACKET_RIGHT     # PathSimpleSymbol
    | PERIOD key=symbolPrimitive                         # PathSimpleDotSymbol
    ;

// Based on https://github.com/partiql/partiql-docs/blob/main/RFCs/0011-partiql-insert.md
// TODO add parsing of target attributes: https://github.com/partiql/partiql-lang-kotlin/issues/841
replaceCommand
    : REPLACE INTO symbolPrimitive asIdent? value=expr;

// Based on https://github.com/partiql/partiql-docs/blob/main/RFCs/0011-partiql-insert.md
// TODO add parsing of target attributes: https://github.com/partiql/partiql-lang-kotlin/issues/841
upsertCommand
    : UPSERT INTO symbolPrimitive asIdent? value=expr;

removeCommand
    : REMOVE pathSimple;

// FIXME #001
//  There is a bug in the old SqlParser that needed to be replicated to the PartiQLParser for the sake of ...
//  ... same functionality. Using 2 returning clauses always uses the second clause. This should be fixed.
//  See GH Issue: https://github.com/partiql/partiql-lang-kotlin/issues/698
//  We essentially use the returning clause, because we currently support this with the SqlParser.
//  See https://github.com/partiql/partiql-lang-kotlin/issues/708
insertCommandReturning
    : INSERT INTO pathSimple VALUE value=expr ( AT pos=expr )? onConflictLegacy? returningClause?;

// See the Grammar at https://github.com/partiql/partiql-docs/blob/main/RFCs/0011-partiql-insert.md#2-proposed-grammar-and-semantics
insertStatement
    : INSERT INTO symbolPrimitive asIdent? value=expr onConflict?
    ;

onConflict
    : ON CONFLICT conflictTarget? conflictAction
    ;

insertStatementLegacy
    : INSERT INTO pathSimple VALUE value=expr ( AT pos=expr )? onConflictLegacy?
    ;

onConflictLegacy
    : ON CONFLICT WHERE expr DO NOTHING
    ;

/**
    <conflict target> ::=
        ( <index target> [, <index target>]... )
        | ( { <primary key> | <composite primary key> } )
        | ON CONSTRAINT <constraint name>
*/
conflictTarget
    : PAREN_LEFT symbolPrimitive (COMMA symbolPrimitive)* PAREN_RIGHT
    | ON CONSTRAINT constraintName;

constraintName
    : symbolPrimitive;

conflictAction
    : DO NOTHING
    | DO REPLACE doReplace
    | DO UPDATE doUpdate;

/*
<do replace> ::= EXCLUDED
    | SET <attr values> [, <attr values>]...
    | VALUE <tuple value>
   [ WHERE <condition> ]
*/
doReplace
    : EXCLUDED ( WHERE condition=expr )?;
    // :TODO add the rest of the grammar

/*
<do update> ::= EXCLUDED
    | SET <attr values> [, <attr values>]...
    | VALUE <tuple value>
   [ WHERE <condition> ]
*/
doUpdate
    : EXCLUDED ( WHERE condition=expr )?;
    // :TODO add the rest of the grammar

updateClause
    : UPDATE tableBaseReference;

setCommand
    : SET setAssignment ( COMMA setAssignment )*;

setAssignment
    : pathSimple EQ expr;

deleteCommand
    : DELETE fromClauseSimple whereClause? returningClause?;

returningClause
    : RETURNING returningColumn ( COMMA returningColumn )*;

returningColumn
    : status=(MODIFIED|ALL) age=(OLD|NEW) ASTERISK
    | status=(MODIFIED|ALL) age=(OLD|NEW) col=expr
    ;

fromClauseSimple
    : FROM pathSimple asIdent? atIdent? byIdent?   # FromClauseSimpleExplicit
    | FROM pathSimple symbolPrimitive              # FromClauseSimpleImplicit
    ;

whereClause
    : WHERE arg=expr;

/**
 *
 * SELECT AND PROJECTION
 *
 */

selectClause
    : SELECT setQuantifierStrategy? ASTERISK          # SelectAll
    | SELECT setQuantifierStrategy? projectionItems   # SelectItems
    | SELECT setQuantifierStrategy? VALUE expr        # SelectValue
    | PIVOT pivot=expr AT at=expr                     # SelectPivot
    ;

projectionItems
    : projectionItem ( COMMA projectionItem )* ;

projectionItem
    : expr ( AS? symbolPrimitive )? ;

setQuantifierStrategy
    : DISTINCT
    | ALL
    ;

/**
 * LET CLAUSE
 */

letClause
    : LET letBinding ( COMMA letBinding )*;

letBinding
    : expr AS symbolPrimitive;

/**
 *
 * ORDER BY CLAUSE
 *
 */

orderByClause
    : ORDER BY orderSortSpec ( COMMA orderSortSpec )*;

orderSortSpec
    : expr dir=(ASC|DESC)? (NULLS nulls=(FIRST|LAST))?;

/**
 *
 * GROUP CLAUSE
 *
 */

groupClause
    : GROUP PARTIAL? BY groupKey ( COMMA groupKey )* groupAlias?;

groupAlias
    : GROUP AS symbolPrimitive;

groupKey
    : key=exprSelect (AS symbolPrimitive)?;

/**
 *
 * Window Function
 * TODO: Remove from experimental once https://github.com/partiql/partiql-docs/issues/31 is resolved and a RFC is approved
 *
 */

over
   : OVER PAREN_LEFT windowPartitionList? windowSortSpecList? PAREN_RIGHT
   ;

windowPartitionList
   : PARTITION BY expr (COMMA expr)*
   ;

windowSortSpecList
   : ORDER BY orderSortSpec (COMMA orderSortSpec)*
   ;

/**
 *
 * SIMPLE CLAUSES
 *
 */

havingClause
    : HAVING arg=exprSelect;

excludeClause
    : EXCLUDE excludeExpr (COMMA excludeExpr)*;

// Require 1 more `excludeExprSteps` (disallow `EXCLUDE a`).
// There's not a clear use case in which a user would exclude a previously introdced binding variable. If a use case
// arises, we can always change the requirement to 0 or more steps.
excludeExpr
    : symbolPrimitive excludeExprSteps+;

excludeExprSteps
    : PERIOD symbolPrimitive                            # ExcludeExprTupleAttr
    | BRACKET_LEFT attr=LITERAL_STRING BRACKET_RIGHT    # ExcludeExprCollectionAttr
    | BRACKET_LEFT index=LITERAL_INTEGER BRACKET_RIGHT  # ExcludeExprCollectionIndex
    | BRACKET_LEFT ASTERISK BRACKET_RIGHT               # ExcludeExprCollectionWildcard
    | PERIOD ASTERISK                                   # ExcludeExprTupleWildcard
    ;

fromClause
    : FROM ( tableReference ( COMMA tableReference)* );

whereClauseSelect
    : WHERE arg=exprSelect;

offsetByClause
    : OFFSET arg=exprSelect;

limitClause
    : LIMIT arg=exprSelect;

/**
 *
 * GRAPH PATTERN MATCHING LANGUAGE (GPML)
 *
 */

gpmlPattern
    : selector=matchSelector? matchPattern;

gpmlPatternList
    : selector=matchSelector? matchPattern ( COMMA matchPattern )*;

matchPattern
    : restrictor=patternRestrictor? variable=patternPathVariable? graphPart*;

graphPart
    : node
    | edge
    | pattern
    ;

matchSelector
    : mod=(ANY|ALL) SHORTEST                  # SelectorBasic
    | ANY k=LITERAL_INTEGER?                  # SelectorAny
    | SHORTEST k=LITERAL_INTEGER GROUP?       # SelectorShortest
    ;

patternPathVariable
    : symbolPrimitive EQ;

patternRestrictor    // Should be TRAIL / ACYCLIC / SIMPLE
    : restrictor=IDENTIFIER;

node
    : PAREN_LEFT symbolPrimitive? ( COLON labelSpec )? whereClause? PAREN_RIGHT;

edge
    : edgeWSpec quantifier=patternQuantifier?    # EdgeWithSpec
    | edgeAbbrev quantifier=patternQuantifier?   # EdgeAbbreviated
    ;

pattern
    : PAREN_LEFT restrictor=patternRestrictor? variable=patternPathVariable? graphPart+ where=whereClause? PAREN_RIGHT quantifier=patternQuantifier?
    | BRACKET_LEFT restrictor=patternRestrictor? variable=patternPathVariable? graphPart+ where=whereClause? BRACKET_RIGHT quantifier=patternQuantifier?
    ;

patternQuantifier
    : quant=( PLUS | ASTERISK )
    | BRACE_LEFT lower=LITERAL_INTEGER COMMA upper=LITERAL_INTEGER? BRACE_RIGHT
    ;

edgeWSpec
    : MINUS edgeSpec MINUS ANGLE_RIGHT             # EdgeSpecRight
    | TILDE edgeSpec TILDE                         # EdgeSpecUndirected
    | ANGLE_LEFT MINUS edgeSpec MINUS              # EdgeSpecLeft
    | TILDE edgeSpec TILDE ANGLE_RIGHT             # EdgeSpecUndirectedRight
    | ANGLE_LEFT TILDE edgeSpec TILDE              # EdgeSpecUndirectedLeft
    | ANGLE_LEFT MINUS edgeSpec MINUS ANGLE_RIGHT  # EdgeSpecBidirectional
    | MINUS edgeSpec MINUS                         # EdgeSpecUndirectedBidirectional
    ;

edgeSpec
    : BRACKET_LEFT symbolPrimitive? ( COLON labelSpec )? whereClause? BRACKET_RIGHT;

labelSpec
    : labelSpec VERTBAR labelTerm        # LabelSpecOr
    | labelTerm                          # LabelSpecTerm
    ;

labelTerm
    : labelTerm AMPERSAND labelFactor    # LabelTermAnd
    | labelFactor                        # LabelTermFactor
    ;

labelFactor
    : BANG labelPrimary                  # LabelFactorNot
    | labelPrimary                       # LabelFactorPrimary
    ;

labelPrimary
    : symbolPrimitive                    # LabelPrimaryName
    | PERCENT                            # LabelPrimaryWild
    | PAREN_LEFT labelSpec PAREN_RIGHT   # LabelPrimaryParen
    ;

edgeAbbrev
    : TILDE
    | TILDE ANGLE_RIGHT
    | ANGLE_LEFT TILDE
    | ANGLE_LEFT? MINUS ANGLE_RIGHT?
    ;

/**
 *
 * TABLES & JOINS
 *
 */

tableReference
    : tablePrimary # TableRefPrimary
    | lhs=tableReference LEFT CROSS JOIN rhs=tablePrimary # TableLeftCrossJoin // PartiQL spec defines LEFT CROSS JOIN; other variants are not defined yet
    | lhs=tableReference CROSS JOIN rhs=tablePrimary # TableCrossJoin // SQL99 defines just CROSS JOIN
    | lhs=tableReference joinType? JOIN rhs=tableReference joinSpec  # TableQualifiedJoin
    ;

tablePrimary
    : tableBaseReference
    | tableUnpivot
    | tableWrapped
    ;

tableBaseReference
    : source=exprSelect symbolPrimitive              # TableBaseRefSymbol
    | source=exprSelect asIdent? atIdent? byIdent?   # TableBaseRefClauses
    | source=exprGraphMatchOne asIdent? atIdent? byIdent?   # TableBaseRefMatch
    ;

tableUnpivot
    : UNPIVOT expr asIdent? atIdent? byIdent?
    ;

tableWrapped
    : PAREN_LEFT tableReference PAREN_RIGHT;

joinType
    : mod=INNER
    | mod=LEFT OUTER?
    | mod=RIGHT OUTER?
    | mod=FULL OUTER?
    | mod=OUTER
    ;

joinSpec
    : ON expr;

/**
 *
 * EXPRESSIONS & PRECEDENCE
 *
 * Precedence Table (from highest to lowest precedence)
 * 1. Primary Expressions: Functions, Literals, Paths, Identifiers, etc (ex: a, f(a), 1, a.b, "a")
 * 2. Unary plus, minus (ex: -a, +a)
 * 3. Multiplication, Division, Modulo (ex: a * b)
 * 4. Addition, Subtraction (ex: a + b)
 * 5. Other operators (ex: a || b, a & b)
 * 6. Predicates (ex: a LIKE b, a < b, a IN b, a = b)
 * 7. IS true/false. Not yet implemented in PartiQL, but defined in SQL-92. (ex: a IS TRUE)
 * 8. NOT (ex: NOT a)
 * 8. AND (ex: a AND b)
 * 9. OR (ex: a OR b)
 *
 */

expr
    : exprBagOp
    ;

exprBagOp
    : lhs=exprBagOp OUTER? op=(UNION | EXCEPT | INTERSECT) (DISTINCT|ALL)? rhs=exprSelect
      order=orderByClause?
      limit=limitClause?
      offset=offsetByClause?  # BagOp
    | exprSelect                                                                                    # QueryBase
    ;

exprSelect
    : select=selectClause
        exclude=excludeClause?
        from=fromClause
        let=letClause?
        where=whereClauseSelect?
        group=groupClause?
        having=havingClause?
        order=orderByClause??
        limit=limitClause??
        offset=offsetByClause?? # SfwQuery
    | exprOr            # SfwBase
    ;

exprOr
    : lhs=exprOr OR rhs=exprAnd     # Or
    | parent=exprAnd                # ExprOrBase
    ;

exprAnd
    : lhs=exprAnd op=AND rhs=exprNot  # And
    | parent=exprNot                  # ExprAndBase
    ;

exprNot
    : <assoc=right> op=NOT rhs=exprNot  # Not
    | parent=exprPredicate              # ExprNotBase
    ;

exprPredicate
    : lhs=exprPredicate op=comparisonOp rhs=mathOp00  # PredicateComparison
    | lhs=exprPredicate IS NOT? type                                                 # PredicateIs
    | lhs=exprPredicate NOT? IN PAREN_LEFT expr PAREN_RIGHT                          # PredicateIn
    | lhs=exprPredicate NOT? IN rhs=mathOp00                                         # PredicateIn
    | lhs=exprPredicate NOT? LIKE rhs=mathOp00 ( ESCAPE escape=expr )?               # PredicateLike
    | lhs=exprPredicate NOT? BETWEEN lower=mathOp00 AND upper=mathOp00               # PredicateBetween
    | parent=mathOp00                                                                # PredicateBase
    ;

comparisonOp
    : LT_EQ
    | GT_EQ
    | ANGLE_LEFT
    | ANGLE_RIGHT
    | EQ
    | ANGLE_LEFT ANGLE_RIGHT
    | BANG EQ
    ;

otherOp
    : OPERATOR
    | AMPERSAND
    // TODO introduce a separate lexical mode for GPML MATCH expressions (https://github.com/partiql/partiql-lang-kotlin/issues/1512)
    //  This will eliminiate the need for this `AMPERSAND` parse branch.
    ;

// TODO : Opreator precedence of `otherOp` may change in the future.
//  SEE: https://github.com/partiql/partiql-docs/issues/50
mathOp00
    : lhs=mathOp00 op=otherOp rhs=mathOp01
    | parent=mathOp01
    ;

mathOp01
    : op=otherOp rhs=mathOp02
    | parent=mathOp02
    ;

mathOp02
    : lhs=mathOp02 op=(PLUS|MINUS) rhs=mathOp03
    | parent=mathOp03
    ;

mathOp03
    : lhs=mathOp03 op=(PERCENT|ASTERISK|SLASH_FORWARD) rhs=valueExpr
    | parent=valueExpr
    ;

valueExpr
    : sign=(PLUS|MINUS) rhs=valueExpr
    | parent=exprPrimary
    ;

exprPrimary
    : exprTerm                   # ExprPrimaryBase
    | cast                       # ExprPrimaryBase
    | sequenceConstructor        # ExprPrimaryBase
    | substring                  # ExprPrimaryBase
    | position                   # ExprPrimaryBase
    | overlay                    # ExprPrimaryBase
    | canCast                    # ExprPrimaryBase  // TODO remove ahead of `v1` release
    | canLosslessCast            # ExprPrimaryBase  // TODO remove ahead of `v1` release
    | extract                    # ExprPrimaryBase
    | coalesce                   # ExprPrimaryBase
    | dateFunction               # ExprPrimaryBase
    | trimFunction               # ExprPrimaryBase
    | nullIf                     # ExprPrimaryBase
    | functionCall               # ExprPrimaryBase
    | exprPrimary pathStep+      # ExprPrimaryPath
    | exprGraphMatchMany         # ExprPrimaryBase
    | caseExpr                   # ExprPrimaryBase
    | valueList                  # ExprPrimaryBase
    | values                     # ExprPrimaryBase
    | windowFunction             # ExprPrimaryBase
    ;

/**
 *
 * PRIMARY EXPRESSIONS
 *
 */

exprTerm
    : PAREN_LEFT expr PAREN_RIGHT    # ExprTermWrappedQuery
    | CURRENT_USER                   # ExprTermCurrentUser
    | CURRENT_DATE                   # ExprTermCurrentDate
    | parameter                      # ExprTermBase
    | varRefExpr                     # ExprTermBase
    | literal                        # ExprTermBase
    | collection                     # ExprTermBase
    | tuple                          # ExprTermBase
    ;

nullIf
    : NULLIF PAREN_LEFT expr COMMA expr PAREN_RIGHT;

coalesce
    : COALESCE PAREN_LEFT expr ( COMMA expr )* PAREN_RIGHT;

caseExpr
    : CASE case=expr? (WHEN whens+=expr THEN thens+=expr)+ (ELSE else=expr)? END;

values
    : VALUES valueRow ( COMMA valueRow )*;

valueRow
    : PAREN_LEFT expr ( COMMA expr )* PAREN_RIGHT;

valueList
    : PAREN_LEFT expr ( COMMA expr )+ PAREN_RIGHT;

sequenceConstructor
    : datatype=(LIST|SEXP) PAREN_LEFT (expr ( COMMA expr )* )? PAREN_RIGHT;

substring
    : SUBSTRING PAREN_LEFT expr ( COMMA expr ( COMMA expr )? )? PAREN_RIGHT
    | SUBSTRING PAREN_LEFT expr ( FROM expr ( FOR expr )? )? PAREN_RIGHT
    ;

/**
* POSITION(<str>, <str>)
* POSITION(<str> IN <str>)
*/
position
    : POSITION PAREN_LEFT expr COMMA expr PAREN_RIGHT
    | POSITION PAREN_LEFT expr IN expr PAREN_RIGHT
    ;

/**
* OVERLAY(<str>, <str>, <int> [, <int>])
* OVERLAY(<str> PLACING <str> FROM <int> [FOR <int>])
*/
overlay
    : OVERLAY PAREN_LEFT expr COMMA expr COMMA expr (COMMA expr)? PAREN_RIGHT
    | OVERLAY PAREN_LEFT expr PLACING expr FROM expr (FOR expr)? PAREN_RIGHT
    ;

// TODO: Remove from experimental once https://github.com/partiql/partiql-docs/issues/31 is resolved and a RFC is approved
/**
*
* Supported Window Functions:
* 1. LAG(expr, [offset [, default]]) OVER([window_partition] window_ordering)
* 2. LEAD(expr, [offset [, default]]) OVER([window_partition] window_ordering)
*
*/
windowFunction
    : func=(LAG|LEAD) PAREN_LEFT expr ( COMMA expr (COMMA expr)?)? PAREN_RIGHT over #LagLeadFunction
    ;

cast
    : CAST PAREN_LEFT expr AS type PAREN_RIGHT;

// TODO remove these ahead of `v1` release
canLosslessCast
    : CAN_LOSSLESS_CAST PAREN_LEFT expr AS type PAREN_RIGHT;

// TODO remove these ahead of `v1` release
canCast
    : CAN_CAST PAREN_LEFT expr AS type PAREN_RIGHT;

extract
    : EXTRACT PAREN_LEFT IDENTIFIER FROM rhs=expr PAREN_RIGHT;

trimFunction
    : func=TRIM PAREN_LEFT ( mod=IDENTIFIER? sub=expr? FROM )? target=expr PAREN_RIGHT;

dateFunction
    : func=(DATE_ADD|DATE_DIFF) PAREN_LEFT dt=IDENTIFIER COMMA expr COMMA expr PAREN_RIGHT;

// SQL-99 10.4 — <routine invocation> ::= <routine name> <SQL argument list>
functionCall
    : qualifiedName PAREN_LEFT ASTERISK PAREN_RIGHT
    | qualifiedName PAREN_LEFT ( setQuantifierStrategy? expr ( COMMA expr )* )? PAREN_RIGHT
    ;

pathStep
    : BRACKET_LEFT key=expr BRACKET_RIGHT        # PathStepIndexExpr
    | BRACKET_LEFT all=ASTERISK BRACKET_RIGHT    # PathStepIndexAll
    | PERIOD key=symbolPrimitive                 # PathStepDotExpr
    | PERIOD all=ASTERISK                        # PathStepDotAll
    ;

exprGraphMatchMany
    :  PAREN_LEFT exprPrimary MATCH gpmlPatternList PAREN_RIGHT ;

exprGraphMatchOne
    :   exprPrimary MATCH gpmlPattern ;


parameter
    : QUESTION_MARK;

varRefExpr
    : qualifier=AT_SIGN? ident=(IDENTIFIER|IDENTIFIER_QUOTED)   # VariableIdentifier
    | qualifier=AT_SIGN? key=nonReserved                # VariableKeyword
    ;

nonReserved
    : /* From SQL99 <non-reserved word> https://ronsavage.github.io/SQL/sql-99.bnf.html#non-reserved%20word */
    ABS | ADA | ADMIN | ASENSITIVE | ASSIGNMENT | ASYMMETRIC | ATOMIC
    | ATTRIBUTE | AVG
    | BIT_LENGTH
    | C | CALLED | CARDINALITY | CATALOG_NAME | CHAIN | CHAR_LENGTH
    | CHARACTERISTICS | CHARACTER_LENGTH | CHARACTER_SET_CATALOG
    | CHARACTER_SET_NAME | CHARACTER_SET_SCHEMA | CHECKED | CLASS_ORIGIN
    | COALESCE | COBOL | COLLATION_CATALOG | COLLATION_NAME | COLLATION_SCHEMA
    | COLUMN_NAME | COMMAND_FUNCTION | COMMAND_FUNCTION_CODE | COMMITTED
    | CONDITION_IDENTIFIER | CONDITION_NUMBER | CONNECTION_NAME
    | CONSTRAINT_CATALOG | CONSTRAINT_NAME | CONSTRAINT_SCHEMA | CONTAINS
    | CONVERT | COUNT | CURSOR_NAME
    | DATETIME_INTERVAL_CODE | DATETIME_INTERVAL_PRECISION | DEFINED
    | DEFINER | DEGREE | DERIVED | DISPATCH
    | EVERY | EXTRACT
    | FINAL | FORTRAN
    | G | GENERATED | GRANTED
    | HIERARCHY
    | IMPLEMENTATION | INSENSITIVE | INSTANCE | INSTANTIABLE | INVOKER
    | K | KEY_MEMBER | KEY_TYPE
    | LENGTH | LOWER
    | M | MAX | MIN | MESSAGE_LENGTH | MESSAGE_OCTET_LENGTH | MESSAGE_TEXT
    | MOD | MORE | MUMPS
    | NAME | NULLABLE | NUMBER | NULLIF
    | OCTET_LENGTH | ORDERING | OPTIONS | OVERLAY | OVERRIDING
    | PASCAL | PARAMETER_MODE | PARAMETER_NAME
    | PARAMETER_ORDINAL_POSITION | PARAMETER_SPECIFIC_CATALOG
    | PARAMETER_SPECIFIC_NAME | PARAMETER_SPECIFIC_SCHEMA | PLI | POSITION
    | REPEATABLE | RETURNED_CARDINALITY | RETURNED_LENGTH
    | RETURNED_OCTET_LENGTH | RETURNED_SQLSTATE | ROUTINE_CATALOG
    | ROUTINE_NAME | ROUTINE_SCHEMA | ROW_COUNT
    | SCALE | SCHEMA_NAME | SCOPE | SECURITY | SELF | SENSITIVE | SERIALIZABLE
    | SERVER_NAME | SIMPLE | SOURCE | SPECIFIC_NAME | STATEMENT | STRUCTURE
    | STYLE | SUBCLASS_ORIGIN | SUBSTRING | SUM | SYMMETRIC | SYSTEM
    | TABLE_NAME | TOP_LEVEL_COUNT | TRANSACTIONS_COMMITTED
    | TRANSACTIONS_ROLLED_BACK | TRANSACTION_ACTIVE | TRANSFORM
    | TRANSFORMS | TRANSLATE | TRIGGER_CATALOG | TRIGGER_SCHEMA
    | TRIGGER_NAME | TRIM | TYPE
    | UNCOMMITTED | UNNAMED | UPPER
    /* PartiQL */
    | EXCLUDED | EXISTS
    | SIZE
    /* Other words not in above */
    | ANY | SOME
    ;

/**
 *
 * LITERALS & TYPES
 *
 */

collection
    : array
    | bag
    ;

array
    : BRACKET_LEFT ( expr ( COMMA expr )* )? BRACKET_RIGHT;

bag
    : ANGLE_LEFT ANGLE_LEFT ( expr ( COMMA expr )* )? ANGLE_RIGHT ANGLE_RIGHT;

tuple
    : BRACE_LEFT ( pair ( COMMA pair )* )? BRACE_RIGHT;

pair
    : lhs=expr COLON rhs=expr;

literal
    : NULL                                                                                # LiteralNull
    | MISSING                                                                             # LiteralMissing
    | TRUE                                                                                # LiteralTrue
    | FALSE                                                                               # LiteralFalse
    | LITERAL_STRING                                                                      # LiteralString
    | LITERAL_INTEGER                                                                     # LiteralInteger
    | LITERAL_DECIMAL                                                                     # LiteralDecimal
    | ION_CLOSURE                                                                         # LiteralIon
    | DATE LITERAL_STRING                                                                 # LiteralDate
    | TIME ( PAREN_LEFT LITERAL_INTEGER PAREN_RIGHT )? (WITH TIME ZONE)? LITERAL_STRING   # LiteralTime
    | TIMESTAMP ( PAREN_LEFT LITERAL_INTEGER PAREN_RIGHT )? (WITH TIME ZONE)? LITERAL_STRING   # LiteralTimestamp
    ;

type
    : datatype=(
        NULL | BOOL | BOOLEAN | SMALLINT | INTEGER2 | INT2 | INTEGER | INT | INTEGER4 | INT4
        | INTEGER8 | INT8 | BIGINT | REAL | CHAR | CHARACTER | MISSING
        | STRING | SYMBOL | BLOB | CLOB | DATE | STRUCT | TUPLE | LIST | SEXP | BAG | ANY
      )                                                                                                                # TypeAtomic
    | datatype=DOUBLE PRECISION                                                                                        # TypeAtomic
    | datatype=(CHARACTER|CHAR|FLOAT|VARCHAR) ( PAREN_LEFT arg0=LITERAL_INTEGER PAREN_RIGHT )?                         # TypeArgSingle
    | CHARACTER VARYING ( PAREN_LEFT arg0=LITERAL_INTEGER PAREN_RIGHT )?                                               # TypeVarChar
    | datatype=(DECIMAL|DEC|NUMERIC) ( PAREN_LEFT arg0=LITERAL_INTEGER ( COMMA arg1=LITERAL_INTEGER )? PAREN_RIGHT )?  # TypeArgDouble
    | datatype=(TIME|TIMESTAMP) ( PAREN_LEFT precision=LITERAL_INTEGER PAREN_RIGHT )? (WITH TIME ZONE)?                # TypeTimeZone
    | symbolPrimitive                                                                                                  # TypeCustom
    ;
