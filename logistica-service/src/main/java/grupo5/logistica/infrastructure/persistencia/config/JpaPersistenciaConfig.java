package grupo5.logistica.infrastructure.persistencia.config; // NOSONAR

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SuppressWarnings("java:S120")
@Configuration
@Profile("postgres")
@EnableJpaRepositories(basePackages = "grupo5.logistica.infrastructure.persistencia.repositories")
public class JpaPersistenciaConfig {}
