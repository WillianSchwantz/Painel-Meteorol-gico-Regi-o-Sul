package dao;

import model.Estacao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Acesso aos dados da tabela {@code stations}.
 */
public class EstacaoDAO {

    private static final String SQL_LISTAR_TODAS = """
            SELECT station_id,
                   station_name,
                   latitude,
                   longitude,
                   qc_status_label,
                   partner_id,
                   last_update_utc,
                   created_at
              FROM stations
             ORDER BY station_name, station_id
            """;

    private static final String SQL_BUSCAR_POR_ID = """
            SELECT station_id,
                   station_name,
                   latitude,
                   longitude,
                   qc_status_label,
                   partner_id,
                   last_update_utc,
                   created_at
              FROM stations
             WHERE station_id = ?
            """;

    private static final String SQL_LISTAR_POR_TEXTO = """
            SELECT station_id,
                   station_name,
                   latitude,
                   longitude,
                   qc_status_label,
                   partner_id,
                   last_update_utc,
                   created_at
              FROM stations
             WHERE station_name LIKE ?
                OR station_id LIKE ?
             ORDER BY station_name, station_id
            """;

    private static final String SQL_LISTAR_POR_CIDADE_OU_TEXTO = """
            SELECT s.station_id,
                   s.station_name,
                   s.latitude,
                   s.longitude,
                   s.qc_status_label,
                   s.partner_id,
                   s.last_update_utc,
                   s.created_at
              FROM stations s
              LEFT JOIN station_location l
                ON l.station_id = s.station_id
             WHERE s.station_name LIKE ?
                OR s.station_id LIKE ?
                OR l.cidade LIKE ?
                OR l.uf LIKE ?
                OR l.regiao LIKE ?
             ORDER BY COALESCE(l.uf, ''),
                      COALESCE(l.cidade, ''),
                      s.station_name,
                      s.station_id
            """;

    private static final String SQL_TABELA_LOCALIZACAO_EXISTE = """
            SELECT COUNT(*) AS total
              FROM information_schema.tables
             WHERE table_schema = DATABASE()
               AND table_name = 'station_location'
            """;

    private static final String SQL_LISTAR_LOCALIDADES = """
            SELECT DISTINCT cidade,
                   uf
              FROM station_location
             WHERE cidade IS NOT NULL
               AND cidade <> ''
             ORDER BY uf, cidade
            """;

    private final ConexaoMySQL conexaoMySQL;

    public EstacaoDAO() {
        this(new ConexaoMySQL());
    }

    public EstacaoDAO(ConexaoMySQL conexaoMySQL) {
        this.conexaoMySQL = Objects.requireNonNull(
                conexaoMySQL,
                "A configuração de conexão não pode ser nula."
        );
    }

    /**
     * Lista todas as estações cadastradas.
     *
     * @return lista vazia quando não houver estações
     */
    public List<Estacao> listarTodas() throws SQLException {
        List<Estacao> estacoes = new ArrayList<>();

        try (
                Connection conexao = conexaoMySQL.abrirConexao();
                PreparedStatement statement = conexao.prepareStatement(SQL_LISTAR_TODAS);
                ResultSet resultado = statement.executeQuery()
        ) {
            while (resultado.next()) {
                estacoes.add(mapearEstacao(resultado));
            }
            return estacoes;
        } catch (SQLException e) {
            throw criarErro("listar as estações", e);
        }
    }

    /**
     * Busca uma estação pela chave primária {@code station_id}.
     *
     * @return estação encontrada ou {@code null}
     */
    public Estacao buscarPorId(String stationId) throws SQLException {
        validarStationId(stationId);

        try (
                Connection conexao = conexaoMySQL.abrirConexao();
                PreparedStatement statement = conexao.prepareStatement(SQL_BUSCAR_POR_ID)
        ) {
            statement.setString(1, stationId.trim());

            try (ResultSet resultado = statement.executeQuery()) {
                return resultado.next() ? mapearEstacao(resultado) : null;
            }
        } catch (SQLException e) {
            throw criarErro("buscar a estação '" + stationId + "'", e);
        }
    }

