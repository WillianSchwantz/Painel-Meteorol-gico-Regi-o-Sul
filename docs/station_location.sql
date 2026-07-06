-- Tabela complementar para atender ao filtro de cidade sem alterar o dump wu.sql.
-- Execute depois de importar o banco weather_pws.

USE weather_pws;

CREATE TABLE IF NOT EXISTS station_location (
    station_id VARCHAR(30) NOT NULL,
    cidade VARCHAR(100) NOT NULL,
    uf CHAR(2) NOT NULL,
    regiao VARCHAR(100) NULL,
    fonte VARCHAR(150) NULL,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (station_id),
    INDEX idx_station_location_cidade (cidade),
    INDEX idx_station_location_uf (uf),
    INDEX idx_station_location_regiao (regiao),
    CONSTRAINT fk_station_location_station
        FOREIGN KEY (station_id)
        REFERENCES stations (station_id)
        ON UPDATE CASCADE
        ON DELETE CASCADE
);

-- Exemplo de carga manual:
-- INSERT INTO station_location (station_id, cidade, uf, regiao, fonte)
-- VALUES ('IITAQU14', 'Itaqui', 'RS', 'Fronteira Oeste/Campanha', 'Cadastro manual academico')
-- ON DUPLICATE KEY UPDATE
--     cidade = VALUES(cidade),
--     uf = VALUES(uf),
--     regiao = VALUES(regiao),
--     fonte = VALUES(fonte);
