import javax.swing.SwingUtilities;
import javax.swing.JFrame;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> launchLogin());
    }

    public static void launchLogin() { // launches the login page
        // pass null as parent if LoginPage requires a parent component in its constructor
        LoginPage lp = new LoginPage(null);
        lp.setVisible(true);
        if (lp.getLoggedIn()) {
            lp.dispose();
            launchStart(lp.getUser());
        } else {
            System.exit(0);
        }
    }

    public static void launchStart(String username) { // opens the start page
        // create a frame and show the map select panel (MapSelectPanel does not have a String constructor)
        JFrame frame = new JFrame("Watermelon Fighters");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setResizable(false);

        MapSelectPanel start = new MapSelectPanel(frame);
        frame.add(start);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        start.requestFocusInWindow();
    }
}