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
    // DQL
    : dql

    // DML
    | insertStatement
    | updateStatementSearched
    | deleteStatementSearched
    | upsertStatement
    | replaceStatement

    // DDL
    | createCommand
    | dropCommand

    // OTHER
    | explainStatement
    // TODO: SQL's <execute statement>: | execCommand
    ;

/**
 *
 * COMMON STRUCTURES
 *
 */

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

// <qualified name> ::= [ <schema name> <period> ] <qualified identifier>
qualifiedName : (qualifier+=symbolPrimitive PERIOD)* name=symbolPrimitive;

tableName : symbolPrimitive;
tableConstraintName : symbolPrimitive;
columnName : symbolPrimitive;
columnConstraintName : symbolPrimitive;

/**
 *
 * DATA QUERY LANGUAGE (DQL)
 *
 */

dql
    : expr                # DqlExpr
    | selectStatement     # DqlSelect
    ;

/**
 * EBNF 2023:
 * <direct select statement: multiple rows> ::= <cursor specification>
 * <cursor specification> ::= <query expression> [ <updatability clause> ]
 */
selectStatement
    : queryExpression
    ;

/**
 * Returns an ExprQuerySet
 * Should check that the queryExpressionBody will be a QueryBody, not an Expr.
 *
 * EBNF 2023:
 * <query expression> ::=
 *  [ <with clause> ] <query expression body>
 *  [ <order by clause> ] [ <result offset clause> ] [ <fetch first clause> ]
 */
queryExpression
    : with=withClause? queryExpressionBody orderByClause? limitClause? offsetByClause?
    ;

/**
 * Returns a QueryBody or an Expr
 *
 * EBNF 1999:
 * <query expression body> ::= <non-join query expression> | <joined table>
 * <non-join query expression>    ::=
 *          <non-join query term>
 *      |     <query expression body> UNION [ ALL | DISTINCT ] [ <corresponding spec> ] <query term>
 *      |     <query expression body> EXCEPT [ ALL | DISTINCT ] [ <corresponding spec> ] <query term>
 */
queryExpressionBody
    : nonJoinQueryTerm                                              # NonJoinQueryExpressionTerm
    | queryExpressionBody OUTER? UNION setQuantifierStrategy? queryTerm    # NonJoinQueryExpressionUnion
    | queryExpressionBody OUTER? EXCEPT setQuantifierStrategy? queryTerm   # NonJoinQueryExpressionExcept
    | joinedTable                                                   # QueryExpressionBodyJoinedTable
    ;

/**
 * Returns either a QueryBody or an Expr
 *
 * EBNF 1999:
 * <query term> ::= <non-join query term> | <joined table>
 * <non-join query term>    ::=
 *          <non-join query primary>
 *      |     <query term> INTERSECT [ ALL | DISTINCT ] [ <corresponding spec> ] <query primary>
 */
queryTerm
    : nonJoinQueryPrimary                                                 # QueryTermPrimary
    | queryTerm OUTER? INTERSECT setQuantifierStrategy? queryPrimary      # QueryTermIntersect
    | joinedTable                                                         # QueryTermTable
    ;

/**
 * Returns either an Expr or a QueryBody
 *
 * EBNF 1999:
 * <query primary> ::= <non-join query primary> | <joined table>
 */
queryPrimary
    : nonJoinQueryPrimary
    | joinedTable
    ;

/**
 * Returns either a QueryBody or an Expr.
 *
 * EBNF 1999:
 * <non-join query primary>    ::=
 *          <simple table>
 *      |     <left paren> <non-join query expression> <right paren>
 * <non-join query expression>    ::=
 *          <non-join query term>
 *      |     <query expression body> UNION [ ALL | DISTINCT ] [ <corresponding spec> ] <query term>
 *      |     <query expression body> EXCEPT [ ALL | DISTINCT ] [ <corresponding spec> ] <query term>
 */
