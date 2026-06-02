import java.awt.*;

public class Projectile {
    public enum PType { ARROW, CANNONBALL, FROST_BOLT, LASER_BEAM, MORTAR_SHELL }

    private float x, y;
    private Enemy target;
    private float damage;
    private float speed;
    private PType ptype;
    private boolean done = false;
    private float splashRadius; // for cannon/mortar
    private java.util.List<Enemy> allEnemies;

    // Laser: start point
    private float sx, sy;
    private boolean isLaser = false;

    public Projectile(float x, float y, Enemy target, float damage, float speed, PType ptype, float splashRadius, java.util.List<Enemy> allEnemies) {
        this.x = x; this.y = y;
        this.sx = x; this.sy = y;
        this.target = target;
        this.damage = damage;
        this.speed = speed;
        this.ptype = ptype;
        this.splashRadius = splashRadius;
        this.allEnemies = allEnemies;
        if (ptype == PType.LASER_BEAM) isLaser = true;
    }

    public void update() {
        if (done) return;
        if (isLaser) {
            // Laser hits instantly
            applyHit();
            done = true;
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
                    float dx = e.getX() - tx, dy = e.getY() - ty;
                    if (Math.sqrt(dx*dx+dy*dy) <= splashRadius) {
                        e.takeDamage(damage);
                    }
                }
            }
        } else {
            if (!target.isDead() && !target.hasReached()) target.takeDamage(damage);
        }
    }

    public void draw(Graphics2D g) {
        if (done) return;
        switch (ptype) {
            case ARROW:
                g.setColor(new Color(0xA0522D));
                g.setStroke(new BasicStroke(2));
                if (!target.isDead()) {
                    float dx = target.getX()-x, dy=target.getY()-y;
                    float len=10, d=(float)Math.sqrt(dx*dx+dy*dy);
                    if(d>0){g.drawLine((int)x,(int)y,(int)(x+dx/d*len),(int)(y+dy/d*len));}
                }
                g.fillOval((int)x-3,(int)y-3,6,6);
                break;
            case CANNONBALL:
                g.setColor(Color.DARK_GRAY);
                g.fillOval((int)x-5,(int)y-5,10,10);
                g.setColor(Color.GRAY);
                g.fillOval((int)x-3,(int)y-4,4,4);
                break;
            case FROST_BOLT:
                g.setColor(new Color(0x00BFFF,true));
                g.fillOval((int)x-4,(int)y-4,8,8);
                g.setColor(Color.WHITE);
                g.fillOval((int)x-2,(int)y-2,4,4);
                break;
            case LASER_BEAM:
                // drawn as line from sx,sy to target
                if (!target.isDead()) {
                    g.setColor(new Color(255,50,50,200));
                    g.setStroke(new BasicStroke(3));
                    g.drawLine((int)sx,(int)sy,(int)target.getX(),(int)target.getY());
                    g.setColor(new Color(255,200,200,120));
                    g.setStroke(new BasicStroke(7));
                    g.drawLine((int)sx,(int)sy,(int)target.getX(),(int)target.getY());
                }
                break;
            case MORTAR_SHELL:
                g.setColor(new Color(0xD4A017));
                g.fillOval((int)x-4,(int)y-4,8,8);
                g.setColor(new Color(0x8B6914));
                g.drawOval((int)x-4,(int)y-4,8,8);
                break;
        }
        g.setStroke(new BasicStroke(1));
    }

    public boolean isDone() { return done; }
}