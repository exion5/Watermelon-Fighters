import java.awt.*;
import java.util.List;

public class Tower {
    public enum TType { ARCHER, CANNON, FROST, LASER, MORTAR }

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
            case ARCHER: cost=80;  range=3.5f; damage=15;  fireRate=40;  color=new Color(0x8B4513); break;
            case CANNON: cost=150; range=2.5f; damage=60;  fireRate=90;  color=new Color(0x555555); break;
            case FROST:  cost=120; range=3.0f; damage=8;   fireRate=50;  color=new Color(0x00BFFF); break;
            case LASER:  cost=200; range=4.0f; damage=25;  fireRate=20;  color=new Color(0xFF2222); break;
            case MORTAR: cost=175; range=5.0f; damage=80;  fireRate=120; color=new Color(0x8B6914); break;
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
            case CANNON: pt = Projectile.PType.CANNONBALL; splash = 40; break;
            case FROST:  pt = Projectile.PType.FROST_BOLT; break;
            case LASER:  pt = Projectile.PType.LASER_BEAM; break;
            case MORTAR: pt = Projectile.PType.MORTAR_SHELL; splash = 60; break;
            default:     pt = Projectile.PType.ARROW; break;
        }
        float projSpeed = ttype == TType.LASER ? 0 : ttype == TType.MORTAR ? 4f : 7f;
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

        // Tile base
        g.setColor(new Color(0x5D4037));
        g.fillRect(px+2, py+2, Constants.TILE-4, Constants.TILE-4);
        g.setColor(new Color(0x4E342E));
        g.drawRect(px+2, py+2, Constants.TILE-4, Constants.TILE-4);

        // Level stars
        for (int i = 0; i < level; i++) {
            g.setColor(new Color(0xFFD700));
            g.fillRect(px+4+i*7, py+Constants.TILE-8, 5, 5);
        }

        // Tower body
        g.setColor(color);
        g.fillOval(cx-10, cy-10, 20, 20);
        g.setColor(color.brighter());
        g.setStroke(new BasicStroke(2));
        g.drawOval(cx-10, cy-10, 20, 20);

        // Barrel (rotates)
        Graphics2D g2 = (Graphics2D) g.create();
        g2.translate(cx, cy);
        g2.rotate(angle);
        g2.setColor(color.darker());
        g2.setStroke(new BasicStroke(3));
        int barrelLen = ttype == TType.MORTAR ? 6 : 14;
        g2.drawLine(0, 0, barrelLen, 0);
        g2.setColor(color);
        g2.setStroke(new BasicStroke(2));
        g2.drawLine(0, 0, barrelLen-2, 0);
        g2.dispose();

        // Type icon
        g.setColor(Color.WHITE);
        g.setFont(new Font("Monospaced", Font.BOLD, 8));
        String icon = ttype.name().substring(0,1);
        FontMetrics fm = g.getFontMetrics();
        g.drawString(icon, cx - fm.stringWidth(icon)/2, cy + 3);

        // Selection ring
        if (selected) {
            g.setColor(new Color(255,255,0,160));
            g.setStroke(new BasicStroke(2, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 0, new float[]{4,4}, 0));
            int rangePixels = (int)(range * Constants.TILE);
            g.drawOval(cx - rangePixels, cy - rangePixels, rangePixels*2, rangePixels*2);
            g.setStroke(new BasicStroke(1));
        }
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
            case ARCHER: return "Archer";
            case CANNON: return "Cannon";
            case FROST:  return "Frost";
            case LASER:  return "Laser";
            case MORTAR: return "Mortar";
            default: return "Tower";
        }
    }
}