
grammar PartiQL;

options {
    tokenVocab=PartiQLTokens;
    caseInsensitive = true;
}

// TODO: Search LATERAL

sfwQuery
    : withClause? selectClause fromClause? whereClause? groupClause? havingClause? # SelectFromWhere
    | withClause? fromClause whereClause? groupClause? havingClause? selectClause  # FromWhereSelect
    ;
    
selectClause
    : SELECT setQuantifierStrategy? ASTERISK          # SelectAll
    | SELECT setQuantifierStrategy? projectionItems   # SelectItems
    | SELECT setQuantifierStrategy? VALUE exprQuery   # SelectValue
    | PIVOT exprQuery AT exprQuery                    # SelectPivot
    ;
    
setQuantifierStrategy
    : DISTINCT
    | ALL
    ;
    
// TODO: Check comma
projectionItems
    : projectionItem ( COMMA projectionItem )*
    ;
    
projectionItem
    : exprQuery ( AS? symbolPrimitive )?
    ;
    
symbolPrimitive
    : IDENTIFIER         # SymbolIdentifierUnquoted
    | IDENTIFIER_QUOTED  # SymbolIdentifierQuoted
    ;
// TODO: Mental note. Needed to duplicate table_joined to remove left recursion
tableReference
    : tableNonJoin                                              # TableRefNonJoin
    | tableReference joinType? CROSS JOIN joinRhs              # TableRefCrossJoin
    | tableReference joinType JOIN LATERAL? joinRhs joinSpec  # TableRefJoin
    | tableReference NATURAL joinType JOIN LATERAL? joinRhs    # TableRefNaturalJoin
    | PAREN_LEFT tableJoined PAREN_RIGHT                         # TableRefWrappedJoin
    ;
tableNonJoin
    : tableBaseReference
    | tableUnpivot
    ;
asIdent
    : AS symbolPrimitive
    ;
atIdent
    : AT symbolPrimitive
    ;
byIdent
    : BY symbolPrimitive
    ;
tableBaseReference
    : exprQuery symbolPrimitive
    | exprQuery asIdent? atIdent? byIdent?
    ;
    
// TODO: Check that all uses use a table_reference before token
tableJoined
    : tableCrossJoin
    | tableQualifiedJoin
    | PAREN_LEFT tableJoined PAREN_RIGHT
    ;
    
tableUnpivot
    : UNPIVOT exprQuery asIdent? atIdent?
    ;
    
// TODO: Check that all uses use a table_reference before token
tableCrossJoin
    : tableReference joinType? CROSS JOIN joinRhs
    ;
// TODO: Check that all uses use a table_reference before token
tableQualifiedJoin
    : tableReference joinType JOIN LATERAL? joinRhs joinSpec
    | tableReference NATURAL joinType JOIN LATERAL? joinRhs
    ;
    
joinRhs
    : tableNonJoin
    | PAREN_LEFT tableJoined PAREN_RIGHT
    ;
    
// TODO: Check comma
joinSpec
    : ON exprQuery
    | USING PAREN_LEFT pathExpr ( COMMA pathExpr )* PAREN_RIGHT
    ;
    
joinType
    : INNER
    | LEFT OUTER?
    | RIGHT OUTER?
    | FULL OUTER?
    | OUTER
    ;
    
// TODO: Check
functionCall
    : name=IDENTIFIER PAREN_LEFT ( functionCallArg ( COMMA functionCallArg )* )? PAREN_RIGHT
    ;
    
functionCallArg
    : functionArgPositional
    | functionArgNamed
    ;
    
functionArgPositional
    : ASTERISK
    | exprQuery
    ;
    
functionArgNamed
    : symbolPrimitive COLON exprQuery
    ;
    
exprPrimary
    : exprTerm
    | functionCall
    ;
    
literal
    : NULL                           # LiteralNull
    | MISSING                        # LiteralMissing
    | TRUE                           # LiteralTrue
    | FALSE                          # LiteralFalse
    | LITERAL_STRING                 # LiteralString
    | LITERAL_INTEGER                # LiteralInteger
    | LITERAL_DECIMAL                # LiteralDecimal
    | ION_CLOSURE                    # LiteralIon
    | DATE LITERAL_STRING            # LiteralDate
    | TIME LITERAL_STRING            # LiteralTime
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
    : exprQuery COLON exprQuery
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

