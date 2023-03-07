SELECT e.name AS employeeName, 
       e.projects[0].name AS firstProjectName
FROM hr.employeesNest AS e
