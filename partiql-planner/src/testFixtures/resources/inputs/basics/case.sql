-- -----------------------------
--  Exact Numeric
-- -----------------------------

--#[case-when-00]
-- type: (int32)
CASE t_item.t_bool
    WHEN true THEN 0
    WHEN false THEN 1
    ELSE 2
END;

--#[case-when-02]
-- type: (int32)
CASE t_item.t_string
    WHEN 'a' THEN t_item.t_int16  -- cast(.. AS INT4)
    ELSE t_item.t_int32           -- INT4
END;

--#[case-when-03]
-- type: (int64)
CASE t_item.t_string
    WHEN 'a' THEN t_item.t_int16   -- cast(.. AS INT8)
    WHEN 'b' THEN t_item.t_int32   -- cast(.. AS INT8)
    ELSE t_item.t_int64            -- INT8
END;

--#[case-when-04]
-- type: (int)
CASE t_item.t_string
    WHEN 'a' THEN t_item.t_int16  -- cast(.. AS INT)
    WHEN 'b' THEN t_item.t_int32  -- cast(.. AS INT)
    WHEN 'c' THEN t_item.t_int64  -- cast(.. AS INT)
    ELSE t_item.t_int             -- INT
END;

--#[case-when-05]
-- type: (int)
CASE t_item.t_string
    WHEN 'b' THEN t_item.t_int32  -- cast(.. AS INT)
    WHEN 'c' THEN t_item.t_int64  -- cast(.. AS INT)
    ELSE t_item.t_int             -- INT
END;

--#[case-when-06]
-- type: (int)
CASE t_item.t_string
    WHEN 'a' THEN t_item.t_int16  -- cast(.. AS INT)
    WHEN 'b' THEN t_item.t_int32  -- cast(.. AS INT)
    ELSE t_item.t_int             -- INT
END;

--#[case-when-07]
-- type: (int64)
CASE t_item.t_string
    WHEN 'a' THEN t_item.t_int32  -- cast(.. AS INT8)
    WHEN 'b' THEN t_item.t_int64  -- INT8
    ELSE t_item.t_int16           -- cast(.. AS INT8)
END;

--#[case-when-08]
-- type: (int|null)
-- nullable default
CASE t_item.t_string
    WHEN 'a' THEN t_item.t_int16  -- cast(.. AS INT)
    WHEN 'b' THEN t_item.t_int32  -- cast(.. AS INT)
    ELSE t_item.t_int_null        -- INT
END;

--#[case-when-09]
-- type: (int|null)
CASE t_item.t_string
    WHEN 'a' THEN t_item.t_int16_null -- cast(.. AS INT)
    WHEN 'b' THEN t_item.t_int32      -- cast(.. AS INT)
    ELSE t_item.t_int
END;

--#[case-when-10]
-- type: (decimal|null)
-- nullable branch
CASE t_item.t_string
    WHEN 'a' THEN t_item.t_decimal
    WHEN 'b' THEN t_item.t_int32
    ELSE NULL
END;

--#[case-when-11]
-- type: (int|null|missing)
-- TODO should really be (int|missing) but our translation of coalesce doesn't consider types.
COALESCE(CAST(t_item.t_string AS INT), 1);

-- -----------------------------
--  Approximate Numeric
-- -----------------------------

-- TODO model approximate numeric
-- We do not have the appropriate StaticType for this.

--#[case-when-12]
-- type: (float64)
CASE t_item.t_string
    WHEN 'a' THEN t_item.t_int
    ELSE t_item.t_float64
END;

--#[case-when-13]
-- type: (float64|null)
-- nullable branch
CASE t_item.t_string
    WHEN 'a' THEN t_item.t_int
    WHEN 'b' THEN t_item.t_float64
    ELSE NULL
END;

-- -----------------------------
--  Character Strings
-- -----------------------------

--#[case-when-14]
-- type: string
CASE t_item.t_string
    WHEN 'a' THEN t_item.t_string
    ELSE 'default'
END;

--#[case-when-15]
-- type: (string|null)
-- null default
CASE t_item.t_string
    WHEN 'a' THEN t_item.t_string
    ELSE NULL
END;

--#[case-when-16]
-- type: clob
CASE t_item.t_string
    WHEN 'a' THEN t_item.t_string
    WHEN 'b' THEN t_item.t_clob
    ELSE 'default'
END;

--#[case-when-17]
-- type: (clob|null)
-- null default
CASE t_item.t_string
    WHEN 'a' THEN t_item.t_string
    WHEN 'b' THEN t_item.t_clob
    ELSE NULL
END;

-- ----------------------------------
--  Variations of null and missing
-- ----------------------------------

--#[case-when-18]
-- type: (string|null)
CASE t_item.t_string
    WHEN 'a' THEN NULL
    ELSE 'default'
END;

