--#[select-00]
SELECT a, b, c FROM T;

--#[select-01]
SELECT * FROM T;

--#[select-02]
SELECT VALUE { 'a': a, 'b': b, 'c': c } FROM T;

--#[select-03]
SELECT VALUE a FROM T;

--#[select-04]
SELECT * FROM T AS t1, T AS t2;

--#[select-05]
SELECT t.d.* FROM T;

--#[select-06]
SELECT t, t.d.* FROM T;

--#[select-07]
SELECT t.d.*, t.d.* FROM T;

--#[select-08]
SELECT d.* FROM T;

--#[select-09]
SELECT t.* FROM T;

--#[select-10]
SELECT t.c || CURRENT_USER FROM T;

--#[select-11]
SELECT CURRENT_USER FROM T;

--#[select-12]
SELECT CURRENT_DATE FROM T;

--#[select-13]
SELECT DATE_DIFF(DAY, CURRENT_DATE, CURRENT_DATE) FROM T;

--#[select-14]
SELECT DATE_ADD(DAY, 5, CURRENT_DATE) FROM T;

--#[select-15]
SELECT DATE_ADD(DAY, -5, CURRENT_DATE) FROM T;

--#[select-16]
SELECT a FROM t;
