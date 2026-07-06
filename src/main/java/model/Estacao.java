package model;

import java.time.LocalDateTime;

/**
 * Representa um registro da tabela {@code stations}.
 */
public class Estacao {

    // Coluna SQL: station_id
    private String stationId;

    // Coluna SQL: station_name
    private String stationName;

    private Double latitude;
    private Double longitude;

    // Coluna SQL: qc_status_label
    private String qcStatusLabel;

    // Coluna SQL: partner_id
    private String partnerId;

    // Coluna SQL: last_update_utc
    private LocalDateTime lastUpdateUtc;

    // Coluna SQL: created_at
    private LocalDateTime createdAt;

    public Estacao() {
    }

    public Estacao(
            String stationId,
            String stationName,
            Double latitude,
            Double longitude,
            String qcStatusLabel,
            String partnerId,
            LocalDateTime lastUpdateUtc,
            LocalDateTime createdAt
    ) {
        this.stationId = stationId;
        this.stationName = stationName;
        this.latitude = latitude;
        this.longitude = longitude;
        this.qcStatusLabel = qcStatusLabel;
        this.partnerId = partnerId;
        this.lastUpdateUtc = lastUpdateUtc;
        this.createdAt = createdAt;
    }

    public String getStationId() {
        return stationId;
    }

    public void setStationId(String stationId) {
        this.stationId = stationId;
    }

    public String getStationName() {
        return stationName;
    }

    public void setStationName(String stationName) {
        this.stationName = stationName;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public String getQcStatusLabel() {
        return qcStatusLabel;
    }

    public void setQcStatusLabel(String qcStatusLabel) {
        this.qcStatusLabel = qcStatusLabel;
    }

    public String getPartnerId() {
        return partnerId;
    }

    public void setPartnerId(String partnerId) {
        this.partnerId = partnerId;
    }

    public LocalDateTime getLastUpdateUtc() {
        return lastUpdateUtc;
    }

    public void setLastUpdateUtc(LocalDateTime lastUpdateUtc) {
        this.lastUpdateUtc = lastUpdateUtc;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    /**
     * Texto usado pelo JComboBox de estações.
     */
    @Override
    public String toString() {
        if (stationId == null || stationId.isBlank()) {
            return stationName == null ? "" : stationName;
        }
        if (stationName == null || stationName.isBlank()) {
            return stationId;
        }
        return stationId + " - " + stationName;
    }
}
