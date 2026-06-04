import java.awt.*;
import java.util.List;

public class Tower {
    public enum TType { DART, BOMB, ICE, SUPER, MORTAR }

    private int col, row;
    private TType ttype;
    private int level = 1; // 1,2,3
    private float range, damage, fireRate;
    private int cost;
    private Color color;
    private int fireCooldown = 0;
    private float angle = 0;
    private boolean selected = false;

    // Upgrade costs per level (to reach level 2, level 3)
    private static final int[] UPGRADE_COSTS = {80, 150};

    public Tower(int col, int row, TType ttype) {
        this.col = col; this.row = row; this.ttype = ttype;
        applyStats();
    }

    private void applyStats() {
        float[] multiplier = {1f, 1.5f, 2.2f};
        float m = multiplier[level-1];
        switch (ttype) {
            case DART:  cost=80;  range=3.5f; damage=15;  fireRate=40;  color=new Color(0xE53935); break; // red
            case BOMB:  cost=150; range=2.5f; damage=60;  fireRate=90;  color=new Color(0x37474F); break; // dark slate
            case ICE:   cost=120; range=3.0f; damage=8;   fireRate=50;  color=new Color(0x29B6F6); break; // ice blue
            case SUPER: cost=200; range=4.0f; damage=25;  fireRate=20;  color=new Color(0xFFD600); break; // golden yellow
            case MORTAR:cost=175; range=5.0f; damage=80;  fireRate=120; color=new Color(0x6D4C41); break; // brown
        }
        damage *= m;
        range  *= (1 + 0.15f*(level-1));
        fireRate = Math.max(5, fireRate / (float)Math.sqrt(m));
    }

    public Projectile update(List<Enemy> enemies, List<Enemy> allEnemies) {
        if (fireCooldown > 0) { fireCooldown--; return null; }
        Enemy target = findTarget(enemies);
        if (target == null) return null;

        float cx = col * Constants.TILE + Constants.TILE/2f;
        float cy = row * Constants.TILE + Constants.TILE/2f;
        angle = (float)Math.atan2(target.getY()-cy, target.getX()-cx);
        fireCooldown = (int)fireRate;

        Projectile.PType pt;
        float splash = 0;
        switch (ttype) {
            case BOMB:  pt = Projectile.PType.CANNONBALL; splash = 40; break;
            case ICE:   pt = Projectile.PType.FROST_BOLT; break;
            case SUPER: pt = Projectile.PType.LASER_BEAM; break;
            case MORTAR:pt = Projectile.PType.MORTAR_SHELL; splash = 60; break;
            default:    pt = Projectile.PType.DART; break;
        }
        float projSpeed = ttype == TType.SUPER ? 0 : ttype == TType.MORTAR ? 4f : 7f;
        return new Projectile(cx, cy, target, damage, projSpeed, pt, splash, allEnemies);
    }

    private Enemy findTarget(List<Enemy> enemies) {
        float cx = col * Constants.TILE + Constants.TILE/2f;
        float cy = row * Constants.TILE + Constants.TILE/2f;
        float rangePixels = range * Constants.TILE;
        Enemy best = null;
        float bestDist = Float.MIN_VALUE;
        for (Enemy e : enemies) {
            if (e.isDead() || e.hasReached()) continue;
            float dx = e.getX()-cx, dy = e.getY()-cy;
            float d = (float)Math.sqrt(dx*dx+dy*dy);
            if (d <= rangePixels) {
                float traveled = e.distanceTraveled();
                if (traveled > bestDist) { bestDist = traveled; best = e; }
            }
        }
        return best;
    }

