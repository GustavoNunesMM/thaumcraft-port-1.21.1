package thaumcraft.api.golems;

import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import thaumcraft.api.golems.seals.ISealEntity;
import thaumcraft.api.golems.tasks.Task;

public class ProvisionRequest {
   private ISealEntity seal;
   private Entity entity;
   private BlockPos pos;
   private EnumFacing side;
   private ItemStack stack;
   private int id;
   private int ui = 0;
   private Task linkedTask;
   private boolean invalid;
   private long timeout;

   ProvisionRequest(ISealEntity seal, ItemStack stack) {
      this.seal = seal;
      this.stack = stack.copy();
      String s = seal.getSealPos().pos.toString() + seal.getSealPos().face.name() + stack.toString();
      if (stack.hasTagCompound()) {
         s = s + stack.getTagCompound().toString();
      }

      this.id = s.hashCode();
      this.timeout = System.currentTimeMillis() + 10000L;
   }

   ProvisionRequest(BlockPos pos, EnumFacing side, ItemStack stack) {
      this.pos = pos;
      this.side = side;
      this.stack = stack.copy();
      String s = pos.toString() + side.name() + stack.toString();
      if (stack.hasTagCompound()) {
         s = s + stack.getTagCompound().toString();
      }

      this.id = s.hashCode();
      this.timeout = System.currentTimeMillis() + 10000L;
   }

   ProvisionRequest(Entity entity, ItemStack stack) {
      this.entity = entity;
      this.stack = stack.copy();
      String s = entity.getEntityId() + stack.toString();
      if (stack.hasTagCompound()) {
         s = s + stack.getTagCompound().toString();
      }

      this.id = s.hashCode();
      this.timeout = System.currentTimeMillis() + 10000L;
   }

   public long getTimeout() {
      return this.timeout;
   }

   public int getId() {
      return this.id;
   }

   public void setId(int id) {
      this.id = id;
   }

   public void setUI(int ui) {
      this.ui = ui;
   }

   public ISealEntity getSeal() {
      return this.seal;
   }

   public Entity getEntity() {
      return this.entity;
   }

   public ItemStack getStack() {
      return this.stack;
   }

   public BlockPos getPos() {
      return this.pos;
   }

   public void setPos(BlockPos pos) {
      this.pos = pos;
   }

   public EnumFacing getSide() {
      return this.side;
   }

   public void setSide(EnumFacing side) {
      this.side = side;
   }

   public Task getLinkedTask() {
      return this.linkedTask;
   }

   public void setLinkedTask(Task linkedTask) {
      this.linkedTask = linkedTask;
      this.timeout = System.currentTimeMillis() + 120000L;
   }

   public boolean isInvalid() {
      return this.invalid;
   }

   public void setInvalid(boolean invalid) {
      this.invalid = invalid;
   }

   @Override
   public boolean equals(Object p_equals_1_) {
      if (this == p_equals_1_) {
         return true;
      } else if (!(p_equals_1_ instanceof ProvisionRequest)) {
         return false;
      } else {
         ProvisionRequest pr = (ProvisionRequest)p_equals_1_;
         return this.id == pr.id && this.ui == pr.ui;
      }
   }

   private boolean isItemStackEqual(ItemStack first, ItemStack other) {
      return first.getCount() != other.getCount()
         ? false
         : (
            first.getItem() != other.getItem()
               ? false
               : (
                  first.getItemDamage() != other.getItemDamage()
                     ? false
                     : (
                        first.getTagCompound() == null && other.getTagCompound() != null
                           ? false
                           : first.getTagCompound() == null || first.getTagCompound().equals(other.getTagCompound())
                     )
               )
         );
   }
}
