@echo off

rem ---------------------------------------------------------------------------
rem Startup script for the local Holobeck B2B command line monitoring tool
rem
rem Environment Variable 
rem
rem   AXIS2_HOME      MAY point at the Holodeck B2B home directory
rem
rem   JAVA_HOME       MUST point at your Java Development Kit installation.
rem ---------------------------------------------------------------------------

if "%OS%"=="Windows_NT" @setlocal
if "%OS%"=="WINNT" @setlocal

rem %~dp0 is expanded pathname of the current script under NT
set DEFAULT_AXIS2_HOME=%~dp0..

if "%AXIS2_HOME%"=="" set AXIS2_HOME=%DEFAULT_AXIS2_HOME%
set DEFAULT_AXIS2_HOME=

rem find AXIS2_HOME if it does not exist due to either an invalid value passed
rem by the user or the %0 problem on Windows 9x
:checkConf
if exist "%AXIS2_HOME%\conf\axis2.xml" goto checkJava

:noAxis2Home
echo AXIS2_HOME environment variable is set incorrectly or AXIS2 could not be located. 
echo Please set the AXIS2_HOME variable appropriately
goto end

:checkJava
set _JAVACMD=%JAVACMD%

if "%JAVA_HOME%" == "" goto noJavaHome
if not exist "%JAVA_HOME%\bin\java.exe" goto noJavaHome
if "%_JAVACMD%" == "" set _JAVACMD=%JAVA_HOME%\bin\java.exe

goto runApp

:noJavaHome
if "%_JAVACMD%" == "" set _JAVACMD=java.exe
echo JAVA_HOME environment variable is set incorrectly or Java runtime could not be located.
echo Please set the JAVA_HOME variable appropriately
goto end

:runApp
rem set the classes by looping through the libs
setlocal EnableDelayedExpansion
set AXIS2_CLASS_PATH=%AXIS2_HOME%;%AXIS2_HOME%\conf;%JAVA_HOME%\lib\tools.jar;%AXIS2_HOME%\lib\*

"%_JAVACMD%" %JAVA_OPTS% -cp "!AXIS2_CLASS_PATH!" org.holodeckb2b.ui.app.cli.HB2BInfoTool %*
goto end

:end
set _JAVACMD=
set AXIS2_CMD_LINE_ARGS=

if "%OS%"=="Windows_NT" @endlocal
if "%OS%"=="WINNT" @endlocal

:mainEnd
