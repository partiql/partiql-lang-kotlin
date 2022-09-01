
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

statement
    : dql COLON_SEMI? EOF          # QueryDql
    | dml COLON_SEMI? EOF          # QueryDml
    | ddl COLON_SEMI? EOF          # QueryDdl
    | execCommand COLON_SEMI? EOF  # QueryExec
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
    : EXEC expr ( args+=expr ( COMMA args+=expr )* )?;

/**
 *
 * DATA DEFINITION LANGUAGE (DDL)
 *
 */

ddl
    : createCommand
    | dropCommand
    ;

createCommand
    : CREATE TABLE symbolPrimitive                                                              # CreateTable
    | CREATE INDEX ON symbolPrimitive PAREN_LEFT pathSimple ( COMMA pathSimple )* PAREN_RIGHT   # CreateIndex
    ;

dropCommand
    : DROP TABLE target=symbolPrimitive                         # DropTable
    | DROP INDEX target=symbolPrimitive ON on=symbolPrimitive   # DropIndex
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
    : insertCommand
    | setCommand
    | removeCommand
    ;

pathSimple
    : symbolPrimitive pathSimpleSteps*;

pathSimpleSteps
    : BRACKET_LEFT key=literal BRACKET_RIGHT             # PathSimpleLiteral
    | BRACKET_LEFT key=symbolPrimitive BRACKET_RIGHT     # PathSimpleSymbol
    | PERIOD key=symbolPrimitive                         # PathSimpleDotSymbol
    ;

removeCommand
    : REMOVE pathSimple;

// FIXME #001
//  There is a bug in the old SqlParser that needed to be replicated to the PartiQLParser for the sake of ...
//  ... same functionality. Using 2 returning clauses always uses the second clause. This should be fixed.
//  See GH Issue: https://github.com/partiql/partiql-lang-kotlin/issues/698
//  We essentially use the returning clause, because we currently support this with the SqlParser.
//  See https://github.com/partiql/partiql-lang-kotlin/issues/708
insertCommandReturning
    : INSERT INTO pathSimple VALUE value=expr ( AT pos=expr )? onConflict? returningClause?;

insertCommand
    : INSERT INTO pathSimple VALUE value=expr ( AT pos=expr )? onConflict?  # InsertValue
    | INSERT INTO pathSimple value=expr                                     # InsertSimple
    ;

onConflict
    : ON CONFLICT WHERE expr DO NOTHING;

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
    : expr (AS symbolPrimitive)?;

/**
 *
 * SIMPLE CLAUSES
 *
 */

havingClause
    : HAVING expr;

fromClause
    : FROM tableReference;

whereClause
    : WHERE expr;

offsetByClause
    : OFFSET expr;

limitClause
    : LIMIT expr;

/**
 *
 * GRAPH PATTERN MATCHING LANGUAGE (GPML)
 *
 */

matchExpr
    : selector=matchSelector? matchPattern;

matchExprList
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
    : PAREN_LEFT symbolPrimitive? patternPartLabel? whereClause? PAREN_RIGHT;

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
    | TILDA edgeSpec TILDA                         # EdgeSpecUndirected
    | ANGLE_LEFT MINUS edgeSpec MINUS              # EdgeSpecLeft
    | TILDA edgeSpec TILDA ANGLE_RIGHT             # EdgeSpecUndirectedRight
    | ANGLE_LEFT TILDA edgeSpec TILDA              # EdgeSpecUndirectedLeft
    | ANGLE_LEFT MINUS edgeSpec MINUS ANGLE_RIGHT  # EdgeSpecBidirectional
    | MINUS edgeSpec MINUS                         # EdgeSpecUndirectedBidirectional
    ;

edgeSpec
    : BRACKET_LEFT symbolPrimitive? patternPartLabel? whereClause? BRACKET_RIGHT;

patternPartLabel
    : COLON symbolPrimitive;

edgeAbbrev
    : TILDA
    | TILDA ANGLE_RIGHT
    | ANGLE_LEFT TILDA
    | ANGLE_LEFT? MINUS ANGLE_RIGHT?
    ;

