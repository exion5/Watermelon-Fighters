import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;

public class MapSelectPanel extends JPanel {

    private int hoveredIndex = -1;
    private final JFrame parentFrame;
    private final String username;

    // Card layout constants
    private static final int CARD_W  = 180;
    private static final int CARD_H  = 220;
    private static final int CARD_GAP = 22;
    private static final int PREVIEW_H = 110; // mini-map preview height inside card

    private java.awt.Rectangle backButtonBounds = new java.awt.Rectangle();

    public MapSelectPanel(JFrame parentFrame, String username) {
        this.parentFrame = parentFrame;
        this.username = username;
        setPreferredSize(new Dimension(Constants.TOTAL_WIDTH, Constants.GAME_HEIGHT));
        setBackground(new Color(0x0A0A18));

        addMouseMotionListener(new MouseMotionAdapter() {
            public void mouseMoved(MouseEvent e) {
                int prev = hoveredIndex;
                hoveredIndex = cardIndexAt(e.getX(), e.getY());
                if (hoveredIndex != prev) repaint();
            }
        });
        addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (backButtonBounds.contains(e.getX(), e.getY())) {
                    Main.onBackToMain();
                    return;
                }
                int idx = cardIndexAt(e.getX(), e.getY());
                if (idx >= 0) launchGame(idx);
            }
        });
    }

    // rendering
    @Override
    protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g = (Graphics2D) g0;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        drawBackground(g);
        drawTitle(g);
        drawCards(g);
        drawFooter(g);
        drawBackButton(g);
    }

    private void drawBackground(Graphics2D g) {
        // Subtle radial gradient
        int w = getWidth(), h = getHeight();
        GradientPaint gp = new GradientPaint(w/2f, 0, new Color(0x12122A), w/2f, h, new Color(0x050510));
        g.setPaint(gp);
        g.fillRect(0, 0, w, h);

        // Faint grid
        g.setColor(new Color(255,255,255,8));
        g.setStroke(new BasicStroke(1));
        for (int x=0; x<w; x+=40) g.drawLine(x,0,x,h);
        for (int y=0; y<h; y+=40) g.drawLine(0,y,w,y);
    }

    private void drawTitle(Graphics2D g) {
        g.setFont(new Font("Georgia", Font.BOLD, 36));
        String title = "\uD83C\uDF49 Watermelon Fighters";
        FontMetrics fm = g.getFontMetrics();
        int tx = (getWidth() - fm.stringWidth(title)) / 2;

        // Glow
        g.setColor(new Color(0x4CAF50, true));
        for (int d=4; d>=1; d--) {
            g.setColor(new Color(76,175,80, 30/d));
            g.drawString(title, tx+d, 56+d);
            g.drawString(title, tx-d, 56-d);
        }
        g.setColor(new Color(0xC8E6C9));
        g.drawString(title, tx, 56);

        g.setFont(new Font("SansSerif", Font.PLAIN, 14));
        g.setColor(new Color(0x80CBC4));
        String sub = "Select a Map to Begin";
        fm = g.getFontMetrics();
        g.drawString(sub, (getWidth() - fm.stringWidth(sub))/2, 80);
    }

    private void drawCards(Graphics2D g) {
        int totalW = MapData.ALL.length * CARD_W + (MapData.ALL.length-1)*CARD_GAP;
        int startX = (getWidth() - totalW) / 2;
        int startY = 105;

        for (int i=0; i<MapData.ALL.length; i++) {
            int cx = startX + i*(CARD_W+CARD_GAP);
            drawCard(g, i, cx, startY);
        }
    }

    private void drawCard(Graphics2D g, int index, int x, int y) {
        MapData map = MapData.ALL[index];
        boolean hovered = index == hoveredIndex;

        // Card shadow
        g.setColor(new Color(0,0,0, hovered ? 120 : 70));
        g.fillRoundRect(x+4, y+6, CARD_W, CARD_H, 14, 14);

        // Card background
        Color cardBg = hovered ? new Color(0x1A2A3A) : new Color(0x111827);
        g.setColor(cardBg);
        g.fillRoundRect(x, y, CARD_W, CARD_H, 12, 12);

        // Border
        g.setColor(hovered ? map.accentColor : new Color(0x2A3A4A));
        g.setStroke(new BasicStroke(hovered ? 2.5f : 1.5f));
        g.drawRoundRect(x, y, CARD_W, CARD_H, 12, 12);
        g.setStroke(new BasicStroke(1));

        // Accent top bar
        g.setColor(map.accentColor);
        g.fillRoundRect(x+1, y+1, CARD_W-2, 5, 12, 12);
        g.fillRect(x+1, y+4, CARD_W-2, 5);  // fill the lower part of round corners

        // Mini-map preview
        drawMiniMap(g, map, x+8, y+14, CARD_W-16, PREVIEW_H);

        // Map number badge
        g.setColor(map.accentColor);
        g.fillOval(x+CARD_W-26, y+10, 20, 20);
        g.setColor(Color.WHITE);
        g.setFont(new Font("Georgia", Font.BOLD, 11));
        FontMetrics fm = g.getFontMetrics();
        String num = String.valueOf(index+1);
        g.drawString(num, x+CARD_W-26+(20-fm.stringWidth(num))/2, y+24);

        // Map name
        g.setColor(hovered ? Color.WHITE : new Color(0xCFD8DC));
        g.setFont(new Font("Georgia", Font.BOLD, 13));
        fm = g.getFontMetrics();
        String name = map.name;
        g.drawString(name, x + (CARD_W-fm.stringWidth(name))/2, y+PREVIEW_H+28);

        // Description (word-wrapped into 2 lines max)
        g.setColor(new Color(0x90A4AE));
        g.setFont(new Font("SansSerif", Font.PLAIN, 10));
        drawWrapped(g, map.description, x+8, y+PREVIEW_H+44, CARD_W-16, 14);

        // Best wave for this map
        int best = readBestWave(username, index);
        int score = readBestScore(username, index);
        String bestStr = best == 0 ? "Best: --" + "       Score : " + score : (best >= 15 ? "★ Completed!" + "       Score : " + score : "Best: Wave " + best + "       Score : " + score);
        Color bestCol = best >= 15 ? new Color(0xFFD700) : (best > 0 ? new Color(0x80CBC4) : new Color(0x445566));
        g.setColor(bestCol);
        g.setFont(new Font("SansSerif", Font.BOLD, 9));
        FontMetrics bfm = g.getFontMetrics();
        g.drawString(bestStr, x + (CARD_W - bfm.stringWidth(bestStr)) / 2, y + PREVIEW_H + 60);

        // "Play" button at bottom
        Color btnBg = hovered ? map.accentColor : new Color(0x1E3A2A);
        Color btnFg = hovered ? Color.WHITE : new Color(0x80CBC4);
        g.setColor(btnBg);
        g.fillRoundRect(x+20, y+CARD_H-36, CARD_W-40, 26, 8, 8);
        g.setColor(btnFg);
        g.setFont(new Font("SansSerif", Font.BOLD, 11));
        fm = g.getFontMetrics();
        String play = hovered ? "▶  Play!" : "▶  Select";
        g.drawString(play, x+20+(CARD_W-40-fm.stringWidth(play))/2, y+CARD_H-36+17);
    }

    // draws the small preview of the map
    private void drawMiniMap(Graphics2D g, MapData map, int ox, int oy, int w, int h) {
        // Background (grass)
        g.setColor(map.grassA);
        g.fillRoundRect(ox, oy, w, h, 6, 6);

        // Clip to preview area
        Shape oldClip = g.getClip();
        g.setClip(new Rectangle(ox, oy, w, h));

        // Draw grass checkerboard at mini scale
        float scaleX = (float)w  / Constants.COLS;
        float scaleY = (float)h  / Constants.ROWS;
        for (int row=0; row<Constants.ROWS; row++) {
            for (int col=0; col<Constants.COLS; col++) {
                g.setColor((col+row)%2==0 ? map.grassA : map.grassB);
                g.fillRect(ox+(int)(col*scaleX), oy+(int)(row*scaleY),
                           (int)scaleX+1, (int)scaleY+1);
            }
        }

        // Draw path as thick line
        int[][] path = map.path;
        int[] pxPrev = null;
        for (int[] pt : path) {
            int px = ox + (int)((pt[0]+0.5f)*scaleX);
            int py = oy + (int)((pt[1]+0.5f)*scaleY);
            g.setColor(map.pathColor);
            g.fillRect(ox+(int)(pt[0]*scaleX), oy+(int)(pt[1]*scaleY),
                       Math.max(2,(int)scaleX), Math.max(2,(int)scaleY));
            if (pxPrev != null) {
                g.setStroke(new BasicStroke(Math.max(2,(int)scaleX)));
                g.setColor(map.pathColor.darker());
                g.drawLine(pxPrev[0], pxPrev[1], px, py);
                g.setStroke(new BasicStroke(1));
            }
            pxPrev = new int[]{px, py};
        }

        // Start / end dots
        if (path.length > 0) {
            int[] sp = path[0], ep = path[path.length-1];
            g.setColor(new Color(0x1565C0));
            g.fillOval(ox+(int)((sp[0]+0.1f)*scaleX), oy+(int)((sp[1]+0.1f)*scaleY), (int)(scaleX*0.8f)+2, (int)(scaleY*0.8f)+2);
            g.setColor(new Color(0xB71C1C));
            g.fillOval(ox+(int)((ep[0]+0.1f)*scaleX), oy+(int)((ep[1]+0.1f)*scaleY), (int)(scaleX*0.8f)+2, (int)(scaleY*0.8f)+2);
        }

        g.setClip(oldClip);

        // Border
        g.setColor(new Color(0,0,0,80));
        g.drawRoundRect(ox, oy, w, h, 6, 6);
    }

    // Very simple word-wrap for card description text
    private void drawWrapped(Graphics2D g, String text, int x, int y, int maxW, int lineH) {
        FontMetrics fm = g.getFontMetrics();
        StringBuilder line = new StringBuilder();
        int lineCount = 0;
        for (String word : text.split(" ")) {
            String test = line.length()==0 ? word : line+" "+word;
            if (fm.stringWidth(test) > maxW && line.length()>0) {
                g.drawString(line.toString(), x, y + lineCount*lineH);
                line = new StringBuilder(word);
                lineCount++;
                if (lineCount >= 2) break;
            } else {
                line = new StringBuilder(test);
            }
        }
        if (lineCount < 2 && line.length()>0)
            g.drawString(line.toString(), x, y + lineCount*lineH);
    }

    private void drawFooter(Graphics2D g) {
        g.setColor(new Color(0x37474F));
        g.setFont(new Font("SansSerif", Font.PLAIN, 11));
        String hint = "Hover a card to preview  •  Click to play";
        FontMetrics fm = g.getFontMetrics();
        g.drawString(hint, (getWidth()-fm.stringWidth(hint))/2, getHeight()-18);
    }

    private int cardIndexAt(int mx, int my) {
        int totalW = MapData.ALL.length * CARD_W + (MapData.ALL.length-1)*CARD_GAP;
        int startX = (getWidth() - totalW) / 2;
        int startY = 105;
        for (int i=0; i<MapData.ALL.length; i++) {
            int cx = startX + i*(CARD_W+CARD_GAP);
            if (mx>=cx && mx<=cx+CARD_W && my>=startY && my<=startY+CARD_H) return i;
        }
        return -1;
    }

    private int readBestWave(String user, int mapIndex) {
        try {
            java.util.List<String> lines = java.nio.file.Files.readAllLines(java.nio.file.Paths.get("Registration.txt"));
            for (int i = 0; i + 11 < lines.size(); i += 12) {
                if (lines.get(i).trim().equals(user))
                    return Integer.parseInt(lines.get(i + 2 + mapIndex).trim());
            }
        } catch (Exception ignored) {}
        return 0;
    }

    private int readBestScore(String user, int mapIndex) {
        try {
            java.util.List<String> lines = java.nio.file.Files.readAllLines(java.nio.file.Paths.get("Registration.txt"));
            for (int i = 0; i + 11 < lines.size(); i += 12) {
                if (lines.get(i).trim().equals(user))
                    return Integer.parseInt(lines.get(i + 7 + mapIndex).trim());
            }
        } catch (Exception ignored) {}
        return 0;
    }

    private void drawBackButton(Graphics2D g) {
        int btnW = 140, btnH = 30;
        int btnX = 16;
        int btnY = getHeight() - btnH - 16;

        g.setColor(new Color(0x1A1A2E));
        g.fillRoundRect(btnX, btnY, btnW, btnH, 8, 8);
        g.setColor(new Color(0x3A5068));
        g.setStroke(new BasicStroke(1.5f));
        g.drawRoundRect(btnX, btnY, btnW, btnH, 8, 8);
        g.setStroke(new BasicStroke(1));

        g.setColor(new Color(0x90A4AE));
        g.setFont(new Font("SansSerif", Font.BOLD, 11));
        FontMetrics fm = g.getFontMetrics();
        String label = "← Main Menu";
        g.drawString(label, btnX + (btnW - fm.stringWidth(label)) / 2, btnY + btnH / 2 + 4);

        backButtonBounds = new java.awt.Rectangle(btnX, btnY, btnW, btnH);
    }

    private void launchGame(int mapIndex) {
        Main.onMapSelected(MapData.ALL[mapIndex]);
    }
}