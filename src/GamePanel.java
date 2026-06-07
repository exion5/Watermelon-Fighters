import javax.swing.*;
import javax.swing.Timer;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.*;
import java.util.List;

public class GamePanel extends JPanel implements ActionListener {

    private int lives     = Constants.STARTING_LIVES; // main game state
    private int currency  = Constants.STARTING_CURRENCY;
    private int score     = 0;
    private boolean gameOver = false, victory = false, paused = false;
    private int gameSpeed = 1;
    private boolean autoStart = false;

    private List<Tower>     towers     = new ArrayList<>(); // arrays of the entities in the game
    private List<Enemy>     enemies    = new ArrayList<>();
    private List<Projectile>projectiles= new ArrayList<>();
    private WaveManager waveManager    = new WaveManager();

    private Set<Long> pathTiles = new HashSet<>();

    private Tower.TType selectedTowerType = null; // ui state for tower placement
    private Tower selectedTower = null;
    private int hoverCol = -1, hoverRow = -1;
    private Rectangle[] towerButtons = new Rectangle[5];
    private Tower.TType[] towerTypes = {
        Tower.TType.DART, Tower.TType.BOMB, Tower.TType.ICE,
        Tower.TType.SUPER, Tower.TType.MORTAR
    };

    private Timer timer;
    private MapData currentMap;

    private int[][] decorations;
    private int[][] grassNoise;

    private static class FloatText { // currency popups
        float x, y, vy, life, maxLife;
        String text; Color color;
        FloatText(float x, float y, String t, Color c) {
            this.x=x; this.y=y; text=t; color=c;
            vy=-0.8f; life=50; maxLife=50;
        }
    }
    private List<FloatText> floatTexts = new ArrayList<>();

    private static final Color UI_BG      = new Color(0x0E0E1C); // sidebar palette
    private static final Color UI_CARD    = new Color(0x161628);
    private static final Color UI_BORDER  = new Color(0x1E3A5F);
    private static final Color UI_ACCENT  = new Color(0xE94560);
    private static final Color UI_TEXT    = new Color(0xEEEEEE);
    private static final Color UI_SUBTEXT = new Color(0x7799BB);
    private static final Color UI_GOLD    = new Color(0xFFD700);
    private static final Color COL_A      = new Color(0xFF7043);
    private static final Color COL_B      = new Color(0x29B6F6);
    private static final Color COL_LOCKED = new Color(0x2E2E3E);

