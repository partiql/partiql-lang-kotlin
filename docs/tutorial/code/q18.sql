SELECT p AS projectName,
       ( SELECT VALUE v.e.name 
         FROM perProjectGroup AS v ) AS employees
FROM hr.employeesNestScalars AS e JOIN e.projects AS p ON p LIKE '%security%'
GROUP BY p GROUP AS perProjectGroup
