import javax.swing.*;
import javax.swing.Timer;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

public class GamePanel extends JPanel implements ActionListener {

    // Game state
    private int lives = Constants.STARTING_LIVES;
    private int currency = Constants.STARTING_CURRENCY;
    private int score = 0;
    private boolean gameOver = false;
    private boolean victory = false;
    private boolean paused = false;

    // Entities
    private List<Tower> towers = new ArrayList<>();
    private List<Enemy> enemies = new ArrayList<>();
    private List<Projectile> projectiles = new ArrayList<>();
    private WaveManager waveManager = new WaveManager();

    // Path tiles set for quick lookup
    private Set<Long> pathTiles = new HashSet<>();

    // UI state
    private Tower.TType selectedTowerType = null;
    private Tower selectedTower = null; // for upgrade/sell panel
    private int hoverCol = -1, hoverRow = -1;

    // Tower type buttons in the sidebar
    private Rectangle[] towerButtons = new Rectangle[5];
    private Tower.TType[] towerTypes = {Tower.TType.ARCHER, Tower.TType.CANNON, Tower.TType.FROST, Tower.TType.LASER, Tower.TType.MORTAR};

    private Timer timer;

    // Sidebar colors
    private static final Color UI_BG       = new Color(0x1A1A2E);
    private static final Color UI_PANEL    = new Color(0x16213E);
    private static final Color UI_BORDER   = new Color(0x0F3460);
    private static final Color UI_ACCENT   = new Color(0xE94560);
    private static final Color UI_TEXT     = new Color(0xEEEEEE);
    private static final Color UI_SUBTEXT  = new Color(0xAAAAAA);

