CREATE FUNCTION "MOD"(
    N1 NUMERIC,
N2 SMALLINT )
RETURNS SMALLINT
SPECIFIC MODNUMERICMP_SMALLINT
RETURN MOD ( N1, N2 ) ;
