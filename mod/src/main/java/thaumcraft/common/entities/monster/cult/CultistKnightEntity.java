package thaumcraft.common.entities.monster.cult;

import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.OpenDoorGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.AbstractIllager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import thaumcraft.common.registers.ModGear;

/** EntityCultistKnight: plate armor and a sword (rarely a thaumium or void one). */
public class CultistKnightEntity extends CultistEntity {
   public CultistKnightEntity(EntityType<? extends CultistKnightEntity> type, Level level) {
      super(type, level);
   }

   public static AttributeSupplier.Builder createAttributes() {
      return CultistEntity.createAttributes().add(Attributes.MAX_HEALTH, 30.0);
   }

   @Override
   protected void registerGoals() {
      goalSelector.addGoal(0, new FloatGoal(this));
      goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.0, false));
      goalSelector.addGoal(5, new OpenDoorGoal(this, true));
      goalSelector.addGoal(6, new MoveTowardsRestrictionGoal(this, 0.8));
      goalSelector.addGoal(7, new RandomStrollGoal(this, 0.8));
      goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
      goalSelector.addGoal(8, new RandomLookAroundGoal(this));
      targetSelector.addGoal(1, new CultGoals.HurtByTarget(this, true));
      targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
      targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, thaumcraft.common.entities.monster.eldritch.EldritchGuardianEntity.class, true));
      targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(this, AbstractIllager.class, true));
   }

   @Override
   protected void setLoot(RandomSource rand, DifficultyInstance diff) {
      setItemSlot(EquipmentSlot.HEAD, new ItemStack(ModGear.CRIMSON_PLATE_HELM.get()));
      setItemSlot(EquipmentSlot.CHEST, new ItemStack(ModGear.CRIMSON_PLATE_CHEST.get()));
      setItemSlot(EquipmentSlot.LEGS, new ItemStack(ModGear.CRIMSON_PLATE_LEGS.get()));
      setItemSlot(EquipmentSlot.FEET, new ItemStack(ModGear.CRIMSON_BOOTS.get()));
      if (rand.nextFloat() < (level().getDifficulty() == Difficulty.HARD ? 0.05F : 0.01F)) {
         if (rand.nextInt(5) == 0) {
            setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ModGear.VOID_SWORD.get()));
            setItemSlot(EquipmentSlot.HEAD, new ItemStack(ModGear.CRIMSON_ROBE_HELM.get()));
         } else {
            setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ModGear.THAUMIUM_SWORD.get()));
            if (rand.nextBoolean()) setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
         }
      } else {
         setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
      }
   }

   @Override
   protected void setEnchantmentBasedOnDifficulty(RandomSource rand, DifficultyInstance diff) {
      float f = diff.getSpecialMultiplier();
      if (!getMainHandItem().isEmpty() && rand.nextFloat() < 0.25F * f) {
         enchantHeld(this, rand, (int) (5.0F + f * rand.nextInt(18)));
      }
   }
}
