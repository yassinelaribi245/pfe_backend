@echo off
echo Starting Git operations...
echo.

echo 1. Adding all files...
git add . > git_add_output.txt 2>&1
type git_add_output.txt
echo.

echo 2. Committing files...
git commit -m "Add Spring Boot backend API project with all source files" > git_commit_output.txt 2>&1
type git_commit_output.txt
echo.

echo 3. Pushing to GitHub...
git push -u origin main > git_push_output.txt 2>&1
type git_push_output.txt
echo.

echo Operations completed. Check the output files above.
pause
