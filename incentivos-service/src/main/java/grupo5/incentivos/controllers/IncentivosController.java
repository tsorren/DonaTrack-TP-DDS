package grupo5.incentivos.controllers;

import grupo5.incentivos.dto.DonacionEventoRequest;
import grupo5.incentivos.dto.InsigniaDTO;
import grupo5.incentivos.dto.MetricasDonanteDTO;
import grupo5.incentivos.dto.MisionDTO;
import grupo5.incentivos.models.entities.donante.EventoDonacion;
import grupo5.incentivos.models.entities.ranking.RankingMensual;
import grupo5.incentivos.services.IncentivosService;
import grupo5.incentivos.services.IncentivosService.DonanteIncentivosNotFoundException;
import grupo5.incentivos.services.RankingService;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/incentivos")
public class IncentivosController {

  private final IncentivosService incentivosService;
  private final RankingService rankingService;

  public IncentivosController(IncentivosService incentivosService, RankingService rankingService) {
    this.incentivosService = incentivosService;
    this.rankingService = rankingService;
  }

  @PostMapping("/donaciones")
  public ResponseEntity<Void> procesarDonacion(@RequestBody DonacionEventoRequest request) {
    EventoDonacion evento =
        EventoDonacion.builder()
            .donacionId(request.donacionId())
            .organizacionId(request.organizacionId())
            .subcategoria(request.subcategoria())
            .cantidadBienes(request.cantidadBienes())
            .fecha(request.fecha())
            .exitosa(request.exitosa())
            .build();

    incentivosService.procesarDonacion(request.donanteId(), request.nombreUsuario(), evento);
    return ResponseEntity.ok().build();
  }

  @PostMapping("/donantes/{donanteId}")
  public ResponseEntity<MetricasDonanteDTO> registrarDonante(
      @PathVariable Long donanteId, @RequestParam String nombreUsuario) {
    var donante = incentivosService.registrarDonante(donanteId, nombreUsuario);
    return ResponseEntity.status(HttpStatus.CREATED).body(MetricasDonanteDTO.desde(donante));
  }

  @GetMapping("/donantes/{donanteId}/metricas")
  public ResponseEntity<MetricasDonanteDTO> obtenerMetricas(@PathVariable Long donanteId) {
    try {
      var donante = incentivosService.obtenerDonante(donanteId);
      return ResponseEntity.ok(MetricasDonanteDTO.desde(donante));
    } catch (DonanteIncentivosNotFoundException e) {
      return ResponseEntity.notFound().build();
    }
  }

  @GetMapping("/donantes/{donanteId}/misiones")
  public ResponseEntity<List<MisionDTO>> obtenerMisiones(@PathVariable Long donanteId) {
    try {
      var donante = incentivosService.obtenerDonante(donanteId);
      List<MisionDTO> misiones = donante.getMisiones().stream().map(MisionDTO::desde).toList();
      return ResponseEntity.ok(misiones);
    } catch (DonanteIncentivosNotFoundException e) {
      return ResponseEntity.notFound().build();
    }
  }

  @GetMapping("/donantes/{donanteId}/insignias")
  public ResponseEntity<List<InsigniaDTO>> obtenerInsignias(@PathVariable Long donanteId) {
    try {
      var donante = incentivosService.obtenerDonante(donanteId);
      List<InsigniaDTO> insignias =
          donante.getInsignias().stream().map(InsigniaDTO::desde).toList();
      return ResponseEntity.ok(insignias);
    } catch (DonanteIncentivosNotFoundException e) {
      return ResponseEntity.notFound().build();
    }
  }

  @PatchMapping("/donantes/{donanteId}/insignias/{nombreInsignia}/visibilidad")
  public ResponseEntity<Void> configurarVisibilidad(
      @PathVariable Long donanteId,
      @PathVariable String nombreInsignia,
      @RequestParam boolean visible) {
    try {
      incentivosService.configurarVisibilidadInsignia(donanteId, nombreInsignia, visible);
      return ResponseEntity.ok().build();
    } catch (DonanteIncentivosNotFoundException e) {
      return ResponseEntity.notFound().build();
    }
  }

  @GetMapping("/ranking/ultimo")
  public ResponseEntity<RankingMensual> obtenerUltimoRanking() {
    Optional<RankingMensual> ranking = rankingService.obtenerUltimoRanking();
    return ranking.map(ResponseEntity::ok).orElse(ResponseEntity.noContent().build());
  }

  @GetMapping("/ranking/historial")
  public ResponseEntity<List<RankingMensual>> obtenerHistorial() {
    return ResponseEntity.ok(rankingService.obtenerHistorial());
  }

  @PostMapping("/ranking/calcular")
  public ResponseEntity<RankingMensual> calcularRanking(
      @RequestParam(required = false) String periodo) {
    YearMonth yearMonth = periodo != null ? YearMonth.parse(periodo) : YearMonth.now();
    RankingMensual ranking = rankingService.calcularYPersistir(yearMonth);
    return ResponseEntity.ok(ranking);
  }
}
