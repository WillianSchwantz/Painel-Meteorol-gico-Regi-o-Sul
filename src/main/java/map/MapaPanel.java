package map;

import model.Estacao;
import model.NivelAlerta;
import model.TendenciaChuva;
import org.jxmapviewer.JXMapViewer;
import org.jxmapviewer.input.CenterMapListener;
import org.jxmapviewer.input.PanMouseInputListener;
import org.jxmapviewer.input.ZoomMouseWheelListenerCursor;
import org.jxmapviewer.painter.Painter;
import org.jxmapviewer.viewer.DefaultTileFactory;
import org.jxmapviewer.viewer.GeoPosition;
import org.jxmapviewer.viewer.TileFactoryInfo;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.event.MouseInputListener;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Painel OpenStreetMap que recebe estações já carregadas pela camada Service.
 */
public class MapaPanel extends JPanel {

    private static final GeoPosition CENTRO_REGIAO_SUL =
            new GeoPosition(-29.3, -52.5);

    /*
     * O JXMapViewer usa zoom invertido. Com max=17, o zoom real do OSM
     * equivale a 17 - zoomDoJXMapViewer.
     */
    private static final int ZOOM_REGIAO_SUL = 13;
    private static final int ZOOM_MAXIMO_OSM = 17;
    private static final String URL_TILES_OSM =
            "https://tile.openstreetmap.org/";

    public interface EstacaoClickListener {
        void onEstacaoClicked(Estacao estacao, Point locationOnScreen);
    }

    private final JXMapViewer mapaViewer;
    private final JLabel labelInformacao;
    private List<EstacaoWaypoint> waypoints;
    private List<Estacao> estacoesIgnoradas;
    private String stationIdSelecionada;
    private List<HeatmapPoint> pontosHeatmap;
    private HeatmapPainter heatmapPainter;
    private IsolinhaPainter isolinhaPainter;
    private ZonaAlagamentoPainter zonaPainter;
    private boolean heatmapAtivo;
    private boolean zonasAtivas;
    private boolean isolinhasAtivas;
    private EstacaoClickListener estacaoClickListener;

    public MapaPanel() {
        mapaViewer = new JXMapViewer();
        labelInformacao = new JLabel();
        waypoints = List.of();
        estacoesIgnoradas = List.of();
        stationIdSelecionada = null;
        pontosHeatmap = List.of();
        heatmapPainter = null;
        isolinhaPainter = null;
        zonaPainter = new ZonaAlagamentoPainter(waypoints);
        heatmapAtivo = false;
        zonasAtivas = false;
        isolinhasAtivas = false;

        configurarMapa();
        configurarLayout();
        atualizarInformacao();
    }

