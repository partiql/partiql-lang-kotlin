
grammar PartiQL;

options {
    tokenVocab=PartiQLTokens;
    caseInsensitive = true;
}

// TODO: Search LATERAL

topQuery: query EOF;

sfwQuery
    : withClause? selectClause fromClause? letClause? whereClause? groupClause? havingClause? orderByClause? limitClause? offsetByClause? # SelectFromWhere
    | withClause? fromClause whereClause? groupClause? havingClause? selectClause orderByClause? limitClause? offsetByClause?  # FromWhereSelect
    ;
    
selectClause
    : SELECT setQuantifierStrategy? ASTERISK          # SelectAll
    | SELECT setQuantifierStrategy? projectionItems   # SelectItems
    | SELECT setQuantifierStrategy? VALUE exprQuery   # SelectValue
    | PIVOT exprQuery AT exprQuery                    # SelectPivot
    ;
    
letClause: LET letBindings;
letBinding: exprQuery AS symbolPrimitive ;
letBindings: letBinding ( COMMA letBinding )* ;
    
setQuantifierStrategy
    : DISTINCT
    | ALL
    ;
    
// TODO: Check comma
projectionItems: projectionItem ( COMMA projectionItem )* ;
projectionItem: exprQuery ( AS? symbolPrimitive )? ;
    
// TODO: Add other identifiers?
symbolPrimitive
    : IDENTIFIER              # SymbolIdentifierUnquoted
    | IDENTIFIER_QUOTED       # SymbolIdentifierQuoted
    | IDENTIFIER_AT_UNQUOTED  # SymbolIdentifierAtUnquoted
    | IDENTIFIER_AT_QUOTED    # SymbolIdentifierAtQuoted
    ;
// TODO: Mental note. Needed to duplicate table_joined to remove left recursion
tableReference
    : tableReference joinType? CROSS JOIN joinRhs              # TableRefCrossJoin
    | tableReference COMMA joinRhs                             # TableRefCrossJoin
    | tableReference joinType JOIN LATERAL? joinRhs joinSpec   # TableRefJoin
    | tableReference NATURAL joinType JOIN LATERAL? joinRhs    # TableRefNaturalJoin
    | PAREN_LEFT tableJoined PAREN_RIGHT                       # TableRefWrappedJoin
    | tableNonJoin                                             # TableRefNonJoin
    ;
tableNonJoin
    : tableBaseReference          # TableNonJoinBaseRef
    | tableUnpivot                # TableNonJoinUnpivot
    ;
asIdent: AS symbolPrimitive ;
atIdent: AT symbolPrimitive ;
byIdent: BY symbolPrimitive ;
tableBaseReference
    : exprQuery symbolPrimitive             # TableBaseRefSymbol
    | exprQuery asIdent? atIdent? byIdent?  # TableBaseRefClauses
    ;
    
// TODO: Check that all uses use a table_reference before token
tableJoined
    : tableCrossJoin                      # TableJoinedCrossJoin
    | tableQualifiedJoin                  # TableJoinedQualified
    | PAREN_LEFT tableJoined PAREN_RIGHT  # NestedTableJoined
    ;
    
tableUnpivot: UNPIVOT exprQuery asIdent? atIdent? ;
    
// TODO: Check that all uses use a table_reference before token
tableCrossJoin: tableReference joinType? CROSS JOIN joinRhs ;

// TODO: Check that all uses use a table_reference before token
tableQualifiedJoin
    : tableReference joinType JOIN LATERAL? joinRhs joinSpec # QualifiedRefJoin
    | tableReference NATURAL joinType JOIN LATERAL? joinRhs  # QualifiedNaturalRefJoin
    ;
    
joinRhs
    : tableNonJoin                        # JoinRhsNonJoin
    | PAREN_LEFT tableJoined PAREN_RIGHT  # JoinRhsTableJoined
    ;
    
joinSpec
    : ON exprQuery   # JoinSpecOn
    ;

