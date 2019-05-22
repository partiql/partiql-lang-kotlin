@ECHO OFF

SET base_dir=%~dp0

SET java_exec=java
IF DEFINED JAVA_HOME (SET java_exec=%JAVA_HOME%/bin/java)

"%java_exec%" -jar %base_dir%\partiql-cli.jar