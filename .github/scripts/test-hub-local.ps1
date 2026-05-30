# Nos aseguramos de estar en la raíz del proyecto
Set-Location (Resolve-Path "$PSScriptRoot\..\..").Path

Write-Host "1. Limpiando y preparando directorio 'staging'..." -ForegroundColor Cyan
if (Test-Path staging) { Remove-Item -Recurse -Force staging }
New-Item -ItemType Directory -Force -Path staging/docs/entregas | Out-Null
New-Item -ItemType Directory -Force -Path staging/documentador | Out-Null
New-Item -ItemType Directory -Force -Path staging/adr-preview | Out-Null

Write-Host "2. Construyendo Log4brains (ADR Preview)..." -ForegroundColor Cyan
npx log4brains build --basePath /adr-preview
Move-Item -Force .log4brains/out/* staging/adr-preview/

Write-Host "3. Copiando Documentador..." -ForegroundColor Cyan
if (Test-Path docs/documentador) {
    Copy-Item -Recurse -Force docs/documentador/* staging/documentador/
}

Write-Host "4. Copiando archivos del Hub..." -ForegroundColor Cyan
Copy-Item -Recurse -Force docs/hub/* staging/
New-Item -ItemType File -Force -Path staging/.nojekyll | Out-Null

Write-Host "5. Copiando PDFs de entregas..." -ForegroundColor Cyan
if (Test-Path docs/entregas) {
    Copy-Item -Recurse -Force docs/entregas/* staging/docs/entregas/
}

Write-Host "6. Generando árbol JSON de entregas..." -ForegroundColor Cyan
node .github/scripts/generate-pdf-tree.js

Write-Host "7. Iniciando servidor local (usa Ctrl+C para detener)..." -ForegroundColor Green
Write-Host "Si te pide instalar 'serve', presiona 'y'" -ForegroundColor Yellow
npx serve staging