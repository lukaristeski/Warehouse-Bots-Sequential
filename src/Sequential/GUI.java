package Sequential;

import javax.swing.*;
import java.awt.*;

public class GUI extends JPanel {
    private static final int MAX_TILE = 28;
    private static final int MIN_TILE = 4;
    private static final int MARGIN = 20;

    private long startTime;
    private final Timer timer;

    private final int tileSize;

    public GUI(boolean controlsSimulation) {

        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        int availableWidth = Math.max(300, screen.width - 100 - MARGIN * 2);
        int availableHeight = Math.max(300, screen.height - 160 - MARGIN * 2);
        int fitX = Math.max(1, availableWidth / Math.max(1, GameState.WIDTH));
        int fitY = Math.max(1, availableHeight / Math.max(1, GameState.HEIGHT));
        tileSize = Math.max(MIN_TILE, Math.min(MAX_TILE, Math.min(fitX, fitY)));

        int contentWidth = MARGIN * 2 + GameState.WIDTH * tileSize;
        int contentHeight = MARGIN * 2 + GameState.HEIGHT * tileSize;
        setPreferredSize(new Dimension(contentWidth, contentHeight));

        JScrollPane scrollPane = new JScrollPane(this);
        scrollPane.setPreferredSize(new Dimension(
                Math.min(contentWidth + 20, screen.width - 80),
                Math.min(contentHeight + 20, screen.height - 120)
        ));

        JFrame frame = new JFrame("Warehouse Bots");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setContentPane(scrollPane);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        timer = new Timer(30, event -> {
            if (GameState.allBotsDone()) {
                ((Timer) event.getSource()).stop();

                long executionTime =
                        (System.nanoTime() - startTime) / 1_000_000L;

                System.out.println();
                System.out.println("Mode: Sequential");
                System.out.println("Final tick: " + GameState.tick);
                System.out.println("Execution time: " + executionTime + " ms");

                repaint();
                return;
            }

            if (controlsSimulation) {
                GameState.step();
            }

            repaint();
        });
    }

    public GUI() { this(true); }

    public void start() {
        startTime = System.nanoTime();
        timer.start(); }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics;
        g.setColor(new Color(245, 245, 245));
        g.fillRect(0, 0, getWidth(), getHeight());

        for (int y = 0; y < GameState.HEIGHT; y++) {
            for (int x = 0; x < GameState.WIDTH; x++) {
                int px = MARGIN + x * tileSize;
                int py = MARGIN + y * tileSize;
                g.setColor(GameState.isShelfTile(x, y) ? new Color(120, 80, 200) : Color.WHITE);
                g.fillRect(px, py, tileSize, tileSize);
                if (tileSize >= 6) {
                    g.setColor(new Color(210, 210, 210));
                    g.drawRect(px, py, tileSize, tileSize);
                }
            }
        }

        if (GameState.bots != null) {
            for (Bot bot : GameState.bots) {
                if (bot == null || !bot.isActive()) continue;
                Location location = bot.getLocation();
                int px = MARGIN + location.getX() * tileSize;
                int py = MARGIN + location.getY() * tileSize;
                switch (bot.getPhase()) {
                    case DONE -> g.setColor(new Color(120, 120, 120));
                    case PICKUP -> g.setColor(new Color(60, 120, 255));
                    case DROPOFF -> g.setColor(new Color(255, 150, 50));
                }
                int inset = Math.max(1, tileSize / 8);
                g.fillRect(px + inset, py + inset,
                        Math.max(1, tileSize - 2 * inset), Math.max(1, tileSize - 2 * inset));
                if (tileSize >= 12) {
                    g.setColor(Color.BLACK);
                    g.setFont(new Font("Arial", Font.BOLD, Math.max(8, tileSize / 3)));
                    g.drawString("B" + (bot.getId() + 1), px + 2, py + tileSize - 3);
                }
            }
        }
        g.setColor(Color.BLACK);
        g.drawString("Tick: " + GameState.tick, 10, 15);
    }
}
