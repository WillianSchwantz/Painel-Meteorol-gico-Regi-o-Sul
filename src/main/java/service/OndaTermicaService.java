package service;

import model.Estacao;
import model.MedicaoDiaria;
import model.RegiaoClimatica;
import model.ResumoMeteorologico;

import java.text.Normalizer;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * Calcula onda de calor/frio usando médias históricas simplificadas por região.
 *
 * <p>Regra adotada:
 * onda de calor = temperatura máxima >= média de verão + 5 °C por 3 dias
 * consecutivos ou mais; onda de frio = temperatura mínima <= média de inverno
 * - 5 °C por 3 dias consecutivos ou mais.</p>
 */
public class OndaTermicaService {

    private static final int DIAS_CONSECUTIVOS_MINIMOS = 3;
    private static final double DIFERENCA_LIMIAR = 5.0;

    public void preencherOndasTermicas(
            ResumoMeteorologico resumo,
            Estacao estacao,
            List<MedicaoDiaria> medicoes
    ) {
        Objects.requireNonNull(resumo, "O resumo não pode ser nulo.");
        Objects.requireNonNull(estacao, "A estação não pode ser nula.");
        Objects.requireNonNull(medicoes, "A lista de medições não pode ser nula.");

        RegiaoClimatica regiao = classificarRegiao(estacao);
        Double mediaVerao = regiao.getMediaHistoricaVerao();
        Double mediaInverno = regiao.getMediaHistoricaInverno();

        int diasCalor = calcularMaiorSequencia(
                medicoes,
                medicao -> medicao.getTempHigh() != null
                        && medicao.getTempHigh() >= mediaVerao + DIFERENCA_LIMIAR
        );
        int diasFrio = calcularMaiorSequencia(
                medicoes,
                medicao -> medicao.getTempLow() != null
                        && medicao.getTempLow() <= mediaInverno - DIFERENCA_LIMIAR
        );

        resumo.setRegiaoClimatica(regiao);
        resumo.setMediaHistoricaVerao(mediaVerao);
        resumo.setMediaHistoricaInverno(mediaInverno);
        resumo.setDiasConsecutivosOndaCalor(diasCalor);
        resumo.setDiasConsecutivosOndaFrio(diasFrio);
        resumo.setOndaCalor(diasCalor >= DIAS_CONSECUTIVOS_MINIMOS);
        resumo.setOndaFrio(diasFrio >= DIAS_CONSECUTIVOS_MINIMOS);
    }

    /**
     * Classificação aproximada porque o banco não possui cidade/UF/altitude.
     * Usa primeiro nomes conhecidos e, depois, faixas simples de coordenadas.
     */
    public RegiaoClimatica classificarRegiao(Estacao estacao) {
        Objects.requireNonNull(estacao, "A estação não pode ser nula.");
        String texto = normalizar(
                nullParaVazio(estacao.getStationId())
                        + " "
                        + nullParaVazio(estacao.getStationName())
        );
        Double latitude = estacao.getLatitude();
        Double longitude = estacao.getLongitude();

        if (contemAlgum(texto, "ausentes")
                || dentroDaCaixa(latitude, longitude, -29.2, -28.3, -50.5, -49.3)) {
            return RegiaoClimatica.SAO_JOSE_DOS_AUSENTES;
        }

        if (contemAlgum(
                texto,
                "uruguaiana",
                "quarai",
                "alegrete",
                "bage",
                "livramento",
                "dom pedrito",
                "sao gabriel",
                "itaqui",
                "barra do quara",
                "artigas",
                "bella union"
        ) || (longitude != null && longitude <= -54.0)) {
            return RegiaoClimatica.FRONTEIRA_OESTE_CAMPANHA;
        }

        if (contemAlgum(
                texto,
                "caxias",
                "farroupilha",
                "bento",
                "canela",
                "gramado",
                "vacaria",
                "andre da rocha",
                "nova prata",
                "flores",
                "bom jesus",
                "sao marcos",
                "passo fundo",
                "erechim",
                "soledade",
                "carazinho",
                "lagoa vermelha"
        ) || ehFaixaPlanaltoSerra(latitude, longitude)) {
            return RegiaoClimatica.PLANALTO_SERRA;
        }

        return RegiaoClimatica.LITORAL_LESTE_DEPRESSAO_CENTRAL;
    }

    public int calcularMaiorSequencia(
            List<MedicaoDiaria> medicoes,
            Predicate<MedicaoDiaria> condicao
    ) {
        Objects.requireNonNull(medicoes, "A lista de medições não pode ser nula.");
        Objects.requireNonNull(condicao, "A condição não pode ser nula.");

        List<MedicaoDiaria> ordenadas = medicoes.stream()
                .filter(medicao -> medicao.getObsDate() != null)
                .sorted(Comparator.comparing(MedicaoDiaria::getObsDate))
                .toList();

        int atual = 0;
        int maior = 0;
        LocalDate dataAnterior = null;

        for (MedicaoDiaria medicao : ordenadas) {
            LocalDate dataAtual = medicao.getObsDate();
            if (dataAnterior != null && dataAtual.equals(dataAnterior)) {
                continue;
            }

            boolean diaConsecutivo = dataAnterior == null
                    || dataAtual.equals(dataAnterior.plusDays(1));
            boolean atingiuCondicao = condicao.test(medicao);

            if (atingiuCondicao) {
                atual = diaConsecutivo ? atual + 1 : 1;
                maior = Math.max(maior, atual);
            } else {
                atual = 0;
            }

            dataAnterior = dataAtual;
        }

        return maior;
    }

    private static boolean ehFaixaPlanaltoSerra(
            Double latitude,
            Double longitude
    ) {
        if (latitude == null || longitude == null) {
            return false;
        }

        boolean serraNordeste = latitude >= -29.8
                && latitude <= -28.0
                && longitude >= -52.3
                && longitude <= -49.8;
        boolean planaltoNorte = latitude >= -28.9
                && latitude <= -27.0
                && longitude >= -53.9
                && longitude <= -50.0;

        return serraNordeste || planaltoNorte;
    }

    private static boolean dentroDaCaixa(
            Double latitude,
            Double longitude,
            double latitudeMinima,
            double latitudeMaxima,
            double longitudeMinima,
            double longitudeMaxima
    ) {
        return latitude != null
                && longitude != null
                && latitude >= latitudeMinima
                && latitude <= latitudeMaxima
                && longitude >= longitudeMinima
                && longitude <= longitudeMaxima;
    }

    private static boolean contemAlgum(String texto, String... termos) {
        for (String termo : termos) {
            if (texto.contains(normalizar(termo))) {
                return true;
            }
        }
        return false;
    }

    private static String nullParaVazio(String texto) {
        return texto == null ? "" : texto;
    }

    private static String normalizar(String texto) {
        String semAcento = Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return semAcento.toLowerCase(Locale.ROOT);
    }
}
