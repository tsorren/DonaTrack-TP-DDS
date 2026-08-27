package grupo5.incentivos.infrastructure;

import java.util.UUID;

public interface INotificacionesClient {

  void notificarMisionCumplida(UUID idPersona, String nombreMision, String recompensa);

  void notificarAscensoCategoria(UUID idPersona, String categoriaNueva, String categoriaVieja);

  void notificarInactividad(UUID idPersona, int diasInactivo);
}
