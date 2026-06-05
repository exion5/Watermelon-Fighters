import javax.swing.*;
import java.awt.*;
import java.awt.geom.*;
import java.awt.event.*;
import javax.swing.border.LineBorder;
import java.io.PrintWriter;
import java.util.Scanner;

public class LoginPage extends JDialog {
    private boolean loggedIn = false;
    private String user = "";

    // ── Palette (matches Watermelon Munchers / monkey tower game) ──────────
    private static final Color bgDark = new Color(0x050510);   // near-black
    private static final Color bgPanel = new Color(0x111827);   // card bg
    private static final Color bgMid = new Color(0x1A1A2E);   // sidebar/mid
    private static final Color borderCol = new Color(0x0F3460);   // deep blue border
    private static final Color accentRed = new Color(0xE94560);   // red accent
    private static final Color teal = new Color(0x80CBC4);   // teal text
    private static final Color ltGreen = new Color(0xC8E6C9);   // light green
    private static final Color gold = new Color(0xFFD700);   // gold
    private static final Color textMain = new Color(0xEEEEEE);   // main text
    private static final Color textSub = new Color(0xAAAAAA);   // subtext
    private static final Color fieldBg = new Color(0x0D1B2A);   // input bg
    private static final Color errRed = new Color(0xFF4444);
    private static final Color okGreen = new Color(0x66BB6A);

