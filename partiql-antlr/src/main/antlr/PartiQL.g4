
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
    
case_expr
    : CASE expr_query? expr_pair_when_then+ else_clause? END
    ;
    
expr_pair_when_then
    : WHEN expr_query THEN expr_query
    ;
else_clause
    : ELSE expr_query
    ;
    
where_clause
    : WHERE expr_query
    ;
    
group_strategy
    : ALL
    | PARTIAL
    ;
group_key
    : expr_query
    | expr_query AS symbol_primitive
    ;
    
// NOTE: Made group_strategy optional
group_clause
    : GROUP group_strategy? BY group_key (COMMA group_key )* group_alias?
    ;
group_alias
    : GROUP AS symbol_primitive
    ;
having_clause
    : HAVING expr_query
    ;
from_clause
    : FROM ( table_reference COMMA LATERAL? )* table_reference
    ;
    
// TODO: Check expansion
values
    : VALUES value_row ( COMMA value_row )*
    ;

value_row
    : PAREN_LEFT expr_query PAREN_RIGHT
    | expr_term_collection
    ;
    
single_query
    : expr_query
    | sfw_query
    | values
    ;
    
// NOTE: Modified rule
query_set
    : query_set set_op_union_except set_quantifier query_set
    | query_set set_op_intersect set_quantifier single_query
    | single_query
    ;
query
    : query_set order_by_clause? limit_clause? offset_by_clause?
    ;
    
set_op_union_except
    : UNION
    | EXCEPT
    ;

set_op_intersect
    : INTERSECT
    ;
    
set_quantifier
    : DISTINCT
    | ALL?
    ;
    
offset_by_clause
    : OFFSET expr_query
    ;
    
// TODO Check expansion
order_by_clause
    : ORDER BY PRESERVE
    | ORDER BY order_sort_spec ( COMMA order_sort_spec )*
    ;
    
order_sort_spec
    : expr_query by_spec? by_null_spec?
    ;
    
by_spec
    : ASC
    | DESC
    ;
    
by_null_spec
    : NULLS FIRST
    | NULLS LAST
    ;
    
limit_clause
    : LIMIT expr_query
    ;
    
// TODO: Find in other grammar

    
// TODO: Need to figure out
with_clause
    :
    ;
