package grupo5.logistica.infrastructure.persistencia.mongo.config; // NOSONAR

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.MongoTransactionManager;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@SuppressWarnings("java:S120")
@Configuration
@Profile("mongo")
@EnableMongoRepositories(
    basePackages = "grupo5.logistica.infrastructure.persistencia.mongo.repositories")
public class MongoPersistenciaConfig {

  /** Habilita {@code @Transactional} sobre MongoDB. Requiere replica set. */
  @Bean
  MongoTransactionManager transactionManager(MongoDatabaseFactory databaseFactory) {
    return new MongoTransactionManager(databaseFactory);
  }
}
