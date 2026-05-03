# CI/CD Documentation - DonaTrack

## Overview
This document details the CI/CD infrastructure for **DonaTrack**. The workflow is designed for a multi-module **Maven** architecture, ensuring code integrity, design quality, and compliance with UTN's academic **Git Flow**.

---

## 1. Remote Pipeline (GitHub Actions)
The `.github/workflows/main.yml` file centralizes all validations in a unified pipeline based on **"Fail-fast"** logic.

### 1.1. Branching Policy:
* **Merges to `main`**: Only allowed from `ENTREGA_N` branches via Pull Request.
* **Merges to `ENTREGA_N`**: Only allowed from requirement branches with the `En_` prefix (e.g., `E1_feature-name`).

### Pipeline Stages:
1. **Git Flow Validation**: Ensures Pull Requests respect the branch hierarchy and naming convention defined by the department.
2. **Static Analysis & Design**: Runs `spotless:check` for formatting and uses **AI (Gemini)** to audit implementation against manual **PlantUML** diagrams.
3. **Test, Coverage & SonarCloud**: Executes **JUnit** suite, generates **JaCoCo** reports, and syncs with **SonarCloud**. Concludes by verifying **Docker** image builds.

---

## 2. IA-Driven Design Audit
**File:** `.github/scripts/compare_diagrams.py`

This script automates parity verification between the domain model (diagrams in `/docs`) and the code. The AI analyzes classes, methods, and privacy levels (`public`, `protected`, `private`), enabling real traceability between design and construction.

---

## 3. Local Validation (Git Hooks)
To maintain a professional Git history and clear messages, a local validation system was implemented using **Git Hooks**.

### Windows Installation:
1. Run the PowerShell script: `./.github/scripts/setup-hooks.ps1`.
2. This script will automatically install the hook into the `.git/hooks/` directory.

### How it works:
Upon `git commit`, the script checks formatting and runs unit tests only for modified modules of the Maven reactor.

---

## 4. Configuration Requirements (Secrets)
The pipeline's correct operation requires GitHub secrets configuration: `SONAR_TOKEN`, `SONAR_PROJECT_KEY`, `SONAR_ORG`, `SONAR_URL`, and `GEMINI_API_KEY`.

---

## 5. Quality Philosophy
### Why `spotless:check` in the Hook?
`check` was chosen over `apply` to ensure **determinism**. The developer is responsible for reviewing formatting changes before signing the commit, avoiding unsupervised automatic modifications in the staging area.