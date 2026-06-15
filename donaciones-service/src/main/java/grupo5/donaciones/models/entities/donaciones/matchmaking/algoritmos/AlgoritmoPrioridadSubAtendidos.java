package grupo5.donaciones.models.entities.donaciones.matchmaking.algoritmos;

import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.ValidationException;
import grupo5.donaciones.models.entities.beneficiarios.EntidadBeneficiaria;
import grupo5.donaciones.models.entities.donacionesIndependientes.DonacionIndependiente;
import grupo5.donaciones.models.entities.necesidades.Necesidad;
import grupo5.donaciones.models.repositories.NecesidadRepository;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AlgoritmoPrioridadSubAtendidos extends AlgoritmoAsignacion {

    private final NecesidadRepository necesidadRepository;

    public AlgoritmoPrioridadSubAtendidos(NecesidadRepository necesidadRepository) {
        this.necesidadRepository = necesidadRepository;
    }

    @Override
    public List<Necesidad> ordenarNecesidades(List<Necesidad> necesidades) {
        if (necesidades == null) throw new ValidationException(ErrorCatalog.ALGORITMO_NECESIDADES_NULAS);

        LocalDate hace3meses = LocalDate.now().minusMonths(3);
        Map<EntidadBeneficiaria, Double> tasasSatisfaccion = calcularTasasSatisfaccionPorEntidad(hace3meses);

        return ordenarPorTasaAscendente(necesidades, tasasSatisfaccion);
    }

    @Override
    public List<DonacionIndependiente> filtrarDonaciones(
            Necesidad necesidad, List<DonacionIndependiente> donaciones) {
        validarParametrosFiltrado(necesidad, donaciones);
        List<DonacionIndependiente> filtradas = new ArrayList<>();
        for (DonacionIndependiente donacion : donaciones) {
            if (mismaSubcategoria(donacion, necesidad)) {
                filtradas.add(donacion);
            }
        }
        return filtradas;
    }

    private Map<EntidadBeneficiaria, Double> calcularTasasSatisfaccionPorEntidad(LocalDate desde) {
        Map<EntidadBeneficiaria, Integer> totalesPorEntidad = new HashMap<>();
        Map<EntidadBeneficiaria, Integer> satisfechasPorEntidad = new HashMap<>();

        for (Necesidad necesidad : necesidadRepository.findAll()) {
            if (necesidad.getEntidad() == null) continue;
            if (necesidad.getFechaInicio() == null) continue;
            if (!necesidad.getFechaInicio().isAfter(desde)) continue;

            EntidadBeneficiaria entidad = necesidad.getEntidad();
            totalesPorEntidad.put(entidad, totalesPorEntidad.getOrDefault(entidad, 0) + 1);
            if (necesidad.estaSatisfecha()) {
                satisfechasPorEntidad.put(entidad, satisfechasPorEntidad.getOrDefault(entidad, 0) + 1);
            }
        }

        Map<EntidadBeneficiaria, Double> tasas = new HashMap<>();
        for (EntidadBeneficiaria entidad : totalesPorEntidad.keySet()) {
            int total = totalesPorEntidad.get(entidad);
            int satisfechas = satisfechasPorEntidad.getOrDefault(entidad, 0);
            tasas.put(entidad, (double) satisfechas / total);
        }
        return tasas;
    }

    private List<Necesidad> ordenarPorTasaAscendente(
            List<Necesidad> necesidades,
            Map<EntidadBeneficiaria, Double> tasas) {
        List<Necesidad> ordenadas = new ArrayList<>(necesidades);
        int n = ordenadas.size();
        for (int i = 0; i < n - 1; i++) {
            int minIdx = i;
            for (int j = i + 1; j < n; j++) {
                double tasaMin = tasas.getOrDefault(ordenadas.get(minIdx).getEntidad(), 0.0);
                double tasaJ = tasas.getOrDefault(ordenadas.get(j).getEntidad(), 0.0);
                if (tasaJ < tasaMin) {
                    minIdx = j;
                }
            }
            Necesidad temp = ordenadas.get(i);
            ordenadas.set(i, ordenadas.get(minIdx));
            ordenadas.set(minIdx, temp);
        }
        return ordenadas;
    }
}
