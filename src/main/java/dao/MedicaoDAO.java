package dao;

import model.MedicaoDiaria;
import model.MedicaoHoraria;
import model.ResumoMeteorologico;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Acesso aos históricos meteorológicos diário e horário.
 */
public class MedicaoDAO {

    private static final String SQL_LISTAR_DIARIAS = """
            SELECT id,
                   station_id,
                   obs_date,
                   obs_time_utc,
                   obs_time_local,
                   epoch,
                   tz,
                   lat,
                   lon,
                   qc_status,
                   solar_radiation_high,
                   uv_high,
                   winddir_avg,
                   humidity_high,
                   humidity_low,
                   humidity_avg,
                   temp_high,
                   temp_low,
                   temp_avg,
                   dewpt_high,
                   dewpt_low,
                   dewpt_avg,
                   heatindex_high,
                   heatindex_low,
                   heatindex_avg,
                   windchill_high,
                   windchill_low,
                   windchill_avg,
                   windspeed_high,
                   windspeed_low,
                   windspeed_avg,
                   windgust_high,
                   windgust_low,
                   windgust_avg,
                   pressure_max,
                   pressure_min,
                   pressure_trend,
                   precip_rate,
                   precip_total,
                   fetched_at
              FROM history_daily
             WHERE station_id = ?
               AND obs_date BETWEEN ? AND ?
             ORDER BY obs_date
            """;

    private static final String SQL_LISTAR_HORARIAS = """
            SELECT id,
                   station_id,
                   obs_time_utc,
                   obs_time_local,
                   epoch,
                   tz,
                   lat,
                   lon,
                   qc_status,
                   solar_radiation_high,
                   uv_high,
                   winddir_avg,
                   humidity_high,
                   humidity_low,
                   humidity_avg,
                   temp_high,
                   temp_low,
                   temp_avg,
                   dewpt_high,
                   dewpt_low,
                   dewpt_avg,
                   heatindex_high,
                   heatindex_low,
                   heatindex_avg,
                   windchill_high,
                   windchill_low,
                   windchill_avg,
                   windspeed_high,
                   windspeed_low,
                   windspeed_avg,
                   windgust_high,
                   windgust_low,
                   windgust_avg,
                   pressure_max,
                   pressure_min,
                   pressure_trend,
                   precip_rate,
                   precip_total,
                   fetched_at
              FROM history_hourly
             WHERE station_id = ?
               AND obs_time_utc BETWEEN ? AND ?
             ORDER BY obs_time_utc
            """;

    private static final String SQL_BUSCAR_RESUMO_DIARIO = """
            SELECT s.station_id,
                   s.station_name,
                   MIN(h.temp_low) AS temperatura_minima,
                   MAX(h.temp_high) AS temperatura_maxima,
                   AVG(h.temp_avg) AS temperatura_media,
                   AVG(h.humidity_avg) AS umidade_media,
                   AVG(h.windspeed_avg) AS vento_medio,
                   MAX(h.windspeed_high) AS vento_maximo,
                   AVG((h.pressure_min + h.pressure_max) / 2.0) AS pressao_media,
                   SUM(h.precip_total) AS chuva_total
              FROM history_daily h
              JOIN stations s
                ON s.station_id = h.station_id
             WHERE h.station_id = ?
               AND h.obs_date BETWEEN ? AND ?
            GROUP BY s.station_id, s.station_name
            """;

    private static final String SQL_LISTAR_RESUMOS_DIARIOS = """
            SELECT s.station_id,
                   s.station_name,
                   MIN(h.temp_low) AS temperatura_minima,
                   MAX(h.temp_high) AS temperatura_maxima,
                   AVG(h.temp_avg) AS temperatura_media,
                   AVG(h.humidity_avg) AS umidade_media,
                   AVG(h.windspeed_avg) AS vento_medio,
                   MAX(h.windspeed_high) AS vento_maximo,
                   AVG((h.pressure_min + h.pressure_max) / 2.0) AS pressao_media,
                   SUM(h.precip_total) AS chuva_total
              FROM history_daily h
              JOIN stations s
                ON s.station_id = h.station_id
             WHERE h.obs_date BETWEEN ? AND ?
             GROUP BY s.station_id, s.station_name
             ORDER BY s.station_id
            """;

