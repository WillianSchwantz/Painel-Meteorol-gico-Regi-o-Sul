package view;

import model.MedicaoDiaria;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.util.List;

/**
 * Tabela de medições diárias.
 */
public class PainelTabela extends JPanel {

    private final DefaultTableModel modeloTabela;
    private final JTable tabela;
    private String variavelSelecionada;
    private List<MedicaoDiaria> medicoesExibidas;

    public PainelTabela() {
        variavelSelecionada = "Temperatura";
        medicoesExibidas = List.of();
        modeloTabela = new DefaultTableModel(colunasDaVariavel(), 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }

            @Override
            public Class<?> getColumnClass(int columnIndex) {
                return columnIndex == 0 ? Object.class : Double.class;
            }
        };

        tabela = new JTable(modeloTabela);
        tabela.setAutoCreateRowSorter(true);
        tabela.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabela.setFillsViewportHeight(true);

        setLayout(new BorderLayout());
        setBorder(BorderFactory.createTitledBorder("Medições diárias"));
        add(new JScrollPane(tabela), BorderLayout.CENTER);
    }

    /**
     * Substitui as linhas atuais pelos valores reais de history_daily.
     */
    public void atualizarTabela(List<MedicaoDiaria> medicoes) {
        medicoesExibidas = List.copyOf(medicoes);
        modeloTabela.setRowCount(0);

        for (MedicaoDiaria medicao : medicoesExibidas) {
            modeloTabela.addRow(criarLinha(medicao));
        }
    }

    public void setVariavelSelecionada(String variavelSelecionada) {
        this.variavelSelecionada = variavelSelecionada == null
                ? "Temperatura"
                : variavelSelecionada;
        medicoesExibidas = List.of();
        modeloTabela.setColumnIdentifiers(colunasDaVariavel());
        modeloTabela.setRowCount(0);
    }

    public void limparTabela() {
        medicoesExibidas = List.of();
        modeloTabela.setRowCount(0);
    }

    public int getQuantidadeRegistros() {
        return modeloTabela.getRowCount();
    }

    public int getLinhaSelecionada() {
        return tabela.getSelectedRow();
    }

    /**
     * Converte o índice visual para o índice do model quando há ordenação.
     */
    public MedicaoDiaria getMedicaoSelecionada() {
        int linhaVisual = tabela.getSelectedRow();
        if (linhaVisual < 0) {
            return null;
        }

        int linhaModel = tabela.convertRowIndexToModel(linhaVisual);
        if (linhaModel < 0 || linhaModel >= medicoesExibidas.size()) {
            return null;
        }
        return medicoesExibidas.get(linhaModel);
    }

    public void adicionarListenerSelecao(ListSelectionListener listener) {
        tabela.getSelectionModel().addListSelectionListener(listener);
    }

    private String[] colunasDaVariavel() {
        return switch (variavelSelecionada) {
            case "Chuva" -> new String[]{
                "Data",
                "Taxa de chuva",
                "Chuva total"
            };
            case "Umidade" -> new String[]{
                "Data",
                "Umidade mínima",
                "Umidade máxima",
                "Umidade média"
            };
            case "Vento" -> new String[]{
                "Data",
                "Vento mínimo",
                "Vento máximo",
                "Vento médio",
                "Rajada máxima"
            };
            case "Pressão" -> new String[]{
                "Data",
                "Pressão mínima",
                "Pressão máxima",
                "Tendência da pressão"
            };
            default -> new String[]{
                "Data",
                "Temperatura mínima",
                "Temperatura máxima",
                "Temperatura média"
            };
        };
    }

    private Object[] criarLinha(MedicaoDiaria medicao) {
        return switch (variavelSelecionada) {
            case "Chuva" -> new Object[]{
                medicao.getObsDate(),
                medicao.getPrecipRate(),
                medicao.getPrecipTotal()
            };
            case "Umidade" -> new Object[]{
                medicao.getObsDate(),
                medicao.getHumidityLow(),
                medicao.getHumidityHigh(),
                medicao.getHumidityAvg()
            };
            case "Vento" -> new Object[]{
                medicao.getObsDate(),
                medicao.getWindspeedLow(),
                medicao.getWindspeedHigh(),
                medicao.getWindspeedAvg(),
                medicao.getWindgustHigh()
            };
            case "Pressão" -> new Object[]{
                medicao.getObsDate(),
                medicao.getPressureMin(),
                medicao.getPressureMax(),
                medicao.getPressureTrend()
            };
            default -> new Object[]{
                medicao.getObsDate(),
                medicao.getTempLow(),
                medicao.getTempHigh(),
                medicao.getTempAvg()
            };
        };
    }
}
