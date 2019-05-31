SELECT p AS projectName,
       ( SELECT VALUE v.e.name 
         FROM perProjectGroup AS v 
         ORDER BY v.e.name ) AS employees
FROM hr.employeesNestScalars AS e JOIN e.projects AS p ON p LIKE '%security%'
GROUP BY p GROUP AS perProjectGroup

