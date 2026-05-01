@echo off
REM ─── Auction House – Run Script (Windows) ────────────────────────────────────
echo ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
echo   AUCTION HOUSE - Build and Run
echo ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
 
where mvn >nul 2>&1
if %errorlevel% neq 0 (
    echo [ERROR] Maven chua cai. Tai tai: https://maven.apache.org/download.cgi
    pause & exit /b 1
)
 
where java >nul 2>&1
if %errorlevel% neq 0 (
    echo [ERROR] Java chua cai. Tai Java 21+ tai: https://adoptium.net
    pause & exit /b 1
)
 
echo [OK] Java va Maven san sang
echo [BUILD] Dang build...
call mvn clean compile -q
 
echo [RUN] Dang khoi dong ung dung...
call mvn javafx:run
 
echo [DONE] Ung dung da tat.
pause