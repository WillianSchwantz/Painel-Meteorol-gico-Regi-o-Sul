package model;

/**
 * Dados calculados para exibição no painel.
 *
 * <p>Esta classe não representa uma tabela do banco de dados.</p>
 */
public class ResumoMeteorologico {

    // Identifica stations.station_id.
    private String stationId;

    // Obtido de stations.station_name.
    private String nomeEstacao;

    // Calculadas a partir de history_daily.temp_low/temp_high/temp_avg.
    private Double temperaturaMinima;
    private Double temperaturaMaxima;
    private Double temperaturaMedia;

    // Calculada a partir de history_daily.humidity_avg.
    private Double umidadeMedia;

    // Calculados a partir de history_daily.windspeed_avg/windspeed_high.
    private Double ventoMedio;
    private Double ventoMaximo;

    /*
     * Calculada a partir de history_daily.pressure_min e pressure_max.
     * O banco não possui uma coluna pressure_avg.
     */
    private Double pressaoMedia;

    // Calculado a partir de history_daily.precip_total no período selecionado.
    private Double chuvaTotal;

    /*
     * Calculados por AlagamentoService. A fonte preferencial é
     * history_hourly.precip_total, com aproximação diária quando indisponível.
     */
    private Double chuva24h;
    private Double chuva48h;
    private Double chuva72h;

    // Resultado calculado por AlagamentoService; não existe no banco.
    private NivelAlerta nivelAlerta;

    /*
     * Campos calculados por OndaTermicaService. O wu.sql nao possui medias
     * climatologicas; as referencias foram informadas para este projeto.
     */
    private RegiaoClimatica regiaoClimatica;
    private Double mediaHistoricaVerao;
    private Double mediaHistoricaInverno;
    private Boolean ondaCalor;
    private Integer diasConsecutivosOndaCalor;
    private Boolean ondaFrio;
    private Integer diasConsecutivosOndaFrio;

    public ResumoMeteorologico() {
    }

    public ResumoMeteorologico(
            String stationId,
            String nomeEstacao,
            Double temperaturaMinima,
            Double temperaturaMaxima,
            Double temperaturaMedia,
            Double umidadeMedia,
            Double ventoMedio,
            Double ventoMaximo,
            Double pressaoMedia,
            Double chuvaTotal,
            Double chuva24h,
            Double chuva48h,
            Double chuva72h,
            NivelAlerta nivelAlerta
    ) {
        this.stationId = stationId;
        this.nomeEstacao = nomeEstacao;
        this.temperaturaMinima = temperaturaMinima;
        this.temperaturaMaxima = temperaturaMaxima;
        this.temperaturaMedia = temperaturaMedia;
        this.umidadeMedia = umidadeMedia;
        this.ventoMedio = ventoMedio;
        this.ventoMaximo = ventoMaximo;
        this.pressaoMedia = pressaoMedia;
        this.chuvaTotal = chuvaTotal;
        this.chuva24h = chuva24h;
        this.chuva48h = chuva48h;
        this.chuva72h = chuva72h;
        this.nivelAlerta = nivelAlerta;
    }

    public ResumoMeteorologico(
            String stationId,
            String nomeEstacao,
            Double temperaturaMinima,
            Double temperaturaMaxima,
            Double temperaturaMedia,
            Double umidadeMedia,
            Double ventoMedio,
            Double ventoMaximo,
            Double pressaoMedia,
            Double chuvaTotal,
            Double chuva24h,
            Double chuva48h,
            Double chuva72h,
            NivelAlerta nivelAlerta,
            RegiaoClimatica regiaoClimatica,
            Double mediaHistoricaVerao,
            Double mediaHistoricaInverno,
            Boolean ondaCalor,
            Integer diasConsecutivosOndaCalor,
            Boolean ondaFrio,
            Integer diasConsecutivosOndaFrio
    ) {
        this(
                stationId,
                nomeEstacao,
                temperaturaMinima,
                temperaturaMaxima,
                temperaturaMedia,
                umidadeMedia,
                ventoMedio,
                ventoMaximo,
                pressaoMedia,
                chuvaTotal,
                chuva24h,
                chuva48h,
                chuva72h,
                nivelAlerta
        );
        this.regiaoClimatica = regiaoClimatica;
        this.mediaHistoricaVerao = mediaHistoricaVerao;
        this.mediaHistoricaInverno = mediaHistoricaInverno;
        this.ondaCalor = ondaCalor;
        this.diasConsecutivosOndaCalor = diasConsecutivosOndaCalor;
        this.ondaFrio = ondaFrio;
        this.diasConsecutivosOndaFrio = diasConsecutivosOndaFrio;
    }

    public String getStationId() {
        return stationId;
    }

    public void setStationId(String stationId) {
        this.stationId = stationId;
    }

