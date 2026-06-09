import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class MainPage extends JPanel {

    private static final Color BG_DARK   = new Color(0x050510);
    private static final Color BG_PANEL  = new Color(0x111827);
    private static final Color BORDER    = new Color(0x0F3460);
    private static final Color ACCENT    = new Color(0xE94560);
    private static final Color TEAL      = new Color(0x80CBC4);
    private static final Color LT_GREEN  = new Color(0xC8E6C9);
    private static final Color GOLD      = new Color(0xFFD700);
    private static final Color TEXT_MAIN = new Color(0xEEEEEE);
    private static final Color TEXT_SUB  = new Color(0xAAAAAA);
    private static final Color WM_GREEN  = new Color(0x66BB6A);

    public MainPage(String username) {
        setPreferredSize(new Dimension(Constants.TOTAL_WIDTH, Constants.GAME_HEIGHT));
        setLayout(new BorderLayout());
        setBackground(BG_DARK);

        Font trebuchet, trebuchetItalic;
        try {
            trebuchet       = Font.createFont(Font.TRUETYPE_FONT, new java.io.File("trebuc.ttf")).deriveFont(Font.PLAIN, 13f);
            trebuchetItalic = Font.createFont(Font.TRUETYPE_FONT, new java.io.File("Trebuchet-MS-Italic.ttf")).deriveFont(Font.ITALIC, 11f);
        } catch (Exception ex) {
            trebuchet       = new Font("Trebuchet MS", Font.PLAIN,  13);
            trebuchetItalic = new Font("Trebuchet MS", Font.ITALIC, 11);
        }
        final Font titleFont  = trebuchet.deriveFont(Font.BOLD,  26f);
        final Font labelFont  = trebuchet.deriveFont(Font.BOLD,  11f);
        final Font italicFont = trebuchetItalic.deriveFont(13f);
        final Font btnFont    = trebuchet.deriveFont(Font.BOLD,  13f);

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setOpaque(false);

        content.add(Box.createVerticalStrut(42));
        content.add(centeredLabel("🍉",                       titleFont.deriveFont(Font.PLAIN, 44f), TEXT_MAIN));
        content.add(Box.createVerticalStrut(8));
        content.add(centeredLabel("WATERMELON FIGHTERS",      titleFont,                             LT_GREEN));
        content.add(Box.createVerticalStrut(4));
        content.add(centeredLabel("Ready to defend the melon?", italicFont.deriveFont(14f),          TEAL));
        content.add(Box.createVerticalStrut(10));
        content.add(makeDivider(ACCENT));
        content.add(Box.createVerticalStrut(18));
        content.add(centeredLabel("Welcome back, " + username + "!", labelFont.deriveFont(15f), GOLD));
        content.add(Box.createVerticalStrut(28));

        JButton playBtn = makeButton("▶  PLAY GAME", WM_GREEN, btnFont);
        JButton quitBtn = makeButton("✕  QUIT",      ACCENT,   btnFont);
        playBtn.addActionListener(e -> Main.onPlay());
        quitBtn.addActionListener(e -> System.exit(0));

        content.add(playBtn);
        content.add(Box.createVerticalStrut(10));
        content.add(quitBtn);
        content.add(Box.createVerticalStrut(20));
        content.add(makeDivider(BORDER));
        content.add(Box.createVerticalStrut(12));

        for (String tip : new String[]{
                "Place towers on the grass  •  Enemies follow the path",
                "Survive all 15 waves to win  •  Don't let them reach the end"}) {
            content.add(centeredLabel(tip, italicFont.deriveFont(10f), TEXT_SUB));
            content.add(Box.createVerticalStrut(4));
        }

        content.add(Box.createVerticalGlue());
        content.add(centeredLabel("ETHAN XIONG & CHARLIE CAMPION  •  Tower Defense Project",
                italicFont.deriveFont(9f), new Color(0x444466)));
        content.add(Box.createVerticalStrut(14));

        JPanel root = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g0) {
                super.paintComponent(g0);
                Graphics2D g = (Graphics2D) g0;
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth(), h = getHeight();
                g.setPaint(new GradientPaint(w / 2f, 0, new Color(0x12122A), w / 2f, h, BG_DARK));
                g.fillRect(0, 0, w, h);
                g.setColor(new Color(255, 255, 255, 8));
                g.setStroke(new BasicStroke(1));
                for (int x = 0; x < w; x += 40) g.drawLine(x, 0, x, h);
                for (int y = 0; y < h; y += 40) g.drawLine(0, y, w, y);
                int cw = 420, ch = 510;
                int cx = (w - cw) / 2, cy = (h - ch) / 2;
                for (int d = 18; d >= 1; d--) {
                    g.setColor(new Color(233, 69, 96, 6));
                    g.drawRoundRect(cx - d * 2, cy - d * 2, cw + d * 4, ch + d * 4, 20, 20);
                }
                g.setColor(BG_PANEL);
                g.fillRoundRect(cx, cy, cw, ch, 14, 14);
                g.setColor(ACCENT);
                g.fillRoundRect(cx + 1, cy + 1, cw - 2, 5, 14, 14);
                g.fillRect(cx + 1, cy + 4, cw - 2, 5);
                g.setColor(BORDER);
                g.setStroke(new BasicStroke(1.5f));
                g.drawRoundRect(cx, cy, cw, ch, 14, 14);
                g.setColor(new Color(0x2E7D32));
                g.setStroke(new BasicStroke(3f));
                g.drawLine(cx + 20, cy + ch - 20, cx + cw - 20, cy + ch - 20);
            }
        };
        root.setOpaque(false);
        root.add(content, BorderLayout.CENTER);
        add(root, BorderLayout.CENTER);
    }

    private JLabel centeredLabel(String text, Font f, Color c) {
        JLabel l = new JLabel(text, SwingConstants.CENTER);
        l.setFont(f); l.setForeground(c);
        l.setAlignmentX(Component.CENTER_ALIGNMENT);
        return l;
    }

    private JSeparator makeDivider(Color c) {
        JSeparator sep = new JSeparator();
        sep.setForeground(c); sep.setBackground(c);
        sep.setMaximumSize(new Dimension(360, 2));
        sep.setAlignmentX(Component.CENTER_ALIGNMENT);
        return sep;
    }

    private JButton makeButton(String text, Color accent, Font f) {
        JButton btn = new JButton(text) {
            @Override protected void paintComponent(Graphics g0) {
                Graphics2D g = (Graphics2D) g0;
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (getModel().isPressed())       g.setColor(accent.darker());
                else if (getModel().isRollover()) g.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 40));
                else                              g.setColor(BG_DARK);
                g.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g.setColor(accent);
                g.setStroke(new BasicStroke(getModel().isRollover() ? 2f : 1.5f));
                g.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 8, 8);
                g.setColor(getModel().isRollover() ? accent : TEXT_MAIN);
                g.setFont(getFont());
                FontMetrics fm = g.getFontMetrics();
                int tx = (getWidth()  - fm.stringWidth(getText())) / 2;
                int ty = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;
                g.drawString(getText(), tx, ty);
            }
        };
        btn.setFont(f);
        btn.setPreferredSize(new Dimension(280, 44));
        btn.setMaximumSize(new Dimension(280, 44));
        btn.setAlignmentX(Component.CENTER_ALIGNMENT);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }
}