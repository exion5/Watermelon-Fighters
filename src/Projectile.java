import java.awt.*;
import java.awt.geom.*;

public class Projectile {
    public enum PType { DART, CANNONBALL, FROST_BOLT, LASER_BEAM, MORTAR_SHELL }

    private float x, y;
    private Enemy target;
    private float damage;
    private float speed;
    private PType ptype;
    private boolean done = false;
    private float splashRadius;
    private java.util.List<Enemy> allEnemies;

    // Origin point (all projectile types)
    private float sx, sy;

    // ── Laser animation state ──────────────────────────────────────────
    private static final int LASER_HOLD_TICKS  = 10; // frames beam stays visible
    private static final int LASER_FADE_TICKS  = 8;  // frames it fades out
    private int  laserTick   = 0;                     // counts up each update()
    private boolean laserHit = false;                 // damage already applied
    // Saved target position (target may die mid-animation)
    private float laserTX, laserTY;

    public Projectile(float x, float y, Enemy target, float damage, float speed,
                      PType ptype, float splashRadius, java.util.List<Enemy> allEnemies) {
        this.x = x; this.y = y;
        this.sx = x; this.sy = y;
        this.target = target;
        this.damage = damage;
        this.speed  = speed;
        this.ptype  = ptype;
        this.splashRadius = splashRadius;
        this.allEnemies = allEnemies;
        if (ptype == PType.LASER_BEAM) {
            laserTX = target.getX();
            laserTY = target.getY();
        }
    }

    public void update() {
        if (done) return;

        if (ptype == PType.LASER_BEAM) {
            // Keep tracking live target during hold phase
            if (laserTick < LASER_HOLD_TICKS && !target.isDead() && !target.hasReached()) {
                laserTX = target.getX();
                laserTY = target.getY();
            }
            // Apply damage once on first tick
            if (!laserHit) {
                applyHit();
                laserHit = true;
            }
            laserTick++;
            if (laserTick >= LASER_HOLD_TICKS + LASER_FADE_TICKS) done = true;
            return;
        }

        if (target.isDead() || target.hasReached()) { done = true; return; }
        float dx = target.getX() - x, dy = target.getY() - y;
        float dist = (float) Math.sqrt(dx*dx + dy*dy);
        if (dist < speed + 2) {
            applyHit();
            done = true;
        } else {
            x += dx/dist * speed;
            y += dy/dist * speed;
        }
    }

    private void applyHit() {
        if (ptype == PType.FROST_BOLT) {
            if (!target.isDead() && !target.hasReached()) {
                target.takeDamage(damage);
                target.applySlow(120);
            }
        } else if (splashRadius > 0) {
            float tx = target.isDead() ? x : target.getX();
            float ty = target.isDead() ? y : target.getY();
            for (Enemy e : allEnemies) {
                if (!e.isDead() && !e.hasReached()) {
                    float dx = e.getX()-tx, dy = e.getY()-ty;
                    if (Math.sqrt(dx*dx+dy*dy) <= splashRadius) e.takeDamage(damage);
                }
            }
        } else {
            if (!target.isDead() && !target.hasReached()) target.takeDamage(damage);
        }
    }

    public void draw(Graphics2D g) {
        if (done) return;
        switch (ptype) {
            case DART:        drawDart(g);       break;
            case CANNONBALL:  drawCannonball(g); break;
            case FROST_BOLT:  drawFrostBolt(g);  break;
            case LASER_BEAM:  drawLaser(g);      break;
            case MORTAR_SHELL:drawMortar(g);     break;
        }
        g.setStroke(new BasicStroke(1));
    }

    // ── Projectile draw helpers ────────────────────────────────────────

    private void drawDart(Graphics2D g) {
        g.setColor(new Color(0x37474F));
        g.setStroke(new BasicStroke(2));
        if (!target.isDead()) {
            float dx = target.getX()-x, dy = target.getY()-y;
            float len = 12, d = (float)Math.sqrt(dx*dx+dy*dy);
            if (d > 0) {
                float ex = x+dx/d*len, ey = y+dy/d*len;
                g.drawLine((int)x,(int)y,(int)ex,(int)ey);
                g.setColor(new Color(0xE53935));
                g.fillOval((int)ex-2,(int)ey-2,5,5);
            }
        }
        g.setColor(new Color(0xA0522D));
        g.fillOval((int)x-2,(int)y-2,5,5);
    }

