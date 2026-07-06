package map;

import model.NivelAlerta;
import org.jxmapviewer.JXMapViewer;
import org.jxmapviewer.painter.Painter;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.util.Collection;
import java.util.List;

/**
 * Desenha círculos translúcidos para áreas alagadiças com base no nível de alerta da estação.
 */
public class ZonaAlagamentoPainter implements Painter<JXMapViewer> {

    private final List<EstacaoWaypoint> waypoints;

    public ZonaAlagamentoPainter(Collection<EstacaoWaypoint> waypoints) {
        this.waypoints = List.copyOf(waypoints);
    }

    @Override
    public void paint(Graphics2D graphics, JXMapViewer mapa, int largura, int altura) {
        Graphics2D g2 = (Graphics2D) graphics.create();
        try {
            g2.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON
            );

            Rectangle viewport = mapa.getViewportBounds();
            g2.translate(-viewport.getX(), -viewport.getY());

            for (EstacaoWaypoint waypoint : waypoints) {
                desenharZona(g2, mapa, waypoint);
            }
        } finally {
            g2.dispose();
        }
    }

    private void desenharZona(Graphics2D g2, JXMapViewer mapa, EstacaoWaypoint waypoint) {
        NivelAlerta alerta = waypoint.getNivelAlerta();
        if (alerta == null || alerta == NivelAlerta.NORMAL) {
            return;
        }

        int diametro = obterDiametro(alerta);
        Color cor = obterCor(alerta);

        Point2D ponto = mapa.getTileFactory().geoToPixel(
                waypoint.getPosition(),
                mapa.getZoom()
        );

        double x = ponto.getX() - diametro / 2.0;
        double y = ponto.getY() - diametro / 2.0;
        Ellipse2D zona = new Ellipse2D.Double(x, y, diametro, diametro);

        g2.setColor(cor);
        g2.fill(zona);
    }

    private int obterDiametro(NivelAlerta alerta) {
        return switch (alerta) {
            case NORMAL -> 0;
            case ATENCAO -> 50;
            case ALERTA -> 100;
            case EMERGENCIA -> 200;
        };
    }

    private Color obterCor(NivelAlerta alerta) {
        return switch (alerta) {
            case NORMAL -> new Color(0, 0, 0, 0);
            case ATENCAO -> new Color(255, 193, 7, 80);
            case ALERTA -> new Color(245, 124, 0, 90);
            case EMERGENCIA -> new Color(211, 47, 47, 100);
        };
    }
}
