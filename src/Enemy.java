import java.awt.*;
import java.util.List;

public class Enemy {
    public enum Type { GOBLIN, ORC, TROLL, DRAGON, SHADE }

    private Type type;
    private float x, y;
    private int pathIndex = 0;
    private float maxHp, hp;
    private float speed;
    private int reward;
    private int damage; // lives lost when reaching end
    private boolean dead = false;
    private boolean reached = false;
    private float slowTimer = 0;

    // Visual
    private Color baseColor;
    private float size;

    public Enemy(Type type) {
        this.type = type;
        int[][] path = Constants.PATH;
        x = path[0][0] * Constants.TILE + Constants.TILE / 2f;
        y = path[0][1] * Constants.TILE + Constants.TILE / 2f;
        switch (type) {
            case GOBLIN: maxHp=60;  speed=1.8f; reward=10; damage=1; baseColor=new Color(0x4CAF50); size=10; break;
            case ORC:    maxHp=180; speed=1.1f; reward=20; damage=2; baseColor=new Color(0x795548); size=14; break;
            case TROLL:  maxHp=500; speed=0.7f; reward=40; damage=3; baseColor=new Color(0x607D8B); size=18; break;
            case DRAGON: maxHp=350; speed=1.4f; reward=55; damage=3; baseColor=new Color(0xFF5722); size=16; break;
            case SHADE:  maxHp=120; speed=2.5f; reward=30; damage=2; baseColor=new Color(0x9C27B0); size=11; break;
        }
        hp = maxHp;
    }

    public void update() {
        if (slowTimer > 0) slowTimer--;

        float spd = slowTimer > 0 ? speed * 0.4f : speed;
        int[][] path = Constants.PATH;
        if (pathIndex >= path.length - 1) {
            reached = true;
            return;
        }
        float tx = path[pathIndex + 1][0] * Constants.TILE + Constants.TILE / 2f;
        float ty = path[pathIndex + 1][1] * Constants.TILE + Constants.TILE / 2f;
        float dx = tx - x, dy = ty - y;
        float dist = (float) Math.sqrt(dx * dx + dy * dy);
        if (dist < spd) {
            x = tx; y = ty;
            pathIndex++;
        } else {
            x += dx / dist * spd;
            y += dy / dist * spd;
        }
    }

    public void draw(Graphics2D g) {
        float r = size;
        // Shadow
        g.setColor(new Color(0,0,0,60));
        g.fillOval((int)(x - r + 2), (int)(y - r + 3), (int)(r*2), (int)(r*2));

        // Body
        Color body = slowTimer > 0 ? baseColor.darker() : baseColor;
        g.setColor(body);
        g.fillOval((int)(x - r), (int)(y - r), (int)(r*2), (int)(r*2));
        g.setColor(body.brighter());
        g.setStroke(new BasicStroke(1.5f));
        g.drawOval((int)(x - r), (int)(y - r), (int)(r*2), (int)(r*2));

        // Type-specific detail
        drawTypeDetail(g);

        // HP bar
        int barW = 28, barH = 4;
        int bx = (int)(x - barW/2f), by = (int)(y - r - 8);
        g.setColor(new Color(50,50,50,200));
        g.fillRect(bx, by, barW, barH);
        float hpRatio = hp / maxHp;
        Color hpColor = hpRatio > 0.6f ? new Color(0x4CAF50) : hpRatio > 0.3f ? new Color(0xFFC107) : new Color(0xF44336);
        g.setColor(hpColor);
        g.fillRect(bx, by, (int)(barW * hpRatio), barH);
    }

    private void drawTypeDetail(Graphics2D g) {
        int ix = (int)x, iy = (int)y;
        g.setColor(Color.WHITE);
        g.setFont(new Font("SansSerif", Font.BOLD, 8));
        FontMetrics fm = g.getFontMetrics();
        String label = type.name().substring(0,1);
        g.drawString(label, ix - fm.stringWidth(label)/2, iy + 3);
    }

    // Accessors
    public float getX() { return x; }
    public float getY() { return y; }
    public boolean isDead() { return dead; }
    public boolean hasReached() { return reached; }
    public int getReward() { return reward; }
    public int getDamage() { return damage; }
    public float getHp() { return hp; }
    public float getMaxHp() { return maxHp; }
    public Type getType() { return type; }

    public float distanceTraveled() {
        int[][] path = Constants.PATH;
        float d = 0;
        for (int i = 1; i <= pathIndex && i < path.length; i++) {
            float ax = path[i-1][0]*Constants.TILE + Constants.TILE/2f;
            float ay = path[i-1][1]*Constants.TILE + Constants.TILE/2f;
            float bx = path[i][0]*Constants.TILE + Constants.TILE/2f;
            float by = path[i][1]*Constants.TILE + Constants.TILE/2f;
            d += Math.sqrt((bx-ax)*(bx-ax)+(by-ay)*(by-ay));
        }
        // add partial
        if (pathIndex < path.length-1) {
            float tx = path[pathIndex+1][0]*Constants.TILE+Constants.TILE/2f;
            float ty = path[pathIndex+1][1]*Constants.TILE+Constants.TILE/2f;
            float cx = path[pathIndex][0]*Constants.TILE+Constants.TILE/2f;
            float cy = path[pathIndex][1]*Constants.TILE+Constants.TILE/2f;
            float seg = (float)Math.sqrt((tx-cx)*(tx-cx)+(ty-cy)*(ty-cy));
            float cur = (float)Math.sqrt((x-cx)*(x-cx)+(y-cy)*(y-cy));
            d += Math.min(cur, seg);
        }
        return d;
    }

    public void takeDamage(float dmg) {
        hp -= dmg;
        if (hp <= 0) dead = true;
    }

    public void applySlow(float ticks) {
        slowTimer = Math.max(slowTimer, ticks);
    }
}