/**
 *
 * TABLES & JOINS
 *
 */
 
tableReference
    : lhs=tableReference tableJoined[$lhs.ctx] # TableRefBase
    | PAREN_LEFT tableReference PAREN_RIGHT    # TableWrapped
    | tableNonJoin                             # TableRefBase
    ;

tableNonJoin
    : tableBaseReference
    | tableUnpivot
    | tableMatch
    ;

tableBaseReference
    : expr symbolPrimitive              # TableBaseRefSymbol
    | expr asIdent? atIdent? byIdent?   # TableBaseRefClauses
    ;

tableUnpivot
    : UNPIVOT expr asIdent? atIdent? byIdent?;

tableMatch
    : lhs=expr MATCH matchExpr                              # MatchSingle
    | lhs=expr MATCH PAREN_LEFT matchExprList PAREN_RIGHT   # MatchMultiple
    ;

tableJoined[ParserRuleContext lhs]
    : tableCrossJoin[$lhs]
    | tableQualifiedJoin[$lhs]
    ;

tableCrossJoin[ParserRuleContext lhs]
    : joinType? CROSS JOIN rhs=joinRhs
    | COMMA rhs=joinRhs
    ;

tableQualifiedJoin[ParserRuleContext lhs]
    : joinType? JOIN rhs=joinRhs joinSpec;

joinRhs
    : tableNonJoin                           # JoinRhsBase
    | PAREN_LEFT tableReference PAREN_RIGHT  # JoinRhsTableJoined
    ;

joinSpec
    : ON expr;

joinType
    : INNER
    | LEFT OUTER?
    | RIGHT OUTER?
    | FULL OUTER?
    | OUTER
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
    : selectClause
        fromClause
        letClause?
        whereClause?
        groupClause?
        havingClause?
        orderByClause?
        limitClause?
        offsetByClause? # SfwQuery
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
    | caseExpr                   # ExprPrimaryBase
    | valueList                  # ExprPrimaryBase
    | values                     # ExprPrimaryBase
    ;

/**
 *
 * PRIMARY EXPRESSIONS
 *
 */

exprTerm
    : PAREN_LEFT expr PAREN_RIGHT    # ExprTermWrappedQuery
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

aggregate
    : func=COUNT PAREN_LEFT ASTERISK PAREN_RIGHT                                        # CountAll
    | func=(COUNT|MAX|MIN|SUM|AVG) PAREN_LEFT setQuantifierStrategy? expr PAREN_RIGHT   # AggregateBase
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

parameter
    : QUESTION_MARK;

varRefExpr
    : qualifier=AT_SIGN? ident=(IDENTIFIER|IDENTIFIER_QUOTED);

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
    ;

type
    : datatype=(
        NULL | BOOL | BOOLEAN | SMALLINT | INTEGER2 | INT2 | INTEGER | INT | INTEGER4 | INT4
        | INTEGER8 | INT8 | BIGINT | REAL | TIMESTAMP | CHAR | CHARACTER | MISSING
        | STRING | SYMBOL | BLOB | CLOB | DATE | STRUCT | TUPLE | LIST | SEXP | BAG | ANY
      )                                                                                                                # TypeAtomic
    | datatype=DOUBLE PRECISION                                                                                        # TypeAtomic
    | datatype=(CHARACTER|CHAR|FLOAT|VARCHAR) ( PAREN_LEFT arg0=LITERAL_INTEGER PAREN_RIGHT )?                         # TypeArgSingle
    | CHARACTER VARYING ( PAREN_LEFT arg0=LITERAL_INTEGER PAREN_RIGHT )?                                               # TypeVarChar
    | datatype=(DECIMAL|DEC|NUMERIC) ( PAREN_LEFT arg0=LITERAL_INTEGER ( COMMA arg1=LITERAL_INTEGER )? PAREN_RIGHT )?  # TypeArgDouble
    | TIME ( PAREN_LEFT precision=LITERAL_INTEGER PAREN_RIGHT )? (WITH TIME ZONE)?                                     # TypeTimeZone
    | symbolPrimitive                                                                                                  # TypeCustom
    ;
