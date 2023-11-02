--#[subquery-00]
SELECT x
FROM T
WHERE x IN (SELECT a FROM S);

--#[subquery-01]
SELECT x
FROM T
WHERE x > (SELECT MAX(a) FROM S);

--#[subquery-02]
SELECT t.*, s.*
FROM T AS t
         JOIN (SELECT * FROM S) AS s
              ON t.x = s.a;
