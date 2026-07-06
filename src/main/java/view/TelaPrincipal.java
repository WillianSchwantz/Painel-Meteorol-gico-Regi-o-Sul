package view;

import map.MapaPanel;
import map.HeatmapPoint;
import model.Estacao;
import model.MedicaoDiaria;
import model.ResumoMeteorologico;
import service.EstacaoService;
import service.HeatmapService;
import service.ChuvaTendenciaService;
import model.TendenciaChuva;

import javax.swing.JPopupMenu;
import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import javax.swing.JTextArea;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.TreeSet;
import java.util.concurrent.ExecutionException;

/**
 * Janela principal que coordena filtros, tabela, resumo e mapa.
 */
public class TelaPrincipal extends JFrame {

    private static final String TITULO =
            "Painel Meteorológico — Região Sul";

    private final EstacaoService estacaoService;
    private final HeatmapService heatmapService;
    private final ChuvaTendenciaService tendenciaService;
    private final PainelFiltros painelFiltros;
    private final PainelTabela painelTabela;
    private final MapaPanel mapaPanel;
    private final JTextArea areaResumo;
    private final JLabel labelStatus;
    private ResumoMeteorologico ultimoResumo;
    private LocalDate ultimaDataInicial;
    private LocalDate ultimaDataFinal;
    private LocalDate dataAtualTimeline;

    private final JSlider sliderTimeline;
    private final JLabel labelTimeline;
    private boolean timelineAtiva = false;

    public TelaPrincipal() {
        this(new EstacaoService(), new HeatmapService(), new ChuvaTendenciaService());
    }

    public TelaPrincipal(EstacaoService estacaoService) {
        this(estacaoService, new HeatmapService(), new ChuvaTendenciaService());
    }