joinType
    : INNER
    | LEFT OUTER?
    | RIGHT OUTER?
    | FULL OUTER?
    | OUTER
    ;

functionCall
    : name=symbolPrimitive PAREN_LEFT ( functionCallArg ( COMMA functionCallArg )* )? PAREN_RIGHT
    ;
    
functionCallArg
    : exprQuery
    ;
    
exprPrimary
    : exprTerm                                                             # ExprPrimaryTerm
    | CAST PAREN_LEFT exprQuery AS type PAREN_RIGHT                        # Cast
    | CAN_CAST PAREN_LEFT exprQuery AS type PAREN_RIGHT                    # CanCast
    | CAN_LOSSLESS_CAST PAREN_LEFT exprQuery AS type PAREN_RIGHT           # CanLosslessCast
    | functionCall                                                         # ExprQueryFunctionCall
    | exprPrimary PERIOD pathSteps                                         # ExprPrimaryPath
    | exprPrimary PERIOD ASTERISK                                          # ExprPrimaryPathAll
    | exprPrimary BRACKET_LEFT ASTERISK BRACKET_RIGHT                      # ExprPrimaryPathIndexAll
    | exprPrimary BRACKET_LEFT exprQuery BRACKET_RIGHT                     # ExprPrimaryIndex
    | caseExpr                                                             # ExprQueryCase
    ;
    
// TODO: Add all types
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
    | (CHARACTER | CHAR) ( PAREN_LEFT length=LITERAL_INTEGER PAREN_RIGHT )?                            # TypeChar
    | TIME ( PAREN_LEFT precision=LITERAL_INTEGER PAREN_RIGHT )? WITH TIME ZONE                        # TypeTimeZone
    | TIME ( PAREN_LEFT precision=LITERAL_INTEGER PAREN_RIGHT )?                                       # TypeTime
    | symbolPrimitive              # TypeCustom
    ;
    
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
    
// TODO: Check the '!' in Rust grammar
exprTerm
    : PAREN_LEFT query PAREN_RIGHT # ExprTermWrappedQuery
    | literal                      # ExprTermLiteral
    | varRefExpr                   # ExprTermVarRefExpr
    | exprTermCollection           # ExprTermExprTermCollection
    | exprTermTuple                # ExprTermExprTermTuple
    ;
    
exprTermCollection
    : exprTermArray
    | exprTermBag
    ;
    
// @TODO Check expansion
exprTermArray
    : BRACKET_LEFT ( exprQuery ( COMMA exprQuery )* )? BRACKET_RIGHT
    ;

exprTermBag
    : ANGLE_DOUBLE_LEFT ( exprQuery ( COMMA exprQuery )* )? ANGLE_DOUBLE_RIGHT
    ;

// TODO: Check expansion
exprTermTuple
    : BRACE_LEFT ( exprPair ( COMMA exprPair )* )? BRACE_RIGHT
    ;

exprPair
    : lhs=exprQuery COLON rhs=exprQuery
    ;
    
varRefExpr
    : IDENTIFIER              # VarRefExprIdentUnquoted
    | IDENTIFIER_AT_UNQUOTED  # VarRefExprIdentAtUnquoted
    | IDENTIFIER_QUOTED       # VarRefExprIdentQuoted
    | IDENTIFIER_AT_QUOTED    # VarRefExprIdentAtQuoted
    ;
    
pathExpr
    : exprPrimary PERIOD pathSteps
    | exprPrimary PERIOD ASTERISK
    | exprPrimary BRACKET_LEFT ASTERISK BRACKET_RIGHT
    | exprPrimary BRACKET_LEFT exprQuery BRACKET_RIGHT
    ;
    
pathSteps
    : pathSteps PERIOD pathExprVarRef
    | pathSteps BRACKET_LEFT ASTERISK BRACKET_RIGHT
    | pathSteps PERIOD ASTERISK
    | pathSteps BRACKET_LEFT exprQuery BRACKET_RIGHT // TODO: Add path expression. See Rust impl TODO.
    | pathExprVarRef
    ;
    
