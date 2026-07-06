package model;

/**
 * Regiões climáticas de apoio usadas para calcular ondas de calor/frio.
 *
 * <p>Essas médias não vêm do banco wu.sql. Elas foram informadas como
 * referência histórica simplificada para o trabalho acadêmico.</p>
 */
public enum RegiaoClimatica {

    LITORAL_LESTE_DEPRESSAO_CENTRAL(
            "Litoral/Leste e Depressão Central",
            31.0,
            10.0
    ),
    PLANALTO_SERRA(
            "Planalto/Serra",
            31.0,
            7.0
    ),
    SAO_JOSE_DOS_AUSENTES(
            "São José dos Ausentes",
            31.0,
            6.0
    ),
    FRONTEIRA_OESTE_CAMPANHA(
            "Fronteira Oeste/Campanha",
            34.0,
            8.0
    );

    private final String descricao;
    private final Double mediaHistoricaVerao;
    private final Double mediaHistoricaInverno;

    RegiaoClimatica(
            String descricao,
            Double mediaHistoricaVerao,
            Double mediaHistoricaInverno
    ) {
        this.descricao = descricao;
        this.mediaHistoricaVerao = mediaHistoricaVerao;
        this.mediaHistoricaInverno = mediaHistoricaInverno;
    }

    public String getDescricao() {
        return descricao;
    }

    public Double getMediaHistoricaVerao() {
        return mediaHistoricaVerao;
    }

    public Double getMediaHistoricaInverno() {
        return mediaHistoricaInverno;
    }

    @Override
    public String toString() {
        return descricao;
    }
}
