package Sequential;

public class Main {
    public static void main(String[] args) {
        String filename = "instructions.txt";
        boolean headless = false;

        for (String arg : args) {
            if ("--headless".equalsIgnoreCase(arg)) headless = true;
            else filename = arg;
        }

        try {
            InstructionParser.parse(filename);

            if (headless) {
                long start = System.nanoTime();
                while (!GameState.allBotsDone()) {
                    GameState.step();
                }
                long elapsedMs = (System.nanoTime() - start) / 1_000_000L;
                System.out.println("Mode: Sequential");
                System.out.println("Final tick: " + GameState.tick);
                System.out.println("Execution time: " + elapsedMs + " ms");
            } else {
                GUI gui = new GUI();
                gui.start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
