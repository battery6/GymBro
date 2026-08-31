@echo off
where mvn >nul 2>nul
if %ERRORLEVEL%==0 (
    mvn %*
    exit /b %ERRORLEVEL%
)
echo Maven was not found on PATH.
echo Install Apache Maven, or open this project in an IDE that bundles Maven,
echo or run the build from WSL / Git Bash using ./mvnw
exit /b 1
