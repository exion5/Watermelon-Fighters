import java.awt.*;

public class Projectile {
    public enum PType { DART, CANNONBALL, FROST_BOLT, LASER_BEAM, MORTAR_SHELL }

    private float x, y;
    private Enemy target;
    private float damage;
    private float speed;
    private PType ptype;
    private boolean done = false;
    private float splashRadius; // for cannon/mortar
    private java.util.List<Enemy> allEnemies;

    // Laser/Super: start point
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
            case DART:
                // Dart: thin pointed projectile
                g.setColor(new Color(0x37474F));
                g.setStroke(new BasicStroke(2));
                if (!target.isDead()) {
                    float dx = target.getX()-x, dy=target.getY()-y;
                    float len=12, d=(float)Math.sqrt(dx*dx+dy*dy);
                    if(d>0){
                        float ex = x+dx/d*len, ey = y+dy/d*len;
                        g.drawLine((int)x,(int)y,(int)ex,(int)ey);
                        // tip
                        g.setColor(new Color(0xE53935));
                        g.fillOval((int)ex-2,(int)ey-2,5,5);
                    }
                }
                g.setColor(new Color(0xA0522D));
                g.fillOval((int)x-2,(int)y-2,5,5);
                break;
            case CANNONBALL:
                // Bomb: black ball with fuse
                g.setColor(Color.BLACK);
                g.fillOval((int)x-6,(int)y-6,12,12);
                g.setColor(new Color(0x555555));
                g.fillOval((int)x-4,(int)y-5,5,5);
                // Fuse spark
                g.setColor(new Color(0xFF6F00));
                g.fillOval((int)x-1,(int)y-8,3,3);
                break;
            case FROST_BOLT:
                // Ice shard
                g.setColor(new Color(0x29B6F6));
                g.fillOval((int)x-5,(int)y-5,10,10);
                g.setColor(new Color(0xE1F5FE));
                g.fillOval((int)x-2,(int)y-2,5,5);
                // sparkle
                g.setColor(Color.WHITE);
                g.setStroke(new BasicStroke(1.2f));
                g.drawLine((int)x-4,(int)y,(int)x+4,(int)y);
                g.drawLine((int)x,(int)y-4,(int)x,(int)y+4);
                break;
            case LASER_BEAM:
                // Super Monkey energy beam
                if (!target.isDead()) {
                    g.setColor(new Color(255,200,0,200));
                    g.setStroke(new BasicStroke(3));
                    g.drawLine((int)sx,(int)sy,(int)target.getX(),(int)target.getY());
                    g.setColor(new Color(255,255,150,100));
                    g.setStroke(new BasicStroke(7));
                    g.drawLine((int)sx,(int)sy,(int)target.getX(),(int)target.getY());
                }
                break;
            case MORTAR_SHELL:
                // Mortar shell
                g.setColor(new Color(0x558B2F));
                g.fillOval((int)x-4,(int)y-4,8,8);
                g.setColor(new Color(0x33691E));
                g.drawOval((int)x-4,(int)y-4,8,8);
                g.setColor(new Color(0xFFD600));
                g.fillOval((int)x-1,(int)y-6,3,3);
                break;
        }
        g.setStroke(new BasicStroke(1));
    }

    public boolean isDone() { return done; }
}
