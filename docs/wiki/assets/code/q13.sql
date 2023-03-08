SELECT e.name AS employeeName, 
       p.name AS projectName, 
       o AS projectPriority
FROM hr.employeesNest AS e, 
     e.projects AS p AT o
WHERE p.name LIKE '%security%'
