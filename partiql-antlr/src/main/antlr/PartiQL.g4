
grammar PartiQL;

options {
    tokenVocab=PartiQLTokens;
    caseInsensitive = true;
}

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
    : IDENTIFIER
    | IDENTIFIER_QUOTED
    ;

/**
 *
 * QUERY
 *
 */

topQuery
    : dql COLON_SEMI? EOF     # QueryDql
    | dml COLON_SEMI? EOF     # QueryDml
    ;

dql 
    : query;

query
    : querySet;

querySet
    : lhs=querySet EXCEPT ALL? rhs=singleQuery           # QuerySetExcept
    | lhs=querySet UNION ALL? rhs=singleQuery            # QuerySetUnion
    | lhs=querySet INTERSECT ALL? rhs=singleQuery        # QuerySetIntersect
    | singleQuery                                        # QuerySetSingleQuery
    ;

singleQuery
    : expr
    | sfwQuery
    ;

sfwQuery
    : selectClause
        fromClause
        letClause?
        whereClause?
        groupClause?
        havingClause?
        orderByClause?
        limitClause?
        offsetByClause?
    ;
    
/**
 *
 * DATA MANIPULATION LANGUAGE (DML)
 *
 */
 
dml
    : updateClause dmlBaseCommand+ whereClause? returningClause?  # DmlUpdateWhereReturn
    | fromClause whereClause? dmlBaseCommand+ returningClause?    # DmlFromWhereReturn
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
    : symbolPrimitive pathSimpleSteps*
    ;

pathSimpleSteps
    : BRACKET_LEFT key=literal BRACKET_RIGHT             # PathSimpleLiteral
    | BRACKET_LEFT key=symbolPrimitive BRACKET_RIGHT     # PathSimpleSymbol
    | PERIOD key=symbolPrimitive                         # PathSimpleDotSymbol
    ;

removeCommand
    : REMOVE pathSimple
    ;

// FIXME #001
// FIXME: There is a bug in the old SqlParser that needed to be replicated to the PartiQLParser for the sake of ...
// FIXME: ... same functionality. Using 2 returning clauses always uses the second clause. This should be fixed.
// FIXME: See GH Issue: https://github.com/partiql/partiql-lang-kotlin/issues/698
// FIXME: We essentially use the returning clause, because we currently support this with the SqlParser
insertCommandReturning
    : INSERT INTO pathSimple VALUE value=querySet ( AT pos=expr )? onConflict? returningClause?
    ;

insertCommand
    : INSERT INTO pathSimple VALUE value=querySet ( AT pos=expr )? onConflict?  # InsertValue
    | INSERT INTO pathSimple value=querySet                                     # InsertSimple
    ;

onConflict
    : ON CONFLICT WHERE expr DO NOTHING
    ;

updateClause
    : UPDATE tableBaseReference
    ;
    
setCommand
    : SET setAssignment ( COMMA setAssignment )*
    ;

setAssignment
    : pathSimple EQ expr
    ;

deleteCommand
    : DELETE fromClause whereClause? returningClause?
    ;

returningClause
    : RETURNING returningColumn ( COMMA returningColumn )*
    ;
    
returningColumn
    : status=(MODIFIED|ALL) age=(OLD|NEW) ASTERISK
    | status=(MODIFIED|ALL) age=(OLD|NEW) col=expr
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
    : ORDER BY orderSortSpec ( COMMA orderSortSpec )*     # OrderBy
    ;
    
orderSortSpec
    : expr bySpec? byNullSpec?      # OrderBySortSpec
    ;
    
bySpec
    : ASC   # OrderByAsc
    | DESC  # OrderByDesc
    ;
    
byNullSpec
    : NULLS FIRST  # NullSpecFirst
    | NULLS LAST   # NullSpecLast
    ;
    
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
    : expr                     # GroupKeyAliasNone
    | expr AS symbolPrimitive  # GroupKeyAlias
    ;

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
    ;

tableBaseReference
    : expr symbolPrimitive              # TableBaseRefSymbol
    | expr asIdent? atIdent? byIdent?   # TableBaseRefClauses
    ;
    
tableUnpivot
    : UNPIVOT expr asIdent? atIdent? byIdent? ;
    
tableJoined[ParserRuleContext lhs]
    : tableCrossJoin[$lhs]
    | tableQualifiedJoin[$lhs]
    ;

tableCrossJoin[ParserRuleContext lhs]
    : joinType? CROSS JOIN rhs=joinRhs
    | COMMA rhs=joinRhs
    ;

tableQualifiedJoin[ParserRuleContext lhs]
    : joinType? JOIN rhs=joinRhs joinSpec
    ;
    
joinRhs
    : tableNonJoin                           # JoinRhsBase
    | PAREN_LEFT tableReference PAREN_RIGHT  # JoinRhsTableJoined
    ;
    
joinSpec
    : ON expr
    ;

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
 */

expr
    : exprOr;

exprOr
    : lhs=exprOr op=OR rhs=exprOr
    | parent=exprAnd
    ;

exprAnd
    : lhs=exprAnd op=AND rhs=exprAnd
    | parent=exprNot
    ;

exprNot
    : <assoc=right> op=NOT rhs=exprNot
    | parent=exprPredicate
    ;

