SELECT e.name AS employeeName, 
       p AS projectName
FROM hr.employeesNestScalars AS e, 
     e.projects AS p
WHERE p LIKE '%security%'
