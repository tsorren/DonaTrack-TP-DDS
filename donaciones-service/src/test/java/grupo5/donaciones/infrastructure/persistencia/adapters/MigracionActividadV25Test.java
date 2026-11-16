package grupo5.donaciones.infrastructure.persistencia.adapters;

import static org.junit.jupiter.api.Assertions.assertEquals;

import grupo5.common.testing.DisabledIfDockerUnavailable;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * R11-26 (C8 de la spec): V25 copia al registro las propuestas ya decididas con autor real, con la
 * razón social de su organización, y deja afuera las de SISTEMA, las sin fecha y las pendientes.
 */
@Testcontainers
@DisabledIfDockerUnavailable
class MigracionActividadV25Test {

  @Container
  static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

  private static final String CATEGORIA = "11111111-1111-1111-1111-111111111111";
  private static final String SUBCATEGORIA = "22222222-2222-2222-2222-222222222222";
  private static final String ORGANIZACION = "33333333-3333-3333-3333-333333333333";
  private static final String NECESIDAD = "44444444-4444-4444-4444-444444444444";

  private Flyway flyway(String hasta) {
    return Flyway.configure()
        .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
        .schemas("donaciones")
        .locations("classpath:db/migration")
        .target(hasta)
        .load();
  }

  @Test
  void copiaSoloLasDecididasConAutorReal() throws SQLException {
    flyway("24").migrate();
    try (Connection conexion =
            DriverManager.getConnection(
                postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
        Statement sql = conexion.createStatement()) {
      sql.execute("SET search_path TO donaciones");
      sql.execute(
          "INSERT INTO categoria (id, nombre, con_uso, con_vencimiento, tipo_unidad) VALUES ('"
              + CATEGORIA
              + "', 'Alimentos', false, true, 'KILOGRAMO')");
      sql.execute(
          "INSERT INTO subcategoria (id, id_categoria, nombre) VALUES ('"
              + SUBCATEGORIA
              + "', '"
              + CATEGORIA
              + "', 'Arroz')");
      sql.execute(
          "INSERT INTO persona (id, tipo_persona) VALUES ('" + ORGANIZACION + "', 'JURIDICA')");
      sql.execute(
          "INSERT INTO juridica (id, razon_social, tipo_juridico, rubro) VALUES ('"
              + ORGANIZACION
              + "', 'Comedor Barrio Ejemplo', 'ONG', 'Comedor')");
      sql.execute("INSERT INTO entidad_beneficiaria (id) VALUES ('" + ORGANIZACION + "')");
      sql.execute(
          "INSERT INTO necesidad (id, tipo_necesidad, subcategoria_id, entidad_beneficiaria_id,"
              + " cantidad_necesitada, descripcion, fecha_inicio) VALUES ('"
              + NECESIDAD
              + "', 'EXTRAORDINARIA', '"
              + SUBCATEGORIA
              + "', '"
              + ORGANIZACION
              + "', 10, 'Arroz', '2026-11-01 10:00')");
      propuesta(sql, "a0000000-0000-0000-0000-000000000001", "APROBADA", "'sub-ana'", true);
      propuesta(sql, "a0000000-0000-0000-0000-000000000002", "DESCARTADA", "'sub-bruno'", true);
      propuesta(sql, "a0000000-0000-0000-0000-000000000003", "APROBADA", "'SISTEMA'", true);
      propuesta(sql, "a0000000-0000-0000-0000-000000000004", "APROBADA", "'sub-ana'", false);
      propuesta(sql, "a0000000-0000-0000-0000-000000000005", "APROBADA", "NULL", true);
      propuesta(sql, "a0000000-0000-0000-0000-000000000006", "PENDIENTE", "NULL", false);
    }

    flyway("25").migrate();

    List<String> filas = new ArrayList<>();
    try (Connection conexion =
            DriverManager.getConnection(
                postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
        Statement sql = conexion.createStatement();
        ResultSet leidas =
            sql.executeQuery(
                "SELECT objeto_id, autor, accion, detalle, ocurrio_en"
                    + " FROM donaciones.actividad_administracion ORDER BY objeto_id")) {
      while (leidas.next()) {
        filas.add(
            String.join(
                " | ",
                leidas.getString(1),
                leidas.getString(2),
                leidas.getString(3),
                leidas.getString(4),
                leidas.getString(5)));
      }
    }
    assertEquals(
        List.of(
            "a0000000-0000-0000-0000-000000000001 | sub-ana | PROPUESTA_APROBADA"
                + " | Comedor Barrio Ejemplo | 2026-11-11 15:30:00",
            "a0000000-0000-0000-0000-000000000002 | sub-bruno | PROPUESTA_DESCARTADA"
                + " | Comedor Barrio Ejemplo | 2026-11-11 15:30:00"),
        filas);
  }

  private static void propuesta(
      Statement sql, String id, String estado, String decididaPor, boolean conFecha)
      throws SQLException {
    sql.execute(
        "INSERT INTO propuesta (id, id_necesidad_que_satisface, estado, fecha_creacion,"
            + " decidida_por, fecha_decision) VALUES ('"
            + id
            + "', '"
            + NECESIDAD
            + "', '"
            + estado
            + "', '2026-11-10 09:00', "
            + decididaPor
            + ", "
            + (conFecha ? "'2026-11-11 15:30'" : "NULL")
            + ")");
  }
}