    private static final String SQL_BUSCAR_CHUVA_HORARIA = """
            SELECT SUM(precip_total) AS chuva_acumulada,
                   COUNT(precip_total) AS quantidade_valores
              FROM history_hourly
             WHERE station_id = ?
               AND obs_time_utc > ?
               AND obs_time_utc <= ?
            """;

    private final ConexaoMySQL conexaoMySQL;

    public MedicaoDAO() {
        this(new ConexaoMySQL());
    }

    public MedicaoDAO(ConexaoMySQL conexaoMySQL) {
        this.conexaoMySQL = Objects.requireNonNull(
                conexaoMySQL,
                "A configuração de conexão não pode ser nula."
        );
    }

    /**
     * Lista medições diárias no período inclusivo informado.
     *
     * @return lista vazia quando não houver dados
     */
    public List<MedicaoDiaria> listarDiariasPorEstacao(
            String stationId,
            LocalDate dataInicial,
            LocalDate dataFinal
    ) throws SQLException {
        validarStationId(stationId);
        validarPeriodo(dataInicial, dataFinal);
        List<MedicaoDiaria> medicoes = new ArrayList<>();

        try (
                Connection conexao = conexaoMySQL.abrirConexao();
                PreparedStatement statement = conexao.prepareStatement(SQL_LISTAR_DIARIAS)
        ) {
            statement.setString(1, stationId.trim());
            statement.setDate(2, Date.valueOf(dataInicial));
            statement.setDate(3, Date.valueOf(dataFinal));

            try (ResultSet resultado = statement.executeQuery()) {
                while (resultado.next()) {
                    medicoes.add(mapearMedicaoDiaria(resultado));
                }
            }
            return medicoes;
        } catch (SQLException e) {
            throw criarErro("listar as medições diárias", e);
        }
    }

    /**
     * Lista medições horárias por {@code obs_time_utc}, em período inclusivo.
     *
     * @return lista vazia quando não houver dados
     */
    public List<MedicaoHoraria> listarHorariasPorEstacao(
            String stationId,
            LocalDateTime inicio,
            LocalDateTime fim
    ) throws SQLException {
        validarStationId(stationId);
        validarPeriodo(inicio, fim);
        List<MedicaoHoraria> medicoes = new ArrayList<>();

        try (
                Connection conexao = conexaoMySQL.abrirConexao();
                PreparedStatement statement = conexao.prepareStatement(SQL_LISTAR_HORARIAS)
        ) {
            statement.setString(1, stationId.trim());
            statement.setTimestamp(2, Timestamp.valueOf(inicio));
            statement.setTimestamp(3, Timestamp.valueOf(fim));

            try (ResultSet resultado = statement.executeQuery()) {
                while (resultado.next()) {
                    medicoes.add(mapearMedicaoHoraria(resultado));
                }
            }
            return medicoes;
        } catch (SQLException e) {
            throw criarErro("listar as medições horárias", e);
        }
    }

    /**
     * Agrega os dados diários de uma estação. O nível de alerta e as janelas
     * de chuva são calculados separadamente pela camada Service.
     *
     * @return resumo agregado ou {@code null} quando não houver medições
     */
    public ResumoMeteorologico buscarResumoDiarioPorEstacao(
            String stationId,
            LocalDate dataInicial,
            LocalDate dataFinal
    ) throws SQLException {
        validarStationId(stationId);
        validarPeriodo(dataInicial, dataFinal);

        try (
                Connection conexao = conexaoMySQL.abrirConexao();
                PreparedStatement statement = conexao.prepareStatement(
                        SQL_BUSCAR_RESUMO_DIARIO
                )
        ) {
            statement.setString(1, stationId.trim());
            statement.setDate(2, Date.valueOf(dataInicial));
            statement.setDate(3, Date.valueOf(dataFinal));

            try (ResultSet resultado = statement.executeQuery()) {
                if (!resultado.next()) {
                    return null;
                }

                return mapearResumoMeteorologico(resultado);
            }
        } catch (SQLException e) {
            throw criarErro("buscar o resumo meteorológico diário", e);
        }
    }

