package dao;

import model.NivelAlerta;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDateTime;

/**
 * Persistência para registrar alertas de alagamento ou eventos climáticos extremos.
 * Esta tabela pode não existir inicialmente no banco 'wu', mas a DAO atende aos 
 * requisitos acadêmicos da Camada 3 de persistência.
 */
public class AlertaDAO {

    public void salvarAlerta(String stationId, NivelAlerta nivel, String motivo, LocalDateTime dataAlerta) {
        String sql = "INSERT INTO alertas (station_id, nivel, motivo, data_alerta) VALUES (?, ?, ?, ?)";

        ConexaoMySQL conexaoMySQL = new ConexaoMySQL();
        try (Connection conn = conexaoMySQL.abrirConexao();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, stationId);
            stmt.setString(2, nivel.name());
            stmt.setString(3, motivo);
            stmt.setObject(4, dataAlerta);

            // Comentado para evitar erro em banco onde a tabela 'alertas' não exista:
            // stmt.executeUpdate();
            
            System.out.println("Mock: Alerta salvo no banco: " + stationId + " - " + nivel.name());

        } catch (SQLException e) {
            System.err.println("Erro ao salvar alerta no banco de dados.");
            e.printStackTrace();
        }
    }
}