nonJoinQueryPrimary
    : simpleTable                                                                          # NonJoinQueryPrimaryTable
    | PAREN_LEFT nonJoinQueryTerm PAREN_RIGHT                                              # NonJoinQueryPrimaryTerm
    | PAREN_LEFT queryExpressionBody OUTER? UNION setQuantifierStrategy? queryTerm PAREN_RIGHT    # NonJoinQueryPrimaryUnion
    | PAREN_LEFT queryExpressionBody OUTER? EXCEPT setQuantifierStrategy? queryTerm PAREN_RIGHT   # NonJoinQueryPrimaryExcept
    ;

/**
 * Returns either an Expr or a QueryBody
 * EBNF 1999:
 * <non-join query term>    ::=
 *          <non-join query primary>
 *      |     <query term> INTERSECT [ ALL | DISTINCT ] [ <corresponding spec> ] <query primary>
 */
nonJoinQueryTerm
    : nonJoinQueryPrimary                                          # NonJoinQueryTermPrimary
    | queryTerm OUTER? INTERSECT setQuantifierStrategy? queryPrimary      # NonJoinQueryTermIntersect
    ;

/**
 * Returns either a QueryBody or an Expr (Table Value Constructor)
 *
 * NOTE: This differs slightly from the EBNF in that we allow for the generic use of Expr to allow for the UNION
 * (and other bag ops) of arbitrary expressions.
 *
 * EBNF 1999:
 * <simple table> ::= <query specification> | <table value constructor> | <explicit table>
 */
simpleTable
    : querySpecification
    | tableValueConstructor
    | expr // This is the rule that deviates from SQL's EBNF.
    ;

/**
 * Returns a QueryBody.SFW.
 * EBNF 1999:
 * <query specification> ::= SELECT [ <set quantifier> ] <select list> <table expression>
 * <table expression> ::= <from clause> [ <where clause> ] [ <group by clause> ] [ <having clause> ] 
 */
querySpecification
    : select=selectClause
        exclude=excludeClause?
        from=fromClause
        let=letClause?
        where=whereClauseSelect?
        group=groupClause?
        having=havingClause?
        window=windowClause?
    ;

/**
 * EBNF 2023:
 * <window clause> ::= WINDOW <window definition list>
 * <window definition list> ::= <window definition> [ { <comma> <window definition> }... ]
 */
windowClause
    : WINDOW windowDefinition (COMMA windowDefinition)*
    ;

/**
 * EBNF 2023:
 * <window definition> ::= <new window name> AS <window specification>
 * <new window name> ::= <window name>
 * <window name> ::= <identifier>
 */
windowDefinition
    : name=symbolPrimitive AS windowSpecification
    ;

//
//
// EXPLAIN
//
//

explainStatement
    : EXPLAIN (PAREN_LEFT explainOption (COMMA explainOption)* PAREN_RIGHT)? statement # Explain
    ;

explainOption
    : param=IDENTIFIER value=IDENTIFIER
    ;

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

comment : COMMENT LITERAL_STRING;

ddl
    : createCommand
    | dropCommand
    ;

createCommand
    : CREATE TABLE qualifiedName ( PAREN_LEFT tableDef PAREN_RIGHT )? tableExtension*               # CreateTable
    // TODO: Do we need this? | CREATE INDEX ON symbolPrimitive PAREN_LEFT pathSimple ( COMMA pathSimple )* PAREN_RIGHT   # CreateIndex
    ;

dropCommand
    : DROP TABLE qualifiedName                                  # DropTable
    | DROP INDEX target=symbolPrimitive ON on=symbolPrimitive   # DropIndex
    ;

tableDef
    : tableElement ( COMMA tableElement)*
    ;

tableElement
    : columnName OPTIONAL? type columnConstraintDef* comment?          # ColumnDefinition
    | ( CONSTRAINT constraintName )?  tableConstraint                  # TableConstrDefinition
    ;

// TODO: For now, we just support table-level Unique/Primary Key constraint
//  Other table-level constraint defined in SQL-99 includes referencial constraint and check constraint
tableConstraint
    : uniqueSpec PAREN_LEFT columnName (COMMA columnName)* PAREN_RIGHT     # TableConstrUnique
    ;

columnConstraintDef
    : ( CONSTRAINT constraintName )?  columnConstraint
    ;

columnConstraint
    : NOT NULL                                  # ColConstrNotNull
    | NULL                                      # ColConstrNull
    | uniqueSpec                                   # ColConstrUnique
    | checkConstraintDef                           # ColConstrCheck
    ;

