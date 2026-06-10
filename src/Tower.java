import java.awt.*;
import java.awt.geom.*;
import java.util.List;

public class Tower {
    public enum TType { DART, BOMB, ICE, SUPER, MORTAR, BANANA, POISON, THORN }

    private int col, row;
    private TType ttype;
    private float range, damage, fireRate;
    private float baseRange = 0f;
    private int cost;
    private Color color;
    private int fireCooldown = 0;
    private float angle = 0;
    private float targetAngle = 0; // smooth barrel rotation
    private boolean selected = false;

    private int pathA = 0; // skill tree paths
    private int pathB = 0;

    // Derived flags
    private float slowMult   = 1.0f;
    private float splashMult = 1.0f;
    private boolean multiShot  = false;
    private boolean slowField  = false;
    private int auraSlowTicks  = 0;
    private int bananaTimer   = 0;
    private int bananaInterval = 300; // ticks between payouts
    private int bananaPayout  = 10;
    private int pierceRange   = 3;    // tiles the thorn travels
    private float poisonDmg   = 0;    // extra poison damage per tick
    private float poisonDuration = 180f;

    private static final int AURA_INTERVAL = 30;
    private static final int[] PATH_COSTS  = {100, 200, 400};

    private float animTick    = 0; // animation state
    private float muzzleFlash = 0;  // countdown after firing, drives muzzle glow
    private float idlePhase   = 0;  // per-tower random offset
    // Upgrade flash
    private float upgradeFlash = 0;
    private static final float MUZZLE_DURATION = 8f;
    private static final float UPGRADE_DURATION = 30f;

    public Tower(int col, int row, TType ttype) {
        this.col = col; this.row = row; this.ttype = ttype;
        idlePhase = (float)(Math.random() * Math.PI * 2);
        applyStats();
        baseRange = range; // capture post-applyStats base range for body scale
    }

    private void applyStats() { // overall stats of monkeys
        switch (ttype) {
            case DART:  cost=70;  range=3.5f; damage=20;  fireRate=38;  color=new Color(0xE53935); break;
            case BOMB:  cost=140; range=2.5f; damage=60;  fireRate=90;  color=new Color(0x37474F); break;
            case ICE:   cost=100; range=3.0f; damage=10;  fireRate=60;  color=new Color(0x29B6F6); break;
            case SUPER: cost=300; range=4.0f; damage=25;  fireRate=20;  color=new Color(0xFFD600); break;
            case MORTAR:cost=225; range=5.0f; damage=80;  fireRate=120; color=new Color(0x6D4C41); break;
            case BANANA:cost=100; range=0f;   damage=0;   fireRate=0;   color=new Color(0xFFD600);
                        bananaInterval=400; bananaPayout=5; break;
            case POISON:cost=120; range=3.0f; damage=12; fireRate=45;  color=new Color(0x7CB342);
                        poisonDmg=4; poisonDuration=250f; break;
            case THORN: cost=100; range=4.0f; damage=20; fireRate=55;  color=new Color(0x795548);
                        pierceRange=4; break;
        }
        multiShot = false; slowField = false; splashMult = 1.0f; slowMult = 1.0f;
        applyPathA(); applyPathB();
    }

    private void applyPathA() { // apply path A upgrades to stats
        switch (ttype) {
            case DART:
                if (pathA >= 1) damage *= 1.4f;
                if (pathA >= 2) { damage *= 1.7f; range *= 1.2f; }
                if (pathA >= 3) { damage *= 2.0f; multiShot = true; } break;
            case BOMB:
                if (pathA >= 1) { damage *= 1.5f; splashMult *= 1.2f; }
                if (pathA >= 2) { damage *= 1.8f; splashMult *= 1.4f; }
                if (pathA >= 3) { damage *= 2.5f; splashMult *= 1.8f; } break;
            case ICE:
                if (pathA >= 1) slowMult = 2.5f;
                if (pathA >= 2) { slowMult = 5.0f; damage *= 1.2f; }
                if (pathA >= 3) { slowMult = 8.0f; damage *= 1.6f; } break;
            case SUPER:
                if (pathA >= 1) damage *= 1.6f;
                if (pathA >= 2) { damage *= 2.2f; range *= 1.15f; }
                if (pathA >= 3) { damage *= 3.0f; range *= 1.3f; fireRate = Math.max(5, fireRate*0.6f); } break;
            case MORTAR:
                if (pathA >= 1) damage *= 1.6f;
                if (pathA >= 2) { damage *= 2.0f; splashMult *= 1.2f; }
                if (pathA >= 3) { damage *= 2.8f; splashMult *= 1.5f; fireRate *= 0.8f; } break;
            case BANANA:
                if (pathA >= 1) bananaPayout = 15;
                if (pathA >= 2) bananaPayout = 22;
                if (pathA >= 3) bananaPayout = 35; break;
            case POISON:
                if (pathA >= 1) poisonDmg *= 1.5f;
                if (pathA >= 2) poisonDmg *= 2.0f;
                if (pathA >= 3) poisonDmg *= 3.0f; break;
            case THORN:
                if (pathA >= 1) { pierceRange = 6; range *= 1.5f; }
                if (pathA >= 2) { pierceRange = 8; range *= 1.33f; }
                if (pathA >= 3) { pierceRange = 10; range *= 1.25f; } break;
        }
    }
    private void applyPathB() { // apply path B upgrades to stats
        switch (ttype) {
            case DART:
                if (pathB >= 1) range *= 1.3f;
                if (pathB >= 2) { range *= 1.6f; damage *= 1.2f; }
                if (pathB >= 3) { range *= 2.0f; damage *= 1.6f; fireRate = Math.max(5, fireRate*0.7f); } break;
            case BOMB:
                if (pathB >= 1) fireRate = Math.max(5, fireRate*0.75f);
                if (pathB >= 2) { fireRate = Math.max(5, fireRate*0.5f); damage *= 1.3f; }
                if (pathB >= 3) { fireRate = Math.max(5, fireRate*0.3f); damage *= 1.6f; range *= 1.5f; } break;
            case ICE:
                if (pathB >= 1) { range *= 1.3f; slowField = true; }
                if (pathB >= 2) { range *= 1.6f; }
                if (pathB >= 3) { range *= 2.0f; damage *= 1.4f; } break;
            case SUPER:
                if (pathB >= 1) fireRate = Math.max(5, fireRate*0.7f);
                if (pathB >= 2) { fireRate = Math.max(5, fireRate*0.45f); damage *= 1.2f; }
                if (pathB >= 3) { fireRate = Math.max(5, fireRate*0.25f); damage *= 1.5f; range *= 1.2f; } break;
            case MORTAR:
                if (pathB >= 1) splashMult *= 1.4f;
                if (pathB >= 2) { splashMult *= 1.8f; range *= 1.2f; }
                if (pathB >= 3) { splashMult *= 2.4f; range *= 1.5f; damage *= 1.6f; } break;
            case BANANA:
                if (pathB >= 1) bananaInterval = 220;
                if (pathB >= 2) bananaInterval = 150;
                if (pathB >= 3) bananaInterval = 90; break;
            case POISON:
                if (pathB >= 1) fireRate = Math.max(5, fireRate*0.75f);
                if (pathB >= 2) fireRate = Math.max(5, fireRate*0.55f);
                if (pathB >= 3) fireRate = Math.max(5, fireRate*0.35f); break;
            case THORN:
                if (pathB >= 1) damage *= 1.5f;
                if (pathB >= 2) damage *= 2.0f;
                if (pathB >= 3) damage *= 3.0f; break;
        }
    }

