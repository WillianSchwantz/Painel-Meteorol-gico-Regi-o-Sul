package service;

import dao.MedicaoDAO;
import model.MedicaoDiaria;
import model.TendenciaChuva;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/**
 * Calcula a tendência da chuva por regressão linear simples.
 */
public class ChuvaTendenciaService {

    private final MedicaoDAO medicaoDAO;

    public ChuvaTendenciaService() {
        this(new MedicaoDAO());
    }

    public ChuvaTendenciaService(MedicaoDAO medicaoDAO) {
        this.medicaoDAO = Objects.requireNonNull(medicaoDAO, "MedicaoDAO não pode ser nulo.");
    }

    /**
     * Calcula a tendência de chuva nos últimos 'diasJanela' a partir da data de referência.
     * Retorna INSUFICIENTE se não houver dados o bastante.
     */
    public TendenciaChuva calcularTendencia(
            String stationId,
            LocalDate referencia,
            int diasJanela
    ) throws SQLException {
        if (stationId == null || stationId.isBlank()) {
            throw new IllegalArgumentException("Identificador da estação é obrigatório.");
        }
        Objects.requireNonNull(referencia, "A data de referência é obrigatória.");
        
        LocalDate inicio = referencia.minusDays(diasJanela - 1L);
        List<MedicaoDiaria> medicoes = medicaoDAO.listarDiariasPorEstacao(stationId, inicio, referencia);
        
        // Verifica se há pelo menos a metade dos dados esperados para gerar tendência
        int count = 0;
        for (MedicaoDiaria m : medicoes) {
            if (m.getPrecipTotal() != null) {
                count++;
            }
        }
        
        if (count < 3 || count < (diasJanela / 3)) {
            return TendenciaChuva.INSUFICIENTE;
        }

        // Regressão Linear Simples: y = a + bx. Vamos descobrir 'b' (inclinação).
        // x = índice do dia (0 a diasJanela-1), y = precip_total
        double sumX = 0;
        double sumY = 0;
        double sumXY = 0;
        double sumX2 = 0;
        int n = 0;

        for (MedicaoDiaria medicao : medicoes) {
            if (medicao.getPrecipTotal() != null && medicao.getObsDate() != null) {
                long diff = medicao.getObsDate().toEpochDay() - inicio.toEpochDay();
                double x = (double) diff;
                double y = medicao.getPrecipTotal();
                
                sumX += x;
                sumY += y;
                sumXY += (x * y);
                sumX2 += (x * x);
                n++;
            }
        }

        if (n < 2) {
            return TendenciaChuva.INSUFICIENTE;
        }

        double denominador = (n * sumX2) - (sumX * sumX);
        if (denominador == 0) {
            return TendenciaChuva.ESTAVEL;
        }

        double b = ((n * sumXY) - (sumX * sumY)) / denominador;

        if (b > 0.1) {
            return TendenciaChuva.CRESCENTE;
        } else if (b < -0.1) {
            return TendenciaChuva.DECRESCENTE;
        } else {
            return TendenciaChuva.ESTAVEL;
        }
    }
}
