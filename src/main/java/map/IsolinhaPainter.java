package map;

import org.jxmapviewer.JXMapViewer;
import org.jxmapviewer.painter.Painter;
import org.jxmapviewer.viewer.GeoPosition;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class IsolinhaPainter implements Painter<JXMapViewer> {

    private static final int TAMANHO_CELULA = 30;
    private final List<HeatmapPoint> pontos;
    private final double minValor;
    private final double maxValor;
    private final List<Double> isolinhas;

    public IsolinhaPainter(Collection<HeatmapPoint> pontos) {
        this.pontos = pontos.stream()
                .filter(ponto -> ponto != null
                        && ponto.getValor() != null
                        && Double.isFinite(ponto.getValor()))
                .toList();

        double min = Double.MAX_VALUE;
        double max = -Double.MAX_VALUE;
        for (HeatmapPoint pt : this.pontos) {
            if (pt.getValor() < min) min = pt.getValor();
            if (pt.getValor() > max) max = pt.getValor();
        }
        this.minValor = min == Double.MAX_VALUE ? 0 : min;
        this.maxValor = max == -Double.MAX_VALUE ? 0 : max;

        this.isolinhas = new ArrayList<>();
        if (maxValor > minValor) {
            int numLinhas = 5;
            double step = (maxValor - minValor) / (numLinhas + 1);
            for (int i = 1; i <= numLinhas; i++) {
                isolinhas.add(minValor + i * step);
            }
        }
    }

    @Override
    public void paint(Graphics2D graphics, JXMapViewer mapa, int largura, int altura) {
        if (pontos.size() < 2 || largura <= 0 || altura <= 0 || isolinhas.isEmpty()) {
            return;
        }

        Graphics2D g2 = (Graphics2D) graphics.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Rectangle viewport = mapa.getViewportBounds();

            int colunas = (largura / TAMANHO_CELULA) + 2;
            int linhas = (altura / TAMANHO_CELULA) + 2;

            Double[][] grid = new Double[linhas][colunas];
            Point2D[][] pontosTela = new Point2D[linhas][colunas];

            for (int r = 0; r < linhas; r++) {
                for (int c = 0; c < colunas; c++) {
                    int x = c * TAMANHO_CELULA;
                    int y = r * TAMANHO_CELULA;
                    
                    Point2D centroGlobal = new Point2D.Double(
                            viewport.getX() + x,
                            viewport.getY() + y
                    );
                    GeoPosition posicao = mapa.getTileFactory().pixelToGeo(
                            centroGlobal,
                            mapa.getZoom()
                    );
                    Double valor = HeatmapPainter.interpolarIDW(
                            posicao.getLatitude(),
                            posicao.getLongitude(),
                            pontos
                    );
                    grid[r][c] = valor;
                    pontosTela[r][c] = new Point2D.Double(x, y);
                }
            }

            g2.setStroke(new BasicStroke(1.2f));
            g2.setFont(new Font("SansSerif", Font.PLAIN, 10));

            for (Double isoValor : isolinhas) {
                Color cor = HeatmapPainter.corDoValor(isoValor, minValor, maxValor, 255);
                g2.setColor(cor.darker());
                Path2D path = new Path2D.Double();

                for (int r = 0; r < linhas - 1; r++) {
                    for (int c = 0; c < colunas - 1; c++) {
                        Double vTL = grid[r][c];
                        Double vTR = grid[r][c + 1];
                        Double vBR = grid[r + 1][c + 1];
                        Double vBL = grid[r + 1][c];

                        if (vTL == null || vTR == null || vBR == null || vBL == null) continue;

                        int state = 0;
                        if (vTL >= isoValor) state |= 8;
                        if (vTR >= isoValor) state |= 4;
                        if (vBR >= isoValor) state |= 2;
                        if (vBL >= isoValor) state |= 1;

                        if (state == 0 || state == 15) continue;

                        Point2D pTL = pontosTela[r][c];
                        Point2D pTR = pontosTela[r][c + 1];
                        Point2D pBR = pontosTela[r + 1][c + 1];
                        Point2D pBL = pontosTela[r + 1][c];

                        Point2D a = interpolate(pTL, pTR, vTL, vTR, isoValor);
                        Point2D b = interpolate(pTR, pBR, vTR, vBR, isoValor);
                        Point2D cPt = interpolate(pBR, pBL, vBR, vBL, isoValor);
                        Point2D d = interpolate(pBL, pTL, vBL, vTL, isoValor);

                        // Lines for marching squares
                        switch (state) {
                            case 1:  case 14: drawSegment(path, cPt, d); break;
                            case 2:  case 13: drawSegment(path, b, cPt); break;
                            case 3:  case 12: drawSegment(path, b, d); break;
                            case 4:  case 11: drawSegment(path, a, b); break;
                            case 5: drawSegment(path, a, d); drawSegment(path, b, cPt); break; // Saddle
                            case 6:  case 9: drawSegment(path, cPt, a); break;
                            case 7:  case 8: drawSegment(path, a, d); break;
                            case 10: drawSegment(path, a, b); drawSegment(path, cPt, d); break; // Saddle
                        }
                        
                        // Draw label occasionally
                        if (c % 10 == 0 && r % 10 == 0 && (state == 6 || state == 9)) {
                            g2.drawString(String.format("%.1f", isoValor), (float)a.getX(), (float)a.getY());
                        }
                    }
                }
                g2.draw(path);
            }

        } finally {
            g2.dispose();
        }
    }

    private void drawSegment(Path2D path, Point2D p1, Point2D p2) {
        if (p1 == null || p2 == null) return;
        path.moveTo(p1.getX(), p1.getY());
        path.lineTo(p2.getX(), p2.getY());
    }

    private Point2D interpolate(Point2D p1, Point2D p2, double v1, double v2, double iso) {
        if (Math.abs(v1 - v2) < 1e-6) return new Point2D.Double(p1.getX(), p1.getY());
        double t = (iso - v1) / (v2 - v1);
        return new Point2D.Double(
                p1.getX() + t * (p2.getX() - p1.getX()),
                p1.getY() + t * (p2.getY() - p1.getY())
        );
    }
}
