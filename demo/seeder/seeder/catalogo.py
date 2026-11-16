"""Catálogo de la demo: las 3 categorías de arranque más 5 que suma el seeder, y los bienes de
ejemplo de cada subcategoría.

Las descripciones de los bienes tienen que normalizarse solas (confianza 1.0): cada una contiene
todas las palabras de un alias de su subcategoría y de ninguna otra. Lo controla
tests/test_catalogo.py con la misma comparación que usa Donaciones.
"""

from __future__ import annotations

from dataclasses import dataclass, field


@dataclass(frozen=True)
class Bien:
    descripcion: str
    peso_kg: float
    volumen_m3: float
    minimo: int
    maximo: int


@dataclass(frozen=True)
class Subcategoria:
    nombre: str
    alias: tuple[str, ...]
    bienes: tuple[Bien, ...]


@dataclass(frozen=True)
class Categoria:
    nombre: str
    con_uso: bool
    con_vencimiento: bool
    unidad: str
    subcategorias: tuple[Subcategoria, ...]
    # Las de arranque las crea Donaciones (CatalogDataInitializer); el seeder solo suma las nuevas
    de_arranque: bool = field(default=False)


def _b(descripcion: str, peso: float, volumen: float, minimo: int, maximo: int) -> Bien:
    return Bien(descripcion, peso, volumen, minimo, maximo)


