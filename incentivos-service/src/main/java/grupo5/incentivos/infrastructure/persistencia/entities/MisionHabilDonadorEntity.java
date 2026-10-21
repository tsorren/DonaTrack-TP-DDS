package grupo5.incentivos.infrastructure.persistencia.entities;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@DiscriminatorValue("HABIL")
@Getter
@Setter
@NoArgsConstructor
public class MisionHabilDonadorEntity extends MisionEntity {}
