CREATE FUNCTION "SUBSTRING"(
    S CHARACTER VARYING,
START NUMERIC,
LENGTH NUMERIC )
RETURNS CHARACTER VARYING
SPECIFIC SUBSTRING2
RETURN SUBSTRING ( S FROM START FOR LENGTH ) ;
