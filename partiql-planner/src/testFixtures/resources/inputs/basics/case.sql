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
-- nullable branch
CASE t_item.t_string
    WHEN 'a' THEN t_item.t_int16_null -- cast(.. AS INT)
    WHEN 'b' THEN t_item.t_int32      -- cast(.. AS INT)
    ELSE t_item.t_int
END;

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
