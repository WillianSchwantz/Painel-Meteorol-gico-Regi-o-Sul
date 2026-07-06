package map;

import model.NivelAlerta;
import model.TendenciaChuva;
import org.jxmapviewer.JXMapViewer;
import org.jxmapviewer.painter.Painter;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Desenha os marcadores com a cor do alerta e destaca a estação selecionada.
 */
public class EstacaoPainter implements Painter<JXMapViewer> {

    public static final Color COR_NORMAL = new Color(46, 160, 67);
    public static final Color COR_ATENCAO = new Color(255, 193, 7);
    public static final Color COR_ALERTA = new Color(245, 124, 0);
    public static final Color COR_EMERGENCIA = new Color(211, 47, 47);
    public static final Color COR_NAO_CALCULADO = new Color(117, 117, 117);

    private static final Color COR_CONTORNO = Color.WHITE;
    private static final int DIAMETRO = 12;
    private static final int DIAMETRO_SELECIONADO = 20;

    private final List<EstacaoWaypoint> waypoints;
    private final String stationIdSelecionada;
    private double minValorVariavel = Double.MAX_VALUE;
    private double maxValorVariavel = -Double.MAX_VALUE;
    private boolean temValorVariavel = false;

    public EstacaoPainter(Collection<EstacaoWaypoint> waypoints) {
        this(waypoints, null);
    }

    public EstacaoPainter(
            Collection<EstacaoWaypoint> waypoints,
            String stationIdSelecionada
    ) {
        this.waypoints = List.copyOf(waypoints);
        this.stationIdSelecionada = stationIdSelecionada;
        for (EstacaoWaypoint wp : this.waypoints) {
            if (wp.getValorVariavel() != null) {
                temValorVariavel = true;
                if (wp.getValorVariavel() < minValorVariavel) minValorVariavel = wp.getValorVariavel();
                if (wp.getValorVariavel() > maxValorVariavel) maxValorVariavel = wp.getValorVariavel();
            }
        }
    }

