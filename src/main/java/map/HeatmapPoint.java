package map;

/**
 * Valor meteorológico já associado à coordenada de uma estação.
 */
public class HeatmapPoint {

    private final String stationId;
    private final double latitude;
    private final double longitude;
    private final Double valor;

    public HeatmapPoint(
            String stationId,
            double latitude,
            double longitude,
            Double valor
    ) {
        this.stationId = stationId;
        this.latitude = latitude;
        this.longitude = longitude;
        this.valor = valor;
    }

    public String getStationId() {
        return stationId;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public Double getValor() {
        return valor;
    }

    @Override
    public String toString() {
        return "HeatmapPoint{"
                + "stationId='" + stationId + '\''
                + ", latitude=" + latitude
                + ", longitude=" + longitude
                + ", valor=" + valor
                + '}';
    }
}
