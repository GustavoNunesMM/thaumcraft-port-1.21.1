package thaumcraft.common.entities.monster.mods;

import java.util.UUID;
import net.minecraft.client.renderer.entity.RenderLivingBase;
import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.EntityAIHurtByTarget;
import net.minecraft.entity.ai.EntityAILookIdle;
import net.minecraft.entity.ai.EntityAIMoveThroughVillage;
import net.minecraft.entity.ai.EntityAIMoveTowardsRestriction;
import net.minecraft.entity.ai.EntityAINearestAttackableTarget;
import net.minecraft.entity.ai.EntityAISwimming;
import net.minecraft.entity.ai.EntityAIWander;
import net.minecraft.entity.ai.EntityAIWatchClosest;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.IAttribute;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.ai.attributes.RangedAttribute;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.monster.EntityPigZombie;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.DamageSource;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import thaumcraft.client.fx.FXDispatcher;
import thaumcraft.client.renderers.entity.mob.LayerTainted;
import thaumcraft.common.entities.ai.combat.EntityCritterAIAttackMelee;

public class ChampionModTainted implements IChampionModifierEffect {
   public static final IAttribute TAINTED_MOD = new RangedAttribute((IAttribute)null, "tc.mobmodtaint", 0.0, 0.0, 1.0).setDescription("Tainted modifier");

   @Override
   public float performEffect(EntityLivingBase boss, EntityLivingBase target, DamageSource source, float amount) {
      resetAI((EntityCreature)boss);
      return amount;
   }

   public static void resetAI(EntityCreature critter) {
      IAttributeInstance modai = critter.getEntityAttribute(TAINTED_MOD);
      if (!(critter instanceof EntityMob) && modai.getAttributeValue() == 0.0) {
         try {
            critter.tasks.taskEntries.clear();
            critter.targetTasks.taskEntries.clear();
            critter.tasks.addTask(0, new EntityAISwimming(critter));
            critter.tasks.addTask(2, new EntityCritterAIAttackMelee(critter, 1.2, false));
            critter.tasks.addTask(5, new EntityAIMoveTowardsRestriction(critter, 1.0));
            critter.tasks.addTask(7, new EntityAIWander(critter, 1.0));
            critter.tasks.addTask(8, new EntityAIWatchClosest(critter, EntityPlayer.class, 8.0F));
            critter.tasks.addTask(8, new EntityAILookIdle(critter));
            critter.tasks.addTask(6, new EntityAIMoveThroughVillage(critter, 1.0, false));
            critter.targetTasks.addTask(1, new EntityAIHurtByTarget(critter, true, new Class[]{EntityPigZombie.class}));
            critter.targetTasks.addTask(2, new EntityAINearestAttackableTarget(critter, EntityPlayer.class, true));
            modai.removeModifier(new AttributeModifier(UUID.fromString("2cb22137-a9d8-4417-ae06-de0e70f11b4c"), "istainted", 0.0, 0));
            modai.applyModifier(new AttributeModifier(UUID.fromString("2cb22137-a9d8-4417-ae06-de0e70f11b4c"), "istainted", 1.0, 0));
         } catch (Exception var3) {
         }
      }

      IAttributeInstance iattributeinstance2 = critter.getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE);
      if (iattributeinstance2 == null) {
         critter.getAttributeMap().registerAttribute(SharedMonsterAttributes.ATTACK_DAMAGE);
         critter.getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).setBaseValue(Math.max(2.0F, (critter.height + critter.width) * 2.0F));
      }
   }

   @SideOnly(Side.CLIENT)
   @Override
   public void preRender(EntityLivingBase boss, RenderLivingBase renderLivingBase) {
      if (!LayerTainted.taintLayers.contains(boss.getEntityId())) {
         renderLivingBase.addLayer(new LayerTainted(boss.getEntityId(), renderLivingBase, renderLivingBase.getMainModel()));
      }
   }

   @SideOnly(Side.CLIENT)
   @Override
   public void showFX(EntityLivingBase boss) {
      float w = boss.world.rand.nextFloat() * boss.width;
      float d = boss.world.rand.nextFloat() * boss.width;
      float h = boss.world.rand.nextFloat() * boss.height;
      FXDispatcher.INSTANCE
         .drawGenericParticles(
            boss.getEntityBoundingBox().minX + w,
            boss.getEntityBoundingBox().minY + h,
            boss.getEntityBoundingBox().minZ + d,
            0.0,
            -0.01,
            0.0,
            0.1F + boss.world.rand.nextFloat() * 0.2F,
            0.0F,
            0.1F + boss.world.rand.nextFloat() * 0.1F,
            0.25F,
            false,
            1,
            5,
            1,
            6 + boss.world.rand.nextInt(6),
            0,
            2.0F + boss.world.rand.nextFloat(),
            0.5F,
            1
         );
   }
}
