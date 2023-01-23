SELECT e.id AS id, 
       e.name AS employeeName, 
       e.title AS title, 
       p.name AS projectName
FROM hr.employeesNest AS e LEFT JOIN e.projects AS p ON true
