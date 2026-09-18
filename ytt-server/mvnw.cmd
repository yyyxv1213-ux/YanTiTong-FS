@echo off
setlocal
set DIRNAME=%~dp0
if "%DIRNAME:~-1%"=="\" set DIRNAME=%DIRNAME:~0,-1%
set MAVEN_PROJECTBASEDIR=%DIRNAME%

set WRAPPER_JAR=%MAVEN_PROJECTBASEDIR%\.mvn\wrapper\maven-wrapper.jar

if defined JAVA_HOME (
  set JAVACMD=%JAVA_HOME%\bin\java.exe
) else (
  set JAVACMD=java.exe
)

"%JAVACMD%" -classpath "%WRAPPER_JAR%" "-Dmaven.multiModuleProjectDirectory=%MAVEN_PROJECTBASEDIR%" org.apache.maven.wrapper.MavenWrapperMain %*

endlocal
