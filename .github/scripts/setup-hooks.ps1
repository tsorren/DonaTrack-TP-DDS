$hookSource = ".github/scripts/pre-commit.sh"
$hookDest = ".git/hooks/pre-commit"

if (Test-Path $hookSource) {
    if (!(Test-Path ".git/hooks")) {
        New-Item -ItemType Directory -Path ".git/hooks" | Out-Null
    }
    Copy-Item -Path $hookSource -Destination $hookDest -Force
    Write-Host "Configuracion de hooks completada."
} else {
    Write-Error "No se encontro el archivo de origen en $hookSource."
}