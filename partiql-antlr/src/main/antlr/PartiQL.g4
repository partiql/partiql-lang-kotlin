
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
    
exprPrecedence01
    : functionCall
    | exprTerm
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
    : exprPrecedence01 PERIOD pathSteps
    | exprPrecedence01 PERIOD ASTERISK
    | exprPrecedence01 BRACKET_LEFT ASTERISK BRACKET_RIGHT
    | exprPrecedence01 BRACKET_LEFT exprQuery BRACKET_RIGHT
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

// TODO: Check order and recheck all
exprQuery
    : exprQuery OR exprQuery                                # ExprQueryOr
    | exprQuery AND exprQuery                               # ExprQueryAnd
    | NOT exprQuery                                         # ExprQueryNot
    | exprQuery IS NOT? exprQuery                           # ExprQueryIs
    | exprQuery EQ exprQuery                                # ExprQueryEq
    | exprQuery NEQ exprQuery                               # ExprQueryNeq
    | exprQuery ANGLE_LEFT exprQuery                        # ExprQueryLt
    | exprQuery ANGLE_RIGHT exprQuery                       # ExprQueryGt
    | exprQuery LT_EQ exprQuery                             # ExprQueryLtEq
    | exprQuery GT_EQ exprQuery                             # ExprQueryGtEq
    | exprQuery NOT? BETWEEN exprQuery AND exprQuery        # ExprQueryBetween
    | exprQuery NOT? LIKE exprQuery ( ESCAPE exprQuery )?   # ExprQueryLike
    | exprQuery NOT? IN exprQuery                           # ExprQueryIn
    | exprQuery CONCAT exprQuery                            # ExprQueryConcat
    | exprQuery PLUS exprQuery                              # ExprQueryPlus
    | exprQuery MINUS exprQuery                             # ExprQueryMinus
    | exprQuery ASTERISK exprQuery                          # ExprQueryMultiply
    | exprQuery SLASH_FORWARD exprQuery                     # ExprQueryDivide
    | exprQuery PERCENT exprQuery                           # ExprQueryModulus
    | exprQuery CARROT exprQuery                            # ExprQueryExponent
    | PLUS exprQuery                                        # ExprQueryPositive
    | MINUS exprQuery                                       # ExprQueryNegative
    | caseExpr                                              # ExprQueryCase
    | pathExpr                                              # ExprQueryPath
    | functionCall                                          # ExprQueryFunctionCall
    | exprPrecedence01                                      # ExprQueryPrimary
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
    
// TODO: Find in other grammar

    
// TODO: Need to figure out
withClause
    : CARROT
    ;