    public void draw(Graphics2D g) {
        int px = col * Constants.TILE, py = row * Constants.TILE;
        int cx = px + Constants.TILE/2, cy = py + Constants.TILE/2;

        // Tile base - wooden platform
        g.setColor(new Color(0x5D4037));
        g.fillRect(px+2, py+2, Constants.TILE-4, Constants.TILE-4);
        g.setColor(new Color(0x4E342E));
        g.drawRect(px+2, py+2, Constants.TILE-4, Constants.TILE-4);

        // Level stars
        for (int i = 0; i < level; i++) {
            g.setColor(new Color(0xFFD700));
            g.fillRect(px+4+i*7, py+Constants.TILE-8, 5, 5);
        }

        // Draw monkey body
        drawMonkeyBody(g, cx, cy);

        // Barrel / aim indicator (rotates)
        Graphics2D g2 = (Graphics2D) g.create();
        g2.translate(cx, cy);
        g2.rotate(angle);
        drawBarrel(g2);
        g2.dispose();

        // Selection ring
        if (selected) {
            g.setColor(new Color(255,255,0,160));
            g.setStroke(new BasicStroke(2, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 0, new float[]{4,4}, 0));
            int rangePixels = (int)(range * Constants.TILE);
            g.drawOval(cx - rangePixels, cy - rangePixels, rangePixels*2, rangePixels*2);
            g.setStroke(new BasicStroke(1));
        }
    }

    /** Draws a cute monkey face centered at (cx, cy) */
    private void drawMonkeyBody(Graphics2D g, int cx, int cy) {
        switch (ttype) {
            case DART:   drawDartMonkey(g, cx, cy);   break;
            case BOMB:   drawBombMonkey(g, cx, cy);   break;
            case ICE:    drawIceMonkey(g, cx, cy);    break;
            case SUPER:  drawSuperMonkey(g, cx, cy);  break;
            case MORTAR: drawMortarMonkey(g, cx, cy); break;
        }
    }

    // ---------- Dart Monkey: classic red fez, dart in hand ----------
    private void drawDartMonkey(Graphics2D g, int cx, int cy) {
        // Body
        g.setColor(new Color(0xC68642)); // tan fur
        g.fillOval(cx-10, cy-9, 20, 18);
        // Face (lighter)
        g.setColor(new Color(0xF5CBA7));
        g.fillOval(cx-7, cy-5, 14, 12);
        // Eyes
        g.setColor(Color.BLACK);
        g.fillOval(cx-5, cy-3, 3, 3);
        g.fillOval(cx+2, cy-3, 3, 3);
        // Nose
        g.setColor(new Color(0xA0522D));
        g.fillOval(cx-2, cy+1, 4, 3);
        // Fez hat (red)
        g.setColor(new Color(0xC0392B));
        int[] hx = {cx-6, cx+6, cx+4, cx-4};
        int[] hy = {cy-9, cy-9, cy-14, cy-14};
        g.fillPolygon(hx, hy, 4);
        g.setColor(new Color(0x7B241C));
        g.fillRect(cx-7, cy-10, 14, 3);
        // Hat tassel
        g.setColor(new Color(0xF39C12));
        g.fillOval(cx-1, cy-16, 3, 3);
        // Outline
        g.setColor(new Color(0x7D5A3C));
        g.setStroke(new BasicStroke(1.2f));
        g.drawOval(cx-10, cy-9, 20, 18);
        g.setStroke(new BasicStroke(1));
    }

    // ---------- Bomb Shooter: dark grey, helmet, stern face ----------
    private void drawBombMonkey(Graphics2D g, int cx, int cy) {
        // Body
        g.setColor(new Color(0x8D6E63)); // brownish
        g.fillOval(cx-11, cy-10, 22, 20);
        // Face
        g.setColor(new Color(0xF5CBA7));
        g.fillOval(cx-7, cy-5, 14, 12);
        // Eyes (angry slant)
        g.setColor(Color.BLACK);
        g.fillOval(cx-5, cy-4, 3, 3);
        g.fillOval(cx+2, cy-4, 3, 3);
        // Angry brows
        g.setColor(Color.BLACK);
        g.setStroke(new BasicStroke(1.5f));
        g.drawLine(cx-6, cy-7, cx-2, cy-5);
        g.drawLine(cx+6, cy-7, cx+2, cy-5);
        // Helmet (dark grey)
        g.setColor(new Color(0x37474F));
        g.fillArc(cx-11, cy-14, 22, 18, 0, 180);
        g.setColor(new Color(0x546E7A));
        g.setStroke(new BasicStroke(2));
        g.drawArc(cx-11, cy-14, 22, 18, 0, 180);
        g.setColor(new Color(0x90A4AE));
        g.setStroke(new BasicStroke(1.5f));
        g.drawLine(cx-11, cy-5, cx+11, cy-5); // brim
        g.setStroke(new BasicStroke(1));
    }

