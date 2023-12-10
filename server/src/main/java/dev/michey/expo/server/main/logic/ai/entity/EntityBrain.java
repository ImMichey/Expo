package dev.michey.expo.server.main.logic.ai.entity;

import com.badlogic.gdx.math.Vector2;
import dev.michey.expo.server.main.logic.ai.AIConstants;
import dev.michey.expo.server.main.logic.entity.arch.ServerEntity;
import dev.michey.expo.server.util.PacketReceiver;
import dev.michey.expo.server.util.ServerPackets;

import java.util.HashMap;

public class EntityBrain {

    private final ServerEntity parentEntity;
    private final HashMap<Integer, BrainModule> modules;
    private BrainModule activeModule;
    private final Vector2 lastMovementDir = new Vector2();

    public EntityBrain(ServerEntity parentEntity) {
        this.parentEntity = parentEntity;
        modules = new HashMap<>();
    }

    public void addBrainModule(BrainModule module) {
        modules.put(module.getType(), module);
        module.setBrain(this);
        module.init();
        if(modules.size() == 1) setActiveModule(module);
    }

    public void setActiveModule(BrainModule module) {
        if(activeModule != null) activeModule.onEnd();
        activeModule = module;
        activeModule.onStart();
    }

    public void setActiveModuleIfExisting(int type) {
        BrainModule existing = modules.get(type);

        if(existing == null) {
            setActiveModule(modules.get(0));
        } else {
            setActiveModule(existing);
        }
    }

    public void resetMovementPacket() {
        ServerPackets.p13EntityMove(parentEntity.entityId, parentEntity.velToPos(lastMovementDir.x), parentEntity.velToPos(lastMovementDir.y), parentEntity.posX, parentEntity.posY, 0, PacketReceiver.whoCanSee(parentEntity));
    }

    public void resetModule() {
        setActiveModule(modules.get(0));
    }

    public void notifyAttacked(ServerEntity attackerEntity, float attackDamage) {
        var flee = modules.get(AIConstants.FLEE);

        if(flee != null) {
            BrainModuleFlee fleeModule = (BrainModuleFlee) flee;
            fleeModule.setMetadata(attackerEntity.entityId, attackDamage);
            setActiveModule(fleeModule);
        }
    }

    public void tickBrain(float delta) {
        activeModule.tick(delta);
    }

    public ServerEntity getEntity() {
        return parentEntity;
    }

    public void setLastMovementDirection(Vector2 dirVector) {
        lastMovementDir.x = dirVector.x;
        lastMovementDir.y = dirVector.y;
    }

    public BrainModule hasModule(int id) {
        return modules.get(id);
    }

    public Vector2 getLastMovementDirection() {
        return lastMovementDir;
    }

    public String getActiveModule() {
        if(activeModule.getType() == AIConstants.IDLE) return "IDLE";
        if(activeModule.getType() == AIConstants.CHASE) return "CHASE";
        if(activeModule.getType() == AIConstants.STROLL) return "STROLL";
        if(activeModule.getType() == AIConstants.FLEE) return "FLEE";
        if(activeModule.getType() == AIConstants.ATTACK) return "ATTACK";
        return "<UNKNOWN>";
    }

}
