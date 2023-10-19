--#[sanity-01]
SELECT ss_ticket_number, ss_quantity, ss_sold_date_sk
FROM store_sales;

--#[sanity-02]
SELECT ss_ticket_number, ss_quantity, ss_sold_date_sk
FROM store_sales
WHERE ss_sold_date_sk > DATE_ADD(DAY, -30, UTCNOW());

--#[sanity-03]
SELECT (ss_wholesale_cost + 10 < ss_list_price) AS x
FROM store_sales;

--#[sanity-04]
SELECT ss_quantity, -- This is a nullable int32
       CASE (ss_quantity) -- This case statement will always return a non-nullable string
           WHEN 0 THEN 'Did not sell anything!'
           WHEN 1 THEN 'Sold a single item!'
           ELSE 'Sold multiple items!'
           END AS ss_quantity_description_1,
       CASE (ss_quantity)
           WHEN 0 THEN 'Hello' -- sometimes STRING
           WHEN 1 THEN 1.0 -- sometimes DECIMAL
           WHEN 2 THEN 2 -- sometimes INT
       -- There isn't an else here, so the output should be nullable as well.
           END AS ss_quantity_description_2,
       CASE (ss_quantity)
           WHEN 0 THEN 'Hello' -- ss_quantity will be cast to an INT for comparison
           WHEN 'not an int32' THEN 'not cast-able' -- cannot be cast!
           ELSE 'fallback'
           -- There is an ELSE here, so the output should NOT be nullable.
           END AS ss_quantity_description_3
FROM store_sales;

--#[sanity-05]
SELECT p.*, e.*
FROM
    main.person AS p
    INNER JOIN
    main.employer AS e
    ON p.employer = e.name;

--#[sanity-06]
SELECT
       p.name.*,
       (p.name."first" || ' ' || p.name."last") AS full_name
FROM main.person AS p;
