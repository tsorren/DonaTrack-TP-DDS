package grupo5.donaciones.models.repositories.impl;

import grupo5.common.repositories.CrudRepositoryEnMemoria;
import grupo5.donaciones.models.entities.donantes.Archivo;
import grupo5.donaciones.models.entities.donantes.FilaConError;
import grupo5.donaciones.models.repositories.IArchivoDonantesRepository;
import grupo5.donaciones.models.repositories.listados.CriterioArchivos;
import grupo5.donaciones.models.repositories.listados.OrdenPorFecha;
import grupo5.donaciones.models.repositories.listados.TextoBuscado;
import grupo5.donaciones.models.repositories.listados.Tramo;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

@Repository
@Profile("!postgres")
public class ArchivoDonantesRepositoryEnMemoria extends CrudRepositoryEnMemoria<Archivo>
    implements IArchivoDonantesRepository {

  // El importador escribe desde un hilo @Async mientras el GET lee.
  private final Map<UUID, List<FilaConError>> filasConError = new ConcurrentHashMap<>();

  @Override
  public void guardarFilasConError(List<FilaConError> filas) {
    // Por tanda y no por fila: cada addAll copia la lista una sola vez.
    filas.stream()
        .collect(Collectors.groupingBy(FilaConError::archivoId))
        .forEach(
            (archivoId, delArchivo) ->
                filasConError
                    .computeIfAbsent(archivoId, id -> new CopyOnWriteArrayList<>())
                    .addAll(delArchivo));
  }

  @Override
  public List<FilaConError> buscarFilasConError(UUID archivoId, int pagina, int tamanio) {
    return filasConError.getOrDefault(archivoId, List.of()).stream()
        .sorted(Comparator.comparingInt(FilaConError::linea))
        .skip((long) pagina * tamanio)
        .limit(tamanio)
        .toList();
  }

  /** Como la consulta del adaptador JPA: las que no tienen fecha, siempre al final. */
  @Override
  public Tramo<Archivo> buscar(CriterioArchivos criterio) {
    Comparator<LocalDateTime> porFecha = Comparator.naturalOrder();
    if (criterio.orden() == OrdenPorFecha.RECIENTES) {
      porFecha = porFecha.reversed();
    }
    boolean conFechas = criterio.desde() != null || criterio.hasta() != null;
    List<Archivo> filtrados =
        storage.values().stream()
            .filter(a -> criterio.estados().isEmpty() || criterio.estados().contains(a.getEstado()))
            .filter(a -> !conFechas || a.getSubidoEn() != null)
            .filter(
                a ->
                    criterio.desde() == null
                        || !a.getSubidoEn().toLocalDate().isBefore(criterio.desde()))
            .filter(
                a ->
                    criterio.hasta() == null
                        || !a.getSubidoEn().toLocalDate().isAfter(criterio.hasta()))
            .filter(
                a ->
                    criterio.q() == null
                        || TextoBuscado.contiene(a.getNombreOriginal(), criterio.q()))
            .sorted(
                Comparator.comparing(Archivo::getSubidoEn, Comparator.nullsLast(porFecha))
                    .thenComparing(Archivo::getId))
            .toList();
    return Tramo.de(filtrados, criterio.paginado());
  }

  /** Como el ON DELETE CASCADE de JPA: las filas con error se van con su archivo. */
  @Override
  public void delete(Archivo archivo) {
    super.delete(archivo);
    filasConError.remove(archivo.getId());
  }

  @Override
  public void deleteAll() {
    super.deleteAll();
    filasConError.clear();
  }
}
