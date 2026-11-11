#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Tests de SPEC-06 E2-12a: Keycloak (auth-service) en docker-compose.preprod.yml y en el CI.

Corren sin Docker y sin dependencias (stdlib; PyYAML solo para validar la sintaxis si está instalado).
Lo que NO cubren —que `keycloak` quede `healthy` en el preprod y que los workflows corran en GitHub—
se verifica a mano en el primer PR; ver la fila E2-12a de SPEC-06.
"""

import json
import os
import re
import shutil
import subprocess
import sys
import tempfile
import unittest

try:
    import yaml
except ImportError:  # pragma: no cover
    yaml = None

SCRIPTS = os.path.abspath(os.path.join(os.path.dirname(os.path.abspath(__file__)), ".."))
RAIZ = os.path.abspath(os.path.join(SCRIPTS, ".."))
sys.path.insert(0, SCRIPTS)

import manifiesto_version as mv  # noqa: E402

BASH = shutil.which("bash")
DIGEST = "sha256:" + "d" * 64
GHCR_BASE = "ghcr.io/tsorren/donatrack-tp-dds"


def leer(*partes):
    with open(os.path.join(RAIZ, *partes), encoding="utf-8") as f:
        return f.read()


def bloque(texto, nombre, sangria):
    """Texto del bloque `nombre:` indentado `sangria` espacios (servicio de compose o job de workflow)."""
    lineas = texto.splitlines()
    cabecera = " " * sangria + nombre + ":"
    inicio = next((i for i, linea in enumerate(lineas) if linea == cabecera), None)
    if inicio is None:
        return None
    fin = len(lineas)
    for j in range(inicio + 1, len(lineas)):
        if lineas[j].strip() and len(lineas[j]) - len(lineas[j].lstrip()) <= sangria:
            fin = j
            break
    return "\n".join(lineas[inicio:fin])


def servicio(compose, nombre):
    return bloque(compose, nombre, 2)


def job(workflow, nombre):
    return bloque(workflow, nombre, 2)


def entradas_environment(texto_servicio):
    """Líneas `- CLAVE=valor` del bloque `environment:` de un servicio."""
    m = re.search(r"^    environment:\n((?:      .*\n?)+)", texto_servicio, re.M)
    return [linea.strip()[2:] for linea in m.group(1).splitlines() if linea.strip().startswith("- ")]


def healthcheck(texto_servicio):
    m = re.search(r"^    healthcheck:\n((?:      .*\n?)+)", texto_servicio, re.M)
    return m.group(1) if m else None


class PreprodComposeTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.preprod = leer("docker-compose.preprod.yml")
        cls.local = leer("docker-compose.yml")
        cls.kc = servicio(cls.preprod, "keycloak")

    def test_existe_el_servicio_keycloak(self):
        self.assertIsNotNone(self.kc)

    def test_la_imagen_es_la_que_deja_el_resolver(self):
        # resolver-imagenes-ghcr.sh etiqueta donatrack/<carpeta>:preprod, y la carpeta es auth-service.
        self.assertIn("image: donatrack/auth-service:preprod", self.kc)

    def test_se_construye_desde_auth_service_con_el_dockerfile_de_keycloak(self):
        self.assertIn("context: ./auth-service", self.kc)
        self.assertIn("dockerfile: Dockerfile", self.kc)
        # No usa el patrón de los servicios Java (target ci con JAR_FILE).
        self.assertNotIn("target: ci", self.kc)
        self.assertNotIn("JAR_FILE", self.kc)

    def test_publica_solo_el_8180_en_loopback_y_no_el_9000(self):
        puertos = re.findall(r'^      - "([^"]+)"$', re.search(r"    ports:\n((?:      .*\n)+)", self.kc).group(1), re.M)
        self.assertEqual(puertos, ["127.0.0.1:8180:8080"])

    def test_el_entorno_es_el_mismo_que_el_del_compose_local(self):
        self.assertEqual(sorted(entradas_environment(self.kc)),
                         sorted(entradas_environment(servicio(self.local, "keycloak"))))

    def test_el_healthcheck_es_el_mismo_que_el_del_compose_local(self):
        # Por el puerto de gestión, sin curl (la imagen no lo trae).
        self.assertEqual(healthcheck(self.kc), healthcheck(servicio(self.local, "keycloak")))
        self.assertIn("/dev/tcp/127.0.0.1/9000", healthcheck(self.kc))
        self.assertNotIn("curl", healthcheck(self.kc))

    def test_espera_a_postgres_sano_y_usa_la_red_de_preprod(self):
        self.assertRegex(self.kc, r"depends_on:\n      postgres:\n        condition: service_healthy")
        self.assertIn("preprod-network", self.kc)

    def test_los_secretos_son_obligatorios_y_sin_literales(self):
        for var in ("KEYCLOAK_DB_PASSWORD", "KC_BOOTSTRAP_ADMIN_PASSWORD", "CLIENTE_LIVIANO_SECRET",
                    "AUTH_ADMIN_PASSWORD"):
            self.assertIn("${%s:?ver .env.example}" % var, self.kc, var)
        for linea in entradas_environment(self.kc):
            clave, _, valor = linea.partition("=")
            if re.search(r"PASSWORD|SECRET", clave) and not clave.startswith("GOOGLE_"):
                self.assertTrue(valor.startswith("${"), "%s lleva un literal" % clave)

    def test_postgres_de_preprod_recibe_la_clave_de_keycloak(self):
        pg = servicio(self.preprod, "postgres")
        self.assertIn("KEYCLOAK_DB_PASSWORD: ${KEYCLOAK_DB_PASSWORD:?ver .env.example}", pg)

    def test_las_variables_obligatorias_las_genera_el_generador_de_env(self):
        generador = leer("scripts", "generar-env-db.sh")
        obligatorias = set(re.findall(r"\$\{([A-Z_]+):\?", self.kc))
        self.assertTrue(obligatorias)
        for var in obligatorias:
            self.assertIn('echo "%s=' % var, generador, var)

    def test_ningun_otro_servicio_dependia_de_keycloak_antes_de_esta_etapa(self):
        # Keycloak llega solo al preprod: lo valida el gateway recién en E2-01. Si alguien lo agrega
        # como dependencia, este test obliga a revisar el orden de arranque a propósito.
        for nombre in ("notificaciones-service", "incentivos-service", "donaciones-service",
                       "logistica-service"):
            self.assertNotIn("keycloak", servicio(self.preprod, nombre), nombre)

    @unittest.skipIf(yaml is None, "PyYAML no disponible")
    def test_el_compose_de_preprod_es_yaml_valido(self):
        datos = yaml.safe_load(self.preprod)
        self.assertIn("keycloak", datos["services"])


@unittest.skipIf(BASH is None, "bash no disponible")
class ResolverTests(unittest.TestCase):
    def setUp(self):
        # Repo mínimo con los servicios que importan: auth-service y un Java. El api-gateway se crea
        # en los tests que lo usan (_crear_servicio).
        self.tmp = tempfile.mkdtemp()
        os.makedirs(os.path.join(self.tmp, "scripts"))
        for nombre in ("resolver-imagenes-ghcr.sh", "manifiesto_version.py"):
            shutil.copy(os.path.join(SCRIPTS, nombre), os.path.join(self.tmp, "scripts", nombre))
        for svc in ("auth-service", "svc-a"):
            self._crear_servicio(svc)
        self.bin = os.path.join(self.tmp, "bin")
        os.makedirs(self.bin)
        self.llamadas = os.path.join(self.tmp, "docker.log")
        docker = os.path.join(self.bin, "docker")
        with open(docker, "w", encoding="utf-8", newline="\n") as f:
            f.write('#!/usr/bin/env bash\necho "$@" >> "%s"\n' % self.llamadas.replace(os.sep, "/"))
        os.chmod(docker, 0o755)

    def tearDown(self):
        shutil.rmtree(self.tmp, ignore_errors=True)

    def _crear_servicio(self, svc):
        os.makedirs(os.path.join(self.tmp, svc))
        for nombre in ("pom.xml", "Dockerfile"):
            open(os.path.join(self.tmp, svc, nombre), "w", encoding="utf-8").close()

    def _correr(self, **extra):
        env = dict(os.environ, GHCR_BASE=GHCR_BASE, PATH=self.bin + os.pathsep + os.environ["PATH"])
        for var in ("PR_TAG", "BASE_TAG", "MANIFIESTO", "CHANGED_SERVICES"):
            env.pop(var, None)
        env.update(extra)
        return subprocess.run([BASH, "scripts/resolver-imagenes-ghcr.sh"], cwd=self.tmp, env=env,
                              capture_output=True, text=True)

    def _docker(self):
        if not os.path.exists(self.llamadas):
            return ""
        with open(self.llamadas, encoding="utf-8") as f:
            return f.read()

    def _manifiesto(self, servicios):
        digests = os.path.join(self.tmp, "digests")
        os.makedirs(digests, exist_ok=True)
        for svc in servicios:
            with open(os.path.join(digests, svc + ".digest"), "w", encoding="utf-8") as f:
                f.write(DIGEST + "\n")
        salida = os.path.join(self.tmp, "manifiesto-version.json")
        self.assertEqual(mv.main(["generar", "--rama", "ENTREGA_6", "--commit", "abc1234", "--corrida", "1",
                                  "--registro", GHCR_BASE, "--digests", digests, "-o", salida]), 0)
        return salida

    def test_con_auth_service_modificado_exige_su_imagen_del_pr_y_la_etiqueta_para_preprod(self):
        r = self._correr(PR_TAG="pr-7", CHANGED_SERVICES="auth-service,svc-a")
        self.assertEqual(r.returncode, 0, r.stdout + r.stderr)
        log = self._docker()
        self.assertIn("pull -q %s/auth-service:pr-7" % GHCR_BASE, log)
        self.assertIn("tag %s/auth-service:pr-7 donatrack/auth-service:preprod" % GHCR_BASE, log)

    def test_auth_service_sin_cambios_se_fija_por_digest_del_manifiesto(self):
        manifiesto = self._manifiesto(["auth-service", "svc-a"])
        r = self._correr(PR_TAG="pr-7", CHANGED_SERVICES="svc-a", MANIFIESTO=manifiesto)
        self.assertEqual(r.returncode, 0, r.stdout + r.stderr)
        self.assertIn("pull -q %s/auth-service@%s" % (GHCR_BASE, DIGEST), self._docker())

    def test_un_manifiesto_sin_auth_service_falla_y_no_se_improvisa_otra_imagen(self):
        # RM-06: sin digest validado no hay imagen. Aplica hasta que el primer merge lo publique.
        manifiesto = self._manifiesto(["svc-a"])
        r = self._correr(PR_TAG="pr-7", CHANGED_SERVICES="svc-a", MANIFIESTO=manifiesto)
        self.assertNotEqual(r.returncode, 0)
        self.assertIn("auth-service", r.stdout + r.stderr)
        self.assertNotIn("auth-service", self._docker())

    def test_api_gateway_modificado_exige_su_imagen_del_pr_y_la_etiqueta_para_preprod(self):
        # El gateway entró al preprod (ADR-E6-02): se resuelve como cualquier otro servicio.
        self._crear_servicio("api-gateway")
        r = self._correr(PR_TAG="pr-7", CHANGED_SERVICES="api-gateway,svc-a,auth-service")
        self.assertEqual(r.returncode, 0, r.stdout + r.stderr)
        log = self._docker()
        self.assertIn("pull -q %s/api-gateway:pr-7" % GHCR_BASE, log)
        self.assertIn("tag %s/api-gateway:pr-7 donatrack/api-gateway:preprod" % GHCR_BASE, log)

    def test_api_gateway_sin_cambios_se_fija_por_digest_del_manifiesto(self):
        self._crear_servicio("api-gateway")
        manifiesto = self._manifiesto(["api-gateway", "auth-service", "svc-a"])
        r = self._correr(PR_TAG="pr-7", CHANGED_SERVICES="svc-a", MANIFIESTO=manifiesto)
        self.assertEqual(r.returncode, 0, r.stdout + r.stderr)
        self.assertIn("pull -q %s/api-gateway@%s" % (GHCR_BASE, DIGEST), self._docker())

    def test_un_manifiesto_sin_api_gateway_falla_y_no_se_improvisa_otra_imagen(self):
        # RM-06: sin digest validado no hay imagen. Aplica hasta que un merge con el gateway llegue a promote.
        self._crear_servicio("api-gateway")
        manifiesto = self._manifiesto(["auth-service", "svc-a"])
        r = self._correr(PR_TAG="pr-7", CHANGED_SERVICES="svc-a", MANIFIESTO=manifiesto)
        self.assertNotEqual(r.returncode, 0)
        self.assertIn("api-gateway", r.stdout + r.stderr)
        self.assertNotIn("api-gateway", self._docker())


class ManifiestoTests(unittest.TestCase):
    def test_el_digest_de_auth_service_entra_al_manifiesto_y_se_lee_por_nombre(self):
        # merge.yml guarda digests/auth-service.digest y preprod-post-merge lo descarga con digest-*.
        with tempfile.TemporaryDirectory() as tmp:
            for svc in ("auth-service", "donaciones-service"):
                with open(os.path.join(tmp, svc + ".digest"), "w", encoding="utf-8") as f:
                    f.write(DIGEST + "\n")
            salida = os.path.join(tmp, "m.json")
            self.assertEqual(mv.main(["generar", "--rama", "ENTREGA_6", "--commit", "abc", "--corrida", "1",
                                      "--registro", GHCR_BASE, "--digests", tmp, "-o", salida]), 0)
            with open(salida, encoding="utf-8") as f:
                refs = mv.referencias(json.load(f))
            self.assertEqual(refs["auth-service"], "%s/auth-service@%s" % (GHCR_BASE, DIGEST))


class MainWorkflowTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.wf = leer(".github", "workflows", "main.yml")
        cls.auth = job(cls.wf, "auth-service")
        cls.preprod = job(cls.wf, "preprod-validation")
        cls.gate = job(cls.wf, "quality-gate")

    def test_hay_un_job_auth_service_que_corre_solo_si_cambio_auth_service(self):
        self.assertIsNotNone(self.auth)
        self.assertIn("needs.detect-changes.outputs.auth-changed == 'true'", self.auth)

    def test_detect_changes_calcula_auth_changed_por_carpeta_o_cambio_global(self):
        self.assertIn("auth-changed: ${{ steps.generate-matrices.outputs.auth-changed }}", self.wf)
        self.assertIn('grep -q "^auth-service/"', self.wf)
        self.assertRegex(self.wf, r'GLOBAL_CHANGED" = "true" \] \|\| echo "\$CHANGED_FILES" \| grep -q "\^auth-service/"')

    def test_auth_service_sigue_fuera_de_las_matrices_java(self):
        # Si entrara, build-and-test correría `mvn -pl auth-service -am` y docker-build exigiría un JAR.
        self.assertRegex(self.wf, r'"\$dir" != "adr-generator" && "\$dir" != "auth-service" \]\]')

    def test_corre_el_contrato_del_realm_con_su_perfil(self):
        self.assertIn("mvn -B -f auth-service/pom.xml -Pcontrato-auth verify", self.auth)

    def test_chequea_el_formato_antes_del_contrato(self):
        self.assertLess(self.auth.index("spotless:check"), self.auth.index("-Pcontrato-auth"))

    def test_la_imagen_se_publica_solo_despues_del_contrato_y_con_el_contexto_propio(self):
        self.assertLess(self.auth.index("-Pcontrato-auth"), self.auth.index("docker/build-push-action"))
        self.assertIn("context: ./auth-service", self.auth)
        self.assertIn("file: auth-service/Dockerfile", self.auth)
        self.assertNotIn("target: ci", self.auth)
        self.assertNotIn("JAR_FILE", self.auth)
        self.assertIn("auth-service:pr-${PR_NUMBER}", self.auth)

    def test_los_pasos_con_efectos_son_acciones_fijadas_por_sha(self):
        for usa in re.findall(r"uses: (\S+)", self.auth):
            self.assertRegex(usa, r"@[0-9a-f]{40}$", usa)

    def test_preprod_espera_al_job_y_tolera_que_se_omita(self):
        self.assertIn("needs: [detect-changes, build-and-test, docker-build, auth-service]", self.preprod)
        self.assertIn("needs.auth-service.result == 'success' || needs.auth-service.result == 'skipped'",
                      self.preprod)

    def test_preprod_corre_aunque_el_pr_toque_solo_auth_service(self):
        self.assertIn("needs.build-and-test.result == 'skipped' && needs.auth-service.result == 'success'",
                      self.preprod)

    def test_un_build_fallido_no_habilita_el_preprod(self):
        # La condición solo admite success (o skipped con auth-service exitoso): nunca failure.
        self.assertNotIn("build-and-test.result != 'failure'", self.preprod)
        self.assertNotIn("build-and-test.result == 'failure'", self.preprod)

    def test_si_cambio_auth_service_su_imagen_del_pr_es_obligatoria_en_el_preprod(self):
        self.assertIn("AUTH_CHANGED: ${{ needs.detect-changes.outputs.auth-changed }}", self.preprod)
        self.assertIn('CHANGED_SERVICES="${CHANGED_SERVICES:+${CHANGED_SERVICES},}auth-service"', self.preprod)

    def test_el_quality_gate_exige_auth_service_cuando_cambio(self):
        self.assertIn("auth-service, generate-diagrams", self.gate)
        self.assertIn('check_job "auth-service"        "${{ needs.auth-service.result }}"        "auth"', self.gate)
        self.assertRegex(self.gate, r'auth\)\s+\[\[ "\$auth_changed" == "true" \]\] && is_required=true')

    def test_el_gate_exige_el_preprod_tambien_si_solo_cambio_auth_service(self):
        self.assertIn('check_job "preprod-validation"  "${{ needs.preprod-validation.result }}"  "any-or-auth"',
                      self.gate)
        self.assertRegex(self.gate, r'any-or-auth\)\s+\[\[ "\$any_service" == "true" \|\| "\$auth_changed" == "true" \]\]')

    @unittest.skipIf(yaml is None, "PyYAML no disponible")
    def test_es_yaml_valido(self):
        self.assertIn("auth-service", yaml.safe_load(self.wf)["jobs"])


class MergeWorkflowTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.wf = leer(".github", "workflows", "merge.yml")
        cls.pub = job(cls.wf, "auth-publish")
        cls.post = job(cls.wf, "preprod-post-merge")

    def test_hay_un_job_auth_publish(self):
        self.assertIsNotNone(self.pub)

    def test_auth_service_sigue_fuera_de_las_matrices_java(self):
        self.assertRegex(self.wf, r'"\$dir" != "adr-generator" && "\$dir" != "auth-service" \]\]')

    def test_verifica_el_contrato_antes_de_publicar_el_candidato(self):
        self.assertLess(self.pub.index("-Pcontrato-auth"), self.pub.index("docker/build-push-action"))

    def test_publica_solo_el_candidato_sha_y_no_las_etiquetas_estables(self):
        # Sin los comentarios: el RM-06 se explica ahí y menciona :latest y :<branch> a propósito.
        codigo = "\n".join(l for l in self.pub.splitlines() if not l.lstrip().startswith("#"))
        self.assertIn("auth-service:sha-${SHA_SHORT}", codigo)
        self.assertNotIn(":latest", codigo)
        self.assertNotIn("BRANCH_TAG", codigo)
        self.assertIn("context: ./auth-service", self.pub)
        self.assertNotIn("target: ci", self.pub)

    def test_guarda_el_digest_con_el_nombre_que_arma_el_manifiesto(self):
        self.assertIn('digests/auth-service.digest', self.pub)
        self.assertIn("name: digest-auth-service", self.pub)
        # preprod-post-merge descarga con este patrón: el nombre del artefacto tiene que calzar.
        self.assertIn("pattern: digest-*", self.post)

    def test_el_post_merge_espera_la_publicacion_de_auth_service(self):
        self.assertIn("needs: [detect-services, docker-publish, auth-publish]", self.post)
        self.assertIn("needs.auth-publish.result == 'success'", self.post)

    def test_el_resumen_falla_si_auth_publish_falla(self):
        resumen = job(self.wf, "merge-summary")
        self.assertIn("auth-publish", resumen)
        self.assertIn('"$RES_AUTH" != "failure"', resumen)

    def test_las_acciones_estan_fijadas_por_sha(self):
        for usa in re.findall(r"uses: (\S+)", self.pub):
            self.assertRegex(usa, r"@[0-9a-f]{40}$", usa)

    @unittest.skipIf(yaml is None, "PyYAML no disponible")
    def test_es_yaml_valido(self):
        self.assertIn("auth-publish", yaml.safe_load(self.wf)["jobs"])


if __name__ == "__main__":
    unittest.main()
