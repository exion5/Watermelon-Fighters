import java.util.*;

public class WaveManager {
    private int currentWave = 0;
    private int spawnTimer = 0;
    private int spawnInterval = 50;
    private Queue<Enemy.Type> spawnQueue = new LinkedList<>();
    private boolean waveActive = false;
    private boolean allSpawned = false;

    private static final int WAVE_BONUS_BASE  = 55; // base bonus for clearing a wave, plus a scaling amount per wave to reward later progress
    private static final int WAVE_BONUS_SCALE = 8;  // +8 per wave

    private static final int[][] WAVE_DEFS = { // each wave definition: number of each enemy type to spawn (Goblin, Orc, Troll, Dragon, Shade)
        { 5,  0,  0,  0,  0},  // W1  - gentle intro
        { 7,  2,  0,  0,  0},  // W2
        { 7,  4,  0,  0,  1},  // W3  - first shade
        { 9,  5,  1,  0,  2},  // W4  - first troll
        { 8,  7,  2,  1,  3},  // W5  - first dragon
        {12,  8,  2,  2,  5},  // W6
        {10, 10,  3,  2,  6},  // W7
        {14,  8,  4,  3,  6},  // W8
        {12, 12,  4,  4,  8},  // W9
        {16, 12,  6,  4,  8},  // W10
        {14, 14,  7,  5, 10},  // W11
        {18, 14,  8,  6, 10},  // W12
        {16, 16,  9,  8, 12},  // W13
        {20, 18, 10,  9, 14},  // W14
        {25, 22, 14, 12, 18},  // W15 – difficult ending
    };

    public int getCurrentWave()  { return currentWave; }
    public int getTotalWaves()   { return WAVE_DEFS.length; }
    public boolean isWaveActive(){ return waveActive; }
    public boolean isAllSpawned(){ return allSpawned; }
    public boolean hasMoreWaves(){ return currentWave < WAVE_DEFS.length; }

    public int waveClearBonus() { // gold bonus for completing a wave
        return WAVE_BONUS_BASE + (currentWave - 1) * WAVE_BONUS_SCALE;
    }

    public void startNextWave() { // sets up the next wave based on WAVE_DEFS, shuffling the spawn order for variety
        if (currentWave >= WAVE_DEFS.length) return;
        int[] def = WAVE_DEFS[currentWave];
        currentWave++;
        spawnQueue.clear();
        allSpawned = false;
        waveActive = true;

        List<Enemy.Type> types = new ArrayList<>();
        for (int i=0; i<def[0]; i++) types.add(Enemy.Type.GOBLIN);
        for (int i=0; i<def[1]; i++) types.add(Enemy.Type.ORC);
        for (int i=0; i<def[2]; i++) types.add(Enemy.Type.TROLL);
        for (int i=0; i<def[3]; i++) types.add(Enemy.Type.DRAGON);
        for (int i=0; i<def[4]; i++) types.add(Enemy.Type.SHADE);

        Collections.shuffle(types);
        spawnQueue.addAll(types);

        spawnInterval = Math.max(18, 45 - currentWave * 2); // faster spawns in later waves, but never less than 18 frames apart
        spawnTimer = 0;
    }

    public Enemy update() {
        if (!waveActive || allSpawned) return null;
        spawnTimer++;
        if (spawnTimer >= spawnInterval) {
            spawnTimer = 0;
            if (!spawnQueue.isEmpty()) {
                Enemy.Type t = spawnQueue.poll();
                if (spawnQueue.isEmpty()) allSpawned = true;
                return new Enemy(t);
            }
        }
        return null;
    }

    public void waveComplete() { waveActive = false; }
}