package map;

import model.Estacao;
import model.NivelAlerta;
import model.TendenciaChuva;
import org.jxmapviewer.viewer.GeoPosition;
import org.jxmapviewer.viewer.Waypoint;

import java.util.Objects;

/**
 * Associa uma estação meteorológica a uma posição no mapa.
 */
public class EstacaoWaypoint implements Waypoint {

    private final Estacao estacao;
    private GeoPosition posicao;
    private NivelAlerta nivelAlerta;
    private TendenciaChuva tendenciaChuva;
    private Double valorVariavel;

    public EstacaoWaypoint(Estacao estacao, GeoPosition posicao) {
        this(estacao, posicao, null);
    }

    public EstacaoWaypoint(
            Estacao estacao,
            GeoPosition posicao,
            NivelAlerta nivelAlerta
    ) {
        this.estacao = Objects.requireNonNull(
                estacao,
                "A estação não pode ser nula."
        );
        this.posicao = Objects.requireNonNull(
                posicao,
                "A posição não pode ser nula."
        );
        this.nivelAlerta = nivelAlerta;
    }

    public Estacao getEstacao() {
        return estacao;
    }

    public GeoPosition getPosicao() {
        return posicao;
    }

    public NivelAlerta getNivelAlerta() {
        return nivelAlerta;
    }

    public TendenciaChuva getTendenciaChuva() {
        return tendenciaChuva;
    }

    public void setTendenciaChuva(TendenciaChuva tendenciaChuva) {
        this.tendenciaChuva = tendenciaChuva;
    }

    public void setNivelAlerta(NivelAlerta nivelAlerta) {
        this.nivelAlerta = nivelAlerta;
    }

    public Double getValorVariavel() {
        return valorVariavel;
    }

    public void setValorVariavel(Double valorVariavel) {
        this.valorVariavel = valorVariavel;
    }

    @Override
    public GeoPosition getPosition() {
        return posicao;
    }

    @Override
    public String toString() {
        return estacao + " @ "
                + posicao.getLatitude()
                + ", "
                + posicao.getLongitude()
                + " [" + (nivelAlerta == null
                        ? "NAO_CALCULADO"
                        : nivelAlerta)
                + "]";
    }
}
