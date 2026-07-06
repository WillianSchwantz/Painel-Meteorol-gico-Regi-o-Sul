package service;

import dao.EstacaoDAO;
import dao.MedicaoDAO;
import model.Estacao;
import model.MedicaoDiaria;
import model.ResumoMeteorologico;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/**
 * Coordena os DAOs e aplica os cálculos meteorológicos do resumo.
 */
public class EstacaoService {

    private final EstacaoDAO estacaoDAO;
    private final MedicaoDAO medicaoDAO;
    private final AlagamentoService alagamentoService;
    private final OndaTermicaService ondaTermicaService;

    public EstacaoService() {
        this.estacaoDAO = new EstacaoDAO();
        this.medicaoDAO = new MedicaoDAO();
        this.alagamentoService = new AlagamentoService(this.medicaoDAO);
        this.ondaTermicaService = new OndaTermicaService();
    }

    public EstacaoService(EstacaoDAO estacaoDAO, MedicaoDAO medicaoDAO) {
        this(
                estacaoDAO,
                medicaoDAO,
                new AlagamentoService(medicaoDAO),
                new OndaTermicaService()
        );
    }

    public EstacaoService(
            EstacaoDAO estacaoDAO,
            MedicaoDAO medicaoDAO,
            AlagamentoService alagamentoService
    ) {
        this(estacaoDAO, medicaoDAO, alagamentoService, new OndaTermicaService());
    }

    public EstacaoService(
            EstacaoDAO estacaoDAO,
            MedicaoDAO medicaoDAO,
            AlagamentoService alagamentoService,
            OndaTermicaService ondaTermicaService
    ) {
        this.estacaoDAO = Objects.requireNonNull(
                estacaoDAO,
                "O EstacaoDAO não pode ser nulo."
        );
        this.medicaoDAO = Objects.requireNonNull(
                medicaoDAO,
                "O MedicaoDAO não pode ser nulo."
        );
        this.alagamentoService = Objects.requireNonNull(
                alagamentoService,
                "O AlagamentoService não pode ser nulo."
        );
        this.ondaTermicaService = Objects.requireNonNull(
                ondaTermicaService,
                "O OndaTermicaService não pode ser nulo."
        );
    }

    public List<Estacao> listarEstacoes() throws SQLException {
        return estacaoDAO.listarTodas();
    }

    public List<Estacao> listarEstacoesPorCidadeOuTexto(String texto)
            throws SQLException {
        return estacaoDAO.listarPorCidadeOuTexto(texto);
    }

    public List<String> listarLocalidades() throws SQLException {
        return estacaoDAO.listarLocalidades();
    }

    public List<MedicaoDiaria> buscarMedicoesDiarias(
            Estacao estacao,
            LocalDate dataInicial,
            LocalDate dataFinal
    ) throws SQLException {
        validarParametros(estacao, dataInicial, dataFinal);
        return medicaoDAO.listarDiariasPorEstacao(
                estacao.getStationId(),
                dataInicial,
                dataFinal
        );
    }

    /**
     * Busca as medições e calcula o resumo do período.
     */
    public ResumoMeteorologico calcularResumo(
            Estacao estacao,
            LocalDate dataInicial,
            LocalDate dataFinal,
            double limite24h,
            double limite48h,
            double limite72h
    ) throws SQLException {
        List<MedicaoDiaria> medicoes = buscarMedicoesDiarias(
                estacao,
                dataInicial,
                dataFinal
        );
        return calcularResumo(
                estacao,
                medicoes,
                dataFinal.atTime(LocalTime.MAX),
                limite24h,
                limite48h,
                limite72h
        );
    }

