-- start query 53 in stream 0 using template query53.tpl 
SELECT * 
FROM   (SELECT i_manufact_id, 
               Sum(ss_sales_price)             sum_sales, 
               Avg(Sum(ss_sales_price)) 
                 OVER ( 
                   partition BY i_manufact_id) avg_quarterly_sales 
        FROM   item, 
               store_sales, 
               date_dim, 
               store 
        WHERE  ss_item_sk = i_item_sk 
               AND ss_sold_date_sk = d_date_sk 
               AND ss_store_sk = s_store_sk 
               AND d_month_seq IN ( 1199, 1199 + 1, 1199 + 2, 1199 + 3, 
                                    1199 + 4, 1199 + 5, 1199 + 6, 1199 + 7, 
                                    1199 + 8, 1199 + 9, 1199 + 10, 1199 + 11 ) 
               AND ( ( i_category IN ( 'Books', 'Children', 'Electronics' ) 
                       AND i_class IN ( 'personal', 'portable', 'reference', 
                                        'self-help' ) 
                       AND i_brand IN ( 'scholaramalgamalg #14', 
                                        'scholaramalgamalg #7' 
                                        , 
                                        'exportiunivamalg #9', 
                                                       'scholaramalgamalg #9' ) 
                     ) 
                      OR ( i_category IN ( 'Women', 'Music', 'Men' ) 
                           AND i_class IN ( 'accessories', 'classical', 
                                            'fragrances', 
                                            'pants' ) 
                           AND i_brand IN ( 'amalgimporto #1', 
                                            'edu packscholar #1', 
                                            'exportiimporto #1', 
                                                'importoamalg #1' ) ) ) 
        GROUP  BY i_manufact_id, 
                  d_qoy) tmp1 
WHERE  CASE 
         WHEN avg_quarterly_sales > 0 THEN Abs (sum_sales - avg_quarterly_sales) 
                                           / 
                                           avg_quarterly_sales 
         ELSE NULL 
       END > 0.1 
ORDER  BY avg_quarterly_sales, 
          sum_sales, 
          i_manufact_id
LIMIT 100; 
