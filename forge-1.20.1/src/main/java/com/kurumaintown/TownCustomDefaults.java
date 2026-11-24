package com.kurumaintown;

/** Keep array literal initialisation outside Mixin classes. */
public final class TownCustomDefaults {
    private static final String[] KEYS = {"enginecustom", "steeringcustom", "suspensioncustom", "tankcustom", "armorcustom", "brakecustom"};
    private TownCustomDefaults() {}
    public static int count() { return KEYS.length; }
    public static String key(int index) { return KEYS[index]; }
    public static int[] maxValues() { return new int[] {3, 3, 3, 3, 5, 3}; }
}
