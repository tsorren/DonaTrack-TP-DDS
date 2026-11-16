package grupo5.cliente.sesion;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.time.Duration;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.session.config.SessionRepositoryCustomizer;
import org.springframework.session.jdbc.JdbcIndexedSessionRepository;
import org.springframework.session.jdbc.config.annotation.SpringSessionDataSource;
import org.springframework.session.jdbc.config.annotation.web.http.EnableJdbcHttpSession;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * La sesión del sitio en PostgreSQL (SPEC-06 E2-02, D5): el navegador solo lleva un id de sesión y
 * los tokens quedan en el servidor, en una base propia ({@code cliente}). Con dos instancias del
 * sitio, la sesión sobrevive a un cambio de instancia y a un reinicio (RNF-CL-04).
 *
 * <p>Se activa con {@code cliente.sesiones.jdbc.habilitado=true}. Apagada, la sesión vive en
 * memoria como siempre y el sitio no necesita base de datos: así siguen andando la maqueta y los
 * tests. Se arma a mano, sin el starter de sesión de Spring Boot, porque ese starter sumaría un
 * {@code DataSource} y un store de sesión a todos los contextos.
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "cliente.sesiones.jdbc.habilitado", havingValue = "true")
@EnableJdbcHttpSession
public class SesionesJdbcConfig {

  private static final String ESQUEMA = "org/springframework/session/jdbc/schema-postgresql.sql";

  @Bean(destroyMethod = "close")
  @SpringSessionDataSource
  public HikariDataSource sesionesDataSource(
      @Value("${cliente.sesiones.jdbc.url}") String url,
      @Value("${cliente.sesiones.jdbc.usuario}") String usuario,
      @Value("${cliente.sesiones.jdbc.clave}") String clave,
      @Value("${cliente.sesiones.jdbc.pool-maximo:5}") int poolMaximo) {
    HikariConfig config = new HikariConfig();
    config.setPoolName("sesiones");
    config.setJdbcUrl(url);
    config.setUsername(usuario);
    config.setPassword(clave);
    config.setMaximumPoolSize(poolMaximo);
    HikariDataSource dataSource = new HikariDataSource(config);
    crearLasTablasSiFaltan(dataSource);
    return dataSource;
  }

  @Bean
  public PlatformTransactionManager sesionesTransactionManager(
      @SpringSessionDataSource DataSource dataSource) {
    return new DataSourceTransactionManager(dataSource);
  }

  @Bean
  public SessionRepositoryCustomizer<JdbcIndexedSessionRepository> inactividadDeLaSesion(
      @Value("${cliente.sesiones.inactividad-minutos:30}") long minutos) {
    return repositorio -> repositorio.setDefaultMaxInactiveInterval(Duration.ofMinutes(minutos));
  }

  /**
   * Corre el script de tablas que trae Spring Session. Si las tablas ya existen, sus {@code CREATE}
   * fallan y se siguen de largo (con dos instancias que arrancan juntas, una de las dos siempre se
   * encuentra con las tablas hechas); lo que importa es que al final existan, y eso se comprueba.
   */
  static void crearLasTablasSiFaltan(DataSource dataSource) {
    ResourceDatabasePopulator esquema =
        new ResourceDatabasePopulator(new ClassPathResource(ESQUEMA));
    esquema.setContinueOnError(true);
    esquema.execute(dataSource);
    try {
      new JdbcTemplate(dataSource)
          .queryForObject("select count(*) from spring_session_attributes", Integer.class);
    } catch (DataAccessException e) {
      throw new IllegalStateException(
          "La base de sesiones no tiene las tablas de Spring Session y no se pudieron crear: "
              + "¿el rol puede crear tablas en su esquema?",
          e);
    }
  }
}
