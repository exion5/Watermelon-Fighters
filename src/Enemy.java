import java.awt.*;
import java.awt.geom.*;
import java.util.List;
import java.util.ArrayList;

public class Enemy {
    public enum Type {
        SEEDLING, RIND, MELON, GIANTMELON, BLACKSEED
    }

    public static int[][] activePath = Constants.PATH;

    private Type type;
    private float x, y;
    private int pathIndex = 0;
    private float maxHp, hp;
    private float speed;
    private int reward;
    private int damage;
    private boolean dead = false;
    private boolean reached = false;
    private float slowTimer = 0;

    private Color baseColor;
    private float size;

    private float poisonDmg   = 0;
    private int   poisonTimer = 0;
    private static final int POISON_TICK = 20; // deal damage every 20 frames
    private int poisonTickTimer = 0;

    private float animTick   = 0; // increments every update; drives all animation
    private float bobPhase   = 0; // per-enemy random phase offset so they don't all sync
    private float deathTimer = 0; // counts up after dead=true; drives death shrink/fade
    private static final float DEATH_DURATION = 20f;

    private static class Particle { // death particles for explosion
        float x, y, vx, vy, life, maxLife;
        Color color;
        Particle(float x, float y, float vx, float vy, float life, Color c) {
            this.x=x; this.y=y; this.vx=vx; this.vy=vy;
            this.life=life; this.maxLife=life; this.color=c;
        }
    }
    private List<Particle> particles = new ArrayList<>();
    private boolean particlesSpawned = false;

    public Enemy(Type type) { // spawns at start of path
        this.type = type;
        int[][] path = activePath;
        x = path[0][0] * Constants.TILE + Constants.TILE / 2f;
        y = path[0][1] * Constants.TILE + Constants.TILE / 2f;
        bobPhase = (float)(Math.random() * Math.PI * 2);
        switch (type) {
            case SEEDLING:   maxHp=80;   speed=1.9f; reward=8;  damage=1; baseColor=new Color(0x4CAF50); size=10; break;
            case RIND:       maxHp=300;  speed=1.3f; reward=15; damage=2; baseColor=new Color(0x2E7D32); size=14; break;
            case MELON:      maxHp=1200; speed=0.9f; reward=25; damage=4; baseColor=new Color(0xC62828); size=18; break;
            case GIANTMELON: maxHp=900;  speed=1.8f; reward=35; damage=4; baseColor=new Color(0x1B5E20); size=16; break;
            case BLACKSEED:  maxHp=280;  speed=3.2f; reward=18; damage=3; baseColor=new Color(0x1A1A1A); size=11; break;
        }
        hp = maxHp;
    }

    public void update() {
        animTick++;

        // Update particles even when dead
        for (Particle p : particles) {
            p.x += p.vx; p.y += p.vy;
            p.vy += 0.08f; // gravity
            p.life--;
        }
        particles.removeIf(p -> p.life <= 0);

        if (dead) {
            deathTimer++;
            if (!particlesSpawned) spawnDeathParticles();
            return;
        }

        if (slowTimer > 0) slowTimer--;

        float spd = speed;
        if (slowTimer > 0) spd *= 0.4f;
        int[][] path = activePath;
        if (pathIndex >= path.length - 1) { reached = true; return; }
        float tx = path[pathIndex + 1][0] * Constants.TILE + Constants.TILE / 2f;
        float ty = path[pathIndex + 1][1] * Constants.TILE + Constants.TILE / 2f;
        float dx = tx - x, dy = ty - y;
        float dist = (float) Math.sqrt(dx*dx + dy*dy);
        if (dist < spd) { x = tx; y = ty; pathIndex++; }
        else { x += dx/dist*spd; y += dy/dist*spd; }

        if (poisonTimer > 0) {
            poisonTimer--;
            poisonTickTimer++;
            if (poisonTickTimer >= POISON_TICK) {
                poisonTickTimer = 0;
                takeDamage(poisonDmg);
            }
        }
    }

