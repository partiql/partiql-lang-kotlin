SELECT c.date AS date, 
       symbol AS symbol, 
       price AS price
FROM closingPrices c, 
     UNPIVOT c AS price AT symbol
WHERE NOT symbol = 'date'

