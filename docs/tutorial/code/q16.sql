SELECT sp.date AS date,
       ( PIVOT dp.price AS dp.symbol FROM datesPrices dp) AS prices
FROM StockPrices sp
GROUP BY sp.date AS datesPrices
