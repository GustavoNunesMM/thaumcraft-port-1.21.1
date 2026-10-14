package thaumcraft.common.blocks.devices;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.aspects.IAspectSource;
import thaumcraft.common.blocks.DeviceBlock;
import thaumcraft.common.lib.EssentiaHandler;
import thaumcraft.common.registers.ModDevices;

/**
 * TileMirrorEssentia: an essentia source for machines around it that really is every jar within 8 blocks in front of
 * the linked mirror. One essentia at a time.
 */
public class EssentiaMirrorBlockEntity extends MirrorBlockEntity implements IAspectSource {
   @Nullable
   public Direction linkedFacing = null;
   /** Guards against mirrors that can see each other's jars (the 1.12 ignoreMirror flag of EssentiaHandler). */
   private static boolean busy = false;

   public EssentiaMirrorBlockEntity(BlockPos pos, BlockState state) {
      super(ModDevices.MIRROR_ESSENTIA_BE.get(), pos, state);
   }

   @Override
   protected int instabilityLimit() {
      return 64;
   }

   @Override
   protected void onLinked(MirrorBlockEntity other) {
      linkedFacing = DeviceBlock.getFacing(other.getBlockState());
   }

   @Override
   protected void onUnlinked() {
      linkedFacing = null;
   }

   @Nullable
   private MirrorBlockEntity linkedMirror() {
      MirrorBlockEntity te = target();
      if (te != null && linkedFacing == null) linkedFacing = DeviceBlock.getFacing(te.getBlockState());
      return te;
   }

   @Override
   public AspectList getAspects() {
      return null;
   }

   @Override
   public void setAspects(AspectList aspects) {
   }

   @Override
   public boolean doesContainerAccept(Aspect tag) {
      MirrorBlockEntity te = linkedMirror();
      if (te == null) return true;
      if (busy) return false;
      busy = true;
      try {
         return EssentiaHandler.canAcceptEssentia(te, tag, linkedFacing, 8, true);
      } finally {
         busy = false;
      }
   }

   @Override
   public int addToContainer(Aspect tag, int amount) {
      if (!isLinkValid() || amount > 1) return amount;
      MirrorBlockEntity te = linkedMirror();
      if (te == null || busy) return amount;
      boolean b;
      busy = true;
      try {
         b = EssentiaHandler.addEssentia(te, tag, linkedFacing, 8, true, 5);
      } finally {
         busy = false;
      }
      if (b) addInstability(amount);
      return b ? 0 : 1;
   }

   @Override
   public boolean takeFromContainer(Aspect tag, int amount) {
      if (!isLinkValid() || amount > 1) return false;
      MirrorBlockEntity te = linkedMirror();
      if (te == null || busy) return false;
      boolean b;
      busy = true;
      try {
         b = EssentiaHandler.drainEssentia(te, tag, linkedFacing, 8, true, 5);
      } finally {
         busy = false;
      }
      if (b) addInstability(amount);
      return b;
   }

   @Override
   public boolean doesContainerContainAmount(Aspect tag, int amount) {
      if (!isLinkValid() || amount > 1) return false;
      MirrorBlockEntity te = linkedMirror();
      if (te == null || busy) return false;
      busy = true;
      try {
         return EssentiaHandler.findEssentia(te, tag, linkedFacing, 8, true);
      } finally {
         busy = false;
      }
   }

   @Override
   public int containerContains(Aspect tag) {
      return 0;
   }

   @Override
   public boolean isBlocked() {
      return false;
   }
}