    public List<Projectile> updateMulti(List<Enemy> enemies, List<Enemy> allEnemies) { // returns list of projectiles to spawn (multi-shot), also updates tower state
        List<Projectile> result = new java.util.ArrayList<>();
        animTick++;
        if (muzzleFlash > 0) muzzleFlash--;
        if (upgradeFlash > 0) upgradeFlash--;
        float diff = targetAngle - angle;
        while (diff >  Math.PI) diff -= (float)(Math.PI*2);
        while (diff < -Math.PI) diff += (float)(Math.PI*2);
        angle += diff * 0.18f;

        if (ttype == TType.BANANA) {
            bananaTimer++;
            return result; // handled externally via getBananaGold()
        }

        if (slowField) {
            auraSlowTicks++;
            if (auraSlowTicks >= AURA_INTERVAL) {
                auraSlowTicks = 0;
                float cx = col*Constants.TILE+Constants.TILE/2f;
                float cy = row*Constants.TILE+Constants.TILE/2f;
                float rp = range*Constants.TILE;
                int slowDur = (int)(40*slowMult);
                for (Enemy e : enemies) {
                    if (e.isDead()||e.hasReached()) continue;
                    float dx=e.getX()-cx, dy=e.getY()-cy;
                    if (Math.sqrt(dx*dx+dy*dy) <= rp) e.applySlow(slowDur);
                }
            }
        }

        if (fireCooldown > 0) { fireCooldown--; return result; }
        Enemy target = findTarget(enemies);
        if (target == null) return result;

        float cx = col*Constants.TILE+Constants.TILE/2f;
        float cy = row*Constants.TILE+Constants.TILE/2f;
        targetAngle = (float)Math.atan2(target.getY()-cy, target.getX()-cx);
        fireCooldown = (int)fireRate;
        muzzleFlash  = MUZZLE_DURATION;

        Projectile.PType pt;
        float splash = 0;
        switch (ttype) {
            case BOMB:   pt = Projectile.PType.CANNONBALL;   splash = 40*splashMult; break;
            case ICE:    pt = Projectile.PType.FROST_BOLT;   break;
            case SUPER:  pt = Projectile.PType.LASER_BEAM;   break;
            case MORTAR: pt = Projectile.PType.MORTAR_SHELL; splash = 60*splashMult; break;
            case POISON: pt = Projectile.PType.POISON_DART;  break;
            case THORN:  pt = Projectile.PType.THORN;        break;
            default:     pt = Projectile.PType.DART;         break;
        }
        float projSpeed = ttype==TType.SUPER ? 0 : ttype==TType.MORTAR ? 4f : 7f;
        result.add(new Projectile(cx, cy, target, damage, projSpeed, pt, splash, allEnemies,
            poisonDmg, poisonDuration, pierceRange));

        if (multiShot) {
            float[] offsets = {-0.25f, 0.25f};
            for (float off : offsets) {
                Enemy t2 = findTargetAt(enemies, targetAngle+off);
                Enemy t3 = t2!=null ? t2 : target;
                result.add(new Projectile(cx, cy, t3, damage*0.7f, projSpeed, pt, splash, allEnemies));
            }
        }
        return result;
    }

    public int collectBanana() {
        if (ttype != TType.BANANA) return 0;
        return bananaPayout;
    }

    public void resetBananaTimer() { bananaTimer = 0; }

    public Projectile update(List<Enemy> enemies, List<Enemy> allEnemies) {
        List<Projectile> list = updateMulti(enemies, allEnemies);
        return list.isEmpty() ? null : list.get(0);
    }

    private Enemy findTarget(List<Enemy> enemies) {
        float cx=col*Constants.TILE+Constants.TILE/2f, cy=row*Constants.TILE+Constants.TILE/2f;
        float rp=range*Constants.TILE;
        Enemy best=null; float bestDist=Float.MIN_VALUE;
        for (Enemy e : enemies) {
            if (e.isDead()||e.hasReached()) continue;
            float dx=e.getX()-cx, dy=e.getY()-cy;
            if (Math.sqrt(dx*dx+dy*dy)<=rp) {
                float t=e.distanceTraveled();
                if (t>bestDist) { bestDist=t; best=e; }
            }
        }
        return best;
    }
    private Enemy findTargetAt(List<Enemy> enemies, float aimAngle) {
        float cx=col*Constants.TILE+Constants.TILE/2f, cy=row*Constants.TILE+Constants.TILE/2f;
        float rp=range*Constants.TILE;
        Enemy best=null; float bestDist=Float.MIN_VALUE;
        for (Enemy e : enemies) {
            if (e.isDead()||e.hasReached()) continue;
            float dx=e.getX()-cx, dy=e.getY()-cy, d=(float)Math.sqrt(dx*dx+dy*dy);
            if (d<=rp) {
                float ea=(float)Math.atan2(dy,dx);
                float diff=Math.abs(ea-aimAngle);
                if (diff<0.6f) { float t=e.distanceTraveled(); if (t>bestDist) { bestDist=t; best=e; } }
            }
        }
        return best;
    }

