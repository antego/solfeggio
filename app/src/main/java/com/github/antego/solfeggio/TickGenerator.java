package com.github.antego.solfeggio;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;


public class TickGenerator {
    public Map<Float, String> generateTicks(int samplingRate) {
        float minFreq = (float) samplingRate / MicrophoneListener.BUFFER_SIZE * 2;
        List<Float> ticks = new LinkedList<>();
        for (int i = 1; i < samplingRate / 1000 + 1; i++) {
            ticks.add((float)i*1000);
        }

        Map<Float, String> ticksWithLabels = new LinkedHashMap<>();
        for (Float tickFreq : ticks) {
            float pos = 2 * (tickFreq - minFreq) / (samplingRate - minFreq) - 1;
            ticksWithLabels.put(pos, (int)(tickFreq / 1000) + "");
        }
        return ticksWithLabels;
    }
}
