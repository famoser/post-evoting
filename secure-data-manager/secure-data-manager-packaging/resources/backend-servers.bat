@echo off
rem Backend servers startup and shutdown

setlocal

rem Set local environment variables
set "SDM_HOME=%cd%"
set "JAVA_HOME=%SDM_HOME%\${adopt-jre.version}"
set "JRE_HOME=%SDM_HOME%\${adopt-jre.version}"
set "CATALINA_HOME=%SDM_HOME%\apache-tomcat-${apache-tomcat.version}"
set "keystore_location=%SDM_HOME%\sdm\config\keystore"

set "CATALINA_TMPDIR=%SDM_HOME%\sdmServices\temp"
if not exist %CATALINA_TMPDIR% mkdir %CATALINA_TMPDIR%

rem Read and extract rest server port from sdm properties
set "SDM_PROPERTIES_FILE=%SDM_HOME%\sdm\sdmConfig\sdm.properties"
for /F "eol=# delims== tokens=1,*" %%a in (%SDM_PROPERTIES_FILE%) do (
    if "%%a"=="server.port" if NOT "%%b"=="" set SDM_REST_SERVER_PORT=%%b
    if "%%a"=="backend.server.port" if NOT "%%b"=="" set TOMCAT_SERVER_PORT=%%b
    if "%%a"=="backend.server.port.shutdown" if NOT "%%b"=="" set TOMCAT_SHUTDOWN_SERVER_PORT=%%b
)
if "%SDM_REST_SERVER_PORT%" == "" goto errorPort
if "%TOMCAT_SERVER_PORT%" == "" goto errorPort
if "%TOMCAT_SHUTDOWN_SERVER_PORT%" == "" goto errorPort
rem Create SDM Rest service with server port
set "SDM_REST_SERVER_SERVICE=sdm-rest-%SDM_REST_SERVER_PORT%"


rem Get action "startup" or "shutdown"
if ""%1""=="""" goto errorAction
if ""%1""==""startup"" goto startupServers
if ""%1""==""shutdown"" goto shutdownServers

:errorPort
echo Missing port definitions properties in %SDM_PROPERTIES_FILE%
goto :end

:errorAction
echo Missing action argument startup or shutdown
goto :end

:startupServers
rem Set Java Opts for crypto servers in Tomcat container
set "TITLE=SDM WebApp - %TOMCAT_SERVER_PORT%"
set "JAVA_OPTS=-Duser.home=%SDM_HOME% -Dtomcat.port.shutdown=%TOMCAT_SHUTDOWN_SERVER_PORT% -Dtomcat.port.http=%TOMCAT_SERVER_PORT%"
rem Run crypto servers in Tomcat container
set "STARTUP_CATALINA=%CATALINA_HOME%\bin\startup.bat"
call "%STARTUP_CATALINA%" 
rem Run Spring boot rest server
start "%SDM_REST_SERVER_SERVICE%" %JAVA_HOME%\bin\java -Duser.home=%SDM_HOME% -jar %SDM_HOME%\sdm\sdm-ws-rest.jar --server.port=%SDM_REST_SERVER_PORT%
goto end

:shutdownServers
rem Shutdown crypto servers in Tomcat container
set "JAVA_OPTS=-Dtomcat.port.shutdown=%TOMCAT_SHUTDOWN_SERVER_PORT%"
set "SHUTDOWN_CATALINA=%CATALINA_HOME%\bin\shutdown.bat"
call "%SHUTDOWN_CATALINA%"
rem Shutdown Spring boot rest server
taskkill /FI "WindowTitle eq %SDM_REST_SERVER_SERVICE%*" /T /F
goto end

:end