CREATE FUNCTION "ABS"(
    N DECIMAL )
    RETURNS DECIMAL
    SPECIFIC ABSDECIMALP_S
    RETURN ABS ( N ) ;
