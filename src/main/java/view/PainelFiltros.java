package view;

import model.Estacao;
import org.jdatepicker.impl.JDatePanelImpl;
import org.jdatepicker.impl.JDatePickerImpl;
import org.jdatepicker.impl.UtilDateModel;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionListener;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Properties;

/**
 * Componentes de entrada usados para consultar as medicoes diarias.
 */
public class PainelFiltros extends JPanel {

    private static final String[] VARIAVEIS = {
        "Temperatura",
        "Chuva",
        "Umidade",
        "Vento",
        "Pressão"
    };

    private final JComboBox<String> comboCidade;
    private final JButton botaoFiltrarCidade;
    private final JComboBox<Estacao> comboEstacoes;
    private final JDatePickerImpl campoDataInicial;
    private final JDatePickerImpl campoDataFinal;
    private final JComboBox<String> comboVariaveis;
    private final JCheckBox checkBoxHeatmap;
    private final JCheckBox checkBoxZonas;
    private final JCheckBox checkBoxIsolinhas;
    private final JComboBox<String> comboTendencia;
    private final JTextField campoLimiar24h;
    private final JTextField campoLimiar48h;
    private final JTextField campoLimiar72h;
    private final JButton botaoBuscar;

    public PainelFiltros() {
        comboCidade = new JComboBox<>(new DefaultComboBoxModel<>());
        comboCidade.setEditable(true);
        botaoFiltrarCidade = new JButton("Filtrar");
        comboEstacoes = new JComboBox<>(new DefaultComboBoxModel<>());
        campoDataInicial = criarDatePicker("2025-01-01");
        campoDataFinal = criarDatePicker("2025-01-31");
        comboVariaveis = new JComboBox<>(VARIAVEIS);
        checkBoxHeatmap = new JCheckBox("Exibir heatmap");
        checkBoxZonas = new JCheckBox("Zonas de alerta");
        checkBoxIsolinhas = new JCheckBox("Isolinhas");
        comboTendencia = new JComboBox<>(
                new String[]{"7 dias", "15 dias", "30 dias"}
        );
        campoLimiar24h = new JTextField("30", 3);
        campoLimiar48h = new JTextField("50", 3);
        campoLimiar72h = new JTextField("60", 3);
        botaoBuscar = new JButton("Buscar");

        limitarLarguraDosCombos();
        configurarLayout();
    }

    private JDatePickerImpl criarDatePicker(String dataInicial) {
        UtilDateModel model = new UtilDateModel();
        try {
            Date date = new SimpleDateFormat("yyyy-MM-dd").parse(dataInicial);
            Calendar cal = Calendar.getInstance();
            cal.setTime(date);
            model.setDate(
                    cal.get(Calendar.YEAR),
                    cal.get(Calendar.MONTH),
                    cal.get(Calendar.DAY_OF_MONTH)
            );
            model.setSelected(true);
        } catch (ParseException e) {
            model.setSelected(false);
        }

        Properties propriedades = new Properties();
        propriedades.put("text.today", "Hoje");
        propriedades.put("text.month", "Mês");
        propriedades.put("text.year", "Ano");
        JDatePanelImpl datePanel = new JDatePanelImpl(model, propriedades);
        return new JDatePickerImpl(datePanel, new JFormattedTextField.AbstractFormatter() {
            private final SimpleDateFormat formatter =
                    new SimpleDateFormat("yyyy-MM-dd");

            @Override
            public Object stringToValue(String text) throws ParseException {
                return formatter.parse(text);
            }

            @Override
            public String valueToString(Object value) {
                if (value != null) {
                    Calendar cal = (Calendar) value;
                    return formatter.format(cal.getTime());
                }
                return "";
            }
        });
    }

    private void limitarLarguraDosCombos() {
        int alturaCidade = comboCidade.getPreferredSize().height;
        comboCidade.setPreferredSize(new Dimension(280, alturaCidade));
        comboCidade.setMinimumSize(new Dimension(180, alturaCidade));

        int alturaEstacao = comboEstacoes.getPreferredSize().height;
        comboEstacoes.setPreferredSize(new Dimension(520, alturaEstacao));
        comboEstacoes.setMinimumSize(new Dimension(260, alturaEstacao));

        int alturaVariavel = comboVariaveis.getPreferredSize().height;
        comboVariaveis.setPreferredSize(new Dimension(180, alturaVariavel));
        comboVariaveis.setMinimumSize(new Dimension(140, alturaVariavel));
    }