    private void spawnDeathParticles() { // Burst of particles in a random spread, colour based on enemy type
        particlesSpawned = true;
        int count = type == Type.MELON ? 18 : type == Type.GIANTMELON ? 22 : 12;
        for (int i = 0; i < count; i++) {
            float angle = (float)(Math.random() * Math.PI * 2);
            float spd   = 0.8f + (float)(Math.random() * 2.5f);
            float life  = 14 + (float)(Math.random() * 14);
            int r = Math.min(255, baseColor.getRed()   + (int)(Math.random()*60 - 30));
            int g2 = Math.min(255, baseColor.getGreen() + (int)(Math.random()*60 - 30));
            int b = Math.min(255, baseColor.getBlue()  + (int)(Math.random()*60 - 30));
            particles.add(new Particle(x, y,
                (float)Math.cos(angle)*spd, (float)Math.sin(angle)*spd - 1.5f,
                life, new Color(Math.max(0,r), Math.max(0,g2), Math.max(0,b))));
        }
        for (int i = 0; i < 5; i++) { // Extra burst of bright yellow particles for non-trolls/dragons
            float angle = (float)(Math.random()*Math.PI*2);
            float spd   = 1.0f + (float)(Math.random()*2.0f);
            particles.add(new Particle(x, y,
                (float)Math.cos(angle)*spd, (float)Math.sin(angle)*spd - 2f,
                20, new Color(0xFFD700)));
        }
    }

    public boolean isFullyGone() { // after death animation is done and all particles are gone, we can remove this enemy from the list
        return dead && deathTimer >= DEATH_DURATION && particles.isEmpty();
    }

    public void applyPoison(float dmgPerTick, int duration) {
        poisonDmg   = Math.max(poisonDmg, dmgPerTick);
        poisonTimer = Math.max(poisonTimer, duration);
    }

    public void draw(Graphics2D g) { // Draw enemy and its particles; if dead, draw death animation instead
        for (Particle p : particles) {
            float alpha = p.life / p.maxLife;
            int a = (int)(alpha * 220);
            g.setColor(new Color(p.color.getRed(), p.color.getGreen(), p.color.getBlue(), Math.max(0,a)));
            float r = 2.5f * alpha;
            g.fillOval((int)(p.x-r),(int)(p.y-r),(int)(r*2+1),(int)(r*2+1));
        }

        if (dead) { // shrink and fade out
            float t = deathTimer / DEATH_DURATION;
            if (t >= 1f) return;
            float scale = 1f - t;
            float alpha = 1f - t;
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
            g2.translate(x, y);
            g2.scale(scale, scale);
            g2.translate(-x, -y);
            drawBody(g2);
            g2.dispose();
            return;
        }

        drawBody(g);
    }

    private void drawBody(Graphics2D g) { // bobbing animation based on tick and speed
        float bobFreq = 0.12f + speed * 0.04f;
        float bob = (float)Math.sin(animTick * bobFreq + bobPhase) * 1.8f;

        Color body = baseColor; // tinted pulse when slowed
        if (slowTimer > 0) {
            float amount = 0.45f + 0.2f * (float)Math.sin(animTick * 0.3f);
            body = blend(baseColor, new Color(0x80D8FF), amount);
        }
        if (poisonTimer > 0) {
            float amount = 0.35f + 0.15f*(float)Math.sin(animTick*0.25f);
            body = blend(body, new Color(0x76FF03), amount);
        }

        float r = size;
        float ey = y + bob; // bobbing y

        g.setColor(new Color(0, 0, 0, 55)); // shadow
        g.fillOval((int)(x - r*0.9f + 2), (int)(ey + r*0.6f), (int)(r*1.8f), (int)(r*0.7f));

        switch (type) { // main body of the enemy
            case SEEDLING:   drawSeedling(g, x, ey, r, body); break;
            case RIND:       drawRind(g, x, ey, r, body);    break;
            case MELON:      drawMelon(g, x, ey, r, body);  break;
            case GIANTMELON: drawGiantmelon(g, x, ey, r, body); break;
            case BLACKSEED:  drawBlackseed(g, x, ey, r, body);  break;
        }

        drawHpBar(g, x, ey, r);
    }

