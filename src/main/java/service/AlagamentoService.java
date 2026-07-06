package service;

import dao.MedicaoDAO;
import model.MedicaoDiaria;
import model.NivelAlerta;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * Calcula acumulados de chuva e o nível de alerta de alagamento.
 */
public class AlagamentoService {

    private static final int HORAS_24 = 24;
    private static final int HORAS_48 = 48;
    private static final int HORAS_72 = 72;

    private final MedicaoDAO medicaoDAO;

    public AlagamentoService() {
        this(new MedicaoDAO());
    }

    public AlagamentoService(MedicaoDAO medicaoDAO) {
        this.medicaoDAO = Objects.requireNonNull(
                medicaoDAO,
                "O MedicaoDAO não pode ser nulo."
        );
    }

    /**
     * Retorna sempre o maior nível atingido. Valores nulos equivalem a zero.
     */
    public NivelAlerta calcularNivelAlerta(
            Double mm24,
            Double mm48,
            Double mm72,
            double limite24h,
            double limite48h,
            double limite72h
    ) {
        double chuva24h = valorOuZero(mm24);
        double chuva48h = valorOuZero(mm48);
        double chuva72h = valorOuZero(mm72);

        // EMERGENCIA: consideramos o dobro do limite de atenção + 20mm (ex: 80, 100, 120 para 30, 50, 60)
        // Usaremos uma proporção simples: Emergência = limiar + 50, Alerta = limiar + 20, Atenção = limiar
        if (chuva24h >= limite24h + 50.0
                || chuva48h >= limite48h + 50.0
                || chuva72h >= limite72h + 60.0) {
            return NivelAlerta.EMERGENCIA;
        }

        if (chuva24h >= limite24h + 20.0
                || chuva48h >= limite48h + 20.0
                || chuva72h >= limite72h + 30.0) {
            return NivelAlerta.ALERTA;
        }

        if (chuva24h >= limite24h
                || chuva48h >= limite48h
                || chuva72h >= limite72h) {
            return NivelAlerta.ATENCAO;
        }

        return NivelAlerta.NORMAL;
    }

    public Double calcularChuvaAcumulada24h(
            String stationId,
            LocalDateTime referencia
    ) throws SQLException {
        return calcularChuvaAcumulada(stationId, referencia, HORAS_24);
    }

    public Double calcularChuvaAcumulada48h(
            String stationId,
            LocalDateTime referencia
    ) throws SQLException {
        return calcularChuvaAcumulada(stationId, referencia, HORAS_48);
    }

    public Double calcularChuvaAcumulada72h(
            String stationId,
            LocalDateTime referencia
    ) throws SQLException {
        return calcularChuvaAcumulada(stationId, referencia, HORAS_72);
    }

    private Double calcularChuvaAcumulada(
            String stationId,
            LocalDateTime referencia,
            int horas
    ) throws SQLException {
        validarParametros(stationId, referencia);

        Double acumuladoHorario = medicaoDAO.buscarChuvaAcumulada(
                stationId,
                referencia,
                horas
        );
        if (acumuladoHorario != null) {
            return acumuladoHorario;
        }

        return calcularAproximacaoDiaria(stationId, referencia, horas);
    }

    /**
     * Aproxima a janela usando dias-calendário completos, pois o dump atual
     * não contém valores em history_hourly.precip_total.
     */
    private Double calcularAproximacaoDiaria(
            String stationId,
            LocalDateTime referencia,
            int horas
    ) throws SQLException {
        int quantidadeDias = horas / 24;
        LocalDate dataFinal = referencia.toLocalDate();
        LocalDate dataInicial = dataFinal.minusDays(quantidadeDias - 1L);

        List<MedicaoDiaria> medicoes =
                medicaoDAO.listarDiariasPorEstacao(
                        stationId,
                        dataInicial,
                        dataFinal
                );

        double soma = 0.0;
        boolean encontrouValor = false;
        for (MedicaoDiaria medicao : medicoes) {
            Double chuva = medicao.getPrecipTotal();
            if (chuva != null) {
                soma += chuva;
                encontrouValor = true;
            }
        }

        return encontrouValor ? soma : null;
    }

    private static double valorOuZero(Double valor) {
        return valor == null ? 0.0 : valor;
    }

    private static void validarParametros(
            String stationId,
            LocalDateTime referencia
    ) {
        if (stationId == null || stationId.isBlank()) {
            throw new IllegalArgumentException(
                    "O identificador da estação é obrigatório."
            );
        }
        Objects.requireNonNull(
                referencia,
                "A data/hora de referência é obrigatória."
        );
    }
}
