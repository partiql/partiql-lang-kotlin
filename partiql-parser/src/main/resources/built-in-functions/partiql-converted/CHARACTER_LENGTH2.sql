CREATE FUNCTION "CHARACTER_LENGTH"(
    S1 CHARACTER VARYING )
RETURNS NUMERIC
SPECIFIC CHARACTER_LENGTH2
RETURN CHARACTER_LENGTH ( S1 ) ;