--#[case-when-19]
-- type: (string|null)
CASE t_item.t_string
    WHEN 'a' THEN NULL
    WHEN 'b' THEN NULL
    WHEN 'c' THEN NULL
    WHEN 'd' THEN NULL
    ELSE 'default'
END;

--#[case-when-20]
-- type: null
-- no default, null anyways
CASE t_item.t_string
    WHEN 'a' THEN NULL
END;

--#[case-when-21]
-- type: (string|null)
-- no default
CASE t_item.t_string
    WHEN 'a' THEN 'ok!'
END;

--#[case-when-22]
-- type: (null|missing|int32)
CASE t_item.t_string
    WHEN 'a' THEN t_item.t_absent
    ELSE -1
END;

--#[case-when-23]
-- type: int32
-- false branch is pruned
CASE
    WHEN false THEN t_item.t_absent
    ELSE -1
END;

-- -----------------------------
--  Heterogeneous Branches
-- -----------------------------

--#[case-when-24]
-- type: (int32|int64|string)
CASE t_item.t_string
    WHEN 'a' THEN t_item.t_int32
    WHEN 'b' THEN t_item.t_int64
    ELSE 'default'
END;

--#[case-when-25]
-- type: (int32|int64|string|null)
CASE t_item.t_string
    WHEN 'a' THEN t_item.t_int32
    WHEN 'b' THEN t_item.t_int64
    WHEN 'c' THEN t_item.t_string
    ELSE NULL
END;

--#[case-when-26]
-- type: (int32|int64|string|null)
CASE t_item.t_string
    WHEN 'a' THEN t_item.t_int32
    WHEN 'b' THEN t_item.t_int64_null
    ELSE 'default'
END;

--#[case-when-27]
-- type: (int16|int32|int64|int|decimal|string|clob)
CASE t_item.t_string
    WHEN 'a' THEN t_item.t_num_exact
    WHEN 'b' THEN t_item.t_str
    ELSE 'default'
END;

--#[case-when-28]
-- type: (int16|int32|int64|int|decimal|string|clob|null)
CASE t_item.t_string
    WHEN 'a' THEN t_item.t_num_exact
    WHEN 'b' THEN t_item.t_str
END;

--#[case-when-29]
-- type: (struct_a|struct_b|null)
CASE t_item.t_string
    WHEN 'a' THEN t_item.t_struct_a
    WHEN 'b' THEN t_item.t_struct_b
END;

--#[case-when-30]
-- type: missing
CASE t_item.t_string
    WHEN 'a' THEN MISSING
    WHEN 'b' THEN MISSING
    ELSE MISSING
END;

-- -----------------------------
--  (Unused) old tests
-- -----------------------------

--#[old-case-when-00]
CASE
    WHEN FALSE THEN 0
    WHEN TRUE THEN 1
    ELSE 2
END;

--#[old-case-when-01]
CASE
    WHEN 1 = 2 THEN 0
    WHEN 2 = 3 THEN 1
    ELSE 3
END;

--#[old-case-when-02]
CASE 1
    WHEN 1 THEN 'MATCH!'
    ELSE 'NO MATCH!'
END;

--#[old-case-when-03]
CASE 'Hello World'
    WHEN 'Hello World' THEN TRUE
    ELSE FALSE
END;

--#[old-case-when-04]
SELECT
    CASE a
        WHEN TRUE THEN 'a IS TRUE'
        ELSE 'a MUST BE FALSE'
    END AS result
FROM T;

--#[old-case-when-05]
SELECT
    CASE
        WHEN a = TRUE THEN 'a IS TRUE'
        ELSE 'a MUST BE FALSE'
    END AS result
FROM T;

--#[old-case-when-06]
SELECT
    CASE b
        WHEN 10 THEN 'b IS 10'
        ELSE 'b IS NOT 10'
    END AS result
FROM T;

--#[old-case-when-07]
-- TODO: This is currently failing as we seemingly cannot search for a nested attribute of a global.
SELECT
    CASE d.e
        WHEN 'WATER' THEN 'd.e IS WATER'
        ELSE 'd.e IS NOT WATER'
    END AS result
FROM T;

--#[old-case-when-08]
SELECT
    CASE x
        WHEN 'WATER' THEN 'x IS WATER'
        WHEN 5 THEN 'x IS 5'
        ELSE 'x IS SOMETHING ELSE'
    END AS result
FROM T;

--#[old-case-when-09]
-- TODO: When using `x IS STRING` or `x IS DECIMAL`, I found that there are issues with the SqlCalls not receiving
--  the length/precision/scale parameters. This doesn't have to do with CASE_WHEN, but it needs to be addressed.
SELECT
    CASE
        WHEN x IS INT THEN 'x IS INT'
        WHEN x IS STRUCT THEN 'x IS STRUCT'
        ELSE 'x IS SOMETHING ELSE'
    END AS result
FROM T;

--#[old-case-when-10]
CASE
    WHEN FALSE THEN 0
    WHEN FALSE THEN 1
    ELSE 2
END;
