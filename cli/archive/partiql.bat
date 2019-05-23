@ECHO OFF

SET base_dir=%~dp0

IF DEFINED JAVA_HOME (SET java_exec=%JAVA_HOME%\bin\java)
ELSE ( 
    WHERE java >nul 2>nul         
    IF %ERRORLEVEL% NEQ 0 (
        echo Java not found! Please install the Java JDK https://openjdk.java.net/
        PAUSE
        exit \b 
    )
    SET java_exec=java    
)

"%java_exec%" -jar "%base_dir%\partiql-cli.jar"