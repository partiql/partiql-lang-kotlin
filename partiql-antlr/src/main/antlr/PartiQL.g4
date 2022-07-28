
grammar PartiQL;

options {
    tokenVocab=PartiQLTokens;
    caseInsensitive = true;
}

// TODO: Search LATERAL

topQuery: query EOF;

sfwQuery
    : withClause? selectClause fromClause? letClause? whereClause? groupClause? havingClause? orderByClause? limitClause? offsetByClause? # SelectFromWhere
    ;
    
// TODO: Use setQuantifierStrategy
selectClause
    : SELECT setQuantifierStrategy? ASTERISK          # SelectAll
    | SELECT setQuantifierStrategy? projectionItems   # SelectItems
    | SELECT setQuantifierStrategy? VALUE exprQuery   # SelectValue
    | PIVOT pivot=exprQuery AT at=exprQuery           # SelectPivot
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
    
// TODO: Check if this should be a symbol primitive. CLI says that identifiers without @ are allowed
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
    
tableUnpivot: UNPIVOT exprQuery asIdent? atIdent? byIdent? ;
    
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

exprPrimary
    : cast                       # ExprPrimaryBase
    | sequenceConstructor        # ExprPrimaryBase
    | substring                  # ExprPrimaryBase
    | canCast                    # ExprPrimaryBase
    | canLosslessCast            # ExprPrimaryBase
    | extract                    # ExprPrimaryBase
    | dateFunction               # ExprPrimaryBase
    | trimFunction               # ExprPrimaryBase
    | functionCall               # ExprPrimaryBase
    | exprPrimary pathStep+      # ExprPrimaryPath
    | caseExpr                   # ExprPrimaryBase
    | exprTerm                   # ExprPrimaryBase
    ;
    
sequenceConstructor: datatype=(LIST|SEXP) PAREN_LEFT (exprQuery ( COMMA exprQuery )* )? PAREN_RIGHT;
substring
    : SUBSTRING PAREN_LEFT exprQuery ( COMMA exprQuery ( COMMA exprQuery )? )? PAREN_RIGHT                                                      
    | SUBSTRING PAREN_LEFT exprQuery ( FROM exprQuery ( FOR exprQuery )? )? PAREN_RIGHT
    ;
cast: CAST PAREN_LEFT exprQuery AS type PAREN_RIGHT ;
canLosslessCast: CAN_LOSSLESS_CAST PAREN_LEFT exprQuery AS type PAREN_RIGHT ;
canCast: CAN_CAST PAREN_LEFT exprQuery AS type PAREN_RIGHT ;
extract: EXTRACT PAREN_LEFT IDENTIFIER FROM rhs=exprQuery PAREN_RIGHT ;
trimFunction: func=TRIM PAREN_LEFT ( mod=(BOTH|LEADING|TRAILING) FROM ) exprQuery PAREN_RIGHT;
dateFunction: func=(DATE_ADD|DATE_DIFF) PAREN_LEFT dt=IDENTIFIER COMMA exprQuery COMMA exprQuery PAREN_RIGHT ;
functionCall: name=symbolPrimitive PAREN_LEFT ( exprQuery ( COMMA exprQuery )* )? PAREN_RIGHT ;
pathStep
    : BRACKET_LEFT key=exprQuery BRACKET_RIGHT   # PathStepIndexExpr
    | BRACKET_LEFT all=ASTERISK BRACKET_RIGHT    # PathStepIndexAll
    | PERIOD key=varRefExpr                      # PathStepDotExpr
    | PERIOD all=ASTERISK                        # PathStepDotAll
    ;
    
    
// TODO: Uncomment or remove
// fragment DATE_TIME_KEYWORDS: ('YEAR'|'MONTH'|'DAY'|'HOUR'|'MINUTE'|'SECOND'|'TIMEZONE_HOUR'|'TIMEZONE_MINUTE') ;
    
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
    | VARCHAR ( PAREN_LEFT length=LITERAL_INTEGER PAREN_RIGHT )?                                       # TypeVarChar
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

caseExpr: CASE case=exprQuery? (WHEN when=exprQuery THEN then=exprQuery)+ (ELSE exprQuery)? END ;
    
whereClause: WHERE exprQuery ;
    
groupKey
    : exprQuery                     # GroupKeyAliasNone
    | exprQuery AS symbolPrimitive  # GroupKeyAlias
    ;
    
// NOTE: Made group_strategy optional
groupClause: GROUP PARTIAL? BY groupKey ( COMMA groupKey )* groupAlias? ;
groupAlias: GROUP AS symbolPrimitive ;
havingClause: HAVING exprQuery ;
fromClause: FROM tableReference ;
    
// TODO: Check expansion
values: VALUES valueRow ( COMMA valueRow )* ;

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
    
query: querySet ;

offsetByClause: OFFSET exprQuery ;
    
// TODO Check expansion
orderByClause
    : ORDER BY orderSortSpec ( COMMA orderSortSpec )*     # OrderBy
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
