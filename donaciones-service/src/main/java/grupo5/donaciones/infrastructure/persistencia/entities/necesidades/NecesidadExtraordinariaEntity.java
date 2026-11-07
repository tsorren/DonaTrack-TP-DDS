package grupo5.donaciones.infrastructure.persistencia.entities.necesidades;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.Setter;

@Entity
@DiscriminatorValue("EXTRAORDINARIA")
@Getter
@Setter
public class NecesidadExtraordinariaEntity extends NecesidadEntity {}