    public String getNomeEstacao() {
        return nomeEstacao;
    }

    public void setNomeEstacao(String nomeEstacao) {
        this.nomeEstacao = nomeEstacao;
    }

    public Double getTemperaturaMinima() {
        return temperaturaMinima;
    }

    public void setTemperaturaMinima(Double temperaturaMinima) {
        this.temperaturaMinima = temperaturaMinima;
    }

    public Double getTemperaturaMaxima() {
        return temperaturaMaxima;
    }

    public void setTemperaturaMaxima(Double temperaturaMaxima) {
        this.temperaturaMaxima = temperaturaMaxima;
    }

    public Double getTemperaturaMedia() {
        return temperaturaMedia;
    }

    public void setTemperaturaMedia(Double temperaturaMedia) {
        this.temperaturaMedia = temperaturaMedia;
    }

    public Double getUmidadeMedia() {
        return umidadeMedia;
    }

    public void setUmidadeMedia(Double umidadeMedia) {
        this.umidadeMedia = umidadeMedia;
    }

    public Double getVentoMedio() {
        return ventoMedio;
    }

    public void setVentoMedio(Double ventoMedio) {
        this.ventoMedio = ventoMedio;
    }

    public Double getVentoMaximo() {
        return ventoMaximo;
    }

    public void setVentoMaximo(Double ventoMaximo) {
        this.ventoMaximo = ventoMaximo;
    }

    public Double getPressaoMedia() {
        return pressaoMedia;
    }

    public void setPressaoMedia(Double pressaoMedia) {
        this.pressaoMedia = pressaoMedia;
    }

    public Double getChuvaTotal() {
        return chuvaTotal;
    }

    public void setChuvaTotal(Double chuvaTotal) {
        this.chuvaTotal = chuvaTotal;
    }

    public Double getChuva24h() {
        return chuva24h;
    }

    public void setChuva24h(Double chuva24h) {
        this.chuva24h = chuva24h;
    }

    public Double getChuva48h() {
        return chuva48h;
    }

    public void setChuva48h(Double chuva48h) {
        this.chuva48h = chuva48h;
    }

    public Double getChuva72h() {
        return chuva72h;
    }

    public void setChuva72h(Double chuva72h) {
        this.chuva72h = chuva72h;
    }

    public NivelAlerta getNivelAlerta() {
        return nivelAlerta;
    }

    public void setNivelAlerta(NivelAlerta nivelAlerta) {
        this.nivelAlerta = nivelAlerta;
    }

    public RegiaoClimatica getRegiaoClimatica() {
        return regiaoClimatica;
    }

    public void setRegiaoClimatica(RegiaoClimatica regiaoClimatica) {
        this.regiaoClimatica = regiaoClimatica;
    }

    public Double getMediaHistoricaVerao() {
        return mediaHistoricaVerao;
    }

    public void setMediaHistoricaVerao(Double mediaHistoricaVerao) {
        this.mediaHistoricaVerao = mediaHistoricaVerao;
    }

    public Double getMediaHistoricaInverno() {
        return mediaHistoricaInverno;
    }

    public void setMediaHistoricaInverno(Double mediaHistoricaInverno) {
        this.mediaHistoricaInverno = mediaHistoricaInverno;
    }

    public Boolean getOndaCalor() {
        return ondaCalor;
    }

    public void setOndaCalor(Boolean ondaCalor) {
        this.ondaCalor = ondaCalor;
    }

    public Integer getDiasConsecutivosOndaCalor() {
        return diasConsecutivosOndaCalor;
    }

    public void setDiasConsecutivosOndaCalor(
            Integer diasConsecutivosOndaCalor
    ) {
        this.diasConsecutivosOndaCalor = diasConsecutivosOndaCalor;
    }

    public Boolean getOndaFrio() {
        return ondaFrio;
    }

    public void setOndaFrio(Boolean ondaFrio) {
        this.ondaFrio = ondaFrio;
    }

    public Integer getDiasConsecutivosOndaFrio() {
        return diasConsecutivosOndaFrio;
    }

    public void setDiasConsecutivosOndaFrio(
            Integer diasConsecutivosOndaFrio
    ) {
        this.diasConsecutivosOndaFrio = diasConsecutivosOndaFrio;
    }

    @Override
    public String toString() {
        return "ResumoMeteorologico{"
                + "stationId='" + stationId + '\''
                + ", nomeEstacao='" + nomeEstacao + '\''
                + ", temperaturaMedia=" + temperaturaMedia
                + ", umidadeMedia=" + umidadeMedia
                + ", chuvaTotal=" + chuvaTotal
                + ", nivelAlerta=" + nivelAlerta
                + ", regiaoClimatica=" + regiaoClimatica
                + ", ondaCalor=" + ondaCalor
                + ", ondaFrio=" + ondaFrio
                + '}';
    }
}