    private void configurarMapa() {
        System.setProperty("http.agent", "PainelMeteorologicoAcademico/1.0");

        TileFactoryInfo tileInfo = new TileFactoryInfo(
                1,
                ZOOM_MAXIMO_OSM - 2,
                ZOOM_MAXIMO_OSM,
                256,
                true,
                true,
                URL_TILES_OSM,
                "z",
                "x",
                "y"
        ) {
            @Override
            public String getTileUrl(int x, int y, int zoom) {
                int zoomOsm = ZOOM_MAXIMO_OSM - zoom;
                return URL_TILES_OSM
                        + zoomOsm
                        + "/"
                        + x
                        + "/"
                        + y
                        + ".png";
            }
        };
        DefaultTileFactory tileFactory = new DefaultTileFactory(tileInfo);
        tileFactory.setThreadPoolSize(4);
        tileFactory.setUserAgent("PainelMeteorologicoAcademico/1.0");

        mapaViewer.setTileFactory(tileFactory);
        mapaViewer.setZoom(ZOOM_REGIAO_SUL);
        mapaViewer.setAddressLocation(CENTRO_REGIAO_SUL);

        MouseInputListener panListener =
                new PanMouseInputListener(mapaViewer);
        mapaViewer.addMouseListener(panListener);
        mapaViewer.addMouseMotionListener(panListener);
        mapaViewer.addMouseListener(new CenterMapListener(mapaViewer));
        mapaViewer.addMouseWheelListener(
                new ZoomMouseWheelListenerCursor(mapaViewer)
        );

        mapaViewer.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON1 && estacaoClickListener != null) {
                    for (EstacaoWaypoint waypoint : waypoints) {
                        Point2D gpPt = mapaViewer.getTileFactory().geoToPixel(waypoint.getPosition(), mapaViewer.getZoom());
                        Rectangle viewportBounds = mapaViewer.getViewportBounds();
                        double x = gpPt.getX() - viewportBounds.getX();
                        double y = gpPt.getY() - viewportBounds.getY();
                        
                        // Check if click is inside the marker (radius ~ 10px)
                        if (e.getPoint().distance(x, y) <= 10.0) {
                            Point screenLocation = e.getLocationOnScreen();
                            estacaoClickListener.onEstacaoClicked(waypoint.getEstacao(), screenLocation);
                            break;
                        }
                    }
                }
            }
        });
    }

    public void setEstacaoClickListener(EstacaoClickListener listener) {
        this.estacaoClickListener = listener;
    }

    private void configurarLayout() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createTitledBorder("Mapa de estações"));
        setPreferredSize(new Dimension(560, 500));

        labelInformacao.setBorder(
                BorderFactory.createEmptyBorder(4, 7, 4, 7)
        );

        JPanel rodape = new JPanel(new BorderLayout());
        rodape.add(labelInformacao, BorderLayout.NORTH);
        rodape.add(criarLegenda(), BorderLayout.SOUTH);

        add(mapaViewer, BorderLayout.CENTER);
        add(rodape, BorderLayout.SOUTH);
    }

    /**
     * Cria os marcadores usando somente coordenadas válidas recebidas.
     */
    public void atualizarEstacoes(List<Estacao> estacoes) {
        Objects.requireNonNull(
                estacoes,
                "A lista de estações não pode ser nula."
        );
        List<EstacaoWaypoint> novosWaypoints = new ArrayList<>();
        List<Estacao> novasIgnoradas = new ArrayList<>();
        Set<GeoPosition> posicoes = new LinkedHashSet<>();

        for (Estacao estacao : estacoes) {
            if (!possuiCoordenadasValidas(estacao)) {
                novasIgnoradas.add(estacao);
                continue;
            }

            GeoPosition posicao = new GeoPosition(
                    estacao.getLatitude(),
                    estacao.getLongitude()
            );
            novosWaypoints.add(new EstacaoWaypoint(estacao, posicao));
            posicoes.add(posicao);
        }

        estacoesIgnoradas = List.copyOf(novasIgnoradas);
        atualizarMarcadores(novosWaypoints);

        if (!posicoes.isEmpty()) {
            SwingUtilities.invokeLater(
                    () -> mapaViewer.zoomToBestFit(posicoes, 0.85)
            );
        }
    }

    /**
     * Substitui os marcadores por associações de estação e nível já prontas.
     */
    public void atualizarMarcadores(
            List<EstacaoWaypoint> novosWaypoints
    ) {
        Objects.requireNonNull(
                novosWaypoints,
                "A lista de marcadores não pode ser nula."
        );
        waypoints = List.copyOf(novosWaypoints);
        zonaPainter = new ZonaAlagamentoPainter(waypoints);
        if (!contemEstacao(stationIdSelecionada)) {
            stationIdSelecionada = null;
        }
        atualizarPainter();
        atualizarInformacao();
        mapaViewer.repaint();
    }

    /**
     * Remove níveis calculados para um filtro anterior.
     */
    public void limparNiveisAlerta() {
        for (EstacaoWaypoint waypoint : waypoints) {
            waypoint.setNivelAlerta(null);
            waypoint.setTendenciaChuva(null);
        }
        mapaViewer.repaint();
    }

    /**
     * Atualiza somente a estação cujo resumo foi calculado.
     */
    public void atualizarNivelAlerta(
            Estacao estacao,
            NivelAlerta nivelAlerta,
            TendenciaChuva tendenciaChuva
    ) {
        if (estacao == null || estacao.getStationId() == null) {
            return;
        }

        for (EstacaoWaypoint waypoint : waypoints) {
            String stationId = waypoint.getEstacao().getStationId();
            if (estacao.getStationId().equals(stationId)) {
                waypoint.setNivelAlerta(nivelAlerta);
                waypoint.setTendenciaChuva(tendenciaChuva);
                break;
            }
        }
        mapaViewer.repaint();
    }

    public void centralizarNaEstacao(Estacao estacao) {
        if (!possuiCoordenadasValidas(estacao)) {
            return;
        }

        mapaViewer.setAddressLocation(
                new GeoPosition(
                        estacao.getLatitude(),
                        estacao.getLongitude()
                )
        );
    }

    /**
     * Seleciona, destaca e centraliza uma estação já presente no mapa.
     */
    public void selecionarEstacao(String stationId) {
        stationIdSelecionada = stationId;

        for (EstacaoWaypoint waypoint : waypoints) {
            String idAtual = waypoint.getEstacao().getStationId();
            if (Objects.equals(stationId, idAtual)) {
                mapaViewer.setAddressLocation(waypoint.getPosition());
                break;
            }
        }

        atualizarPainter();
        mapaViewer.repaint();
    }

    public String getStationIdSelecionada() {
        return stationIdSelecionada;
    }

    public void atualizarHeatmap(List<HeatmapPoint> novosPontos) {
        Objects.requireNonNull(
                novosPontos,
                "A lista de pontos do heatmap não pode ser nula."
        );
        pontosHeatmap = List.copyOf(novosPontos);
        heatmapPainter = pontosHeatmap.size() < 2
                ? null
                : new HeatmapPainter(pontosHeatmap);
        isolinhaPainter = pontosHeatmap.size() < 2
                ? null
                : new IsolinhaPainter(pontosHeatmap);
        atualizarPainter();
        atualizarInformacao();
        mapaViewer.repaint();
    }

    public void atualizarCoresDosMarcadores(List<HeatmapPoint> pontosVariaveis) {
        if (pontosVariaveis == null) return;
        for (EstacaoWaypoint waypoint : waypoints) {
            String stationId = waypoint.getEstacao().getStationId();
            Double valorVariavel = null;
            for (HeatmapPoint p : pontosVariaveis) {
                if (p.getStationId() != null && p.getStationId().equals(stationId)) {
                    valorVariavel = p.getValor();
                    break;
                }
            }
            waypoint.setValorVariavel(valorVariavel);
        }
        // Repass the min/max values to EstacaoPainter or let it calculate them?
        // EstacaoPainter can just calculate max/min from the waypoints.
        mapaViewer.repaint();
    }

    public void setHeatmapAtivo(boolean ativo) {
        heatmapAtivo = ativo;
        atualizarPainter();
        atualizarInformacao();
        mapaViewer.repaint();
    }

    public boolean isHeatmapAtivo() {
        return heatmapAtivo;
    }

    public void setZonasAtivas(boolean ativas) {
        this.zonasAtivas = ativas;
        atualizarPainter();
        atualizarInformacao();
        mapaViewer.repaint();
    }

    public boolean isZonasAtivas() {
        return zonasAtivas;
    }

    public void setIsolinhasAtivas(boolean ativas) {
        this.isolinhasAtivas = ativas;
        atualizarPainter();
        atualizarInformacao();
        mapaViewer.repaint();
    }

    public boolean isIsolinhasAtivas() {
        return isolinhasAtivas;
    }

    public int getQuantidadePontosHeatmap() {
        return pontosHeatmap.size();
    }

    public int getQuantidadeMarcadores() {
        return waypoints.size();
    }

    public List<Estacao> getEstacoesIgnoradasSemCoordenadas() {
        return estacoesIgnoradas;
    }

    private void atualizarPainter() {
        Painter<JXMapViewer> painterEstacoes =
                new EstacaoPainter(waypoints, stationIdSelecionada);
        mapaViewer.setOverlayPainter((graphics, mapa, largura, altura) -> {
            if (heatmapAtivo && heatmapPainter != null) {
                heatmapPainter.paint(graphics, mapa, largura, altura);
            }
            if (zonasAtivas && zonaPainter != null) {
                zonaPainter.paint(graphics, mapa, largura, altura);
            }
            if (isolinhasAtivas && isolinhaPainter != null) {
                isolinhaPainter.paint(graphics, mapa, largura, altura);
            }
            painterEstacoes.paint(graphics, mapa, largura, altura);
        });
    }

    private boolean contemEstacao(String stationId) {
        if (stationId == null) {
            return false;
        }

        for (EstacaoWaypoint waypoint : waypoints) {
            if (stationId.equals(
                    waypoint.getEstacao().getStationId()
            )) {
                return true;
            }
        }
        return false;
    }

    private void atualizarInformacao() {
        labelInformacao.setText(
                "© OpenStreetMap contributors"
                        + "  |  Marcadores: " + waypoints.size()
                        + "  |  Ignoradas: " + estacoesIgnoradas.size()
                        + "  |  Heatmap: " + descricaoHeatmap()
                        + "  |  Zonas: " + (zonasAtivas ? "ligadas" : "desligadas")
                        + "  |  Isolinhas: " + (isolinhasAtivas ? "ligadas" : "desligadas")
        );
    }

    private String descricaoHeatmap() {
        if (!heatmapAtivo) {
            return "desligado";
        }
        if (pontosHeatmap.size() < 2) {
            return "dados insuficientes";
        }
        return pontosHeatmap.size() + " pontos";
    }

    private JPanel criarLegenda() {
        JPanel legenda = new JPanel(new GridLayout(0, 1, 0, 5));
        legenda.setBorder(BorderFactory.createTitledBorder("Legenda"));

        JPanel painelMarcadores = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        painelMarcadores.add(new JLabel("Marcadores (Nível):"));
        painelMarcadores.add(criarItemLegenda("Não calculado", EstacaoPainter.COR_NAO_CALCULADO));
        painelMarcadores.add(criarItemLegenda("Normal", EstacaoPainter.COR_NORMAL));
        painelMarcadores.add(criarItemLegenda("Atenção", EstacaoPainter.COR_ATENCAO));
        painelMarcadores.add(criarItemLegenda("Alerta", EstacaoPainter.COR_ALERTA));
        painelMarcadores.add(criarItemLegenda("Emergência", EstacaoPainter.COR_EMERGENCIA));
        legenda.add(painelMarcadores);

        JPanel painelZonas = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        painelZonas.add(new JLabel("Zonas Alagadiças (Círculo Translúcido):"));
        painelZonas.add(criarItemLegenda("Atenção (Amarelo)", new Color(255, 193, 7, 80)));
        painelZonas.add(criarItemLegenda("Alerta (Laranja)", new Color(245, 124, 0, 90)));
        painelZonas.add(criarItemLegenda("Emergência (Vermelho)", new Color(211, 47, 47, 100)));
        legenda.add(painelZonas);

        JPanel painelOutros = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        painelOutros.add(new JLabel("Tendência de Chuva: ↑ Crescente | ↓ Decrescente | (sem seta: Estável/Insuficiente)"));
        painelOutros.add(new JLabel("  ||  Heatmap: Azul → Amarelo → Vermelho"));
        legenda.add(painelOutros);

        return legenda;
    }

    private JPanel criarItemLegenda(String texto, Color cor) {
        JLabel amostra = new JLabel("   ");
        amostra.setOpaque(true);
        amostra.setBackground(cor);
        amostra.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY));

        JPanel item = new JPanel(new FlowLayout(FlowLayout.LEFT, 3, 0));
        item.add(amostra);
        item.add(new JLabel(texto));
        return item;
    }

    private static boolean possuiCoordenadasValidas(Estacao estacao) {
        if (estacao == null
                || estacao.getLatitude() == null
                || estacao.getLongitude() == null) {
            return false;
        }

        double latitude = estacao.getLatitude();
        double longitude = estacao.getLongitude();
        return Double.isFinite(latitude)
                && Double.isFinite(longitude)
                && latitude >= -90.0
                && latitude <= 90.0
                && longitude >= -180.0
                && longitude <= 180.0;
    }
}
