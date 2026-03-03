package Parallel;

import javax.swing.*;
import java.awt.*;

public class GUI extends JPanel {

    private static final int TILE = 28;
    private static final int MARGIN = 20;

    private final Timer timer;

    public GUI() {
        int w = MARGIN * 2 + Parallel.GameState.WIDTH * TILE;
        int h = MARGIN * 2 + Parallel.GameState.HEIGHT * TILE;

        JFrame frame = new JFrame("Warehouse Bots");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setSize(w, h);
        frame.setResizable(false);
        frame.add(this);
        frame.setVisible(true);

        timer = new Timer(120, e -> {
            if (Parallel.GameState.allBotsDone()){
                ((Timer) e.getSource()).stop();
                repaint();
                System.out.println("All tasks finished at tick " + Parallel.GameState.tick + " ===");

                return;
            }
            Parallel.GameState.step();
            repaint();

        });
    }

    public void start() {
        timer.start();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;

        //background
        g2.setColor(new Color(245, 245, 245));
        g2.fillRect(0, 0, getWidth(), getHeight());

        //grid
        for (int y = 0; y < Parallel.GameState.HEIGHT; y++) {
            for (int x = 0; x < Parallel.GameState.WIDTH; x++) {
                int px = MARGIN + x * TILE;
                int py = MARGIN + y * TILE;

                if (Parallel.GameState.isShelfTile(x, y)) {
                    g2.setColor(new Color(120, 80, 200));
                } else {
                    g2.setColor(Color.WHITE);
                }
                g2.fillRect(px, py, TILE, TILE);

                g2.setColor(new Color(210, 210, 210));
                g2.drawRect(px, py, TILE, TILE);
            }
        }

        for (Bot b : Parallel.GameState.bots) {
            Location p = b.getLocation();
            int px = MARGIN + p.getX() * TILE;
            int py = MARGIN + p.getY() * TILE;


            switch (b.getPhase()) {
                case DONE -> g2.setColor(new Color(120, 120, 120));
                case PICKUP -> g2.setColor(new Color(60, 120, 255));
                case DROPOFF -> g2.setColor(new Color(255, 150, 50));
            }

            //bot body
            g2.fillRect(px + 3, py + 3, TILE - 6, TILE - 6);
            g2.setColor(Color.BLACK);
            g2.drawRect(px + 3, py + 3, TILE - 6, TILE - 6);

            //bot label
            g2.setFont(new Font("Arial", Font.BOLD, 12));
            g2.drawString("B" + (b.getId() + 1), px + 6, py + TILE - 8);

            //done overlay
            if (b.isDone()) {
                g2.setFont(new Font("Arial", Font.BOLD, 10));
                g2.drawString("DONE", px + 6, py + 12);
            }
        }
        //hud
        g2.setColor(Color.BLACK);
        g2.drawString("Tick: " + GameState.tick, 10, 15);
    }
}
