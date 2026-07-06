package model;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Representa um registro da tabela {@code history_daily}.
 */
public class MedicaoDiaria {

    private Long id;

    // Coluna SQL: station_id
    private String stationId;

    // Coluna SQL: obs_date
    private LocalDate obsDate;

    // Coluna SQL: obs_time_utc
    private LocalDateTime obsTimeUtc;

    // Coluna SQL: obs_time_local
    private LocalDateTime obsTimeLocal;

    private Long epoch;
    private String tz;
    private Double lat;
    private Double lon;

    // Coluna SQL: qc_status
    private Integer qcStatus;

    // Coluna SQL: solar_radiation_high
    private Double solarRadiationHigh;

    // Coluna SQL: uv_high
    private Double uvHigh;

    // Coluna SQL: winddir_avg
    private Double winddirAvg;

    // Coluna SQL: humidity_high
    private Double humidityHigh;

    // Coluna SQL: humidity_low
    private Double humidityLow;

    // Coluna SQL: humidity_avg
    private Double humidityAvg;

    // Coluna SQL: temp_high
    private Double tempHigh;

    // Coluna SQL: temp_low
    private Double tempLow;

    // Coluna SQL: temp_avg
    private Double tempAvg;

    // Coluna SQL: dewpt_high
    private Double dewptHigh;

    // Coluna SQL: dewpt_low
    private Double dewptLow;

    // Coluna SQL: dewpt_avg
    private Double dewptAvg;

    // Coluna SQL: heatindex_high
    private Double heatindexHigh;

    // Coluna SQL: heatindex_low
    private Double heatindexLow;

    // Coluna SQL: heatindex_avg
    private Double heatindexAvg;

    // Coluna SQL: windchill_high
    private Double windchillHigh;

    // Coluna SQL: windchill_low
    private Double windchillLow;

    // Coluna SQL: windchill_avg
    private Double windchillAvg;

    // Coluna SQL: windspeed_high
    private Double windspeedHigh;

    // Coluna SQL: windspeed_low
    private Double windspeedLow;

    // Coluna SQL: windspeed_avg
    private Double windspeedAvg;

    // Coluna SQL: windgust_high
    private Double windgustHigh;

    // Coluna SQL: windgust_low
    private Double windgustLow;

    // Coluna SQL: windgust_avg
    private Double windgustAvg;

    // Coluna SQL: pressure_max
    private Double pressureMax;

    // Coluna SQL: pressure_min
    private Double pressureMin;

    // Coluna SQL: pressure_trend
    private Double pressureTrend;

    // Coluna SQL: precip_rate
    private Double precipRate;

    // Coluna SQL: precip_total
    private Double precipTotal;

    // Coluna SQL: fetched_at
    private LocalDateTime fetchedAt;

    public MedicaoDiaria() {
    }

