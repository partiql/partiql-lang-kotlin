-- ----------------------------------------
--  PartiQL Path Navigation
-- ----------------------------------------

--#[paths-00]
-- tuple navigation
x.y;

--#[paths-01]
-- array navigation with literal
x[0];
    
--#[paths-02]
-- tuple navigation with array notation
x['y'];

--#[paths-03]
-- tuple navigation (2)
x."y";

--#[paths-04]
-- tuple navigation with explicit cast as string
x[CAST(z AS STRING)];

-- ----------------------------------------
--  Composition of Navigation (5 choose 3)
-- ----------------------------------------

--#[paths-05]
x.y[0]['y'];

--#[paths-06]
x.y[0]."y";

--#[paths-07]
x.y[0][CAST(z AS STRING)];

--#[paths-08]
x.y['y']."y";

--#[paths-09]
x.y['y'][CAST(z AS STRING)];

--#[paths-10]
x.y."y"[CAST(z AS STRING)];

--#[paths-11]
x[0]['y']."y";

--#[paths-12]
x[0]['y'][CAST(z AS STRING)];

--#[paths-13]
x[0]."y"[CAST(z AS STRING)];

--#[paths-14]
x['y']."y"[CAST(z AS STRING)];

-- ----------------------------------------
--  Array Navigation with Expressions
-- ----------------------------------------

--#[paths-15]
x[0+1];

--#[paths-16]
x[ABS(1)];

-- ----------------------------------------
--  PartiQL Path Navigation (+SFW)
-- ----------------------------------------

--#[paths-sfw-00]
-- tuple navigation
SELECT t.x.y AS v FROM t;

--#[paths-sfw-01]
-- array navigation with literal
SELECT t.x[0] AS v FROM t;

--#[paths-sfw-02]
-- tuple navigation with array notation (1)
SELECT t.x['y'] AS v FROM t;

--#[paths-sfw-03]
-- tuple navigation with array notation (2)
SELECT t.x."y" AS v FROM t;

--#[paths-sfw-04]
-- tuple navigation with explicit cast as string
SELECT t.x[CAST(t.z AS STRING)] AS v FROM t;

-- ----------------------------------------
--  Composition of Navigation (5 choose 3)
-- ----------------------------------------

--#[paths-sfw-05]
SELECT t.x.y[0]['y'] AS v FROM t;

--#[paths-sfw-06]
SELECT t.x.y[0]."y" AS v FROM t;

--#[paths-sfw-07]
SELECT t.x.y[0][CAST(t.z AS STRING)] AS v FROM t;

--#[paths-sfw-08]
SELECT t.x.y['y']."y" AS v FROM t;

--#[paths-sfw-09]
SELECT t.x.y['y'][CAST(t.z AS STRING)] AS v FROM t;

--#[paths-sfw-10]
SELECT t.x.y."y"[CAST(t.z AS STRING)] AS v FROM t;

--#[paths-sfw-11]
SELECT t.x[0]['y']."y" AS v FROM t;

--#[paths-sfw-12]
SELECT t.x[0]['y'][CAST(t.z AS STRING)] AS v FROM t;

--#[paths-sfw-13]
SELECT t.x[0]."y"[CAST(t.z AS STRING)] AS v FROM t;

--#[paths-sfw-14]
SELECT t.x['y']."y"[CAST(t.z AS STRING)] AS v FROM t;

-- ----------------------------------------
--  Array Navigation with Expressions
-- ----------------------------------------

--#[paths-sfw-15]
SELECT t.x[0 + 1] AS v FROM t;

--#[paths-sfw-16]
SELECT t.x[ABS(1)] AS v FROM t;