checkConstraintDef
    : CHECK PAREN_LEFT searchCondition PAREN_RIGHT
    ;

uniqueSpec
    : PRIMARY KEY                                # PrimaryKey
    | UNIQUE                                     # Unique
    ;

// <search condition>    ::= <boolean term> | <search condition> OR <boolean term>
// we cannot do exactly that for the way expression precedence is structured in the grammar file.
// but we at least can eliminate SFW query here.
searchCondition : exprOr;

// SQL/HIVE DDL Extension, Support additional table metadatas such as partition by, tblProperties, etc.
tableExtension
    : PARTITION BY partitionBy                                                             # TblExtensionPartition
    | TBLPROPERTIES PAREN_LEFT keyValuePair (COMMA keyValuePair)* PAREN_RIGHT              # TblExtensionTblProperties
    ;

// Limiting the scope to only allow String as valid value for now
keyValuePair : key=LITERAL_STRING EQ value=LITERAL_STRING;

// For now: just support a list of columns name
// In the future, we might support common partition expression such as Hash(), Range(), etc.
partitionBy
    : PAREN_LEFT columnName (COMMA columnName)* PAREN_RIGHT                   #PartitionColList
    ;

/**
 *
 * DATA MANIPULATION LANGUAGE (DML)
 * TODO: Determine future of REMOVE DML statement: https://github.com/partiql/partiql-lang-kotlin/issues/1668
 * TODO: Determine future of FROM (INSERT, SET, REMOVE) statements: https://github.com/partiql/partiql-lang-kotlin/issues/1669
 * TODO: Implement the RETURNING clause for INSERT/UPDATE. See https://github.com/partiql/partiql-lang-kotlin/issues/1667
 */
 
//
//
// DML Statements
// 
//

/**
 * @see https://github.com/partiql/partiql-lang/blob/main/RFCs/0011-partiql-insert.md#2-proposed-grammar-and-semantics
 */
insertStatement: INSERT INTO tblName=qualifiedName asIdent? insertSource onConflict?;

/**
 * @see https://ronsavage.github.io/SQL/sql-99.bnf.html#update%20statement:%20searched
 */
updateStatementSearched:  UPDATE targetTable=qualifiedName SET setClauseList ( WHERE searchCond=expr )?;

/**
 * @see https://ronsavage.github.io/SQL/sql-99.bnf.html#delete%20statement:%20searched
 */
deleteStatementSearched: DELETE FROM targetTable=qualifiedName ( WHERE searchCond=expr )?;

/**
 * @see https://github.com/partiql/partiql-lang/blob/main/RFCs/0030-partiql-upsert-replace.md
 */
upsertStatement: UPSERT INTO tblName=qualifiedName asIdent? insertSource;

/**
 * @see https://github.com/partiql/partiql-lang/blob/main/RFCs/0030-partiql-upsert-replace.md
 */
replaceStatement: REPLACE INTO tblName=qualifiedName asIdent? insertSource;

//
//
// INSERT STATEMENT STRUCTURES
//
//

insertSource
    : insertFromSubquery
    | insertFromDefault
    ;

insertFromSubquery: insertColumnList? expr;

insertFromDefault: DEFAULT VALUES;

insertColumnList: PAREN_LEFT names+=symbolPrimitive ( COMMA names+=symbolPrimitive )* PAREN_RIGHT;

//
//
// UPDATE STATEMENT STRUCTURES
//
//

/**
 * @see https://ronsavage.github.io/SQL/sql-99.bnf.html#set%20clause%20list
 */
setClauseList: setClause ( COMMA setClause )*;

/**
 * @see https://ronsavage.github.io/SQL/sql-99.bnf.html#set%20clause
 * The above referenced set clause doesn't exactly allow for the flexibility provided by updateTarget (previously
 * named pathSimple). The SQL:1999 EBNF states that <update target> can either be a column name or a column name
 * followed by square brackets and a literal (or other simple value). Since PartiQL allows for setting a nested attribute
 * the updateTarget here provides for a superset of SQL's <update target>
 */
setClause: updateTarget EQ expr;

