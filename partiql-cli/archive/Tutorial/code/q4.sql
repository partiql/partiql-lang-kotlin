SELECT e.name AS employeeName, 
       COUNT(p.name) AS queryProjectsNum
FROM hr.employeesNest e LEFT JOIN e.projects AS p ON p.name LIKE '%querying%'
GROUP BY e.id, e.name
