package grupo5.common.testing;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Anotación que desactiva selectivamente una clase o método de prueba JUnit 5 cuando el socket de
 * Docker no se encuentra disponible en el entorno local, reportando la razón {@code
 * [DEFERRED_NO_DOCKER]}.
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@ExtendWith(DisabledIfDockerUnavailableCondition.class)
public @interface DisabledIfDockerUnavailable {}