updateTarget: symbolPrimitive updateTargetStep*;

// TODO: https://github.com/partiql/partiql-lang-kotlin/issues/1671
updateTargetStep
    : updateTargetStepElement
    | updateTargetStepField
    ;

updateTargetStepElement: BRACKET_LEFT key=literal BRACKET_RIGHT;

updateTargetStepField: PERIOD key=symbolPrimitive;

//
//
// ON CONFLICT CLAUSE
// @see https://github.com/partiql/partiql-lang/blob/main/RFCs/0030-partiql-upsert-replace.md
// TODO: Add the rest of the grammar
//

onConflict: ON CONFLICT conflictTarget? conflictAction;

conflictTarget
    : conflictTargetIndex
    | conflictTargetConstraint
    ;

conflictTargetIndex: PAREN_LEFT symbolPrimitive (COMMA symbolPrimitive)* PAREN_RIGHT;

conflictTargetConstraint: ON CONSTRAINT constraintName ;

constraintName: qualifiedName;

conflictAction
    : doNothing
    | doReplace
    | doUpdate
    ;

doNothing: DO NOTHING;

doReplace: DO REPLACE doReplaceAction ( WHERE condition=expr )?;

doUpdate: DO UPDATE doUpdateAction ( WHERE condition=expr )?;

doReplaceAction
    : EXCLUDED
    // ...
    ;

doUpdateAction
    : EXCLUDED
    // ...
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

/**
 * This returns a SetQuantifier.
 *
 * NOTE: This isn't directly represented in SQL's EBNF, however, the fragment is used across several rules. By extracting
 * the fragment, it is easier to share logic to convert these tokens into a SetQuantifier.
 */
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
 * WITH CLAUSE
 *
 * EBNF 1999:
 * <with clause>    ::=   WITH [ RECURSIVE ] <with list>
 * <with list>    ::=   <with list element> [ { <comma> <with list element> }... ] 
 */
withClause
    : WITH RECURSIVE? elements+=withListElement ( COMMA elements+=withListElement)*
    ;

/**
 * EBNF 1999:
 * <with list element>    ::=
 *         <query name>
 *         [ <left paren> <with column list> <right paren> ]
 *         AS <left paren> <query expression> <right paren>
 *         [ <search or cycle clause> ]
 * <with column list>    ::=   <column name list>
 * <column name list>    ::=   <column name> [ { <comma> <column name> }... ]
 * <column name>    ::=   <identifier>
 */
withListElement
    : queryName=symbolPrimitive
        ( PAREN_LEFT withColumnList PAREN_RIGHT )?
        AS PAREN_LEFT queryExpression PAREN_RIGHT // This isn't exactly correct, since exprSelect in the G4 defers to arbitrary expressions. This will need to be checked at the conversion between ANTLR to AST.
    ;

withColumnList
    : columnNames+=symbolPrimitive (COMMA columnNames+=symbolPrimitive)*
    ;

/**
 *
 * ORDER BY CLAUSE
 *
 */

/**
 * <sort specification> ::= <sort key> [ <ordering specification> ] [ <null ordering> ]
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
    : key=expr (AS symbolPrimitive)?;

/**
 *
 * SIMPLE CLAUSES
 *
 */

havingClause
    : HAVING arg=expr;

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
    : WHERE arg=expr;

offsetByClause
    : OFFSET arg=expr;

limitClause
    : LIMIT arg=expr;

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

/**
 * Returns a FromTableRef (FromJoin or FromExpr)
 *
 * NOTE: Some rules needed to be duplicated due to ANTLR's lack of support for indirect left recursion. See joinedTable.
 */
tableReference
    : tablePrimary                                                    # TableReferencePrimary
    | tableReference joinType? CROSS JOIN tablePrimary                # TableReferenceCrossJoin
    | lhs=tableReference joinType? JOIN rhs=tableReference joinSpec   # TableReferenceQualifiedJoin
    ;

/**
 * Returns a FromJoin
 *
 * EBNF:
 * <joined table> ::=   <cross join> | <qualified join> | <natural join> | <union join>
 */
joinedTable
    : crossJoin
    | qualifiedJoin
    ;

