grammar PartiQL;

options {
    tokenVocab=PartiQLTokens;
    caseInsensitive = true;
}

/**
 *
 * TOP LEVEL
 *
 */

root
    : (EXPLAIN (PAREN_LEFT explainOption (COMMA explainOption)* PAREN_RIGHT)? )? statement;

statement
    : dql COLON_SEMI? EOF          # QueryDql
    | dml COLON_SEMI? EOF          # QueryDml
    | ddl COLON_SEMI? EOF          # QueryDdl
    | execCommand COLON_SEMI? EOF  # QueryExec
    | sqlInvokedRoutine            # CreateRoutineStatement
    ;

// SQL:1999
// <SQL-invoked routine> ::= <schema routine>
sqlInvokedRoutine
    : schemaRoutine
    ;

// SQL:1999
// <schema routine> ::=
//     <schema procedure>
//     | <schema function>
schemaRoutine
    : schemaFunction
    ;

// SQL:1999
// <schema function> ::=
//     CREATE <SQL-invoked function>
schemaFunction
    : CREATE sqlInvokedFunction
    ;

// SQL:1999
// <SQL-invoked function> ::=
//     { <function specification> | <method specification designator> }
//     <routine body>
sqlInvokedFunction
    :
        (
            functionSpecification
        ) routineBody
    ;

// SQL:1999
// <function specification> ::=
//    FUNCTION <schema qualified routine name>
//        <SQL parameter declaration list>
//        <returns clause>
//        <routine characteristics>
//        [ <dispatch clause> ]
functionSpecification
    : FUNCTION name=symbolPrimitive // FIXME: Should be schemaQualifiedRoutineName
        sqlParameterDeclarationList
        returnsClause
        routineCharacteristic*
    ;

// SQL:1999
// <routine characteristic> ::=
//     SPECIFIC <specific name>
//     | and many more...
routineCharacteristic
    : SPECIFIC specificName
    ;

// SQL:1999
// <specific name> ::= <schema qualified name>
specificName
    : name=symbolPrimitive // FIXME: should be ```schemaQualifiedName```
    ;

// SQL:1999
// <returns clause> ::= RETURNS <returns data type> [ <result cast> ]
returnsClause
    : RETURNS returnsDataType
    ;

// SQL:1999
// <returns data type> ::= <data type> [ <locator indication> ]
returnsDataType
    : type
    ;

// SQL:1999
// <routine body> ::=
//     <SQL routine body>
//     | <external body reference>
routineBody
    : sqlRoutineBody
    ;

// SQL:1999
// <SQL parameter declaration list> ::=
//     <left paren>
//     [ <SQL parameter declaration> [ { <comma> <SQL parameter declaration> }... ] ]
//     <right paren>
sqlParameterDeclarationList
    : PAREN_LEFT (sqlParameterDeclaration (COMMA sqlParameterDeclaration)* )? PAREN_RIGHT
    ;

// SQL:1999
// <SQL parameter declaration> ::=
//     [ <parameter mode> ] [ <SQL parameter name> ]
//     <parameter type>
//     [ RESULT ]
sqlParameterDeclaration
    : sqlParameterName? parameterType
    ;

// SQL:1999
// <SQL parameter name> ::= <identifier>
sqlParameterName
    : symbolPrimitive
    ;

// SQL:1999
// <parameter type> ::=
//     <data type> [ <locator indication> ]
parameterType
    : type
    ;

// SQL:1999
// <SQL routine body> ::= <SQL procedure statement>
sqlRoutineBody: sqlProcedureStatement;

// SQL:1999
// <SQL procedure statement> ::= <SQL executable statement>
sqlProcedureStatement: sqlExecutableStatement;

// SQL:1999
// <SQL executable statement> ::=
//     <SQL control statement>
//     | many more alternatives...
sqlExecutableStatement
    : sqlControlStatement
    ;

// SQL:1999
// <SQL control statement> ::=
//     <call statement>
//     | <return statement>
sqlControlStatement
    : returnStatement
    ;

// SQL:1999
// <return statement> ::=
//     RETURN <return value>
// <return value> ::=
//     <value expression>
//     | NULL
returnStatement
    : RETURN expr;

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
    : ident=( IDENTIFIER | IDENTIFIER_QUOTED )
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

tableName : symbolPrimitive;
tableConstraintName : symbolPrimitive;
columnName : symbolPrimitive;
columnConstraintName : symbolPrimitive;

ddl
    : createCommand
    | dropCommand
    ;

createCommand
    : CREATE TABLE tableName ( PAREN_LEFT tableDef PAREN_RIGHT )?                               # CreateTable
    | CREATE INDEX ON symbolPrimitive PAREN_LEFT pathSimple ( COMMA pathSimple )* PAREN_RIGHT   # CreateIndex
    ;

dropCommand
    : DROP TABLE target=tableName                               # DropTable
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

fromClause
    : FROM tableReference;

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
    : lhs=tableReference joinType? CROSS JOIN rhs=joinRhs     # TableCrossJoin
    | lhs=tableReference COMMA rhs=joinRhs                    # TableCrossJoin
    | lhs=tableReference joinType? JOIN rhs=joinRhs joinSpec  # TableQualifiedJoin
    | tableNonJoin                                            # TableRefBase
    | PAREN_LEFT tableReference PAREN_RIGHT                   # TableWrapped
    ;

tableNonJoin
    : tableBaseReference
    | tableUnpivot
    ;