    public int getPathA() { return pathA; } // skill trees
    public int getPathB() { return pathB; }

    public boolean canUpgradePathA() {
        if (pathA >= 3) return false;
        if (pathB >= 3) return false;
        if (pathA >= 2 && pathB >= 2) return false;
        return true;
    }
    public boolean canUpgradePathB() {
        if (pathB >= 3) return false;
        if (pathA >= 3) return false;
        if (pathB >= 2 && pathA >= 2) return false;
        return true;
    }
    public boolean isPathAHardLocked() { return pathB>=3 || (pathB>=2 && pathA==0); }
    public boolean isPathBHardLocked() { return pathA>=3 || (pathA>=2 && pathB==0); }
    public int primaryPath() {
        if (pathA==0&&pathB==0) return 0;
        if (pathA>pathB) return 1;
        if (pathB>pathA) return 2;
        return -1;
    }

    public int pathACost() { return pathA<3 ? PATH_COSTS[pathA] : 0; }
    public int pathBCost() { return pathB<3 ? PATH_COSTS[pathB] : 0; }

    public void upgradePathA() { if (canUpgradePathA()) { pathA++; upgradeFlash=UPGRADE_DURATION; applyStats(); } }
    public void upgradePathB() { if (canUpgradePathB()) { pathB++; upgradeFlash=UPGRADE_DURATION; applyStats(); } }

    public String[] getPathANames() { // paths
        switch (ttype) {
            case DART:   return new String[]{"Sharp Darts","Razor Wind","Storm"};
            case BOMB:   return new String[]{"Bigger Bombs","Frag Shells","MOAB Buster"};
            case ICE:    return new String[]{"Deep Freeze","Permafrost","Absolute Zero"};
            case SUPER:  return new String[]{"Plasma Beam","Solar Flare","Sun God"};
            case MORTAR: return new String[]{"Heavy Shell","Shrapnel","Artillery"};
            case BANANA: return new String[]{"Ripe Bunch","Mega Bunch","Banana Plantation"};
            case POISON: return new String[]{"Toxic Darts","Venom","Neurotoxin"};
            case THORN:  return new String[]{"Long Thorns","Bramble","Thornwall"};
            default: return new String[]{"T1","T2","T3"};
        }
    }
    public String[] getPathBNames() {
        switch (ttype) {
            case DART:   return new String[]{"Long Range","Eagle Eye","Sniper"};
            case BOMB:   return new String[]{"Fast Fuse","Cluster","Carpet Bomb"};
            case ICE:    return new String[]{"Wide Aura","Ice Field","Blizzard"};
            case SUPER:  return new String[]{"Rapid Fire","Hypersonic","Overdrive"};
            case MORTAR: return new String[]{"Wide Blast","Siege Mode","Annihilator"};
            case BANANA: return new String[]{"Quick Harvest","Fast Farm","Supermarket"};
            case POISON: return new String[]{"Quick Shot","Blowpipe","Rapid Venom"};
            case THORN:  return new String[]{"Heavy Thorns","Spike","Razorback"};
            default: return new String[]{"T1","T2","T3"};
        }
    }
    public String[] getPathADescs() {
        switch (ttype) {
            case DART:   return new String[]{"+40% DMG","+70% DMG +20% RNG","3-dart storm"};
            case BOMB:   return new String[]{"+50% DMG +splash","+80% DMG +splash","+150% DMG +splash"};
            case ICE:    return new String[]{"2.5x slow duration","5x slow +20% DMG","8x slow +60% DMG"};
            case SUPER:  return new String[]{"+60% DMG","+120% DMG +RNG","+200% DMG, faster"};
            case MORTAR: return new String[]{"+60% DMG","+100% DMG +splash","+180% DMG, faster"};
            case BANANA: return new String[]{"+5 gold/tick","+12 gold/tick","+25 gold/tick"};
            case POISON: return new String[]{"1.5x poison DMG","2x poison DMG","3x poison DMG"};
            case THORN:  return new String[]{"Pierce 6 tiles","Pierce 9 tiles","Pierce 12 tiles"};
            default: return new String[]{"Tier 1","Tier 2","Tier 3"};
        }
    }
    public String[] getPathBDescs() {
        switch (ttype) {
            case DART:   return new String[]{"+30% RNG","+60% RNG +20% DMG","+100% RNG +60% DMG"};
            case BOMB:   return new String[]{"25% faster fire","50% faster +30% DMG","70% faster +range"};
            case ICE:    return new String[]{"+30% RNG, aura","+60% RNG, stronger","+100% RNG +40% DMG"};
            case SUPER:  return new String[]{"30% faster","55% faster +20% DMG","75% faster +DMG"};
            case MORTAR: return new String[]{"+40% splash","+80% splash +RNG","+140% splash +DMG"};
            case BANANA: return new String[]{"Faster payout","Much faster","Very fast payout"};
            case POISON: return new String[]{"25% faster","45% faster","65% faster"};
            case THORN:  return new String[]{"1.5x DMG","2x DMG","3x DMG"};
            default: return new String[]{"Tier 1","Tier 2","Tier 3"};
        }
    }
    public String getPathALabel() {
        switch (ttype) {
            case DART:return"Power"; 
            case BOMB:return"Damage"; 
            case ICE:return"Freeze";
            case SUPER:return"Beam"; 
            case MORTAR:return"Power";
            case BANANA: return "Yield";  
            case POISON: return "Venom"; 
            case THORN: return "Pierce";
            default:return"Path A";
        }
    }
    public String getPathBLabel() {
        switch (ttype) {
            case DART:return"Range"; 
            case BOMB:return"Speed"; 
            case ICE:return"Aura";
            case SUPER:return"Speed"; 
            case MORTAR:return"Blast"; 
            case BANANA: return "Speed";  
            case POISON: return "Speed"; 
            case THORN: return "Power";
            default:return"Path B";
        }
    }
    public int sellValue() {
        int spent=cost;
        for (int i=0;i<pathA;i++) spent+=PATH_COSTS[i];
        for (int i=0;i<pathB;i++) spent+=PATH_COSTS[i];
        return (int)(spent*0.6f);
    }