    private void drawSeedling(Graphics2D g, float x, float y, float r, Color body) {
        // Teardrop seed shape
        g.setColor(body.darker());
        g.fillOval((int)(x-r+1), (int)(y-r+1), (int)(r*2-2), (int)(r*2+3));
        g.setColor(body);
        g.fillOval((int)(x-r+2), (int)(y-r), (int)(r*2-4), (int)(r*2+2));

        // White seed stripe
        g.setColor(new Color(255, 255, 255, 120));
        g.fillOval((int)(x-r*0.15f), (int)(y-r*0.6f), (int)(r*0.3f), (int)(r*0.9f));

        // Tiny sprout on top
        g.setColor(new Color(0x2E7D32));
        g.setStroke(new BasicStroke(1.5f));
        g.drawLine((int)x, (int)(y-r), (int)(x-3), (int)(y-r-5));
        g.drawLine((int)x, (int)(y-r), (int)(x+2), (int)(y-r-4));
        g.setStroke(new BasicStroke(1));

        // Dot eyes
        g.setColor(Color.BLACK);
        g.fillOval((int)(x-r*0.3f-1), (int)(y-r*0.2f-1), 3, 3);
        g.fillOval((int)(x+r*0.15f), (int)(y-r*0.2f-1), 3, 3);
    }

    private void drawRind(Graphics2D g, float x, float y, float r, Color body) {
        // Dark green outer rind
        g.setColor(new Color(0x1B5E20));
        g.fillOval((int)(x-r), (int)(y-r), (int)(r*2), (int)(r*2));

        // White rind layer
        g.setColor(new Color(0xDCEDC8));
        g.fillOval((int)(x-r*0.82f), (int)(y-r*0.82f), (int)(r*1.64f), (int)(r*1.64f));

        // Red flesh
        g.setColor(body); // dark green color set earlier → override
        g.setColor(new Color(0xC62828));
        g.fillOval((int)(x-r*0.62f), (int)(y-r*0.62f), (int)(r*1.24f), (int)(r*1.24f));

        // Seeds
        g.setColor(new Color(0x111111));
        g.fillOval((int)(x-r*0.3f-2), (int)(y-2), 4, 6);
        g.fillOval((int)(x+r*0.15f), (int)(y-3), 4, 6);

        // Angry brow lines
        g.setColor(new Color(0x1B5E20));
        g.setStroke(new BasicStroke(2f));
        g.drawLine((int)(x-r*0.5f), (int)(y-r*0.55f), (int)(x-r*0.15f), (int)(y-r*0.4f));
        g.drawLine((int)(x+r*0.15f), (int)(y-r*0.4f), (int)(x+r*0.5f), (int)(y-r*0.55f));
        g.setStroke(new BasicStroke(1));
    }

    private void drawMelon(Graphics2D g, float x, float y, float r, Color body) {
        // Dark green base
        g.setColor(new Color(0x1B5E20));
        g.fillOval((int)(x-r), (int)(y-r), (int)(r*2), (int)(r*2));

        // Light green stripes
        g.setColor(new Color(0x66BB6A));
        for (int i = -1; i <= 1; i++) {
            g.fillOval((int)(x + i*r*0.5f - r*0.15f), (int)(y-r), (int)(r*0.3f), (int)(r*2));
        }

        // Outline
        g.setColor(new Color(0x111111));
        g.setStroke(new BasicStroke(2f));
        g.drawOval((int)(x-r), (int)(y-r), (int)(r*2), (int)(r*2));
        g.setStroke(new BasicStroke(1));

        // Grumpy eyes
        float eo = r * 0.3f;
        g.setColor(new Color(0xFFEB3B));
        g.fillOval((int)(x-eo-3), (int)(y-r*0.15f-3), 7, 6);
        g.fillOval((int)(x+eo-3), (int)(y-r*0.15f-3), 7, 6);
        g.setColor(new Color(0x111100));
        g.fillOval((int)(x-eo-1), (int)(y-r*0.15f-1), 4, 4);
        g.fillOval((int)(x+eo-1), (int)(y-r*0.15f-1), 4, 4);

        // Curl vine on top
        g.setColor(new Color(0x2E7D32));
        g.setStroke(new BasicStroke(1.5f));
        g.drawArc((int)(x-r*0.2f), (int)(y-r-6), (int)(r*0.4f), 8, 0, 200);
        g.setStroke(new BasicStroke(1));
    }

