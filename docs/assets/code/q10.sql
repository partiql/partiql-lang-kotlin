SELECT e.id, 
       e.name AS employeeName, 
       UPPER(e.title) AS outputTitle
FROM hr.employeesWithMissing AS e
