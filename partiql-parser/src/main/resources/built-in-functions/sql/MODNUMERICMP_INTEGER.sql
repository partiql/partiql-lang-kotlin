CREATE FUNCTION "MOD"(
    N1 NUMERIC ( MP, 0 ),
N2 INTEGER )
RETURNS INTEGER
SPECIFIC MODNUMERICMP_INTEGER
RETURN MOD ( N1, N2 ) ;