exprPredicate
    : lhs=exprPredicate op=(LT_EQ|GT_EQ|ANGLE_LEFT|ANGLE_RIGHT|NEQ|EQ) rhs=mathOp00  # PredicateComparison
    | lhs=exprPredicate IS NOT? type                                                 # PredicateIs
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
    : cast                       # ExprPrimaryBase
    | sequenceConstructor        # ExprPrimaryBase
    | substring                  # ExprPrimaryBase
    | canCast                    # ExprPrimaryBase
    | canLosslessCast            # ExprPrimaryBase
    | extract                    # ExprPrimaryBase
    | dateFunction               # ExprPrimaryBase
    | aggregate                  # ExprPrimaryBase
    | trimFunction               # ExprPrimaryBase
    | functionCall               # ExprPrimaryBase
    | exprPrimary pathStep+      # ExprPrimaryPath
    | caseExpr                   # ExprPrimaryBase
    | valueList                  # ExprPrimaryBase
    | values                     # ExprPrimaryBase
    | exprTerm                   # ExprPrimaryBase
    ;
    
/**
 *
 * PRIMARY EXPRESSIONS
 *
 */

exprTerm
    : PAREN_LEFT query PAREN_RIGHT   # ExprTermWrappedQuery
    | parameter                      # ExprTermBase
    | varRefExpr                     # ExprTermBase
    | literal                        # ExprTermBase
    | collection                     # ExprTermBase
    | tuple                          # ExprTermBase
    ;

caseExpr
    : CASE case=expr? (WHEN when=expr THEN then=expr)+ (ELSE expr)? END;

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
    : func=COUNT PAREN_LEFT ASTERISK PAREN_RIGHT                                             # CountAll
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
    : name=symbolPrimitive PAREN_LEFT ( expr ( COMMA expr )* )? PAREN_RIGHT;

pathStep
    : BRACKET_LEFT key=expr BRACKET_RIGHT        # PathStepIndexExpr
    | BRACKET_LEFT all=ASTERISK BRACKET_RIGHT    # PathStepIndexAll
    | PERIOD key=symbolPrimitive                 # PathStepDotExpr
    | PERIOD all=ASTERISK                        # PathStepDotAll
    ;
    
parameter
    : QUESTION_MARK;

varRefExpr
    : IDENTIFIER
    | IDENTIFIER_AT_UNQUOTED
    | IDENTIFIER_QUOTED
    | IDENTIFIER_AT_QUOTED
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
    : NULL                           # LiteralNull
    | MISSING                        # LiteralMissing
    | TRUE                           # LiteralTrue
    | FALSE                          # LiteralFalse
    | LITERAL_STRING                 # LiteralString
    | LITERAL_INTEGER                # LiteralInteger
    | LITERAL_DECIMAL                                                                   # LiteralDecimal
    | ION_CLOSURE                                                                       # LiteralIon
    | DATE LITERAL_STRING                                                               # LiteralDate
    | TIME ( PAREN_LEFT LITERAL_INTEGER PAREN_RIGHT )? WITH TIME ZONE LITERAL_STRING    # LiteralTimeZone
    | TIME ( PAREN_LEFT LITERAL_INTEGER PAREN_RIGHT )? LITERAL_STRING                   # LiteralTime
    ;

type
    : NULL                         # TypeAtomic
    | BOOL                         # TypeAtomic
    | BOOLEAN                      # TypeAtomic
    | SMALLINT                     # TypeAtomic
    | INTEGER2                     # TypeAtomic
    | INT2                         # TypeAtomic
    | INTEGER                      # TypeAtomic
    | INT                          # TypeAtomic
    | INTEGER4                     # TypeAtomic
    | INT4                         # TypeAtomic
    | INTEGER8                     # TypeAtomic
    | INT8                         # TypeAtomic
    | BIGINT                       # TypeAtomic
    | REAL                         # TypeAtomic
    | DOUBLE                       # TypeAtomic
    | TIMESTAMP                    # TypeAtomic
    | CHAR                         # TypeAtomic
    | CHARACTER                    # TypeAtomic
    | MISSING                      # TypeAtomic
    | STRING                       # TypeAtomic
    | SYMBOL                       # TypeAtomic
    | BLOB                         # TypeAtomic
    | CLOB                         # TypeAtomic
    | DATE                         # TypeAtomic
    | STRUCT                       # TypeAtomic
    | TUPLE                        # TypeAtomic
    | LIST                         # TypeAtomic
    | SEXP                         # TypeAtomic
    | BAG                          # TypeAtomic
    | ANY                          # TypeAtomic
    | FLOAT ( PAREN_LEFT precision=LITERAL_INTEGER PAREN_RIGHT )?                                      # TypeFloat
    | DECIMAL ( PAREN_LEFT precision=LITERAL_INTEGER ( COMMA scale=LITERAL_INTEGER )? PAREN_RIGHT )?   # TypeDecimal
    | NUMERIC ( PAREN_LEFT precision=LITERAL_INTEGER ( COMMA scale=LITERAL_INTEGER )? PAREN_RIGHT )?   # TypeNumeric
    | CHARACTER VARYING ( PAREN_LEFT length=LITERAL_INTEGER PAREN_RIGHT )?                             # TypeVarChar
    | VARCHAR ( PAREN_LEFT length=LITERAL_INTEGER PAREN_RIGHT )?                                       # TypeVarChar
    | (CHARACTER | CHAR) ( PAREN_LEFT length=LITERAL_INTEGER PAREN_RIGHT )?                            # TypeChar
    | TIME ( PAREN_LEFT precision=LITERAL_INTEGER PAREN_RIGHT )? WITH TIME ZONE                        # TypeTimeZone
    | TIME ( PAREN_LEFT precision=LITERAL_INTEGER PAREN_RIGHT )?                                       # TypeTime
    | symbolPrimitive              # TypeCustom
    ;
