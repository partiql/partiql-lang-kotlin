# PartiQL Window Function Doc

Window function:
PartiQL currently supports SQL’s window functions with in-line window specification, i.e. `f(x) OVER( [window specification])`.  Note that frame clause is not currently supported ( but will be in the future).

Window function are special functions that compute aggregated values over a window of input binding tuples. Like a general function, a window function returns a value for every binding tuple in the binding collection. At a very high level, window functions behaves as if they can access binding tuples other than the current one in the binding collection.

A window function call is identified by an OVER clause, which can optionally contains three sub-clauses `PARTITION BY`, `ORDER BY`. The `PARTITION BY` sub-clause acts similar to `GROUP BY`, which splits the input data into partitions based on partition keys. For example, a set of cars may be partitioned by makers, a set of stock may be partitioned by ticker. 

The window function can only access the binding tuples within the same partition, and if there is no partition by, all the binding tuples are within one partition. The `ORDER BY` sub-clauses uses the same logic as a normal `ORDER BY` clause, but it determine only the ordering of binding tuples within each partition, and may not necessarily has effects on the final order of the results.

## Support Window Functions:
- [Lag](#lag-function)
- [Lead](#lead-function)

### Lag Function:
Syntax:
```
LAG(expr [, offset [, default]] ) 
    OVER ( [window partition clause] window order by clause ) 
```

Purpose:
Returns the value from a binding tuple at a given offset *prior to* the current binding tuple position in the window partition.

Arguments:
* expr: 
  * The expression to be evaluated based on specific offset. `expr` can be an expression of any type (literal, attribute name, path expression, or subquery) but another window function expression.
* offset: 
  * The number of “rows” back from the current binding tuple from which the `expr` should be evaluated upon. If `offset` is not specified, the default value is 1. `offset` can be an expression of any type but window function. If `offset` is not a constant value, it should be evaluated upon the current binding tuple instead of the offset binding tuple. `offset` should be evaluated to a non-negative integer.
* default: 
  * The value to return when `offset` is out of the scope of the partition. If  `default` is not specified, the default value is `NULL`.

Window specification:

* PARTITION BY sub-clause is optional.
* ORDER BY sub-clause has to be specified in order to use `LAG` function.

Example:

Consider our dataset is :

```
stock_price : <<
    { date: 2022-09-30, ticker: AMZN, price: 113.00}
    { date: 2022-10-03, ticker: AMZN, price: 115.88}
    { date: 2022-10-04, ticker: AMZN, price: 121.09}
    { date: 2022-09-30, ticker: GOOG, price: 96.15}
    { date: 2022-10-03, ticker: GOOG, price: 99.30}
    { date: 2022-10-04, ticker: GOOG, price: 101.04}
    >>
```

1. For each ticker, find the price for each day and the previous day

Consider how we would achieve this query without `LAG` function:
```
SELECT 
    all_record."date",
    all_record.ticker,
    all_record.price AS curr_price, 
    (CASE WHEN non_null_prev.prev_price IS MISSING THEN NULL 
          ELSE non_null_prev.prev_price END) AS prev_price 
FROM stock_price AS all_record 
    LEFT JOIN 
        (SELECT curr."date", 
                curr.ticker, 
                curr.price, 
                prev.price AS prev_price 
         FROM stock_price AS curr 
             JOIN stock_price AS prev 
             ON curr.ticker = prev.ticker 
                AND prev."date" < curr."date" 
                AND NOT EXISTS( 
                    SELECT * FROM stock_price AS inter 
                        WHERE inter.ticker = curr.ticker
                        AND inter."date" < curr."date" 
                        AND inter."date" > prev."date")
         ) AS non_null_prev 
         ON all_record."date" = non_null_prev."date" 
         AND all_record.ticker = non_null_prev.ticker
```

With lag function, the query can be simplified as
```
SELECT sp.date as date, 
       sp.ticker as ticker, 
       sp.price as current_price,
       lag(sp.price) OVER (PARTITION BY sp.ticker ORDER BY sp.date) as previous_price
    FROM stock_price as sp
```

The result is
```
<< 
   { date: 2022-09-30, ticker: AMZN, current_price: 113.00, previous_price: NULL}
   { date: 2022-10-03, ticker: AMZN, current_price: 115.88, previous_price: 113.00}
   { date: 2022-10-04, ticker: AMZN, current_price: 121.09, previous_price: 115.88}
   { date: 2022-09-30, ticker: GOOG, current_price: 96.15, previous_price: NULL}
   { date: 2022-10-03, ticker: GOOG, current_price: 99.30, previous_price: 96.15}
   { date: 2022-10-04, ticker: GOOG, current_price: 101.04, previous_price: 99.30}
>>     
```

2. Use of aggregation function:
```
SELECT 
    month as current_month, 
    ticker as ticker, 
    avg(price) as current_month_average,
    lag(avg(price)) OVER (PARTITION BY ticker ORDER BY month) as previous_month_avg
    FROM stock_price as sp 
    GROUP BY EXTRACT(MONTH FROM sp.date) as month, sp.ticker as ticker GROUP AS g
```

```
<< 
   { current_month: 9, ticker: AMZN, current_month_average: 113.00, previous_month_avg: NULL}
   { current_month: 10, ticker: AMZN, current_month_average: 118.49, previous_month_avg: 113.00}
   { current_month: 9, ticker: GOOG, current_month_average: 96.15, previous_month_avg: NULL}
   { current_month: 10, ticker: GOOG, current_month_average: 100.17, previous_month_avg: 96.15}
>> 
```

3. Outer Order By:
```
SELECT sp."date" as "date",
       sp.ticker as ticker,
       sp.price as current_price,
       lag(sp.price) OVER (PARTITION BY sp.ticker ORDER BY sp."date") as previous_price
FROM stock_price as sp
ORDER BY sp."date" DESC
```

```
[
  { 'date': `2022-10-04`, 'ticker': 'AMZN', 'current_price': 121.09, 'previous_price': 115.88},
  { 'date': `2022-10-04`, 'ticker': 'GOOG', 'current_price': 101.04, 'previous_price': 99.30},
  { 'date': `2022-10-03`, 'ticker': 'AMZN', 'current_price': 115.88, 'previous_price': 113.00},
  { 'date': `2022-10-03`, 'ticker': 'GOOG', 'current_price': 99.30, 'previous_price': 96.15},
  { 'date': `2022-09-30`, 'ticker': 'AMZN', 'current_price': 113.00, 'previous_price': NULL},
  { 'date': `2022-09-30`, 'ticker': 'GOOG', 'current_price': 96.15, 'previous_price': NULL}
]
```

Notice here the output is a list, whereas the results in previous query return a bag. This is because the window order by only logically order the partition.

4. Missing Binding:

It is worth to distinguish the `out of window partition` vs `no such binding.`

Consider:
```
SELECT 
       lag(sp.a, 1, 'Out Of Partition') 
            OVER (PARTITION BY sp.ticker ORDER BY sp."date") as previous_a
FROM stock_price as sp
ORDER BY sp."date" DESC
```

The result is :
```
<<
  { 'previous_a': 'Out of Partition' },
  {},
  {}
  { 'previous_a': 'Out of Partition' }, 
  {}
  {}
>>
```

The first row returns `{ 'previous_a': 'Out of Partition' }` because current row is the first row in partition, and `lag(sp.a)` tries to access the row before, which is out of the window partition. Therefore it returns the default value.
The second row returns an empty struct `{}` , this is because the current row is now the second row , and `lag(sp.a)` essentially evaluate `sp.a` over the binding tuple `{ date: 2022-09-30, ticker: AMZN, price: 113.00}`. since there is no binding name `a` in the binding tuple, the query returns `missing`.

### Lead Function:
Syntax:
```
LEAD(expr [, offset [, default]] ) 
    OVER ( [window partition clause] window order by clause ) 
```

Purpose:
Returns the value from a binding tuple at a given offset *after* the current binding tuple position in the window partition.

Arguments:
* expr:
    * The expression to be evaluated based on specific offset. `expr` can be an expression of any type (literal, attribute name, path expression, or subquery) but another window function expression.
* offset:
    * The number of “rows” after the current binding tuple from which the `expr` should be evaluated upon. If `offset` is not specified, the default value is 1. `offset` can be an expression of any type but window function. If `offset` is not a constant value, it should be evaluated upon the current binding tuple instead of the offset binding tuple. `offset` should be evaluated to a non-negative integer.
* default:
    * The value to return when `offset` is out of the scope of the partition. If  `default` is not specified, the default value is `NULL`.

Window specification:

* PARTITION BY sub-clause is optional.
* ORDER BY sub-clause has to be specified in order to use `LEAD` function.

Example:

Consider our dataset is :

```
stock_price : <<
    { date: 2022-09-30, ticker: AMZN, price: 113.00}
    { date: 2022-10-03, ticker: AMZN, price: 115.88}
    { date: 2022-10-04, ticker: AMZN, price: 121.09}
    { date: 2022-09-30, ticker: GOOG, price: 96.15}
    { date: 2022-10-03, ticker: GOOG, price: 99.30}
    { date: 2022-10-04, ticker: GOOG, price: 101.04}
    >>
```

1. For each ticker, find the price for each day and the following day
```
SELECT sp.date as date, 
       sp.ticker as ticker, 
       sp.price as current_price,
       lead(sp.price) OVER (PARTITION BY sp.ticker ORDER BY sp.date) as next_price
    FROM stock_price as sp
```

The result is
```
<< 
   { date: 2022-09-30, ticker: AMZN, current_price: 113.00, next_price: 115.88}
   { date: 2022-10-03, ticker: AMZN, current_price: 115.88, next_price: 121.09}
   { date: 2022-10-04, ticker: AMZN, current_price: 121.09, next_price: NULL}
   { date: 2022-09-30, ticker: GOOG, current_price: 96.15, next_price: 99.30}
   { date: 2022-10-03, ticker: GOOG, current_price: 99.30, next_price: 101.04}
   { date: 2022-10-04, ticker: GOOG, current_price: 101.04, next_price: NULL}
>>     
```

`Lag` and `Lead` performs similar operation and have similar semantics, expect for the fact that `Lag` looks for x rows prior to the current row and `Lead` looks for x rows after. To see more example, you can go to the example section in [Lag function](#lag-function).