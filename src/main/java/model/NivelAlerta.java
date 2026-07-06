package model;

/**
 * Níveis calculados pela regra de alerta de alagamento.
 */
public enum NivelAlerta {
    NORMAL,
    ATENCAO,
    ALERTA,
    EMERGENCIA;

    @Override
    public String toString() {
        return switch (this) {
            case NORMAL -> "Normal";
            case ATENCAO -> "Atenção";
            case ALERTA -> "Alerta";
            case EMERGENCIA -> "Emergência";
        };
    }
}
