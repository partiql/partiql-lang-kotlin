CREATE FUNCTION "ABS"(
    N INTERVAL ( HOUR ( IP ) TO MINUTE))
    RETURNS INTERVAL
    (HOUR ( IP) TO MINUTE)
    SPECIFIC ABSINTERVALHOURIP_MINUTE
    RETURN ABS ( N ) ;