    private void drawCannonball(Graphics2D g) {
        g.setColor(Color.BLACK);
        g.fillOval((int)x-6,(int)y-6,12,12);
        g.setColor(new Color(0x555555));
        g.fillOval((int)x-4,(int)y-5,5,5);
        g.setColor(new Color(0xFF6F00));
        g.fillOval((int)x-1,(int)y-8,3,3);
    }

    private void drawFrostBolt(Graphics2D g) {
        g.setColor(new Color(0x29B6F6));
        g.fillOval((int)x-5,(int)y-5,10,10);
        g.setColor(new Color(0xE1F5FE));
        g.fillOval((int)x-2,(int)y-2,5,5);
        g.setColor(Color.WHITE);
        g.setStroke(new BasicStroke(1.2f));
        g.drawLine((int)x-4,(int)y,(int)x+4,(int)y);
        g.drawLine((int)x,(int)y-4,(int)x,(int)y+4);
    }

    private void drawMortar(Graphics2D g) {
        g.setColor(new Color(0x558B2F));
        g.fillOval((int)x-4,(int)y-4,8,8);
        g.setColor(new Color(0x33691E));
        g.drawOval((int)x-4,(int)y-4,8,8);
        g.setColor(new Color(0xFFD600));
        g.fillOval((int)x-1,(int)y-6,3,3);
    }

    // ── Laser beam animation ───────────────────────────────────────────
    private void drawLaser(Graphics2D g) {
        int totalTicks = LASER_HOLD_TICKS + LASER_FADE_TICKS;

        // Alpha: full during hold, then linear fade
        float alpha;
        if (laserTick < LASER_HOLD_TICKS) {
            alpha = 1.0f;
        } else {
            alpha = 1.0f - (float)(laserTick - LASER_HOLD_TICKS) / LASER_FADE_TICKS;
        }
        alpha = Math.max(0, Math.min(1, alpha));

        // Flicker during hold phase — fast pulse using sine
        float flicker = 1.0f;
        if (laserTick < LASER_HOLD_TICKS) {
            flicker = 0.75f + 0.25f * (float)Math.abs(Math.sin(laserTick * 1.8));
        }
        float intensity = alpha * flicker;

        int tx = (int)laserTX, ty = (int)laserTY;
        int ox = (int)sx,      oy = (int)sy;

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // ── Layer 1: wide outer glow (gold/white) ──────────────────────
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, intensity * 0.18f));
        g2.setColor(new Color(255, 240, 100));
        g2.setStroke(new BasicStroke(18, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.drawLine(ox, oy, tx, ty);

        // ── Layer 2: mid glow (golden yellow) ─────────────────────────
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, intensity * 0.40f));
        g2.setColor(new Color(255, 200, 0));
        g2.setStroke(new BasicStroke(8, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.drawLine(ox, oy, tx, ty);

        // ── Layer 3: core beam (bright white-yellow) ───────────────────
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, intensity * 0.90f));
        g2.setColor(new Color(255, 255, 180));
        g2.setStroke(new BasicStroke(3, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.drawLine(ox, oy, tx, ty);

        // ── Layer 4: hot white centre ──────────────────────────────────
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, intensity));
        g2.setColor(Color.WHITE);
        g2.setStroke(new BasicStroke(1.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.drawLine(ox, oy, tx, ty);

        // ── Impact flash at target end (hold phase only) ───────────────
        if (laserTick < LASER_HOLD_TICKS) {
            float flashSize = 6 + 4 * flicker;
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, intensity * 0.85f));
            // Outer flash
            g2.setColor(new Color(255, 220, 50, (int)(180 * intensity)));
            g2.fillOval((int)(tx - flashSize), (int)(ty - flashSize),
                        (int)(flashSize*2), (int)(flashSize*2));
            // Inner hot spot
            g2.setColor(new Color(255, 255, 220, (int)(230 * intensity)));
            g2.fillOval((int)(tx - flashSize*0.4f), (int)(ty - flashSize*0.4f),
                        (int)(flashSize*0.8f), (int)(flashSize*0.8f));
        }

        // ── Muzzle flash at origin ─────────────────────────────────────
        if (laserTick < LASER_HOLD_TICKS) {
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, intensity * 0.7f));
            g2.setColor(new Color(255, 240, 120));
            float mf = 5 + 2 * flicker;
            g2.fillOval((int)(ox - mf), (int)(oy - mf), (int)(mf*2), (int)(mf*2));
        }

        g2.dispose();
    }

    public boolean isDone() { return done; }
}