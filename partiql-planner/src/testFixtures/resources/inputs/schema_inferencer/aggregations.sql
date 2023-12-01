--#[aggs-00]
SELECT COUNT(*) FROM T;

--#[aggs-01]
SELECT COUNT(*), COUNT(1), MIN(b), MAX(b), AVG(b) FROM T;

--#[aggs-02]
SELECT COUNT(*) AS count_star FROM T;

--#[aggs-03]
SELECT COUNT(*) AS count_star,
       COUNT(b) AS count_b,
       MIN(b) AS min_b,
       MAX(b) AS max_b,
       AVG(b) AS avg_b
FROM T;

--#[aggs-04]
SELECT a, COUNT(*) FROM T GROUP BY a;

--#[aggs-05]
SELECT COUNT(*), a FROM T GROUP BY a;

--#[aggs-06]
SELECT a, b, c, MIN(b), MAX(b) FROM T GROUP BY a, b, c;

--#[aggs-07]
SELECT MIN(b), MAX(b), a, b, c FROM T GROUP BY a, b, c;

--#[aggs-08]
SELECT a AS _a, COUNT(*) AS count_star FROM T GROUP BY a;

--#[aggs-09]
SELECT COUNT(*) AS count_star, a AS _a FROM T GROUP BY a;

--#[aggs-10]
SELECT a AS _a, b AS _b, c AS _c, MIN(b) AS min_b, MAX(b) AS max_b FROM T GROUP BY a, b, c;

--#[aggs-11]
SELECT MIN(b) AS min_b, MAX(b) AS max_b, a AS _a, b AS _b, c AS _c FROM T GROUP BY a, b, c;

--#[aggs-12]
SELECT a AS _a, AVG(b) AS avg_b FROM T
GROUP BY a
HAVING a = true;

--#[aggs-13]
SELECT a AS _a, AVG(b) AS avg_b FROM T
GROUP BY a
HAVING avg_b > 0;
