--#[select-00]
SELECT "T"['a'] AS "a", "T"['b'] AS "b", "T"['c'] AS "c" FROM "default"."SCHEMA"."T" AS "T";

--#[select-01]
SELECT "T".* FROM "default"."SCHEMA"."T" AS "T";

--#[select-02]
SELECT "T"['a'] AS "a", "T"['b'] AS "b", "T"['c'] AS "c" FROM "default"."SCHEMA"."T" AS "T";

--#[select-03]
SELECT VALUE "T"['a'] FROM "default"."SCHEMA"."T" AS "T";

--#[select-04]
SELECT "t1".*, "t2".* FROM "default"."SCHEMA"."T" AS "t1" INNER JOIN "default"."SCHEMA"."T" AS "t2" ON true;

--#[select-05]
SELECT "T"['d'].* FROM "default"."SCHEMA"."T" AS "T";

--#[select-06]
SELECT "T" AS "t", "T"['d'].* FROM "default"."SCHEMA"."T" AS "T";

--#[select-07]
SELECT "T"['d'].*, "T"['d'].* FROM "default"."SCHEMA"."T" AS "T";

--#[select-08]
SELECT "T"['d'].* FROM "default"."SCHEMA"."T" AS "T";

--#[select-09]
SELECT "T".* FROM "default"."SCHEMA"."T" AS "T";

--#[select-10]
SELECT "T"['c'] || CURRENT_USER AS "_1" FROM "default"."SCHEMA"."T" AS "T";

--#[select-11]
SELECT CURRENT_USER AS "CURRENT_USER" FROM "default"."SCHEMA"."T" AS "T";

--#[select-12]
SELECT CURRENT_DATE AS "CURRENT_DATE" FROM "default"."SCHEMA"."T" AS "T";

--#[select-13]
SELECT DATE_DIFF(DAY, CURRENT_DATE, CURRENT_DATE) AS "_1" FROM "default"."SCHEMA"."T" AS "T";

--#[select-14]
SELECT DATE_ADD(DAY, 5, CURRENT_DATE) AS "_1" FROM "default"."SCHEMA"."T" AS "T";

--#[select-15]
SELECT DATE_ADD(DAY, -5, CURRENT_DATE) AS "_1" FROM "default"."SCHEMA"."T" AS "T";

--#[select-16]
SELECT "t"['a'] AS "a" FROM "default"."SCHEMA"."T" AS "t";
