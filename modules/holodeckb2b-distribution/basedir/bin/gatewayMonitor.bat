@echo off

rem ---------------------------------------------------------------------------
rem Startup script for the local Holobeck B2B command line monitoring tool
rem
rem Environment Variable 
rem
rem   HB2B_HOME      MAY point at the Holodeck B2B home directory
rem
rem   JAVA_HOME       MUST point at your Java Development Kit installation.
rem ---------------------------------------------------------------------------

if "%OS%"=="Windows_NT" @setlocal
if "%OS%"=="WINNT" @setlocal

rem %~dp0 is expanded pathname of the current script under NT
set DEFAULT_HB2B_HOME=%~dp0..

if "%HB2B_HOME%"=="" set HB2B_HOME=%DEFAULT_HB2B_HOME%
set DEFAULT_HB2B_HOME=

rem find HB2B_HOME if it does not exist due to either an invalid value passed
rem by the user or the %0 problem on Windows 9x
:checkConf
if exist "%HB2B_HOME%\conf\axis2.xml" goto checkJava

:noHB2BHome
echo HB2B_HOME environment variable is set incorrectly or Holodeck B2B could not be located. 
echo Please set the HB2B_HOME variable appropriately
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
set HB2B_CLASSPATH=%HB2B_HOME%;%HB2B_HOME%\conf;%JAVA_HOME%\lib\tools.jar;%HB2B_HOME%\lib\*

"%_JAVACMD%" %JAVA_OPTS% -cp "!HB2B_CLASSPATH!" org.holodeckb2b.ui.app.cli.HB2BInfoTool %*
goto end

:end
set _JAVACMD=

if "%OS%"=="Windows_NT" @endlocal
if "%OS%"=="WINNT" @endlocal

:mainEnd
