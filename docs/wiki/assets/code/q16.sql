SELECT sp."date" AS "date", 
       (PIVOT dp.sp.price AT dp.sp."symbol" 
        FROM datesPrices as dp ) AS prices
FROM StockPrices AS sp GROUP BY sp."date" GROUP AS datesPrices