    public MedicaoDiaria(
            Long id,
            String stationId,
            LocalDate obsDate,
            LocalDateTime obsTimeUtc,
            LocalDateTime obsTimeLocal,
            Long epoch,
            String tz,
            Double lat,
            Double lon,
            Integer qcStatus,
            Double solarRadiationHigh,
            Double uvHigh,
            Double winddirAvg,
            Double humidityHigh,
            Double humidityLow,
            Double humidityAvg,
            Double tempHigh,
            Double tempLow,
            Double tempAvg,
            Double dewptHigh,
            Double dewptLow,
            Double dewptAvg,
            Double heatindexHigh,
            Double heatindexLow,
            Double heatindexAvg,
            Double windchillHigh,
            Double windchillLow,
            Double windchillAvg,
            Double windspeedHigh,
            Double windspeedLow,
            Double windspeedAvg,
            Double windgustHigh,
            Double windgustLow,
            Double windgustAvg,
            Double pressureMax,
            Double pressureMin,
            Double pressureTrend,
            Double precipRate,
            Double precipTotal,
            LocalDateTime fetchedAt
    ) {
        this.id = id;
        this.stationId = stationId;
        this.obsDate = obsDate;
        this.obsTimeUtc = obsTimeUtc;
        this.obsTimeLocal = obsTimeLocal;
        this.epoch = epoch;
        this.tz = tz;
        this.lat = lat;
        this.lon = lon;
        this.qcStatus = qcStatus;
        this.solarRadiationHigh = solarRadiationHigh;
        this.uvHigh = uvHigh;
        this.winddirAvg = winddirAvg;
        this.humidityHigh = humidityHigh;
        this.humidityLow = humidityLow;
        this.humidityAvg = humidityAvg;
        this.tempHigh = tempHigh;
        this.tempLow = tempLow;
        this.tempAvg = tempAvg;
        this.dewptHigh = dewptHigh;
        this.dewptLow = dewptLow;
        this.dewptAvg = dewptAvg;
        this.heatindexHigh = heatindexHigh;
        this.heatindexLow = heatindexLow;
        this.heatindexAvg = heatindexAvg;
        this.windchillHigh = windchillHigh;
        this.windchillLow = windchillLow;
        this.windchillAvg = windchillAvg;
        this.windspeedHigh = windspeedHigh;
        this.windspeedLow = windspeedLow;
        this.windspeedAvg = windspeedAvg;
        this.windgustHigh = windgustHigh;
        this.windgustLow = windgustLow;
        this.windgustAvg = windgustAvg;
        this.pressureMax = pressureMax;
        this.pressureMin = pressureMin;
        this.pressureTrend = pressureTrend;
        this.precipRate = precipRate;
        this.precipTotal = precipTotal;
        this.fetchedAt = fetchedAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getStationId() {
        return stationId;
    }

    public void setStationId(String stationId) {
        this.stationId = stationId;
    }

    public LocalDate getObsDate() {
        return obsDate;
    }

    public void setObsDate(LocalDate obsDate) {
        this.obsDate = obsDate;
    }

    public LocalDateTime getObsTimeUtc() {
        return obsTimeUtc;
    }

    public void setObsTimeUtc(LocalDateTime obsTimeUtc) {
        this.obsTimeUtc = obsTimeUtc;
    }

    public LocalDateTime getObsTimeLocal() {
        return obsTimeLocal;
    }

    public void setObsTimeLocal(LocalDateTime obsTimeLocal) {
        this.obsTimeLocal = obsTimeLocal;
    }

    public Long getEpoch() {
        return epoch;
    }

    public void setEpoch(Long epoch) {
        this.epoch = epoch;
    }

    public String getTz() {
        return tz;
    }

    public void setTz(String tz) {
        this.tz = tz;
    }

    public Double getLat() {
        return lat;
    }

    public void setLat(Double lat) {
        this.lat = lat;
    }

    public Double getLon() {
        return lon;
    }

    public void setLon(Double lon) {
        this.lon = lon;
    }

    public Integer getQcStatus() {
        return qcStatus;
    }

    public void setQcStatus(Integer qcStatus) {
        this.qcStatus = qcStatus;
    }

    public Double getSolarRadiationHigh() {
        return solarRadiationHigh;
    }

    public void setSolarRadiationHigh(Double solarRadiationHigh) {
        this.solarRadiationHigh = solarRadiationHigh;
    }

    public Double getUvHigh() {
        return uvHigh;
    }

    public void setUvHigh(Double uvHigh) {
        this.uvHigh = uvHigh;
    }

    public Double getWinddirAvg() {
        return winddirAvg;
    }

    public void setWinddirAvg(Double winddirAvg) {
        this.winddirAvg = winddirAvg;
    }

    public Double getHumidityHigh() {
        return humidityHigh;
    }

    public void setHumidityHigh(Double humidityHigh) {
        this.humidityHigh = humidityHigh;
    }

    public Double getHumidityLow() {
        return humidityLow;
    }

    public void setHumidityLow(Double humidityLow) {
        this.humidityLow = humidityLow;
    }

    public Double getHumidityAvg() {
        return humidityAvg;
    }

    public void setHumidityAvg(Double humidityAvg) {
        this.humidityAvg = humidityAvg;
    }

    public Double getTempHigh() {
        return tempHigh;
    }

    public void setTempHigh(Double tempHigh) {
        this.tempHigh = tempHigh;
    }

    public Double getTempLow() {
        return tempLow;
    }

    public void setTempLow(Double tempLow) {
        this.tempLow = tempLow;
    }

    public Double getTempAvg() {
        return tempAvg;
    }

    public void setTempAvg(Double tempAvg) {
        this.tempAvg = tempAvg;
    }

    public Double getDewptHigh() {
        return dewptHigh;
    }

    public void setDewptHigh(Double dewptHigh) {
        this.dewptHigh = dewptHigh;
    }

    public Double getDewptLow() {
        return dewptLow;
    }

    public void setDewptLow(Double dewptLow) {
        this.dewptLow = dewptLow;
    }

    public Double getDewptAvg() {
        return dewptAvg;
    }

    public void setDewptAvg(Double dewptAvg) {
        this.dewptAvg = dewptAvg;
    }

    public Double getHeatindexHigh() {
        return heatindexHigh;
    }

    public void setHeatindexHigh(Double heatindexHigh) {
        this.heatindexHigh = heatindexHigh;
    }

    public Double getHeatindexLow() {
        return heatindexLow;
    }

    public void setHeatindexLow(Double heatindexLow) {
        this.heatindexLow = heatindexLow;
    }

    public Double getHeatindexAvg() {
        return heatindexAvg;
    }

    public void setHeatindexAvg(Double heatindexAvg) {
        this.heatindexAvg = heatindexAvg;
    }

    public Double getWindchillHigh() {
        return windchillHigh;
    }

    public void setWindchillHigh(Double windchillHigh) {
        this.windchillHigh = windchillHigh;
    }

    public Double getWindchillLow() {
        return windchillLow;
    }

    public void setWindchillLow(Double windchillLow) {
        this.windchillLow = windchillLow;
    }

    public Double getWindchillAvg() {
        return windchillAvg;
    }

    public void setWindchillAvg(Double windchillAvg) {
        this.windchillAvg = windchillAvg;
    }

    public Double getWindspeedHigh() {
        return windspeedHigh;
    }

    public void setWindspeedHigh(Double windspeedHigh) {
        this.windspeedHigh = windspeedHigh;
    }

    public Double getWindspeedLow() {
        return windspeedLow;
    }

    public void setWindspeedLow(Double windspeedLow) {
        this.windspeedLow = windspeedLow;
    }

    public Double getWindspeedAvg() {
        return windspeedAvg;
    }

    public void setWindspeedAvg(Double windspeedAvg) {
        this.windspeedAvg = windspeedAvg;
    }

    public Double getWindgustHigh() {
        return windgustHigh;
    }

    public void setWindgustHigh(Double windgustHigh) {
        this.windgustHigh = windgustHigh;
    }

    public Double getWindgustLow() {
        return windgustLow;
    }

    public void setWindgustLow(Double windgustLow) {
        this.windgustLow = windgustLow;
    }

    public Double getWindgustAvg() {
        return windgustAvg;
    }

    public void setWindgustAvg(Double windgustAvg) {
        this.windgustAvg = windgustAvg;
    }

    public Double getPressureMax() {
        return pressureMax;
    }

    public void setPressureMax(Double pressureMax) {
        this.pressureMax = pressureMax;
    }

    public Double getPressureMin() {
        return pressureMin;
    }

    public void setPressureMin(Double pressureMin) {
        this.pressureMin = pressureMin;
    }

    public Double getPressureTrend() {
        return pressureTrend;
    }

    public void setPressureTrend(Double pressureTrend) {
        this.pressureTrend = pressureTrend;
    }

    public Double getPrecipRate() {
        return precipRate;
    }

    public void setPrecipRate(Double precipRate) {
        this.precipRate = precipRate;
    }

    public Double getPrecipTotal() {
        return precipTotal;
    }

    public void setPrecipTotal(Double precipTotal) {
        this.precipTotal = precipTotal;
    }

    public LocalDateTime getFetchedAt() {
        return fetchedAt;
    }

    public void setFetchedAt(LocalDateTime fetchedAt) {
        this.fetchedAt = fetchedAt;
    }

    @Override
    public String toString() {
        return "MedicaoDiaria{"
                + "id=" + id
                + ", stationId='" + stationId + '\''
                + ", obsDate=" + obsDate
                + ", tempAvg=" + tempAvg
                + ", humidityAvg=" + humidityAvg
                + ", precipTotal=" + precipTotal
                + '}';
    }
}