/**
 * Returns a (FromJoin).
 * 
 * NOTE: The EBNF is different here, since the PartiQL Specification allows for the use of a joinType
 * before the CROSS token.
 *
 * EBNF 1999:
 * <cross join> ::= <table reference> CROSS JOIN <table primary>
 */
crossJoin
    : tableReference joinType? CROSS JOIN tablePrimary
    ;

/**
 * Returns a (FromJoin)
 *
 * EBNF:
 * <qualified join> ::= <table reference> [ <join type> ] JOIN <table reference> <join specification>
 */
qualifiedJoin
    : lhs=tableReference joinType? JOIN rhs=tableReference joinSpec
    ;

/**
 * Returns a FromTableRef (FromJoin or FromExpr)
 */
tablePrimary
    : tableBaseReference
    | tableUnpivot
    | tableWrapped
    ;

// TODO: We may want to limit this to be more similar to <table primary> in SQL:1999
tableBaseReference
    : source=expr symbolPrimitive              # TableBaseRefSymbol
    | source=expr asIdent? atIdent? byIdent?   # TableBaseRefClauses
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
 * 6. Predicates (ex: a LIKE b, a < b, a IN b, a = b, IS [NOT] NULL|MISSING)
 * 7. IS [NOT] TRUE|FALSE|UNKNOWN
 * 8. NOT (ex: NOT a)
 * 8. AND (ex: a AND b)
 * 9. OR (ex: a OR b)
 *
 */

expr
    : exprOr
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
    | parent=exprBoolTest               # ExprNotBase
    ;

exprBoolTest
    : exprBoolTest IS NOT? truthValue=(TRUE|FALSE|UNKNOWN) # BoolTest
    | parent=exprPredicate                                 # ExprBoolTestBase
    ;

exprPredicate
    : lhs=exprPredicate op=comparisonOp rhs=mathOp00                   # PredicateComparison
    | lhs=exprPredicate IS NOT? NULL                                   # PredicateNull
    | lhs=exprPredicate IS NOT? MISSING                                # PredicateMissing
    | lhs=exprPredicate IS NOT? type                                   # PredicateIs
    | lhs=exprPredicate NOT? IN PAREN_LEFT expr PAREN_RIGHT            # PredicateIn
    | lhs=exprPredicate NOT? IN rhs=mathOp00                           # PredicateIn
    | lhs=exprPredicate NOT? LIKE rhs=mathOp00 ( ESCAPE escape=expr )? # PredicateLike
    | lhs=exprPredicate NOT? BETWEEN lower=mathOp00 AND upper=mathOp00 # PredicateBetween
    | parent=mathOp00                                                  # PredicateBase
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
    : sign rhs=valueExpr
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
    | windowFunction             # ExprPrimaryBase
    | rowValueConstructor        # ExprPrimaryBase
    | tableValueConstructor      # ExprPrimaryBase
    ;

/**
 *
 * PRIMARY EXPRESSIONS
 *
 */
 
/**
 * From SQL:1999:
 * <contextually typed table value constructor> ::= VALUES <contextually typed row value expression list>
 * Or:
 * <table value constructor> ::= VALUES <row value expression list>
 *
 * Since this can be used as a <query specification> (top-level value), we are making this an [expr].
 */
tableValueConstructor
    : VALUES rowValueExpressionList
    ;

/**
 * From SQL:1999:
 * <contextually typed row value expression list>    ::=
 *     <contextually typed row value expression> [ { <comma> <contextually typed row value expression> }... ]
 */
rowValueExpressionList
    : expr ( COMMA expr )*
    ;

/**
 * From SQL:1999:
 * <row value constructor>    ::=
 *     <row value constructor element>
 *     |     [ ROW ] <left paren> <row value constructor element list> <right paren>
 *     |     <row subquery>
 *
 * Since the other variants are covered by [expr], this ANTLR rule specifically targets the second variant.
 */
rowValueConstructor
    : ROW? PAREN_LEFT expr ( COMMA expr )* PAREN_RIGHT
    ;

exprTerm
    : PAREN_LEFT expr PAREN_RIGHT               # ExprTermWrappedQuery
    | PAREN_LEFT queryExpression PAREN_RIGHT    # Subquery
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