    /**
     * Calcula o resumo usando medições já carregadas.
     *
     * <p>Este overload permite que a interface atualize a tabela e o resumo
     * com a mesma consulta, sem acessar o banco duas vezes.</p>
     */
    public ResumoMeteorologico calcularResumo(
            Estacao estacao,
            List<MedicaoDiaria> medicoes,
            double limite24h,
            double limite48h,
            double limite72h
    ) throws SQLException {
        LocalDate ultimaData = buscarUltimaData(medicoes);
        LocalDateTime referencia = ultimaData == null
                ? null
                : ultimaData.atTime(LocalTime.MAX);
        return calcularResumo(estacao, medicoes, referencia, limite24h, limite48h, limite72h);
    }

    /**
     * Calcula o resumo com uma referência explícita para as janelas de chuva.
     */
    public ResumoMeteorologico calcularResumo(
            Estacao estacao,
            List<MedicaoDiaria> medicoes,
            LocalDate dataReferencia,
            double limite24h,
            double limite48h,
            double limite72h
    ) throws SQLException {
        Objects.requireNonNull(
                dataReferencia,
                "A data de referência é obrigatória."
        );
        return calcularResumo(
                estacao,
                medicoes,
                dataReferencia.atTime(LocalTime.MAX),
                limite24h,
                limite48h,
                limite72h
        );
    }

    /**
     * Calcula o resumo com uma referência explícita para as janelas de chuva.
     */
    public ResumoMeteorologico calcularResumo(
            Estacao estacao,
            List<MedicaoDiaria> medicoes,
            LocalDateTime referencia,
            double limite24h,
            double limite48h,
            double limite72h
    ) throws SQLException {
        validarEstacao(estacao);
        Objects.requireNonNull(medicoes, "A lista de medições não pode ser nula.");

        ResumoMeteorologico resumo = calcularResumoBasico(estacao, medicoes);
        ondaTermicaService.preencherOndasTermicas(
                resumo,
                estacao,
                medicoes
        );
        preencherDadosDeAlagamento(
                resumo,
                estacao.getStationId(),
                referencia,
                limite24h,
                limite48h,
                limite72h
        );
        return resumo;
    }

    private ResumoMeteorologico calcularResumoBasico(
            Estacao estacao,
            List<MedicaoDiaria> medicoes
    ) {
        ResumoMeteorologico resumo = new ResumoMeteorologico();
        resumo.setStationId(estacao.getStationId());
        resumo.setNomeEstacao(estacao.getStationName());

        if (!medicoes.isEmpty()) {
            resumo.setTemperaturaMinima(
                    calcularMinimo(medicoes, MedicaoDiaria::getTempLow)
            );
            resumo.setTemperaturaMaxima(
                    calcularMaximo(medicoes, MedicaoDiaria::getTempHigh)
            );
            resumo.setTemperaturaMedia(
                    calcularMedia(medicoes, MedicaoDiaria::getTempAvg)
            );
            resumo.setUmidadeMedia(
                    calcularMedia(medicoes, MedicaoDiaria::getHumidityAvg)
            );
            resumo.setVentoMedio(
                    calcularMedia(medicoes, MedicaoDiaria::getWindspeedAvg)
            );
            resumo.setVentoMaximo(
                    calcularMaximo(medicoes, MedicaoDiaria::getWindspeedHigh)
            );
            resumo.setPressaoMedia(calcularPressaoMedia(medicoes));
            resumo.setChuvaTotal(
                    calcularSoma(medicoes, MedicaoDiaria::getPrecipTotal)
            );
        }

        return resumo;
    }

    private void preencherDadosDeAlagamento(
            ResumoMeteorologico resumo,
            String stationId,
            LocalDateTime referencia,
            double limite24h,
            double limite48h,
            double limite72h
    ) throws SQLException {
        Double chuva24h = null;
        Double chuva48h = null;
        Double chuva72h = null;

        if (referencia != null) {
            chuva24h = alagamentoService.calcularChuvaAcumulada24h(
                    stationId,
                    referencia
            );
            chuva48h = alagamentoService.calcularChuvaAcumulada48h(
                    stationId,
                    referencia
            );
            chuva72h = alagamentoService.calcularChuvaAcumulada72h(
                    stationId,
                    referencia
            );
        }

        resumo.setChuva24h(chuva24h);
        resumo.setChuva48h(chuva48h);
        resumo.setChuva72h(chuva72h);
        resumo.setNivelAlerta(
                alagamentoService.calcularNivelAlerta(
                        chuva24h,
                        chuva48h,
                        chuva72h,
                        limite24h,
                        limite48h,
                        limite72h
                )
        );
    }

