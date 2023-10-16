-- start query 92 in stream 0 using template query92.tpl 
SELECT 
         Sum(ws_ext_discount_amt) AS `Excess Discount Amount`
FROM     web_sales , 
         item , 
         date_dim 
WHERE    i_manufact_id = 718 
AND      i_item_sk = ws_item_sk 
AND      d_date BETWEEN '2002-03-29' AND      ( 
                  Cast('2002-03-29' AS DATE) +  INTERVAL '90' day) 
AND      d_date_sk = ws_sold_date_sk 
AND      ws_ext_discount_amt > 
         ( 
                SELECT 1.3 * avg(ws_ext_discount_amt) 
                FROM   web_sales , 
                       date_dim 
                WHERE  ws_item_sk = i_item_sk 
                AND    d_date BETWEEN '2002-03-29' AND    ( 
                              cast('2002-03-29' AS date) + INTERVAL '90' day) 
                AND    d_date_sk = ws_sold_date_sk ) 
ORDER BY sum(ws_ext_discount_amt) 
LIMIT 100; 

