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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.config.TaskManagementConfigUtils;

/**
 * Una sola réplica corre cada tarea programada (E7-S01d-1, ADR de despliegue en Docker Swarm): los
 * métodos con {@code @SchedulerLock} toman un lock en la tabla {@code shedlock} del esquema del
 * servicio, que crea su migración de Flyway.
 *
 * <p>Se activa solo si el servicio tiene un {@link DataSource} y programa tareas
 * ({@code @EnableScheduling}). Sin base (perfil en memoria, gateway) o sin tareas (Notificaciones),
 * no crea nada y {@code @SchedulerLock} no tiene efecto.
 */
@AutoConfiguration(
    afterName = "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration")
@ConditionalOnClass({JdbcTemplate.class, JdbcTemplateLockProvider.class})
@ConditionalOnBean(
    value = DataSource.class,
    name = TaskManagementConfigUtils.SCHEDULED_ANNOTATION_PROCESSOR_BEAN_NAME)
@EnableSchedulerLock(defaultLockAtMostFor = "PT30M")
public class ShedLockAutoConfiguration {

  @Bean
  @ConditionalOnMissingBean(LockProvider.class)
  public LockProvider lockProvider(DataSource dataSource) {
    return new JdbcTemplateLockProvider(
        JdbcTemplateLockProvider.Configuration.builder()
            .withJdbcTemplate(new JdbcTemplate(dataSource))
            .usingDbTime()
            .build());
  }
}