    private static LocalDate buscarUltimaData(
            List<MedicaoDiaria> medicoes
    ) {
        LocalDate ultimaData = null;
        for (MedicaoDiaria medicao : medicoes) {
            LocalDate data = medicao.getObsDate();
            if (data != null
                    && (ultimaData == null || data.isAfter(ultimaData))) {
                ultimaData = data;
            }
        }
        return ultimaData;
    }

    private static Double calcularMinimo(
            List<MedicaoDiaria> medicoes,
            Function<MedicaoDiaria, Double> extrator
    ) {
        Double minimo = null;
        for (MedicaoDiaria medicao : medicoes) {
            Double valor = extrator.apply(medicao);
            if (valor != null && (minimo == null || valor < minimo)) {
                minimo = valor;
            }
        }
        return minimo;
    }

    private static Double calcularMaximo(
            List<MedicaoDiaria> medicoes,
            Function<MedicaoDiaria, Double> extrator
    ) {
        Double maximo = null;
        for (MedicaoDiaria medicao : medicoes) {
            Double valor = extrator.apply(medicao);
            if (valor != null && (maximo == null || valor > maximo)) {
                maximo = valor;
            }
        }
        return maximo;
    }

    private static Double calcularMedia(
            List<MedicaoDiaria> medicoes,
            Function<MedicaoDiaria, Double> extrator
    ) {
        double soma = 0.0;
        int quantidade = 0;

        for (MedicaoDiaria medicao : medicoes) {
            Double valor = extrator.apply(medicao);
            if (valor != null) {
                soma += valor;
                quantidade++;
            }
        }

        return quantidade == 0 ? null : soma / quantidade;
    }

    private static Double calcularSoma(
            List<MedicaoDiaria> medicoes,
            Function<MedicaoDiaria, Double> extrator
    ) {
        double soma = 0.0;
        boolean encontrouValor = false;

        for (MedicaoDiaria medicao : medicoes) {
            Double valor = extrator.apply(medicao);
            if (valor != null) {
                soma += valor;
                encontrouValor = true;
            }
        }

        return encontrouValor ? soma : null;
    }

    private static Double calcularPressaoMedia(
            List<MedicaoDiaria> medicoes
    ) {
        double soma = 0.0;
        int quantidade = 0;

        for (MedicaoDiaria medicao : medicoes) {
            Double pressaoMinima = medicao.getPressureMin();
            Double pressaoMaxima = medicao.getPressureMax();

            if (pressaoMinima != null && pressaoMaxima != null) {
                soma += (pressaoMinima + pressaoMaxima) / 2.0;
                quantidade++;
            }
        }

        return quantidade == 0 ? null : soma / quantidade;
    }

    private static void validarParametros(
            Estacao estacao,
            LocalDate dataInicial,
            LocalDate dataFinal
    ) {
        validarEstacao(estacao);
        Objects.requireNonNull(dataInicial, "A data inicial é obrigatória.");
        Objects.requireNonNull(dataFinal, "A data final é obrigatória.");

        if (dataInicial.isAfter(dataFinal)) {
            throw new IllegalArgumentException(
                    "A data inicial não pode ser posterior à data final."
            );
        }
    }

    private static void validarEstacao(Estacao estacao) {
        Objects.requireNonNull(estacao, "A estação é obrigatória.");
        if (estacao.getStationId() == null
                || estacao.getStationId().isBlank()) {
            throw new IllegalArgumentException(
                    "A estação deve possuir um identificador válido."
            );
        }
    }
}