    /**
     * Retorna um resumo agregado por estação para alimentar o heatmap.
     * A consulta única evita abrir uma conexão para cada estação.
     */
    public List<ResumoMeteorologico> listarResumosDiariosPorEstacao(
            LocalDate dataInicial,
            LocalDate dataFinal
    ) throws SQLException {
        validarPeriodo(dataInicial, dataFinal);
        List<ResumoMeteorologico> resumos = new ArrayList<>();

        try (
                Connection conexao = conexaoMySQL.abrirConexao();
                PreparedStatement statement = conexao.prepareStatement(
                        SQL_LISTAR_RESUMOS_DIARIOS
                )
        ) {
            statement.setDate(1, Date.valueOf(dataInicial));
            statement.setDate(2, Date.valueOf(dataFinal));

            try (ResultSet resultado = statement.executeQuery()) {
                while (resultado.next()) {
                    resumos.add(mapearResumoMeteorologico(resultado));
                }
            }
            return resumos;
        } catch (SQLException e) {
            throw criarErro(
                    "listar os resumos meteorológicos por estação",
                    e
            );
        }
    }

    /**
     * Soma {@code history_hourly.precip_total} na janela
     * {@code (referencia - horas, referencia]}.
     *
     * <p>Retorna {@code null} quando a janela não possui nenhum valor não nulo.
     * O schema não registra a cadência esperada das observações, portanto o
     * DAO não pode afirmar se eventuais lacunas tornam a janela incompleta.</p>
     */
    public Double buscarChuvaAcumulada(
            String stationId,
            LocalDateTime referencia,
            int horas
    ) throws SQLException {
        validarStationId(stationId);
        Objects.requireNonNull(referencia, "A data/hora de referência é obrigatória.");
        if (horas <= 0) {
            throw new IllegalArgumentException(
                    "A quantidade de horas deve ser maior que zero."
            );
        }

        LocalDateTime inicio = referencia.minusHours(horas);

        try (
                Connection conexao = conexaoMySQL.abrirConexao();
                PreparedStatement statement = conexao.prepareStatement(
                        SQL_BUSCAR_CHUVA_HORARIA
                )
        ) {
            statement.setString(1, stationId.trim());
            statement.setTimestamp(2, Timestamp.valueOf(inicio));
            statement.setTimestamp(3, Timestamp.valueOf(referencia));

            try (ResultSet resultado = statement.executeQuery()) {
                if (!resultado.next()
                        || resultado.getLong("quantidade_valores") == 0) {
                    return null;
                }
                return getDoubleNullable(resultado, "chuva_acumulada");
            }
        } catch (SQLException e) {
            throw criarErro("buscar a chuva acumulada horária", e);
        }
    }

