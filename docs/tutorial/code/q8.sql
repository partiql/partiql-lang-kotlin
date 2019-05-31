SELECT e.id, 
       e.name AS employeeName, 
       e.title AS title
FROM hr.employeeWithMissing AS e
WHERE e.title = 'Dev Mgr'

