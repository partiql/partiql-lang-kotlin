-- start query 78 in stream 0 using template query78.tpl 
WITH ws 
     AS (SELECT d_year                 AS ws_sold_year, 
                ws_item_sk, 
                ws_bill_customer_sk    ws_customer_sk, 
                Sum(ws_quantity)       ws_qty, 
                Sum(ws_wholesale_cost) ws_wc, 
                Sum(ws_sales_price)    ws_sp 
         FROM   web_sales 
                LEFT JOIN web_returns 
                       ON wr_order_number = ws_order_number 
                          AND ws_item_sk = wr_item_sk 
                JOIN date_dim 
                  ON ws_sold_date_sk = d_date_sk 
         WHERE  wr_order_number IS NULL 
         GROUP  BY d_year, 
                   ws_item_sk, 
                   ws_bill_customer_sk), 
     cs 
     AS (SELECT d_year                 AS cs_sold_year, 
                cs_item_sk, 
                cs_bill_customer_sk    cs_customer_sk, 
                Sum(cs_quantity)       cs_qty, 
                Sum(cs_wholesale_cost) cs_wc, 
                Sum(cs_sales_price)    cs_sp 
         FROM   catalog_sales 
                LEFT JOIN catalog_returns 
                       ON cr_order_number = cs_order_number 
                          AND cs_item_sk = cr_item_sk 
                JOIN date_dim 
                  ON cs_sold_date_sk = d_date_sk 
         WHERE  cr_order_number IS NULL 
         GROUP  BY d_year, 
                   cs_item_sk, 
                   cs_bill_customer_sk), 
     ss 
     AS (SELECT d_year                 AS ss_sold_year, 
                ss_item_sk, 
                ss_customer_sk, 
                Sum(ss_quantity)       ss_qty, 
                Sum(ss_wholesale_cost) ss_wc, 
                Sum(ss_sales_price)    ss_sp 
         FROM   store_sales 
                LEFT JOIN store_returns 
                       ON sr_ticket_number = ss_ticket_number 
                          AND ss_item_sk = sr_item_sk 
                JOIN date_dim 
                  ON ss_sold_date_sk = d_date_sk 
         WHERE  sr_ticket_number IS NULL 
         GROUP  BY d_year, 
                   ss_item_sk, 
                   ss_customer_sk) 
SELECT ss_item_sk, 
               Round(ss_qty / ( COALESCE(ws_qty + cs_qty, 1) ), 2) ratio, 
               ss_qty                                              store_qty, 
               ss_wc 
               store_wholesale_cost, 
               ss_sp 
               store_sales_price, 
               COALESCE(ws_qty, 0) + COALESCE(cs_qty, 0) 
               other_chan_qty, 
               COALESCE(ws_wc, 0) + COALESCE(cs_wc, 0) 
               other_chan_wholesale_cost, 
               COALESCE(ws_sp, 0) + COALESCE(cs_sp, 0) 
               other_chan_sales_price 
FROM   ss 
       LEFT JOIN ws 
              ON ( ws_sold_year = ss_sold_year 
                   AND ws_item_sk = ss_item_sk 
                   AND ws_customer_sk = ss_customer_sk ) 
       LEFT JOIN cs 
              ON ( cs_sold_year = ss_sold_year 
                   AND cs_item_sk = cs_item_sk 
                   AND cs_customer_sk = ss_customer_sk ) 
WHERE  COALESCE(ws_qty, 0) > 0 
       AND COALESCE(cs_qty, 0) > 0 
       AND ss_sold_year = 1999 
ORDER  BY ss_item_sk, 
          ss_qty DESC, 
          ss_wc DESC, 
          ss_sp DESC, 
          other_chan_qty, 
          other_chan_wholesale_cost, 
          other_chan_sales_price, 
          Round(ss_qty / ( COALESCE(ws_qty + cs_qty, 1) ), 2)
LIMIT 100; 
