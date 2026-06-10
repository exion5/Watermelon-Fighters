import javax.swing.*;
import java.awt.*;

public class Main {

    // Shared card container and layout for switching between screens
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

        JPanel placeholder = new JPanel();
        placeholder.setPreferredSize(new Dimension(Constants.TOTAL_WIDTH, Constants.GAME_HEIGHT));
        cards.add(placeholder, "placeholder");

        frame.add(cards);
        cardLayout.show(cards, "login");
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    // Called by LoginPage once credentials are verified
    public static void onLogin(String username) {
        currentUser = username;
        // Build (or rebuild) MainPage for this user
        mainPanel = new MainPage(username);
        cards.add(mainPanel, "main");

        // Build MapSelectPanel 
        mapPanel = new MapSelectPanel(frame, username);
        cards.add(mapPanel, "mapselect");

        showCard("main");
    }

    // Called by MainPage "Play" button
    public static void onPlay() {
        showCard("mapselect");
    }

    // Called by MapSelectPanel when the player picks a map
    public static void onMapSelected(MapData map) {
        Sound.stopBgMusic();
        if (gamePanel != null) cards.remove(gamePanel);
        gamePanel = new GamePanel(map);
        cards.add(gamePanel, "game");
        showCard("game");
        gamePanel.requestFocusInWindow();
    }

    // Called by GamePanel (or wherever) to return to map selection
    public static void onBackToMapSelect() {
        cards.remove(mapPanel);
        mapPanel = new MapSelectPanel(frame, currentUser);
        cards.add(mapPanel, "mapselect");
        showCard("mapselect");
        mapPanel.requestFocusInWindow();
    }

    // Called from anywhere to go back to main/home
    public static void onBackToMain() {
        Sound.stopBgMusic();
        if (gamePanel != null) { 
            cards.remove(gamePanel); 
            gamePanel = null; }
        mainPanel = new MainPage(currentUser);
        cards.add(mainPanel, "main");
        showCard("main");
    }

    // Called by MainPage "Back to Login" button
    public static void onBackToLogin() {
        currentUser = "";
        showCard("login");
    }

    private static void showCard(String key) {
        cardLayout.show(cards, key);
        frame.revalidate();
        frame.repaint();
    }
}