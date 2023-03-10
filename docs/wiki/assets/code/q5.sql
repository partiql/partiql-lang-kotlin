SELECT e.name AS employeeName, 
       e.project.name AS projectName
FROM hr.employeesWithTuples e
WHERE e.project.org = 'AWS'
