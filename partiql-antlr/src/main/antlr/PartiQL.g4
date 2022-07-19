
grammar PartiQL;

options {
    tokenVocab=PartiQLTokens;
    caseInsensitive = true;
}

// TODO: Search LATERAL

topQuery: query;

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
    
symbolPrimitive
    : IDENTIFIER         # SymbolIdentifierUnquoted
    | IDENTIFIER_QUOTED  # SymbolIdentifierQuoted
    ;
// TODO: Mental note. Needed to duplicate table_joined to remove left recursion
tableReference
    : tableReference joinType? CROSS JOIN joinRhs              # TableRefCrossJoin
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
    : exprTerm                                                             # ExprPrimaryTerm
    | functionCall                                                         # ExprQueryFunctionCall
    | exprPrimary PERIOD pathSteps                                         # ExprPrimaryPath
    | exprPrimary PERIOD ASTERISK                                          # ExprPrimaryPathAll
    | exprPrimary BRACKET_LEFT ASTERISK BRACKET_RIGHT                      # ExprPrimaryPathIndexAll
    | exprPrimary BRACKET_LEFT exprQuery BRACKET_RIGHT                     # ExprPrimaryIndex
    | caseExpr                                                             # ExprQueryCase
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
    
exprQuery
    : booleanExpr
    ;
    
booleanExpr
    : valueExpr predicate[$valueExpr.ctx]?                                  # ExprPredicate
    | NOT rhs=booleanExpr                                                   # ExprQueryNot
    | lhs=booleanExpr AND rhs=booleanExpr                                   # ExprQueryAnd
    | lhs=booleanExpr OR rhs=booleanExpr                                    # ExprQueryOr
    ;
    
// TODO
valueExpr
    : exprPrimary                                                          # ExprQueryPrimary
    | PLUS rhs=valueExpr                                                   # ExprQueryPositive
    | MINUS rhs=valueExpr                                                  # ExprQueryNegative
    | lhs=valueExpr ASTERISK rhs=valueExpr                                 # ExprQueryMultiply
    | lhs=valueExpr SLASH_FORWARD rhs=valueExpr                            # ExprQueryDivide
    | lhs=valueExpr PERCENT rhs=valueExpr                                  # ExprQueryModulo
    | lhs=valueExpr PLUS rhs=valueExpr                                     # ExprQueryPlus
    | lhs=valueExpr MINUS rhs=valueExpr                                    # ExprQueryMinus
    | lhs=valueExpr CONCAT rhs=valueExpr                                   # ExprQueryConcat
    ;
    
// TODO
predicate[ParserRuleContext lhs]
    : ANGLE_LEFT rhs=valueExpr                               # ExprQueryLt
    | LT_EQ rhs=valueExpr                                    # ExprQueryLtEq
    | ANGLE_RIGHT rhs=valueExpr                              # ExprQueryGt
    | GT_EQ rhs=valueExpr                                    # ExprQueryGtEq
    | NEQ rhs=valueExpr                                      # ExprQueryNeq
    | EQ rhs=valueExpr                                       # ExprQueryEq
    | NOT? BETWEEN lower=valueExpr AND upper=valueExpr       # ExprQueryBetween
    | NOT? IN rhs=exprQuery                                       # ExprQueryIn
    | NOT? LIKE rhs=valueExpr ( ESCAPE escape=valueExpr )?   # ExprQueryLike
    | IS NOT? rhs=valueExpr                                  # ExprQueryIs
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
    : exprQuery   # QueryExpr
    | sfwQuery    # QuerySfw
    | values      # QueryValues
    ;
    
// NOTE: Modified rule
querySet
    : querySet setOpUnionExcept setQuantifier querySet
    | querySet setOpIntersect setQuantifier singleQuery
    | singleQuery
    ;
    
// TODO: Determine if the following needs to be uncommented
query
    : querySet //  orderByClause? limitClause? offsetByClause?
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