    private static MedicaoDiaria mapearMedicaoDiaria(
            ResultSet resultado
    ) throws SQLException {
        return new MedicaoDiaria(
                getLongNullable(resultado, "id"),
                resultado.getString("station_id"),
                getLocalDateNullable(resultado, "obs_date"),
                getLocalDateTimeNullable(resultado, "obs_time_utc"),
                getLocalDateTimeNullable(resultado, "obs_time_local"),
                getLongNullable(resultado, "epoch"),
                resultado.getString("tz"),
                getDoubleNullable(resultado, "lat"),
                getDoubleNullable(resultado, "lon"),
                getIntegerNullable(resultado, "qc_status"),
                getDoubleNullable(resultado, "solar_radiation_high"),
                getDoubleNullable(resultado, "uv_high"),
                getDoubleNullable(resultado, "winddir_avg"),
                getDoubleNullable(resultado, "humidity_high"),
                getDoubleNullable(resultado, "humidity_low"),
                getDoubleNullable(resultado, "humidity_avg"),
                getDoubleNullable(resultado, "temp_high"),
                getDoubleNullable(resultado, "temp_low"),
                getDoubleNullable(resultado, "temp_avg"),
                getDoubleNullable(resultado, "dewpt_high"),
                getDoubleNullable(resultado, "dewpt_low"),
                getDoubleNullable(resultado, "dewpt_avg"),
                getDoubleNullable(resultado, "heatindex_high"),
                getDoubleNullable(resultado, "heatindex_low"),
                getDoubleNullable(resultado, "heatindex_avg"),
                getDoubleNullable(resultado, "windchill_high"),
                getDoubleNullable(resultado, "windchill_low"),
                getDoubleNullable(resultado, "windchill_avg"),
                getDoubleNullable(resultado, "windspeed_high"),
                getDoubleNullable(resultado, "windspeed_low"),
                getDoubleNullable(resultado, "windspeed_avg"),
                getDoubleNullable(resultado, "windgust_high"),
                getDoubleNullable(resultado, "windgust_low"),
                getDoubleNullable(resultado, "windgust_avg"),
                getDoubleNullable(resultado, "pressure_max"),
                getDoubleNullable(resultado, "pressure_min"),
                getDoubleNullable(resultado, "pressure_trend"),
                getDoubleNullable(resultado, "precip_rate"),
                getDoubleNullable(resultado, "precip_total"),
                getLocalDateTimeNullable(resultado, "fetched_at")
        );
    }

    private static MedicaoHoraria mapearMedicaoHoraria(
            ResultSet resultado
    ) throws SQLException {
        return new MedicaoHoraria(
                getLongNullable(resultado, "id"),
                resultado.getString("station_id"),
                getLocalDateTimeNullable(resultado, "obs_time_utc"),
                getLocalDateTimeNullable(resultado, "obs_time_local"),
                getLongNullable(resultado, "epoch"),
                resultado.getString("tz"),
                getDoubleNullable(resultado, "lat"),
                getDoubleNullable(resultado, "lon"),
                getIntegerNullable(resultado, "qc_status"),
                getDoubleNullable(resultado, "solar_radiation_high"),
                getDoubleNullable(resultado, "uv_high"),
                getDoubleNullable(resultado, "winddir_avg"),
                getDoubleNullable(resultado, "humidity_high"),
                getDoubleNullable(resultado, "humidity_low"),
                getDoubleNullable(resultado, "humidity_avg"),
                getDoubleNullable(resultado, "temp_high"),
                getDoubleNullable(resultado, "temp_low"),
                getDoubleNullable(resultado, "temp_avg"),
                getDoubleNullable(resultado, "dewpt_high"),
                getDoubleNullable(resultado, "dewpt_low"),
                getDoubleNullable(resultado, "dewpt_avg"),
                getDoubleNullable(resultado, "heatindex_high"),
                getDoubleNullable(resultado, "heatindex_low"),
                getDoubleNullable(resultado, "heatindex_avg"),
                getDoubleNullable(resultado, "windchill_high"),
                getDoubleNullable(resultado, "windchill_low"),
                getDoubleNullable(resultado, "windchill_avg"),
                getDoubleNullable(resultado, "windspeed_high"),
                getDoubleNullable(resultado, "windspeed_low"),
                getDoubleNullable(resultado, "windspeed_avg"),
                getDoubleNullable(resultado, "windgust_high"),
                getDoubleNullable(resultado, "windgust_low"),
                getDoubleNullable(resultado, "windgust_avg"),
                getDoubleNullable(resultado, "pressure_max"),
                getDoubleNullable(resultado, "pressure_min"),
                getDoubleNullable(resultado, "pressure_trend"),
                getDoubleNullable(resultado, "precip_rate"),
                getDoubleNullable(resultado, "precip_total"),
                getLocalDateTimeNullable(resultado, "fetched_at")
        );
    }

