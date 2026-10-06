@echo off
rem ============================================================
rem  SCARLET CINEMA - chay web tren may (bam dup vao file nay)
rem  Can: JDK 17 tro len va MySQL dang bat (chua co du lieu thi web tu tao)
rem ============================================================
setlocal
chcp 65001 >nul
title Scarlet Cinema
cd /d "%~dp0"

rem --- 1. Tim Java
if defined JAVA_HOME goto co_java
for /d %%d in ("%ProgramFiles%\Java\jdk-*") do set "JAVA_HOME=%%~d"
if defined JAVA_HOME goto co_java
for /d %%d in ("%ProgramFiles%\Eclipse Adoptium\jdk-*") do set "JAVA_HOME=%%~d"
if defined JAVA_HOME goto co_java
where java >nul 2>nul
if not errorlevel 1 goto co_java
echo [LOI] Khong tim thay Java. Hay cai JDK 17 tro len roi chay lai file nay.
pause
exit /b 1
:co_java

rem --- 2. Tim Maven. Chua co thi tai ve thu muc .maven (chi lan dau)
set "MVN=mvn"
where mvn >nul 2>nul
if not errorlevel 1 goto co_maven
set "MVN=%~dp0.maven\apache-maven-3.9.9\bin\mvn.cmd"
if exist "%MVN%" goto co_maven
echo Lan dau chay: dang tai Maven, khoang 9 MB...
if not exist ".maven" mkdir ".maven"
curl.exe -fsSL -o ".maven\maven.zip" "https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.9/apache-maven-3.9.9-bin.zip"
if errorlevel 1 goto loi_tai
tar.exe -xf ".maven\maven.zip" -C ".maven"
if exist "%MVN%" goto co_maven
:loi_tai
echo [LOI] Khong tai duoc Maven. Kiem tra mang roi chay lai file nay.
pause
exit /b 1
:co_maven

echo.
echo Dang build va chay web. Lan dau se tai thu vien Spring Boot ve may, mat vai phut.
echo Khi thay dong "Started ScarletCinemaApplication", mo trinh duyet vao: http://localhost:8080
echo Muon tat web: bam Ctrl + C trong cua so nay, hoac dong cua so.
echo.
call "%MVN%" -B -ntp spring-boot:run
echo.
echo Web da dung.
pause
