package dev.michey.expo.server.main.packet;

import com.esotericsoftware.kryo.Kryo;
import dev.michey.expo.noise.BiomeType;
import dev.michey.expo.server.main.logic.entity.arch.ServerEntityType;
import dev.michey.expo.server.main.logic.inventory.item.ItemMetadata;
import dev.michey.expo.server.main.logic.inventory.item.ServerInventoryItem;
import dev.michey.expo.server.main.logic.inventory.item.ToolType;
import dev.michey.expo.server.packet.*;

public class ExpoServerRegistry {

    public static void registerPackets(Kryo kryo) {
        register(kryo,
                // Java classes
                int[].class,
                boolean[].class,
                String[].class,

                // Util classes
                ServerEntityType.class,
                BiomeType.class,
                BiomeType[].class,
                ServerInventoryItem.class,
                ServerInventoryItem[].class,
                ItemMetadata.class,
                ToolType.class,

                // Packets
                P0_Auth_Req.class,
                P1_Auth_Rsp.class,
                P2_EntityCreate.class,
                P3_PlayerJoin.class,
                P4_EntityDelete.class,
                P5_PlayerVelocity.class,
                P6_EntityPosition.class,
                P7_ChunkSnapshot.class,
                P8_EntityDeleteStack.class,
                P9_PlayerCreate.class,
                P10_PlayerQuit.class,
                P11_ChunkData.class,
                P12_PlayerDirection.class,
                P13_EntityMove.class,
                P14_WorldUpdate.class,
                P15_PingList.class,
                P16_PlayerPunch.class,
                P17_PlayerPunchData.class,
                P18_PlayerInventoryInteraction.class,
                P19_PlayerInventoryUpdate.class,
                P20_PlayerInventorySwitch.class,
                P21_PlayerGearUpdate.class,
                P22_PlayerArmDirection.class,
                P23_PlayerLifeUpdate.class
        );
    }

    private static void register(Kryo kryo, Class<?>... classes) {
        for(Class<?> c : classes) {
            kryo.register(c);
        }
    }

}
