SELECT e.id AS id, 
       e.name AS name, 
       e.title AS title,
       ( SELECT VALUE p
         FROM e.projects AS p
         WHERE p LIKE '%security%'
       ) AS securityProjects
FROM hr.employeesNestScalar AS e
