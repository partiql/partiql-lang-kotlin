CREATE FUNCTION "MOD"(
    N1 DECIMAL,
N2 SMALLINT )
RETURNS SMALLINT
SPECIFIC MODDECIMALMP_SMALLINT
RETURN MOD ( N1, N2 ) ;
