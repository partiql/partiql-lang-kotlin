CREATE FUNCTION "OCTET_LENGTH"(
    S1 CHARACTER ( CML ) )
RETURNS NUMERIC ( P2, 0 )
SPECIFIC OCTET_LENGTH1
RETURN OCTET_LENGTH ( S1 ) ;
