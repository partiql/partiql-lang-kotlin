SELECT t.id AS id, 
       x AS even
FROM matrices AS t, 
     t.matrix AS y,
     y AS x
WHERE x % 2 = 0
