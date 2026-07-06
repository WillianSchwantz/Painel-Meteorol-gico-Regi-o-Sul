package service;

import dao.EstacaoDAO;
import dao.MedicaoDAO;
import map.HeatmapPoint;
import model.Estacao;
import model.ResumoMeteorologico;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Prepara valores reais, agregados por estação, para o mapa de calor.
 */
public class HeatmapService {

    private final EstacaoDAO estacaoDAO;
    private final MedicaoDAO medicaoDAO;

    public HeatmapService() {
        this(new EstacaoDAO(), new MedicaoDAO());
    }

    public HeatmapService(
            EstacaoDAO estacaoDAO,
            MedicaoDAO medicaoDAO
    ) {
        this.estacaoDAO = Objects.requireNonNull(
                estacaoDAO,
                "O EstacaoDAO não pode ser nulo."
        );
        this.medicaoDAO = Objects.requireNonNull(
                medicaoDAO,
                "O MedicaoDAO não pode ser nulo."
        );
    }

    public List<HeatmapPoint> criarPontos(
            LocalDate dataInicial,
            LocalDate dataFinal,
            String variavel
    ) throws SQLException {
        validarParametros(dataInicial, dataFinal, variavel);

        Map<String, Estacao> estacoesPorId = new HashMap<>();
        for (Estacao estacao : estacaoDAO.listarTodas()) {
            if (possuiCoordenadasValidas(estacao)) {
                estacoesPorId.put(estacao.getStationId(), estacao);
            }
        }

        List<HeatmapPoint> pontos = new ArrayList<>();
        List<ResumoMeteorologico> resumos =
                medicaoDAO.listarResumosDiariosPorEstacao(
                        dataInicial,
                        dataFinal
                );

        for (ResumoMeteorologico resumo : resumos) {
            Estacao estacao = estacoesPorId.get(resumo.getStationId());
            Double valor = extrairValor(resumo, variavel);
            if (estacao == null
                    || valor == null
                    || !Double.isFinite(valor)) {
                continue;
            }

            pontos.add(new HeatmapPoint(
                    estacao.getStationId(),
                    estacao.getLatitude(),
                    estacao.getLongitude(),
                    valor
            ));
        }
        return List.copyOf(pontos);
    }

    private static Double extrairValor(
            ResumoMeteorologico resumo,
            String variavel
    ) {
        return switch (variavel) {
            case "Temperatura" -> resumo.getTemperaturaMedia();
            case "Chuva" -> resumo.getChuvaTotal();
            case "Umidade" -> resumo.getUmidadeMedia();
            case "Vento" -> resumo.getVentoMedio();
            case "Pressão" -> resumo.getPressaoMedia();
            default -> throw new IllegalArgumentException(
                    "Variável não suportada pelo heatmap: " + variavel
            );
        };
    }

    private static void validarParametros(
            LocalDate dataInicial,
            LocalDate dataFinal,
            String variavel
    ) {
        Objects.requireNonNull(
                dataInicial,
                "A data inicial do heatmap é obrigatória."
        );
        Objects.requireNonNull(
                dataFinal,
                "A data final do heatmap é obrigatória."
        );
        if (dataInicial.isAfter(dataFinal)) {
            throw new IllegalArgumentException(
                    "A data inicial não pode ser posterior à data final."
            );
        }
        if (variavel == null || variavel.isBlank()) {
            throw new IllegalArgumentException(
                    "A variável do heatmap é obrigatória."
            );
        }
    }

    private static boolean possuiCoordenadasValidas(Estacao estacao) {
        if (estacao == null
                || estacao.getStationId() == null
                || estacao.getLatitude() == null
                || estacao.getLongitude() == null) {
            return false;
        }

        double latitude = estacao.getLatitude();
        double longitude = estacao.getLongitude();
        return Double.isFinite(latitude)
                && Double.isFinite(longitude)
                && latitude >= -90.0
                && latitude <= 90.0
                && longitude >= -180.0
                && longitude <= 180.0;
    }
}
