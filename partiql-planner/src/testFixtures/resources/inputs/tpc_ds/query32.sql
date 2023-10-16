-- start query 32 in stream 0 using template query32.tpl 
SELECT 
       Sum(cs_ext_discount_amt) AS `excess discount amount`
FROM   catalog_sales , 
       item , 
       date_dim 
WHERE  i_manufact_id = 610 
AND    i_item_sk = cs_item_sk 
AND    d_date BETWEEN '2001-03-04' AND    ( 
              Cast('2001-03-04' AS DATE) + INTERVAL '90' day) 
AND    d_date_sk = cs_sold_date_sk 
AND    cs_ext_discount_amt > 
       ( 
              SELECT 1.3 * avg(cs_ext_discount_amt) 
              FROM   catalog_sales , 
                     date_dim 
              WHERE  cs_item_sk = i_item_sk 
              AND    d_date BETWEEN '2001-03-04' AND    ( 
                            cast('2001-03-04' AS date) + INTERVAL '90' day) 
              AND    d_date_sk = cs_sold_date_sk ) 
LIMIT 100; 

