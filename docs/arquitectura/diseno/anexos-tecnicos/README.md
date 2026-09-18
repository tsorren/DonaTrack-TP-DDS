# Anexos Técnicos PlantUML — DonaTrack

> **Modelos Técnicos Autogenerados por Maven (`plantuml-generator-maven-plugin`)**  
> **Propósito:** Complemento técnico y trazabilidad exhaustiva de clases Java compiladas frente a los diagramas de diseño de dominio de alto nivel.

---

## 1. Dualidad Documental: Dominio vs. Modelo Técnico

En DonaTrack se preservan formalmente dos niveles de modelado en PlantUML:

1. **Diagramas de Diseño de Dominio (Canónicos):**
   - Ubicación: `docs/arquitectura/diseno/<servicio>/diagrama-de-clases-<servicio>.puml`
   - Enfoque: Modelado conceptual DDD (Agregados, Entidades, Value Objects, Patrones GoF: State, Strategy, Template Method, Puertos y Adaptadores).
   - Estilo: Paleta canónica importada mediante `!include ../common/donatrack-style.puml`.

2. **Modelos Técnicos Autogenerados (Anexos de Build):**
   - Ubicación: `docs/arquitectura/diseno/anexos-tecnicos/`
   - Enfoque: Inspección mecánica de bytecode Java (`target/classes`), visibilidad exacta de campos, métodos y herencia de clases compiladas.

---

## 2. Configuración en `pom.xml`

El plugin `plantuml-generator-maven-plugin` (versión `3.0.1`) está configurado a nivel de reactor para ejecutarse durante la fase `test`:

```xml
<plugin>
    <groupId>de.elnarion.maven</groupId>
    <artifactId>plantuml-generator-maven-plugin</artifactId>
    <version>${plantuml-generator.version}</version>
    <executions>
        <execution>
            <id>generate-plantuml</id>
            <phase>test</phase>
            <goals>
                <goal>generate</goal>
            </goals>
            <configuration>
                <outputDirectory>${project.build.directory}</outputDirectory>
                <outputFilename>modelo_tecnico.puml</outputFilename>
                <scanPackages>
                    <scanPackage>grupo5</scanPackage>
                </scanPackages>
                <hideFields>false</hideFields>
                <hideMethods>false</hideMethods>
            </configuration>
        </execution>
    </executions>
</plugin>
```

---

## 3. Comandos de Regeneración

Para regenerar los modelos técnicos tras refactors estructurales:

```bash
# Regeneración para common-lib
mvn test -pl common-lib

# Regeneración de reactor completo
mvn test
```

Los artefactos generados residen en `${project.build.directory}/modelo_tecnico.puml` de cada microservicio y pueden copiarse a este directorio como instantáneas de auditoría.
