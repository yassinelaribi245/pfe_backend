@echo off
echo Setting up Git configuration...
git config user.name "yassinelaribi245"
git config user.email "yassinelaribi245@users.noreply.github.com"
echo.

echo Adding all files...
git add .
echo.

echo Committing...
git commit -m "Initial commit: Add Spring Boot backend API project"
echo.

echo Pushing to GitHub...
git push -u origin main
echo.

echo Done! Check if there were any errors above.
pause
