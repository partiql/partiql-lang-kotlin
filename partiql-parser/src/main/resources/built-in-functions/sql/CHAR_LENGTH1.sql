CREATE FUNCTION "CHAR_LENGTH"(
    S1 CHARACTER ( CML ) )
RETURNS NUMERIC ( P2, 0 )
SPECIFIC CHAR_LENGTH1
RETURN CHAR_LENGTH ( S1 ) ;