/**
 * RFC: https://github.com/partiql/partiql-docs/issues/31.
 * EBNF 2023:
 * <window function> ::= <window function type> OVER <window name or specification>
 */
windowFunction: funcType=windowFunctionType OVER spec=windowNameOrSpecification;

/**
 * EBNF 2023:
 * <window function type> ::=
 *   <rank function type> <left paren> <right paren>
 *   | ROW_NUMBER <left paren> <right paren>
 *   | <lead or lag function>
 *   | and more...
 */
windowFunctionType
    : rankFunctionType PAREN_LEFT PAREN_RIGHT # WindowFunctionTypeRank
    | ROW_NUMBER PAREN_LEFT PAREN_RIGHT       # WindowFunctionTypeRowNumber
    | leadOrLagFunction                       # WindowFunctionTypeLeadOrLag
    ;

/**
 * EBNF 2023:
 * <rank function type> ::= RANK | DENSE_RANK | PERCENT_RANK | CUME_DIST
 */
rankFunctionType
    : RANK | DENSE_RANK | PERCENT_RANK | CUME_DIST
    ;

/**
 * EBNF 2023:
 * <lead or lag function> ::=
 *   <lead or lag> <left paren> <lead or lag extent>
 *   [ <comma> <offset> [ <comma> <default expression> ] ] <right paren>
 *   [ <window function null treatment> ]
 * <lead or lag extent> ::= <value expression>
 * <offset> ::= <unsigned integer>
 * <default expression> ::= <value expression>
 */
leadOrLagFunction
    : name=(LEAD|LAG) PAREN_LEFT extent=expr (COMMA offset=LITERAL_INTEGER (COMMA default=expr)? )? PAREN_RIGHT windowFunctionNullTreatment?
    ;

/**
 * EBNF 2023:
 * <window function null treatment> ::= RESPECT NULLS | IGNORE NULLS
 */
windowFunctionNullTreatment
    : RESPECT NULLS
    | IGNORE NULLS
    ;

/**
 * EBNF 2023:
 * <window name or specification> ::= <window name> | <in-line window specification>
 * <window name> ::= <identifier>
 * <in-line window specification> ::= <window specification>
 */
windowNameOrSpecification
    : symbolPrimitive       # WindowNameOrSpec1
    | windowSpecification   # WindowNameOrSpec2
    ;

/**
 * EBNF 2023:
 * <window specification> ::= <left paren> <window specification details> <right paren>
 * <window specification details> ::= [ <existing window name> ] [ <window partition clause> ] [ <window order clause> ] [ <window frame clause> ]
 * <existing window name> ::= <window name>
 * <window frame clause> ::= ...
 */
windowSpecification
    : PAREN_LEFT
        existingWindowName=symbolPrimitive?
        partition=windowPartitionClause?
        order=orderByClause?
        // TODO: windowFrameClause
        PAREN_RIGHT
    ;

/**
 * <window partition clause> ::= PARTITION BY <window partition column reference list>
 * <window partition column reference list> ::= <window partition column reference> [ { <comma> <window partition column reference> }... ]
 */
windowPartitionClause: PARTITION BY col+=windowPartitionColumnReference (COMMA col+=windowPartitionColumnReference)*;

/**
 * <window partition column reference> ::= <column reference> [ <collate clause> ]
 * <column reference> ::=
 *   <basic identifier chain>
 *   | MODULE <period> <qualified identifier> <period> <column name>
 */
windowPartitionColumnReference: qualifiedName;

cast
    : CAST PAREN_LEFT expr AS type PAREN_RIGHT;

// TODO remove these ahead of `v1` release
canLosslessCast
    : CAN_LOSSLESS_CAST PAREN_LEFT expr AS type PAREN_RIGHT;

// TODO remove these ahead of `v1` release
canCast
    : CAN_CAST PAREN_LEFT expr AS type PAREN_RIGHT;

/**
 * <extract expression> ::= EXTRACT <left paren> <extract field> FROM <extract source> <right paren>
 */
extract
    : EXTRACT PAREN_LEFT extractField FROM rhs=expr PAREN_RIGHT;

