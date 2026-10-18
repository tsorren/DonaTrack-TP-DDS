[CmdletBinding()]
param(
    [switch]$Fast,
    [switch]$DryRun,
    [string]$BaseRef = ""
)

$ErrorActionPreference = "Stop"

function Write-Info($msg) { Write-Host "[TIA] $msg" -ForegroundColor Cyan }
function Write-Ok($msg) { Write-Host "[TIA:OK] $msg" -ForegroundColor Green }
function Write-Warn($msg) { Write-Host "[TIA:WARN] $msg" -ForegroundColor Yellow }
function Write-ErrorMsg($msg) { Write-Host "[TIA:FAIL] $msg" -ForegroundColor Red }

# 1. Collect changed files from git
$changedFiles = [System.Collections.Generic.HashSet[string]]::new()

# Unstaged and staged changes
$statusOutput = git status --porcelain 2>$null
if ($statusOutput) {
    foreach ($line in $statusOutput) {
        if ($line.Length -ge 4) {
            $filePath = $line.Substring(3).Trim()
            if ($filePath.Contains(" -> ")) {
                $filePath = $filePath.Split(" -> ")[1].Trim()
            }
            $filePath = $filePath.Replace("\", "/")
            [void]$changedFiles.Add($filePath)
        }
    }
}

# Diff against BaseRef or HEAD
$diffCmd = if ($BaseRef) { "git diff --name-only $BaseRef" } else { "git diff --name-only HEAD" }
$diffOutput = Invoke-Expression $diffCmd 2>$null
if ($diffOutput) {
    foreach ($f in $diffOutput) {
        $normalized = $f.Trim().Replace("\", "/")
        if ($normalized) {
            [void]$changedFiles.Add($normalized)
        }
    }
}

if ($changedFiles.Count -eq 0) {
    Write-Ok "No changed files detected. Working tree clean."
    exit 0
}

Write-Info "Detected $($changedFiles.Count) changed file(s):"
foreach ($f in $changedFiles) {
    Write-Host "  - $f" -ForegroundColor DarkGray
}

# 2. Check if all changes are docs / non-code
$codeFiles = $changedFiles | Where-Object {
    $_ -notmatch '^docs/' -and
    $_ -notmatch '\.md$' -and
    $_ -notmatch '^\.gitignore$' -and
    $_ -notmatch '^\.git/' -and
    $_ -notmatch '^\.idea/'
}

if (@($codeFiles).Count -eq 0) {
    Write-Ok "Only documentation or repository metadata modified. No Java test execution needed."
    exit 0
}

# 3. Check for Reactor escalation triggers
$escalateToReactor = $false
$reactorTriggers = @()

foreach ($f in $codeFiles) {
    if ($f -eq "pom.xml") {
        $escalateToReactor = $true
        $reactorTriggers += "root pom.xml"
    }
    elseif ($f -like "common-lib/*") {
        $escalateToReactor = $true
        $reactorTriggers += "common-lib ($f)"
    }
    elseif ($f -like "docker-compose*" -or $f -like ".github/*") {
        $escalateToReactor = $true
        $reactorTriggers += "transversal config ($f)"
    }
}

# Extra flags for -Fast mode
$extraFlags = @()
if ($Fast) {
    $extraFlags += "-Dspotless.check.skip=true"
    $extraFlags += "-DfailIfNoSpecifiedTests=false"
    $extraFlags += "-Dtest=!*IntegrationTest,!*E2E*"
}

if ($escalateToReactor) {
    Write-Warn "Escalating to FULL REACTOR test run due to shared changes: $($reactorTriggers -join ', ')"
    $cmd = "mvn test"
    if ($extraFlags.Count -gt 0) {
        $cmd += " " + ($extraFlags -join " ")
    }
    Write-Info "Executing: $cmd"
    if ($DryRun) {
        Write-Ok "[DryRun] Skipped execution."
        exit 0
    }
    Invoke-Expression $cmd
    exit $LASTEXITCODE
}

# 4. Analyze per-service changes
$knownModules = @("donaciones-service", "logistica-service", "notificaciones-service", "viandas-service", "incentivos-service", "integration-tests")
$moduleMap = @{}

foreach ($mod in $knownModules) {
    $modFiles = $codeFiles | Where-Object { $_ -like "$mod/*" }
    if (@($modFiles).Count -gt 0) {
        $moduleMap[$mod] = @($modFiles)
    }
}

if ($moduleMap.Keys.Count -eq 0) {
    Write-Warn "Changes outside known Maven service modules. Running default checks."
    exit 0
}

# Evaluate escalation vs surgical test selection per module
$moduleCommands = @()

foreach ($mod in $moduleMap.Keys) {
    $files = $moduleMap[$mod]
    $escalateModule = $false
    $moduleTriggers = @()
    $targetTests = [System.Collections.Generic.HashSet[string]]::new()

    if ($files.Count -gt 8) {
        $escalateModule = $true
        $moduleTriggers += "more than 8 files changed in module"
    }

    foreach ($f in $files) {
        # Check module-level escalation triggers
        if ($f -match "/pom\.xml$") {
            $escalateModule = $true
            $moduleTriggers += "module pom.xml"
        }
        elseif ($f -match "/src/main/resources/") {
            $escalateModule = $true
            $moduleTriggers += "resources/configuration ($f)"
        }
        elseif ($f -match "/domain/" -or $f -match "/entities/" -or $f -match "/model/") {
            $escalateModule = $true
            $moduleTriggers += "domain entity ($f)"
        }
        elseif ($f -match "/services/I[A-Z].*\.java$") {
            $escalateModule = $true
            $moduleTriggers += "service interface contract ($f)"
        }
        elseif ($f -match "/config/") {
            $escalateModule = $true
            $moduleTriggers += "Spring configuration ($f)"
        }
        # Surgical mapping for unit/slice tests
        elseif ($f -match "/src/test/java/.*/([A-Za-z0-9_]+)\.java$") {
            [void]$targetTests.Add($Matches[1])
        }
        elseif ($f -match "/src/main/java/.*/([A-Za-z0-9_]+)Controller\.java$") {
            [void]$targetTests.Add("$($Matches[1])ControllerTest")
        }
        elseif ($f -match "/src/main/java/.*/([A-Za-z0-9_]+)ServiceImpl\.java$") {
            [void]$targetTests.Add("$($Matches[1])ServiceTest")
        }
        elseif ($f -match "/src/main/java/.*/([A-Za-z0-9_]+)Service\.java$") {
            [void]$targetTests.Add("$($Matches[1])ServiceTest")
        }
        elseif ($f -match "/src/main/java/.*/([A-Za-z0-9_]+)\.java$") {
            [void]$targetTests.Add("$($Matches[1])Test")
        }
    }

    if ($escalateModule -or $targetTests.Count -eq 0) {
        Write-Info "Module [$mod]: Escalating to full module suite (Triggers: $($moduleTriggers -join ', '))"
        $mCmd = "mvn test -pl $mod -am"
        if ($extraFlags.Count -gt 0) {
            $mCmd += " " + ($extraFlags -join " ")
        }
        $moduleCommands += $mCmd
    }
    else {
        $testList = ($targetTests -join ",")
        Write-Ok "Module [$mod]: Surgical testing target(s) -> $testList"
        $mCmd = "mvn test -pl $mod -am -Dtest=$testList"
        if ($extraFlags.Count -gt 0) {
            $mCmd += " " + ($extraFlags -join " ")
        }
        $moduleCommands += $mCmd
    }
}

# 5. Execute commands
foreach ($cmd in $moduleCommands) {
    Write-Info "Executing: $cmd"
    if ($DryRun) {
        Write-Ok "[DryRun] Skipped execution."
    }
    else {
        Invoke-Expression $cmd
        if ($LASTEXITCODE -ne 0) {
            Write-ErrorMsg "Command failed with exit code $LASTEXITCODE"
            exit $LASTEXITCODE
        }
    }
}

Write-Ok "All targeted tests completed successfully."
exit 0
