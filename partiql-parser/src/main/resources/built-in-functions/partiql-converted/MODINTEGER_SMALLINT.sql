CREATE FUNCTION "MOD"(
    N1 INTEGER,
    N2 SMALLINT)
    RETURNS SMALLINT
    SPECIFIC MODINTEGER_SMALLINT
    RETURN MOD(N1, N2) ;
