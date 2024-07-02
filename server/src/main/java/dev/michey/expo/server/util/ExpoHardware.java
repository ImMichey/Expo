package dev.michey.expo.server.util;

import dev.michey.expo.log.ExpoLogger;
import oshi.SystemInfo;
import oshi.hardware.GraphicsCard;

import java.text.CharacterIterator;
import java.text.StringCharacterIterator;

public class ExpoHardware {

    public static SystemInfo si = new SystemInfo();

    public static void dump() {
        var cpu = si.getHardware().getProcessor();
        ExpoLogger.log("CPU: " + cpu.toString());

        var list = si.getHardware().getGraphicsCards();
        String gpuString;

        if(list.isEmpty()) {
            gpuString = "GPU: /";
        } else {
            StringBuilder builder = new StringBuilder();

            for(int i = 0; i < list.size(); i++) {
                builder.append("GPU #").append(i);
                builder.append(" [");

                GraphicsCard gpu = list.get(i);
                builder.append(gpu.getName()).append(", deviceId=").append(gpu.getDeviceId()).append(", vendor=").append(gpu.getVendor()).append(", vRam=").append(humanReadableByteCountBin(gpu.getVRam())).append(", versionInfo=").append(gpu.getVersionInfo());
                builder.append("]");

                if(i < (list.size() - 1)) {
                    builder.append(System.lineSeparator());
                }
            }

            gpuString = builder.toString();
        }
        ExpoLogger.log(gpuString);

        ExpoLogger.log("Memory: " + si.getHardware().getMemory().toString());
    }

    public static String humanReadableByteCountBin(long bytes) {
        long absB = bytes == Long.MIN_VALUE ? Long.MAX_VALUE : Math.abs(bytes);

        if(absB < 1024) {
            return bytes + " B";
        }

        long value = absB;
        CharacterIterator ci = new StringCharacterIterator("KMGTPE");

        for(int i = 40; i >= 0 && absB > 0xfffccccccccccccL >> i; i -= 10) {
            value >>= 10;
            ci.next();
        }

        value *= Long.signum(bytes);
        return String.format("%.1f %ciB", value / 1024.0, ci.current());
    }

}