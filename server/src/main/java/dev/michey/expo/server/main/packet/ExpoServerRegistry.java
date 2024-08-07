package dev.michey.expo.server.main.packet;

import com.esotericsoftware.kryo.Kryo;
import dev.michey.expo.noise.BiomeType;
import dev.michey.expo.noise.TileLayerType;
import dev.michey.expo.server.main.logic.entity.arch.ServerEntityType;
import dev.michey.expo.server.main.logic.entity.flora.ServerOakTree;
import dev.michey.expo.server.main.logic.inventory.InventoryViewType;
import dev.michey.expo.server.main.logic.inventory.ServerInventorySlot;
import dev.michey.expo.server.main.logic.inventory.item.ItemMetadata;
import dev.michey.expo.server.main.logic.inventory.item.ServerInventoryItem;
import dev.michey.expo.server.main.logic.inventory.item.ToolType;
import dev.michey.expo.server.main.logic.world.chunk.DynamicTilePart;
import dev.michey.expo.server.main.logic.world.gen.*;
import dev.michey.expo.server.packet.*;
import dev.michey.expo.server.util.TeleportReason;
import dev.michey.expo.util.EntityRemovalReason;

import java.util.HashMap;
import java.util.LinkedList;

public class ExpoServerRegistry {

    public static void registerPackets(Kryo kryo) {
        register(kryo,
                // Java classes
                int[].class,
                int[][].class,
                boolean[].class,
                float[].class,
                byte[].class,
                String[].class,
                Object[].class,
                HashMap.class,
                LinkedList.class,

                // Util classes
                ServerEntityType.class,
                ServerEntityType[].class,
                BiomeType.class,
                BiomeType[].class,
                ServerInventoryItem.class,
                ServerInventoryItem[].class,
                ItemMetadata.class,
                ToolType.class,
                EntityRemovalReason.class,
                EntityRemovalReason[].class,
                WorldGenNoiseSettings.class,
                NoiseWrapper.class,
                NoisePostProcessor.class,
                TileLayerType.class,
                TileLayerType[].class,
                TileLayerType[][].class,
                DynamicTilePart.class,
                DynamicTilePart[].class,
                DynamicTilePart[][].class,
                PostProcessorLogic.class,
                PostProcessorLayer.class,
                PostProcessorBiome.class,
                InventoryViewType.class,
                ServerInventorySlot.class,
                ServerInventorySlot[].class,
                TeleportReason.class,
                BiomeDefinition.class,
                ServerOakTree.TreeData.class,
                ServerOakTree.BeehiveData.class,

                // Packets
                P0_Connect_Req.class,
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
                P19_ContainerUpdate.class,
                P20_PlayerInventorySwitch.class,
                P21_PlayerGearUpdate.class,
                P22_PlayerArmDirection.class,
                P23_PlayerLifeUpdate.class,
                P24_PositionalSound.class,
                P25_ChatMessage.class,
                P26_EntityDamage.class,
                P27_PlayerEntitySelection.class,
                P28_PlayerFoodParticle.class,
                P29_EntityCreateAdvanced.class,
                P30_EntityDataUpdate.class,
                P31_PlayerDig.class,
                P32_ChunkDataSingle.class,
                P33_TileDig.class,
                P34_PlayerPlace.class,
                P35_PlayerCraft.class,
                P36_PlayerReceiveItem.class,
                P37_EntityTeleport.class,
                P38_PlayerAnimation.class,
                P39_PlayerInteractEntity.class,
                P40_InventoryView.class,
                P41_InventoryViewQuit.class,
                P42_EntityAnimation.class,
                P43_EntityDeleteAdvanced.class,
                P44_Connect_Rsp.class,
                P45_Auth_Req.class,
                P46_EntityConstruct.class,
                P47_ItemConsume.class,
                P48_ClientPlayerPosition.class,
                P49_PlayerEntityThrow.class,
                P50_TileFullUpdate.class,
                P51_PositionalSoundAdvanced.class,
                P52_TranslatableChatMessage.class
        );
    }

    private static void register(Kryo kryo, Class<?>... classes) {
        for(Class<?> c : classes) {
            kryo.register(c);
        }
    }

}
