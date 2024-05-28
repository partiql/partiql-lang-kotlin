--#[cast-00]
CAST('true' AS BOOL);

-- #[cast-01]
-- CAST('1' AS TINYINT);

--#[cast-02]
CAST('1' AS SMALLINT);

--#[cast-03]
CAST('1' AS INT2);

--#[cast-04]
CAST('1' AS INTEGER);

--#[cast-05]
CAST('1' AS INT);

--#[cast-06]
CAST('1' AS INT4);

--#[cast-07]
CAST('1' AS BIGINT);

--#[cast-08]
CAST('1' AS INT8);

--#[cast-09]
CAST('1' AS REAL);

--#[cast-10]
CAST('1' AS FLOAT);

--#[cast-11]
CAST('1' AS DOUBLE PRECISION);

--#[cast-12]
CAST('1' AS DECIMAL);

--#[cast-13]
CAST('1' AS DECIMAL(1));

--#[cast-14]
CAST('1' AS DECIMAL(1, 0));

--#[cast-15]
CAST('1' AS NUMERIC);

--#[cast-16]
CAST('1' AS NUMERIC(1));

--#[cast-17]
CAST('1' AS NUMERIC(1, 0));

--#[cast-18]
CAST('1' AS CHAR);

--#[cast-19]
CAST('1' AS CHARACTER);

--#[cast-20]
CAST('1' AS CHAR(1));

--#[cast-21]
CAST('1' AS CHARACTER(1));

--#[cast-22]
CAST('1' AS CHARACTER VARYING);

--#[cast-23]
CAST('1' AS VARCHAR);

--#[cast-24]
CAST('1' AS CHARACTER VARYING (1));

--#[cast-25]
CAST('1' AS VARCHAR(1));

--#[cast-26]
CAST('1' AS STRING);

--#[cast-27]
CAST('1' AS BLOB);

--#[cast-28]
CAST('1' AS BLOB(1));

--#[cast-29]
CAST('1' AS CLOB);

--#[cast-30]
-- CAST('1' AS CLOB(1));

-- TODO add datetime casts as its own suite.

--#[cast-31]
CAST('1969-07-16' AS DATE);

--#[cast-32]
CAST('12:00:00' AS TIME);

--#[cast-33]
CAST('1969-07-16 12:00:00' AS TIMESTAMP);

--#[cast-34]
CAST([1,2,3] AS BAG);

--#[cast-35]
CAST(<<1,2,3>> AS LIST);

--#[cast-36]
CAST('1' AS TUPLE);

--#[cast-37]
CAST('1' AS STRUCT);