trimFunction
    : func=TRIM PAREN_LEFT ( mod=IDENTIFIER? sub=expr? FROM )? target=expr PAREN_RIGHT;

dateFunction
    : func=(DATE_ADD|DATE_DIFF) PAREN_LEFT dt=extractField COMMA expr COMMA expr PAREN_RIGHT;

/**
 * <extract field> ::= <primary datetime field> | <time zone field>
 */
extractField
    : primaryDatetimeField
    | timeZoneField
    ;

/**
 * <primary datetime field> ::= <non-second primary datetime field> | SECOND 
 */
primaryDatetimeField
    : nonSecondPrimaryDatetimeField
    | SECOND
    ;

/**
 * <time zone field> ::= TIMEZONE_HOUR | TIMEZONE_MINUTE 
 */
timeZoneField
    : TIMEZONE_HOUR
    | TIMEZONE_MINUTE
    ;

// SQL-99 10.4 — <routine invocation> ::= <routine name> <SQL argument list>
functionCall
    : qualifiedName PAREN_LEFT ASTERISK PAREN_RIGHT                                          # FunctionCallAsterisk
    | qualifiedName PAREN_LEFT ( setQuantifierStrategy? expr ( COMMA expr )* )? PAREN_RIGHT  # FunctionCallExprArgs
    // This handles COLL_AGG(SELECT ...), where SELECT is not typically allowed. It normally needs to be wrapped in parentheses.
    | qualifiedName PAREN_LEFT ( setQuantifierStrategy? queryExpression)? PAREN_RIGHT        # FunctionCallQueryExpression
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
    | CONVERT | COUNT | CUME_DIST | CURSOR_NAME
    | DATETIME_INTERVAL_CODE | DATETIME_INTERVAL_PRECISION | DEFINED
    | DEFINER | DEGREE | DENSE_RANK | DERIVED | DISPATCH
    | EVERY | EXTRACT
    | FINAL | FORTRAN
    | G | GENERATED | GRANTED
    | HIERARCHY
    | IGNORE | IMPLEMENTATION | INSENSITIVE | INSTANCE | INSTANTIABLE | INVOKER
    | K | KEY_MEMBER | KEY_TYPE
    | LENGTH | LOWER
    | M | MAX | MIN | MESSAGE_LENGTH | MESSAGE_OCTET_LENGTH | MESSAGE_TEXT
    | MOD | MORE | MUMPS
    | NAME | NULLABLE | NUMBER | NULLIF
    | OCTET_LENGTH | ORDERING | OPTIONS | OVERLAY | OVERRIDING
    | PASCAL | PARAMETER_MODE | PARAMETER_NAME
    | PARAMETER_ORDINAL_POSITION | PARAMETER_SPECIFIC_CATALOG
    | PARAMETER_SPECIFIC_NAME | PARAMETER_SPECIFIC_SCHEMA | PERCENT_RANK | PLI | POSITION
    | RANK | REPEATABLE | RESPECT | RETURNED_CARDINALITY | RETURNED_LENGTH
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
    | LITERAL_FLOAT                                                                       # LiteralFloat
    | ION_CLOSURE                                                                         # LiteralIon
    | DATE LITERAL_STRING                                                                 # LiteralDate
    | TIME ( PAREN_LEFT LITERAL_INTEGER PAREN_RIGHT )? (WITH TIME ZONE)? LITERAL_STRING   # LiteralTime
    | TIMESTAMP ( PAREN_LEFT LITERAL_INTEGER PAREN_RIGHT )? (WITH TIME ZONE)? LITERAL_STRING   # LiteralTimestamp
    | intervalLiteral                                                                     # LiteralInterval
    ;

/**
 * EBNF 1999:
 * <interval literal> ::= INTERVAL [ <sign> ] <interval string> <interval qualifier>
 */
intervalLiteral
    : INTERVAL sign? LITERAL_STRING intervalQualifier
    ;

/**
 * EBNF 2023:
 * <sign> ::= <plus sign> | <minus sign>
 */
sign
    : PLUS | MINUS
    ;

