package thaumcraft.api.golems.tasks;

import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import thaumcraft.api.golems.GolemHelper;
import thaumcraft.api.golems.IGolemAPI;
import thaumcraft.api.golems.ProvisionRequest;
import thaumcraft.api.golems.seals.ISealEntity;
import thaumcraft.api.golems.seals.SealPos;

/** A job handed out by a seal (or a provisioning request): go to a block (type 0) or to an entity (type 1). */
public class Task {
   private UUID golemUUID;
   private int id;
   private byte type;
   private SealPos sealPos;
   private BlockPos pos;
   private Entity entity;
   private boolean reserved;
   private boolean suspended;
   private boolean completed;
   private int data;
   private ProvisionRequest linkedProvision;
   private short lifespan;
   private byte priority = 0;

   public Task(@Nullable SealPos sealPos, BlockPos pos) {
      this.sealPos = sealPos;
      this.pos = pos.immutable();
      if (sealPos == null) {
         this.id = (System.currentTimeMillis() + "/BNPOS/" + pos).hashCode();
      } else {
         this.id = (System.currentTimeMillis() + "/B/" + sealPos.face + "/" + sealPos.pos + "/" + pos).hashCode();
      }
      this.type = 0;
      this.lifespan = 300;
   }

   public Task(@Nullable SealPos sealPos, Entity entity) {
      this.sealPos = sealPos;
      this.entity = entity;
      if (sealPos == null) {
         this.id = (System.currentTimeMillis() + "/ENPOS/" + entity.getId()).hashCode();
      } else {
         this.id = (System.currentTimeMillis() + "/E/" + sealPos.face + "/" + sealPos.pos + "/" + entity.getId()).hashCode();
      }
      this.type = 1;
      this.lifespan = 300;
   }

   public byte getPriority() {
      return priority;
   }

   public void setPriority(byte priority) {
      this.priority = priority;
   }

   public boolean isCompleted() {
      return completed;
   }

   public void setCompletion(boolean fulfilled) {
      this.completed = fulfilled;
      this.lifespan++;
   }

   public UUID getGolemUUID() {
      return golemUUID;
   }

   public void setGolemUUID(UUID golemUUID) {
      this.golemUUID = golemUUID;
   }

   public BlockPos getPos() {
      return type == 1 ? entity.blockPosition() : pos;
   }

   public byte getType() {
      return type;
   }

   public Entity getEntity() {
      return entity;
   }

   public int getId() {
      return id;
   }

   public boolean isReserved() {
      return reserved;
   }

   public void setReserved(boolean res) {
      this.reserved = res;
      this.lifespan = (short) (this.lifespan + 120);
   }

   public boolean isSuspended() {
      return suspended;
   }

   public void setSuspended(boolean suspended) {
      setLinkedProvision(null);
      this.suspended = suspended;
   }

   @Nullable
   public SealPos getSealPos() {
      return sealPos;
   }

   @Override
   public boolean equals(Object o) {
      return o instanceof Task t && t.id == this.id;
   }

   @Override
   public int hashCode() {
      return id;
   }

   public long getLifespan() {
      return lifespan;
   }

   public void setLifespan(short ls) {
      this.lifespan = ls;
   }

   public boolean canGolemPerformTask(IGolemAPI golem) {
      ISealEntity se = GolemHelper.getSealEntity(golem.getGolemWorld(), sealPos);
      if (se != null) {
         if (golem.getGolemColor() > 0 && se.getColor() > 0 && golem.getGolemColor() != se.getColor()) return false;
         return se.getSeal().canGolemPerformTask(golem, this);
      }
      return true;
   }

   public int getData() {
      return data;
   }

   public void setData(int data) {
      this.data = data;
   }

   public ProvisionRequest getLinkedProvision() {
      return linkedProvision;
   }

   public void setLinkedProvision(ProvisionRequest linkedProvision) {
      this.linkedProvision = linkedProvision;
   }
}
