import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.*;
import java.io.PrintWriter;
import java.util.Scanner;

public class LoginPage extends JPanel {

    // palette
    private static final Color BG_DARK   = new Color(0x050510);
    private static final Color BG_PANEL  = new Color(0x111827);
    private static final Color BG_MID    = new Color(0x1A1A2E);
    private static final Color BORDER    = new Color(0x0F3460);
    private static final Color ACCENT    = new Color(0xE94560);
    private static final Color TEAL      = new Color(0x80CBC4);
    private static final Color LT_GREEN  = new Color(0xC8E6C9);
    private static final Color GOLD      = new Color(0xFFD700);
    private static final Color TEXT_MAIN = new Color(0xEEEEEE);
    private static final Color TEXT_SUB  = new Color(0xAAAAAA);
    private static final Color FIELD_BG  = new Color(0x0D1B2A);
    private static final Color ERR_RED   = new Color(0xFF4444);
    private static final Color OK_GREEN  = new Color(0x66BB6A);

    public LoginPage() {
        setPreferredSize(new Dimension(Constants.TOTAL_WIDTH, Constants.GAME_HEIGHT));
        setLayout(new BorderLayout());
        setBackground(BG_DARK);

        // fonts
        Font trebuchet, trebuchetItalic;
        try {
            trebuchet       = Font.createFont(Font.TRUETYPE_FONT, new java.io.File("trebuc.ttf")).deriveFont(Font.PLAIN, 13f);
            trebuchetItalic = Font.createFont(Font.TRUETYPE_FONT, new java.io.File("Trebuchet-MS-Italic.ttf")).deriveFont(Font.ITALIC, 11f);
        } catch (Exception e) {
            trebuchet       = new Font("Trebuchet MS", Font.PLAIN,  13);
            trebuchetItalic = new Font("Trebuchet MS", Font.ITALIC, 11);
        }
        final Font titleFont  = trebuchet.deriveFont(Font.BOLD,  28f);
        final Font labelFont  = trebuchet.deriveFont(Font.BOLD,  10f);
        final Font bodyFont   = trebuchet;
        final Font italicFont = trebuchetItalic;
        final Font btnFont    = trebuchet.deriveFont(Font.BOLD,  13f);

        // card layout
        CardLayout cl = new CardLayout();
        JPanel showing = new JPanel(cl);
        showing.setOpaque(false);

        // login page
        JPanel loginPage = makeBoxPanel();
        JTextField   loginUser = makeField(bodyFont);
        JPasswordField loginPass = makePassField(bodyFont);

        JButton lBtn   = makeButton("▶  LOGIN",   ACCENT,  btnFont);
        JButton toRBtn = makeButton("  REGISTER", BORDER,  btnFont);

        lBtn.addActionListener(e -> {
            try (Scanner sc = Prompt.getInputScanner()) {
                if (sc != null) {
                    while (sc.hasNextLine()) {
                        String u = sc.nextLine();
                        String p = sc.hasNextLine() ? sc.nextLine() : "";
                        for (int i = 0; i < 10 && sc.hasNextLine(); i++) sc.nextLine();
                        if (loginUser.getText().equals(u) &&
                            new String(loginPass.getPassword()).equals(p)) {
                            String username = loginUser.getText();
                            loginUser.setText(""); loginPass.setText("");
                            Main.onLogin(username);
                            return;
                        }
                    }
                }
            }
            showRetroDialog("INVALID CREDENTIALS", ERR_RED);
            loginUser.setText(""); loginPass.setText("");
        });

        toRBtn.addActionListener(e -> {
            loginUser.setText(""); loginPass.setText("");
            cl.show(showing, "register");
        });

        loginPage.add(Box.createVerticalStrut(36));
        loginPage.add(centeredLabel("🍉", titleFont.deriveFont(Font.PLAIN, 38f), TEXT_MAIN));
        loginPage.add(Box.createVerticalStrut(6));
        loginPage.add(centeredLabel("WATERMELON FIGHTERS", titleFont, LT_GREEN));
        loginPage.add(Box.createVerticalStrut(4));
        loginPage.add(centeredLabel("Player Login", italicFont.deriveFont(14f), TEAL));
        loginPage.add(Box.createVerticalStrut(6));
        loginPage.add(makeDivider(ACCENT));
        loginPage.add(Box.createVerticalStrut(28));
        loginPage.add(makeFormBlock("USERNAME", loginUser, labelFont));
        loginPage.add(Box.createVerticalStrut(18));
        loginPage.add(makeFormBlock("PASSWORD", loginPass, labelFont));
        loginPage.add(Box.createVerticalStrut(32));
        loginPage.add(lBtn);
        loginPage.add(Box.createVerticalStrut(12));
        loginPage.add(toRBtn);
        loginPage.add(Box.createVerticalStrut(20));
        loginPage.add(centeredLabel("ESC closes window", italicFont, TEXT_SUB));

        // register page
        JPanel regPage = makeBoxPanel();
        JTextField   regUser = makeField(bodyFont);
        JPasswordField regPass = makePassField(bodyFont);

        JButton rBtn   = makeButton("▶  CREATE ACCOUNT", TEAL,   btnFont);
        JButton tolBtn = makeButton("  BACK TO LOGIN",   BORDER, btnFont);

        rBtn.addActionListener(e -> {
            String u = regUser.getText();
            String p = new String(regPass.getPassword());
            if (!u.isEmpty() && !p.isEmpty()) {
                try (PrintWriter fw = Prompt.getPrintWriter()) {
                    if (fw != null) {
                        fw.println(u); fw.println(p); 
                        for (int i = 0; i < 10; i++) fw.println(0);
                        showRetroDialog("ACCOUNT CREATED!", OK_GREEN);
                        regUser.setText(""); regPass.setText("");
                        cl.show(showing, "login");
                    }
                } catch (Exception ex) { ex.printStackTrace(); }
            } else {
                showRetroDialog("FILL ALL FIELDS", GOLD);
            }
        });

        tolBtn.addActionListener(e -> {
            regUser.setText(""); regPass.setText("");
            cl.show(showing, "login");
        });

        regPage.add(Box.createVerticalStrut(36));
        regPage.add(centeredLabel("🍉",                                      titleFont.deriveFont(Font.PLAIN, 38f), TEXT_MAIN));
        regPage.add(Box.createVerticalStrut(6));
        regPage.add(centeredLabel("CREATE ACCOUNT",                          titleFont,                             TEAL));
        regPage.add(Box.createVerticalStrut(4));
        regPage.add(centeredLabel("Join the Watermelon Fighters",            italicFont.deriveFont(14f),            LT_GREEN));
        regPage.add(Box.createVerticalStrut(6));
        regPage.add(makeDivider(TEAL));
        regPage.add(Box.createVerticalStrut(28));
        regPage.add(makeFormBlock("USERNAME", regUser,  labelFont));
        regPage.add(Box.createVerticalStrut(18));
        regPage.add(makeFormBlock("PASSWORD", regPass,  labelFont));
        regPage.add(Box.createVerticalStrut(32));
        regPage.add(rBtn);
        regPage.add(Box.createVerticalStrut(12));
        regPage.add(tolBtn);
        regPage.add(Box.createVerticalStrut(20));
        regPage.add(centeredLabel("Passwords stored locally in Registration.txt", italicFont, TEXT_SUB));

        showing.add(loginPage, "login");
        showing.add(regPage,   "register");

        // custom card background drawn
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

                int cw = 400, ch = 500;
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
            }
        };
        root.setOpaque(false);
        JPanel cardWrapper = new JPanel(new GridBagLayout());
        cardWrapper.setOpaque(false);
        JPanel cardSizer = new JPanel(new BorderLayout());
        cardSizer.setOpaque(false);
        cardSizer.setPreferredSize(new Dimension(400, 500));
        cardSizer.setMaximumSize(new Dimension(400, 500));
        cardSizer.add(showing, BorderLayout.CENTER);
        cardWrapper.add(cardSizer);
        root.add(cardWrapper, BorderLayout.CENTER);
        add(root, BorderLayout.CENTER);
    }

    // helpers 

    private JPanel makeBoxPanel() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setOpaque(false);
        return p;
    }

    private JLabel centeredLabel(String text, Font f, Color c) {
        JLabel l = new JLabel(text, SwingConstants.CENTER);
        l.setFont(f);
        l.setForeground(c);
        l.setAlignmentX(Component.CENTER_ALIGNMENT);
        return l;
    }

    private JSeparator makeDivider(Color c) {
        JSeparator sep = new JSeparator();
        sep.setForeground(c);
        sep.setBackground(c);
        sep.setMaximumSize(new Dimension(340, 2));
        sep.setAlignmentX(Component.CENTER_ALIGNMENT);
        return sep;
    }

    private JPanel makeFormBlock(String label, JComponent field, Font labelFont) {
        JPanel block = new JPanel();
        block.setLayout(new BoxLayout(block, BoxLayout.Y_AXIS));
        block.setOpaque(false);
        block.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel lbl = new JLabel(label);
        lbl.setFont(labelFont);
        lbl.setForeground(TEXT_SUB);
        lbl.setAlignmentX(Component.CENTER_ALIGNMENT);
        block.add(lbl);
        block.add(Box.createVerticalStrut(5));
        field.setAlignmentX(Component.CENTER_ALIGNMENT);
        block.add(field);
        return block;
    }

    private JTextField makeField(Font f) {
        JTextField tf = new JTextField(20);
        styleInput(tf, f);
        return tf;
    }

    private JPasswordField makePassField(Font f) {
        JPasswordField pf = new JPasswordField(20);
        styleInput(pf, f);
        return pf;
    }

    private void styleInput(JTextField tf, Font f) {
        tf.setFont(f.deriveFont(14f));
        tf.setForeground(TEXT_MAIN);
        tf.setBackground(FIELD_BG);
        tf.setCaretColor(TEAL);
        tf.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(BORDER, 1),
            BorderFactory.createEmptyBorder(8, 12, 8, 12)));
        tf.setPreferredSize(new Dimension(320, 42));
        tf.setMinimumSize(new Dimension(320, 42));
        tf.setMaximumSize(new Dimension(320, 42));
        tf.addFocusListener(new FocusAdapter() {
            public void focusGained(FocusEvent e) {
                tf.setBorder(BorderFactory.createCompoundBorder(
                    new LineBorder(TEAL, 1), BorderFactory.createEmptyBorder(8, 12, 8, 12)));
            }
            public void focusLost(FocusEvent e) {
                tf.setBorder(BorderFactory.createCompoundBorder(
                    new LineBorder(BORDER, 1), BorderFactory.createEmptyBorder(8, 12, 8, 12)));
            }
        });
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

    private void showRetroDialog(String message, Color accent) {
        JDialog dlg = new JDialog(Main.frame, "", true);
        dlg.setUndecorated(true);
        dlg.setSize(340, 130);
        dlg.setLocationRelativeTo(Main.frame);
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(BG_PANEL);
        p.setBorder(new LineBorder(accent, 2));
        JLabel msg = new JLabel(message, SwingConstants.CENTER);
        msg.setFont(new Font("Trebuchet MS", Font.BOLD, 14));
        msg.setForeground(accent);
        p.add(msg, BorderLayout.CENTER);
        JButton ok = new JButton("OK");
        ok.setFont(new Font("Trebuchet MS", Font.BOLD, 12));
        ok.setForeground(TEXT_MAIN);
        ok.setBackground(BG_MID);
        ok.setBorder(new LineBorder(accent, 1));
        ok.setFocusPainted(false);
        ok.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        ok.addActionListener(e -> dlg.dispose());
        JPanel btnP = new JPanel();
        btnP.setBackground(BG_PANEL);
        btnP.add(ok);
        p.add(btnP, BorderLayout.SOUTH);
        dlg.add(p);
        dlg.setVisible(true);
    }
}