    public LoginPage(Frame owner) {
        super(owner, "Watermelon Fighters - Login", true);
        setModal(true);
        setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) { System.exit(0); }
        });
        setSize(520, 600);
        setLocationRelativeTo(null);
        setResizable(false);

        Font trebuchet, trebuchetItalic; // fonts
        try {
            trebuchet = Font.createFont(Font.TRUETYPE_FONT,
                new java.io.File("trebuc.ttf")).deriveFont(Font.PLAIN, 13f);
            trebuchetItalic = Font.createFont(Font.TRUETYPE_FONT,
                new java.io.File("Trebuchet-MS-Italic.ttf")).deriveFont(Font.ITALIC, 11f);
        } catch (Exception e) {
            trebuchet       = new Font("Trebuchet MS", Font.PLAIN,  13);
            trebuchetItalic = new Font("Trebuchet MS", Font.ITALIC, 11);
        }
        final Font bodyFont = trebuchet;
        final Font italicFont = trebuchetItalic;
        final Font titleFont = trebuchet.deriveFont(Font.BOLD, 28f);
        final Font labelFont = trebuchet.deriveFont(Font.BOLD, 10f);
        final Font monoFont = new Font("Monospaced", Font.BOLD, 11);

        CardLayout cl = new CardLayout();
        JPanel showing = new JPanel(cl);
        showing.setOpaque(false);

        // actual login page
        JPanel loginPage = makeBackgroundPanel();
        JLabel loginEmoji = makeLabel("🍉", titleFont.deriveFont(Font.PLAIN, 38f), textMain);
        JLabel loginTitle = makeLabel("WATERMELON FIGHTERS", titleFont, ltGreen);
        JLabel loginSub   = makeLabel("Player Login", italicFont.deriveFont(14f), teal);

        JTextField  loginUser = makeField(bodyFont);
        JPasswordField loginPass = makePassField(bodyFont);

        JButton lBtn   = makeButton("▶  LOGIN",    accentRed, titleFont.deriveFont(Font.BOLD, 13f));
        JButton toRBtn = makeButton("  REGISTER",  borderCol, titleFont.deriveFont(Font.BOLD, 13f));

        lBtn.addActionListener(e -> {
            try (Scanner sc = Prompt.getInputScanner()) {
                if (sc != null) {
                    while (sc.hasNextLine()) {
                        String u = sc.nextLine();
                        String p = sc.hasNextLine() ? sc.nextLine() : "";
                        if (sc.hasNextLine()) sc.nextLine(); // score line
                        if (loginUser.getText().equals(u) &&
                            new String(loginPass.getPassword()).equals(p)) {
                            loggedIn = true;
                            user = loginUser.getText();
                            loginUser.setText(""); loginPass.setText("");
                            setVisible(false); dispose(); return;
                        }
                    }
                }
            }
            showRetroDialog(showing, "INVALID CREDENTIALS", errRed);
            loginUser.setText(""); loginPass.setText("");
        });

        toRBtn.addActionListener(e -> {
            loginUser.setText(""); loginPass.setText("");
            cl.show(showing, "register");
        });

        loginPage.add(Box.createVerticalStrut(36));
        loginPage.add(loginEmoji);
        loginPage.add(Box.createVerticalStrut(6));
        loginPage.add(loginTitle);
        loginPage.add(Box.createVerticalStrut(4));
        loginPage.add(loginSub);
        loginPage.add(Box.createVerticalStrut(6));
        loginPage.add(makeDivider(accentRed));
        loginPage.add(Box.createVerticalStrut(28));
        loginPage.add(makeFormBlock("USERNAME", loginUser, labelFont, bodyFont));
        loginPage.add(Box.createVerticalStrut(18));
        loginPage.add(makeFormBlock("PASSWORD", loginPass, labelFont, bodyFont));
        loginPage.add(Box.createVerticalStrut(32));
        loginPage.add(lBtn);
        loginPage.add(Box.createVerticalStrut(12));
        loginPage.add(toRBtn);
        loginPage.add(Box.createVerticalStrut(20));
        loginPage.add(makeFooterLabel("ESC closes • All rights reserved", italicFont, textSub));

        //register page
        JPanel regPage = makeBackgroundPanel();
        JLabel regEmoji = makeLabel("🍉", titleFont.deriveFont(Font.PLAIN, 38f), textMain);
        JLabel regTitle = makeLabel("CREATE ACCOUNT", titleFont, teal);
        JLabel regSub   = makeLabel("Join the Watermelon Fighters", italicFont.deriveFont(14f), ltGreen);

        JTextField   regUser = makeField(bodyFont);
        JPasswordField regPass = makePassField(bodyFont);

        JButton rBtn   = makeButton("▶  CREATE ACCOUNT", teal,       titleFont.deriveFont(Font.BOLD, 13f));
        JButton tolBtn = makeButton("  BACK TO LOGIN",   borderCol, titleFont.deriveFont(Font.BOLD, 13f));

        rBtn.addActionListener(e -> {
            String u = regUser.getText();
            String p = new String(regPass.getPassword());
            if (!u.isEmpty() && !p.isEmpty()) {
                try (PrintWriter fw = Prompt.getPrintWriter()) {
                    if (fw != null) {
                        fw.println(u); fw.println(p); fw.println(0);
                        showRetroDialog(showing, "ACCOUNT CREATED!", okGreen);
                        regUser.setText(""); regPass.setText("");
                        cl.show(showing, "login");
                    }
                } catch (Exception ex) { ex.printStackTrace(); }
            } else {
                showRetroDialog(showing, "FILL ALL FIELDS", gold);
            }
        });

        tolBtn.addActionListener(e -> {
            regUser.setText(""); regPass.setText("");
            cl.show(showing, "login");
        });

        regPage.add(Box.createVerticalStrut(36));
        regPage.add(regEmoji);
        regPage.add(Box.createVerticalStrut(6));
        regPage.add(regTitle);
        regPage.add(Box.createVerticalStrut(4));
        regPage.add(regSub);
        regPage.add(Box.createVerticalStrut(6));
        regPage.add(makeDivider(teal));
        regPage.add(Box.createVerticalStrut(28));
        regPage.add(makeFormBlock("USERNAME", regUser, labelFont, bodyFont));
        regPage.add(Box.createVerticalStrut(18));
        regPage.add(makeFormBlock("PASSWORD", regPass, labelFont, bodyFont));
        regPage.add(Box.createVerticalStrut(32));
        regPage.add(rBtn);
        regPage.add(Box.createVerticalStrut(12));
        regPage.add(tolBtn);
        regPage.add(Box.createVerticalStrut(20));
        regPage.add(makeFooterLabel("Passwords stored locally in Registration.txt", italicFont, textSub));

        showing.add(loginPage,  "login");
        showing.add(regPage, "register");


        JPanel root = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g0) {
                super.paintComponent(g0);
                Graphics2D g = (Graphics2D) g0;
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth(), h = getHeight();

                GradientPaint gp = new GradientPaint(w/2f, 0, new Color(0x12122A), w/2f, h, bgDark);
                g.setPaint(gp);
                g.fillRect(0, 0, w, h);

                // Faint grid (matches MapSelectPanel)
                g.setColor(new Color(255, 255, 255, 8));
                g.setStroke(new BasicStroke(1));
                for (int x = 0; x < w; x += 40) g.drawLine(x, 0, x, h);
                for (int y = 0; y < h; y += 40) g.drawLine(0, y, w, y);

                // Centered card glow
                int cw = 400, ch = 500;
                int cx = (w - cw) / 2, cy = (h - ch) / 2;
                g.setColor(new Color(0xE94560, true));
                for (int d = 18; d >= 1; d--) {
                    g.setColor(new Color(233, 69, 96, 6));
                    g.drawRoundRect(cx - d*2, cy - d*2, cw + d*4, ch + d*4, 20, 20);
                }
                // Card background
                g.setColor(bgPanel);
                g.fillRoundRect(cx, cy, cw, ch, 14, 14);
                // Card top accent bar
                g.setColor(accentRed);
                g.fillRoundRect(cx+1, cy+1, cw-2, 5, 14, 14);
                g.fillRect(cx+1, cy+4, cw-2, 5);
                // Card border
                g.setColor(borderCol);
                g.setStroke(new BasicStroke(1.5f));
                g.drawRoundRect(cx, cy, cw, ch, 14, 14);
            }
        };
        root.setBackground(bgDark);
        root.add(showing, BorderLayout.CENTER);
        add(root);
    }

    private JPanel makeBackgroundPanel() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setOpaque(false);
        return p;
    }

    private JLabel makeLabel(String text, Font f, Color c) {
        JLabel l = new JLabel(text, SwingConstants.CENTER);
        l.setFont(f);
        l.setForeground(c);
        l.setAlignmentX(Component.CENTER_ALIGNMENT);
        return l;
    }

    private JLabel makeFooterLabel(String text, Font f, Color c) {
        JLabel l = makeLabel(text, f, c);
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

    private JPanel makeFormBlock(String label, JComponent field, Font labelFont, Font bodyFont) {
        JPanel block = new JPanel();
        block.setLayout(new BoxLayout(block, BoxLayout.Y_AXIS));
        block.setOpaque(false);
        block.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel lbl = new JLabel(label);
        lbl.setFont(labelFont);
        lbl.setForeground(textSub);
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
        tf.setForeground(textMain);
        tf.setBackground(fieldBg);
        tf.setCaretColor(teal);
        tf.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(borderCol, 1),
            BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));
        tf.setMaximumSize(new Dimension(320, 42));
        // Highlight border on focus
        tf.addFocusListener(new FocusAdapter() {
            public void focusGained(FocusEvent e) {
                tf.setBorder(BorderFactory.createCompoundBorder(
                    new LineBorder(teal, 1),
                    BorderFactory.createEmptyBorder(8, 12, 8, 12)));
            }
            public void focusLost(FocusEvent e) {
                tf.setBorder(BorderFactory.createCompoundBorder(
                    new LineBorder(borderCol, 1),
                    BorderFactory.createEmptyBorder(8, 12, 8, 12)));
            }
        });
    }

    private JButton makeButton(String text, Color accentColor, Font f) {
        JButton btn = new JButton(text) {
            @Override protected void paintComponent(Graphics g0) {
                Graphics2D g = (Graphics2D) g0;
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (getModel().isPressed()) {
                    g.setColor(accentColor.darker());
                } else if (getModel().isRollover()) {
                    g.setColor(new Color(accentColor.getRed(), accentColor.getGreen(),
                                        accentColor.getBlue(), 40));
                } else {
                    g.setColor(bgDark);
                }
                g.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                // Border
                g.setColor(accentColor);
                g.setStroke(new BasicStroke(getModel().isRollover() ? 2f : 1.5f));
                g.drawRoundRect(1, 1, getWidth()-3, getHeight()-3, 8, 8);
                // Text
                g.setColor(getModel().isRollover() ? accentColor : textMain);
                g.setFont(getFont());
                FontMetrics fm = g.getFontMetrics();
                int tx = (getWidth() - fm.stringWidth(getText())) / 2;
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

    private void showRetroDialog(Component parent, String message, Color accent) {
        JDialog dlg = new JDialog();
        dlg.setUndecorated(true);
        dlg.setModal(true);
        dlg.setSize(340, 130);
        dlg.setLocationRelativeTo(parent);

        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(bgPanel);
        p.setBorder(new LineBorder(accent, 2));

        JLabel msg = new JLabel(message, SwingConstants.CENTER);
        msg.setFont(new Font("Trebuchet MS", Font.BOLD, 14));
        msg.setForeground(accent);
        p.add(msg, BorderLayout.CENTER);

        JButton ok = new JButton("OK");
        ok.setFont(new Font("Trebuchet MS", Font.BOLD, 12));
        ok.setForeground(textMain);
        ok.setBackground(bgMid);
        ok.setBorder(new LineBorder(accent, 1));
        ok.setFocusPainted(false);
        ok.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        ok.addActionListener(e -> dlg.dispose());

        JPanel btnP = new JPanel();
        btnP.setBackground(bgPanel);
        btnP.add(ok);
        p.add(btnP, BorderLayout.SOUTH);
        dlg.add(p);
        dlg.setVisible(true);
    }

    public boolean getLoggedIn() { return loggedIn; }
    public String getUser()      { return user;      }
}