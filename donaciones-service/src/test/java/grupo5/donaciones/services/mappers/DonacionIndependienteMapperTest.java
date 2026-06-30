package grupo5.donaciones.services.mappers;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import grupo5.donaciones.dto.donacionesIndependientes.*;
import grupo5.donaciones.models.entities.categorias.Categoria;
import grupo5.donaciones.models.entities.categorias.Subcategoria;
import grupo5.donaciones.models.entities.categorias.Unidad;
import grupo5.donaciones.models.entities.donaciones.Bien;
import grupo5.donaciones.models.entities.donaciones.Estado;
import grupo5.donaciones.models.entities.donacionesIndependientes.*;
import grupo5.donaciones.models.entities.itemsNormalizados.BienNormalizado;
import grupo5.donaciones.models.entities.itemsNormalizados.EstadoNormalizacion;
import grupo5.donaciones.models.repositories.ICategoriasRepository;
import grupo5.donaciones.models.repositories.ISubcategoriasRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DonacionIndependienteMapperTest {

  private ISubcategoriasRepository subcategoriasRepositoryMock;
  private ICategoriasRepository categoriasRepositoryMock;
  private DonacionIndependienteMapper mapper;

  @BeforeEach
  void setUp() {
    subcategoriasRepositoryMock = mock(ISubcategoriasRepository.class);
    categoriasRepositoryMock = mock(ICategoriasRepository.class);
    mapper = new DonacionIndependienteMapper(subcategoriasRepositoryMock, categoriasRepositoryMock);
  }

  @Test
  void toDTO_donacionNula_deberiaRetornarNull() {
    assertNull(mapper.toDTO(null));
  }

  @Test
  void toDTO_donacionValidaCompleta_deberiaMapearCorrectamente() {
    UUID originalId = UUID.randomUUID();
    Bien bienOriginal = new Bien("Arroz", "url_foto", LocalDate.now(), Estado.NUEVO);

    UUID subcategoriaId = UUID.randomUUID();
    BienNormalizado bienNormalizado =
        new BienNormalizado(bienOriginal, subcategoriaId, 0.9, EstadoNormalizacion.ACEPTADO);
    ItemDonacionIndependiente item = new ItemDonacionIndependiente(bienNormalizado, 5);

    List<ItemDonacionIndependiente> items = List.of(item);
    DonacionIndependiente donacion = new DonacionIndependiente(originalId, items);

    // mock subcategoria y categoria
    Subcategoria subcategoria = new Subcategoria(UUID.randomUUID(), "Alimento Seco");
    Categoria categoria = new Categoria("Alimentos", false, true, Unidad.KILOGRAMO);
    subcategoria.actualizar(categoria.getId(), "Alimento Seco");

    when(subcategoriasRepositoryMock.findById(subcategoriaId))
        .thenReturn(Optional.of(subcategoria));
    when(categoriasRepositoryMock.findById(categoria.getId())).thenReturn(Optional.of(categoria));

    DonacionIndependienteResponseDTO dto = mapper.toDTO(donacion);

    assertNotNull(dto);
    assertEquals(donacion.getId(), dto.id());
    assertEquals(originalId, dto.donacionOriginalId());
    assertEquals("Arroz ", dto.descripcion());
    assertEquals("EnDeposito", dto.estadoActual());
    assertEquals(donacion.getFechaRegistro(), dto.fechaRegistro());
    assertEquals(5, dto.cantidad());
    assertEquals(1, dto.items().size());

    ItemDonacionIndependienteResponseDTO itemDto = dto.items().getFirst();
    assertEquals(5, itemDto.cantidad());
    assertNotNull(itemDto.bien());
    assertEquals("Arroz", itemDto.bien().bien().descripcion());
    assertEquals("url_foto", itemDto.bien().bien().fotoUrl());
    assertEquals(Estado.NUEVO, itemDto.bien().bien().estado());

    assertEquals(subcategoria.getId(), itemDto.bien().subcategoria().id());
    assertEquals("Alimento Seco", itemDto.bien().subcategoria().nombre());

    assertEquals(categoria.getId(), itemDto.bien().categoria().id());
    assertEquals("Alimentos", itemDto.bien().categoria().nombre());
    assertEquals("KILOGRAMO", itemDto.bien().categoria().unidad());
  }

  @Test
  void toDTO_sinSubcategoria_deberiaMapearSubcategoriaYCategoriaComoNull() {
    UUID originalId = UUID.randomUUID();
    Bien bienOriginal = new Bien("Arroz", "url_foto", LocalDate.now(), Estado.NUEVO);
    BienNormalizado bienNormalizado =
        new BienNormalizado(bienOriginal, null, 0.9, EstadoNormalizacion.ACEPTADO);
    ItemDonacionIndependiente item = new ItemDonacionIndependiente(bienNormalizado, 3);

    DonacionIndependiente donacion = new DonacionIndependiente(originalId, List.of(item));

    DonacionIndependienteResponseDTO dto = mapper.toDTO(donacion);

    assertNotNull(dto);
    assertEquals(1, dto.items().size());
    assertNull(dto.items().getFirst().bien().subcategoria());
    assertNull(dto.items().getFirst().bien().categoria());
  }

  @Test
  void toDTO_subcategoriaInexistente_deberiaMapearSubcategoriaYCategoriaComoNull() {
    UUID originalId = UUID.randomUUID();
    Bien bienOriginal = new Bien("Arroz", "url_foto", LocalDate.now(), Estado.NUEVO);
    UUID subcategoriaId = UUID.randomUUID();
    BienNormalizado bienNormalizado =
        new BienNormalizado(bienOriginal, subcategoriaId, 0.9, EstadoNormalizacion.ACEPTADO);
    ItemDonacionIndependiente item = new ItemDonacionIndependiente(bienNormalizado, 3);

    DonacionIndependiente donacion = new DonacionIndependiente(originalId, List.of(item));

    when(subcategoriasRepositoryMock.findById(subcategoriaId)).thenReturn(Optional.empty());

    DonacionIndependienteResponseDTO dto = mapper.toDTO(donacion);

    assertNotNull(dto);
    assertEquals(1, dto.items().size());
    assertNull(dto.items().getFirst().bien().subcategoria());
    assertNull(dto.items().getFirst().bien().categoria());
  }

  @Test
  void toDTO_subcategoriaExistenteSinCategoria_deberiaMapearCategoriaComoNull() {
    UUID originalId = UUID.randomUUID();
    Bien bienOriginal = new Bien("Arroz", "url_foto", LocalDate.now(), Estado.NUEVO);
    UUID subcategoriaId = UUID.randomUUID();
    BienNormalizado bienNormalizado =
        new BienNormalizado(bienOriginal, subcategoriaId, 0.9, EstadoNormalizacion.ACEPTADO);
    ItemDonacionIndependiente item = new ItemDonacionIndependiente(bienNormalizado, 3);

    DonacionIndependiente donacion = new DonacionIndependiente(originalId, List.of(item));

    Subcategoria subcategoriaMock = mock(Subcategoria.class);
    when(subcategoriaMock.getId()).thenReturn(subcategoriaId);
    when(subcategoriaMock.getNombre()).thenReturn("Subcategoria Mock");
    when(subcategoriaMock.getCategoriaId()).thenReturn(null);

    when(subcategoriasRepositoryMock.findById(subcategoriaId))
        .thenReturn(Optional.of(subcategoriaMock));

    DonacionIndependienteResponseDTO dto = mapper.toDTO(donacion);

    assertNotNull(dto);
    assertEquals(1, dto.items().size());
    assertNotNull(dto.items().getFirst().bien().subcategoria());
    assertNull(dto.items().getFirst().bien().categoria());
  }

  @Test
  void toDTO_categoriaExistenteConYSinTipoUnidad_deberiaMapearCorrectamente() {
    UUID originalId = UUID.randomUUID();
    Bien bienOriginal = new Bien("Arroz", "url_foto", LocalDate.now(), Estado.NUEVO);
    UUID subcategoriaId = UUID.randomUUID();
    BienNormalizado bienNormalizado =
        new BienNormalizado(bienOriginal, subcategoriaId, 0.9, EstadoNormalizacion.ACEPTADO);
    ItemDonacionIndependiente item = new ItemDonacionIndependiente(bienNormalizado, 3);

    DonacionIndependiente donacion = new DonacionIndependiente(originalId, List.of(item));

    UUID categoriaId = UUID.randomUUID();
    Subcategoria subcategoria = new Subcategoria(categoriaId, "Alimento Seco");

    // Caso 1: Categoria con tipo de unidad nulo
    Categoria categoriaSinUnidad = mock(Categoria.class);
    when(categoriaSinUnidad.getId()).thenReturn(categoriaId);
    when(categoriaSinUnidad.getNombre()).thenReturn("Alimentos");
    when(categoriaSinUnidad.getTipoUnidad()).thenReturn(null);

    when(subcategoriasRepositoryMock.findById(subcategoriaId))
        .thenReturn(Optional.of(subcategoria));
    when(categoriasRepositoryMock.findById(categoriaId))
        .thenReturn(Optional.of(categoriaSinUnidad));

    DonacionIndependienteResponseDTO dto1 = mapper.toDTO(donacion);
    assertEquals("UNIDADES", dto1.items().getFirst().bien().categoria().unidad());

    // Caso 2: Categoria con tipo de unidad LITROS
    Categoria categoriaConUnidad = mock(Categoria.class);
    when(categoriaConUnidad.getId()).thenReturn(categoriaId);
    when(categoriaConUnidad.getNombre()).thenReturn("Liquidos");
    when(categoriaConUnidad.getTipoUnidad()).thenReturn(Unidad.LITROS);

    when(categoriasRepositoryMock.findById(categoriaId))
        .thenReturn(Optional.of(categoriaConUnidad));

    DonacionIndependienteResponseDTO dto2 = mapper.toDTO(donacion);
    assertEquals("LITROS", dto2.items().getFirst().bien().categoria().unidad());
  }

  @Test
  void toDTO_conHistorialDeEstados_deberiaMapearHistorialCorrectamente() {
    UUID originalId = UUID.randomUUID();
    Bien bienOriginal = new Bien("Arroz", "url_foto", LocalDate.now(), Estado.NUEVO);
    BienNormalizado bienNormalizado =
        new BienNormalizado(bienOriginal, UUID.randomUUID(), 0.9, EstadoNormalizacion.ACEPTADO);
    ItemDonacionIndependiente item = new ItemDonacionIndependiente(bienNormalizado, 1);

    DonacionIndependiente donacion = new DonacionIndependiente(originalId, List.of(item));

    DonacionIndependienteResponseDTO dto = mapper.toDTO(donacion);

    assertNotNull(dto);
    assertEquals(0, dto.historial().size());
  }
}