    // ---------- Ice Monkey: pale blue, snowflake crown ----------
    private void drawIceMonkey(Graphics2D g, int cx, int cy) {
        // Body (icy blue tint)
        g.setColor(new Color(0xB3E5FC));
        g.fillOval(cx-10, cy-9, 20, 18);
        // Face
        g.setColor(new Color(0xE1F5FE));
        g.fillOval(cx-7, cy-5, 14, 12);
        // Eyes (icy)
        g.setColor(new Color(0x0277BD));
        g.fillOval(cx-5, cy-3, 3, 3);
        g.fillOval(cx+2, cy-3, 3, 3);
        g.setColor(Color.WHITE);
        g.fillOval(cx-4, cy-4, 1, 1);
        g.fillOval(cx+3, cy-4, 1, 1);
        // Snowflake crown
        g.setColor(new Color(0xFFFFFF));
        g.setStroke(new BasicStroke(1.5f));
        for (int a = 0; a < 360; a += 60) {
            double rad = Math.toRadians(a);
            int ex = cx + (int)(7 * Math.cos(rad));
            int ey = (cy-12) + (int)(4 * Math.sin(rad));
            g.drawLine(cx, cy-12, ex, ey);
        }
        g.setColor(new Color(0x29B6F6));
        g.fillOval(cx-3, cy-15, 6, 6);
        // Outline
        g.setColor(new Color(0x0277BD));
        g.setStroke(new BasicStroke(1.2f));
        g.drawOval(cx-10, cy-9, 20, 18);
        g.setStroke(new BasicStroke(1));
    }

    // ---------- Super Monkey: golden, cape, heroic ----------
    private void drawSuperMonkey(Graphics2D g, int cx, int cy) {
        // Cape (behind body)
        g.setColor(new Color(0xB71C1C));
        int[] capex = {cx-8, cx+8, cx+12, cx-12};
        int[] capey = {cy-2, cy-2, cy+10, cy+10};
        g.fillPolygon(capex, capey, 4);
        // Body (golden)
        g.setColor(new Color(0xFFCA28));
        g.fillOval(cx-10, cy-10, 20, 20);
        // Face
        g.setColor(new Color(0xFFE082));
        g.fillOval(cx-7, cy-5, 14, 11);
        // Star mask eyes
        g.setColor(new Color(0xE65100));
        g.fillOval(cx-6, cy-4, 4, 3);
        g.fillOval(cx+2, cy-4, 4, 3);
        g.setColor(Color.WHITE);
        g.fillOval(cx-5, cy-4, 2, 2);
        g.fillOval(cx+3, cy-4, 2, 2);
        // Crown
        g.setColor(new Color(0xFFD600));
        int[] crownx = {cx-7, cx-5, cx-3, cx, cx+3, cx+5, cx+7, cx+7, cx-7};
        int[] crowny = {cy-10, cy-14, cy-10, cy-14, cy-10, cy-14, cy-10, cy-8, cy-8};
        g.fillPolygon(crownx, crowny, 9);
        g.setColor(new Color(0xE65100));
        g.setStroke(new BasicStroke(1.2f));
        g.drawPolygon(crownx, crowny, 9);
        // Outline
        g.setColor(new Color(0xE65100));
        g.setStroke(new BasicStroke(1.2f));
        g.drawOval(cx-10, cy-10, 20, 20);
        g.setStroke(new BasicStroke(1));
    }

    // ---------- Mortar Monkey: army helmet, camo, tough look ----------
    private void drawMortarMonkey(Graphics2D g, int cx, int cy) {
        // Body (camo green)
        g.setColor(new Color(0x558B2F));
        g.fillOval(cx-11, cy-10, 22, 20);
        // Face
        g.setColor(new Color(0xD7B58A));
        g.fillOval(cx-7, cy-5, 14, 12);
        // Camo patches on face
        g.setColor(new Color(0x33691E, true));
        g.fillOval(cx-5, cy-2, 4, 3);
        g.fillOval(cx+2, cy+1, 3, 3);
        // Eyes
        g.setColor(new Color(0x1B1B1B));
        g.fillOval(cx-5, cy-4, 3, 3);
        g.fillOval(cx+2, cy-4, 3, 3);
        // Army helmet (dark green dome)
        g.setColor(new Color(0x33691E));
        g.fillArc(cx-12, cy-16, 24, 20, 0, 180);
        g.setColor(new Color(0x1B5E20));
        g.setStroke(new BasicStroke(2));
        g.drawArc(cx-12, cy-16, 24, 20, 0, 180);
        g.drawLine(cx-13, cy-6, cx+13, cy-6); // brim
        // Star on helmet
        g.setColor(new Color(0xFFFFFF));
        g.setFont(new Font("SansSerif", Font.BOLD, 7));
        g.drawString("★", cx-3, cy-8);
        g.setStroke(new BasicStroke(1));
    }

