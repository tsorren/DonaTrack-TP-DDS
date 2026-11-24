package grupo5.common.shedlock;

import javax.sql.DataSource;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider;
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.config.TaskManagementConfigUtils;
import org.springframework.util.StringUtils;

/**
 * Una sola réplica corre cada tarea programada (E7-S01d-1, ADR de despliegue en Docker Swarm): los
 * métodos con {@code @SchedulerLock} toman un lock en la tabla {@code shedlock} del esquema del
 * servicio, que crea su migración de Flyway.
 *
 * <p>Se activa solo si el servicio tiene un {@link DataSource} y programa tareas
 * ({@code @EnableScheduling}). Sin base (perfil en memoria, gateway) o sin tareas (Notificaciones),
 * no crea nada y {@code @SchedulerLock} no tiene efecto. Logística con el perfil {@code mongo} no
 * tiene base relacional: su lock lo arma su propia configuración, sobre Mongo.
 */
@AutoConfiguration(
    afterName = "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration")
// Por nombre: common-lib trae ShedLock como optional y el servicio que no lo declara no tiene estas
// clases.
@ConditionalOnClass(
    name = {
      "org.springframework.jdbc.core.JdbcTemplate",
      "net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider",
      "net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock"
    })
@ConditionalOnBean(
    value = DataSource.class,
    name = TaskManagementConfigUtils.SCHEDULED_ANNOTATION_PROCESSOR_BEAN_NAME)
@EnableSchedulerLock(defaultLockAtMostFor = "PT30M")
public class ShedLockAutoConfiguration {

  private static final String TABLA = "shedlock";

  @Bean
  @ConditionalOnMissingBean(LockProvider.class)
  public LockProvider lockProvider(DataSource dataSource, Environment entorno) {
    return new JdbcTemplateLockProvider(
        JdbcTemplateLockProvider.Configuration.builder()
            .withJdbcTemplate(new JdbcTemplate(dataSource))
            .withTableName(tabla(entorno))
            .usingDbTime()
            .build());
  }

  /**
   * La tabla en el esquema donde la crea Flyway (el {@code default-schema} o el primero de {@code
   * schemas}), para no depender de que la URL traiga {@code currentSchema}. Sin esquema de Flyway,
   * queda sin calificar.
   */
  static String tabla(Environment entorno) {
    String esquema = entorno.getProperty("spring.flyway.default-schema");
    if (!StringUtils.hasText(esquema)) {
      String[] esquemas = entorno.getProperty("spring.flyway.schemas", String[].class);
      esquema = esquemas != null && esquemas.length > 0 ? esquemas[0].trim() : null;
    }
    return StringUtils.hasText(esquema) ? esquema + "." + TABLA : TABLA;
  }
}
