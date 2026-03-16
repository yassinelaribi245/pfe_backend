@echo off
echo Initializing git repository...
git init
echo.
echo Adding remote origin...
git remote add origin https://github.com/yassinelaribi245/pfe_backend.git
echo.
echo Adding all files...
git add .
echo.
echo Committing changes...
git commit -m "Initial commit: Add Spring Boot backend API project"
echo.
echo Setting main branch...
git branch -M main
echo.
echo Pushing to GitHub...
git push -u origin main
echo.
echo Done! Check the output above for any errors.
pause
