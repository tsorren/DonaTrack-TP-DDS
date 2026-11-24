package grupo5.logistica.infrastructure.persistencia.mongo; // NOSONAR

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import grupo5.common.testing.DisabledIfDockerUnavailable;
import grupo5.logistica.infrastructure.persistencia.mongo.config.ShedLockMongoConfig;
import grupo5.logistica.schedulers.PlanificadorDeEntregas;
import grupo5.logistica.services.IPlanificacionService;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import net.javacrumbs.shedlock.core.LockProvider;
import org.bson.Document;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.data.mongodb.autoconfigure.DataMongoAutoConfiguration;
import org.springframework.boot.mongodb.autoconfigure.MongoAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.data.mongodb.core.MongoTemplate;

/**
 * Dos réplicas que disparan a la vez la misma tarea programada con el perfil {@code mongo}: con el
 * lock en la colección {@code shedLock} de la base, la tarea corre una sola vez.
 */
@DisabledIfDockerUnavailable
class ShedLockMongoTest {

  private static final String LOCK = "logistica.planificacion-entregas";

  @Test
  void dosEjecucionesSimultaneasDelMismoLock_correnUnaSolaVez() {
    IPlanificacionService servicio = mock(IPlanificacionService.class);
    CountDownLatch dentro = new CountDownLatch(1);
    CountDownLatch soltar = new CountDownLatch(1);
    doAnswer(
            invocacion -> {
              dentro.countDown();
              assertThat(soltar.await(30, TimeUnit.SECONDS)).isTrue();
              return null;
            })
        .when(servicio)
        .iniciarPlanificacion();

    new ApplicationContextRunner()
        .withConfiguration(
            AutoConfigurations.of(MongoAutoConfiguration.class, DataMongoAutoConfiguration.class))
        .withPropertyValues(
            "spring.profiles.active=mongo", "spring.mongodb.uri=" + MongoDePrueba.uri("shedlock"))
        .withUserConfiguration(ShedLockMongoConfig.class)
        .withBean(IPlanificacionService.class, () -> servicio)
        .withBean(PlanificadorDeEntregas.class)
        .run(
            contexto -> {
              assertThat(contexto).hasSingleBean(LockProvider.class);
              PlanificadorDeEntregas tarea = contexto.getBean(PlanificadorDeEntregas.class);

              CompletableFuture<Void> primera = CompletableFuture.runAsync(tarea::ejecutar);
              assertThat(dentro.await(30, TimeUnit.SECONDS)).isTrue();

              // La segunda llega mientras la primera tiene el lock: no corre y vuelve enseguida.
              CompletableFuture.runAsync(tarea::ejecutar).get(30, TimeUnit.SECONDS);
              soltar.countDown();
              primera.get(30, TimeUnit.SECONDS);

              verify(servicio, times(1)).iniciarPlanificacion();
              Document lock =
                  contexto
                      .getBean(MongoTemplate.class)
                      .getCollection("shedLock")
                      .find(new Document("_id", LOCK))
                      .first();
              assertThat(lock).isNotNull();
            });
  }
}
