<#
.SYNOPSIS
    Extrae contexto estructurado y token-frugal de un Pull Request o rama para la skill review-pr.
.DESCRIPTION
    Obtiene metadatos (titulo, descripcion, ramas), estadisticas de diff y clasificacion
    de archivos impactados por microservicio y criticidad sin sobrecargar la ventana de contexto.
.PARAMETER PR
    Numero de Pull Request en GitHub (ej: 42 o #42).
.PARAMETER Branch
    Nombre de la rama a comparar contra Base (ej: feature/nueva-mision).
.PARAMETER Base
    Rama base para la comparacion (por defecto: autodetectada o 'origin/main').
.PARAMETER Current
    Compara los cambios de la rama o working tree actual contra la rama base.
.PARAMETER Json
    Retorna el resultado en formato JSON estructurado.
#>

[CmdletBinding()]
param(
    [string]$PR = "",
    [string]$Branch = "",
    [string]$Base = "",
    [switch]$Current,
    [switch]$Json
)

$ErrorActionPreference = "Continue"

function Categorize-File([string]$filePath) {
    $normalized = $filePath.Trim().Replace("\", "/")
    $category = "Other"
    $service = "Repository Root"

    if ($normalized -match "^common-lib/") { $service = "common-lib (Shared Kernel)" }
    elseif ($normalized -match "^donaciones-service/") { $service = "donaciones-service" }
    elseif ($normalized -match "^logistica-service/") { $service = "logistica-service" }
    elseif ($normalized -match "^incentivos-service/") { $service = "incentivos-service" }
    elseif ($normalized -match "^notificaciones-service/") { $service = "notificaciones-service" }
    elseif ($normalized -match "^integration-tests/") { $service = "integration-tests" }
    elseif ($normalized -match "^docs/") { $service = "Documentation (docs/)" }
    elseif ($normalized -match "^scripts/") { $service = "Tooling & CI (scripts/)" }
    elseif ($normalized -match "^\.agents/") { $service = "Agents & Skills (.agents/)" }

    if ($normalized -match "Controller\.java$") { $category = "Controller (REST Adapter)" }
    elseif ($normalized -match "(DTO|Request|Response)\.java$") { $category = "Contract (DTO)" }
    elseif ($normalized -match "(Event|Payload)\.java$") { $category = "Contract (Event AMQP)" }
    elseif ($normalized -match "docs/arquitectura/contratos/") { $category = "Contract (OpenAPI/Schema)" }
    elseif ($normalized -match "(Entity|Model)\.java$") { $category = "Domain Entity" }
    elseif ($normalized -match "(Test|IT)\.java$") { $category = "Test Suite" }
    elseif ($normalized -match "(pom\.xml|Dockerfile|docker-compose\.yml)") { $category = "Build & Config" }
    elseif ($normalized -match "\.md$") { $category = "Markdown Documentation" }

    return [PSCustomObject]@{
        File = $normalized
        Service = $service
        Category = $category
    }
}

$prData = $null
$changedFilesList = [System.Collections.Generic.List[string]]::new()
$diffStatOutput = ""

if ($PR) {
    $cleanPR = $PR.TrimStart('#')
    try {
        $ghRaw = gh pr view $cleanPR --json number,title,body,baseRefName,headRefName,url,author,changedFiles,additions,deletions 2>$null
        if ($ghRaw) {
            $prData = $ghRaw | ConvertFrom-Json
        }
        $statLines = gh pr diff $cleanPR --stat 2>$null
        if ($statLines) { $diffStatOutput = ($statLines -join "`n") }
        $names = gh pr diff $cleanPR --name-only 2>$null
        if ($names) {
            foreach ($n in $names) {
                if ($n.Trim()) { $changedFilesList.Add($n.Trim()) }
            }
        }
    } catch {
        Write-Warning "Aviso: no se pudo consultar gh CLI para PR $cleanPR"
    }
}

if (-not $prData) {
    $targetHead = if ($Branch) { $Branch } else { "HEAD" }
    $targetBase = if ($Base) { $Base } else {
        $hasOriginMain = git rev-parse --verify origin/main 2>$null
        if ($LASTEXITCODE -eq 0) { "origin/main" } else { "main" }
    }

    $mergeBase = (git merge-base "$targetBase" "$targetHead" 2>$null)
    $compareRef = if ($mergeBase) { "$mergeBase" } else { "$targetBase" }

    if ($Current -or (-not $Branch)) {
        $statLines = git diff "$compareRef" --stat 2>$null
        if ($statLines) { $diffStatOutput = ($statLines -join "`n") }
        $names = git diff "$compareRef" --name-only 2>$null
        if ($names) {
            foreach ($n in $names) {
                if ($n.Trim()) { $changedFilesList.Add($n.Trim()) }
            }
        }
        $untracked = git ls-files --others --exclude-standard 2>$null
        if ($untracked) {
            foreach ($u in $untracked) {
                if ($u.Trim() -and (-not $changedFilesList.Contains($u.Trim()))) {
                    $changedFilesList.Add($u.Trim())
                }
            }
        }
    } else {
        $range = "$compareRef...$targetHead"
        $statLines = git diff "$range" --stat 2>$null
        if ($statLines) { $diffStatOutput = ($statLines -join "`n") }
        $names = git diff "$range" --name-only 2>$null
        if ($names) {
            foreach ($n in $names) {
                if ($n.Trim()) { $changedFilesList.Add($n.Trim()) }
            }
        }
    }

    $lastCommitMsg = (git log -1 --pretty=format:"%s" "$targetHead" 2>$null)
    $authorName = (git config user.name 2>$null)
    if (-not $authorName) { $authorName = "local-developer" }
    $currentBranchName = (git branch --show-current 2>$null)
    if (-not $currentBranchName) { $currentBranchName = "HEAD" }

    $prData = [PSCustomObject]@{
        number = if ($PR) { $PR } else { "LOCAL" }
        title = if ($lastCommitMsg) { $lastCommitMsg } else { "Cambios locales en working tree" }
        body = "Comparacion automatica contra $targetBase"
        baseRefName = $targetBase
        headRefName = if ($Branch) { $Branch } else { $currentBranchName }
        url = "git diff $compareRef"
        author = [PSCustomObject]@{ login = $authorName }
        changedFiles = $changedFilesList.Count
        additions = 0
        deletions = 0
    }
}

$categorized = @()
foreach ($f in $changedFilesList) {
    $categorized += (Categorize-File $f)
}

$byService = $categorized | Group-Object Service

if ($Json) {
    $result = [PSCustomObject]@{
        Metadata = $prData
        ServicesImpacted = $byService | ForEach-Object {
            [PSCustomObject]@{
                Service = $_.Name
                FileCount = $_.Count
                Files = $_.Group | Select-Object File, Category
            }
        }
        DiffStat = $diffStatOutput
    }
    $result | ConvertTo-Json -Depth 5
    exit 0
}

Write-Host "# [PR REVIEW CONTEXT REPORT]" -ForegroundColor Cyan
Write-Host ""
Write-Host "### 1. Metadatos del PR" -ForegroundColor Yellow
Write-Host "- PR / Rama: $($prData.number) - $($prData.headRefName)"
Write-Host "- Base: $($prData.baseRefName)"
Write-Host "- Titulo: $($prData.title)"
Write-Host "- Autor: $($prData.author.login)"
Write-Host "- Referencia: $($prData.url)"
Write-Host ""

if ($prData.body -and $prData.body.Trim().Length -gt 0) {
    Write-Host "### 2. Descripcion / Objetivo Declarado" -ForegroundColor Yellow
    $bodyPreview = ($prData.body.Trim() -split "`n" | Select-Object -First 10) -join "`n"
    Write-Host $bodyPreview
    Write-Host ""
}

Write-Host "### 3. Microservicios y Modulos Afectados ($($changedFilesList.Count) archivos)" -ForegroundColor Yellow
foreach ($grp in $byService) {
    Write-Host "- $($grp.Name) ($($grp.Count) archivos):" -ForegroundColor Green
    foreach ($item in $grp.Group) {
        Write-Host "  * $($item.File) - [$($item.Category)]"
    }
}

Write-Host ""
Write-Host "### 4. Resumen Estadistico (Diff Stat)" -ForegroundColor Yellow
if ($diffStatOutput) {
    Write-Host $diffStatOutput
} else {
    Write-Host "(Sin modificaciones confirmadas o working tree limpio)"
}