tableBaseReference
    : source=exprSelect symbolPrimitive              # TableBaseRefSymbol
    | source=exprSelect asIdent? atIdent? byIdent?   # TableBaseRefClauses
    | source=exprGraphMatchOne asIdent? atIdent? byIdent?   # TableBaseRefMatch
    ;

tableUnpivot
    : UNPIVOT expr asIdent? atIdent? byIdent?;

joinRhs
    : tableNonJoin                           # JoinRhsBase
    | PAREN_LEFT tableReference PAREN_RIGHT  # JoinRhsTableJoined
    ;

joinSpec
    : ON expr;

joinType
    : mod=INNER
    | mod=LEFT OUTER?
    | mod=RIGHT OUTER?
    | mod=FULL OUTER?
    | mod=OUTER
    ;

/**
 *
 * EXPRESSIONS & PRECEDENCE
 *
 * Precedence Table:
 * 1. Primary Expressions: Functions, Literals, Paths, Identifiers, etc (ex: a, f(a), 1, a.b, "a")
 * 2. Unary plus, minus (ex: -a, +a)
 * 3. Multiplication, Division, Modulo (ex: a * b)
 * 4. Addition, Subtraction (ex: a + b)
 * 5. Other operators (ex: a || b)
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
    : lhs=exprBagOp OUTER? EXCEPT (DISTINCT|ALL)? rhs=exprSelect           # Except
    | lhs=exprBagOp OUTER? UNION (DISTINCT|ALL)? rhs=exprSelect            # Union
    | lhs=exprBagOp OUTER? INTERSECT (DISTINCT|ALL)? rhs=exprSelect        # Intersect
    | exprSelect                                                           # QueryBase
    ;

exprSelect
    : select=selectClause
        from=fromClause
        let=letClause?
        where=whereClauseSelect?
        group=groupClause?
        having=havingClause?
        order=orderByClause?
        limit=limitClause?
        offset=offsetByClause? # SfwQuery
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
    : lhs=exprPredicate op=(LT_EQ|GT_EQ|ANGLE_LEFT|ANGLE_RIGHT|NEQ|EQ) rhs=mathOp00  # PredicateComparison
    | lhs=exprPredicate IS NOT? type                                                 # PredicateIs
    | lhs=exprPredicate NOT? IN PAREN_LEFT expr PAREN_RIGHT                          # PredicateIn
    | lhs=exprPredicate NOT? IN rhs=mathOp00                                         # PredicateIn
    | lhs=exprPredicate NOT? LIKE rhs=mathOp00 ( ESCAPE escape=expr )?               # PredicateLike
    | lhs=exprPredicate NOT? BETWEEN lower=mathOp00 AND upper=mathOp00               # PredicateBetween
    | parent=mathOp00                                                                # PredicateBase
    ;

mathOp00
    : lhs=mathOp00 op=CONCAT rhs=mathOp01
    | parent=mathOp01
    ;

mathOp01
    : lhs=mathOp01 op=(PLUS|MINUS) rhs=mathOp02
    | parent=mathOp02
    ;

mathOp02
    : lhs=mathOp02 op=(PERCENT|ASTERISK|SLASH_FORWARD) rhs=valueExpr
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
    | canCast                    # ExprPrimaryBase
    | canLosslessCast            # ExprPrimaryBase
    | extract                    # ExprPrimaryBase
    | coalesce                   # ExprPrimaryBase
    | dateFunction               # ExprPrimaryBase
    | aggregate                  # ExprPrimaryBase
    | trimFunction               # ExprPrimaryBase
    | functionCall               # ExprPrimaryBase
    | nullIf                     # ExprPrimaryBase
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

aggregate
    : func=COUNT PAREN_LEFT ASTERISK PAREN_RIGHT                                        # CountAll
    | func=(COUNT|MAX|MIN|SUM|AVG|EVERY|ANY|SOME) PAREN_LEFT setQuantifierStrategy? expr PAREN_RIGHT   # AggregateBase
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

canLosslessCast
    : CAN_LOSSLESS_CAST PAREN_LEFT expr AS type PAREN_RIGHT;

canCast
    : CAN_CAST PAREN_LEFT expr AS type PAREN_RIGHT;

extract
    : EXTRACT PAREN_LEFT IDENTIFIER FROM rhs=expr PAREN_RIGHT;

trimFunction
    : func=TRIM PAREN_LEFT ( mod=IDENTIFIER? sub=expr? FROM )? target=expr PAREN_RIGHT;

dateFunction
    : func=(DATE_ADD|DATE_DIFF) PAREN_LEFT dt=IDENTIFIER COMMA expr COMMA expr PAREN_RIGHT;

functionCall
    : name=( CHAR_LENGTH | CHARACTER_LENGTH | OCTET_LENGTH |
        BIT_LENGTH | UPPER | LOWER | SIZE | EXISTS | COUNT )
        PAREN_LEFT ( expr ( COMMA expr )* )? PAREN_RIGHT                         # FunctionCallReserved
    | name=symbolPrimitive PAREN_LEFT ( expr ( COMMA expr )* )? PAREN_RIGHT      # FunctionCallIdent
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
    | qualifier=AT_SIGN? key=nonReservedKeywords                # VariableKeyword
    ;

nonReservedKeywords
    : EXCLUDED
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
    : ANGLE_DOUBLE_LEFT ( expr ( COMMA expr )* )? ANGLE_DOUBLE_RIGHT;

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
