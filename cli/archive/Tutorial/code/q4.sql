SELECT e.name AS employeeName, 
       ( SELECT COUNT(*)
         FROM e.projects AS p
         WHERE p.name LIKE '%querying%'
       ) AS queryProjectsNum
FROM hr.employeesNest AS e
