CREATE FUNCTION "ABS"(
    N DECIMAL ( P, S ) )
    RETURNS DECIMAL ( P, S )
    SPECIFIC ABSDECIMALP_S
    RETURN ABS ( N ) ;
