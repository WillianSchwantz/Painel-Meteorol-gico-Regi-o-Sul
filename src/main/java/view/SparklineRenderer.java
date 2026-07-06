package view;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.List;
import javax.swing.JPanel;

public class SparklineRenderer extends JPanel {

    private final List<Double> values;
    private final Color lineColor;

    public SparklineRenderer(List<Double> values, Color lineColor) {
        this.values = values;
        this.lineColor = lineColor;
        setPreferredSize(new Dimension(120, 40));
        setOpaque(false);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (values == null || values.size() < 2) {
            return;
        }

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        double min = Double.MAX_VALUE;
        double max = -Double.MAX_VALUE;
        for (Double v : values) {
            if (v != null) {
                if (v < min) min = v;
                if (v > max) max = v;
            }
        }

        if (min == Double.MAX_VALUE) {
            g2.dispose();
            return;
        }

        if (max == min) {
            max = min + 1; // Prevent division by zero
        }

        int width = getWidth();
        int height = getHeight();
        int padding = 4;
        
        double xStep = (double) (width - 2 * padding) / (values.size() - 1);
        double yScale = (height - 2 * padding) / (max - min);

        g2.setColor(lineColor);
        g2.setStroke(new BasicStroke(1.5f));

        Integer prevX = null;
        Integer prevY = null;

        for (int i = 0; i < values.size(); i++) {
            Double v = values.get(i);
            if (v != null) {
                int x = padding + (int) (i * xStep);
                int y = height - padding - (int) ((v - min) * yScale);

                if (prevX != null && prevY != null) {
                    g2.drawLine(prevX, prevY, x, y);
                }
                
                prevX = x;
                prevY = y;
            } else {
                prevX = null;
                prevY = null;
            }
        }

        g2.dispose();
    }
}
