import javax.swing.*;
import java.awt.*;

public class Main {

    // Shared card container — panels call App.show(...) to navigate
    public static JFrame      frame;
    public static JPanel      cards;
    public static CardLayout  cardLayout;

    // Keep references so we can replace the game panel each run
    private static LoginPage     loginPanel;
    private static MainPage      mainPanel;
    private static MapSelectPanel mapPanel;
    private static GamePanel     gamePanel;
    public  static String        currentUser = "";

    public static void main(String[] args) {
        SwingUtilities.invokeLater(Main::init);
    }

    private static void init() {
        frame = new JFrame("Watermelon Fighters");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setResizable(false);

        cardLayout = new CardLayout();
        cards = new JPanel(cardLayout);

        loginPanel = new LoginPage();
        cards.add(loginPanel, "login");

        // MainPage and MapSelectPanel are added lazily (need username / frame ref)
        // but we still need a placeholder so pack() sizes to the game resolution
        JPanel placeholder = new JPanel();
        placeholder.setPreferredSize(new Dimension(Constants.TOTAL_WIDTH, Constants.GAME_HEIGHT));
        cards.add(placeholder, "placeholder");

        frame.add(cards);
        cardLayout.show(cards, "login");
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    /** Called by LoginPage once credentials are verified. */
    public static void onLogin(String username) {
        currentUser = username;
        // Build (or rebuild) MainPage for this user
        mainPanel = new MainPage(username);
        cards.add(mainPanel, "main");

        // Build MapSelectPanel (needs a reference back so it can show the game)
        // Pass the main frame to the MapSelectPanel constructor (panel needs a reference back)
        mapPanel = new MapSelectPanel(frame, username);
        cards.add(mapPanel, "mapselect");

        showCard("main");
    }

    /** Called by MainPage "Play" button. */
    public static void onPlay() {
        showCard("mapselect");
    }

    /** Called by MapSelectPanel when the player picks a map. */
    public static void onMapSelected(MapData map) {
        // Remove old game panel if one exists
        if (gamePanel != null) cards.remove(gamePanel);
        gamePanel = new GamePanel(map);
        cards.add(gamePanel, "game");
        showCard("game");
        gamePanel.requestFocusInWindow();
    }

    /** Called by GamePanel (or wherever) to return to map selection. */
    public static void onBackToMapSelect() {
        cards.remove(mapPanel);
        mapPanel = new MapSelectPanel(frame, currentUser);
        cards.add(mapPanel, "mapselect");
        showCard("mapselect");
        mapPanel.requestFocusInWindow();
    }

    /** Called from anywhere to go back to main/home. */
    public static void onBackToMain() {
        showCard("main");
    }

    private static void showCard(String key) {
        cardLayout.show(cards, key);
        frame.revalidate();
        frame.repaint();
    }
}