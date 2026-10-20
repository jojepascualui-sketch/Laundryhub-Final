@echo off
rem Laundry HUB: build the site with Maven/TeaVM, then upload web-dist to Vercel.
rem Vercel's build machines have no Java, so we upload the already-built files.
rem Usage:  build-and-deploy.cmd          (production)
rem         build-and-deploy.cmd preview  (preview URL, production stays as it is)
setlocal
cd /d "%~dp0"

if not exist config.properties (
  echo [!] config.properties is missing. Copy config.example.properties to config.properties and fill in your keys.
  exit /b 1
)

where vercel >nul 2>nul
if errorlevel 1 (
  echo [!] Vercel CLI not found. Install it with:  npm install -g vercel   then run:  vercel login
  exit /b 1
)

echo === 1/2 Building (tests + TeaVM) ===
call mvnw.cmd clean package
if errorlevel 1 (
  echo [!] Build failed. Fix the first error above and try again.
  exit /b 1
)

echo === 2/2 Uploading web-dist to Vercel ===
cd web-dist
if /i "%~1"=="preview" (
  call vercel deploy
) else (
  call vercel deploy --prod
)
if errorlevel 1 (
  echo [!] Upload failed. Run "vercel login" if you are not signed in.
  exit /b 1
)

echo.
echo Done. First time? Add your https://YOUR-APP.vercel.app address to the Google Maps key
echo and to Supabase (Authentication - URL Configuration). See docs\DEPLOYMENT.md
endlocal
