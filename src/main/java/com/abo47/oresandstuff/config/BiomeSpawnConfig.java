package com.abo47.oresandstuff.config;

import java.util.ArrayList;
import java.util.List;

public class BiomeSpawnConfig {
    public int weight = 1;
    public int minY = 0;
    public int maxY = 63;
    public boolean surfaceSpawn = false;
    public List<QualityBand> qualityBands = new ArrayList<>();
}
