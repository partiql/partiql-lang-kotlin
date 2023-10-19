-- start query 18 in stream 0 using template query18.tpl 
SELECT i_item_id, 
               ca_country, 
               ca_state, 
               ca_county, 
               Avg(Cast(cs_quantity AS NUMERIC(12, 2)))      agg1, 
               Avg(Cast(cs_list_price AS NUMERIC(12, 2)))    agg2, 
               Avg(Cast(cs_coupon_amt AS NUMERIC(12, 2)))    agg3, 
               Avg(Cast(cs_sales_price AS NUMERIC(12, 2)))   agg4, 
               Avg(Cast(cs_net_profit AS NUMERIC(12, 2)))    agg5, 
               Avg(Cast(c_birth_year AS NUMERIC(12, 2)))     agg6, 
               Avg(Cast(cd1.cd_dep_count AS NUMERIC(12, 2))) agg7 
FROM   catalog_sales, 
       customer_demographics cd1, 
       customer_demographics cd2, 
       customer, 
       customer_address, 
       date_dim, 
       item 
WHERE  cs_sold_date_sk = d_date_sk 
       AND cs_item_sk = i_item_sk 
       AND cs_bill_cdemo_sk = cd1.cd_demo_sk 
       AND cs_bill_customer_sk = c_customer_sk 
       AND cd1.cd_gender = 'F' 
       AND cd1.cd_education_status = 'Secondary' 
       AND c_current_cdemo_sk = cd2.cd_demo_sk 
       AND c_current_addr_sk = ca_address_sk 
       AND c_birth_month IN ( 8, 4, 2, 5, 
                              11, 9 ) 
       AND d_year = 2001 
       AND ca_state IN ( 'KS', 'IA', 'AL', 'UT', 
                         'VA', 'NC', 'TX' ) 
GROUP  BY rollup ( i_item_id, ca_country, ca_state, ca_county ) 
ORDER  BY ca_country, 
          ca_state, 
          ca_county, 
          i_item_id
LIMIT 100; 
