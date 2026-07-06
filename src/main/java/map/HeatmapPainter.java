package map;

import org.jxmapviewer.JXMapViewer;
import org.jxmapviewer.painter.Painter;
import org.jxmapviewer.viewer.GeoPosition;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.Collection;
import java.util.List;

/**
 * Interpola os pontos meteorológicos com IDW e pinta uma grade translúcida.
 */
public class HeatmapPainter implements Painter<JXMapViewer> {

    private static final int TAMANHO_CELULA = 30;
    private static final int ALPHA = 85;
    private static final double POTENCIA_IDW = 2.0;
    private static final double DISTANCIA_MINIMA_QUADRADA = 1.0e-12;

    private static final Color COR_BAIXA = new Color(30, 110, 220);
    private static final Color COR_MEDIA = new Color(255, 215, 0);
    private static final Color COR_ALTA = new Color(220, 45, 35);

    private final List<HeatmapPoint> pontos;
    private final double valorMinimo;
    private final double valorMaximo;

    private BufferedImage imagemCache;
    private ChaveCache chaveCache;

    public HeatmapPainter(Collection<HeatmapPoint> pontos) {
        this.pontos = pontos.stream()
                .filter(ponto -> ponto != null
                        && ponto.getValor() != null
                        && Double.isFinite(ponto.getValor()))
                .toList();
        this.valorMinimo = this.pontos.stream()
                .mapToDouble(HeatmapPoint::getValor)
                .min()
                .orElse(0.0);
        this.valorMaximo = this.pontos.stream()
                .mapToDouble(HeatmapPoint::getValor)
                .max()
                .orElse(0.0);
    }

    @Override
    public void paint(
            Graphics2D graphics,
            JXMapViewer mapa,
            int largura,
            int altura
    ) {
        if (pontos.size() < 2 || largura <= 0 || altura <= 0) {
            return;
        }

        Rectangle viewport = mapa.getViewportBounds();
        ChaveCache chaveAtual = new ChaveCache(
                viewport.x,
                viewport.y,
                mapa.getZoom(),
                largura,
                altura
        );

        if (!chaveAtual.equals(chaveCache) || imagemCache == null) {
            imagemCache = renderizarGrade(
                    mapa,
                    viewport,
                    largura,
                    altura
            );
            chaveCache = chaveAtual;
        }

        graphics.drawImage(imagemCache, 0, 0, null);
    }

    private BufferedImage renderizarGrade(
            JXMapViewer mapa,
            Rectangle viewport,
            int largura,
            int altura
    ) {
        BufferedImage imagem = new BufferedImage(
                largura,
                altura,
                BufferedImage.TYPE_INT_ARGB
        );
        Graphics2D g2 = imagem.createGraphics();
        try {
            for (int y = 0; y < altura; y += TAMANHO_CELULA) {
                for (int x = 0; x < largura; x += TAMANHO_CELULA) {
                    Point2D centroGlobal = new Point2D.Double(
                            viewport.getX() + x + TAMANHO_CELULA / 2.0,
                            viewport.getY() + y + TAMANHO_CELULA / 2.0
                    );
                    GeoPosition posicao = mapa.getTileFactory().pixelToGeo(
                            centroGlobal,
                            mapa.getZoom()
                    );
                    Double valor = interpolarIDW(
                            posicao.getLatitude(),
                            posicao.getLongitude(),
                            pontos
                    );
                    if (valor == null) {
                        continue;
                    }

                    g2.setColor(corDoValor(valor));
                    g2.fillRect(
                            x,
                            y,
                            TAMANHO_CELULA + 1,
                            TAMANHO_CELULA + 1
                    );
                }
            }
        } finally {
            g2.dispose();
        }
        return imagem;
    }

    /**
     * IDW com p=2. A longitude é ajustada pela latitude para reduzir a
     * distorção da distância geográfica em uma grade simples.
     */
    public static Double interpolarIDW(
            double latitude,
            double longitude,
            Collection<HeatmapPoint> pontos
    ) {
        double somaPonderada = 0.0;
        double somaPesos = 0.0;

        for (HeatmapPoint ponto : pontos) {
            if (ponto == null
                    || ponto.getValor() == null
                    || !Double.isFinite(ponto.getValor())) {
                continue;
            }

            double latitudeMedia = Math.toRadians(
                    (latitude + ponto.getLatitude()) / 2.0
            );
            double deltaLatitude = latitude - ponto.getLatitude();
            double deltaLongitude = (longitude - ponto.getLongitude())
                    * Math.cos(latitudeMedia);
            double distanciaQuadrada = deltaLatitude * deltaLatitude
                    + deltaLongitude * deltaLongitude;

            if (distanciaQuadrada <= DISTANCIA_MINIMA_QUADRADA) {
                return ponto.getValor();
            }

            double peso = 1.0 / Math.pow(
                    Math.sqrt(distanciaQuadrada),
                    POTENCIA_IDW
            );
            somaPonderada += ponto.getValor() * peso;
            somaPesos += peso;
        }

        return somaPesos == 0.0 ? null : somaPonderada / somaPesos;
    }

    public static Color corDoValor(double valor, double valorMinimo, double valorMaximo, int alpha) {
        double amplitude = valorMaximo - valorMinimo;
        double proporcao = amplitude == 0.0
                ? 0.5
                : (valor - valorMinimo) / amplitude;
        proporcao = Math.max(0.0, Math.min(1.0, proporcao));

        Color cor;
        if (proporcao <= 0.5) {
            cor = interpolarCor(
                    COR_BAIXA,
                    COR_MEDIA,
                    proporcao * 2.0
            );
        } else {
            cor = interpolarCor(
                    COR_MEDIA,
                    COR_ALTA,
                    (proporcao - 0.5) * 2.0
            );
        }
        return new Color(cor.getRed(), cor.getGreen(), cor.getBlue(), alpha);
    }

    private Color corDoValor(double valor) {
        return corDoValor(valor, valorMinimo, valorMaximo, ALPHA);
    }

    private static Color interpolarCor(
            Color inicio,
            Color fim,
            double proporcao
    ) {
        int vermelho = (int) Math.round(
                inicio.getRed()
                        + (fim.getRed() - inicio.getRed()) * proporcao
        );
        int verde = (int) Math.round(
                inicio.getGreen()
                        + (fim.getGreen() - inicio.getGreen()) * proporcao
        );
        int azul = (int) Math.round(
                inicio.getBlue()
                        + (fim.getBlue() - inicio.getBlue()) * proporcao
        );
        return new Color(vermelho, verde, azul);
    }

    private record ChaveCache(
            int viewportX,
            int viewportY,
            int zoom,
            int largura,
            int altura
    ) {
    }
}
