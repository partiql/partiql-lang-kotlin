CREATE FUNCTION "MOD"(
    N1 NUMERIC,
N2 NUMERIC)
RETURNS NUMERIC
SPECIFIC MODNUMERICMP_NUMERICP
RETURN MOD ( N1, N2 ) ;
