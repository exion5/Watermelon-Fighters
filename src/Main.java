import javax.swing.*;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Watermelon Fighters");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setResizable(false);

            MapSelectPanel mapSelect = new MapSelectPanel(frame);
            frame.add(mapSelect);
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
            mapSelect.requestFocusInWindow();
        });
    }
}