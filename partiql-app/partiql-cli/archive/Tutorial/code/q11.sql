SELECT e.name AS employeeName,
       CASE WHEN (p IS TUPLE) THEN p.name 
       ELSE p END AS projectName
FROM hr.employeesMixed2 AS e,
     e.projects AS p
