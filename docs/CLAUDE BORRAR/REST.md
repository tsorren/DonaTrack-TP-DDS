## ¿QUÉ ES REST?

REST (Representational State Transfer) es un estilo arquitectónico para crear APIs web. Se basa en:

### Principios clave:

1. Recursos: Cada entidad (Donante, Donación, SubCategoria, etc.) es un recurso identificado por una URL  
2. Métodos HTTP: Usar verbos HTTP estándar para operaciones:  
   * GET → Obtener/Consultar  
   * POST → Crear  
   * PUT → Actualizar  
   * DELETE → Eliminar  
3. Stateless: Cada petición es independiente, sin estado guardado en el servidor  
4. Representaciones: Los datos se devuelven generalmente en JSON

### Ejemplo de URLs REST:

Code

GET    /api/donantes              → Obtener lista de donantes  
GET    /api/donantes/123          → Obtener donante con ID 123  
POST   /api/donantes              → Crear nuevo donante  
PUT    /api/donantes/123          → Actualizar donante 123  
DELETE /api/donantes/123          → Eliminar donante 123

---

## ESTRUCTURA GENERAL DE UN CONTROLLER REST EN TU PROYECTO

Basándome en lo que veo en tu proyecto (con Spring Boot), aquí está la estructura estándar:

"EjemploController.java"

package grupo5.donaciones.controllers;

import org.springframework.beans.factory.annotation.Autowired;  
import org.springframework.http.HttpStatus;  
import org.springframework.http.ResponseEntity;  
import org.springframework.web.bind.annotation.\*;  
import grupo5.donaciones.models.entities.Donante;  
import grupo5.donaciones.services.DonanteService;

@RestController  // ← Marca esta clase como controlador REST (devuelve JSON)  
@RequestMapping("/api/donantes")  // ← Ruta base para todos los endpoints  
public class DonanteController {

  @Autowired  // ← Inyección de dependencias (Spring instancia el service)  
  private DonanteService donanteService;

  // ═══════════════════════════════════════════════════════════  
  // GET \- OBTENER  
  // ═══════════════════════════════════════════════════════════  
    
  @GetMapping  // ← GET /api/donantes  
  public ResponseEntity\<List\<DonanteDTOResponse\>\> obtenerTodos() {  
    List\<Donante\> donantes \= donanteService.obtenerTodos();  
    List\<DonanteDTOResponse\> response \= donantes.stream()  
        .map(d \-\> new DonanteDTOResponse(d.getId(), d.getNombre()))  
        .collect(Collectors.toList());  
    return ResponseEntity.ok(response);  // ← 200 OK  
  }

  @GetMapping("/{id}")  // ← GET /api/donantes/123  
  public ResponseEntity\<DonanteDTOResponse\> obtenerPorId(@PathVariable Long id) {  
    Donante donante \= donanteService.obtenerPorId(id);  
    return ResponseEntity.ok(new DonanteDTOResponse(donante.getId(), donante.getNombre()));  
  }

  // ═══════════════════════════════════════════════════════════  
  // POST \- CREAR  
  // ═══════════════════════════════════════════════════════════  
    
  @PostMapping  // ← POST /api/donantes  
  public ResponseEntity\<DonanteDTOResponse\> crear(@RequestBody DonanteDTORequest request) {  
    // @RequestBody convierte el JSON a objeto Java  
    Donante donante \= donanteService.crear(request.getNombre());  
    return ResponseEntity.status(HttpStatus.CREATED)  // ← 201 Created  
        .body(new DonanteDTOResponse(donante.getId(), donante.getNombre()));  
  }

  // ═══════════════════════════════════════════════════════════  
  // PUT \- ACTUALIZAR  
  // ═══════════════════════════════════════════════════════════  
    
  @PutMapping("/{id}")  // ← PUT /api/donantes/123  
  public ResponseEntity\<DonanteDTOResponse\> actualizar(  
      @PathVariable Long id,  
      @RequestBody DonanteDTORequest request) {  
    Donante donante \= donanteService.actualizar(id, request.getNombre());  
    return ResponseEntity.ok(new DonanteDTOResponse(donante.getId(), donante.getNombre()));  
  }

  // ═══════════════════════════════════════════════════════════  
  // DELETE \- ELIMINAR  
  // ═══════════════════════════════════════════════════════════  
    
  @DeleteMapping("/{id}")  // ← DELETE /api/donantes/123  
  public ResponseEntity\<Void\> eliminar(@PathVariable Long id) {  
    donanteService.eliminar(id);  
    return ResponseEntity.noContent().build();  // ← 204 No Content  
  }  
}

## 

## COMPONENTES PRINCIPALES

### 1\. @RestController

Java

@RestController  
public class DonanteController {  
  // Automáticamente serializa respuestas a JSON  
}

### 2\. @RequestMapping (Ruta base)

Java

@RequestMapping("/api/donantes")  
// Todos los métodos usan /api/donantes como prefijo

### 3\. @GetMapping / @PostMapping / @PutMapping / @DeleteMapping

Java

@GetMapping        // GET /api/donantes  
@PostMapping       // POST /api/donantes  
@PutMapping("/{id}")    // PUT /api/donantes/123  
@DeleteMapping("/{id}") // DELETE /api/donantes/123

### 4\. @PathVariable (Parámetro en la URL)

Java

@GetMapping("/{id}")  
public ResponseEntity obtenerPorId(@PathVariable Long id) {  
  // El valor de {id} en la URL se pasa aquí  
}

### 5\. @RequestBody (Datos JSON en el cuerpo)

Java

@PostMapping  
public ResponseEntity crear(@RequestBody DonanteDTORequest request) {  
  // El JSON se convierte automáticamente al objeto  
}

### 6\. ResponseEntity (Respuesta con estado HTTP)

Java

ResponseEntity.ok(datos)                    // 200 OK  
ResponseEntity.status(HttpStatus.CREATED)   // 201 Created  
ResponseEntity.noContent().build()          // 204 No Content  
ResponseEntity.badRequest().body(error)     // 400 Bad Request

---

## 

## DTOs (Data Transfer Objects)

Se usan para separar lo que recibe/devuelve la API de la lógica interna:

"DonanteDTORequest.java"

@Getter  
@Setter  
public class DonanteDTORequest {  
  private String nombre;  
  private String apellido;  
  private String email;

## FLUJO DE UNA PETICIÓN REST

Code

Cliente (Frontend/Postman)  
    ↓  
GET /api/donantes/123  
    ↓  
@RestController recibe  
    ↓  
@GetMapping("/{id}") → obtenerPorId(@PathVariable Long id)  
    ↓  
Inyección: @Autowired DonanteService  
    ↓  
donanteService.obtenerPorId(id)  
    ↓  
Logica de negocio en Service  
    ↓  
ResponseEntity.ok(DTO)  
    ↓  
Spring serializa a JSON  
    ↓  
HTTP 200 OK \+ JSON  
    ↓  
Cliente recibe respuesta

