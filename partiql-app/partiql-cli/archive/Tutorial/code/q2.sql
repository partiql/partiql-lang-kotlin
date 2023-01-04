SELECT e.name AS employeeName, 
       p.name AS projectName
FROM hr.employeesNest AS e, 
     e.projects AS p
WHERE p.name LIKE '%security%'
