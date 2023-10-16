-- start query 41 in stream 0 using template query41.tpl 
SELECT Distinct(i_product_name) 
FROM   item i1 
WHERE  i_manufact_id BETWEEN 765 AND 765 + 40 
       AND (SELECT Count(*) AS item_cnt 
            FROM   item 
            WHERE  ( i_manufact = i1.i_manufact 
                     AND ( ( i_category = 'Women' 
                             AND ( i_color = 'dim' 
                                    OR i_color = 'green' ) 
                             AND ( i_units = 'Gross' 
                                    OR i_units = 'Dozen' ) 
                             AND ( i_size = 'economy' 
                                    OR i_size = 'petite' ) ) 
                            OR ( i_category = 'Women' 
                                 AND ( i_color = 'navajo' 
                                        OR i_color = 'aquamarine' ) 
                                 AND ( i_units = 'Case' 
                                        OR i_units = 'Unknown' ) 
                                 AND ( i_size = 'large' 
                                        OR i_size = 'N/A' ) ) 
                            OR ( i_category = 'Men' 
                                 AND ( i_color = 'indian' 
                                        OR i_color = 'dark' ) 
                                 AND ( i_units = 'Oz' 
                                        OR i_units = 'Lb' ) 
                                 AND ( i_size = 'extra large' 
                                        OR i_size = 'small' ) ) 
                            OR ( i_category = 'Men' 
                                 AND ( i_color = 'peach' 
                                        OR i_color = 'purple' ) 
                                 AND ( i_units = 'Tbl' 
                                        OR i_units = 'Bunch' ) 
                                 AND ( i_size = 'economy' 
                                        OR i_size = 'petite' ) ) ) ) 
                    OR ( i_manufact = i1.i_manufact 
                         AND ( ( i_category = 'Women' 
                                 AND ( i_color = 'orchid' 
                                        OR i_color = 'peru' ) 
                                 AND ( i_units = 'Carton' 
                                        OR i_units = 'Cup' ) 
                                 AND ( i_size = 'economy' 
                                        OR i_size = 'petite' ) ) 
                                OR ( i_category = 'Women' 
                                     AND ( i_color = 'violet' 
                                            OR i_color = 'papaya' ) 
                                     AND ( i_units = 'Ounce' 
                                            OR i_units = 'Box' ) 
                                     AND ( i_size = 'large' 
                                            OR i_size = 'N/A' ) ) 
                                OR ( i_category = 'Men' 
                                     AND ( i_color = 'drab' 
                                            OR i_color = 'grey' ) 
                                     AND ( i_units = 'Each' 
                                            OR i_units = 'N/A' ) 
                                     AND ( i_size = 'extra large' 
                                            OR i_size = 'small' ) ) 
                                OR ( i_category = 'Men' 
                                     AND ( i_color = 'chocolate' 
                                            OR i_color = 'antique' ) 
                                     AND ( i_units = 'Dram' 
                                            OR i_units = 'Gram' ) 
                                     AND ( i_size = 'economy' 
                                            OR i_size = 'petite' ) ) ) )) > 0 
ORDER  BY i_product_name
LIMIT 100; 