type
    : datatype=(
        BOOL | BOOLEAN | TINYINT | SMALLINT | INTEGER2 | INT2 | INTEGER | INT | INTEGER4 | INT4
        | INTEGER8 | INT8 | BIGINT | REAL | CHAR | CHARACTER
        | STRING | SYMBOL | BLOB | CLOB | DATE | ANY
      )                                                                                                                # TypeAtomic
    | datatype=( STRUCT | TUPLE | LIST | ARRAY | SEXP | BAG )                                                          # TypeComplexAtomic
    | datatype=DOUBLE PRECISION                                                                                        # TypeAtomic
    | datatype=(CHARACTER|CHAR|FLOAT|VARCHAR) ( PAREN_LEFT arg0=LITERAL_INTEGER PAREN_RIGHT )?                         # TypeArgSingle
    | CHARACTER VARYING ( PAREN_LEFT arg0=LITERAL_INTEGER PAREN_RIGHT )?                                               # TypeVarChar
    | datatype=(DECIMAL|DEC|NUMERIC) ( PAREN_LEFT arg0=LITERAL_INTEGER ( COMMA arg1=LITERAL_INTEGER )? PAREN_RIGHT )?  # TypeArgDouble
    | datatype=(TIME|TIMESTAMP) ( PAREN_LEFT precision=LITERAL_INTEGER PAREN_RIGHT )? (WITH TIME ZONE)?                # TypeTimeZone
    | datatype=(STRUCT|TUPLE) (ANGLE_LEFT structField ( COMMA structField )* ANGLE_RIGHT)                              # TypeStruct
    | datatype=(LIST|ARRAY) ANGLE_LEFT type ANGLE_RIGHT                                                                # TypeList
    | symbolPrimitive                                                                                                  # TypeCustom
    | intervalType                                                                                                     # TypeInterval
    ;

/**
 * EBNF 2023:
 * <interval type> ::= INTERVAL <interval qualifier>
 */
intervalType: INTERVAL intervalQualifier;

/**
 * EBNF 2023:
 * <interval qualifier> ::=
 *   <start field> TO <end field>
 *   | <single datetime field>
 */
intervalQualifier
    : startField TO endField      # IntervalQualifierBoth
    | singleDatetimeField         # IntervalQualifierSingle
    ;

/**
 * EBNF 2023:
 * <start field> ::= <non-second primary datetime field> [ <left paren> <interval leading field precision> <right paren> ]
 */
startField
    : nonSecondPrimaryDatetimeField (PAREN_LEFT intervalLeadingFieldPrecision PAREN_RIGHT)?
    ;

/**
 * EBNF 2023:
 * <end field> ::= <non-second primary datetime field> | SECOND [ <left paren> <interval fractional seconds precision> <right paren> ]
 */
endField
    : nonSecondPrimaryDatetimeField                                   # EndFieldNonSecond
    | SECOND (PAREN_LEFT intervalLeadingFieldPrecision PAREN_RIGHT)?  # EndFieldSecond
    ;

/**
 * EBNF 1999:
 * <single datetime field> ::=
 *     <non-second primary datetime field> [ <left paren> <interval leading field precision> <right paren> ]
 *     | SECOND [ <left paren> <interval leading field precision> [ <comma> <interval fractional seconds precision> ] <right paren> ]
 */
singleDatetimeField
    : nonSecondPrimaryDatetimeField (PAREN_LEFT intervalLeadingFieldPrecision PAREN_RIGHT)?
    | SECOND (PAREN_LEFT intervalLeadingFieldPrecision (COMMA intervalFractionalSecondsPrecision)? PAREN_RIGHT)?
    ;

/**
 * EBNF 2023:
 * <non-second primary datetime field> ::= YEAR | MONTH | DAY | HOUR | MINUTE
 */
nonSecondPrimaryDatetimeField: YEAR | MONTH | DAY | HOUR | MINUTE;

/**
 * EBNF 2023:
 * <interval leading field precision> ::= <unsigned integer>
 */
intervalLeadingFieldPrecision: LITERAL_INTEGER;

/**
 * EBNF 2023:
 * <interval fractional seconds precision> ::= <unsigned integer>
 */
intervalFractionalSecondsPrecision: LITERAL_INTEGER;

structField
    : columnName OPTIONAL? COLON type columnConstraintDef* comment?
    ;
