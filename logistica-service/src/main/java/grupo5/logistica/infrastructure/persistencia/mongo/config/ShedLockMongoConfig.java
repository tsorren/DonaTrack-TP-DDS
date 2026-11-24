package grupo5.logistica.infrastructure.persistencia.mongo.config;

import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.mongo.MongoLockProvider;
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.mongodb.MongoDatabaseFactory;

/**
 * Una sola réplica corre cada tarea programada también con el perfil {@code mongo} (E7-S01d-1, ADR
 * de despliegue en Docker Swarm). Ese perfil excluye el {@code DataSource}, así que la
 * autoconfiguración JDBC de common-lib no se activa: el lock va en la colección {@code shedLock} de
 * la base de Logística, con el reloj del servidor de Mongo.
 */
@Configuration(proxyBeanMethods = false)
@Profile("mongo")
@EnableSchedulerLock(defaultLockAtMostFor = "PT30M")
public class ShedLockMongoConfig {

  @Bean
  LockProvider lockProvider(MongoDatabaseFactory databaseFactory) {
    return MongoLockProvider.create(databaseFactory.getMongoDatabase(), true);
  }
}