    public TelaPrincipal(
            EstacaoService estacaoService,
            HeatmapService heatmapService,
            ChuvaTendenciaService tendenciaService
    ) {
        super(TITULO);
        this.estacaoService = Objects.requireNonNull(estacaoService);
        this.heatmapService = Objects.requireNonNull(heatmapService);
        this.tendenciaService = Objects.requireNonNull(tendenciaService);
        this.painelFiltros = new PainelFiltros();
        this.painelTabela = new PainelTabela();
        this.mapaPanel = new MapaPanel();
        this.areaResumo = new JTextArea();
        this.labelStatus = new JLabel(
                "Carregando estações...",
                SwingConstants.LEFT
        );
        this.sliderTimeline = new JSlider(0, 0, 0);
        this.sliderTimeline.setEnabled(false);
        this.labelTimeline = new JLabel("Timeline (Data)", SwingConstants.CENTER);

        configurarJanela();
        painelFiltros.adicionarAcaoBuscar(evento -> buscarMedicoes());
        painelFiltros.adicionarAcaoFiltrarCidade(
                evento -> filtrarEstacoesPorCidade()
        );
        painelFiltros.adicionarAcaoSelecaoEstacao(
                evento -> selecionarEstacaoDoFiltro()
        );
        painelFiltros.adicionarAcaoHeatmap(
                evento -> alternarHeatmap()
        );
        painelFiltros.adicionarAcaoZonas(
                evento -> alternarZonas()
        );
        painelFiltros.adicionarAcaoIsolinhas(
                evento -> alternarIsolinhas()
        );
        painelTabela.adicionarListenerSelecao(evento -> {
            if (!evento.getValueIsAdjusting()) {
                tratarSelecaoTabela();
            }
        });
        sliderTimeline.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                if (ultimaDataInicial != null && timelineAtiva) {
                    dataAtualTimeline = ultimaDataInicial.plusDays(sliderTimeline.getValue());
                    labelTimeline.setText("Timeline: " + dataAtualTimeline.toString());
                    if (!sliderTimeline.getValueIsAdjusting()) {
                        atualizarMapaPorData(dataAtualTimeline);
                    }
                }
            }
        });
        mapaPanel.setEstacaoClickListener((estacao, pt) -> {
            mostrarPopupSparkline(estacao, pt);
        });
        carregarEstacoes();
    }

    private void configurarJanela() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(8, 8));

        JPanel conteudo = new JPanel(new BorderLayout(8, 8));
        conteudo.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        conteudo.add(painelFiltros, BorderLayout.NORTH);

        JPanel painelDados = new JPanel(new BorderLayout(8, 8));
        painelDados.add(painelTabela, BorderLayout.CENTER);
        painelDados.add(criarAreaResumo(), BorderLayout.SOUTH);
        painelDados.setMinimumSize(new Dimension(560, 420));
        mapaPanel.setMinimumSize(new Dimension(360, 420));

        JSplitPane divisor = new JSplitPane(
                JSplitPane.HORIZONTAL_SPLIT,
                painelDados,
                mapaPanel
        );
        divisor.setContinuousLayout(true);
        divisor.setOneTouchExpandable(true);
        divisor.setResizeWeight(0.60);
        divisor.setDividerLocation(0.62);
        conteudo.add(divisor, BorderLayout.CENTER);

        labelStatus.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEtchedBorder(),
                BorderFactory.createEmptyBorder(5, 8, 5, 8)
        ));

        JPanel painelSul = new JPanel(new BorderLayout());
        JPanel painelSlider = new JPanel(new BorderLayout());
        painelSlider.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        painelSlider.add(labelTimeline, BorderLayout.NORTH);
        painelSlider.add(sliderTimeline, BorderLayout.CENTER);
        
        painelSul.add(painelSlider, BorderLayout.CENTER);
        painelSul.add(labelStatus, BorderLayout.SOUTH);

        conteudo.add(painelSul, BorderLayout.SOUTH);

        setContentPane(conteudo);

        Dimension tela = Toolkit.getDefaultToolkit().getScreenSize();
        int largura = Math.min(1400, Math.max(1100, tela.width - 80));
        int altura = Math.min(820, Math.max(640, tela.height - 90));
        setMinimumSize(new Dimension(1050, 600));
        setSize(largura, altura);
        setLocationRelativeTo(null);
    }

    private JScrollPane criarAreaResumo() {
        areaResumo.setEditable(false);
        areaResumo.setFocusable(false);
        areaResumo.setLineWrap(true);
        areaResumo.setWrapStyleWord(true);
        areaResumo.setText("Faça uma busca para visualizar o resumo.");
        areaResumo.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JScrollPane rolagem = new JScrollPane(areaResumo);
        rolagem.setBorder(
                BorderFactory.createTitledBorder("Resumo do período")
        );
        rolagem.setPreferredSize(new Dimension(0, 285));
        return rolagem;
    }

    /**
     * Carrega as estações fora da Event Dispatch Thread para não congelar a
     * janela durante o acesso ao banco.
     */
    private void carregarEstacoes() {
        painelFiltros.setControlesHabilitados(false);
        atualizarStatus("Carregando estações do banco...");

        SwingWorker<ResultadoCargaInicial, Void> worker =
                new SwingWorker<>() {
                    @Override
                    protected ResultadoCargaInicial doInBackground()
                            throws Exception {
                        List<Estacao> estacoes =
                                estacaoService.listarEstacoes();
                        List<String> localidades =
                                estacaoService.listarLocalidades();
                        if (localidades.isEmpty()) {
                            localidades = extrairLocalidadesDasEstacoes(
                                    estacoes
                            );
                        }
                        return new ResultadoCargaInicial(estacoes, localidades);
                    }

                    @Override
                    protected void done() {
                        try {
                            ResultadoCargaInicial resultado = get();
                            List<Estacao> estacoes = resultado.estacoes();
                            painelFiltros.setEstacoes(estacoes);
                            painelFiltros.setLocalidades(
                                    resultado.localidades()
                            );
                            mapaPanel.atualizarEstacoes(estacoes);
                            painelFiltros.setControlesHabilitados(true);

                            if (estacoes.isEmpty()) {
                                atualizarStatus(
                                        "Nenhuma estação cadastrada no banco."
                                );
                            } else {
                                atualizarStatus(
                                        estacoes.size()
                                                + " estações carregadas."
                                );
                            }
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            tratarErroCarregamento(
                                    "O carregamento das estações foi interrompido.",
                                    e
                            );
                        } catch (ExecutionException e) {
                            tratarErroCarregamento(
                                    "Não foi possível carregar as estações.",
                                    e.getCause()
                            );
                        }
                    }
                };

        worker.execute();
    }

    private void filtrarEstacoesPorCidade() {
        String filtro = painelFiltros.getFiltroCidade();
        painelFiltros.setControlesHabilitados(false);
        atualizarStatus(
                filtro.isBlank()
                        ? "Carregando todas as estações..."
                        : "Filtrando estações por cidade/UF: " + filtro + "..."
        );

        SwingWorker<List<Estacao>, Void> worker =
                new SwingWorker<>() {
                    @Override
                    protected List<Estacao> doInBackground()
                            throws Exception {
                        return estacaoService.listarEstacoesPorCidadeOuTexto(
                                filtro
                        );
                    }

                    @Override
                    protected void done() {
                        try {
                            List<Estacao> estacoes = get();
                            painelFiltros.setEstacoes(estacoes);
                            mapaPanel.atualizarEstacoes(estacoes);
                            mapaPanel.limparNiveisAlerta();
                            mapaPanel.atualizarHeatmap(List.of());

                            if (estacoes.isEmpty()) {
                                atualizarStatus(
                                        "Nenhuma estação encontrada para o filtro informado."
                                );
                            } else if (filtro.isBlank()) {
                                atualizarStatus(
                                        estacoes.size()
                                                + " estações carregadas."
                                );
                            } else {
                                atualizarStatus(
                                        estacoes.size()
                                                + " estações encontradas para cidade/UF: "
                                                + filtro
                                                + "."
                                );
                            }
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            mostrarErro(
                                    "O filtro de cidade foi interrompido.",
                                    e
                            );
                        } catch (ExecutionException e) {
                            mostrarErro(
                                    "Não foi possível filtrar as estações.",
                                    e.getCause()
                            );
                        } finally {
                            painelFiltros.setControlesHabilitados(true);
                        }
                    }
                };

        worker.execute();
    }

    private static List<String> extrairLocalidadesDasEstacoes(
            List<Estacao> estacoes
    ) {
        TreeSet<String> nomes = new TreeSet<>(
                String.CASE_INSENSITIVE_ORDER
        );
        for (Estacao estacao : estacoes) {
            String nome = estacao.getStationName();
            if (nome != null && !nome.isBlank()) {
                nomes.add(nome.trim());
            }
        }
        return new ArrayList<>(nomes);
    }

    private void buscarMedicoes() {
        Estacao estacao = painelFiltros.getEstacaoSelecionada();
        if (estacao == null) {
            mostrarAviso("Selecione uma estação antes de buscar.");
            return;
        }

        final LocalDate dataInicial;
        final LocalDate dataFinal;

        try {
            dataInicial = converterData(
                    painelFiltros.getDataInicial(),
                    "data inicial"
            );
            dataFinal = converterData(
                    painelFiltros.getDataFinal(),
                    "data final"
            );

            if (dataInicial.isAfter(dataFinal)) {
                throw new IllegalArgumentException(
                        "A data inicial não pode ser posterior à data final."
                );
            }
        } catch (IllegalArgumentException e) {
            mostrarAviso(e.getMessage());
            return;
        }

        String variavel = painelFiltros.getVariavelSelecionada();
        boolean heatmapAtivo = painelFiltros.isHeatmapAtivo();
        boolean zonasAtivas = painelFiltros.isZonasAtivas();
        boolean isolinhasAtivas = painelFiltros.isIsolinhasAtivas();
        int diasTendencia = painelFiltros.getDiasTendencia();
        double limiar24 = painelFiltros.getLimiar24h();
        double limiar48 = painelFiltros.getLimiar48h();
        double limiar72 = painelFiltros.getLimiar72h();
        mapaPanel.limparNiveisAlerta();
        mapaPanel.selecionarEstacao(estacao.getStationId());
        mapaPanel.atualizarHeatmap(List.of());
        mapaPanel.setHeatmapAtivo(heatmapAtivo);
        mapaPanel.setZonasAtivas(zonasAtivas);
        mapaPanel.setIsolinhasAtivas(isolinhasAtivas);
        painelFiltros.setControlesHabilitados(false);
        areaResumo.setText("Calculando resumo...");
        atualizarStatus(
                "Buscando medições de " + estacao + "..."
        );

        SwingWorker<ResultadoBusca, Void> worker =
                new SwingWorker<>() {
                    @Override
                    protected ResultadoBusca doInBackground()
                            throws Exception {
                        List<MedicaoDiaria> medicoes =
                                estacaoService.buscarMedicoesDiarias(
                                estacao,
                                dataInicial,
                                dataFinal
                        );
                        ResumoMeteorologico resumo =
                                estacaoService.calcularResumo(
                                        estacao,
                                        medicoes,
                                        dataFinal,
                                        limiar24,
                                        limiar48,
                                        limiar72
                                );
                        List<HeatmapPoint> pontosVariaveis = heatmapService.criarPontos(
                                dataInicial,
                                dataFinal,
                                variavel
                        );
                        List<HeatmapPoint> pontosHeatmap = heatmapAtivo
                                ? pontosVariaveis
                                : List.of();
                        TendenciaChuva tendencia = tendenciaService.calcularTendencia(
                                estacao.getStationId(),
                                dataFinal,
                                diasTendencia
                        );
                        return new ResultadoBusca(
                                medicoes,
                                resumo,
                                pontosHeatmap,
                                pontosVariaveis,
                                tendencia
                        );
                    }

                    @Override
                    protected void done() {
                        try {
                            ResultadoBusca resultado = get();
                            List<MedicaoDiaria> medicoes =
                                    resultado.medicoes();
                            painelTabela.setVariavelSelecionada(variavel);
                            painelTabela.atualizarTabela(medicoes);
                            exibirResumo(
                                    resultado.resumo(),
                                    dataInicial,
                                    dataFinal
                            );
                            ultimoResumo = resultado.resumo();
                            ultimaDataInicial = dataInicial;
                            ultimaDataFinal = dataFinal;
                            dataAtualTimeline = dataFinal;

                            timelineAtiva = false;
                            long diasTotais = java.time.temporal.ChronoUnit.DAYS.between(dataInicial, dataFinal);
                            sliderTimeline.setMaximum((int) diasTotais);
                            sliderTimeline.setValue((int) diasTotais);
                            sliderTimeline.setEnabled(true);
                            labelTimeline.setText("Timeline: " + dataFinal.toString());
                            timelineAtiva = true;

                            mapaPanel.atualizarNivelAlerta(
                                    estacao,
                                    resultado.resumo().getNivelAlerta(),
                                    resultado.tendencia()
                            );
                            mapaPanel.atualizarHeatmap(
                                    resultado.pontosHeatmap()
                            );
                            mapaPanel.atualizarCoresDosMarcadores(resultado.pontosVariaveis());

                            if (medicoes.isEmpty()) {
                                atualizarStatus(
                                        "Nenhuma medição encontrada para "
                                                + "a estação e o período."
                                                + descricaoResultadoHeatmap(
                                                        heatmapAtivo,
                                                        resultado
                                                                .pontosHeatmap()
                                                                .size()
                                                )
                                );
                                JOptionPane.showMessageDialog(
                                        TelaPrincipal.this,
                                        "Nenhuma medição foi encontrada para "
                                                + "a estação e o período informados.",
                                        "Busca sem resultados",
                                        JOptionPane.INFORMATION_MESSAGE
                                );
                            } else {
                                atualizarStatus(
                                        medicoes.size()
                                                + " medições encontradas"
                                                + " — variável selecionada: "
                                                + variavel
                                                + "."
                                                + descricaoResultadoHeatmap(
                                                        heatmapAtivo,
                                                        resultado
                                                                .pontosHeatmap()
                                                                .size()
                                                )
                                );
                            }
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            mostrarErro(
                                    "A busca foi interrompida.",
                                    e
                            );
                        } catch (ExecutionException e) {
                            painelTabela.limparTabela();
                            areaResumo.setText("Resumo indisponível.");
                            ultimoResumo = null;
                            mostrarErro(
                                    "Não foi possível buscar as medições.",
                                    e.getCause()
                            );
                        } finally {
                            painelFiltros.setControlesHabilitados(true);
                        }
                    }
                };

        worker.execute();
    }

    private void atualizarMapaPorData(LocalDate data) {
        if (ultimaDataInicial == null || ultimaDataFinal == null) return;
        
        String variavel = painelFiltros.getVariavelSelecionada();
        boolean heatmapAtivo = painelFiltros.isHeatmapAtivo();
        boolean isolinhasAtivas = painelFiltros.isIsolinhasAtivas();
        int diasTendencia = painelFiltros.getDiasTendencia();
        double limiar24 = painelFiltros.getLimiar24h();
        double limiar48 = painelFiltros.getLimiar48h();
        double limiar72 = painelFiltros.getLimiar72h();
        Estacao estacao = painelFiltros.getEstacaoSelecionada();
        
        atualizarStatus("Atualizando mapa para " + data + "...");
        
        SwingWorker<ResultadoBuscaRapida, Void> worker = new SwingWorker<>() {
            @Override
            protected ResultadoBuscaRapida doInBackground() throws Exception {
                List<HeatmapPoint> pontosVariaveis = heatmapService.criarPontos(
                        data,
                        data,
                        variavel
                );
                List<HeatmapPoint> pontosHeatmap = heatmapAtivo ? pontosVariaveis : List.of();
                
                List<MedicaoDiaria> medicoes = estacaoService.buscarMedicoesDiarias(estacao, ultimaDataInicial, ultimaDataFinal);
                ResumoMeteorologico resumo = estacaoService.calcularResumo(estacao, medicoes, data, limiar24, limiar48, limiar72);
                TendenciaChuva tendencia = tendenciaService.calcularTendencia(estacao.getStationId(), data, diasTendencia);
                
                return new ResultadoBuscaRapida(pontosHeatmap, pontosVariaveis, resumo, tendencia);
            }
            
            @Override
            protected void done() {
                try {
                    ResultadoBuscaRapida res = get();
                    mapaPanel.atualizarHeatmap(res.pontosHeatmap());
                    mapaPanel.atualizarCoresDosMarcadores(res.pontosVariaveis());
                    mapaPanel.atualizarNivelAlerta(estacao, res.resumo().getNivelAlerta(), res.tendencia());
                    atualizarStatus("Mapa atualizado para " + data + ".");
                } catch (Exception e) {
                    atualizarStatus("Erro ao atualizar mapa na timeline.");
                }
            }
        };
        worker.execute();
    }
    
    private void mostrarPopupSparkline(Estacao estacao, java.awt.Point telaPt) {
        if (dataAtualTimeline == null) return;
        
        JPopupMenu popup = new JPopupMenu();
        popup.add(new JLabel(" Carregando... "));
        popup.show(this, telaPt.x - this.getLocationOnScreen().x, telaPt.y - this.getLocationOnScreen().y);
        
        String variavel = painelFiltros.getVariavelSelecionada();
        LocalDate fim = dataAtualTimeline;
        LocalDate inicio = fim.minusDays(6);
        
        SwingWorker<List<Double>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<Double> doInBackground() throws Exception {
                List<MedicaoDiaria> medicoes = estacaoService.buscarMedicoesDiarias(estacao, inicio, fim);
                List<Double> valores = new java.util.ArrayList<>();
                
                // Map the dates
                for (int i = 0; i < 7; i++) {
                    LocalDate d = inicio.plusDays(i);
                    Double val = null;
                    for (MedicaoDiaria m : medicoes) {
                        if (m.getObsDate() != null && m.getObsDate().equals(d)) {
                            val = extrairValorVariavelDiaria(m, variavel);
                            break;
                        }
                    }
                    valores.add(val);
                }
                return valores;
            }
            
            @Override
            protected void done() {
                try {
                    List<Double> valores = get();
                    popup.removeAll();
                    JPanel p = new JPanel(new BorderLayout());
                    p.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
                    
                    double min = Double.MAX_VALUE;
                    double max = -Double.MAX_VALUE;
                    double soma = 0;
                    int count = 0;
                    for (Double val : valores) {
                        if (val != null) {
                            if (val < min) min = val;
                            if (val > max) max = val;
                            soma += val;
                            count++;
                        }
                    }
                    
                    JPanel header = new JPanel(new java.awt.GridLayout(2, 1));
                    header.add(new JLabel(" " + estacao.getStationName() + " (Últimos 7 dias)"));
                    if (count > 0) {
                        double media = soma / count;
                        String stats = String.format(Locale.forLanguageTag("pt-BR"), " Min: %.1f | Max: %.1f | Média: %.1f", min, max, media);
                        JLabel statsLabel = new JLabel(stats);
                        statsLabel.setFont(new java.awt.Font("SansSerif", java.awt.Font.PLAIN, 10));
                        header.add(statsLabel);
                    } else {
                        header.add(new JLabel(" Sem dados no período"));
                    }
                    
                    p.add(header, BorderLayout.NORTH);
                    p.add(new SparklineRenderer(valores, new java.awt.Color(46, 160, 67)), BorderLayout.CENTER);
                    popup.add(p);
                    popup.pack();
                } catch (Exception e) {
                    popup.removeAll();
                    popup.add(new JLabel(" Erro ao carregar dados. "));
                    popup.pack();
                }
            }
        };
        worker.execute();
    }
    
    private Double extrairValorVariavelDiaria(MedicaoDiaria medicao, String variavel) {
        return switch (variavel) {
            case "Temperatura" -> medicao.getTempAvg();
            case "Chuva" -> medicao.getPrecipTotal();
            case "Umidade" -> medicao.getHumidityAvg();
            case "Vento" -> medicao.getWindspeedAvg();
            case "Pressão" -> medicao.getPressureMax() != null && medicao.getPressureMin() != null ? (medicao.getPressureMax() + medicao.getPressureMin()) / 2.0 : medicao.getPressureMax();
            default -> null;
        };
    }

    private void alternarHeatmap() {
        boolean ativo = painelFiltros.isHeatmapAtivo();
        mapaPanel.setHeatmapAtivo(ativo);
        if (ativo && mapaPanel.getQuantidadePontosHeatmap() < 2) {
            atualizarStatus(
                    "Heatmap ativado. Clique em Buscar para carregar "
                            + "os dados do período e da variável."
            );
        } else if (ativo) {
            atualizarStatus(
                    "Heatmap ativado com "
                            + mapaPanel.getQuantidadePontosHeatmap()
                            + " pontos."
            );
        } else if (!ativo) {
            atualizarStatus("Heatmap desativado.");
        }
    }

    private void alternarZonas() {
        boolean ativas = painelFiltros.isZonasAtivas();
        mapaPanel.setZonasAtivas(ativas);
        atualizarStatus(ativas ? "Zonas de alagamento ativadas." : "Zonas de alagamento desativadas.");
    }

    private void alternarIsolinhas() {
        boolean ativas = painelFiltros.isIsolinhasAtivas();
        mapaPanel.setIsolinhasAtivas(ativas);
        atualizarStatus(ativas ? "Isolinhas ativadas." : "Isolinhas desativadas.");
    }

    private String descricaoResultadoHeatmap(
            boolean heatmapAtivo,
            int quantidadePontos
    ) {
        if (!heatmapAtivo) {
            return "";
        }
        if (quantidadePontos < 2) {
            return " Heatmap não exibido: são necessários ao menos "
                    + "dois pontos válidos.";
        }
        return " Heatmap atualizado com "
                + quantidadePontos
                + " estações.";
    }

    private void selecionarEstacaoDoFiltro() {
        Estacao estacao = painelFiltros.getEstacaoSelecionada();
        if (estacao != null) {
            mapaPanel.selecionarEstacao(estacao.getStationId());
        }
    }

    /**
     * A linha mantém o objeto MedicaoDiaria, inclusive o station_id real.
     * A View apenas sincroniza os componentes; o resumo continua vindo da
     * Service.
     */
    private void tratarSelecaoTabela() {
        MedicaoDiaria medicao = painelTabela.getMedicaoSelecionada();
        if (medicao == null) {
            return;
        }

        String stationId = medicao.getStationId();
        if (stationId == null || stationId.isBlank()) {
            Estacao estacaoAtual = painelFiltros.getEstacaoSelecionada();
            stationId = estacaoAtual == null
                    ? null
                    : estacaoAtual.getStationId();
        } else {
            painelFiltros.selecionarEstacaoPorId(stationId);
        }

        if (stationId == null) {
            return;
        }

        mapaPanel.selecionarEstacao(stationId);
        if (ultimoResumo != null
                && Objects.equals(
                        stationId,
                        ultimoResumo.getStationId()
                )) {
            exibirResumo(
                    ultimoResumo,
                    ultimaDataInicial,
                    ultimaDataFinal
            );
        }
        atualizarStatus(
                "Estação " + stationId
                        + " selecionada na tabela e destacada no mapa."
        );
    }

    private void exibirResumo(
            ResumoMeteorologico resumo,
            LocalDate dataInicial,
            LocalDate dataFinal
    ) {
        String estacao = resumo.getStationId();
        if (resumo.getNomeEstacao() != null
                && !resumo.getNomeEstacao().isBlank()) {
            estacao += " - " + resumo.getNomeEstacao();
        }

        String texto = """
                Estação: %s
                Período: %s a %s

                Temperatura mínima: %s
                Temperatura máxima: %s
                Temperatura média: %s
                Umidade média: %s
                Vento médio: %s
                Vento máximo: %s
                Pressão média: %s
                Chuva total no período: %s
                Chuva 24h: %s
                Chuva 48h: %s
                Chuva 72h: %s
                Nível de alerta: %s

                Região climática: %s
                Média histórica de verão: %s
                Média histórica de inverno: %s
                Onda de calor: %s
                Onda de frio: %s
                """.formatted(
                estacao,
                dataInicial,
                dataFinal,
                formatarValor(resumo.getTemperaturaMinima()),
                formatarValor(resumo.getTemperaturaMaxima()),
                formatarValor(resumo.getTemperaturaMedia()),
                formatarValor(resumo.getUmidadeMedia()),
                formatarValor(resumo.getVentoMedio()),
                formatarValor(resumo.getVentoMaximo()),
                formatarValor(resumo.getPressaoMedia()),
                formatarValor(resumo.getChuvaTotal()),
                formatarValor(resumo.getChuva24h()),
                formatarValor(resumo.getChuva48h()),
                formatarValor(resumo.getChuva72h()),
                resumo.getNivelAlerta() == null
                        ? "—"
                        : resumo.getNivelAlerta().toString(),
                resumo.getRegiaoClimatica() == null
                        ? "—"
                        : resumo.getRegiaoClimatica().toString(),
                formatarValor(resumo.getMediaHistoricaVerao()),
                formatarValor(resumo.getMediaHistoricaInverno()),
                formatarOnda(
                        resumo.getOndaCalor(),
                        resumo.getDiasConsecutivosOndaCalor()
                ),
                formatarOnda(
                        resumo.getOndaFrio(),
                        resumo.getDiasConsecutivosOndaFrio()
                )
        );

        areaResumo.setText(texto);
        areaResumo.setCaretPosition(0);
    }

    private String formatarValor(Double valor) {
        return valor == null
                ? "—"
                : String.format(Locale.forLanguageTag("pt-BR"), "%.2f", valor);
    }

    private String formatarOnda(Boolean ativo, Integer diasConsecutivos) {
        String texto = Boolean.TRUE.equals(ativo) ? "SIM" : "Não";
        int dias = diasConsecutivos == null ? 0 : diasConsecutivos;
        return texto + " (maior sequência: " + dias + " dias)";
    }

    private LocalDate converterData(String texto, String nomeCampo) {
        if (texto == null || texto.isBlank()) {
            throw new IllegalArgumentException(
                    "Preencha a " + nomeCampo + "."
            );
        }

        try {
            return LocalDate.parse(texto);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException(
                    "A " + nomeCampo
                            + " deve usar o formato yyyy-MM-dd.",
                    e
            );
        }
    }

    private void tratarErroCarregamento(String mensagem, Throwable causa) {
        painelFiltros.setEstacoes(List.of());
        mapaPanel.atualizarEstacoes(List.of());
        painelFiltros.setControlesHabilitados(false);
        mostrarErro(mensagem, causa);
    }

    private void mostrarAviso(String mensagem) {
        atualizarStatus(mensagem);
        JOptionPane.showMessageDialog(
                this,
                mensagem,
                "Atenção",
                JOptionPane.WARNING_MESSAGE
        );
    }

    private void mostrarErro(String mensagem, Throwable causa) {
        String detalhe = causa == null || causa.getMessage() == null
                ? "Detalhes indisponíveis."
                : causa.getMessage();
        String mensagemCompleta = mensagem + System.lineSeparator() + detalhe;

        atualizarStatus(mensagem);
        JOptionPane.showMessageDialog(
                this,
                mensagemCompleta,
                "Erro",
                JOptionPane.ERROR_MESSAGE
        );
    }

    private void atualizarStatus(String mensagem) {
        labelStatus.setText(mensagem);
    }

    private record ResultadoCargaInicial(
            List<Estacao> estacoes,
            List<String> localidades
    ) {
    }

    private record ResultadoBusca(
            List<MedicaoDiaria> medicoes,
            ResumoMeteorologico resumo,
            List<HeatmapPoint> pontosHeatmap,
            List<HeatmapPoint> pontosVariaveis,
            TendenciaChuva tendencia
    ) {
    }

    private record ResultadoBuscaRapida(
            List<HeatmapPoint> pontosHeatmap,
            List<HeatmapPoint> pontosVariaveis,
            ResumoMeteorologico resumo,
            TendenciaChuva tendencia
    ) {
    }
}
