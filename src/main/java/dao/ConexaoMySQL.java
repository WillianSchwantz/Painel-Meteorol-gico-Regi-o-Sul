package dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Centraliza a configuração e a abertura de conexões JDBC com o MySQL.
 *
 * <p>A configuração pode ser fornecida por propriedades da JVM ou por
 * variáveis de ambiente. Propriedades da JVM têm precedência.</p>
 */
public final class ConexaoMySQL {

    private static final String DRIVER_MYSQL = "com.mysql.cj.jdbc.Driver";

    private static final String HOST_PADRAO = "localhost";
    private static final int PORTA_PADRAO = 3306;
    private static final String BANCO_PADRAO = "weather_pws";
    private static final String USUARIO_PADRAO = "root";
    private static final String SENHA_PADRAO = "";

    private final String host;
    private final int porta;
    private final String banco;
    private final String usuario;
    private final String senha;

    /**
     * Cria a configuração usando propriedades da JVM, variáveis de ambiente
     * ou os valores locais padrão.
     *
     * <table>
     *     <caption>Opções de configuração</caption>
     *     <tr><th>Propriedade JVM</th><th>Variável de ambiente</th></tr>
     *     <tr><td>weather.db.host</td><td>WEATHER_DB_HOST</td></tr>
     *     <tr><td>weather.db.port</td><td>WEATHER_DB_PORT</td></tr>
     *     <tr><td>weather.db.name</td><td>WEATHER_DB_NAME</td></tr>
     *     <tr><td>weather.db.user</td><td>WEATHER_DB_USER</td></tr>
     *     <tr><td>weather.db.password</td><td>WEATHER_DB_PASSWORD</td></tr>
     * </table>
     */
    public ConexaoMySQL() {
        this(
                lerTexto("weather.db.host", "WEATHER_DB_HOST", HOST_PADRAO),
                lerPorta(),
                lerTexto("weather.db.name", "WEATHER_DB_NAME", BANCO_PADRAO),
                lerTexto("weather.db.user", "WEATHER_DB_USER", USUARIO_PADRAO),
                lerSenha()
        );
    }

    /**
     * Cria uma configuração informada diretamente pela aplicação.
     */
    public ConexaoMySQL(
            String host,
            int porta,
            String banco,
            String usuario,
            String senha
    ) {
        this.host = exigirTexto(host, "host");
        this.porta = validarPorta(porta);
        this.banco = validarNomeBanco(banco);
        this.usuario = exigirTexto(usuario, "usuário");
        this.senha = senha == null ? "" : senha;
    }

    /**
     * Abre uma nova conexão JDBC. Quem chama este método é responsável por
     * fechar a conexão, preferencialmente com try-with-resources.
     *
     * @return conexão aberta com o banco configurado
     * @throws SQLException se o driver não estiver disponível ou se o MySQL
     *                      recusar/não puder estabelecer a conexão
     */
    public Connection abrirConexao() throws SQLException {
        carregarDriver();

        try {
            return DriverManager.getConnection(criarUrlJdbc(), usuario, senha);
        } catch (SQLException e) {
            String mensagem = String.format(
                    "Não foi possível conectar ao MySQL em %s usando o usuário '%s'. "
                            + "Verifique servidor, porta, banco, usuário e senha. Detalhes: %s",
                    getDescricaoDestino(),
                    usuario,
                    e.getMessage()
            );

            throw new SQLException(
                    mensagem,
                    e.getSQLState(),
                    e.getErrorCode(),
                    e
            );
        }
    }

    /**
     * Retorna o destino sem expor a senha.
     */
    public String getDescricaoDestino() {
        return host + ":" + porta + "/" + banco;
    }

    private String criarUrlJdbc() {
        return String.format(
                "jdbc:mysql://%s:%d/%s"
                        + "?serverTimezone=America/Sao_Paulo"
                        + "&useUnicode=true"
                        + "&characterEncoding=UTF-8"
                        + "&connectTimeout=5000",
                host,
                porta,
                banco
        );
    }

    private static void carregarDriver() throws SQLException {
        try {
            Class.forName(DRIVER_MYSQL);
        } catch (ClassNotFoundException e) {
            throw new SQLException(
                    "Driver JDBC do MySQL não encontrado. "
                            + "Verifique a dependência com.mysql:mysql-connector-j no pom.xml.",
                    e
            );
        }
    }

    private static int lerPorta() {
        String valor = lerTexto(
                "weather.db.port",
                "WEATHER_DB_PORT",
                Integer.toString(PORTA_PADRAO)
        );

        try {
            return validarPorta(Integer.parseInt(valor));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    "A porta do MySQL deve ser um número inteiro: " + valor,
                    e
            );
        }
    }

    private static String lerTexto(
            String nomePropriedade,
            String nomeVariavel,
            String valorPadrao
    ) {
        String propriedade = System.getProperty(nomePropriedade);
        if (propriedade != null && !propriedade.isBlank()) {
            return propriedade.trim();
        }

        String variavel = System.getenv(nomeVariavel);
        if (variavel != null && !variavel.isBlank()) {
            return variavel.trim();
        }

        return valorPadrao;
    }

    private static String lerSenha() {
        String propriedade = System.getProperty("weather.db.password");
        if (propriedade != null) {
            return propriedade;
        }

        String variavel = System.getenv("WEATHER_DB_PASSWORD");
        return variavel == null ? SENHA_PADRAO : variavel;
    }

    private static String exigirTexto(String valor, String nomeCampo) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException(
                    "O campo " + nomeCampo + " não pode ficar vazio."
            );
        }
        return valor.trim();
    }

    private static int validarPorta(int porta) {
        if (porta < 1 || porta > 65_535) {
            throw new IllegalArgumentException(
                    "A porta do MySQL deve estar entre 1 e 65535: " + porta
            );
        }
        return porta;
    }

    private static String validarNomeBanco(String banco) {
        String nome = exigirTexto(banco, "banco");
        if (!nome.matches("[A-Za-z0-9_]+")) {
            throw new IllegalArgumentException(
                    "O nome do banco contém caracteres inválidos: " + nome
            );
        }
        return nome;
    }
}
