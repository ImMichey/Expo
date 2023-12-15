package dev.michey.expo.server.util;

import dev.michey.expo.log.ExpoLogger;
import oshi.SystemInfo;
import oshi.hardware.GraphicsCard;

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
                builder.append(gpu.getName()).append(", deviceId=").append(gpu.getDeviceId()).append(", vendor=").append(gpu.getVendor()).append(", vRam=").append(gpu.getVendor()).append(", versionInfo=").append(gpu.getVersionInfo());
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

}