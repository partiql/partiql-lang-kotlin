SELECT e.id, 
       e.name AS employeeName, 
       e.title AS title
FROM hr.employeesWithMissing AS e
WHERE e.title = 'Dev Mgr'