    private static ResumoMeteorologico mapearResumoMeteorologico(
            ResultSet resultado
    ) throws SQLException {
        ResumoMeteorologico resumo = new ResumoMeteorologico();
        resumo.setStationId(resultado.getString("station_id"));
        resumo.setNomeEstacao(resultado.getString("station_name"));
        resumo.setTemperaturaMinima(
                getDoubleNullable(resultado, "temperatura_minima")
        );
        resumo.setTemperaturaMaxima(
                getDoubleNullable(resultado, "temperatura_maxima")
        );
        resumo.setTemperaturaMedia(
                getDoubleNullable(resultado, "temperatura_media")
        );
        resumo.setUmidadeMedia(
                getDoubleNullable(resultado, "umidade_media")
        );
        resumo.setVentoMedio(
                getDoubleNullable(resultado, "vento_medio")
        );
        resumo.setVentoMaximo(
                getDoubleNullable(resultado, "vento_maximo")
        );
        resumo.setPressaoMedia(
                getDoubleNullable(resultado, "pressao_media")
        );
        resumo.setChuvaTotal(
                getDoubleNullable(resultado, "chuva_total")
        );
        return resumo;
    }

    private static Double getDoubleNullable(
            ResultSet resultado,
            String coluna
    ) throws SQLException {
        double valor = resultado.getDouble(coluna);
        return resultado.wasNull() ? null : valor;
    }

    private static Integer getIntegerNullable(
            ResultSet resultado,
            String coluna
    ) throws SQLException {
        int valor = resultado.getInt(coluna);
        return resultado.wasNull() ? null : valor;
    }

    private static Long getLongNullable(
            ResultSet resultado,
            String coluna
    ) throws SQLException {
        long valor = resultado.getLong(coluna);
        return resultado.wasNull() ? null : valor;
    }

    private static LocalDate getLocalDateNullable(
            ResultSet resultado,
            String coluna
    ) throws SQLException {
        Date valor = resultado.getDate(coluna);
        return valor == null ? null : valor.toLocalDate();
    }

    private static LocalDateTime getLocalDateTimeNullable(
            ResultSet resultado,
            String coluna
    ) throws SQLException {
        Timestamp valor = resultado.getTimestamp(coluna);
        return valor == null ? null : valor.toLocalDateTime();
    }

    private static void validarStationId(String stationId) {
        if (stationId == null || stationId.isBlank()) {
            throw new IllegalArgumentException(
                    "O identificador da estação não pode ficar vazio."
            );
        }
    }

    private static void validarPeriodo(
            LocalDate inicio,
            LocalDate fim
    ) {
        Objects.requireNonNull(inicio, "A data inicial é obrigatória.");
        Objects.requireNonNull(fim, "A data final é obrigatória.");
        if (inicio.isAfter(fim)) {
            throw new IllegalArgumentException(
                    "A data inicial não pode ser posterior à data final."
            );
        }
    }

    private static void validarPeriodo(
            LocalDateTime inicio,
            LocalDateTime fim
    ) {
        Objects.requireNonNull(inicio, "A data/hora inicial é obrigatória.");
        Objects.requireNonNull(fim, "A data/hora final é obrigatória.");
        if (inicio.isAfter(fim)) {
            throw new IllegalArgumentException(
                    "A data/hora inicial não pode ser posterior à final."
            );
        }
    }

    private static SQLException criarErro(String operacao, SQLException causa) {
        return new SQLException(
                "Erro ao " + operacao + ": " + causa.getMessage(),
                causa.getSQLState(),
                causa.getErrorCode(),
                causa
        );
    }
}