    private void drawGiantmelon(Graphics2D g, float x, float y, float r, Color body) {
        // Wing animation — flap on animTick
        float flapAngle = (float)Math.sin(animTick * 0.18f) * 0.4f;

        // Wings (behind body)
        Graphics2D gw = (Graphics2D)g.create();
        gw.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.8f));
        // Left wing
        gw.translate(x - r*0.3f, y);
        gw.rotate(-0.5f - flapAngle);
        gw.setColor(new Color(0xB71C1C));
        int[] wingXL = {0, -(int)(r*1.4f), -(int)(r*1.0f), -(int)(r*0.5f)};
        int[] wingYL = {0, -(int)(r*1.2f), 0, -(int)(r*0.4f)};
        gw.fillPolygon(wingXL, wingYL, 4);
        gw.setColor(new Color(0xE53935, true));
        gw.drawPolyline(wingXL, wingYL, 4);
        gw.dispose();

        Graphics2D gw2 = (Graphics2D)g.create();
        gw2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.8f));
        // Right wing
        gw2.translate(x + r*0.3f, y);
        gw2.rotate(0.5f + flapAngle);
        gw2.setColor(new Color(0xB71C1C));
        int[] wingXR = {0, (int)(r*1.4f), (int)(r*1.0f), (int)(r*0.5f)};
        int[] wingYR = {0, -(int)(r*1.2f), 0, -(int)(r*0.4f)};
        gw2.fillPolygon(wingXR, wingYR, 4);
        gw2.setColor(new Color(0xE53935, true));
        gw2.drawPolyline(wingXR, wingYR, 4);
        gw2.dispose();

        // Body
        g.setColor(new Color(0x1B5E20)); // outer green
        g.fillOval((int)(x-r),(int)(y-r),(int)(r*2),(int)(r*2));
        // Stripe overlay
        g.setColor(new Color(0x66BB6A));
        g.fillOval((int)(x-r*0.2f),(int)(y-r),(int)(r*0.4f),(int)(r*2));

        g.setColor(body.darker());
        g.setStroke(new BasicStroke(1.5f));
        g.drawOval((int)(x-r),(int)(y-r),(int)(r*2),(int)(r*2));
        g.setStroke(new BasicStroke(1));

        // Horns
        g.setColor(new Color(0xC62828));
        int[] hornLx = {(int)(x-r*0.35f),(int)(x-r*0.55f),(int)(x-r*0.2f)};
        int[] hornLy = {(int)(y-r+1),(int)(y-r-7),(int)(y-r-2)};
        g.fillPolygon(hornLx, hornLy, 3);
        int[] hornRx = {(int)(x+r*0.2f),(int)(x+r*0.55f),(int)(x+r*0.35f)};
        int[] hornRy = {(int)(y-r-2),(int)(y-r-7),(int)(y-r+1)};
        g.fillPolygon(hornRx, hornRy, 3);

        // Slit eyes — glowing orange
        float eo = r * 0.3f;
        g.setColor(new Color(0xFF6F00));
        g.fillOval((int)(x-eo-3),(int)(y-r*0.1f-3),7,5);
        g.fillOval((int)(x+eo-3),(int)(y-r*0.1f-3),7,5);
        // Vertical slit pupil
        g.setColor(Color.BLACK);
        g.fillRect((int)(x-eo),(int)(y-r*0.1f-2),2,4);
        g.fillRect((int)(x+eo),(int)(y-r*0.1f-2),2,4);

        // Flame breath hint when hp < 50%
        if (hp / maxHp < 0.5f) {
            float phase = (float)Math.sin(animTick * 0.25f);
            g.setColor(new Color(255, 100+(int)(phase*80), 0, 120+(int)(phase*60)));
            g.fillOval((int)(x-4),(int)(y+r*0.3f),8,5);
        }
    }

    private void drawBlackseed(Graphics2D g, float x, float y, float r, Color body) {
        // Ghostly pulsing aura
        float pulse = 0.5f + 0.5f*(float)Math.sin(animTick * 0.15f);
        g.setColor(new Color(156, 39, 176, (int)(40*pulse)));
        g.fillOval((int)(x-r*1.6f),(int)(y-r*1.6f),(int)(r*3.2f),(int)(r*3.2f));
        g.setColor(new Color(156, 39, 176, (int)(25*pulse)));
        g.fillOval((int)(x-r*2.0f),(int)(y-r*2.0f),(int)(r*4.0f),(int)(r*4.0f));

        // Wispy cloak tendrils
        g.setColor(new Color(74, 0, 88, 160));
        float tendrilBase = (float)Math.sin(animTick * 0.1f) * 2;
        g.fillOval((int)(x-r*0.4f+tendrilBase),(int)(y+r*0.7f),(int)(r*0.6f),(int)(r*0.5f));
        g.fillOval((int)(x+r*0.1f-tendrilBase),(int)(y+r*0.8f),(int)(r*0.5f),(int)(r*0.4f));
        g.fillOval((int)(x-r*0.7f),(int)(y+r*0.5f+(int)tendrilBase),(int)(r*0.4f),(int)(r*0.5f));

        // Translucent body
        Graphics2D gs = (Graphics2D)g.create();
        gs.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.75f + 0.15f*pulse));
        gs.setColor(body);
        gs.fillOval((int)(x-r),(int)(y-r),(int)(r*2),(int)(r*2));
        // Inner lighter core
        gs.setColor(new Color(180, 80, 220, 120));
        gs.fillOval((int)(x-r*0.5f),(int)(y-r*0.6f),(int)(r),(int)(r*0.8f));
        gs.dispose();

        // Hollow glowing eyes
        float eo = r * 0.28f;
        g.setColor(new Color(0xFF1744));
        g.fillOval((int)(x-eo-2),(int)(y-r*0.15f-2),6,5);
        g.fillOval((int)(x+eo-2),(int)(y-r*0.15f-2),6,5);
        g.setColor(new Color(0xFFFFFF, true));
        g.fillOval((int)(x-eo),(int)(y-r*0.15f),2,2);
        g.fillOval((int)(x+eo),(int)(y-r*0.15f),2,2);
    }

    private void drawHpBar(Graphics2D g, float x, float y, float r) {
        int barW = (int)(r * 3.0f), barH = 4;
        int bx = (int)(x - barW/2f);
        int by = (int)(y - r - 10);
        // Background
        g.setColor(new Color(20, 20, 20, 200));
        g.fillRoundRect(bx-1, by-1, barW+2, barH+2, 3, 3);
        // Fill
        float ratio = hp / maxHp;
        Color hpCol = ratio > 0.6f ? new Color(0x4CAF50)
                    : ratio > 0.3f ? new Color(0xFFC107)
                    :                new Color(0xF44336);
        g.setColor(hpCol);
        g.fillRoundRect(bx, by, (int)(barW * ratio), barH, 2, 2);
        // Shine
        g.setColor(new Color(255,255,255,50));
        g.fillRect(bx, by, (int)(barW*ratio), barH/2);
    }

    private static Color blend(Color a, Color b, float t) {
        float s = 1-t;
        return new Color(
            (int)(a.getRed()*s   + b.getRed()*t),
            (int)(a.getGreen()*s + b.getGreen()*t),
            (int)(a.getBlue()*s  + b.getBlue()*t));
    }

    public float getX() { return x; }
    public float getY() { return y; }
    public boolean isDead()       { return dead; }
    public boolean hasReached()   { return reached; }
    public int getReward()        { return reward; }
    public int getDamage()        { return damage; }
    public float getHp()          { return hp; }
    public float getMaxHp()       { return maxHp; }
    public Type getType()         { return type; }

    public float distanceTraveled() {
        int[][] path = activePath;
        float d = 0;
        for (int i=1; i<=pathIndex && i<path.length; i++) {
            float ax=path[i-1][0]*Constants.TILE+Constants.TILE/2f, ay=path[i-1][1]*Constants.TILE+Constants.TILE/2f;
            float bx=path[i][0]*Constants.TILE+Constants.TILE/2f,   by=path[i][1]*Constants.TILE+Constants.TILE/2f;
            d += Math.sqrt((bx-ax)*(bx-ax)+(by-ay)*(by-ay));
        }
        if (pathIndex < path.length-1) {
            float tx=path[pathIndex+1][0]*Constants.TILE+Constants.TILE/2f, ty=path[pathIndex+1][1]*Constants.TILE+Constants.TILE/2f;
            float cx=path[pathIndex][0]*Constants.TILE+Constants.TILE/2f,   cy=path[pathIndex][1]*Constants.TILE+Constants.TILE/2f;
            float seg=(float)Math.sqrt((tx-cx)*(tx-cx)+(ty-cy)*(ty-cy));
            float cur=(float)Math.sqrt((x-cx)*(x-cx)+(y-cy)*(y-cy));
            d += Math.min(cur, seg);
        }
        return d;
    }

    public void takeDamage(float dmg) {
        hp -= dmg;
        if (hp <= 0 && !dead) { hp = 0; dead = true; }
    }
    public void applySlow(float ticks) { slowTimer = Math.max(slowTimer, ticks); }
}