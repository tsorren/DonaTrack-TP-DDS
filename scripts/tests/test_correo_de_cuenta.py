#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Tests de SPEC-06 E2-06 (iteración 8): el correo de cuenta por RabbitMQ con un SPI de Keycloak.

Corren sin Java, Docker ni Keycloak (stdlib). Fijan lo que cruza archivos: que el contrato del evento sea
idéntico en el SPI, en el record de Notificaciones, en el schema y en el catálogo; que la topología coincida
entre el productor, el consumidor y el compose; que el Dockerfile, el pom, el compose y el CI estén
coordinados; y que nada pueda filtrar el email ni el enlace a un log o al historial de notificaciones.

Que el SPI funcione dentro de Keycloak lo probó `scripts/e2e_correo_de_cuenta.py` contra un Keycloak y un
RabbitMQ reales; que Notificaciones lea lo que publica el SPI lo prueba `EventoCuentaEnlaceSolicitadoContratoTest`.
"""

import json
import os
import re
import unittest

RAIZ = os.path.abspath(os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", ".."))
COMPOSES = ("docker-compose.yml", "docker-compose.preprod.yml")
SPI = ("auth-service", "spi", "src", "main", "java", "grupo5", "auth", "correo")
NOTIF = ("notificaciones-service", "src", "main", "java", "grupo5", "notificaciones")


def leer(*partes):
    with open(os.path.join(RAIZ, *partes), encoding="utf-8", errors="replace") as f:
        return f.read()


def leer_abs(ruta):
    with open(ruta, encoding="utf-8", errors="replace") as f:
        return f.read()


def bloque(compose, servicio):
    texto = leer(compose)
    inicio = texto.index("\n  %s:\n" % servicio)
    fin = re.search(r"\n  \S", texto[inicio + 3:]).start() + inicio + 3
    return texto[inicio:fin]


def entorno(b):
    return [l.strip()[2:] for l in b.splitlines() if l.strip().startswith("- ")]


def componentes_de_record(codigo):
    cabecera = re.search(r"public record \w+\((.*?)\)\s*\{", codigo, re.S).group(1)
    sin_anotaciones = re.sub(r'@\w+(\((?:[^()"]|"[^"]*")*\))?', "", cabecera)
    return [p.strip().split()[-1] for p in sin_anotaciones.split(",") if p.strip()]


class ContratoDelEventoTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.spi = leer(*SPI, "EventoEnlaceDeCuenta.java")
        cls.record = leer(*NOTIF, "dto", "input", "EventoCuentaEnlaceSolicitadoV1.java")
        cls.schema = json.loads(leer("docs", "arquitectura", "contratos", "schemas", "evento-cuenta-enlace-solicitado-v1.schema.json"))
        cls.catalogo = leer("docs", "arquitectura", "eventos-amqp.md")

    def test_los_campos_son_los_mismos_y_en_el_mismo_orden_en_los_cuatro_lados(self):
        spi = componentes_de_record(self.spi)
        self.assertEqual(spi, ["tipo", "cuentaId", "destinatario", "enlace", "venceEnMinutos", "idioma", "fecha"])
        self.assertEqual(componentes_de_record(self.record), spi)
        self.assertEqual(list(self.schema["properties"]), spi)
        self.assertEqual(self.schema["required"], spi)
        ejemplo = re.search(r"#### Enlace de Cuenta Solicitado.*?```json\n(.*?)```", self.catalogo, re.S).group(1)
        self.assertEqual(re.findall(r'^  "(\w+)":', ejemplo, re.M), spi)

    def test_el_json_que_arma_el_spi_lleva_los_campos_en_ese_orden(self):
        cuerpo = re.search(r"public String aJson\(\) \{(.*?)\n  \}", self.spi, re.S).group(1)
        self.assertEqual(re.findall(r'\\"(\w+)\\":', cuerpo), componentes_de_record(self.spi))

    def test_los_tipos_son_compatibles(self):
        self.assertIn("long venceEnMinutos", self.spi)
        self.assertIn("long venceEnMinutos", self.record)
        self.assertEqual(self.schema["properties"]["venceEnMinutos"]["type"], "integer")
        self.assertIn("UUID cuentaId", self.record)
        self.assertEqual(self.schema["properties"]["cuentaId"]["format"], "uuid")
        self.assertIn("Instant fecha", self.record)
        self.assertEqual(self.schema["properties"]["fecha"]["format"], "date-time")

    def test_los_tipos_de_enlace_son_los_mismos_en_el_spi_el_record_y_el_schema(self):
        self.assertEqual(self.schema["properties"]["tipo"]["enum"], ["RECUPERAR_CLAVE", "VERIFICAR_EMAIL"])
        for texto in (self.spi, self.record):
            self.assertIn('RECUPERAR_CLAVE = "RECUPERAR_CLAVE"', texto)
            self.assertIn('VERIFICAR_EMAIL = "VERIFICAR_EMAIL"', texto)
        self.assertIn('regexp = "RECUPERAR_CLAVE|VERIFICAR_EMAIL"', self.record)

    def test_el_spi_trunca_la_fecha_a_segundos_como_los_demas_eventos(self):
        self.assertIn("fecha.truncatedTo(ChronoUnit.SECONDS)", self.spi)

    def test_la_routing_key_es_la_misma_en_el_spi_el_consumidor_y_el_catalogo(self):
        clave = "cuenta.enlace-solicitado.v1"
        self.assertIn('ROUTING_KEY = "%s"' % clave, self.spi)
        self.assertIn('ROUTING_KEY_CUENTA_ENLACE_SOLICITADO = "%s"' % clave, leer(*NOTIF, "config", "RabbitMQConfig.java"))
        self.assertIn("| `%s` | `%s` | `notificaciones.auth` |" % (clave, clave), self.catalogo)
        self.assertIn("HEADER_TIPO = \"__TypeId__\"", leer(*SPI, "PublicadorRabbitMq.java"))
        self.assertIn("HEADER_TRACE = \"X-Trace-Id\"", leer(*SPI, "PublicadorRabbitMq.java"))
        self.assertIn('HEADER_TRACE_ID = "X-Trace-Id"', leer(*NOTIF, "infrastructure", "amqp", "CuentaEventListener.java"))

    def test_el_json_del_test_de_contrato_tiene_los_campos_del_record(self):
        test = leer("notificaciones-service", "src", "test", "java", "grupo5", "notificaciones", "infrastructure", "amqp",
                    "EventoCuentaEnlaceSolicitadoContratoTest.java")
        union = "".join(re.findall(r'"((?:[^"\\]|\\.)*)"', re.search(r"JSON_DEL_SPI =(.*?);", test, re.S).group(1)))
        self.assertEqual(re.findall(r'\\"(\w+)\\":', union), componentes_de_record(self.record))

    def test_el_schema_exige_un_enlace_http_o_https(self):
        self.assertEqual(self.schema["properties"]["enlace"]["pattern"], "^https?://\\S+$")
        self.assertIn('regexp = "^https?://\\\\S+$"', self.record)


class TopologiaTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.config = leer(*NOTIF, "config", "RabbitMQConfig.java")
        cls.catalogo = leer("docs", "arquitectura", "eventos-amqp.md")

    def test_el_exchange_es_el_mismo_en_el_spi_el_consumidor_y_el_catalogo(self):
        self.assertIn('EXCHANGE_POR_DEFECTO = "auth.exchange"', leer(*SPI, "ConfiguracionAmqp.java"))
        self.assertIn('EXCHANGE_AUTH = "auth.exchange"', self.config)
        self.assertIn("**`auth.exchange`** | `TopicExchange` (Durable) |", self.catalogo)

    def test_el_spi_declara_el_exchange_durable_y_de_tipo_topic_como_el_consumidor(self):
        self.assertIn("exchangeDeclare(config.exchange(), BuiltinExchangeType.TOPIC, true)", leer(*SPI, "PublicadorRabbitMq.java"))
        self.assertIn("new TopicExchange(EXCHANGE_AUTH, true, false)", self.config)

    def test_la_cola_tiene_su_cola_de_mensajes_muertos_como_las_demas(self):
        self.assertIn('QUEUE_NOTIFICACIONES_AUTH = "notificaciones.auth"', self.config)
        self.assertIn('QUEUE_NOTIFICACIONES_AUTH_DLQ = "notificaciones.auth.dlq"', self.config)
        cola = re.search(r"public Queue queueNotificacionesAuth\(\) \{(.*?)\}", self.config, re.S).group(1)
        self.assertIn('"x-dead-letter-exchange", DLX_NOTIFICACIONES', cola)
        self.assertIn('"x-dead-letter-routing-key", QUEUE_NOTIFICACIONES_AUTH_DLQ', cola)
        self.assertIn("| `notificaciones.auth` | `x-dead-letter-exchange` | `notificaciones.dlx` | `notificaciones.auth.dlq` |", self.catalogo)

    def test_el_binding_toma_solo_las_claves_de_cuenta(self):
        self.assertIn('ROUTING_KEY_CUENTA_WILDCARD = "cuenta.#"', self.config)
        binding = re.search(r"public Binding bindingNotificacionesAuth\((.*?)\n  \}", self.config, re.S).group(1)
        self.assertIn(".to(authExchange)", binding)
        self.assertIn(".with(ROUTING_KEY_CUENTA_WILDCARD)", binding)

    def test_el_evento_esta_mapeado_para_que_notificaciones_pueda_leerlo(self):
        self.assertRegex(self.config, r"idClassMapping\.put\(\s*ROUTING_KEY_CUENTA_ENLACE_SOLICITADO,\s*EventoCuentaEnlaceSolicitadoV1\.class\)")

    def test_el_test_existente_de_donaciones_excluye_las_claves_de_cuenta(self):
        # Sin esto, mapear la clave de cuenta lo haría exigir un binding de Donaciones a una clave de otro contexto.
        test = leer("notificaciones-service", "src", "test", "java", "grupo5", "notificaciones", "config", "RabbitMQConfigTest.java")
        self.assertIn('!k.startsWith("cuenta.")', test)

    def test_el_listener_escucha_la_cola_de_cuentas(self):
        self.assertIn("@RabbitListener(queues = RabbitMQConfig.QUEUE_NOTIFICACIONES_AUTH)", leer(*NOTIF, "infrastructure", "amqp", "CuentaEventListener.java"))


class DockerfileYComposeTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.dockerfile = leer("auth-service", "Dockerfile")

    def test_hay_una_etapa_de_maven_antes_de_la_imagen_de_keycloak(self):
        etapas = re.findall(r"(?m)^FROM (\S+)(?: AS (\w+))?", self.dockerfile)
        self.assertEqual(len(etapas), 2)
        self.assertTrue(etapas[0][0].startswith("maven:3.9.6-eclipse-temurin-21") and etapas[0][1] == "spi")
        self.assertTrue(etapas[1][0].startswith("quay.io/keycloak/keycloak:"))

    def test_la_etapa_de_maven_no_usa_mount_porque_testcontainers_usa_el_constructor_clasico(self):
        self.assertNotRegex(re.sub(r"(?m)^#.*$", "", self.dockerfile), r"RUN --mount")

    def test_copia_el_pom_antes_que_las_fuentes_para_aprovechar_la_cache(self):
        self.assertLess(self.dockerfile.index("COPY spi/pom.xml"), self.dockerfile.index("dependency:go-offline"))
        self.assertLess(self.dockerfile.index("dependency:go-offline"), self.dockerfile.index("COPY spi/src"))

    def test_no_compila_los_tests_al_construir_la_imagen(self):
        self.assertIn("mvn -B -q package -Dmaven.test.skip=true", self.dockerfile)

    def test_el_jar_va_a_providers_antes_del_build_de_keycloak(self):
        self.assertIn("COPY --from=spi /spi/target/correo-spi.jar /opt/keycloak/providers/correo-spi.jar", self.dockerfile)
        self.assertLess(self.dockerfile.index("COPY --from=spi"), self.dockerfile.index("RUN /opt/keycloak/bin/kc.sh build"))

    def test_el_jar_se_llama_como_el_finalname_del_pom(self):
        self.assertIn("<finalName>correo-spi</finalName>", leer("auth-service", "spi", "pom.xml"))

    def test_el_proveedor_se_elige_al_construir_y_su_id_es_el_de_la_fabrica(self):
        # Es una opción de build: con `start --optimized`, un valor puesto solo al arrancar no se usa.
        self.assertIn("ENV KC_SPI_EMAIL_TEMPLATE__PROVIDER=donatrack-amqp", self.dockerfile)
        self.assertLess(self.dockerfile.index("KC_SPI_EMAIL_TEMPLATE__PROVIDER"), self.dockerfile.index("RUN /opt/keycloak/bin/kc.sh build"))
        self.assertIn('ID = "donatrack-amqp"', leer(*SPI, "CorreoPorAmqpProviderFactory.java"))

    def test_ningun_compose_pone_la_eleccion_del_proveedor_al_arrancar(self):
        for compose in COMPOSES:
            self.assertNotIn("KC_SPI_EMAIL_TEMPLATE__PROVIDER", leer(compose), compose)

    def test_el_dockerignore_deja_pasar_el_spi_y_excluye_su_target(self):
        patrones = [l.strip() for l in leer("auth-service", ".dockerignore").splitlines() if l.strip()]

        def excluido(ruta):
            return any(ruta == p or ruta.startswith(p + "/") for p in patrones)

        for ruta in ("spi/pom.xml", "spi/src/main/java/grupo5/auth/correo/EventoEnlaceDeCuenta.java"):
            self.assertFalse(excluido(ruta), "el build necesita %s" % ruta)
        self.assertTrue(excluido("spi/target/correo-spi.jar"))

    def test_keycloak_recibe_la_conexion_a_rabbitmq_y_la_misma_en_los_dos_compose(self):
        # El id `donatrack-amqp` en la variable de entorno: guiones a guiones bajos y en mayúsculas.
        prefijo = "KC_SPI_EMAIL_TEMPLATE__DONATRACK_AMQP__"
        por_compose = []
        for compose in COMPOSES:
            e = [x for x in entorno(bloque(compose, "keycloak")) if x.startswith(prefijo)]
            self.assertEqual(sorted(x.split("=")[0] for x in e), [prefijo + "HOST", prefijo + "PASSWORD", prefijo + "USER"], compose)
            self.assertIn(prefijo + "HOST=rabbitmq", e, compose)
            self.assertIn(prefijo + "USER=donatrack", e, compose)
            self.assertIn(prefijo + "PASSWORD=${RABBITMQ_PASS:?ver .env.example}", e, compose)
            por_compose.append(sorted(e))
        self.assertEqual(por_compose[0], por_compose[1])

    def test_el_host_y_el_usuario_son_los_del_servicio_de_rabbitmq(self):
        for compose in COMPOSES:
            rabbit = bloque(compose, "rabbitmq")
            self.assertIn("RABBITMQ_DEFAULT_USER=donatrack", rabbit, compose)

    def test_keycloak_no_espera_a_rabbitmq_para_arrancar(self):
        # Si RabbitMQ cae, Keycloak tiene que seguir andando: publica recién al pedirse un enlace.
        for compose in COMPOSES:
            self.assertNotRegex(bloque(compose, "keycloak"), r"depends_on:[\s\S]*rabbitmq", compose)

    def test_notificaciones_acepta_solo_enlaces_de_su_keycloak_y_con_el_mismo_prefijo_en_todos_lados(self):
        prefijo = "${KC_HOSTNAME:-http://localhost:8180}/realms/donatrack/login-actions/"
        for compose in COMPOSES:
            self.assertIn("KEYCLOAK_ENLACES_PREFIJO=" + prefijo, entorno(bloque(compose, "notificaciones-service")), compose)
        props = leer("notificaciones-service", "src", "main", "resources", "application.properties")
        self.assertIn("notificaciones.cuentas.prefijo-de-enlaces=${KEYCLOAK_ENLACES_PREFIJO:http://localhost:8180/realms/donatrack/login-actions/}", props)
        realm = json.loads(leer("auth-service", "realm", "donatrack-realm.json"))
        self.assertEqual(realm["realm"], "donatrack")

    def test_el_correo_simulado_imprime_el_enlace_solo_en_el_compose_local(self):
        variable = "LOGGING_LEVEL_GRUPO5_NOTIFICACIONES_INFRASTRUCTURE_ADAPTERS=DEBUG"
        self.assertIn(variable, entorno(bloque("docker-compose.yml", "notificaciones-service")))
        self.assertNotIn("LOGGING_LEVEL_GRUPO5_NOTIFICACIONES", leer("docker-compose.preprod.yml"))


class PomDelSpiTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.pom = leer("auth-service", "spi", "pom.xml")

    def test_es_un_proyecto_autonomo_sin_el_pom_raiz(self):
        self.assertNotIn("<parent>", self.pom)

    def test_compila_contra_la_misma_version_de_keycloak_que_la_imagen(self):
        version = re.search(r"<keycloak\.version>([^<]+)", self.pom).group(1)
        self.assertIn("quay.io/keycloak/keycloak:%s@" % version, leer("auth-service", "Dockerfile"))

    def test_las_clases_de_keycloak_son_provided_y_no_se_empaquetan(self):
        for artefacto in ("keycloak-core", "keycloak-server-spi", "keycloak-server-spi-private", "keycloak-services"):
            m = re.search(r"<artifactId>%s</artifactId>\s*<version>\$\{keycloak\.version\}</version>\s*<scope>provided</scope>" % artefacto, self.pom)
            self.assertIsNotNone(m, artefacto)

    def test_solo_el_cliente_de_rabbitmq_va_dentro_del_jar(self):
        self.assertRegex(self.pom, r"<artifactSet>\s*<includes>\s*<include>com\.rabbitmq:amqp-client</include>\s*</includes>\s*</artifactSet>")
        self.assertRegex(self.pom, r"<artifactId>slf4j-api</artifactId>\s*<version>[^<]+</version>\s*<scope>provided</scope>")

    def test_spotless_no_engancha_en_la_compilacion(self):
        # Un problema de formato no tiene que romper la construcción de la imagen: lo corre el CI aparte.
        spotless = re.search(r"<artifactId>spotless-maven-plugin</artifactId>(.*?)</plugin>", self.pom, re.S).group(1)
        self.assertNotIn("<executions>", spotless)
        self.assertIn("<version>1.22.0</version>", spotless)
        self.assertIn("<version>1.22.0</version>", leer("pom.xml"))

    def test_trae_las_librerias_de_test(self):
        self.assertRegex(self.pom, r"<artifactId>junit-jupiter</artifactId>\s*<version>[^<]+</version>\s*<scope>test</scope>")


class CiYRealmTests(unittest.TestCase):
    def test_el_job_de_auth_service_corre_el_formato_y_los_tests_del_spi_antes_del_contrato_del_realm(self):
        main = leer(".github", "workflows", "main.yml")
        paso = "mvn -B -f auth-service/spi/pom.xml spotless:check verify"
        self.assertIn(paso, main)
        self.assertLess(main.index(paso), main.index("mvn -B -f auth-service/pom.xml -Pcontrato-auth verify"))

    def test_el_realm_permite_recuperar_la_clave_y_el_enlace_vale_30_minutos(self):
        realm = json.loads(leer("auth-service", "realm", "donatrack-realm.json"))
        self.assertIs(realm["resetPasswordAllowed"], True)
        self.assertEqual(realm["actionTokenGeneratedByUserLifespan"], 1800)

    def test_el_vencimiento_del_realm_es_el_que_espera_el_script_e2e(self):
        e2e = leer("scripts", "e2e_correo_de_cuenta.py")
        self.assertIn('evento.get("venceEnMinutos") == 30', e2e)
        self.assertIn('str(props.get("expiration")) == "1800000"', e2e)

    def test_verify_email_sigue_apagado_y_no_hay_smtp_configurado(self):
        # Decisión de producto: activar verifyEmail cambia el registro y rompe el recorrido de Playwright.
        realm = json.loads(leer("auth-service", "realm", "donatrack-realm.json"))
        self.assertNotIn("verifyEmail", realm)
        self.assertNotIn("smtpServer", realm)


class PrivacidadTests(unittest.TestCase):
    """El enlace es un secreto y el email un dato personal: no pueden llegar a un log ni a la base."""

    ARCHIVOS_SPI = ("EventoEnlaceDeCuenta.java", "EmisorDeEnlacesDeCuenta.java", "PublicadorRabbitMq.java",
                    "CorreoPorAmqpProvider.java", "CorreoPorAmqpProviderFactory.java", "ConfiguracionAmqp.java")

    def test_ningun_log_del_spi_menciona_el_email_ni_el_enlace(self):
        for nombre in self.ARCHIVOS_SPI:
            for m in re.finditer(r"LOG\.\w+\((.*?)\);", leer(*SPI, nombre), re.S):
                self.assertNotRegex(m.group(1), r"destinatario|enlace|getEmail|aJson", "%s: %s" % (nombre, m.group(0)[:80]))

    def test_los_mensajes_de_error_del_spi_no_llevan_el_email_ni_el_enlace(self):
        for nombre in self.ARCHIVOS_SPI:
            for m in re.finditer(r"new (?:PublicacionFallida|EmailException)\((.*?)\);", leer(*SPI, nombre), re.S):
                self.assertNotRegex(m.group(1), r"destinatario\(\)|enlace\(\)|getEmail", "%s: %s" % (nombre, m.group(0)[:80]))

    def test_ningun_log_de_notificaciones_para_cuentas_menciona_el_email_ni_el_enlace(self):
        for ruta in (("services", "impl", "EnlaceDeCuentaService.java"), ("infrastructure", "amqp", "CuentaEventListener.java")):
            codigo = leer(*NOTIF, *ruta)
            for m in re.finditer(r"log\.\w+\((.*?)\);", codigo, re.S):
                self.assertNotRegex(m.group(1), r"destinatario|enlace\(\)|\bevento\)|\bevento,", "%s: %s" % (ruta[-1], m.group(0)[:90]))

    def test_el_servicio_de_cuentas_no_tiene_como_guardar_nada(self):
        codigo = leer(*NOTIF, "services", "impl", "EnlaceDeCuentaService.java")
        self.assertNotRegex(codigo, r"INotificacionRepository|\brepository\b|EntityManager|\.save\(|saveAll")
        self.assertNotIn("EventoNotificable", leer(*NOTIF, "dto", "input", "EventoCuentaEnlaceSolicitadoV1.java").split("public record")[1])

    def test_el_evento_oculta_el_email_y_el_enlace_en_su_to_string(self):
        record = leer(*NOTIF, "dto", "input", "EventoCuentaEnlaceSolicitadoV1.java")
        cuerpo = re.search(r"public String toString\(\) \{(.*?)\}", record, re.S).group(1)
        self.assertNotRegex(cuerpo, r"destinatario|enlace")

    def test_el_mensaje_expira_junto_con_el_enlace(self):
        self.assertIn('.expiration(String.valueOf(evento.venceEnMinutos() * 60_000L))', leer(*SPI, "PublicadorRabbitMq.java"))

    def test_el_publicador_pide_confirmacion_y_mandatory(self):
        codigo = leer(*SPI, "PublicadorRabbitMq.java")
        self.assertIn("canal.confirmSelect()", codigo)
        self.assertIn("canal.waitForConfirmsOrDie(config.timeoutMs())", codigo)
        self.assertRegex(codigo, r"basicPublish\(\s*config\.exchange\(\),\s*EventoEnlaceDeCuenta\.ROUTING_KEY,\s*true,")

    def test_solo_se_acepta_un_enlace_que_empiece_con_el_prefijo_de_keycloak(self):
        servicio = leer(*NOTIF, "services", "impl", "EnlaceDeCuentaService.java")
        self.assertIn("evento.enlace().startsWith(prefijoDeEnlaces)", servicio)
        self.assertLess(servicio.index("startsWith(prefijoDeEnlaces)"), servicio.index("yaProcesado(messageId)"),
                        "el chequeo del enlace va antes que el inbox: un enlace ajeno no debe ni consumir el id")


class PruebasTests(unittest.TestCase):
    def test_existen_los_tests_del_spi_y_de_notificaciones(self):
        spi = ("EventoEnlaceDeCuentaTest", "EmisorDeEnlacesDeCuentaTest", "PublicadorRabbitMqTest", "ConfiguracionAmqpTest",
               "CorreoPorAmqpProviderFactoryTest")
        for nombre in spi:
            self.assertTrue(os.path.isfile(os.path.join(RAIZ, "auth-service", "spi", "src", "test", "java", "grupo5", "auth", "correo", nombre + ".java")), nombre)
        base = ("notificaciones-service", "src", "test", "java", "grupo5", "notificaciones")
        for ruta in (("services", "impl", "MensajeDeCuentaTest.java"), ("services", "impl", "EnlaceDeCuentaServiceTest.java"),
                     ("infrastructure", "amqp", "CuentaEventListenerTest.java"),
                     ("infrastructure", "amqp", "EventoCuentaEnlaceSolicitadoContratoTest.java"),
                     ("dto", "input", "EventoCuentaEnlaceSolicitadoV1Test.java")):
            self.assertTrue(os.path.isfile(os.path.join(RAIZ, *base, *ruta)), ruta[-1])

    def test_los_tests_nuevos_no_usan_nombres_con_guion_bajo(self):
        carpetas = [os.path.join(RAIZ, "auth-service", "spi", "src", "test")]
        nuevos = ("MensajeDeCuentaTest", "EnlaceDeCuentaServiceTest", "CuentaEventListenerTest", "EventoCuentaEnlaceSolicitadoContratoTest",
                  "EventoCuentaEnlaceSolicitadoV1Test")
        for carpeta in carpetas:
            for ruta, _, archivos in os.walk(carpeta):
                for a in archivos:
                    self.assertNotRegex(leer_abs(os.path.join(ruta, a)), r"void [a-z]\w*_\w*\(", a)
        for ruta, _, archivos in os.walk(os.path.join(RAIZ, "notificaciones-service", "src", "test")):
            for a in archivos:
                if a[:-5] in nuevos:
                    self.assertNotRegex(leer_abs(os.path.join(ruta, a)), r"void [a-z]\w*_\w*\(", a)

    def test_los_assert_throws_de_una_sola_llamada_no_mezclan_varias_en_la_lambda(self):
        # Sonar (java:S5778): la lambda de assertThrows tiene que invocar una sola cosa que pueda lanzar.
        for ruta, _, archivos in os.walk(os.path.join(RAIZ, "auth-service", "spi", "src", "test")):
            for a in archivos:
                for m in re.finditer(r"assertThrows\([^,]+,\s*\(\) ->\s*\{", leer_abs(os.path.join(ruta, a))):
                    self.fail("%s: la lambda de assertThrows tiene un bloque: %s" % (a, m.group(0)))

    def test_el_script_e2e_lee_las_credenciales_del_env_y_esas_variables_existen(self):
        # Sin esto, en PowerShell hay que exportar variables a mano (la sintaxis VAR=valor cmd es de bash).
        e2e = leer("scripts", "e2e_correo_de_cuenta.py")
        env = leer(".env")
        for destino, origen in (("KC_URL", "KC_HOSTNAME"), ("KC_ADMIN_USER", "KC_BOOTSTRAP_ADMIN_USERNAME"),
                                ("KC_ADMIN_PASSWORD", "KC_BOOTSTRAP_ADMIN_PASSWORD"), ("RABBIT_PASS", "RABBITMQ_PASS")):
            self.assertIn('("%s", "%s")' % (destino, origen), e2e)
            self.assertRegex(env, r"(?m)^%s=\S+" % origen)
        self.assertIn("completar_desde_el_env()", e2e)
        self.assertLess(e2e.index("completar_desde_el_env()\nKC_URL"), e2e.index("ADMIN_PASSWORD = os.environ"))

    def test_el_script_e2e_existe_y_no_lo_descubre_unittest(self):
        ruta = os.path.join(RAIZ, "scripts", "e2e_correo_de_cuenta.py")
        self.assertTrue(os.path.isfile(ruta))
        self.assertFalse(os.path.basename(ruta).startswith("test_"), "necesita un stack: no debe correr con los tests estáticos")
        self.assertIn("COLA_ESPIA", leer("scripts", "e2e_correo_de_cuenta.py"))


if __name__ == "__main__":
    unittest.main()
