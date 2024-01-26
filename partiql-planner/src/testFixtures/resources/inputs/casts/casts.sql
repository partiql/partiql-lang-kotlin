--#[cast-00]
cast(t1 as ANY);

--#[cast-01]
cast(t1 as NULL);

--#[cast-02]
cast(t1 as MISSING);

--#[cast-03]
cast(t1 as BOOL);

--#[cast-04]
cast(t1 as INT2);

--#[cast-05]
cast(t1 as INT4);

--#[cast-06]
cast(t1 as INT8);

--#[cast-07]
cast(t1 as INT);

-- #[cast-08]
-- TODO: Cast to parameterized type
cast(t1 as DECIMAL(3,2));

-- #[cast-09]
-- TODO: Cast to parameterized type
cast(t1 as FLOAT(32));

--#[cast-10]
cast(t1 as DOUBLE PRECISION);

--#[cast-11]
cast(t1 as DECIMAL);

--#[cast-12]
cast(t1 as CHAR);

-- #[cast-13]
-- TODO: Cast to parameterized type
cast(t1 as CHAR(3));

--#[cast-14]
cast(t1 as STRING);

-- #[cast-15]
-- TODO: Cast to parameterized type
cast(t1 as VARCHAR(10));

--#[cast-16]
cast(t1 as SYMBOL);

--#[cast-17]
cast(t1 as CLOB);

-- #[cast-18]
-- TODO: Not supported in static type
cast(t1 as BINARY);

--#[cast-19]
-- TODO: Not supported in static type
cast(t1 as BYTE);

--#[cast-20]
cast(t1 as BLOB);

--#[cast-21]
cast(t1 as DATE);

--#[cast-22]
cast(t1 as TIME);

-- #[cast-23]
-- TODO: Cast to parameterized type
cast(t1 as TIME(3));

-- #[cast-24]
-- TODO: Cast to parameterized type
cast(t1 as TIME WITH TIME ZONE);

-- #[cast-25]
-- TODO: Cast to parameterized type
cast(t1 as TIME(3) WITH TIME ZONE);

-- #[cast-26]
-- TODO: Timestamp WITHOUT TIME ZONE semantics changes
cast(t1 as TIMESTAMP);

-- #[cast-27]
-- TODO: Cast to parameterized type
cast(t1 as TIMESTAMP(3));

-- #[cast-28]
-- TODO: Cast to parameterized type
cast(t1 as TIMESTAMP WITH TIME ZONE);

-- #[cast-29]
-- TODO: Cast to parameterized type
cast(t1 as TIMESTAMP(3) WITH TIME ZONE);

-- #[cast-30]
-- TODO: Interval not implemented
cast(t1 as INTERVAL);

--#[cast-31]
cast(t1 as LIST);

--#[cast-32]
cast(t1 as SEXP);

--#[cast-33]
cast(t1 as STRUCT);

--#[cast-34]
cast(t1 as BAG);