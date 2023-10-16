-- start query 79 in stream 0 using template query79.tpl 
SELECT c_last_name, 
               c_first_name, 
               Substr(s_city, 1, 30), 
               ss_ticket_number, 
               amt, 
               profit 
FROM   (SELECT ss_ticket_number, 
               ss_customer_sk, 
               store.s_city, 
               Sum(ss_coupon_amt) amt, 
               Sum(ss_net_profit) profit 
        FROM   store_sales, 
               date_dim, 
               store, 
               household_demographics 
        WHERE  store_sales.ss_sold_date_sk = date_dim.d_date_sk 
               AND store_sales.ss_store_sk = store.s_store_sk 
               AND store_sales.ss_hdemo_sk = household_demographics.hd_demo_sk 
               AND ( household_demographics.hd_dep_count = 8 
                      OR household_demographics.hd_vehicle_count > 4 ) 
               AND date_dim.d_dow = 1 
               AND date_dim.d_year IN ( 2000, 2000 + 1, 2000 + 2 ) 
               AND store.s_number_employees BETWEEN 200 AND 295 
        GROUP  BY ss_ticket_number, 
                  ss_customer_sk, 
                  ss_addr_sk, 
                  store.s_city) ms, 
       customer 
WHERE  ss_customer_sk = c_customer_sk 
ORDER  BY c_last_name, 
          c_first_name, 
          Substr(s_city, 1, 30), 
          profit
LIMIT 100; 
