import java.util.*;

public class WaveManager {
    private int currentWave = 0;
    private int spawnTimer = 0;
    private int spawnInterval = 50;
    private Queue<Enemy.Type> spawnQueue = new LinkedList<>();
    private boolean waveActive = false;
    private boolean allSpawned = false;

    // Wave definitions: each sub-array is {GOBLIN_count, ORC_count, TROLL_count, DRAGON_count, SHADE_count}
    private static final int[][] WAVE_DEFS = {
        {5, 0, 0, 0, 0},
        {8, 2, 0, 0, 0},
        {6, 4, 1, 0, 0},
        {8, 4, 0, 0, 3},
        {5, 6, 2, 1, 2},
        {10, 5, 2, 2, 3},
        {8, 8, 3, 2, 5},
        {12, 6, 4, 3, 4},
        {10, 10, 5, 4, 6},
        {15, 10, 6, 5, 8},
        {12, 12, 8, 6, 10},
        {20, 15, 8, 8, 10},
        {15, 15, 10, 10, 12},
        {20, 20, 12, 12, 15},
        {25, 20, 15, 15, 20},
    };

    public int getCurrentWave() { return currentWave; }
    public int getTotalWaves() { return WAVE_DEFS.length; }
    public boolean isWaveActive() { return waveActive; }
    public boolean isAllSpawned() { return allSpawned; }
    public boolean hasMoreWaves() { return currentWave < WAVE_DEFS.length; }

    public void startNextWave() {
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

        // Shuffle for variety
        Collections.shuffle(types);
        spawnQueue.addAll(types);

        // Tighten spawn interval on later waves
        spawnInterval = Math.max(25, 55 - currentWave * 2);
        spawnTimer = 0;
    }

    /** Returns a new enemy to spawn this tick, or null */
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

    public void waveComplete() {
        waveActive = false;
    }
}