    private void configurarLayout() {
        setLayout(new GridBagLayout());
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Filtros"),
                BorderFactory.createEmptyBorder(4, 6, 6, 6)
        ));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.insets = new Insets(2, 2, 2, 2);

        gbc.gridy = 0;
        add(criarLinhaLocalizacao(), gbc);

        gbc.gridy = 1;
        add(criarLinhaConsulta(), gbc);

        gbc.gridy = 2;
        add(criarLinhaOpcoes(), gbc);
    }

    private JPanel criarLinhaLocalizacao() {
        JPanel linha = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(2, 5, 2, 5);
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0;
        linha.add(new JLabel("Cidade/UF:"), gbc);

        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 0.25;
        linha.add(comboCidade, gbc);

        gbc.gridx = 2;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0.0;
        linha.add(botaoFiltrarCidade, gbc);

        gbc.gridx = 3;
        linha.add(new JLabel("Estação:"), gbc);

        gbc.gridx = 4;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        linha.add(comboEstacoes, gbc);

        return linha;
    }

    private JPanel criarLinhaConsulta() {
        JPanel linha = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2));
        linha.add(new JLabel("Data inicial:"));
        linha.add(campoDataInicial);
        linha.add(new JLabel("Data final:"));
        linha.add(campoDataFinal);
        linha.add(new JLabel("Variável:"));
        linha.add(comboVariaveis);
        linha.add(new JLabel("Tendência:"));
        linha.add(comboTendencia);
        return linha;
    }

    private JPanel criarLinhaOpcoes() {
        JPanel linha = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2));
        linha.add(checkBoxHeatmap);
        linha.add(checkBoxZonas);
        linha.add(checkBoxIsolinhas);
        linha.add(new JLabel("Limiares(24/48/72):"));

        JPanel painelLimiares = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));
        painelLimiares.add(campoLimiar24h);
        painelLimiares.add(campoLimiar48h);
        painelLimiares.add(campoLimiar72h);
        linha.add(painelLimiares);
        linha.add(botaoBuscar);
        return linha;
    }

    /**
     * Substitui o conteudo do combo, mantendo objetos Estacao como itens.
     */
    public void setEstacoes(List<Estacao> estacoes) {
        DefaultComboBoxModel<Estacao> modelo = new DefaultComboBoxModel<>();
        for (Estacao estacao : estacoes) {
            modelo.addElement(estacao);
        }
        comboEstacoes.setModel(modelo);
        if (modelo.getSize() > 0) {
            comboEstacoes.setSelectedIndex(0);
        }
    }

    public void setLocalidades(List<String> localidades) {
        Object selecionado = comboCidade.getEditor().getItem();
        DefaultComboBoxModel<String> modelo = new DefaultComboBoxModel<>();
        modelo.addElement("");

        for (String localidade : localidades) {
            if (localidade != null && !localidade.isBlank()) {
                modelo.addElement(localidade);
            }
        }

        comboCidade.setModel(modelo);
        comboCidade.setEditable(true);
        if (selecionado != null && !selecionado.toString().isBlank()) {
            comboCidade.setSelectedItem(selecionado.toString());
        } else {
            comboCidade.setSelectedIndex(0);
        }
    }

    public Estacao getEstacaoSelecionada() {
        return (Estacao) comboEstacoes.getSelectedItem();
    }

    public String getFiltroCidade() {
        Object valor = comboCidade.getEditor().getItem();
        return valor == null ? "" : valor.toString().trim();
    }

    public String getDataInicial() {
        return campoDataInicial.getJFormattedTextField().getText().trim();
    }

    public String getDataFinal() {
        return campoDataFinal.getJFormattedTextField().getText().trim();
    }

    public String getVariavelSelecionada() {
        return (String) comboVariaveis.getSelectedItem();
    }

    public void adicionarAcaoBuscar(ActionListener listener) {
        botaoBuscar.addActionListener(listener);
    }

    public void adicionarAcaoFiltrarCidade(ActionListener listener) {
        botaoFiltrarCidade.addActionListener(listener);
        if (comboCidade.getEditor().getEditorComponent() instanceof JTextField editor) {
            editor.addActionListener(listener);
        }
    }

    public void adicionarAcaoSelecaoEstacao(ActionListener listener) {
        comboEstacoes.addActionListener(listener);
    }

    public boolean isHeatmapAtivo() {
        return checkBoxHeatmap.isSelected();
    }

    public void adicionarAcaoHeatmap(ActionListener listener) {
        checkBoxHeatmap.addActionListener(listener);
    }

    public boolean isZonasAtivas() {
        return checkBoxZonas.isSelected();
    }

    public void adicionarAcaoZonas(ActionListener listener) {
        checkBoxZonas.addActionListener(listener);
    }

    public boolean isIsolinhasAtivas() {
        return checkBoxIsolinhas.isSelected();
    }

    public void adicionarAcaoIsolinhas(ActionListener listener) {
        checkBoxIsolinhas.addActionListener(listener);
    }

    public int getDiasTendencia() {
        String selecionado = (String) comboTendencia.getSelectedItem();
        if (selecionado == null) {
            return 7;
        }
        return Integer.parseInt(selecionado.split(" ")[0]);
    }

    public double getLimiar24h() {
        try {
            return Double.parseDouble(campoLimiar24h.getText().trim());
        } catch (NumberFormatException e) {
            return 30.0;
        }
    }

    public double getLimiar48h() {
        try {
            return Double.parseDouble(campoLimiar48h.getText().trim());
        } catch (NumberFormatException e) {
            return 50.0;
        }
    }

    public double getLimiar72h() {
        try {
            return Double.parseDouble(campoLimiar72h.getText().trim());
        } catch (NumberFormatException e) {
            return 60.0;
        }
    }

    public boolean selecionarEstacaoPorId(String stationId) {
        for (int indice = 0; indice < comboEstacoes.getItemCount(); indice++) {
            Estacao estacao = comboEstacoes.getItemAt(indice);
            if (Objects.equals(stationId, estacao.getStationId())) {
                comboEstacoes.setSelectedIndex(indice);
                return true;
            }
        }
        return false;
    }

    public void setControlesHabilitados(boolean habilitados) {
        comboCidade.setEnabled(habilitados);
        botaoFiltrarCidade.setEnabled(habilitados);
        comboEstacoes.setEnabled(habilitados);
        campoDataInicial.setEnabled(habilitados);
        campoDataFinal.setEnabled(habilitados);
        comboVariaveis.setEnabled(habilitados);
        checkBoxHeatmap.setEnabled(habilitados);
        checkBoxZonas.setEnabled(habilitados);
        checkBoxIsolinhas.setEnabled(habilitados);
        comboTendencia.setEnabled(habilitados);
        campoLimiar24h.setEnabled(habilitados);
        campoLimiar48h.setEnabled(habilitados);
        campoLimiar72h.setEnabled(habilitados);
        botaoBuscar.setEnabled(habilitados);
    }
}
