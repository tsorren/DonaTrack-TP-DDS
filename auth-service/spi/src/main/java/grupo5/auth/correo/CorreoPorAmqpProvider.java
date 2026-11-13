package grupo5.auth.correo;

import java.util.Locale;
import org.keycloak.email.EmailException;
import org.keycloak.email.EmailTemplateProvider;
import org.keycloak.email.freemarker.FreeMarkerEmailTemplateProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

/**
 * El correo de la cuenta sale por RabbitMQ y no por SMTP (SPEC-06 E2-06, D8). Keycloak arma el
 * enlace de un solo uso y su vencimiento; acá, en lugar de renderizar y enviar el mail, se publica
 * un evento y {@code notificaciones-service} manda el correo con su propio contenido.
 *
 * <p>Solo cubre los dos correos que hacen falta: recuperar la clave y verificar el email. Los demás
 * (acciones requeridas por un admin, invitaciones, etc.) siguen el camino normal de Keycloak, que
 * sin un SMTP configurado falla: en DonaTrack no se usan.
 *
 * <p>Es una capa fina sobre {@link EmisorDeEnlacesDeCuenta}: lo que depende de Keycloak (la sesión,
 * el usuario, el idioma) se queda acá y se prueba con un Keycloak real.
 */
public class CorreoPorAmqpProvider extends FreeMarkerEmailTemplateProvider {

  static final String IDIOMA_POR_DEFECTO = "es";

  private final KeycloakSession sesion;
  private final EmisorDeEnlacesDeCuenta emisor;
  private UserModel usuario;
  private RealmModel reino;

  public CorreoPorAmqpProvider(KeycloakSession sesion, PublicadorDeEventos publicador) {
    super(sesion);
    this.sesion = sesion;
    this.emisor = new EmisorDeEnlacesDeCuenta(publicador);
  }

  @Override
  public EmailTemplateProvider setUser(UserModel user) {
    this.usuario = user;
    return super.setUser(user);
  }

  @Override
  public EmailTemplateProvider setRealm(RealmModel realm) {
    this.reino = realm;
    return super.setRealm(realm);
  }

  @Override
  public void sendPasswordReset(String link, long expirationInMinutes) throws EmailException {
    emisor.emitir(
        EventoEnlaceDeCuenta.RECUPERAR_CLAVE,
        usuario,
        idiomaDeLaCuenta(),
        link,
        expirationInMinutes);
  }

  @Override
  public void sendVerifyEmail(String link, long expirationInMinutes) throws EmailException {
    emisor.emitir(
        EventoEnlaceDeCuenta.VERIFICAR_EMAIL,
        usuario,
        idiomaDeLaCuenta(),
        link,
        expirationInMinutes);
  }

  /** El idioma en que la cuenta ve Keycloak; si no se resuelve, el del realm y, si no, español. */
  private String idiomaDeLaCuenta() {
    try {
      Locale idioma = sesion.getContext().resolveLocale(usuario);
      if (idioma != null && !idioma.getLanguage().isBlank()) {
        return idioma.getLanguage();
      }
    } catch (RuntimeException e) {
      // Sin contexto de request: se sigue con el idioma por defecto.
    }
    String delRealm = reino == null ? null : reino.getDefaultLocale();
    return delRealm == null || delRealm.isBlank() ? IDIOMA_POR_DEFECTO : delRealm;
  }
}
