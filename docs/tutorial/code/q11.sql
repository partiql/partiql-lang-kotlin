SELECT e.name AS employeeName,
       CASE WHEN isTuple(p) THEN p.name 
       ELSE p END AS projectName
FROM hr.employeesMixed2 AS e,
     e.projects AS p
