--#[is-type-00]
false IS BOOL;

--#[is-type-01]
item.i_class_id IS INT;

--#[is-type-02]
item.i_brand IS STRING;

--#[is-type-03]
1 IS NULL;

--#[is-type-04]
MISSING IS NULL;

--#[is-type-05]
NULL IS NULL;

--#[is-type-06]
MISSING IS MISSING;

--#[is-type-07]
NULL IS MISSING;

--#[is-type-08]
-- ERROR! always MISSING
MISSING IS BOOL;
