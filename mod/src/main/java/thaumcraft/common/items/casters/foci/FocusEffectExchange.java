package thaumcraft.common.items.casters.foci;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.casters.FocusEffect;
import thaumcraft.api.casters.ICaster;
import thaumcraft.api.casters.IFocusBlockPicker;
import thaumcraft.api.casters.NodeSetting;
import thaumcraft.api.casters.Trajectory;

/** Exchange: swaps the block hit for the block picked with the caster (sneak + right click), from the inventory. */
public class FocusEffectExchange extends FocusEffect implements IFocusBlockPicker {
   @Override
   public String getResearch() {
      return "FOCUSEXCHANGE";
   }

   @Override
   public String getKey() {
      return "thaumcraft.EXCHANGE";
   }

   @Override
   public Aspect getAspect() {
      return Aspect.EXCHANGE;
   }

   /** As in the original, operator precedence makes this just the fortune term. */
   @Override
   public int getComplexity() {
      return 5 + getSettingValue("silk") * 4 + getSettingValue("fortune") == 0 ? 0 : (getSettingValue("fortune") + 1) * 3;
   }

   @Override
   public boolean execute(HitResult target, Trajectory trajectory, float finalPower, int num) {
      if (!(target instanceof BlockHitResult bhr)) return false;
      ItemStack casterStack = FocusUtils.heldCaster(getPackage().getCaster());
      if (casterStack.isEmpty()) return false;
      boolean silk = getSettingValue("silk") > 0;
      int fortune = getSettingValue("fortune");
      ItemStack picked = ((ICaster) casterStack.getItem()).getPickedBlock(casterStack);
      if (getPackage().getCaster() instanceof Player p && picked != null && !picked.isEmpty()) {
         Level world = getPackage().world;
         BlockWorks.addSwapper(world, bhr.getBlockPos(), world.getBlockState(bhr.getBlockPos()), picked, true, 0, p, true, false, 8038177, true, silk,
            fortune, BlockWorks.DEFAULT_PREDICATE, 0.25F + (silk ? 0.25F : 0.0F) + fortune * 0.1F);
      }
      return true;
   }

   @Override
   public NodeSetting[] createSettings() {
      int[] silk = {0, 1};
      String[] silkDesc = {"focus.common.no", "focus.common.yes"};
      int[] fortune = {0, 1, 2, 3, 4};
      String[] fortuneDesc = {"focus.common.no", "I", "II", "III", "IV"};
      return new NodeSetting[]{new NodeSetting("fortune", "focus.common.fortune", new NodeSetting.NodeSettingIntList(fortune, fortuneDesc)),
         new NodeSetting("silk", "focus.common.silk", new NodeSetting.NodeSettingIntList(silk, silkDesc))};
   }

   @Override
   public void renderParticleFX(Level level, double x, double y, double z, double vx, double vy, double vz) {
      thaumcraft.client.fx.FociParticles.exchange(x, y, z, vx, vy, vz);
   }

   @Override
   public void onCast(Entity caster) {
      caster.level().playSound(null, caster.blockPosition().above(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 0.2F,
         2.0F + (float) (caster.level().random.nextGaussian() * 0.05F));
   }
}