CATALOGO: tuple[Categoria, ...] = (
    Categoria("Alimentos", False, True, "KILOGRAMO", (
        Subcategoria("No Perecederos", (
            "arroz", "fideos", "alimentos", "lentejas", "porotos", "comida",
            "alimentos no perecederos"), (
            _b("Paquetes de arroz largo fino", 1.0, 0.0015, 15, 60),
            _b("Fideos secos tipo tirabuzón", 0.5, 0.0012, 20, 80),
            _b("Lentejas secas en bolsa", 1.0, 0.0015, 10, 40),
            _b("Porotos alubia en bolsa", 1.0, 0.0015, 10, 40),
        )),
        Subcategoria("Frutas", ("manzana", "banana", "fruta"), (
            _b("Manzana roja de estación", 1.0, 0.002, 10, 40),
            _b("Banana ecuatoriana", 1.0, 0.002, 10, 35),
        )),
    ), de_arranque=True),
    Categoria("Ropa", True, False, "UNIDADES", (
        Subcategoria("Ropa de Invierno", ("pantalon", "abrigo", "campera", "buzo", "ropa"), (
            _b("Camperas de abrigo talles surtidos", 0.9, 0.01, 5, 25),
            _b("Buzo de frisa para chicos", 0.5, 0.006, 8, 30),
            _b("Pantalon de jean talle 40", 0.6, 0.004, 5, 20),
        )),
        Subcategoria("Ropa de Verano", ("remera", "short", "malla"), (
            _b("Remera de algodón lisa", 0.2, 0.002, 10, 40),
            _b("Short deportivo de microfibra", 0.2, 0.002, 8, 30),
        )),
    ), de_arranque=True),
    Categoria("Muebles", True, False, "UNIDADES", (
        Subcategoria("Muebles Escolares", ("banco", "pizarron", "silla escolar"), (
            _b("Silla escolar de madera", 4.5, 0.12, 4, 20),
            _b("Pizarrón blanco de 120 x 80", 6.0, 0.08, 1, 4),
        )),
        Subcategoria("Muebles de Oficina", ("escritorio", "silla de oficina"), (
            _b("Escritorio de melamina con cajonera", 25.0, 0.45, 1, 5),
        )),
        Subcategoria("Mesa de luz", ("mesa de luz",), (
            _b("Mesa de luz con cajón", 9.0, 0.09, 1, 6),
        )),
        Subcategoria("Velador", ("velador",), (
            _b("Velador de pie con pantalla", 2.0, 0.03, 1, 6),
        )),
    ), de_arranque=True),
    Categoria("Higiene", False, False, "UNIDADES", (
        Subcategoria("Higiene personal", (
            "shampoo", "desodorante", "pasta dental", "jabon de tocador"), (
            _b("Shampoo familiar de 930 ml", 1.0, 0.0012, 12, 48),
            _b("Pasta dental de 90 g", 0.1, 0.0002, 24, 96),
            _b("Jabón de tocador por 3", 0.3, 0.0004, 12, 60),
        )),
        Subcategoria("Limpieza del hogar", ("lavandina", "detergente", "desinfectante"), (
            _b("Lavandina en bidón de 5 litros", 5.2, 0.006, 4, 20),
            _b("Detergente concentrado de 750 ml", 0.8, 0.001, 12, 48),
        )),
    )),
    Categoria("Útiles escolares", False, False, "UNIDADES", (
        Subcategoria("Cuadernos y papelería", ("cuaderno", "cuadernos", "resma", "lapices"), (
            _b("Cuadernos de 48 hojas rayadas", 0.2, 0.0006, 30, 120),
            _b("Resma de papel A4", 2.5, 0.003, 5, 20),
            _b("Caja de lápices de colores", 0.2, 0.0004, 20, 80),
        )),
        Subcategoria("Mochilas y cartucheras", (
            "mochila", "mochilas", "cartuchera", "cartucheras"), (
            _b("Mochilas escolares con carro", 1.2, 0.025, 5, 25),
            _b("Cartucheras de dos pisos", 0.3, 0.002, 10, 40),
        )),
    )),
    Categoria("Juguetes", True, False, "UNIDADES", (
        Subcategoria("Juegos de mesa", ("rompecabezas", "juego de mesa", "juegos de mesa"), (
            _b("Rompecabezas de 500 piezas", 0.6, 0.003, 4, 20),
            _b("Juegos de mesa para la familia", 0.9, 0.004, 3, 15),
        )),
        Subcategoria("Peluches y pelotas", ("peluche", "peluches", "pelota", "pelotas"), (
            _b("Peluches medianos surtidos", 0.3, 0.006, 6, 30),
            _b("Pelotas de fútbol número 5", 0.45, 0.006, 4, 20),
        )),
    )),
    Categoria("Blanquería", True, False, "UNIDADES", (
        Subcategoria("Ropa de cama", ("frazada", "frazadas", "acolchado", "sabanas"), (
            _b("Frazadas de dos plazas", 1.8, 0.02, 5, 30),
            _b("Juego de sábanas de una plaza", 0.8, 0.004, 5, 25),
        )),
        Subcategoria("Toallas", ("toalla", "toallas", "toallon"), (
            _b("Toallas de mano de algodón", 0.2, 0.002, 10, 40),
            _b("Toallón de baño", 0.5, 0.004, 6, 24),
        )),
    )),
    Categoria("Electrodomésticos", True, False, "UNIDADES", (
        Subcategoria("Cocina", ("heladera", "microondas", "anafe"), (
            _b("Heladera con freezer", 55.0, 0.75, 1, 2),
            _b("Microondas de 20 litros", 11.0, 0.06, 1, 3),
        )),
        Subcategoria("Calefacción", ("estufa", "caloventor"), (
            _b("Estufa a gas de tiro balanceado", 14.0, 0.09, 1, 4),
            _b("Caloventor eléctrico de 2000 W", 1.5, 0.01, 2, 8),
        )),
    )),
)


def categoria_de(nombre_subcategoria: str) -> Categoria:
    for categoria in CATALOGO:
        if any(s.nombre == nombre_subcategoria for s in categoria.subcategorias):
            return categoria
    raise KeyError(nombre_subcategoria)


def subcategoria(nombre: str) -> Subcategoria:
    for categoria in CATALOGO:
        for sub in categoria.subcategorias:
            if sub.nombre == nombre:
                return sub
    raise KeyError(nombre)


def todas_las_subcategorias() -> list[str]:
    return [s.nombre for c in CATALOGO for s in c.subcategorias]
