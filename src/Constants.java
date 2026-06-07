public class Constants {
    public static final int GAME_WIDTH = 900; // main window
    public static final int GAME_HEIGHT = 660;
    public static final int UI_WIDTH = 220;
    public static final int TOTAL_WIDTH = GAME_WIDTH + UI_WIDTH;
    public static final int TOTAL_HEIGHT = GAME_HEIGHT;

    public static final int MAP_PAD = 10; // black padding around the map

    public static final int TILE = 40; // grid pattern size
    public static final int COLS = GAME_WIDTH  / TILE; // 22
    public static final int ROWS = GAME_HEIGHT / TILE; // 16

    public static final int STARTING_LIVES = 20;
    public static final int STARTING_CURRENCY = 200;
    public static final int FPS = 60;

    public static final int[][] PATH = { // predefined path for balloons
        {0,2},{1,2},{2,2},{3,2},{4,2},{5,2},{6,2},{6,3},{6,4},{6,5},{6,6},
        {7,6},{8,6},{9,6},{10,6},{11,6},{12,6},{12,5},{12,4},{12,3},{12,2},
        {13,2},{14,2},{15,2},{16,2},{16,3},{16,4},{16,5},{16,6},{16,7},{16,8},
        {15,8},{14,8},{13,8},{12,8},{11,8},{10,8},{9,8},{8,8},{7,8},{6,8},
        {5,8},{4,8},{4,9},{4,10},{4,11},{4,12},{5,12},{6,12},{7,12},{8,12},
        {9,12},{10,12},{11,12},{12,12},{13,12},{14,12},{15,12},{16,12},{17,12},
        {18,12},{19,12},{20,12},{21,12},{21,13}
    };

    public static final Object[][] TOWER_DEFS = { // the different tower types and stats
        {"Dart Monkey", 80, 3.5, 15, 40, new java.awt.Color(0xE53935)},
        {"Bomb Shooter", 150, 2.5, 60, 90, new java.awt.Color(0x37474F)},
        {"Ice Monkey", 120, 3.0, 8, 50, new java.awt.Color(0x29B6F6)},
        {"Super Monkey", 200, 4.0, 25, 20, new java.awt.Color(0xFFD600)},
        {"Mortar Monkey", 175, 5.0, 80, 120, new java.awt.Color(0x558B2F)},
        {"Banana Farm",   120, 0.0, 0,  0,  new java.awt.Color(0xFFD600)},
        {"Poison Monkey", 130, 3.0, 12, 45, new java.awt.Color(0x7CB342)},
        {"Thorn Monkey",  110, 3.5, 20, 55, new java.awt.Color(0x795548)},
    };
}