    public GamePanel() {
        setPreferredSize(new Dimension(Constants.TOTAL_WIDTH, Constants.GAME_HEIGHT));
        setBackground(Color.BLACK);
        buildPathSet();

        addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) { handleClick(e); }
        });
        addMouseMotionListener(new MouseMotionAdapter() {
            public void mouseMoved(MouseEvent e) { handleHover(e); }
        });
        addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ESCAPE) { selectedTowerType=null; deselectTower(); }
                if (e.getKeyCode() == KeyEvent.VK_P) { paused = !paused; }
            }
        });
        setFocusable(true);

        timer = new Timer(16, this);
        timer.start();
    }

    private void buildPathSet() {
        for (int[] pt : Constants.PATH) pathTiles.add((long)pt[0] * 1000 + pt[1]);
    }

    private boolean isPathTile(int col, int row) {
        return pathTiles.contains((long)col*1000+row);
    }

    private boolean hasTower(int col, int row) {
        for (Tower t : towers) if (t.getCol()==col && t.getRow()==row) return true;
        return false;
    }

    // -------- Game Loop --------
    @Override
    public void actionPerformed(ActionEvent e) {
        if (!paused && !gameOver && !victory) update();
        repaint();
    }

    private void update() {
        // Spawn enemies
        Enemy newEnemy = waveManager.update();
        if (newEnemy != null) enemies.add(newEnemy);

        // Update enemies
        for (Enemy en : enemies) en.update();

        // Check reached end
        Iterator<Enemy> ei = enemies.iterator();
        while (ei.hasNext()) {
            Enemy en = ei.next();
            if (en.hasReached()) {
                lives -= en.getDamage();
                ei.remove();
                if (lives <= 0) { lives = 0; gameOver = true; return; }
            } else if (en.isDead()) {
                currency += en.getReward();
                score    += en.getReward();
                ei.remove();
            }
        }

        // Towers fire
        List<Projectile> newProj = new ArrayList<>();
        for (Tower t : towers) {
            Projectile p = t.update(enemies, enemies);
            if (p != null) newProj.add(p);
        }
        projectiles.addAll(newProj);

        // Update projectiles
        Iterator<Projectile> pi = projectiles.iterator();
        while (pi.hasNext()) { Projectile p = pi.next(); p.update(); if (p.isDone()) pi.remove(); }

        // Wave complete check
        if (waveManager.isWaveActive() && waveManager.isAllSpawned() && enemies.isEmpty()) {
            waveManager.waveComplete();
            if (!waveManager.hasMoreWaves()) { victory = true; }
        }
    }

    // -------- Rendering --------
    @Override
    protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g = (Graphics2D) g0;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        drawMap(g);
        drawTowers(g);
        drawEnemies(g);
        drawProjectiles(g);
        if (selectedTowerType != null) drawPlacementPreview(g);
        drawUI(g);
        if (gameOver || victory) drawOverlay(g);
        if (paused && !gameOver && !victory) drawPause(g);
    }

    private void drawMap(Graphics2D g) {
        // Grass background
        for (int row=0; row<Constants.ROWS; row++) {
            for (int col=0; col<Constants.COLS; col++) {
                boolean isPath = isPathTile(col, row);
                if (isPath) {
                    g.setColor(new Color(0xC2A45A));
                } else {
                    // Checkerboard subtle
                    g.setColor((col+row)%2==0 ? new Color(0x2E7D32) : new Color(0x388E3C));
                }
                g.fillRect(col*Constants.TILE, row*Constants.TILE, Constants.TILE, Constants.TILE);
            }
        }

        // Path overlay lines
        g.setColor(new Color(0xA08040, true));
        g.setStroke(new BasicStroke(1));
        for (int[] pt : Constants.PATH) {
            int col=pt[0], row=pt[1];
            g.drawRect(col*Constants.TILE, row*Constants.TILE, Constants.TILE, Constants.TILE);
        }

        // Grid lines
        g.setColor(new Color(0,0,0,25));
        g.setStroke(new BasicStroke(0.5f));
        for (int col=0; col<=Constants.COLS; col++) g.drawLine(col*Constants.TILE,0,col*Constants.TILE,Constants.GAME_HEIGHT);
        for (int row=0; row<=Constants.ROWS; row++) g.drawLine(0,row*Constants.TILE,Constants.GAME_WIDTH,row*Constants.TILE);

        // Hover highlight
        if (hoverCol>=0 && hoverCol<Constants.COLS && hoverRow>=0 && hoverRow<Constants.ROWS && selectedTowerType!=null) {
            boolean canPlace = !isPathTile(hoverCol,hoverRow) && !hasTower(hoverCol,hoverRow);
            g.setColor(canPlace ? new Color(0,255,0,60) : new Color(255,0,0,60));
            g.fillRect(hoverCol*Constants.TILE, hoverRow*Constants.TILE, Constants.TILE, Constants.TILE);
        }

        // Start/end markers
        int[] sp = Constants.PATH[0], ep = Constants.PATH[Constants.PATH.length-1];
        g.setColor(new Color(0x1565C0));
        g.fillRoundRect(sp[0]*Constants.TILE+2, sp[1]*Constants.TILE+2, Constants.TILE-4, Constants.TILE-4, 8, 8);
        g.setColor(Color.WHITE);
        g.setFont(new Font("SansSerif", Font.BOLD, 9));
        g.drawString("IN", sp[0]*Constants.TILE+8, sp[1]*Constants.TILE+Constants.TILE/2+3);

        g.setColor(new Color(0xB71C1C));
        g.fillRoundRect(ep[0]*Constants.TILE+2, ep[1]*Constants.TILE+2, Constants.TILE-4, Constants.TILE-4, 8, 8);
        g.setColor(Color.WHITE);
        g.drawString("OUT", ep[0]*Constants.TILE+4, ep[1]*Constants.TILE+Constants.TILE/2+3);
    }

    private void drawTowers(Graphics2D g) {
        for (Tower t : towers) {
            if (t.isSelected()) t.drawRangePreview(g);
            t.draw(g);
        }
    }

    private void drawEnemies(Graphics2D g) {
        for (Enemy e : enemies) e.draw(g);
    }

    private void drawProjectiles(Graphics2D g) {
        for (Projectile p : projectiles) p.draw(g);
    }

    private void drawPlacementPreview(Graphics2D g) {
        if (hoverCol<0||hoverRow<0) return;
        // Temp tower for range preview
        Tower preview = new Tower(hoverCol, hoverRow, selectedTowerType);
        preview.drawRangePreview(g);
    }

    // -------- UI Sidebar --------
    private void drawUI(Graphics2D g) {
        int ux = Constants.GAME_WIDTH;
        int uw = Constants.UI_WIDTH;
        int uh = Constants.GAME_HEIGHT;

        // Background
        g.setColor(UI_BG);
        g.fillRect(ux, 0, uw, uh);
        g.setColor(UI_BORDER);
        g.fillRect(ux, 0, 3, uh);

        int y = 10;

        // Title
        g.setColor(UI_ACCENT);
        g.setFont(new Font("Georgia", Font.BOLD, 14));
        drawCentered(g, "TOWER DEFENSE", ux, uw, y); y+=18;

        // Wave info
        g.setColor(UI_BORDER);
        g.fillRoundRect(ux+6, y, uw-12, 1, 2, 2);
        y += 8;

        g.setColor(UI_TEXT);
        g.setFont(new Font("Monospaced", Font.BOLD, 12));
        String waveStr = "Wave " + waveManager.getCurrentWave() + " / " + waveManager.getTotalWaves();
        drawCentered(g, waveStr, ux, uw, y); y+=18;

        // Stats row
        g.setFont(new Font("SansSerif", Font.BOLD, 11));
        g.setColor(new Color(0xFF6B6B));
        g.drawString("♥ " + lives, ux+8, y);
        g.setColor(new Color(0xFFD700));
        String goldStr = "$ " + currency;
        g.drawString(goldStr, ux + uw - 8 - g.getFontMetrics().stringWidth(goldStr), y);
        y += 16;
        g.setColor(UI_SUBTEXT);
        g.setFont(new Font("SansSerif", Font.PLAIN, 10));
        String scoreStr = "Score: " + score;
        drawCentered(g, scoreStr, ux, uw, y); y += 18;

        // Wave control button
        g.setColor(UI_BORDER);
        g.fillRoundRect(ux+6, y, uw-12, 1, 2, 2); y+=6;

        boolean canStart = !waveManager.isWaveActive() && waveManager.hasMoreWaves() && !gameOver && !victory;
        Color btnColor = canStart ? new Color(0x1B5E20) : new Color(0x333355);
        drawButton(g, ux+8, y, uw-16, 26, btnColor, canStart ? "► Start Wave" : waveManager.isWaveActive() ? "Wave In Progress" : "No More Waves", canStart ? UI_TEXT : UI_SUBTEXT);
        waveButtonBounds = new Rectangle(ux+8, y, uw-16, 26);
        y += 34;

        // Separator
        g.setColor(UI_BORDER);
        g.fillRoundRect(ux+6, y, uw-12, 1, 2, 2); y+=6;
        g.setColor(UI_SUBTEXT);
        g.setFont(new Font("SansSerif", Font.BOLD, 10));
        drawCentered(g, "PLACE TOWER", ux, uw, y); y+=14;

        // Tower buttons
        String[] names = {"Archer","Cannon","Frost","Laser","Mortar"};
        int[] costs     = {80,150,120,200,175};
        Color[] colors  = {new Color(0x8B4513),new Color(0x555555),new Color(0x00BFFF),new Color(0xFF2222),new Color(0x8B6914)};

        for (int i=0; i<5; i++) {
            boolean sel = selectedTowerType == towerTypes[i];
            boolean canAfford = currency >= costs[i];
            Color bg = sel ? new Color(0x1A3A5C) : canAfford ? new Color(0x0D2137) : new Color(0x1A1A1A);
            Color border = sel ? UI_ACCENT : canAfford ? UI_BORDER : new Color(0x333333);
            Color fg = canAfford ? UI_TEXT : UI_SUBTEXT;

            int bx=ux+6, by=y, bw=uw-12, bh=28;
            g.setColor(bg);
            g.fillRoundRect(bx, by, bw, bh, 6, 6);
            g.setColor(border);
            g.setStroke(new BasicStroke(sel?2:1));
            g.drawRoundRect(bx, by, bw, bh, 6, 6);
            g.setStroke(new BasicStroke(1));

            // Color dot
            g.setColor(colors[i]);
            g.fillOval(bx+6, by+bh/2-5, 10, 10);

            g.setColor(fg);
            g.setFont(new Font("SansSerif", Font.BOLD, 10));
            g.drawString(names[i], bx+22, by+bh/2+4);

            g.setColor(new Color(0xFFD700));
            g.setFont(new Font("Monospaced", Font.PLAIN, 10));
            String cs = "$"+costs[i];
            g.drawString(cs, bx+bw-g.getFontMetrics().stringWidth(cs)-6, by+bh/2+4);

            towerButtons[i] = new Rectangle(bx, by, bw, bh);
            y += 32;
        }

        // Selected tower info / upgrade panel
        y += 4;
        g.setColor(UI_BORDER);
        g.fillRoundRect(ux+6, y, uw-12, 1, 2, 2); y+=6;

        if (selectedTower != null) {
            drawTowerInfo(g, ux, uw, y);
        } else {
            g.setColor(UI_SUBTEXT);
            g.setFont(new Font("SansSerif", Font.ITALIC, 10));
            drawCentered(g, "Click tower to upgrade", ux, uw, y); y+=14;
            drawCentered(g, "ESC to deselect", ux, uw, y);
        }

        // Bottom hint
        g.setColor(new Color(0x555577));
        g.setFont(new Font("SansSerif", Font.PLAIN, 9));
        drawCentered(g, "P = Pause | ESC = Cancel", ux, uw, uh-12);
    }

    private Rectangle waveButtonBounds = new Rectangle();
    private Rectangle upgradeButtonBounds = new Rectangle(0,0,0,0);
    private Rectangle sellButtonBounds = new Rectangle(0,0,0,0);

    private void drawTowerInfo(Graphics2D g, int ux, int uw, int y) {
        Tower t = selectedTower;
        g.setColor(UI_ACCENT);
        g.setFont(new Font("Georgia", Font.BOLD, 12));
        drawCentered(g, t.getName() + " (Lv." + t.getLevel() + ")", ux, uw, y); y+=16;

        g.setColor(UI_TEXT);
        g.setFont(new Font("Monospaced", Font.PLAIN, 10));
        g.drawString("DMG: " + String.format("%.0f", t.getDamage()), ux+8, y); y+=13;
        g.drawString("RNG: " + String.format("%.1f", t.getRange()), ux+8, y); y+=13;
        g.drawString("SPD: " + String.format("%.0f", t.getFireRate()), ux+8, y); y+=16;

        // Stars
        for (int i=0; i<3; i++) {
            g.setColor(i < t.getLevel() ? new Color(0xFFD700) : new Color(0x444444));
            int sx = ux + 8 + i*16;
            g.fillPolygon(starPoly(sx+6, y-2, 6, 4, 5));
        }
        y += 18;

        // Upgrade button
        if (t.canUpgrade()) {
            boolean canAff = currency >= t.upgradeCost();
            Color ub = canAff ? new Color(0x1B5E20) : new Color(0x2A1A1A);
            drawButton(g, ux+8, y, uw-16, 24, ub, "↑ Upgrade $" + t.upgradeCost(), canAff ? new Color(0x80FF80) : UI_SUBTEXT);
            upgradeButtonBounds = new Rectangle(ux+8, y, uw-16, 24);
        } else {
            g.setColor(new Color(0xFFD700));
            g.setFont(new Font("SansSerif", Font.BOLD, 10));
            drawCentered(g, "★ MAX LEVEL ★", ux, uw, y+12);
            upgradeButtonBounds = new Rectangle(0,0,0,0);
        }
        y += 30;

        // Sell button
        drawButton(g, ux+8, y, uw-16, 24, new Color(0x4A1010), "✕ Sell $" + t.sellValue(), new Color(0xFF8080));
        sellButtonBounds = new Rectangle(ux+8, y, uw-16, 24);
    }

    private static Polygon starPoly(int cx, int cy, int outer, int inner, int points) {
        int[] xs = new int[points*2], ys = new int[points*2];
        for (int i=0; i<points*2; i++) {
            double angle = Math.PI/points*i - Math.PI/2;
            int r = (i%2==0) ? outer : inner;
            xs[i] = (int)(cx + r*Math.cos(angle));
            ys[i] = (int)(cy + r*Math.sin(angle));
        }
        return new Polygon(xs,ys,points*2);
    }

    private void drawButton(Graphics2D g, int x, int y, int w, int h, Color bg, String label, Color fg) {
        g.setColor(bg);
        g.fillRoundRect(x, y, w, h, 6, 6);
        g.setColor(bg.brighter());
        g.setStroke(new BasicStroke(1));
        g.drawRoundRect(x, y, w, h, 6, 6);
        g.setStroke(new BasicStroke(1));
        g.setColor(fg);
        g.setFont(new Font("SansSerif", Font.BOLD, 10));
        FontMetrics fm = g.getFontMetrics();
        g.drawString(label, x + (w - fm.stringWidth(label))/2, y + h/2 + 4);
    }

    private void drawCentered(Graphics2D g, String s, int ux, int uw, int y) {
        FontMetrics fm = g.getFontMetrics();
        g.drawString(s, ux + (uw - fm.stringWidth(s))/2, y);
    }

    private void drawOverlay(Graphics2D g) {
        g.setColor(new Color(0,0,0,160));
        g.fillRect(0, 0, Constants.TOTAL_WIDTH, Constants.GAME_HEIGHT);
        g.setFont(new Font("Georgia", Font.BOLD, 48));
        g.setColor(victory ? new Color(0xFFD700) : new Color(0xFF4444));
        String msg = victory ? "VICTORY!" : "GAME OVER";
        FontMetrics fm = g.getFontMetrics();
        int mx = (Constants.TOTAL_WIDTH - fm.stringWidth(msg))/2;
        g.drawString(msg, mx, Constants.GAME_HEIGHT/2 - 20);
        g.setFont(new Font("SansSerif", Font.PLAIN, 18));
        g.setColor(Color.WHITE);
        String sub = "Score: " + score + " | Wave: " + waveManager.getCurrentWave();
        fm = g.getFontMetrics();
        g.drawString(sub, (Constants.TOTAL_WIDTH - fm.stringWidth(sub))/2, Constants.GAME_HEIGHT/2+20);
        String hint = "Press F5 / restart to play again";
        g.setFont(new Font("SansSerif", Font.ITALIC, 13));
        g.setColor(UI_SUBTEXT);
        fm = g.getFontMetrics();
        g.drawString(hint, (Constants.TOTAL_WIDTH - fm.stringWidth(hint))/2, Constants.GAME_HEIGHT/2+50);
    }

    private void drawPause(Graphics2D g) {
        g.setColor(new Color(0,0,0,120));
        g.fillRect(0, 0, Constants.TOTAL_WIDTH, Constants.GAME_HEIGHT);
        g.setColor(UI_TEXT);
        g.setFont(new Font("Georgia", Font.BOLD, 36));
        FontMetrics fm = g.getFontMetrics();
        String msg = "PAUSED";
        g.drawString(msg, (Constants.TOTAL_WIDTH - fm.stringWidth(msg))/2, Constants.GAME_HEIGHT/2);
    }

    // -------- Input Handling --------
    private void handleHover(MouseEvent e) {
        hoverCol = e.getX() / Constants.TILE;
        hoverRow = e.getY() / Constants.TILE;
    }

    private void handleClick(MouseEvent e) {
        int mx = e.getX(), my = e.getY();

        // UI area clicks
        if (mx >= Constants.GAME_WIDTH) {
            // Wave button
            if (waveButtonBounds.contains(mx, my)) {
                if (!waveManager.isWaveActive() && waveManager.hasMoreWaves() && !gameOver && !victory) {
                    waveManager.startNextWave();
                }
                return;
            }
            // Tower buttons
            for (int i=0; i<towerButtons.length; i++) {
                if (towerButtons[i] != null && towerButtons[i].contains(mx,my)) {
                    int cost = new int[]{80,150,120,200,175}[i];
                    if (currency >= cost) {
                        selectedTowerType = towerTypes[i];
                        deselectTower();
                    }
                    return;
                }
            }
            // Upgrade button
            if (selectedTower != null && upgradeButtonBounds.contains(mx,my)) {
                if (selectedTower.canUpgrade() && currency >= selectedTower.upgradeCost()) {
                    currency -= selectedTower.upgradeCost();
                    selectedTower.upgrade();
                }
                return;
            }
            // Sell button
            if (selectedTower != null && sellButtonBounds.contains(mx,my)) {
                currency += selectedTower.sellValue();
                towers.remove(selectedTower);
                selectedTower = null;
                return;
            }
            return;
        }

        // Game area clicks
        int col = mx / Constants.TILE, row = my / Constants.TILE;
        if (selectedTowerType != null) {
            // Place tower
            if (!isPathTile(col,row) && !hasTower(col,row)) {
                int cost = costOf(selectedTowerType);
                if (currency >= cost) {
                    currency -= cost;
                    Tower t = new Tower(col, row, selectedTowerType);
                    towers.add(t);
                }
            }
        } else {
            // Select tower
            deselectTower();
            for (Tower t : towers) {
                if (t.getCol()==col && t.getRow()==row) {
                    t.setSelected(true);
                    selectedTower = t;
                    break;
                }
            }
        }
    }

    private void deselectTower() {
        if (selectedTower != null) { selectedTower.setSelected(false); selectedTower = null; }
    }

    private int costOf(Tower.TType t) {
        switch(t) {
            case ARCHER: return 80;
            case CANNON: return 150;
            case FROST:  return 120;
            case LASER:  return 200;
            case MORTAR: return 175;
            default: return 999;
        }
    }
}