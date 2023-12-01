--#[case-00]
CASE
    WHEN FALSE THEN 0
    WHEN TRUE THEN 1
    ELSE 2
END;

--#[case-01]
CASE
    WHEN 1 = 2 THEN 0
    WHEN 2 = 3 THEN 1
    ELSE 3
END;

--#[case-02]
CASE 1
    WHEN 1 THEN 'MATCH!'
    ELSE 'NO MATCH!'
END;

--#[case-03]
CASE 'Hello World'
    WHEN 'Hello World' THEN TRUE
    ELSE FALSE
END;

--#[case-04]
SELECT
    CASE a
        WHEN TRUE THEN 'a IS TRUE'
        ELSE 'a MUST BE FALSE'
    END AS result
FROM T;

--#[case-05]
SELECT
    CASE
        WHEN a = TRUE THEN 'a IS TRUE'
        ELSE 'a MUST BE FALSE'
    END AS result
FROM T;

--#[case-06]
SELECT
    CASE b
        WHEN 10 THEN 'b IS 10'
        ELSE 'b IS NOT 10'
    END AS result
FROM T;

--#[case-07]
-- TODO: This is currently failing as we seemingly cannot search for a nested attribute of a global.
SELECT
    CASE d.e
        WHEN 'WATER' THEN 'd.e IS WATER'
        ELSE 'd.e IS NOT WATER'
    END AS result
FROM T;

--#[case-08]
SELECT
    CASE x
        WHEN 'WATER' THEN 'x IS WATER'
        WHEN 5 THEN 'x IS 5'
        ELSE 'x IS SOMETHING ELSE'
    END AS result
FROM T;

--#[case-09]
-- TODO: When using `x IS STRING` or `x IS DECIMAL`, I found that there are issues with the SqlCalls not receiving
--  the length/precision/scale parameters. This doesn't have to do with CASE_WHEN, but it needs to be addressed.
SELECT
    CASE
        WHEN x IS INT THEN 'x IS INT'
        WHEN x IS STRUCT THEN 'x IS STRUCT'
        ELSE 'x IS SOMETHING ELSE'
    END AS result
FROM T;

--#[case-10]
CASE
    WHEN FALSE THEN 0
    WHEN FALSE THEN 1
    ELSE 2
END;
