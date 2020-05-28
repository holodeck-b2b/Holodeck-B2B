@echo off

rem ---------------------------------------------------------------------------
rem  Holodeck B2B Server start script
rem
rem Environment Variable Prequisites
rem
rem   H2B_HOME   Home of the Holodeck B2B installation. If not set I will try
rem              to figure it out.
rem
rem   JAVA_HOME  Must point at your Java Runtime Environment
rem ---------------------------------------------------------------------------

if "%OS%"=="Windows_NT" @setlocal
if "%OS%"=="WINNT" @setlocal

rem %~dp0 is expanded pathname of the current script under NT
set DEFAULT_HB2B_HOME=%~dp0..

if "%HB2B_HOME%"=="" set HB2B_HOME=%DEFAULT_HB2B_HOME%
set DEFAULT_HB2B_HOME=

:loop
if ""%1""==""-xdebug"" goto xdebug
if ""%1""==""-security"" goto security
if ""%1""==""-h"" goto help
if ""%1""=="""" goto checkConf

:xdebug
set JAVA_OPTS= %JAVA_OPTS% -Xdebug -Xnoagent -Xrunjdwp:transport=dt_socket,server=y,address=8000
shift
goto loop

:security
set JAVA_OPTS=%JAVA_OPTS% -Djava.security.manager -Djava.security.policy="%HB2B_HOME%\conf\axis2.policy" -Daxis2.home="%HB2B_HOME%"
shift
goto loop

:help
echo  Usage: startServer.bat
   
echo  commands:    
echo   -xdebug    Start Holodeck B2B Server under JPDA debugger
echo   -security  Enable Java 2 security
echo   -h         help
goto end



rem find HB2B_HOME if it does not exist due to either an invalid value passed
rem by the user or the %0 problem on Windows 9x
:checkConf
if exist "%HB2B_HOME%\conf\holodeckb2b.xml" goto checkJava

:noHB2BHome
echo HB2B_HOME environment variable is set incorrectly or Holodeck B2B could not be located. 
echo Please set the HB2B_HOME variable appropriately
goto end

:checkJava
set _JAVACMD=%JAVACMD%

if "%JAVA_HOME%" == "" goto noJavaHome
if not exist "%JAVA_HOME%\bin\java.exe" goto noJavaHome
if "%_JAVACMD%" == "" set _JAVACMD=%JAVA_HOME%\bin\java.exe

goto runHB2B

:noJavaHome
if "%_JAVACMD%" == "" set _JAVACMD=java.exe
echo JAVA_HOME environment variable is set incorrectly or Java runtime could not be located.
echo Please set the JAVA_HOME variable appropriately
goto end

:runHB2B
rem set the classes by looping through the libs
setlocal EnableDelayedExpansion
set HB2B_CLASS_PATH=%HB2B_HOME%;%HB2B_HOME%\conf;%JAVA_HOME%\lib\tools.jar;%HB2B_HOME%\lib\*

echo Using JAVA_HOME    %JAVA_HOME%
echo Using HB2B_HOME    %HB2B_HOME%

cd "%HB2B_HOME%"
"%_JAVACMD%" %JAVA_OPTS% -Dfile.encoding=UTF-8 -Dderby.stream.error.file="%HB2B_HOME%\logs\derby.log" -cp "!HB2B_CLASS_PATH!" org.holodeckb2b.core.HolodeckB2BServer -home "%HB2B_HOME%"
goto end

:end
set _JAVACMD=

if "%OS%"=="Windows_NT" @endlocal
if "%OS%"=="WINNT" @endlocal

:mainEnd

