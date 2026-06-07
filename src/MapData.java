import java.awt.Color;

public class MapData {

    public final String  name;
    public final String  description;
    public final int[][] path;
    public final Color grassA; // even tiles
    public final Color grassB; // odd tiles
    public final Color pathColor;
    public final Color accentColor; // UI accent for this map

    private MapData(String name, String description, int[][] path,
                    Color grassA, Color grassB, Color pathColor, Color accentColor) {
        this.name        = name;
        this.description = description;
        this.path        = path;
        this.grassA      = grassA;
        this.grassB      = grassB;
        this.pathColor   = pathColor;
        this.accentColor = accentColor;
    }

    private static final int[][] PATH_MEADOW = { // first map path: gentle S-curve through grassy fields
        {0,2},{1,2},{2,2},{3,2},{4,2},{5,2},{6,2},
        {6,3},{6,4},{6,5},{6,6},
        {7,6},{8,6},{9,6},{10,6},{11,6},{12,6},
        {12,5},{12,4},{12,3},{12,2},
        {13,2},{14,2},{15,2},{16,2},
        {16,3},{16,4},{16,5},{16,6},{16,7},{16,8},
        {15,8},{14,8},{13,8},{12,8},{11,8},{10,8},{9,8},{8,8},{7,8},{6,8},{5,8},{4,8},
        {4,9},{4,10},{4,11},{4,12},
        {5,12},{6,12},{7,12},{8,12},{9,12},{10,12},{11,12},{12,12},
        {13,12},{14,12},{15,12},{16,12},{17,12},{18,12},{19,12},{20,12},{21,12},
        {21,13}
    };

    private static final int[][] PATH_DESERT = { // second map path: zigzag with long vertical drops, through sandy terrain
        {0,1},{1,1},{2,1},{3,1},{4,1},{5,1},
        {5,2},{5,3},{5,4},{5,5},{5,6},{5,7},{5,8},{5,9},{5,10},{5,11},{5,12},{5,13},
        {6,13},{7,13},{8,13},{9,13},{10,13},
        {10,12},{10,11},{10,10},{10,9},{10,8},{10,7},{10,6},{10,5},{10,4},{10,3},{10,2},{10,1},
        {11,1},{12,1},{13,1},{14,1},{15,1},
        {15,2},{15,3},{15,4},{15,5},{15,6},{15,7},{15,8},{15,9},{15,10},{15,11},{15,12},{15,13},
        {16,13},{17,13},{18,13},{19,13},{20,13},{21,13},{21,14}
    };

    private static final int[][] PATH_SPIRAL = { // third map path: tight spiral vortex, enemies take a long route to exit
        // Enter top-left, sweep right along row 0
        {0,0},{1,0},{2,0},{3,0},{4,0},{5,0},{6,0},{7,0},{8,0},{9,0},{10,0},
        {11,0},{12,0},{13,0},{14,0},{15,0},{16,0},{17,0},{18,0},{19,0},{20,0},{21,0},
        // Drop down right edge
        {21,1},{21,2},{21,3},{21,4},{21,5},{21,6},{21,7},{21,8},{21,9},{21,10},{21,11},{21,12},
        // Sweep left along row 12
        {20,12},{19,12},{18,12},{17,12},{16,12},{15,12},{14,12},{13,12},{12,12},
        {11,12},{10,12},{9,12},{8,12},{7,12},{6,12},{5,12},{4,12},{3,12},{2,12},{1,12},
        // Up left edge
        {1,11},{1,10},{1,9},{1,8},{1,7},{1,6},{1,5},{1,4},{1,3},{1,2},
        // Short right sweep row 2
        {2,2},{3,2},{4,2},{5,2},{6,2},{7,2},{8,2},{9,2},{10,2},{11,2},{12,2},{13,2},{14,2},{15,2},{16,2},{17,2},{18,2},{19,2},
        // Drop to row 10
        {19,3},{19,4},{19,5},{19,6},{19,7},{19,8},{19,9},{19,10},
        // Short left sweep row 10
        {18,10},{17,10},{16,10},{15,10},{14,10},{13,10},{12,10},{11,10},{10,10},{9,10},{8,10},{7,10},{6,10},{5,10},{4,10},{3,10},
        // Drop to exit
        {3,11},{3,12},{3,13},{3,14},{3,15}
    };