    public GamePanel()            { this(MapData.ALL[0]); }
    public GamePanel(MapData map) {
        this.currentMap = map;
        Enemy.activePath = map.path;
        setPreferredSize(new Dimension(Constants.TOTAL_WIDTH, Constants.GAME_HEIGHT));
        setBackground(Color.BLACK);
        buildPathSet();
        buildDecorations();
        buildGrassNoise();

        addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) { 
                handleClick(e); 
            }
        });
        addMouseMotionListener(new MouseMotionAdapter() {
            public void mouseMoved(MouseEvent e) { 
                handleHover(e); 
            }
        });
        addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode()==KeyEvent.VK_ESCAPE) { 
                    selectedTowerType=null; 
                    deselectTower(); 
                }
                if (e.getKeyCode()==KeyEvent.VK_P) paused=!paused;
            }
        });
        setFocusable(true);
        timer = new Timer(16, this);
        timer.start();
    }

    private void buildPathSet() {
        for (int[] pt : currentMap.path) pathTiles.add((long)pt[0]*1000+pt[1]);
    }
    private boolean isPathTile(int c, int r) { return pathTiles.contains((long)c*1000+r); }
    private boolean hasTower(int c, int r) {
        for (Tower t : towers) if (t.getCol()==c && t.getRow()==r) return true;
        return false;
    }

    private void buildDecorations() {
        Random rng = new Random(currentMap.name.hashCode()); // deterministic per map
        List<int[]> list = new ArrayList<>();
        for (int row=0; row<Constants.ROWS; row++) {
            for (int col=0; col<Constants.COLS; col++) {
                if (isPathTile(col,row)) continue;
                if (rng.nextFloat() < 0.22f) { // 22% chance of decoration on non-path tile
                    int type = rng.nextInt(3); // 0=tree,1=rock,2=flower
                    int variant = rng.nextInt(3);
                    list.add(new int[]{col, row, type, variant});
                }
            }
        }
        decorations = list.toArray(new int[0][]);
    }

    private void buildGrassNoise() {
        grassNoise = new int[Constants.COLS][Constants.ROWS];
        Random rng = new Random(currentMap.name.hashCode() * 31L);
        for (int c=0;c<Constants.COLS;c++)
            for (int r=0;r<Constants.ROWS;r++)
                grassNoise[c][r] = rng.nextInt(18) - 9; // -9..+8
    }

    @Override public void actionPerformed(ActionEvent e) { // game loop
        if (!paused && !gameOver && !victory) {
            for (int s=0; s<gameSpeed; s++) update();
            if (autoStart && !waveManager.isWaveActive() && waveManager.hasMoreWaves() && !gameOver && !victory) {
                selectedTowerType=null; deselectTower(); waveManager.startNextWave();
            }
        }
        floatTexts.removeIf(ft -> { ft.y+=ft.vy; ft.life--; return ft.life<=0; });
        repaint();
    }

    private void update() {
        Enemy newEnemy = waveManager.update();
        if (newEnemy!=null) enemies.add(newEnemy);

        for (Enemy en : enemies) en.update();

        Iterator<Enemy> ei = enemies.iterator();
        while (ei.hasNext()) {
            Enemy en = ei.next();
            if (en.hasReached()) {
                lives -= en.getDamage(); ei.remove();
                if (lives<=0) { lives=0; gameOver=true; return; }
            } else if (en.isDead()) {
                currency += en.getReward(); score += en.getReward();
                floatTexts.add(new FloatText(en.getX(), en.getY()-10, "+$"+en.getReward(), UI_GOLD));
                ei.remove();
            }
        }

        List<Projectile> newProj = new ArrayList<>();
        for (Tower t : towers) newProj.addAll(t.updateMulti(enemies, enemies));
        projectiles.addAll(newProj);

        Iterator<Projectile> pi = projectiles.iterator();
        while (pi.hasNext()) { Projectile p = pi.next(); p.update(); if (p.isDone()) pi.remove(); }

        if (waveManager.isWaveActive() && waveManager.isAllSpawned() && enemies.isEmpty()) {
            int bonus = waveManager.waveClearBonus();
            currency += bonus; score += bonus / 2;
            floatTexts.add(new FloatText(Constants.GAME_WIDTH/2f, Constants.GAME_HEIGHT/2f - 30,
                "Wave Clear!  +$"+bonus, new Color(0x88FF88)));
            waveManager.waveComplete();
            if (!waveManager.hasMoreWaves()) victory=true;
        }
    }

    @Override protected void paintComponent(Graphics g0) { // rendering code - draws the entire game scene each frame
        super.paintComponent(g0);
        Graphics2D g = (Graphics2D) g0;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,      RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING,         RenderingHints.VALUE_RENDER_QUALITY);

        g.setColor(new Color(0x050508));
        g.fillRect(0, 0, Constants.GAME_WIDTH, Constants.GAME_HEIGHT);

        drawMap(g);
        g.translate(Constants.MAP_PAD, Constants.MAP_PAD);
        drawTowers(g);
        drawEnemies(g);
        drawProjectiles(g);
        if (!waveManager.isWaveActive() && selectedTowerType!=null) drawPlacementPreview(g);
        drawFloatTexts(g);
        g.translate(-Constants.MAP_PAD, -Constants.MAP_PAD);
        drawGameBorder(g);
        drawUI(g);
        if (gameOver||victory) drawOverlay(g);
        if (paused&&!gameOver&&!victory) drawPause(g);
    }

    private void drawMap(Graphics2D g) { // map drawing
        int p = Constants.MAP_PAD;

        for (int row=0; row<Constants.ROWS; row++) { // draw each tile with path vs grass logic
            for (int col=0; col<Constants.COLS; col++) {
                int tx = p + col*Constants.TILE, ty = p + row*Constants.TILE;
                boolean isPath = isPathTile(col, row);

                if (isPath) {
                    g.setColor(currentMap.pathColor);
                    g.fillRect(tx, ty, Constants.TILE, Constants.TILE);
                    g.setColor(new Color(0,0,0, 18));
                    for (int stripe=2; stripe<Constants.TILE; stripe+=6) g.drawLine(tx+stripe, ty, tx+stripe, ty+Constants.TILE);
                    drawPathEdgeShading(g, col, row, tx, ty);

                } else {
                    int noise = grassNoise[col][row];
                    Color base = (col+row)%2==0 ? currentMap.grassA : currentMap.grassB;
                    int nr = clamp(base.getRed()  +noise);
                    int ng = clamp(base.getGreen()+noise);
                    int nb = clamp(base.getBlue() +noise/2);
                    g.setColor(new Color(nr, ng, nb));
                    g.fillRect(tx, ty, Constants.TILE, Constants.TILE);
                    g.setColor(new Color(0,0,0,14));
                    g.drawRect(tx, ty, Constants.TILE-1, Constants.TILE-1);
                }
            }
        }

        for (int[] dec : decorations) {
            int col=dec[0], row=dec[1], dtype=dec[2], variant=dec[3];
            if (hasTower(col,row)) continue;
            int tx = p + col*Constants.TILE, ty = p + row*Constants.TILE;
            switch (dtype) {
                case 0: drawTree(g, tx, ty, variant); break;
                case 1: drawRock(g, tx, ty, variant); break;
                case 2: drawFlower(g, tx, ty, variant); break;
            }
        }

        g.setColor(new Color(0,0,0,20));
        g.setStroke(new BasicStroke(0.5f));
        for (int col=0; col<=Constants.COLS; col++)
            g.drawLine(p+col*Constants.TILE, p, p+col*Constants.TILE, p+Constants.ROWS*Constants.TILE);
        for (int row=0; row<=Constants.ROWS; row++)
            g.drawLine(p, p+row*Constants.TILE, p+Constants.COLS*Constants.TILE, p+row*Constants.TILE);

        if (!waveManager.isWaveActive() && hoverCol>=0 && hoverCol<Constants.COLS
                && hoverRow>=0 && hoverRow<Constants.ROWS && selectedTowerType!=null) {
            boolean canPlace = !isPathTile(hoverCol,hoverRow) && !hasTower(hoverCol,hoverRow);
            g.setColor(canPlace ? new Color(100,255,120,70) : new Color(255,80,80,70));
            g.fillRect(p+hoverCol*Constants.TILE, p+hoverRow*Constants.TILE, Constants.TILE, Constants.TILE);
            g.setColor(canPlace ? new Color(100,255,120,150) : new Color(255,80,80,150));
            g.setStroke(new BasicStroke(1.5f));
            g.drawRect(p+hoverCol*Constants.TILE, p+hoverRow*Constants.TILE, Constants.TILE, Constants.TILE);
            g.setStroke(new BasicStroke(1));
        }

        drawPortal(g, currentMap.path[0],                        new Color(0x1565C0), new Color(0x42A5F5), "IN",  p);
        drawPortal(g, currentMap.path[currentMap.path.length-1], new Color(0xB71C1C), new Color(0xEF5350), "OUT", p);
    }

    private void drawPathEdgeShading(Graphics2D g, int col, int row, int tx, int ty) {
        int[][] dirs = {{0,-1,tx,ty,Constants.TILE,3},{0,1,tx,ty+Constants.TILE-3,Constants.TILE,3},
                        {-1,0,tx,ty,3,Constants.TILE},{1,0,tx+Constants.TILE-3,ty,3,Constants.TILE}};
        for (int[] d : dirs) {
            int nc=col+d[0], nr=row+d[1];
            if (nc<0||nc>=Constants.COLS||nr<0||nr>=Constants.ROWS||!isPathTile(nc,nr)) {
                g.setColor(new Color(0,0,0,45));
                g.fillRect(d[2], d[3], d[4], d[5]);
            }
        }
    }

    private void drawTree(Graphics2D g, int tx, int ty, int variant) {
        int cx = tx+Constants.TILE/2, cy = ty+Constants.TILE/2;
        g.setColor(new Color(0x5D4037));
        g.fillRect(cx-2, cy, 4, 10);
        Color[] trunkCols = {new Color(0x1B5E20), new Color(0x2E7D32), new Color(0x388E3C)};
        Color c = trunkCols[variant % 3];
        g.setColor(c);
        g.fillOval(cx-9, cy-10, 18, 14);
        g.setColor(c.brighter());
        g.fillOval(cx-6, cy-13, 12, 10);
        g.setColor(new Color(0,0,0,30));
        g.fillOval(cx-8, cy+6, 16, 5);
    }

    private void drawRock(Graphics2D g, int tx, int ty, int variant) {
        int cx = tx+Constants.TILE/2, cy = ty+Constants.TILE/2+3;
        Color[] rockCols = {new Color(0x78909C), new Color(0x90A4AE), new Color(0x607D8B)};
        g.setColor(new Color(0,0,0,30));
        g.fillOval(cx-8, cy+3, 16, 5);
        g.setColor(rockCols[variant % 3]);
        g.fillOval(cx-7, cy-6, 14, 10);
        g.setColor(rockCols[variant%3].brighter());
        g.fillOval(cx-4, cy-5, 5, 4);
    }

    private void drawFlower(Graphics2D g, int tx, int ty, int variant) {
        int cx = tx+Constants.TILE/2, cy = ty+Constants.TILE/2+2;
        Color[] petalCols = {new Color(0xFF8A80), new Color(0xFFFF8D), new Color(0xEA80FC)};
        g.setColor(new Color(0x2E7D32));
        g.fillRect(cx-1, cy, 2, 7);
        Color pc = petalCols[variant%3];
        for (int a=0; a<360; a+=72) {
            double rad = Math.toRadians(a);
            g.setColor(pc);
            g.fillOval(cx+(int)(4*Math.cos(rad))-2, cy-2+(int)(4*Math.sin(rad))-2, 5, 5);
        }
        g.setColor(new Color(0xFFD700));
        g.fillOval(cx-2, cy-4, 5, 5);
    }

    private void drawPortal(Graphics2D g, int[] pt, Color inner, Color outer, String label, int pad) {
        int tx = pad+pt[0]*Constants.TILE, ty = pad+pt[1]*Constants.TILE;
        g.setColor(new Color(inner.getRed(),inner.getGreen(),inner.getBlue(),40));
        g.fillRoundRect(tx-2, ty-2, Constants.TILE+4, Constants.TILE+4, 10, 10);
        g.setColor(inner);
        g.fillRoundRect(tx+2, ty+2, Constants.TILE-4, Constants.TILE-4, 8, 8);
        g.setColor(outer);
        g.setStroke(new BasicStroke(2f));
        g.drawRoundRect(tx+2, ty+2, Constants.TILE-4, Constants.TILE-4, 8, 8);
        g.setStroke(new BasicStroke(1));
        g.setColor(Color.WHITE);
        g.setFont(new Font("SansSerif", Font.BOLD, 9));
        FontMetrics fm = g.getFontMetrics();
        g.drawString(label, tx+(Constants.TILE-fm.stringWidth(label))/2, ty+Constants.TILE/2+4);
    }

    private void drawTowers(Graphics2D g) {
        for (Tower t : towers) { if (t.isSelected()) t.drawRangePreview(g); t.draw(g); }
    }
    private void drawEnemies(Graphics2D g) { for (Enemy e : enemies) e.draw(g); }
    private void drawProjectiles(Graphics2D g) { for (Projectile p : projectiles) p.draw(g); }

    private void drawPlacementPreview(Graphics2D g) {
        if (hoverCol<0||hoverRow<0) return;
        new Tower(hoverCol, hoverRow, selectedTowerType).drawRangePreview(g);
    }

    private void drawFloatTexts(Graphics2D g) {
        for (FloatText ft : floatTexts) {
            float alpha = ft.life / ft.maxLife;
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
            g.setFont(new Font("SansSerif", Font.BOLD, 11));
            g.setColor(ft.color);
            FontMetrics fm = g.getFontMetrics();
            g.drawString(ft.text, ft.x - fm.stringWidth(ft.text)/2f, ft.y);
            // Outline for readability
            g.setColor(new Color(0,0,0,(int)(180*alpha)));
            g.setFont(new Font("SansSerif", Font.BOLD, 11));
            g.drawString(ft.text, ft.x - fm.stringWidth(ft.text)/2f + 0.8f, ft.y + 0.8f);
        }
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
    }

    private void drawGameBorder(Graphics2D g) { // creates the black border around the game
        int p=Constants.MAP_PAD, tileW=Constants.COLS*Constants.TILE, tileH=Constants.ROWS*Constants.TILE;
        int[][] gl = {{6,14,15},{4,9,30},{3,5,50},{2,3,75},{1,2,105}};
        for (int[] l : gl) {
            g.setColor(new Color(233,69,96,l[2]));
            g.setStroke(new BasicStroke(l[1]));
            g.drawRect(p-l[0], p-l[0], tileW+l[0]*2, tileH+l[0]*2);
        }
        g.setColor(Color.BLACK); g.setStroke(new BasicStroke(p));
        g.drawRect(p/2, p/2, tileW+p, tileH+p);
        g.setColor(new Color(0x0F3460)); g.setStroke(new BasicStroke(2.5f));
        g.drawRect(p, p, tileW, tileH);
        g.setColor(new Color(0xE94560)); g.setStroke(new BasicStroke(1.5f));
        g.drawRect(p+2, p+2, tileW-4, tileH-4);
        int arm=18;
        g.setColor(UI_GOLD); g.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER));
        int x0=p, y0=p, x1=p+tileW, y1=p+tileH;
        g.drawLine(x0,y0,x0+arm,y0); g.drawLine(x0,y0,x0,y0+arm);
        g.drawLine(x1-arm,y0,x1,y0); g.drawLine(x1,y0,x1,y0+arm);
        g.drawLine(x0,y1-arm,x0,y1); g.drawLine(x0,y1,x0+arm,y1);
        g.drawLine(x1-arm,y1,x1,y1); g.drawLine(x1,y1-arm,x1,y1);
        g.setStroke(new BasicStroke(1));
    }

    private Rectangle waveButtonBounds  = new Rectangle(); // ui sidebar
    private Rectangle upgradeABounds   = new Rectangle(0,0,0,0);
    private Rectangle upgradeBBounds   = new Rectangle(0,0,0,0);
    private Rectangle sellButtonBounds = new Rectangle(0,0,0,0);
    private Rectangle[] speedButtonBounds = {new Rectangle(), new Rectangle(), new Rectangle()};
    private Rectangle autoButtonBounds = new Rectangle();

    private void drawUI(Graphics2D g) {
        int ux=Constants.GAME_WIDTH, uw=Constants.UI_WIDTH, uh=Constants.GAME_HEIGHT;
        GradientPaint bg = new GradientPaint(ux, 0, new Color(0x0E0E1C), ux, uh, new Color(0x09090F));
        g.setPaint(bg); g.fillRect(ux, 0, uw, uh);
        GradientPaint borderGrad = new GradientPaint(ux, 0, UI_ACCENT, ux, uh, new Color(0x0F3460));
        g.setPaint(borderGrad); g.fillRect(ux, 0, 2, uh);
        g.setColor(UI_BG); g.fillRect(ux+2, 0, 2, uh);
        g.setPaint(null);

        final int[] yRef = {0};

        yRef[0] = drawCard(g, ux, yRef[0], uw, 68, () -> {
            g.setFont(new Font("Georgia", Font.BOLD, 13));
            g.setColor(UI_ACCENT);
            drawCentered(g, "WATERMELON FIGHTERS", ux, uw, 16);

            g.setFont(new Font("SansSerif", Font.ITALIC, 9));
            g.setColor(UI_SUBTEXT);
            drawCentered(g, currentMap.name, ux, uw, 28);

            g.setColor(new Color(0x1E3A5F)); g.fillRect(ux+12, 32, uw-24, 1);

            int wave = waveManager.getCurrentWave(), total = waveManager.getTotalWaves();
            g.setFont(new Font("Monospaced", Font.BOLD, 10));
            g.setColor(UI_TEXT);
            drawCentered(g, "Wave  " + wave + " / " + total, ux, uw, 47);

            int barX=ux+10, barW=uw-20, barH=5;
            g.setColor(new Color(0x1A2A3A)); g.fillRoundRect(barX, 51, barW, barH, 3,3);
            float prog = total>0 ? (float)wave/total : 0;
            GradientPaint wp = new GradientPaint(barX,0,new Color(0x0F3460), barX+barW,0, UI_ACCENT);
            g.setPaint(wp); g.fillRoundRect(barX, 51, (int)(barW*prog), barH, 3,3); g.setPaint(null);
            g.setColor(new Color(0x2A4A6A)); g.setStroke(new BasicStroke(1));
            g.drawRoundRect(barX, 51, barW, barH, 3,3); g.setStroke(new BasicStroke(1));
        });

        final int statsY = yRef[0];
        yRef[0] = drawCard(g, ux, yRef[0], uw, 36, () -> {
            g.setFont(new Font("SansSerif", Font.BOLD, 10)); // lives
            float lifeRatio = (float)lives / Constants.STARTING_LIVES;
            Color heartCol = lifeRatio > 0.5f ? new Color(0xFF6B6B) : lifeRatio > 0.25f ? new Color(0xFFC107) : new Color(0xFF1744);
            g.setColor(heartCol);
            g.drawString("♥ " + lives, ux+9, statsY-36+22);
            g.setColor(UI_GOLD); // currency
            String gs = "$ " + currency;
            g.setFont(new Font("Monospaced", Font.BOLD, 10));
            FontMetrics fm = g.getFontMetrics();
            g.drawString(gs, ux+uw-9-fm.stringWidth(gs), statsY-36+22);
            g.setColor(new Color(0x66AACC)); g.setFont(new Font("SansSerif", Font.PLAIN, 9));
            drawCentered(g, "Score: "+score, ux, uw, statsY-36+33); // score
        });

        boolean canStart = !waveManager.isWaveActive() && waveManager.hasMoreWaves() && !gameOver && !victory; // wave
        int y = yRef[0];
        y += 4;
        int wbH = 28;
        if (canStart) {
            g.setColor(new Color(30,120,50,40)); g.fillRoundRect(ux+6, y, uw-12, wbH, 8,8);
            g.setColor(new Color(50,200,80,25)); g.fillRoundRect(ux+4, y-39, uw-8, wbH+2, 10,10);
        }
        Color wBg = canStart ? new Color(0x1B3F20) : new Color(0x12121E);
        Color wFg = canStart ? new Color(0xAAFFAA) : UI_SUBTEXT;
        String wLabel = canStart ? "▶  Start Wave " + (waveManager.getCurrentWave()+1)
                      : waveManager.isWaveActive() ? "⚔  Wave In Progress"
                      : "✓  All Waves Done";
        drawButton(g, ux+6, y-38, uw-12, wbH, wBg, wLabel, wFg);
        if (canStart) { g.setColor(new Color(0x55BB55)); g.setStroke(new BasicStroke(1.5f));
                        g.drawRoundRect(ux+6,y-38,uw-12,wbH,8,8); g.setStroke(new BasicStroke(1)); }
        waveButtonBounds = new Rectangle(ux+6, y-38, uw-12, wbH);
        y += wbH + 15;

        int speedCardTop = y; // speed changes and auto button
        y = drawCard(g, ux, y, uw, 54, () -> {
            int yy = speedCardTop - 54;
            g.setColor(UI_SUBTEXT); g.setFont(new Font("SansSerif", Font.BOLD, 8));
            drawCentered(g, "SPEED", ux, uw, yy+13);
            int[] spds = {1,2,4}; String[] sLbls = {"1×","2×","4×"};
            int sw = (uw-20)/3-2;
            for (int i=0;i<3;i++) {
                int sx2 = ux+8+i*(sw+4);
                boolean act = gameSpeed==spds[i];
                g.setColor(act ? new Color(0x1A3050) : new Color(0x0E0E1A));
                g.fillRoundRect(sx2, yy+17, sw, 18, 4,4);
                g.setColor(act ? UI_ACCENT : new Color(0x1E3A5F));
                g.setStroke(act ? new BasicStroke(1.5f) : new BasicStroke(1));
                g.drawRoundRect(sx2, yy+17, sw, 18, 4,4);
                g.setStroke(new BasicStroke(1));
                g.setColor(act ? UI_GOLD : UI_SUBTEXT);
                g.setFont(new Font("Monospaced", Font.BOLD, 9));
                FontMetrics fm2 = g.getFontMetrics();
                g.drawString(sLbls[i], sx2+(sw-fm2.stringWidth(sLbls[i]))/2, yy+30);
                speedButtonBounds[i] = new Rectangle(sx2, yy+17, sw, 18);
            }
            // Auto toggle
            Color aBg = autoStart ? new Color(0x152A15) : new Color(0x0E0E1A);
            g.setColor(aBg); g.fillRoundRect(ux+8, yy+40, uw-16, 10, 4,4);
            g.setColor(autoStart ? new Color(0x44AA44) : new Color(0x1E3A5F));
            g.setStroke(autoStart ? new BasicStroke(1.5f):new BasicStroke(1));
            g.drawRoundRect(ux+8, yy+40, uw-16, 10, 4,4);
            g.setStroke(new BasicStroke(1));
            g.setColor(autoStart ? new Color(0x66FF66) : new Color(0x334455));
            g.fillOval(ux+11, yy+42, 8, 8);
            g.setColor(autoStart ? new Color(0xAAFFAA):UI_SUBTEXT);
            g.setFont(new Font("SansSerif", Font.BOLD, 8));
            g.drawString(autoStart?"AUTO ON":"AUTO OFF", ux+24, yy+50);
            autoButtonBounds = new Rectangle(ux+8, yy+40, uw-16, 10);
        });
        y += 4;

        boolean waveLocked = waveManager.isWaveActive();
        if (waveLocked) {
            g.setColor(new Color(0xE94560,true)); g.setFont(new Font("SansSerif",Font.BOLD,8));
            drawCentered(g, "⚔  WAVE ACTIVE — placement locked", ux, uw, y+11); y+=16;
        }

        String[] names  = {"Dart Monkey","Bomb Shooter","Ice Monkey","Super Monkey","Mortar Monkey"}; // monkey types
        int[]    costs  = {70, 120, 120, 300, 225};
        Color[]  dots   = {new Color(0xE53935),new Color(0x546E7A),new Color(0x29B6F6),new Color(0xFFD600),new Color(0x558B2F)};

        for (int i=0;i<5;i++) {
            boolean sel = selectedTowerType==towerTypes[i];
            boolean can = !waveLocked && currency>=costs[i];
            boolean dim = waveLocked || !can;

            Color bg2 = waveLocked ? new Color(0x0C0C14) : sel ? new Color(0x1A3558) : can ? new Color(0x0D1E30) : new Color(0x111116);
            Color bdr = waveLocked ? new Color(0x181828) : sel ? UI_ACCENT : can ? UI_BORDER : new Color(0x1A1A28);

            int bx=ux+6, bw=uw-12, bh=25;
            g.setColor(bg2); g.fillRoundRect(bx, y, bw, bh, 6, 6);
            g.setColor(bdr); g.setStroke(new BasicStroke(sel?2f:1f));
            g.drawRoundRect(bx, y, bw, bh, 6, 6);
            g.setStroke(new BasicStroke(1));

            if (sel) {
                g.setColor(UI_ACCENT); g.fillRoundRect(bx, y+4, 3, bh-8, 2, 2);
            }

            Color dotC = dim ? new Color(dots[i].getRed()/4,dots[i].getGreen()/4,dots[i].getBlue()/4) : dots[i];
            g.setColor(dotC); g.fillOval(bx+7, y+bh/2-5, 10, 10);
            if (!dim) { g.setColor(dotC.brighter()); g.setStroke(new BasicStroke(0.8f)); g.drawOval(bx+7,y+bh/2-5,10,10); g.setStroke(new BasicStroke(1)); }

            g.setColor(dim ? new Color(0x3A3A4A) : UI_TEXT);
            g.setFont(new Font("SansSerif", Font.BOLD, 9));
            g.drawString(names[i], bx+22, y+bh/2+4);

            g.setColor(can ? UI_GOLD : new Color(0x554400));
            g.setFont(new Font("Monospaced", Font.PLAIN, 9));
            String cs="$"+costs[i]; FontMetrics fm3=g.getFontMetrics();
            g.drawString(cs, bx+bw-fm3.stringWidth(cs)-5, y+bh/2+4);

            towerButtons[i] = new Rectangle(bx, y, bw, bh);
            y += 28;
        }

        y += 2;
        g.setColor(new Color(0x1A2A3A)); g.fillRect(ux+6, y, uw-12, 1); y += 5;
        if (selectedTower!=null) drawTowerInfo(g, ux, uw, y, waveLocked);
        else {
            g.setColor(UI_SUBTEXT); g.setFont(new Font("SansSerif", Font.ITALIC, 9));
            drawCentered(g, "Click a tower to upgrade", ux, uw, y+10);
        }

        g.setColor(new Color(0x334455)); g.setFont(new Font("SansSerif", Font.PLAIN, 8));
        drawCentered(g, "P = Pause  |  ESC = Cancel", ux, uw, uh-6);
    }

    private int drawCard(Graphics2D g, int ux, int y, int uw, int h, Runnable content) {
        int margin = 4;
        g.setColor(UI_CARD);
        g.fillRoundRect(ux+margin, y+2, uw-margin*2, h-4, 8, 8);
        g.setColor(UI_BORDER);
        g.setStroke(new BasicStroke(1f));
        g.drawRoundRect(ux+margin, y+2, uw-margin*2, h-4, 8, 8);
        g.setStroke(new BasicStroke(1));
        content.run();
        return y + h + 2;
    }

    private void drawTowerInfo(Graphics2D g, int ux, int uw, int y, boolean waveLocked) { // tower panel info
        Tower t = selectedTower;
        int pA=t.getPathA(), pB=t.getPathB();
        boolean aLock=t.isPathAHardLocked(), bLock=t.isPathBHardLocked();

        g.setColor(new Color(0x12203A)); g.fillRoundRect(ux+6, y, uw-12, 20, 6,6);
        g.setColor(UI_ACCENT); g.setStroke(new BasicStroke(1)); g.drawRoundRect(ux+6,y,uw-12,20,6,6); g.setStroke(new BasicStroke(1));
        g.setColor(UI_TEXT); g.setFont(new Font("Georgia",Font.BOLD,11));
        drawCentered(g, t.getName(), ux, uw, y+14);
        y += 23;

        g.setColor(UI_SUBTEXT); g.setFont(new Font("Monospaced",Font.PLAIN,8));
        String stats = String.format("DMG:%.0f  RNG:%.1f  SPD:%.0f", t.getDamage(), t.getRange(), t.getFireRate());
        drawCentered(g, stats, ux, uw, y); y += 12;
        g.setColor(new Color(0x667788)); g.setFont(new Font("SansSerif",Font.PLAIN,8));
        drawCentered(g, "Sell: $"+t.sellValue(), ux, uw, y); y += 12;

        g.setColor(new Color(0x1A2A3A)); g.fillRect(ux+8, y, uw-16, 1); y += 6;

        g.setColor(new Color(0xBBCCDD)); g.setFont(new Font("SansSerif",Font.BOLD,8));
        drawCentered(g, "SKILL  TREE", ux, uw, y); y += 12;

        if (pA==0&&pB==0) {
            g.setColor(new Color(0x667788)); g.setFont(new Font("SansSerif",Font.ITALIC,7));
            drawCentered(g, "Commit T2+ to lock other path", ux, uw, y); y += 9;
        } else if ((pA>=2&&pB<2)||(pB>=2&&pA<2)) {
            g.setColor(new Color(0xFFAA44)); g.setFont(new Font("SansSerif",Font.ITALIC,7));
            String hint = pA>=pB ? "Path B now capped at T2" : "Path A now capped at T2";
            drawCentered(g, hint, ux, uw, y); y += 9;
        } else { y += 2; }

        y = drawPathBlock(g, ux, uw, y, "A", t.getPathALabel(), pA, t.getPathANames(), t.getPathADescs(),
                          COL_A, aLock, waveLocked, t.canUpgradePathA(), t.pathACost(), currency, true);
        y = drawPathBlock(g, ux, uw, y, "B", t.getPathBLabel(), pB, t.getPathBNames(), t.getPathBDescs(),
                          COL_B, bLock, waveLocked, t.canUpgradePathB(), t.pathBCost(), currency, false);

        Color sBg = waveLocked ? new Color(0x0C0C14) : new Color(0x2A0A0A);
        Color sFg = waveLocked ? new Color(0x333344) : new Color(0xFF7070);
        drawButton(g, ux+6, y, uw-12, 20, sBg, "\u2715  Sell  $"+t.sellValue(), sFg);
        if (!waveLocked) { g.setColor(new Color(0x772222)); g.setStroke(new BasicStroke(1));
                           g.drawRoundRect(ux+6,y,uw-12,20,6,6); g.setStroke(new BasicStroke(1)); }
        sellButtonBounds = waveLocked ? new Rectangle(0,0,0,0) : new Rectangle(ux+6,y,uw-12,20);
    }

    private int drawPathBlock(Graphics2D g, int ux, int uw, int y, String letter, String label,
                              int level, String[] names, String[] descs, Color col,
                              boolean hardLocked, boolean waveLocked, boolean canUpgrade,
                              int cost, int currency, boolean isA) {
        int iw = uw-12;
        boolean locked = hardLocked || waveLocked;

        g.setColor(locked ? new Color(0x0C0C18) : new Color(0x0D1F30));
        g.fillRoundRect(ux+6, y, iw, 16, 4, 4);
        g.setColor(locked ? new Color(0x1E1E2E) : col.darker().darker());
        g.setStroke(new BasicStroke(1));
        g.drawRoundRect(ux+6, y, iw, 16, 4, 4);
        g.setStroke(new BasicStroke(1));

        g.setColor(locked ? COL_LOCKED : col);
        g.fillOval(ux+9, y+2, 12, 12);
        g.setColor(locked ? new Color(0x445566) : Color.WHITE);
        g.setFont(new Font("SansSerif",Font.BOLD,8)); g.drawString(letter, ux+12, y+11);

        g.setColor(locked ? new Color(0x334455) : col);
        g.setFont(new Font("SansSerif",Font.BOLD,8)); g.drawString(label, ux+25, y+11);

        if (hardLocked) drawLockIcon(g, ux+uw-22, y+3, 10);
        else if (level>=3) { g.setColor(UI_GOLD); g.setFont(new Font("SansSerif",Font.BOLD,7)); g.drawString("MAX", ux+uw-24, y+11); }
        y += 20;

        int nodeR=6, ns=3, nsW=iw-4, nSp=nsW/ns;
        for (int i=0;i<ns;i++) {
            int nx=ux+8+nSp/2+i*nSp, ny=y+nodeR+2;
            if (i>0) {
                g.setColor(i<=level && !locked ? col : COL_LOCKED);
                g.setStroke(new BasicStroke(2)); g.drawLine(nx-nSp+nodeR+1,ny,nx-nodeR-1,ny); g.setStroke(new BasicStroke(1));
            }
            boolean filled=i<level, isNext=i==level&&!hardLocked;
            if (filled) {
                g.setColor(locked ? COL_LOCKED.darker() : col); g.fillOval(nx-nodeR,ny-nodeR,nodeR*2,nodeR*2);
                g.setColor(locked ? new Color(0x2A2A3A) : col.brighter()); g.setStroke(new BasicStroke(1.5f));
                g.drawOval(nx-nodeR,ny-nodeR,nodeR*2,nodeR*2); g.setStroke(new BasicStroke(1));
                g.setColor(Color.WHITE); g.setFont(new Font("SansSerif",Font.BOLD,7)); g.drawString("✓",nx-3,ny+3);
            } else if (isNext&&!locked) {
                g.setColor(new Color(0x080810)); g.fillOval(nx-nodeR,ny-nodeR,nodeR*2,nodeR*2);
                g.setColor(col); g.setStroke(new BasicStroke(2f)); g.drawOval(nx-nodeR,ny-nodeR,nodeR*2,nodeR*2); g.setStroke(new BasicStroke(1));
                g.setColor(col.brighter()); g.setFont(new Font("SansSerif",Font.BOLD,7)); g.drawString(String.valueOf(i+1),nx-2,ny+3);
            } else {
                g.setColor(new Color(0x080810)); g.fillOval(nx-nodeR,ny-nodeR,nodeR*2,nodeR*2);
                g.setColor(hardLocked ? new Color(0x1A1A28) : new Color(0x222233));
                g.drawOval(nx-nodeR,ny-nodeR,nodeR*2,nodeR*2);
                g.setColor(new Color(0x334455)); g.setFont(new Font("SansSerif",Font.BOLD,7)); g.drawString(String.valueOf(i+1),nx-2,ny+3);
            }
        }
        y += nodeR*2+6;

        g.setFont(new Font("SansSerif",Font.PLAIN,7));
        for (int i=0;i<3;i++) {
            int nx=ux+8+nSp/2+i*nSp;
            g.setColor(i<level&&!locked ? col : (hardLocked ? new Color(0x2A2A3A) : new Color(0x445566)));
            FontMetrics fm=g.getFontMetrics();
            String nm=names[i]; while(fm.stringWidth(nm)>nSp-2&&nm.length()>3) nm=nm.substring(0,nm.length()-1);
            g.drawString(nm, nx-fm.stringWidth(nm)/2, y);
        }
        y += 10;

        if (level<3&&!hardLocked) {
            g.setColor(new Color(0x4A6680)); g.setFont(new Font("SansSerif",Font.ITALIC,7));
            String desc="Next: "+descs[level]; FontMetrics fmd=g.getFontMetrics();
            while(fmd.stringWidth(desc)>iw-4&&desc.length()>6) desc=desc.substring(0,desc.length()-1)+"…";
            drawCentered(g, desc, ux, uw, y);
        }
        y += 9;

        String btnLabel; Color btnBg, btnFg;
        if (hardLocked) { btnLabel="\uD83D\uDD12  Path "+letter+" Locked"; btnBg=new Color(0x0A0A14); btnFg=new Color(0x2A3A4A); }
        else if (level>=3) { btnLabel="★  MAX  ★"; btnBg=new Color(0x1A1600); btnFg=new Color(0x887700); }
        else if (waveLocked) { btnLabel="▲  Upgrade  $"+cost; btnBg=new Color(0x0C0C14); btnFg=new Color(0x334455); }
        else if (canUpgrade && currency>=cost) { btnLabel="▲  "+names[level]+"  $"+cost; btnBg=isA?new Color(0x2A1208):new Color(0x081828); btnFg=col; }
        else { btnLabel="▲  $"+cost+" needed"; btnBg=new Color(0x0D0D1A); btnFg=new Color(0x2A3A4A); }

        drawButton(g, ux+6, y, iw, 20, btnBg, btnLabel, btnFg);
        boolean active = !hardLocked && canUpgrade && !waveLocked && level<3 && currency>=cost;
        if (!hardLocked && level<3) {
            g.setColor(active ? col.darker() : new Color(0x1A1A28));
            g.setStroke(new BasicStroke(active?1.5f:1f));
            g.drawRoundRect(ux+6, y, iw, 20, 6, 6);
            g.setStroke(new BasicStroke(1));
        }
        Rectangle bounds = (!hardLocked&&canUpgrade&&!waveLocked) ? new Rectangle(ux+6,y,iw,20) : new Rectangle(0,0,0,0);
        if (isA) upgradeABounds=bounds; else upgradeBBounds=bounds;
        y += 26;
        return y;
    }

    private void drawLockIcon(Graphics2D g, int x, int y, int size) {
        g.setColor(new Color(0x8B1A1A));
        int sw=(int)(size*0.55f), sh=(int)(size*0.45f);
        int sx=x+(size-sw)/2;
        g.setStroke(new BasicStroke(1.5f));
        g.drawArc(sx, y, sw, sh*2, 0, 180);
        g.setStroke(new BasicStroke(1));
        int by=y+sh;
        g.setColor(new Color(0x6B1515));
        g.fillRoundRect(x, by, size, size-sh, 2, 2);
        g.setColor(new Color(0xFFAAAA, true));
        g.fillOval(x+size/2-1, by+1, 3, 3);
    }

    private void drawButton(Graphics2D g, int x, int y, int w, int h, Color bg, String label, Color fg) {
        g.setColor(bg); g.fillRoundRect(x,y,w,h,6,6);
        g.setColor(fg); g.setFont(new Font("SansSerif",Font.BOLD,9));
        FontMetrics fm=g.getFontMetrics();
        String l=label; while(fm.stringWidth(l)>w-6&&l.length()>4) l=l.substring(0,l.length()-1)+"…";
        g.drawString(l, x+(w-fm.stringWidth(l))/2, y+h/2+4);
    }

    private void drawCentered(Graphics2D g, String s, int ux, int uw, int y) {
        FontMetrics fm=g.getFontMetrics();
        g.drawString(s, ux+(uw-fm.stringWidth(s))/2, y);
    }

    private void drawOverlay(Graphics2D g) {
        g.setColor(new Color(0,0,0,170)); g.fillRect(0,0,Constants.TOTAL_WIDTH,Constants.GAME_HEIGHT);
        g.setFont(new Font("Georgia",Font.BOLD,52));
        Color mainCol = victory ? UI_GOLD : new Color(0xFF4444);
        String msg = victory ? "VICTORY!" : "GAME  OVER";
        FontMetrics fm=g.getFontMetrics();
        g.setColor(new Color(0,0,0,140));
        g.drawString(msg, (Constants.TOTAL_WIDTH-fm.stringWidth(msg))/2+3, Constants.GAME_HEIGHT/2-16);
        g.setColor(mainCol);
        g.drawString(msg, (Constants.TOTAL_WIDTH-fm.stringWidth(msg))/2, Constants.GAME_HEIGHT/2-18);
        g.setFont(new Font("SansSerif",Font.PLAIN,16));
        g.setColor(UI_TEXT);
        String sub="Score: "+score+" · Wave "+waveManager.getCurrentWave()+"/"+waveManager.getTotalWaves();
        fm=g.getFontMetrics();
        g.drawString(sub, (Constants.TOTAL_WIDTH-fm.stringWidth(sub))/2, Constants.GAME_HEIGHT/2+18);
        g.setFont(new Font("SansSerif",Font.ITALIC,12));
        g.setColor(UI_SUBTEXT);
        String hint="Press F5 or restart to play again";
        fm=g.getFontMetrics();
        g.drawString(hint, (Constants.TOTAL_WIDTH-fm.stringWidth(hint))/2, Constants.GAME_HEIGHT/2+46);
    }

    private void drawPause(Graphics2D g) {
        g.setColor(new Color(0,0,0,110)); g.fillRect(0,0,Constants.TOTAL_WIDTH,Constants.GAME_HEIGHT);
        g.setColor(UI_TEXT); g.setFont(new Font("Georgia",Font.BOLD,38));
        FontMetrics fm=g.getFontMetrics(); String msg="PAUSED";
        g.drawString(msg, (Constants.TOTAL_WIDTH-fm.stringWidth(msg))/2, Constants.GAME_HEIGHT/2);
        g.setFont(new Font("SansSerif",Font.PLAIN,13)); g.setColor(UI_SUBTEXT);
        String hint="Press P to resume"; fm=g.getFontMetrics();
        g.drawString(hint, (Constants.TOTAL_WIDTH-fm.stringWidth(hint))/2, Constants.GAME_HEIGHT/2+26);
    }

    private void handleHover(MouseEvent e) { // input for hover effects and tile highlights
        int p=Constants.MAP_PAD;
        hoverCol=(e.getX()-p)/Constants.TILE;
        hoverRow=(e.getY()-p)/Constants.TILE;
    }

    private void handleClick(MouseEvent e) {
        int p=Constants.MAP_PAD; int mx=e.getX(), my=e.getY();
        boolean waveActive=waveManager.isWaveActive();
        if (mx>=Constants.GAME_WIDTH) {
            if (waveButtonBounds.contains(mx,my)) {
                if (!waveActive&&waveManager.hasMoreWaves()&&!gameOver&&!victory) { selectedTowerType=null; deselectTower(); waveManager.startNextWave(); }
                return;
            }
            int[] spds={1,2,4};
            for (int i=0;i<3;i++) if (speedButtonBounds[i].contains(mx,my)) { gameSpeed=spds[i]; return; }
            if (autoButtonBounds.contains(mx,my)) { autoStart=!autoStart; return; }
            if (!waveActive) {
                for (int i=0;i<towerButtons.length;i++) {
                    if (towerButtons[i]!=null&&towerButtons[i].contains(mx,my)) {
                        int cost=new int[]{70,120,120,300,225}[i];
                        if (currency>=cost) { selectedTowerType=towerTypes[i]; deselectTower(); } return;
                    }
                }
                if (selectedTower!=null) {
                    if (upgradeABounds.contains(mx,my)) {
                        if (selectedTower.canUpgradePathA()&&currency>=selectedTower.pathACost()) { currency-=selectedTower.pathACost(); selectedTower.upgradePathA(); }
                        return;
                    }
                    if (upgradeBBounds.contains(mx,my)) {
                        if (selectedTower.canUpgradePathB()&&currency>=selectedTower.pathBCost()) { currency-=selectedTower.pathBCost(); selectedTower.upgradePathB(); }
                        return;
                    }
                    if (sellButtonBounds.contains(mx,my)) { currency+=selectedTower.sellValue(); towers.remove(selectedTower); selectedTower=null; return; }
                }
            }
            return;
        }
        if (waveActive) return;
        int gx=mx-p, gy=my-p; if (gx<0||gy<0) return;
        int col=gx/Constants.TILE, row=gy/Constants.TILE;
        if (col>=Constants.COLS||row>=Constants.ROWS) return;
        if (selectedTowerType!=null) {
            if (!isPathTile(col,row)&&!hasTower(col,row)) {
                int c=costOf(selectedTowerType); if (currency>=c) { currency-=c; towers.add(new Tower(col,row,selectedTowerType)); }
            }
        } else {
            deselectTower();
            for (Tower t : towers) if (t.getCol()==col&&t.getRow()==row) { t.setSelected(true); selectedTower=t; break; }
        }
    }

    private void deselectTower() { if (selectedTower!=null) { selectedTower.setSelected(false); selectedTower=null; } }
    private int costOf(Tower.TType t) {
        switch(t) { case DART:return 70; case BOMB:return 120; case ICE:return 120; case SUPER:return 300; case MORTAR:return 225; default:return 999; }
    }
    private static int clamp(int v) { return Math.max(0,Math.min(255,v)); }
}