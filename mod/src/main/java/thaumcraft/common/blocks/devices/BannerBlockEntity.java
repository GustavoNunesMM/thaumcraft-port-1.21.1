package thaumcraft.common.blocks.devices;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.common.blocks.TCBlockEntity;
import thaumcraft.common.registers.ModDevices;

/** TileBanner: rotation (0-15, like vanilla banners), whether it hangs on a wall, and the aspect painted on it. */
public class BannerBlockEntity extends TCBlockEntity {
   private byte facing = 0;
   @Nullable
   private Aspect aspect = null;
   private boolean onWall = false;

   public BannerBlockEntity(BlockPos pos, BlockState state) {
      super(ModDevices.BANNER_BE.get(), pos, state);
   }

   public byte getBannerFacing() {
      return facing;
   }

   public void setBannerFacing(byte face) {
      facing = face;
      setChanged();
   }

   public boolean getWall() {
      return onWall;
   }

   public void setWall(boolean b) {
      onWall = b;
      setChanged();
   }

   @Nullable
   public Aspect getAspect() {
      return aspect;
   }

   public void setAspect(@Nullable Aspect aspect) {
      this.aspect = aspect;
   }

   public int getColor() {
      return getBlockState().getBlock() instanceof BannerBlock b ? b.getColor() : -1;
   }

   @Override
   protected void applyImplicitComponents(DataComponentInput input) {
      super.applyImplicitComponents(input);
      aspect = input.get(ModDevices.BANNER_ASPECT.get());
   }

   @Override
   protected void collectImplicitComponents(DataComponentMap.Builder builder) {
      super.collectImplicitComponents(builder);
      if (aspect != null) builder.set(ModDevices.BANNER_ASPECT.get(), aspect);
   }

   @Override
   public void removeComponentsFromTag(CompoundTag tag) {
      tag.remove("aspect");
   }

   @Override
   protected void readSync(CompoundTag tag, HolderLookup.Provider registries) {
      facing = tag.getByte("facing");
      String as = tag.getString("aspect");
      aspect = as.isEmpty() ? null : Aspect.getAspect(as);
      onWall = tag.getBoolean("wall");
   }

   @Override
   protected void writeSync(CompoundTag tag, HolderLookup.Provider registries) {
      tag.putByte("facing", facing);
      tag.putString("aspect", aspect == null ? "" : aspect.getTag());
      tag.putBoolean("wall", onWall);
   }
}
