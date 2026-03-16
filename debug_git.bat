@echo off
echo Checking git status...
git status
echo.
echo Checking if files exist...
dir /b
echo.
echo Checking git log...
git log --oneline
echo.
echo Checking remote...
git remote -v
echo.
pause