    /**
     * Lista estações cujo nome ou identificador contém o texto informado.
     * Uma busca vazia equivale a listar todas as estações.
     */
    public List<Estacao> listarPorTexto(String texto) throws SQLException {
        String termo = texto == null ? "" : texto.trim();
        String padrao = "%" + termo + "%";
        List<Estacao> estacoes = new ArrayList<>();

        try (
                Connection conexao = conexaoMySQL.abrirConexao();
                PreparedStatement statement = conexao.prepareStatement(SQL_LISTAR_POR_TEXTO)
        ) {
            statement.setString(1, padrao);
            statement.setString(2, padrao);

            try (ResultSet resultado = statement.executeQuery()) {
                while (resultado.next()) {
                    estacoes.add(mapearEstacao(resultado));
                }
            }
            return estacoes;
        } catch (SQLException e) {
            throw criarErro("listar estações pelo texto informado", e);
        }
    }

    /**
     * Lista estacoes filtrando por cidade, UF ou regiao quando a tabela
     * auxiliar {@code station_location} existir. Sem essa tabela, mantem o
     * fallback por nome ou identificador da estacao.
     */
    public List<Estacao> listarPorCidadeOuTexto(String texto) throws SQLException {
        String termo = texto == null ? "" : texto.trim();
        if (termo.isEmpty()) {
            return listarTodas();
        }

        if (!possuiTabelaLocalizacao()) {
            return listarPorTexto(termo);
        }

        String padrao = "%" + termo + "%";
        List<Estacao> estacoes = new ArrayList<>();

        try (
                Connection conexao = conexaoMySQL.abrirConexao();
                PreparedStatement statement = conexao.prepareStatement(
                        SQL_LISTAR_POR_CIDADE_OU_TEXTO
                )
        ) {
            statement.setString(1, padrao);
            statement.setString(2, padrao);
            statement.setString(3, padrao);
            statement.setString(4, padrao);
            statement.setString(5, padrao);

            try (ResultSet resultado = statement.executeQuery()) {
                while (resultado.next()) {
                    estacoes.add(mapearEstacao(resultado));
                }
            }
            return estacoes;
        } catch (SQLException e) {
            throw criarErro("listar estacoes por cidade ou texto", e);
        }
    }

    public boolean possuiTabelaLocalizacao() throws SQLException {
        try (
                Connection conexao = conexaoMySQL.abrirConexao();
                PreparedStatement statement = conexao.prepareStatement(
                        SQL_TABELA_LOCALIZACAO_EXISTE
                );
                ResultSet resultado = statement.executeQuery()
        ) {
            return resultado.next() && resultado.getInt("total") > 0;
        } catch (SQLException e) {
            throw criarErro("verificar a tabela station_location", e);
        }
    }

    public List<String> listarLocalidades() throws SQLException {
        if (!possuiTabelaLocalizacao()) {
            return List.of();
        }

        List<String> localidades = new ArrayList<>();
        try (
                Connection conexao = conexaoMySQL.abrirConexao();
                PreparedStatement statement = conexao.prepareStatement(
                        SQL_LISTAR_LOCALIDADES
                );
                ResultSet resultado = statement.executeQuery()
        ) {
            while (resultado.next()) {
                String cidade = resultado.getString("cidade");
                String uf = resultado.getString("uf");
                if (uf == null || uf.isBlank()) {
                    localidades.add(cidade);
                } else {
                    localidades.add(cidade + " - " + uf);
                }
            }
            return localidades;
        } catch (SQLException e) {
            throw criarErro("listar cidades da tabela station_location", e);
        }
    }

    private static Estacao mapearEstacao(ResultSet resultado) throws SQLException {
        return new Estacao(
                resultado.getString("station_id"),
                resultado.getString("station_name"),
                getDoubleNullable(resultado, "latitude"),
                getDoubleNullable(resultado, "longitude"),
                resultado.getString("qc_status_label"),
                resultado.getString("partner_id"),
                getLocalDateTimeNullable(resultado, "last_update_utc"),
                getLocalDateTimeNullable(resultado, "created_at")
        );
    }

    private static Double getDoubleNullable(
            ResultSet resultado,
            String coluna
    ) throws SQLException {
        double valor = resultado.getDouble(coluna);
        return resultado.wasNull() ? null : valor;
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

    private static SQLException criarErro(String operacao, SQLException causa) {
        return new SQLException(
                "Erro ao " + operacao + ": " + causa.getMessage(),
                causa.getSQLState(),
                causa.getErrorCode(),
                causa
        );
    }
}
