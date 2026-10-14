package thaumcraft.api.golems;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import thaumcraft.api.golems.seals.ISealEntity;
import thaumcraft.api.golems.tasks.Task;

/** A request for items, answered by provider seals: for a seal, a block face or an entity (a player). */
public class ProvisionRequest {
   private ISealEntity seal;
   private Entity entity;
   private BlockPos pos;
   private Direction side;
   private final ItemStack stack;
   private int id;
   private int ui = 0;
   private Task linkedTask;
   private boolean invalid;
   private long timeout;

   ProvisionRequest(ISealEntity seal, ItemStack stack) {
      this.seal = seal;
      this.stack = stack.copy();
      this.id = (seal.getSealPos().pos.toString() + seal.getSealPos().face.name() + key(stack)).hashCode();
      this.timeout = System.currentTimeMillis() + 10000L;
   }

   ProvisionRequest(BlockPos pos, Direction side, ItemStack stack) {
      this.pos = pos;
      this.side = side;
      this.stack = stack.copy();
      this.id = (pos.toString() + side.name() + key(stack)).hashCode();
      this.timeout = System.currentTimeMillis() + 10000L;
   }

   ProvisionRequest(Entity entity, ItemStack stack) {
      this.entity = entity;
      this.stack = stack.copy();
      this.id = (entity.getId() + key(stack)).hashCode();
      this.timeout = System.currentTimeMillis() + 10000L;
   }

   private static String key(ItemStack stack) {
      return stack.toString() + stack.getComponentsPatch();
   }

   public long getTimeout() {
      return timeout;
   }

   public int getId() {
      return id;
   }

   public void setId(int id) {
      this.id = id;
   }

   public void setUI(int ui) {
      this.ui = ui;
   }

   public ISealEntity getSeal() {
      return seal;
   }

   public Entity getEntity() {
      return entity;
   }

   public ItemStack getStack() {
      return stack;
   }

   public BlockPos getPos() {
      return pos;
   }

   public void setPos(BlockPos pos) {
      this.pos = pos;
   }

   public Direction getSide() {
      return side;
   }

   public void setSide(Direction side) {
      this.side = side;
   }

   public Task getLinkedTask() {
      return linkedTask;
   }

   public void setLinkedTask(Task linkedTask) {
      this.linkedTask = linkedTask;
      this.timeout = System.currentTimeMillis() + 120000L;
   }

   public boolean isInvalid() {
      return invalid;
   }

   public void setInvalid(boolean invalid) {
      this.invalid = invalid;
   }

   @Override
   public boolean equals(Object o) {
      return this == o || o instanceof ProvisionRequest pr && id == pr.id && ui == pr.ui;
   }

   @Override
   public int hashCode() {
      return id * 31 + ui;
   }
}
