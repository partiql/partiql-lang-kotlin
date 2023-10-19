--#[join-01]
SELECT * FROM <<{ 'a': 1 }>> AS t1, <<{ 'b': 2.0 }>> AS t2;

--#[join-02]
SELECT * FROM <<{ 'a': 1 }>> AS t1 LEFT JOIN <<{ 'b': 2.0 }>> AS t2 ON TRUE;

--#[join-03]
SELECT b, a FROM <<{ 'a': 1 }>> AS t1 LEFT JOIN <<{ 'b': 2.0 }>> AS t2 ON TRUE;

--#[join-04]
SELECT t1.a, t2.a FROM <<{ 'a': 1 }>> AS t1 LEFT JOIN <<{ 'a': 2.0 }>> AS t2 ON t1.a = t2.a;

--#[join-05]
SELECT * FROM <<{ 'a': 1 }>> AS t1 LEFT JOIN <<{ 'a': 2.0 }>> AS t2 ON t1.a = t2.a;

--#[join-06]
SELECT * FROM
    <<{ 'a': 1 }>> AS t1
    LEFT JOIN
    <<{ 'a': 2.0 }>> AS t2
    ON t1.a = t2.a
    LEFT JOIN
    <<{ 'a': 'hello, world' }>> AS t3
    ON t3.a = 'hello';