// Note: Reversed order, but see below.
// TODO: Check order and recheck all
exprQuery
    : exprPrimary                                                          # ExprQueryPrimary
    | functionCall                                                         # ExprQueryFunctionCall
    | pathExpr                                                             # ExprQueryPath
    | caseExpr                                                             # ExprQueryCase
    | MINUS rhs=exprQuery                                                  # ExprQueryNegative
    | PLUS rhs=exprQuery                                                   # ExprQueryPositive
    | lhs=exprQuery ASTERISK rhs=exprQuery                                 # ExprQueryMultiply
    | lhs=exprQuery SLASH_FORWARD rhs=exprQuery                            # ExprQueryDivide
    | lhs=exprQuery PERCENT rhs=exprQuery                                  # ExprQueryModulo
    | lhs=exprQuery PLUS rhs=exprQuery                                     # ExprQueryPlus
    | lhs=exprQuery MINUS rhs=exprQuery                                    # ExprQueryMinus
    | lhs=exprQuery CONCAT rhs=exprQuery                                   # ExprQueryConcat
    | lhs=exprQuery NOT? IN rhs=exprQuery                                  # ExprQueryIn
    | lhs=exprQuery NOT? LIKE rhs=exprQuery ( ESCAPE escape=exprQuery )?   # ExprQueryLike
    | lhs=exprQuery NOT? BETWEEN lower=exprQuery AND upper=exprQuery       # ExprQueryBetween
    | lhs=exprQuery ANGLE_LEFT rhs=exprQuery                               # ExprQueryLt
    | lhs=exprQuery LT_EQ rhs=exprQuery                                    # ExprQueryLtEq
    | lhs=exprQuery ANGLE_RIGHT rhs=exprQuery                              # ExprQueryGt
    | lhs=exprQuery GT_EQ rhs=exprQuery                                    # ExprQueryGtEq
    | lhs=exprQuery NEQ rhs=exprQuery                                      # ExprQueryNeq
    | lhs=exprQuery EQ rhs=exprQuery                                       # ExprQueryEq
    | lhs=exprQuery IS NOT? rhs=exprQuery                                  # ExprQueryIs
    | NOT rhs=exprQuery                                                    # ExprQueryNot
    | lhs=exprQuery AND rhs=exprQuery                                      # ExprQueryAnd
    | lhs=exprQuery OR rhs=exprQuery                                       # ExprQueryOr
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
    
groupStrategy
    : ALL
    | PARTIAL
    ;
groupKey
    : exprQuery
    | exprQuery AS symbolPrimitive
    ;
    
// NOTE: Made group_strategy optional
groupClause
    : GROUP groupStrategy? BY groupKey (COMMA groupKey )* groupAlias?
    ;
groupAlias
    : GROUP AS symbolPrimitive
    ;
havingClause
    : HAVING exprQuery
    ;
fromClause
    : FROM ( tableReference COMMA LATERAL? )* tableReference
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
    : exprQuery
    | sfwQuery
    | values
    ;
    
// NOTE: Modified rule
querySet
    : querySet setOpUnionExcept setQuantifier querySet
    | querySet setOpIntersect setQuantifier singleQuery
    | singleQuery
    ;
query
    : querySet orderByClause? limitClause? offsetByClause?
    ;
    
setOpUnionExcept
    : UNION
    | EXCEPT
    ;

setOpIntersect
    : INTERSECT
    ;
    
setQuantifier
    : DISTINCT
    | ALL?
    ;
    
offsetByClause
    : OFFSET exprQuery
    ;
    
// TODO Check expansion
orderByClause
    : ORDER BY PRESERVE
    | ORDER BY orderSortSpec ( COMMA orderSortSpec )*
    ;
    
orderSortSpec
    : exprQuery bySpec? byNullSpec?
    ;
    
bySpec
    : ASC
    | DESC
    ;
    
byNullSpec
    : NULLS FIRST
    | NULLS LAST
    ;
    
limitClause
    : LIMIT exprQuery
    ;
    
// TODO: Need to figure out
withClause
    : CARROT
    ;
