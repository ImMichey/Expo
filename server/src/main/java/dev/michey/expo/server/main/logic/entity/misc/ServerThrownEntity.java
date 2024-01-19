package dev.michey.expo.server.main.logic.entity.misc;

import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.Vector2;
import com.dongbat.jbump.Collision;
import dev.michey.expo.server.fs.world.entity.SavableEntity;
import dev.michey.expo.server.main.logic.entity.arch.PhysicsEntity;
import dev.michey.expo.server.main.logic.entity.arch.PhysicsMassClassification;
import dev.michey.expo.server.main.logic.entity.arch.ServerEntity;
import dev.michey.expo.server.main.logic.entity.arch.ServerEntityType;
import dev.michey.expo.server.main.logic.entity.flora.ServerOakTree;
import dev.michey.expo.server.main.logic.entity.player.ServerPlayer;
import dev.michey.expo.server.main.logic.inventory.item.mapping.ItemMapper;
import dev.michey.expo.server.main.logic.world.bbox.EntityPhysicsBox;
import dev.michey.expo.server.main.logic.world.bbox.PhysicsBoxFilters;
import dev.michey.expo.server.main.logic.world.chunk.ServerTile;
import dev.michey.expo.server.util.PacketReceiver;
import dev.michey.expo.server.util.ServerPackets;
import dev.michey.expo.util.EntityRemovalReason;
import dev.michey.expo.util.ExpoShared;
import org.json.JSONObject;

import java.util.Collection;

import static dev.michey.expo.util.ExpoShared.TILE_SIZE;

public class ServerThrownEntity extends ServerEntity implements PhysicsEntity {

    public int thrownItemId;
    public Vector2 originPos;
    public Vector2 dstPos;
    private Vector2 normPos;
    private float thrownProgress;
    public float thrownSpeed;

    private EntityPhysicsBox body;
    public int ignoreThrowerId;

    public float explosionRadius;
    public float explosionDamage;
    public float knockbackStrength;
    public float knockbackDuration;

    @Override
    public void onCreation() {
        normPos = dstPos.cpy().sub(originPos);
        body = new EntityPhysicsBox(this, 0, 0, 3, 3);
    }

    @Override
    public ServerEntityType getEntityType() {
        return ServerEntityType.THROWN_ENTITY;
    }

    @Override
    public void onDeletion() {
        body.dispose();

        if(thrownItemId == ItemMapper.get().getMapping("item_bomb").id || thrownItemId == ItemMapper.get().getMapping("item_nuke").id) {
            Collection<ServerEntity> check = getDimension().getEntityManager().getAllEntities();

            for(ServerEntity se : check) {
                if(se.entityId == entityId) continue;
                if(se.invincibility > 0) continue;

                if(se.health > 0) {
                    float rawDst = Vector2.dst(posX, posY, se.posX, se.posY);

                    if(rawDst <= explosionRadius) {
                        float relative = 1f - Interpolation.exp5Out.apply(rawDst / explosionRadius);
                        float applyDamage = explosionDamage * relative;

                        boolean applied = se.applyDamageWithPacket(this, applyDamage, EntityRemovalReason.EXPLOSION);

                        if(applied) {
                            float normRelative = 0.5f + relative * 0.5f;
                            se.addKnockback(knockbackStrength * normRelative, knockbackDuration * normRelative,
                                    new Vector2(se.posX, se.posY).sub(posX, posY).nor());
                        }

                        // Player packet
                        if(se instanceof ServerPlayer player) {
                            ServerPackets.p23PlayerLifeUpdate(player.health, player.hunger, PacketReceiver.player(player));
                        }
                    }
                }
            }

            float halfRadius = explosionRadius * 0.5f;  // 128 * 0.5f = 64
            float startWorldX = posX - halfRadius;      // 100 - 64 = 36
            float startWorldY = posY - halfRadius;      // 100 - 64 = 36

            int tilesPerRow = (int) (explosionRadius / TILE_SIZE);

            for(int i = 0; i < tilesPerRow; i++) {
                for(int j = 0; j < tilesPerRow; j++) {
                    int tx = ExpoShared.posToTile(startWorldX + i * TILE_SIZE);
                    int ty = ExpoShared.posToTile(startWorldY + j * TILE_SIZE);
                    float _x = ExpoShared.tileToPos(tx) + 8;
                    float _y = ExpoShared.tileToPos(ty) + 8;

                    float dst = Vector2.dst(_x, _y, posX, posY);

                    if(dst <= explosionRadius) {
                        ServerTile tile = getChunkGrid().getTile(tx, ty);

                        if(tile != null) {
                            int digLayer = tile.selectDigLayer();

                            if(digLayer != -1) {
                                float relative = 1f - Interpolation.exp5Out.apply(dst / explosionRadius);
                                float applyDamage = explosionDamage * relative * 0.5f;

                                tile.performDigOperation(applyDamage, null, false, true, posX, posY);
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    public void tick(float delta) {
        thrownProgress += delta * thrownSpeed;

        if(thrownProgress > 1) {
            thrownProgress = 1f;
            killEntityWithPacket();
        }

        float newPosX = originPos.x + normPos.x * thrownProgress;
        float newPosY = originPos.y + normPos.y * thrownProgress;

        var result = body.moveAbsolute(newPosX, newPosY, PhysicsBoxFilters.thrownFilter);

        posX = result.goalX - body.xOffset;
        posY = result.goalY - body.yOffset;

        if(!result.projectedCollisions.isEmpty()) {
            for(int i = 0; i < result.projectedCollisions.size(); i++) {
                Collision c = result.projectedCollisions.get(i);

                if(c.overlaps && c.other.userData instanceof PhysicsEntity pe) {
                    PhysicsMassClassification m = pe.getPhysicsMassClassification();

                    if(m == PhysicsMassClassification.HEAVY) {
                        if(pe instanceof ServerBoulder) {
                            continue;
                        }

                        if(pe instanceof ServerOakTree sot && sot.cut) {
                            continue;
                        }
                    }

                    if(m == PhysicsMassClassification.HEAVY || m == PhysicsMassClassification.PLAYER || m == PhysicsMassClassification.WALL) {
                        boolean isThrower = true;

                        if(c.other.userData instanceof ServerEntity se) {
                            isThrower = se.entityId == ignoreThrowerId;
                        }

                        if(!isThrower) {
                            killEntityWithPacket();
                            break;
                        }
                    }
                }
            }
        }
    }

    @Override
    public SavableEntity onSave() {
        return new SavableEntity(this).pack().add("id", thrownItemId).add("dt", thrownItemId).add("ox", originPos.x).add("oy", originPos.y).add("dx", dstPos.x).add("dy", dstPos.y);
    }

    @Override
    public void onLoad(JSONObject saved) {
        thrownItemId = saved.getInt("thrownItemId");
        originPos = new Vector2(saved.getFloat("ox"), saved.getFloat("oy"));
        dstPos = new Vector2(saved.getFloat("dx"), saved.getFloat("dy"));
        thrownProgress = saved.getFloat("dt");
    }

    @Override
    public Object[] getPacketPayload() {
        return new Object[] {thrownItemId, originPos.x, originPos.y, dstPos.x, dstPos.y, thrownProgress, thrownSpeed};
    }

    @Override
    public EntityPhysicsBox getPhysicsBox() {
        return body;
    }

    @Override
    public PhysicsMassClassification getPhysicsMassClassification() {
        return PhysicsMassClassification.ITEM;
    }

}