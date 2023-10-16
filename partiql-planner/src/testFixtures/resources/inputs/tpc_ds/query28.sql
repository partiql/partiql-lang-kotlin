-- start query 28 in stream 0 using template query28.tpl 
SELECT * 
FROM   (SELECT Avg(ss_list_price)            B1_LP, 
               Count(ss_list_price)          B1_CNT, 
               Count(DISTINCT ss_list_price) B1_CNTD 
        FROM   store_sales 
        WHERE  ss_quantity BETWEEN 0 AND 5 
               AND ( ss_list_price BETWEEN 18 AND 18 + 10 
                      OR ss_coupon_amt BETWEEN 1939 AND 1939 + 1000 
                      OR ss_wholesale_cost BETWEEN 34 AND 34 + 20 )) B1, 
       (SELECT Avg(ss_list_price)            B2_LP, 
               Count(ss_list_price)          B2_CNT, 
               Count(DISTINCT ss_list_price) B2_CNTD 
        FROM   store_sales 
        WHERE  ss_quantity BETWEEN 6 AND 10 
               AND ( ss_list_price BETWEEN 1 AND 1 + 10 
                      OR ss_coupon_amt BETWEEN 35 AND 35 + 1000 
                      OR ss_wholesale_cost BETWEEN 50 AND 50 + 20 )) B2, 
       (SELECT Avg(ss_list_price)            B3_LP, 
               Count(ss_list_price)          B3_CNT, 
               Count(DISTINCT ss_list_price) B3_CNTD 
        FROM   store_sales 
        WHERE  ss_quantity BETWEEN 11 AND 15 
               AND ( ss_list_price BETWEEN 91 AND 91 + 10 
                      OR ss_coupon_amt BETWEEN 1412 AND 1412 + 1000 
                      OR ss_wholesale_cost BETWEEN 17 AND 17 + 20 )) B3, 
       (SELECT Avg(ss_list_price)            B4_LP, 
               Count(ss_list_price)          B4_CNT, 
               Count(DISTINCT ss_list_price) B4_CNTD 
        FROM   store_sales 
        WHERE  ss_quantity BETWEEN 16 AND 20 
               AND ( ss_list_price BETWEEN 9 AND 9 + 10 
                      OR ss_coupon_amt BETWEEN 5270 AND 5270 + 1000 
                      OR ss_wholesale_cost BETWEEN 29 AND 29 + 20 )) B4, 
       (SELECT Avg(ss_list_price)            B5_LP, 
               Count(ss_list_price)          B5_CNT, 
               Count(DISTINCT ss_list_price) B5_CNTD 
        FROM   store_sales 
        WHERE  ss_quantity BETWEEN 21 AND 25 
               AND ( ss_list_price BETWEEN 45 AND 45 + 10 
                      OR ss_coupon_amt BETWEEN 826 AND 826 + 1000 
                      OR ss_wholesale_cost BETWEEN 5 AND 5 + 20 )) B5, 
       (SELECT Avg(ss_list_price)            B6_LP, 
               Count(ss_list_price)          B6_CNT, 
               Count(DISTINCT ss_list_price) B6_CNTD 
        FROM   store_sales 
        WHERE  ss_quantity BETWEEN 26 AND 30 
               AND ( ss_list_price BETWEEN 174 AND 174 + 10 
                      OR ss_coupon_amt BETWEEN 5548 AND 5548 + 1000 
                      OR ss_wholesale_cost BETWEEN 42 AND 42 + 20 )) B6
LIMIT 100; 
