SELECT c."date" AS "date", 
       sym AS "symbol", 
       price AS price
FROM closingPrices AS c, 
     UNPIVOT c AS price AT sym
WHERE NOT sym = 'date'
