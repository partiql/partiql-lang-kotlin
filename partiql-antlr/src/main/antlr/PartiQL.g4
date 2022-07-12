
grammar PartiQL;

options {
    tokenVocab=PartiQLTokens;
    caseInsensitive = true;
}

// TODO: Search LATERAL

sfw_query
    : sfw_clauses
    | fws_clauses
    ;
    
sfw_clauses
    : with_clause? select_clause from_clause? where_clause? group_clause? having_clause?
    ;

fws_clauses
    : with_clause? from_clause where_clause? group_clause? having_clause? select_clause
    ;

select_clause
    : SELECT set_quantifier_strategy? ASTERISK
    | SELECT set_quantifier_strategy? projection_items
    | SELECT set_quantifier_strategy? VALUE expr_query
    | PIVOT expr_query AT expr_query
    ;
    
set_quantifier_strategy
    : DISTINCT
    | ALL
    ;
    
// TODO: Check comma
projection_items
    : projection_item ( COMMA projection_item )*
    ;
    
projection_item
    : expr_query ( AS? symbol_primitive )?
    ;
    
symbol_primitive
    : IDENTIFIER
    | IDENTIFIER_QUOTED
    ;
// TODO: Mental note. Needed to duplicate table_joined to remove left recursion
table_reference
    : table_non_join
    | table_reference join_type? CROSS JOIN join_rhs
    | table_reference join_type JOIN LATERAL? join_rhs join_spec
    | table_reference NATURAL join_type JOIN LATERAL? join_rhs
    | PAREN_LEFT table_joined PAREN_RIGHT
    ;
table_non_join
    : table_base_reference
    | table_unpivot
    ;
as_ident
    : AS symbol_primitive
    ;
at_ident
    : AT symbol_primitive
    ;
by_ident
    : BY symbol_primitive
    ;
table_base_reference
    : expr_query symbol_primitive
    | expr_query as_ident? at_ident? by_ident?
    ;
    
// TODO: Check that all uses use a table_reference before token
table_joined
    : table_cross_join
    | table_qualified_join
    | PAREN_LEFT table_joined PAREN_RIGHT
    ;
    
table_unpivot
    : UNPIVOT expr_query as_ident? at_ident?
    ;
    
// TODO: Check that all uses use a table_reference before token
table_cross_join
    : table_reference join_type? CROSS JOIN join_rhs
    ;
// TODO: Check that all uses use a table_reference before token
table_qualified_join
    : table_reference join_type JOIN LATERAL? join_rhs join_spec
    | table_reference NATURAL join_type JOIN LATERAL? join_rhs
    ;
    
join_rhs
    : table_non_join
    | PAREN_LEFT table_joined PAREN_RIGHT
    ;
    
// TODO: Check comma
join_spec
    : ON expr_query
    | USING PAREN_LEFT path_expr ( COMMA path_expr )* PAREN_RIGHT
    ;
    
join_type
    : INNER
    | LEFT OUTER?
    | RIGHT OUTER?
    | FULL OUTER?
    | OUTER
    ;
    
// TODO: Check
function_call
    : name=IDENTIFIER PAREN_LEFT ( function_call_arg ( COMMA function_call_arg )* )? PAREN_RIGHT
    ;
    
function_call_arg
    : function_arg_positional
    | function_arg_named
    ;
    
function_arg_positional
    : ASTERISK
    | expr_query
    ;
    
function_arg_named
    : symbol_primitive COLON expr_query
    ;
    
expr_precedence_01
    : function_call
    | expr_term
    ;
    
literal
    : NULL
    | MISSING
    | TRUE
    | FALSE
    | LITERAL_STRING
    | LITERAL_INTEGER
    | LITERAL_DECIMAL
    | ION_CLOSURE
    | DATE LITERAL_STRING
    | TIME LITERAL_STRING
    | TIMESTAMP LITERAL_STRING
    ;
    
// TODO: Check the '!' in Rust grammar
expr_term
    : PAREN_LEFT query PAREN_RIGHT
    | literal
    | var_ref_expr
    | expr_term_collection
    | expr_term_tuple
    ;
    
expr_term_collection
    : expr_term_array
    | expr_term_bag
    ;
    
// @TODO Check expansion
expr_term_array
    : BRACKET_LEFT ( expr_query ( COMMA expr_query )* )? BRACKET_RIGHT
    ;
expr_term_bag
    : ANGLE_DOUBLE_LEFT ( expr_query ( COMMA expr_query )* )? ANGLE_DOUBLE_RIGHT
    ;
    
// TODO: Check expansion
expr_term_tuple
    : BRACE_LEFT ( expr_pair ( COMMA expr_pair )* )? BRACE_RIGHT
    ;
    
expr_pair
    : expr_query COLON expr_query
    ;
    
var_ref_expr
    : IDENTIFIER
    | IDENTIFIER_AT_UNQUOTED
    | IDENTIFIER_QUOTED
    | IDENTIFIER_AT_QUOTED
    ;
    
path_expr
    : expr_precedence_01 PERIOD path_steps
    | expr_precedence_01 PERIOD ASTERISK
    | expr_precedence_01 BRACKET_LEFT ASTERISK BRACKET_RIGHT
    | expr_precedence_01 BRACKET_LEFT expr_query BRACKET_RIGHT
    ;
    
path_steps
    : path_steps PERIOD path_expr_var_ref
    | path_steps BRACKET_LEFT ASTERISK BRACKET_RIGHT
    | path_steps PERIOD ASTERISK
    | path_steps BRACKET_LEFT expr_query BRACKET_RIGHT // TODO: Add path expression. See Rust impl TODO.
    | path_expr_var_ref
    ;
    
path_expr_var_ref
    : LITERAL_STRING
    | var_ref_expr
    ;

// TODO: Check order and recheck all
expr_query
    : expr_query OR expr_query
    | expr_query AND expr_query
    | NOT expr_query
    | expr_query IS expr_query
    | expr_query IS NOT expr_query
    | expr_query EQ expr_query
    | expr_query NEQ expr_query
    | expr_query ANGLE_LEFT expr_query
    | expr_query ANGLE_RIGHT expr_query
    | expr_query LT_EQ expr_query
    | expr_query GT_EQ expr_query
    | expr_query NOT? BETWEEN expr_query AND expr_query
    | expr_query NOT? LIKE expr_query ( ESCAPE expr_query )?
    | expr_query NOT? IN expr_query
    | expr_query CONCAT expr_query
    | expr_query PLUS expr_query
    | expr_query MINUS expr_query
    | expr_query ASTERISK expr_query
    | expr_query SLASH_FORWARD expr_query
    | expr_query PERCENT expr_query
    | expr_query CARROT expr_query
    | PLUS expr_query
    | MINUS expr_query
    | case_expr
    | path_expr
    | function_call
    | expr_precedence_01
    ;


// TODO: Find in other grammar
case_expr
    :
    ;

    
from_clause
    : FROM table_reference
    ;
    
query
    :
    ;
where_clause
    :
    ;
group_clause
    :
    ;
having_clause
    :
    ;
    
// TODO: Need to figure out
with_clause
    :
    ;
