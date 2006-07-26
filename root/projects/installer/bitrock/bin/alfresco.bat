@echo off
rem ---------------------------------------------------------------------------
rem Start script for the Alfresco Server
rem ---------------------------------------------------------------------------

set CATALINA_HOME=@@BITROCK_ALFRESCO_INSTALLDIR@@\tomcat

rem Set any default JVM options
set JAVA_HOME=@@BITROCK_ALFRESCO_INSTALLDIR@@\java
set JAVA_OPTS=-Xms128m -Xmx512m -Xss64k -server -XX:CompileCommand=exclude,org/apache/lucene/index/IndexReader$1,doBody

set PATH=%JAVA_HOME%\bin;@@BITROCK_ALFRESCO_INSTALLDIR@@\bin;%CATALINA_HOME%\bin;%PATH%
rem ---------------------------------------
rem Start Components
rem ---------------------------------------

if not ""%1"" == ""start"" goto nostart

rem ---------------------------------------
rem Start Tomcat
rem ---------------------------------------

echo Starting Tomcat...
call "%CATALINA_HOME%\bin\startup.bat"

rem ---------------------------------------
rem Start OpenOffice for transformations
rem ---------------------------------------
if exist "start_oo.bat" call "start_oo.bat"

goto nostop
:nostart

rem ---------------------------------------
rem Stop Components
rem ---------------------------------------

if not ""%1"" == ""stop"" goto nostop

echo Shutting down Tomcat...
call "%CATALINA_HOME%\bin\shutdown.bat" 

rem ---------------------------------------
rem Stop OpenOffice for transformations
rem ---------------------------------------
call "stop_oo.bat"
:nostop