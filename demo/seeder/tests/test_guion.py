"""El guion: un mundo coherente para cualquier día en que se corra."""

import unittest
from collections import Counter
from datetime import date, timedelta

from seeder import catalogo, guion

DIAS = [date(2026, 11, 11), date(2026, 12, 3), date(2027, 1, 31), date(2027, 3, 1),
        date(2027, 6, 15)]
ORDEN = ["EN_DEPOSITO", "ASIGNACION_REALIZADA", "LISTA_PARA_ENTREGAR", "EN_TRASLADO",
         "EN_PUERTA", "ENTREGADA"]


def _recibido(mundo, necesidad):
    actual = necesidad.periodos[-1].id if necesidad.periodos else None
    return sum(i.cantidad - (i.cantidad_no_recibida or 0) for _, i in mundo.independientes()
               if i.necesidad == necesidad.id and i.estado_actual == "ENTREGADA"
               and (necesidad.tipo == "EXTRAORDINARIA" or i.periodo == actual))


class GuionTest(unittest.TestCase):
    def setUp(self):
        self.mundos = {hoy: guion.construir_mundo(hoy, 2026) for hoy in DIAS}

    def test_es_deterministico(self):
        otro = guion.construir_mundo(DIAS[0], 2026)
        self.assertEqual([d.id for d in otro.donaciones],
                         [d.id for d in self.mundos[DIAS[0]].donaciones])

    def test_fechas_dentro_de_la_ventana_y_estados_en_orden(self):
        for hoy, mundo in self.mundos.items():
            limite = guion._a_las(hoy, 23, 59)
            for donacion, ind in mundo.independientes():
                with self.subTest(hoy=hoy, donacion=donacion.id):
                    self.assertLess(donacion.fecha.date(), hoy)
                    self.assertGreaterEqual(donacion.fecha.date(), hoy - timedelta(days=200))
                    momentos = [p.momento for p in ind.pasos]
                    self.assertEqual(momentos, sorted(momentos))
                    self.assertLess(momentos[-1], limite)
                    self.assertEqual("EN_DEPOSITO", ind.pasos[0].estado)
                    if ind.estado_actual == "ENTREGADA":
                        self.assertIsNotNone(ind.necesidad)
                        self.assertIsNotNone(ind.id_entrega)
                        self.assertIsNotNone(ind.patente)
                        self.assertGreater(ind.fotos, 0)
                        self.assertEqual("EN_PUERTA", ind.pasos[-2].estado)

    def test_nada_dispara_los_jobs_nocturnos(self):
        for hoy, mundo in self.mundos.items():
            for _, ind in mundo.independientes():
                if ind.estado_actual == "EN_DEPOSITO" and ind.vencimiento:
                    self.assertGreater(ind.vencimiento, hoy)
            for necesidad in mundo.necesidades:
                if necesidad.tipo == "RECURRENTE":
                    self.assertGreater(necesidad.periodos[-1].fin.date(), hoy)
                    for a, b in zip(necesidad.periodos, necesidad.periodos[1:]):
                        self.assertEqual(a.fin, b.inicio)

    def test_las_necesidades_se_ven_en_el_estado_planeado(self):
        for hoy, mundo in self.mundos.items():
            for necesidad in mundo.necesidades:
                if necesidad.tipo != "EXTRAORDINARIA":
                    continue
                with self.subTest(hoy=hoy, necesidad=necesidad.clave):
                    cumplida = _recibido(mundo, necesidad) >= necesidad.cantidad
                    self.assertEqual(necesidad.estado_esperado == "CUMPLIDA", cumplida)
                    if necesidad.estado_esperado == "TERMINADA":
                        self.assertLess(necesidad.fecha_limite, hoy)
                    if necesidad.estado_esperado == "VIGENTE":
                        self.assertGreater(necesidad.fecha_limite, hoy)

    def test_la_estrella_es_sostenedora_y_le_falta_poco_para_constancia(self):
        for hoy, mundo in self.mundos.items():
            inc = mundo.incentivos["lucia"]
            with self.subTest(hoy=hoy):
                self.assertEqual("SOSTENEDOR", inc.categoria)
                self.assertEqual(["Racha Inicial", "Explorador", "Gran Aporte"],
                                 [m.insignia for m in inc.misiones if m.completada])
                constancia = inc.misiones[3]
                self.assertIn(constancia.progreso, (4, 5))

    def test_misiones_coherentes(self):
        for mundo in self.mundos.values():
            for clave, inc in mundo.incentivos.items():
                with self.subTest(donante=clave):
                    completadas = [m.numero for m in inc.misiones if m.completada]
                    self.assertEqual(completadas, list(range(1, len(completadas) + 1)))
                    self.assertEqual(len(completadas), len(inc.insignias))
                    for m in inc.misiones:
                        if m.completada:
                            self.assertIsNotNone(m.fecha_completada)
                        elif m.tipo != "HABIL":
                            self.assertLess(m.progreso, m.objetivo)
                    esperada = ("COLABORADOR" if completadas[:2] != [1, 2]
                                else "SOSTENEDOR" if completadas[:4] != [1, 2, 3, 4]
                                else "TRANSFORMADOR")
                    self.assertEqual(esperada, inc.categoria)
                    self.assertEqual(sum(inc.por_mes.values()), inc.total_donaciones)

    def test_entregas_y_rutas_cruzan_ids(self):
        for mundo in self.mundos.values():
            entregadas = {i.id for _, i in mundo.independientes()
                          if i.estado_actual == "ENTREGADA"}
            en_rutas = [e for r in mundo.rutas for e in r.entregas]
            self.assertEqual(len(en_rutas), len(set(en_rutas)))
            self.assertEqual(entregadas, set(en_rutas))
            patentes = {c.patente for c in mundo.camiones}
            for _, ind in mundo.independientes():
                if ind.patente:
                    self.assertIn(ind.patente, patentes)

    def test_elenco_sintetico(self):
        mundo = self.mundos[DIAS[0]]
        correos = [d.correo for d in mundo.donantes] + [e.correo for e in mundo.entidades]
        self.assertEqual(len(correos), len(set(correos)))
        for correo in correos:
            self.assertTrue(correo.endswith(guion.DOMINIO), correo)
        documentos = [d.documento for d in mundo.donantes] + [e.documento for e in mundo.entidades]
        self.assertEqual(len(documentos), len(set(documentos)))
        perfiles = Counter(d.perfil for d in mundo.donantes)
        self.assertEqual(1, perfiles["estrella"])
        self.assertEqual(1, perfiles["nuevo"])
        self.assertEqual({"activa", "pendiente", "baja"}, {e.estado for e in mundo.entidades})

    def test_hay_de_todo_para_las_pantallas(self):
        for hoy, mundo in self.mundos.items():
            estados = Counter(i.estado_actual for _, i in mundo.independientes())
            with self.subTest(hoy=hoy):
                self.assertGreater(estados["ENTREGADA"], 80)
                self.assertGreater(estados["EN_DEPOSITO"], 5)
                self.assertTrue(any(i.fallo for _, i in mundo.independientes()))
                self.assertTrue(any(i.reporte for _, i in mundo.independientes()))
                por_vencer = [i for _, i in mundo.independientes()
                              if i.estado_actual == "EN_DEPOSITO" and i.vencimiento
                              and i.vencimiento <= hoy + timedelta(days=3)]
                self.assertTrue(por_vencer)
                recibidas_comedor = [i for _, i in mundo.independientes()
                                     if i.entidad == "comedor" and i.estado_actual == "ENTREGADA"]
                self.assertGreater(len(recibidas_comedor), 10)

    def test_subcategorias_del_guion_estan_en_el_catalogo(self):
        nombres = set(catalogo.todas_las_subcategorias())
        for mundo in self.mundos.values():
            for necesidad in mundo.necesidades:
                self.assertIn(necesidad.subcategoria, nombres)


if __name__ == "__main__":
    unittest.main()