    public void draw(Graphics2D g) {
        int px = col*Constants.TILE, py = row*Constants.TILE;
        int cx = px+Constants.TILE/2,  cy = py+Constants.TILE/2;

        // Tile base with subtle gradient
        g.setColor(new Color(0x4A3728));
        g.fillRoundRect(px+2, py+2, Constants.TILE-4, Constants.TILE-4, 4, 4);
        g.setColor(new Color(0x3E2B1A));
        g.setStroke(new BasicStroke(1.2f));
        g.drawRoundRect(px+2, py+2, Constants.TILE-4, Constants.TILE-4, 4, 4);
        g.setStroke(new BasicStroke(1));

        // Upgrade glow flash
        if (upgradeFlash > 0) {
            float t = upgradeFlash / UPGRADE_DURATION;
            int alpha = (int)(t * 160);
            g.setColor(new Color(255, 215, 0, alpha));
            g.setStroke(new BasicStroke(3));
            g.drawRoundRect(px+1, py+1, Constants.TILE-2, Constants.TILE-2, 6, 6);
            g.setStroke(new BasicStroke(1));
        }

        // Aura ring for ICE slow-field
        if (slowField) {
            float pulse = 0.5f + 0.5f*(float)Math.sin(animTick * 0.08f);
            g.setColor(new Color(41, 182, 246, (int)(30*pulse)));
            int rp = (int)(range*Constants.TILE);
            g.fillOval(cx-rp, cy-rp, rp*2, rp*2);
            g.setColor(new Color(41, 182, 246, (int)(60*pulse)));
            g.setStroke(new BasicStroke(1.5f));
            g.drawOval(cx-rp, cy-rp, rp*2, rp*2);
            g.setStroke(new BasicStroke(1));
        }

        // Path indicator dots
        for (int i=0;i<pathA;i++) { g.setColor(new Color(0xFF7043)); g.fillOval(px+3+i*6, py+3, 5, 5); }
        for (int i=0;i<pathB;i++) { g.setColor(new Color(0x29B6F6)); g.fillOval(px+Constants.TILE-8-i*6, py+3, 5, 5); }

        // Subtle idle breathing scale on the monkey body
        float rangeScale = (baseRange > 0f) ? Math.min(range / baseRange, 1.5f) : 1.0f;
        float idlePulse  = 1.0f + 0.025f*(float)Math.sin(animTick*0.06f + idlePhase);
        float bodyScale  = rangeScale * idlePulse;
        Graphics2D gm = (Graphics2D)g.create();
        gm.translate(cx, cy);
        gm.scale(bodyScale, bodyScale);
        gm.translate(-cx, -cy);
        drawMonkeyBody(gm, cx, cy);
        gm.dispose();

        // Barrel (smooth rotation)
        Graphics2D g2 = (Graphics2D)g.create();
        g2.translate(cx, cy);
        g2.rotate(angle);
        drawBarrel(g2);

        // Muzzle flash
        if (muzzleFlash > 0) {
            float mf = muzzleFlash / MUZZLE_DURATION;
            drawMuzzleFlash(g2, mf);
        }
        g2.dispose();

        // Selection ring
        if (selected) {
            g.setColor(new Color(255, 255, 0, 170));
            g.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 0, new float[]{5,4}, animTick % 9));
            int rp = (int)(range*Constants.TILE);
            g.drawOval(cx-rp, cy-rp, rp*2, rp*2);
            g.setStroke(new BasicStroke(1));
        }
    }

    private void drawMuzzleFlash(Graphics2D g2, float intensity) {
        // Barrel tip offset ~14-16px along x axis (after rotation)
        int tipX = 15;
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, intensity * 0.9f));
        Color flashCol;
        switch (ttype) {
            case BOMB:   flashCol = new Color(255, 120, 0);  break;
            case ICE:    flashCol = new Color(150, 230, 255); break;
            case SUPER:  flashCol = new Color(255, 255, 150); break;
            case MORTAR: flashCol = new Color(200, 180, 0);  break;
            default:     flashCol = new Color(255, 80, 60);  break;
        }
        // Outer bloom
        g2.setColor(new Color(flashCol.getRed(), flashCol.getGreen(), flashCol.getBlue(), (int)(60*intensity)));
        g2.fillOval(tipX-8, -8, 16, 16);
        // Inner burst
        g2.setColor(new Color(flashCol.getRed(), flashCol.getGreen(), flashCol.getBlue(), (int)(200*intensity)));
        g2.fillOval(tipX-4, -4, 8, 8);
        // Hot white centre
        g2.setColor(new Color(255, 255, 255, (int)(220*intensity)));
        g2.fillOval(tipX-2, -2, 5, 5);
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
    }

    private void drawMonkeyBody(Graphics2D g, int cx, int cy) {
        switch (ttype) {
            case DART:   drawDartMonkey(g, cx, cy);   break;
            case BOMB:   drawBombMonkey(g, cx, cy);   break;
            case ICE:    drawIceMonkey(g, cx, cy);    break;
            case SUPER:  drawSuperMonkey(g, cx, cy);  break;
            case MORTAR: drawMortarMonkey(g, cx, cy); break;
            case BANANA: drawBananaFarm(g, cx, cy);   break;
            case POISON: drawPoisonMonkey(g, cx, cy); break;
            case THORN:  drawThornMonkey(g, cx, cy);  break;
        }
    }

    private void drawDartMonkey(Graphics2D g, int cx, int cy) {
        // Red beret
        g.setColor(new Color(0xC0392B));
        int[] bx = {cx-7, cx+7, cx+5, cx-5};
        int[] by = {cy-9, cy-9, cy-14, cy-14};
        g.fillPolygon(bx, by, 4);
        g.setColor(new Color(0x7B241C));
        g.fillRect(cx-7, cy-10, 14, 3);
        // Beret badge
        g.setColor(new Color(0xF39C12));
        g.fillOval(cx-2, cy-13, 4, 4);

        // Ears
        g.setColor(new Color(0xA0522D));
        g.fillOval(cx-12, cy-4, 5, 7);
        g.fillOval(cx+7,  cy-4, 5, 7);
        g.setColor(new Color(0xF5CBA7));
        g.fillOval(cx-11, cy-3, 3, 5);
        g.fillOval(cx+8,  cy-3, 3, 5);

        // Body
        g.setColor(new Color(0xC68642));
        g.fillOval(cx-10, cy-9, 20, 18);
        // Highlight
        g.setColor(new Color(222, 184, 135, 120));
        g.fillOval(cx-6, cy-7, 8, 6);
        // Face
        g.setColor(new Color(0xF5CBA7));
        g.fillOval(cx-7, cy-5, 14, 12);
        // Eyes
        g.setColor(Color.BLACK);
        g.fillOval(cx-5, cy-3, 3, 3);
        g.fillOval(cx+2, cy-3, 3, 3);
        g.setColor(Color.WHITE); g.fillOval(cx-4, cy-4, 1, 1); g.fillOval(cx+3, cy-4, 1, 1);
        // Nose
        g.setColor(new Color(0xA0522D));
        g.fillOval(cx-2, cy+1, 4, 3);
        // Outline
        g.setColor(new Color(0x7D5A3C));
        g.setStroke(new BasicStroke(1.3f));
        g.drawOval(cx-10, cy-9, 20, 18);
        g.setStroke(new BasicStroke(1));
        // Path A tier glow tint
        if (pathA >= 3) { g.setColor(new Color(255,80,60,60)); g.fillOval(cx-10,cy-9,20,18); }
    }

    private void drawBombMonkey(Graphics2D g, int cx, int cy) {
        // Helmet
        g.setColor(new Color(0x37474F));
        g.fillArc(cx-11, cy-14, 22, 18, 0, 180);
        g.setColor(new Color(0x546E7A));
        g.setStroke(new BasicStroke(2));
        g.drawArc(cx-11, cy-14, 22, 18, 0, 180);
        g.setColor(new Color(0x90A4AE));
        g.setStroke(new BasicStroke(1.5f));
        g.drawLine(cx-11, cy-5, cx+11, cy-5);
        // Spike
        g.setColor(new Color(0xB0BEC5));
        int[] sx={cx-2,cx+2,cx}; int[] sy={cx-8,cx-8,cx-16};
        // correction: use cy
        g.fillPolygon(new int[]{cx-2,cx+2,cx}, new int[]{cy-12,cy-12,cy-18}, 3);
        g.setStroke(new BasicStroke(1));

        // Ears
        g.setColor(new Color(0x8D6E63));
        g.fillOval(cx-13, cy-4, 5, 7); g.fillOval(cx+8, cy-4, 5, 7);
        g.setColor(new Color(0xBCAAA4));
        g.fillOval(cx-12, cy-3, 3, 5); g.fillOval(cx+9, cy-3, 3, 5);

        // Body + brows
        g.setColor(new Color(0x8D6E63));
        g.fillOval(cx-11, cy-10, 22, 20);
        g.setColor(new Color(222, 184, 135, 100));
        g.fillOval(cx-6, cy-7, 8, 5);
        g.setColor(new Color(0xF5CBA7));
        g.fillOval(cx-7, cy-5, 14, 12);
        g.setColor(Color.BLACK);
        g.setStroke(new BasicStroke(1.8f));
        g.drawLine(cx-6, cy-7, cx-2, cy-5);
        g.drawLine(cx+6, cy-7, cx+2, cy-5);
        g.setStroke(new BasicStroke(1));
        g.fillOval(cx-5, cy-4, 3, 3); g.fillOval(cx+2, cy-4, 3, 3);
        g.setColor(Color.WHITE); g.fillOval(cx-4,cy-5,1,1); g.fillOval(cx+3,cy-5,1,1);
        g.setColor(new Color(0x7D5A3C));
        g.setStroke(new BasicStroke(1.3f));
        g.drawOval(cx-11, cy-10, 22, 20);
        g.setStroke(new BasicStroke(1));
        if (pathA >= 3) { g.setColor(new Color(255,100,0,55)); g.fillOval(cx-11,cy-10,22,20); }
    }

    private void drawIceMonkey(Graphics2D g, int cx, int cy) {
        // Crown of ice spikes
        g.setColor(new Color(0x81D4FA));
        for (int a = -60; a <= 60; a += 30) {
            double rad = Math.toRadians(a - 90);
            int sx = cx + (int)(8*Math.cos(rad));
            int sy = cy + (int)(8*Math.sin(rad)) - 4;
            int tipX = cx + (int)(14*Math.cos(rad));
            int tipY = cy + (int)(14*Math.sin(rad)) - 4;
            g.setStroke(new BasicStroke(3));
            g.drawLine(sx, sy, tipX, tipY);
        }
        g.setStroke(new BasicStroke(1));

        // Body
        g.setColor(new Color(0xB3E5FC));
        g.fillOval(cx-10, cy-9, 20, 18);
        g.setColor(new Color(225, 245, 254, 150));
        g.fillOval(cx-6, cy-7, 8, 5);
        g.setColor(new Color(0xE1F5FE));
        g.fillOval(cx-7, cy-5, 14, 12);

        // Eyes — icy blue
        g.setColor(new Color(0x0277BD));
        g.fillOval(cx-5, cy-3, 3, 3); g.fillOval(cx+2, cy-3, 3, 3);
        g.setColor(new Color(0xE3F2FD));
        g.fillOval(cx-4, cy-4, 1, 1); g.fillOval(cx+3, cy-4, 1, 1);

        // Snowflake crown centre
        g.setColor(new Color(0x29B6F6));
        g.fillOval(cx-3, cy-15, 6, 6);
        g.setColor(Color.WHITE);
        g.setStroke(new BasicStroke(1.2f));
        g.drawLine(cx, cy-16, cx, cy-11);
        g.drawLine(cx-2, cy-14, cx+2, cy-12);
        g.drawLine(cx+2, cy-14, cx-2, cy-12);
        g.setStroke(new BasicStroke(1));

        g.setColor(new Color(0x0288D1));
        g.setStroke(new BasicStroke(1.3f));
        g.drawOval(cx-10, cy-9, 20, 18);
        g.setStroke(new BasicStroke(1));

        // Aura shimmer at B2+
        if (pathB >= 2) {
            float shimmer = 0.3f + 0.3f*(float)Math.sin(animTick*0.1f);
            g.setColor(new Color(41,182,246,(int)(40*shimmer)));
            g.fillOval(cx-14,cy-13,28,26);
        }
    }

    private void drawSuperMonkey(Graphics2D g, int cx, int cy) {
        // Cape
        g.setColor(new Color(0xB71C1C));
        int[] capex = {cx-9, cx+9, cx+13, cx-13};
        int[] capey = {cy-2, cy-2, cy+12, cy+12};
        g.fillPolygon(capex, capey, 4);
        // Cape highlight
        g.setColor(new Color(239, 83, 80, 120));
        g.fillPolygon(new int[]{cx-4,cx+4,cx+6,cx-6}, new int[]{cy-2,cy-2,cy+12,cy+12}, 4);

        // Crown
        g.setColor(new Color(0xFFD600));
        int[] crownx = {cx-7,cx-5,cx-3,cx,cx+3,cx+5,cx+7,cx+7,cx-7};
        int[] crowny = {cy-10,cy-14,cy-10,cy-14,cy-10,cy-14,cy-10,cy-8,cy-8};
        g.fillPolygon(crownx, crowny, 9);
        g.setColor(new Color(0xE65100));
        g.setStroke(new BasicStroke(1.2f));
        g.drawPolygon(crownx, crowny, 9);
        g.setStroke(new BasicStroke(1));
        // Crown gems
        g.setColor(new Color(0xFF4444)); g.fillOval(cx-2,cy-15,4,4);
        g.setColor(new Color(0x44FF88)); g.fillOval(cx-5,cy-13,3,3);
        g.setColor(new Color(0x4488FF)); g.fillOval(cx+2,cy-13,3,3);

        // Body
        g.setColor(new Color(0xFFCA28));
        g.fillOval(cx-10, cy-10, 20, 20);
        g.setColor(new Color(255, 224, 130, 150));
        g.fillOval(cx-6, cy-8, 8, 6);
        g.setColor(new Color(0xFFE082));
        g.fillOval(cx-7, cy-5, 14, 11);

        // Eyes — glowing orange (higher tier = brighter)
        Color eyeCol = pathA>=3 ? new Color(0xFF3D00) : pathA>=1 ? new Color(0xFF6D00) : new Color(0xE65100);
        g.setColor(eyeCol);
        g.fillOval(cx-6, cy-4, 4, 3); g.fillOval(cx+2, cy-4, 4, 3);
        g.setColor(Color.WHITE); g.fillOval(cx-5,cy-4,2,2); g.fillOval(cx+3,cy-4,2,2);

        g.setColor(new Color(0xE65100));
        g.setStroke(new BasicStroke(1.2f));
        g.drawOval(cx-10, cy-10, 20, 20);
        g.setStroke(new BasicStroke(1));

        // Sun God aura halo
        if (pathA >= 3) {
            float haloA = 0.4f + 0.3f*(float)Math.sin(animTick*0.1f);
            g.setColor(new Color(255,215,0,(int)(60*haloA)));
            g.fillOval(cx-16,cy-16,32,32);
            g.setColor(new Color(255,150,0,(int)(100*haloA)));
            g.setStroke(new BasicStroke(2));
            g.drawOval(cx-13,cy-13,26,26);
            g.setStroke(new BasicStroke(1));
        }
    }

    private void drawMortarMonkey(Graphics2D g, int cx, int cy) {
        // Camo helmet
        g.setColor(new Color(0x2E7D32));
        g.fillArc(cx-12, cy-16, 24, 20, 0, 180);
        g.setColor(new Color(0x558B2F));
        g.setStroke(new BasicStroke(2));
        g.drawArc(cx-12, cy-16, 24, 20, 0, 180);
        g.drawLine(cx-13, cy-6, cx+13, cy-6);
        g.setStroke(new BasicStroke(1));
        // Camo spots on helmet
        g.setColor(new Color(27, 94, 32, 160));
        g.fillOval(cx-6,cy-14,5,4); g.fillOval(cx+2,cy-12,4,3); g.fillOval(cx-3,cy-10,3,3);
        // Helmet star
        g.setColor(new Color(0xFFFFFF));
        g.setFont(new Font("SansSerif", Font.BOLD, 7));
        g.drawString("★", cx-3, cy-8);

        // Body
        g.setColor(new Color(0x558B2F));
        g.fillOval(cx-11, cy-10, 22, 20);
        g.setColor(new Color(139, 195, 74, 100));
        g.fillOval(cx-6, cy-7, 8, 5);
        g.setColor(new Color(0xD7B58A));
        g.fillOval(cx-7, cy-5, 14, 12);

        // Determined eyes — squinted
        g.setColor(new Color(0x1B1B1B));
        g.fillOval(cx-5, cy-4, 3, 2); g.fillOval(cx+2, cy-4, 3, 2);
        // Brow lines
        g.setColor(new Color(0x33691E));
        g.fillOval(cx-5, cy-2, 4, 3); g.fillOval(cx+2, cy+1, 3, 3);
        g.setColor(new Color(0x7D5A3C));
        g.setStroke(new BasicStroke(1.3f));
        g.drawOval(cx-11, cy-10, 22, 20);
        g.setStroke(new BasicStroke(1));
        if (pathA >= 3) { g.setColor(new Color(200,160,0,55)); g.fillOval(cx-11,cy-10,22,20); }
    }

    private void drawBarrel(Graphics2D g2) {
        switch (ttype) {
            case DART:
                // Wooden shaft
                g2.setColor(new Color(0x8D6E63));
                g2.setStroke(new BasicStroke(3, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.drawLine(5, 0, 15, 0);
                g2.setColor(new Color(0x37474F));
                g2.setStroke(new BasicStroke(2));
                g2.drawLine(5, 0, 13, 0);
                // Metal tip
                g2.setColor(new Color(0xE53935));
                g2.fillPolygon(new int[]{13,18,13}, new int[]{-3,0,3}, 3);
                g2.setColor(new Color(0xFF8A80));
                g2.drawLine(13,-1,17,0);
                break;
            case BOMB:
                // Thick cannon
                g2.setColor(new Color(0x1A1A1A));
                g2.setStroke(new BasicStroke(7, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.drawLine(2, 0, 13, 0);
                g2.setColor(new Color(0x37474F));
                g2.setStroke(new BasicStroke(5));
                g2.drawLine(2, 0, 13, 0);
                // Band rings
                g2.setColor(new Color(0x90A4AE));
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawLine(5, -3, 5, 3);
                g2.drawLine(9, -3, 9, 3);
                break;
            case ICE:
                // Crystal wand
                g2.setColor(new Color(0x4FC3F7));
                g2.setStroke(new BasicStroke(3, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.drawLine(4, 0, 14, 0);
                g2.setColor(new Color(0xB3E5FC));
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawLine(4, 0, 13, 0);
                // Crystal tip
                g2.setColor(new Color(0xE1F5FE));
                g2.fillOval(12, -4, 7, 7);
                g2.setColor(new Color(0x29B6F6));
                g2.setStroke(new BasicStroke(1.2f));
                g2.drawOval(12, -4, 7, 7);
                break;
            case SUPER:
                // Energy cannon
                g2.setColor(new Color(0xF57F17));
                g2.setStroke(new BasicStroke(4, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.drawLine(4, 0, 17, 0);
                g2.setColor(new Color(0xFFD54F));
                g2.setStroke(new BasicStroke(2.5f));
                g2.drawLine(4, 0, 16, 0);
                // Energy orb
                g2.setColor(new Color(0xFFF9C4));
                g2.fillOval(14, -4, 8, 8);
                g2.setColor(new Color(0xFFCA28));
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawOval(14, -4, 8, 8);
                // inner glow
                if (muzzleFlash < 0) { // always draw inner
                    g2.setColor(new Color(255,255,255,180));
                    g2.fillOval(16,-2,4,4);
                }
                break;
            case MORTAR:
                // Short thick mortar tube
                g2.setColor(new Color(0x3E2723));
                g2.setStroke(new BasicStroke(8, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.drawLine(0, 0, 8, 0);
                g2.setColor(new Color(0x6D4C41));
                g2.setStroke(new BasicStroke(5));
                g2.drawLine(0, 0, 8, 0);
                g2.setColor(new Color(0xA1887F));
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawLine(3, -3, 3, 3);
                break;
            case BANANA:
                break;
            case POISON:
                // Blowpipe
                g2.setColor(new Color(0x4E342E));
                g2.setStroke(new BasicStroke(3, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.drawLine(4, 0, 16, 0);
                g2.setColor(new Color(0x7CB342));
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawLine(4, 0, 15, 0);
                // Drip at tip
                g2.setColor(new Color(0xAED581));
                g2.fillOval(14, -3, 6, 6);
                g2.setColor(new Color(0x558B2F));
                g2.setStroke(new BasicStroke(1.2f));
                g2.drawOval(14, -3, 6, 6);
                break;
            case THORN:
                // Wooden shaft with thorn tip
                g2.setColor(new Color(0x5D4037));
                g2.setStroke(new BasicStroke(3, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.drawLine(4, 0, 14, 0);
                g2.setColor(new Color(0x795548));
                g2.setStroke(new BasicStroke(1.8f));
                g2.drawLine(4, 0, 13, 0);
                // Sharp thorn tip
                g2.setColor(new Color(0x3E2723));
                g2.fillPolygon(new int[]{12, 19, 12}, new int[]{-4, 0, 4}, 3);
                g2.setColor(new Color(0x6D4C41));
                g2.drawLine(12, -2, 18, 0);
                break;
        }
        g2.setStroke(new BasicStroke(1));
    }

    private void drawBananaFarm(Graphics2D g, int cx, int cy) {
        // Wooden crate base
        g.setColor(new Color(0x8D6E63));
        g.fillRoundRect(cx-11, cy-6, 22, 14, 4, 4);
        g.setColor(new Color(0x6D4C41));
        g.setStroke(new BasicStroke(1.2f));
        g.drawRoundRect(cx-11, cy-6, 22, 14, 4, 4);
        g.drawLine(cx, cy-6, cx, cy+8);
        g.drawLine(cx-11, cy+1, cx+11, cy+1);
        g.setStroke(new BasicStroke(1));

        // Three bananas on top
        g.setColor(new Color(0xFFD600));
        g.fillArc(cx-10, cy-16, 10, 12, 0, 200);
        g.fillArc(cx-3,  cy-17, 10, 12, 0, 200);
        g.fillArc(cx+4,  cy-16, 10, 12, 0, 200);
        g.setColor(new Color(0xF9A825));
        g.setStroke(new BasicStroke(1.5f));
        g.drawArc(cx-10, cy-16, 10, 12, 0, 200);
        g.drawArc(cx-3,  cy-17, 10, 12, 0, 200);
        g.drawArc(cx+4,  cy-16, 10, 12, 0, 200);
        g.setStroke(new BasicStroke(1));

        // Brown tips
        g.setColor(new Color(0x5D4037));
        g.fillOval(cx-10, cy-11, 3, 3);
        g.fillOval(cx-3,  cy-12, 3, 3);
        g.fillOval(cx+4,  cy-11, 3, 3);

        // Gold shimmer at higher tiers
        if (pathA >= 2 || pathB >= 2) {
            float shimmer = 0.4f + 0.3f*(float)Math.sin(animTick*0.08f);
            g.setColor(new Color(255, 220, 0, (int)(50*shimmer)));
            g.fillOval(cx-13, cy-18, 26, 26);
        }
    }

    private void drawPoisonMonkey(Graphics2D g, int cx, int cy) {
        // Toxic hood
        g.setColor(new Color(0x558B2F));
        g.fillArc(cx-11, cy-15, 22, 18, 0, 180);
        g.setColor(new Color(0x7CB342));
        g.setStroke(new BasicStroke(1.5f));
        g.drawArc(cx-11, cy-15, 22, 18, 0, 180);
        g.drawLine(cx-11, cy-6, cx+11, cy-6);
        g.setStroke(new BasicStroke(1));

        // Dripping poison drop on hood
        g.setColor(new Color(0xAED581));
        g.fillOval(cx-2, cy-17, 5, 5);
        g.fillOval(cx-1, cy-13, 3, 5);

        // Ears
        g.setColor(new Color(0x8D6E63));
        g.fillOval(cx-13, cy-4, 5, 7); g.fillOval(cx+8, cy-4, 5, 7);
        g.setColor(new Color(0xBCAAA4));
        g.fillOval(cx-12, cy-3, 3, 5); g.fillOval(cx+9, cy-3, 3, 5);

        // Body
        g.setColor(new Color(0x8D6E63));
        g.fillOval(cx-11, cy-10, 22, 20);
        g.setColor(new Color(0xF5CBA7));
        g.fillOval(cx-7, cy-5, 14, 12);

        // Green tinted eyes — sinister
        g.setColor(new Color(0x76FF03));
        g.fillOval(cx-5, cy-4, 4, 3); g.fillOval(cx+1, cy-4, 4, 3);
        g.setColor(new Color(0x1B5E20));
        g.fillOval(cx-4, cy-4, 2, 2); g.fillOval(cx+2, cy-4, 2, 2);

        // Nose
        g.setColor(new Color(0xA0522D));
        g.fillOval(cx-2, cy+1, 4, 3);

        g.setColor(new Color(0x558B2F));
        g.setStroke(new BasicStroke(1.3f));
        g.drawOval(cx-11, cy-10, 22, 20);
        g.setStroke(new BasicStroke(1));

        // Poison aura pulse at A2+
        if (pathA >= 2) {
            float pulse = 0.3f + 0.3f*(float)Math.sin(animTick*0.1f);
            g.setColor(new Color(100, 200, 0, (int)(45*pulse)));
            g.fillOval(cx-14, cy-13, 28, 26);
        }
    }

    private void drawThornMonkey(Graphics2D g, int cx, int cy) {
        // Thorn crown — spikes around top
        g.setColor(new Color(0x4E342E));
        for (int a = -70; a <= 70; a += 28) {
            double rad = Math.toRadians(a - 90);
            int sx = cx + (int)(8*Math.cos(rad));
            int sy = cy + (int)(8*Math.sin(rad)) - 3;
            int tipX = cx + (int)(15*Math.cos(rad));
            int tipY = cy + (int)(15*Math.sin(rad)) - 3;
            g.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.drawLine(sx, sy, tipX, tipY);
        }
        g.setColor(new Color(0x795548));
        for (int a = -70; a <= 70; a += 28) {
            double rad = Math.toRadians(a - 90);
            int tipX = cx + (int)(15*Math.cos(rad));
            int tipY = cy + (int)(15*Math.sin(rad)) - 3;
            g.fillOval(tipX-2, tipY-2, 4, 4);
        }
        g.setStroke(new BasicStroke(1));

        // Ears
        g.setColor(new Color(0x8D6E63));
        g.fillOval(cx-13, cy-4, 5, 7); g.fillOval(cx+8, cy-4, 5, 7);
        g.setColor(new Color(0xBCAAA4));
        g.fillOval(cx-12, cy-3, 3, 5); g.fillOval(cx+9, cy-3, 3, 5);

        // Body
        g.setColor(new Color(0x8D6E63));
        g.fillOval(cx-11, cy-10, 22, 20);
        g.setColor(new Color(0xF5CBA7));
        g.fillOval(cx-7, cy-5, 14, 12);

        // Determined eyes
        g.setColor(new Color(0x3E2723));
        g.fillOval(cx-5, cy-4, 3, 3); g.fillOval(cx+2, cy-4, 3, 3);
        g.setColor(Color.WHITE);
        g.fillOval(cx-4, cy-5, 1, 1); g.fillOval(cx+3, cy-5, 1, 1);

        // Nose
        g.setColor(new Color(0xA0522D));
        g.fillOval(cx-2, cy+1, 4, 3);

        g.setColor(new Color(0x4E342E));
        g.setStroke(new BasicStroke(1.3f));
        g.drawOval(cx-11, cy-10, 22, 20);
        g.setStroke(new BasicStroke(1));

        // Extra thorns on body at A2+
        if (pathA >= 2) {
            g.setColor(new Color(0x3E2723));
            g.setStroke(new BasicStroke(2f));
            g.drawLine(cx-11, cy-3, cx-16, cy-7);
            g.drawLine(cx+11, cy-3, cx+16, cy-7);
            g.setStroke(new BasicStroke(1));
        }
    }

    public void drawRangePreview(Graphics2D g) {
        int cx = col*Constants.TILE+Constants.TILE/2;
        int cy = row*Constants.TILE+Constants.TILE/2;
        int rp = (int)(range*Constants.TILE);
        // Gradient fill
        g.setColor(new Color(255,255,255,20));
        g.fillOval(cx-rp, cy-rp, rp*2, rp*2);
        // Dashed ring
        g.setColor(new Color(255,255,255,100));
        g.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 0, new float[]{6,4}, 0));
        g.drawOval(cx-rp, cy-rp, rp*2, rp*2);
        g.setStroke(new BasicStroke(1));
    }

    // Getters
    public int getCol()    { return col; }
    public int getRow()    { return row; }
    public int getCost()   { return cost; }
    public TType getTType(){ return ttype; }
    public float getRange()    { return range; }
    public float getDamage()   { return damage; }
    public float getFireRate() { return fireRate; }
    public boolean isSelected()       { return selected; }
    public void setSelected(boolean s){ selected=s; }
    public String getName() {
        switch(ttype) {
            case DART:return"Dart Monkey"; 
            case BOMB:return"Bomb Shooter"; 
            case ICE:return"Ice Monkey";
            case SUPER:return"Super Monkey"; 
            case MORTAR:return"Mortar Monkey";
            case BANANA: return "Banana Farm";
            case POISON: return "Poison Monkey";
            case THORN:  return "Thorn Monkey";
            default:return"Monkey";
        }
    }
}