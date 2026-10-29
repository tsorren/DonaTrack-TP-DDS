#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Tests de resolver-imagenes-ghcr.sh: rechazo de un manifiesto adulterado (RM-06)."""

import os
import shutil
import subprocess
import sys
import tempfile
import unittest

SCRIPTS = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..")
sys.path.insert(0, SCRIPTS)

import manifiesto_version as mv

DIGEST = "sha256:" + "c" * 64
GHCR_BASE = "ghcr.io/tsorren/donatrack-tp-dds"
BASH = shutil.which("bash")


@unittest.skipIf(BASH is None, "bash no disponible")
class ResolverManifiestoTests(unittest.TestCase):
    def setUp(self):
        # Repo mínimo: un servicio con pom y Dockerfile, y los dos scripts que usa el resolvedor.
        self.tmp = tempfile.mkdtemp()
        os.makedirs(os.path.join(self.tmp, "scripts"))
        for nombre in ("resolver-imagenes-ghcr.sh", "manifiesto_version.py"):
            shutil.copy(os.path.join(SCRIPTS, nombre), os.path.join(self.tmp, "scripts", nombre))
        os.makedirs(os.path.join(self.tmp, "svc-a"))
        for nombre in ("pom.xml", "Dockerfile"):
            open(os.path.join(self.tmp, "svc-a", nombre), "w", encoding="utf-8").close()
        # docker falso: registra cada llamada y no baja nada.
        self.bin = os.path.join(self.tmp, "bin")
        os.makedirs(self.bin)
        self.llamadas = os.path.join(self.tmp, "docker.log")
        docker = os.path.join(self.bin, "docker")
        with open(docker, "w", encoding="utf-8", newline="\n") as f:
            f.write(f'#!/usr/bin/env bash\necho "$@" >> "{self.llamadas.replace(os.sep, "/")}"\n')
        os.chmod(docker, 0o755)

    def tearDown(self):
        shutil.rmtree(self.tmp, ignore_errors=True)

    def _manifiesto(self, registro):
        digests = os.path.join(self.tmp, "digests")
        os.makedirs(digests, exist_ok=True)
        with open(os.path.join(digests, "svc-a.digest"), "w", encoding="utf-8") as f:
            f.write(DIGEST + "\n")
        salida = os.path.join(self.tmp, "manifiesto-version.json")
        self.assertEqual(mv.main(["generar", "--rama", "ENTREGA_5", "--commit", "abc1234",
                                  "--corrida", "1", "--registro", registro, "--digests", digests,
                                  "-o", salida]), 0)
        return salida

    def _resolver(self, manifiesto):
        env = dict(os.environ, GHCR_BASE=GHCR_BASE, MANIFIESTO=manifiesto,
                   PATH=self.bin + os.pathsep + os.environ["PATH"])
        env.pop("PR_TAG", None)
        env.pop("BASE_TAG", None)
        return subprocess.run([BASH, "scripts/resolver-imagenes-ghcr.sh"], cwd=self.tmp, env=env,
                              capture_output=True, text=True, encoding="utf-8")

    def _docker_llamado(self):
        return os.path.exists(self.llamadas)

    def test_manifiesto_con_registro_ajeno_se_rechaza_sin_bajar_la_imagen(self):
        r = self._resolver(self._manifiesto("ghcr.io/atacante/otro-repo"))
        self.assertEqual(r.returncode, 1, r.stdout + r.stderr)
        self.assertIn(f"fuera de {GHCR_BASE}/svc-a@sha256", r.stdout)
        self.assertFalse(self._docker_llamado())

    def test_manifiesto_del_registro_del_repo_se_resuelve_por_digest(self):
        r = self._resolver(self._manifiesto(GHCR_BASE))
        self.assertEqual(r.returncode, 0, r.stdout + r.stderr)
        with open(self.llamadas, encoding="utf-8") as f:
            self.assertIn(f"pull -q {GHCR_BASE}/svc-a@{DIGEST}", f.read())


if __name__ == "__main__":
    unittest.main()