    @Override
    public void paint(
            Graphics2D graphics,
            JXMapViewer mapa,
            int largura,
            int altura
    ) {
        Graphics2D g2 = (Graphics2D) graphics.create();
        try {
            g2.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON
            );

            Rectangle viewport = mapa.getViewportBounds();
            g2.translate(-viewport.getX(), -viewport.getY());

            EstacaoWaypoint waypointSelecionado = null;
            List<EstacaoWaypoint> waypointsNaoSelecionados = new ArrayList<>();
            for (EstacaoWaypoint waypoint : waypoints) {
                if (isSelecionado(waypoint)) {
                    waypointSelecionado = waypoint;
                } else {
                    waypointsNaoSelecionados.add(waypoint);
                }
            }
            
            // JXMapViewer inverte o zoom. No formato OSM usado no mapa: zoom OSM = 17 - zoom JX.
            boolean clusterAtivo = (17 - mapa.getZoom()) < 8;
            
            if (clusterAtivo) {
                List<List<EstacaoWaypoint>> clusters = new ArrayList<>();
                for (EstacaoWaypoint wp : waypointsNaoSelecionados) {
                    Point2D p1 = mapa.getTileFactory().geoToPixel(wp.getPosition(), mapa.getZoom());
                    boolean adicionado = false;
                    for (List<EstacaoWaypoint> cluster : clusters) {
                        Point2D pCenter = mapa.getTileFactory().geoToPixel(cluster.get(0).getPosition(), mapa.getZoom());
                        if (p1.distance(pCenter) < 30) {
                            cluster.add(wp);
                            adicionado = true;
                            break;
                        }
                    }
                    if (!adicionado) {
                        List<EstacaoWaypoint> novo = new ArrayList<>();
                        novo.add(wp);
                        clusters.add(novo);
                    }
                }
                for (List<EstacaoWaypoint> cluster : clusters) {
                    if (cluster.size() == 1) {
                        desenharMarcador(g2, mapa, cluster.get(0), false);
                    } else {
                        desenharCluster(g2, mapa, cluster);
                    }
                }
            } else {
                for (EstacaoWaypoint wp : waypointsNaoSelecionados) {
                    desenharMarcador(g2, mapa, wp, false);
                }
            }

            // O selecionado é desenhado por último para não ficar encoberto.
            if (waypointSelecionado != null) {
                desenharMarcador(
                        g2,
                        mapa,
                        waypointSelecionado,
                        true
                );
            }
        } finally {
            g2.dispose();
        }
    }

    private void desenharMarcador(
            Graphics2D g2,
            JXMapViewer mapa,
            EstacaoWaypoint waypoint,
            boolean selecionado
    ) {
        int diametro = selecionado ? DIAMETRO_SELECIONADO : DIAMETRO;
        Point2D ponto = mapa.getTileFactory().geoToPixel(
                waypoint.getPosition(),
                mapa.getZoom()
        );

        double x = ponto.getX() - diametro / 2.0;
        double y = ponto.getY() - diametro / 2.0;
        Ellipse2D marcador = new Ellipse2D.Double(
                x,
                y,
                diametro,
                diametro
        );

        Color corFundo;
        if (temValorVariavel && waypoint.getValorVariavel() != null) {
            corFundo = HeatmapPainter.corDoValor(waypoint.getValorVariavel(), minValorVariavel, maxValorVariavel, 255);
        } else {
            corFundo = corPara(waypoint.getNivelAlerta());
        }

        g2.setColor(corFundo);
        g2.fill(marcador);
        g2.setColor(selecionado ? Color.BLACK : COR_CONTORNO);
        g2.setStroke(new BasicStroke(selecionado ? 3.5f : 2.0f));
        g2.draw(marcador);

        // Desenhar Seta de Tendência de Chuva
        TendenciaChuva tendencia = waypoint.getTendenciaChuva();
        if (tendencia != null && (tendencia == TendenciaChuva.CRESCENTE || tendencia == TendenciaChuva.DECRESCENTE)) {
            g2.setColor(Color.WHITE);
            g2.setFont(new Font("SansSerif", Font.BOLD, selecionado ? 14 : 10));
            String seta = tendencia == TendenciaChuva.CRESCENTE ? "↑" : "↓";
            FontMetrics metrics = g2.getFontMetrics();
            int larguraTexto = metrics.stringWidth(seta);
            int alturaTexto = metrics.getAscent();
            g2.drawString(seta, (float) (ponto.getX() - larguraTexto / 2.0), (float) (ponto.getY() + alturaTexto / 2.0 - 2));
        }
    }

    private void desenharCluster(
            Graphics2D g2,
            JXMapViewer mapa,
            List<EstacaoWaypoint> cluster
    ) {
        EstacaoWaypoint centro = cluster.get(0);
        Point2D ponto = mapa.getTileFactory().geoToPixel(centro.getPosition(), mapa.getZoom());
        
        int diametro = 24;
        double x = ponto.getX() - diametro / 2.0;
        double y = ponto.getY() - diametro / 2.0;
        Ellipse2D marcador = new Ellipse2D.Double(x, y, diametro, diametro);
        
        g2.setColor(new Color(100, 150, 255, 200));
        g2.fill(marcador);
        g2.setColor(Color.WHITE);
        g2.setStroke(new BasicStroke(2.0f));
        g2.draw(marcador);
        
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("SansSerif", Font.BOLD, 12));
        String texto = String.valueOf(cluster.size());
        FontMetrics metrics = g2.getFontMetrics();
        int w = metrics.stringWidth(texto);
        int h = metrics.getAscent();
        g2.drawString(texto, (float) (ponto.getX() - w / 2.0), (float) (ponto.getY() + h / 2.0 - 2));
    }

    private boolean isSelecionado(EstacaoWaypoint waypoint) {
        return stationIdSelecionada != null
                && waypoint.getEstacao() != null
                && stationIdSelecionada.equals(
                        waypoint.getEstacao().getStationId()
                );
    }

    /**
     * Converte o nível já calculado pela Service em uma cor de apresentação.
     */
    public static Color corPara(NivelAlerta nivelAlerta) {
        if (nivelAlerta == null) {
            return COR_NAO_CALCULADO;
        }

        return switch (nivelAlerta) {
            case NORMAL -> COR_NORMAL;
            case ATENCAO -> COR_ATENCAO;
            case ALERTA -> COR_ALERTA;
            case EMERGENCIA -> COR_EMERGENCIA;
        };
    }
}
