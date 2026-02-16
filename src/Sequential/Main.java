package Sequential;

public class Main {
    public static void main(String[] args) {
        try {
            InstructionParser.parse("instructions.txt");
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

            GUI gui = new GUI();
            gui.start();
    }
}
