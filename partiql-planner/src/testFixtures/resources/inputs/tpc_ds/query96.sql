-- start query 96 in stream 0 using template query96.tpl 
SELECT Count(*) 
FROM   store_sales, 
       household_demographics, 
       time_dim, 
       store 
WHERE  ss_sold_time_sk = time_dim.t_time_sk 
       AND ss_hdemo_sk = household_demographics.hd_demo_sk 
       AND ss_store_sk = s_store_sk 
       AND time_dim.t_hour = 15 
       AND time_dim.t_minute >= 30 
       AND household_demographics.hd_dep_count = 7 
       AND store.s_store_name = 'ese' 
ORDER  BY Count(*)
LIMIT 100; 
