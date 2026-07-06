package app;

import view.TelaPrincipal;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

/**
 * Ponto de entrada da interface Swing.
 */
public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        configurarAparencia();
        SwingUtilities.invokeLater(() -> {
            TelaPrincipal tela = new TelaPrincipal();
            tela.setVisible(true);
        });
    }

    private static void configurarAparencia() {
        try {
            UIManager.setLookAndFeel(
                    UIManager.getSystemLookAndFeelClassName()
            );
        } catch (
                ClassNotFoundException
                | InstantiationException
                | IllegalAccessException
                | UnsupportedLookAndFeelException e
        ) {
            System.err.println(
                    "Não foi possível aplicar o tema do sistema: "
                            + e.getMessage()
            );
        }
    }
}