pathExprVarRef
    : LITERAL_STRING
    | varRefExpr
    ;
    
exprQuery : booleanExpr ;
    
/**
 * PRECEDENCE RULES:
 * With the nature of PartiQL's AST, there are some oddities requiring the precedence to be set as f
 */

booleanExpr
    : exprQueryOr
    ;

exprQueryOr
    : lhs=exprQueryOr op=OR rhs=exprQueryOr
    | parent=exprQueryAnd
    ;

exprQueryAnd
    : lhs=exprQueryAnd op=AND rhs=exprQueryAnd
    | parent=exprQueryNot
    ;

exprQueryNot
    : <assoc=right> op=NOT rhs=exprQueryNot
    | parent=exprQueryPredicate
    ;

exprQueryPredicate
    : lhs=exprQueryPredicate op=(LT_EQ|GT_EQ|ANGLE_LEFT|ANGLE_RIGHT|NEQ|EQ) rhs=mathOp00  # PredicateComparison
    | lhs=exprQueryPredicate IS NOT? type                                                 # PredicateIs
    | lhs=exprQueryPredicate NOT? IN rhs=mathOp00                                         # PredicateIn
    | lhs=exprQueryPredicate NOT? LIKE rhs=mathOp00 ( ESCAPE escape=booleanExpr )?        # PredicateLike
    | lhs=exprQueryPredicate NOT? BETWEEN lower=mathOp00 AND upper=mathOp00               # PredicateBetween
    | parent=mathOp00                                                                     # PredicateBase
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

caseExpr
    : CASE exprQuery? exprPairWhenThen+ elseClause? END
    ;
    
exprPairWhenThen
    : WHEN exprQuery THEN exprQuery
    ;
elseClause
    : ELSE exprQuery
    ;
    
whereClause
    : WHERE exprQuery
    ;
    
groupKey
    : exprQuery                     # GroupKeyAliasNone
    | exprQuery AS symbolPrimitive  # GroupKeyAlias
    ;
    
// NOTE: Made group_strategy optional
groupClause
    : GROUP PARTIAL? BY groupKey (COMMA groupKey )* groupAlias?
    ;
groupAlias
    : GROUP AS symbolPrimitive
    ;
havingClause
    : HAVING exprQuery
    ;
fromClause
    : FROM tableReference
    ;
    
// TODO: Check expansion
values
    : VALUES valueRow ( COMMA valueRow )*
    ;

valueRow
    : PAREN_LEFT exprQuery PAREN_RIGHT
    | exprTermCollection
    ;
    
singleQuery
    : exprQuery   # QueryExpr
    | sfwQuery    # QuerySfw
    | values      # QueryValues
    ;
    
// NOTE: Modified rule
querySet
    : lhs=querySet EXCEPT ALL? rhs=singleQuery           # QuerySetExcept
    | lhs=querySet UNION ALL? rhs=singleQuery            # QuerySetUnion
    | lhs=querySet INTERSECT ALL? rhs=singleQuery        # QuerySetIntersect
    | singleQuery                                     # QuerySetSingleQuery
    ;
    
// TODO: Determine if the following needs to be uncommented
query
    : querySet //  orderByClause? limitClause? offsetByClause?
    ;

offsetByClause
    : OFFSET exprQuery
    ;
    
// TODO Check expansion
orderByClause
    : ORDER BY orderSortSpec ( COMMA orderSortSpec )*     # OrderBy
    // ORDER BY PRESERVE
    ;
    
orderSortSpec
    : exprQuery bySpec? byNullSpec?      # OrderBySortSpec
    ;
    
bySpec
    : ASC   # OrderByAsc
    | DESC  # OrderByDesc
    ;
    
byNullSpec
    : NULLS FIRST  # NullSpecFirst
    | NULLS LAST   # NullSpecLast
    ;
    
limitClause
    : LIMIT exprQuery
    ;
    
// TODO: Need to figure out
withClause
    : CARROT
    ;