    private static final int[][] PATH_RIVER = { // fourth map path: follow the river with limited tower spots, enemies enter from left-middle
        {0,7},{1,7},{2,7},{3,7},{4,7},
        {4,6},{4,5},{4,4},{4,3},{4,2},
        {5,2},{6,2},{7,2},{8,2},{9,2},{10,2},{11,2},{12,2},{13,2},{14,2},{15,2},{16,2},{17,2},
        {17,3},{17,4},{17,5},{17,6},{17,7},{17,8},{17,9},
        {16,9},{15,9},{14,9},{13,9},{12,9},{11,9},{10,9},{9,9},{8,9},{7,9},{6,9},{5,9},{4,9},
        {4,10},{4,11},{4,12},{4,13},
        {5,13},{6,13},{7,13},{8,13},{9,13},{10,13},{11,13},{12,13},{13,13},{14,13},{15,13},{16,13},{17,13},{18,13},{19,13},{20,13},{21,13},
        {21,14}
    };

    private static final int[][] PATH_GAUNTLET = { // fifth map path: four long lanes with tight U-turns, enemies enter from top-left
        {0,1},{1,1},{2,1},{3,1},{4,1},{5,1},{6,1},{7,1},{8,1},{9,1},
        {10,1},{11,1},{12,1},{13,1},{14,1},{15,1},{16,1},{17,1},{18,1},{19,1},{20,1},
        {20,2},{20,3},{20,4},
        {19,4},{18,4},{17,4},{16,4},{15,4},{14,4},{13,4},{12,4},{11,4},{10,4},
        {9,4},{8,4},{7,4},{6,4},{5,4},{4,4},{3,4},{2,4},{1,4},
        {1,5},{1,6},{1,7},
        {2,7},{3,7},{4,7},{5,7},{6,7},{7,7},{8,7},{9,7},{10,7},{11,7},{12,7},
        {13,7},{14,7},{15,7},{16,7},{17,7},{18,7},{19,7},{20,7},
        {20,8},{20,9},{20,10},
        {19,10},{18,10},{17,10},{16,10},{15,10},{14,10},{13,10},{12,10},{11,10},
        {10,10},{9,10},{8,10},{7,10},{6,10},{5,10},{4,10},{3,10},{2,10},{1,10},
        {1,11},{1,12},{1,13},
        {2,13},{3,13},{4,13},{5,13},{6,13},{7,13},{8,13},{9,13},{10,13},
        {11,13},{12,13},{13,13},{14,13},{15,13},{16,13},{17,13},{18,13},{19,13},{20,13},{21,13},
        {21,14}
    };

    public static final MapData[] ALL = { // all map options with their paths and colors
        new MapData(
            "Meadow Loop",
            "Classic S-curve through green fields",
            PATH_MEADOW,
            new Color(0x2E7D32), new Color(0x388E3C),
            new Color(0xC2A45A),
            new Color(0x66BB6A)
        ),
        new MapData(
            "Desert Zigzag",
            "Harsh vertical drops across scorched sand",
            PATH_DESERT,
            new Color(0xB8860B), new Color(0xDAA520),
            new Color(0xD2691E),
            new Color(0xFF8C00)
        ),
        new MapData(
            "Spiral Vortex",
            "Winding spiral — enemies travel a long road",
            PATH_SPIRAL,
            new Color(0x1A237E), new Color(0x283593),
            new Color(0x9E9E9E),
            new Color(0x7986CB)
        ),
        new MapData(
            "River Fork",
            "Follow the river — limited tower spots",
            PATH_RIVER,
            new Color(0x1B5E20), new Color(0x2E7D32),
            new Color(0x1565C0),
            new Color(0x4FC3F7)
        ),
        new MapData(
            "Gauntlet",
            "Four long lanes with tight U-turns",
            PATH_GAUNTLET,
            new Color(0x4A148C), new Color(0x6A1B9A),
            new Color(0xAA7744),
            new Color(0xCE93D8)
        ),
    };
}