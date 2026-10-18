package grupo5.logistica.infrastructure.persistencia.mongo.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@SuppressWarnings("java:S120")
@Configuration
@Profile("mongo")
@EnableMongoRepositories(
    basePackages = "grupo5.logistica.infrastructure.persistencia.mongo.repositories")
public class MongoPersistenciaConfig {}
