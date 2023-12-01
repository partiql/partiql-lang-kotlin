-- start query 83 in stream 0 using template query83.tpl 
WITH sr_items 
     AS (SELECT i_item_id               item_id, 
                Sum(sr_return_quantity) sr_item_qty 
         FROM   store_returns, 
                item, 
                date_dim 
         WHERE  sr_item_sk = i_item_sk 
                AND d_date IN (SELECT d_date 
                               FROM   date_dim 
                               WHERE  d_week_seq IN (SELECT d_week_seq 
                                                     FROM   date_dim 
                                                     WHERE 
                                      d_date IN ( '1999-06-30', 
                                                  '1999-08-28', 
                                                  '1999-11-18' 
                                                ))) 
                AND sr_returned_date_sk = d_date_sk 
         GROUP  BY i_item_id), 
     cr_items 
     AS (SELECT i_item_id               item_id, 
                Sum(cr_return_quantity) cr_item_qty 
         FROM   catalog_returns, 
                item, 
                date_dim 
         WHERE  cr_item_sk = i_item_sk 
                AND d_date IN (SELECT d_date 
                               FROM   date_dim 
                               WHERE  d_week_seq IN (SELECT d_week_seq 
                                                     FROM   date_dim 
                                                     WHERE 
                                      d_date IN ( '1999-06-30', 
                                                  '1999-08-28', 
                                                  '1999-11-18' 
                                                ))) 
                AND cr_returned_date_sk = d_date_sk 
         GROUP  BY i_item_id), 
     wr_items 
     AS (SELECT i_item_id               item_id, 
                Sum(wr_return_quantity) wr_item_qty 
         FROM   web_returns, 
                item, 
                date_dim 
         WHERE  wr_item_sk = i_item_sk 
                AND d_date IN (SELECT d_date 
                               FROM   date_dim 
                               WHERE  d_week_seq IN (SELECT d_week_seq 
                                                     FROM   date_dim 
                                                     WHERE 
                                      d_date IN ( '1999-06-30', 
                                                  '1999-08-28', 
                                                  '1999-11-18' 
                                                ))) 
                AND wr_returned_date_sk = d_date_sk 
         GROUP  BY i_item_id) 
SELECT sr_items.item_id, 
               sr_item_qty, 
               sr_item_qty / ( sr_item_qty + cr_item_qty + wr_item_qty ) / 3.0 * 
               100 sr_dev, 
               cr_item_qty, 
               cr_item_qty / ( sr_item_qty + cr_item_qty + wr_item_qty ) / 3.0 * 
               100 cr_dev, 
               wr_item_qty, 
               wr_item_qty / ( sr_item_qty + cr_item_qty + wr_item_qty ) / 3.0 * 
               100 wr_dev, 
               ( sr_item_qty + cr_item_qty + wr_item_qty ) / 3.0 
               average 
FROM   sr_items, 
       cr_items, 
       wr_items 
WHERE  sr_items.item_id = cr_items.item_id 
       AND sr_items.item_id = wr_items.item_id 
ORDER  BY sr_items.item_id, 
          sr_item_qty
LIMIT 100; 
