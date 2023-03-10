SELECT e.id, 
       e.name AS employeeName, 
       e.title AS title
FROM hr.employees e
WHERE e.title = 'Dev Mgr'