    private void drawBarrel(Graphics2D g2) {
        switch (ttype) {
            case DART:
                // Thin dart
                g2.setColor(new Color(0x37474F));
                g2.setStroke(new BasicStroke(2));
                g2.drawLine(4, 0, 14, 0);
                g2.setColor(new Color(0xE53935));
                g2.fillPolygon(new int[]{14,18,14}, new int[]{-3,0,3}, 3); // dart tip
                break;
            case BOMB:
                // Short cannon barrel
                g2.setColor(new Color(0x263238));
                g2.setStroke(new BasicStroke(5));
                g2.drawLine(2, 0, 12, 0);
                g2.setColor(new Color(0x546E7A));
                g2.setStroke(new BasicStroke(3));
                g2.drawLine(2, 0, 10, 0);
                break;
            case ICE:
                // Snowflake wand
                g2.setColor(new Color(0x29B6F6));
                g2.setStroke(new BasicStroke(2));
                g2.drawLine(4, 0, 14, 0);
                g2.setColor(Color.WHITE);
                g2.fillOval(12, -3, 6, 6);
                break;
            case SUPER:
                // Energy beam emitter
                g2.setColor(new Color(0xFFCA28));
                g2.setStroke(new BasicStroke(3));
                g2.drawLine(4, 0, 16, 0);
                g2.setColor(new Color(0xFFFFFF));
                g2.setStroke(new BasicStroke(1));
                g2.fillOval(14, -3, 6, 6);
                break;
            case MORTAR:
                // Stubby mortar tube (points up, but rotates to aim)
                g2.setColor(new Color(0x4E342E));
                g2.setStroke(new BasicStroke(6));
                g2.drawLine(0, 0, 8, 0);
                g2.setColor(new Color(0x6D4C41));
                g2.setStroke(new BasicStroke(4));
                g2.drawLine(0, 0, 6, 0);
                break;
        }
        g2.setStroke(new BasicStroke(1));
    }

    public void drawRangePreview(Graphics2D g) {
        int cx = col * Constants.TILE + Constants.TILE/2;
        int cy = row * Constants.TILE + Constants.TILE/2;
        int rangePixels = (int)(range * Constants.TILE);
        g.setColor(new Color(255,255,255,30));
        g.fillOval(cx-rangePixels, cy-rangePixels, rangePixels*2, rangePixels*2);
        g.setColor(new Color(255,255,255,80));
        g.setStroke(new BasicStroke(1));
        g.drawOval(cx-rangePixels, cy-rangePixels, rangePixels*2, rangePixels*2);
    }

    // Getters
    public int getCol() { return col; }
    public int getRow() { return row; }
    public int getCost() { return cost; }
    public int getLevel() { return level; }
    public TType getTType() { return ttype; }
    public float getRange() { return range; }
    public float getDamage() { return damage; }
    public float getFireRate() { return fireRate; }
    public boolean isSelected() { return selected; }
    public void setSelected(boolean s) { selected = s; }

    public boolean canUpgrade() { return level < 3; }
    public int upgradeCost() { return level < 3 ? UPGRADE_COSTS[level-1] : 0; }
    public int sellValue() { return (int)(cost * 0.6f * level); }

    public void upgrade() {
        if (level < 3) { level++; applyStats(); }
    }

    public String getName() {
        switch(ttype) {
            case DART:   return "Dart Monkey";
            case BOMB:   return "Bomb Shooter";
            case ICE:    return "Ice Monkey";
            case SUPER:  return "Super Monkey";
            case MORTAR: return "Mortar Monkey";
            default: return "Monkey";
        }
    }
}
