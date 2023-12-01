--#[collections-01]
-- Collection BAG<INT>
<< 1, 2, 3 >>;

--#[collections-02]
-- Collection LIST<INT>
[ 1, 2, 3 ];

--#[collections-03]
-- Collection LIST<INT>
( 1, 2, 3 );

--#[collections-04]
-- Collection SEXP<INT>
SEXP ( 1, 2, 3 );

--#[collections-05]
--SELECT VALUE from array
SELECT VALUE x FROM [ 1, 2, 3 ] as x;

--#[collections-06]
--SELECT from array
SELECT x FROM [ 1, 2, 3 ] as x;
