#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Tests de K6: scripts/swarm/caos.sh, la prueba del autorestart de Swarm.

Corren sin Docker y sin dependencias (stdlib). Lo que NO cubren —que Swarm reemplace la réplica rota y
el servicio vuelva a N/N— se verifica corriendo el script contra el stack; ver scripts/swarm/README.md,
"Prueba del autorestart".
"""

import os
import re
import shutil
import subprocess
import unittest

RAIZ = os.path.abspath(os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", ".."))
CAOS = os.path.join("scripts", "swarm", "caos.sh")
BASH = shutil.which("bash")


def leer(*partes):
    with open(os.path.join(RAIZ, *partes), encoding="utf-8") as f:
        return f.read()


def puertos(texto):
    """Mapa servicio -> puerto del `declare -A PUERTO=(...)` de un script."""
    bloque = re.search(r"declare -A PUERTO=\((.*?)\)", texto, re.S).group(1)
    return dict(re.findall(r"\[([a-z-]+)\]=(\d+)", bloque))


@unittest.skipIf(BASH is None, "sin bash")
class CaosArgumentosTests(unittest.TestCase):
    def correr(self, *args):
        return subprocess.run([BASH, CAOS, *args], cwd=RAIZ, capture_output=True, text=True, timeout=30)

    def test_la_sintaxis_es_valida(self):
        r = subprocess.run([BASH, "-n", CAOS], cwd=RAIZ, capture_output=True, text=True, timeout=30)
        self.assertEqual(r.returncode, 0, r.stderr)

    def test_sin_argumentos_muestra_el_uso_y_sale_con_2(self):
        r = self.correr()
        self.assertEqual(r.returncode, 2)
        self.assertIn("Uso: scripts/swarm/caos.sh <servicio> <congelar|matar>", r.stderr)

    def test_un_servicio_que_no_es_propio_sale_con_2(self):
        for servicio in ("rabbitmq", "postgres", "nada"):
            with self.subTest(servicio=servicio):
                r = self.correr(servicio, "matar")
                self.assertEqual(r.returncode, 2)
                self.assertIn(f"Servicio desconocido: {servicio}", r.stderr)

    def test_un_modo_desconocido_sale_con_2(self):
        r = self.correr("api-gateway", "romper")
        self.assertEqual(r.returncode, 2)
        self.assertIn("Modo desconocido: romper", r.stderr)


class CaosContenidoTests(unittest.TestCase):
    def test_los_modulos_y_puertos_son_los_de_verificar_replicas(self):
        # Los dos scripts tienen que apuntar a los mismos seis módulos y puertos internos del preprod.
        self.assertEqual(
            puertos(leer(CAOS)), puertos(leer("scripts", "swarm", "verificar-replicas.sh"))
        )

    def test_cada_puerto_es_el_del_healthcheck_del_preprod(self):
        compose = leer("docker-compose.preprod.yml")
        for servicio, puerto in puertos(leer(CAOS)).items():
            with self.subTest(servicio=servicio):
                self.assertIn(f"http://localhost:{puerto}/actuator/health/liveness", compose)

    def test_pide_liveness_y_rompe_con_las_dos_senales_del_adr(self):
        # ADR de Swarm, "Prueba del autorestart": docker kill -s STOP y docker kill.
        texto = leer(CAOS)
        self.assertIn("/actuator/health/liveness", texto)
        self.assertIn("congelar) senal=STOP", texto)
        self.assertIn("matar) senal=KILL", texto)
        self.assertIn('docker kill -s "$senal"', texto)


if __name__ == "__main__":
    unittest.main()
