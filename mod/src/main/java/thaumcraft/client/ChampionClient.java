package thaumcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.phys.AABB;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import thaumcraft.Thaumcraft;
import thaumcraft.client.fx.FXDispatcher;
import thaumcraft.client.fx.FocusFX;
import thaumcraft.common.entities.monster.mods.ChampionModifier;
import thaumcraft.common.lib.MobFXBridge;

/**
 * Client side of the champions: each modifier's particles (IChampionModifierEffect.showFX, run from
 * RenderEventHandler.livingTick) and the taint fibres drawn over tainted champions (LayerTainted, added to every
 * living entity renderer). The 1.12 colour tints of some modifiers (glColor before rendering) are not reproduced.
 */
@EventBusSubscriber(modid = Thaumcraft.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class ChampionClient {
   private static final ResourceLocation TAINT_TEXTURE = ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "textures/models/taint_fibres.png");

   private ChampionClient() {
   }

   @SubscribeEvent
   @SuppressWarnings({"unchecked", "rawtypes"})
   public static void addLayers(EntityRenderersEvent.AddLayers event) {
      for (var type : event.getEntityTypes()) {
         EntityRenderer<?> r = event.getRenderer(type);
         if (r instanceof LivingEntityRenderer lr) lr.addLayer(new TaintedLayer(lr));
      }
   }

   @EventBusSubscriber(modid = Thaumcraft.MODID, value = Dist.CLIENT)
   public static final class GameEvents {
      private GameEvents() {
      }

      @SubscribeEvent
      public static void livingTick(EntityTickEvent.Post event) {
         if (!(event.getEntity() instanceof PathfinderMob mob) || !mob.level().isClientSide || !mob.isAlive()) return;
         ChampionModifier mod = ChampionModifier.get(mob);
         if (mod != null) showFX(mod.id, mob);
      }
   }

   /** The showFX of each ChampionMod*. */
   static void showFX(int id, LivingEntity boss) {
      RandomSource r = boss.getRandom();
      AABB bb = boss.getBoundingBox();
      float w = r.nextFloat() * boss.getBbWidth();
      float d = r.nextFloat() * boss.getBbWidth();
      float h = r.nextFloat() * boss.getBbHeight();
      double x = bb.minX + w, y = bb.minY + h, z = bb.minZ + d;
      switch (id) {
         case 0 -> { // bold
            if (!r.nextBoolean()) FocusFX.spark(x, bb.minY + r.nextFloat() * boss.getBbHeight() / 3.0F, z, 0.2F, 0.3F - r.nextFloat() * 0.1F, 0.0F,
               0.8F + r.nextFloat() * 0.2F, 1.0F);
         }
         case 1 -> { // spined
            if (!r.nextBoolean()) FXDispatcher.drawGenericParticles(x, y, z, 0, 0, 0, 0.5F + r.nextFloat() * 0.2F, 0.1F + r.nextFloat() * 0.2F,
               0.1F + r.nextFloat() * 0.2F, 0.7F, false, 704 + r.nextInt(4) * 3, 3, 1, 3, 1.2F + r.nextFloat() * 0.3F, 0.0F, 0);
         }
         case 2 -> { // armored
            if (r.nextInt(4) == 0) FXDispatcher.drawGenericParticles(x, y, z, 0, 0, 0, 0.9F, 0.9F, 0.9F + r.nextFloat() * 0.1F, 0.7F, false, 448, 9, 1,
               5 + r.nextInt(4), 0.6F + r.nextFloat() * 0.2F, 0.0F, 0);
         }
         case 3 -> { // mighty
            if (r.nextFloat() <= 0.3F) FXDispatcher.drawGenericParticles(x, y, z, 0, 0, 0, 0.8F + r.nextFloat() * 0.2F, 0.8F + r.nextFloat() * 0.2F,
               0.8F + r.nextFloat() * 0.2F, 0.7F, false, 704 + r.nextInt(4) * 3, 3, 1, 4 + r.nextInt(3), 1.0F + r.nextFloat() * 0.3F, 0.0F, 0);
         }
         case 4 -> { // grim
            if (!r.nextBoolean()) FXDispatcher.drawGenericParticles(x, y, z, 0, -0.02, 0, r.nextFloat() * 0.2F, r.nextFloat() * 0.2F, r.nextFloat() * 0.2F,
               0.8F, false, 640, 10, 1, 8 + r.nextInt(4), 0.6F + r.nextFloat() * 0.4F, 0.0F, 0);
         }
         case 5 -> { // warded
            if (!r.nextBoolean()) FXDispatcher.drawGenericParticles(x, y, z, 0, 0, 0, 0.5F + r.nextFloat() * 0.1F, 0.5F + r.nextFloat() * 0.1F,
               0.5F + r.nextFloat() * 0.1F, 0.6F, true, 69, 4, 1, 4 + r.nextInt(4), 0.8F + r.nextFloat() * 0.3F, 0.0F, 0);
         }
         case 6 -> { // warp
            if (!r.nextBoolean()) FXDispatcher.drawGenericParticles(x, y, z, 0, 0, 0, 0.8F + r.nextFloat() * 0.2F, 0.0F, 0.9F + r.nextFloat() * 0.1F, 0.7F,
               true, 264, 8, 1, 10 + r.nextInt(4), 0.6F + r.nextFloat() * 0.4F, 0.0F, 0);
         }
         case 7 -> { // undying
            if (!r.nextBoolean()) FXDispatcher.drawGenericParticles(x, y, z, 0, 0.03, 0, 0.1F + r.nextFloat() * 0.1F, 0.8F + r.nextFloat() * 0.2F,
               0.1F + r.nextFloat() * 0.1F, 0.9F, true, 69, 4, 1, 4 + r.nextInt(4), 0.5F + r.nextFloat() * 0.2F, 0.0F, 0);
         }
         case 8 -> // fiery
            FXDispatcher.drawGenericParticles(x, y, z, 0, 0.03, 0, 0.9F + r.nextFloat() * 0.1F, 1.0F, 1.0F, 0.7F, false, 640, 10, 1, 8 + r.nextInt(4),
               0.7F + r.nextFloat() * 0.2F, 0.0F, 0);
         case 9 -> { // sickly
            if (!r.nextBoolean()) FXDispatcher.drawGenericParticles(x, y, z, 0, -0.02, 0, 0.2F, 0.6F + r.nextFloat() * 0.1F, 0.2F + r.nextFloat() * 0.1F,
               0.5F, false, 1, 4, 2, 5 + r.nextInt(4), 0.9F + r.nextFloat() * 0.3F, 0.0F, 0);
         }
         case 10 -> { // venomous
            if (!r.nextBoolean()) FXDispatcher.drawGenericParticles(x, y, z, 0, 0.02, 0, 0.2F, 0.6F + r.nextFloat() * 0.1F, 0.2F + r.nextFloat() * 0.1F,
               0.7F, false, 579, 4, 1, 8 + r.nextInt(4), 0.5F + r.nextFloat() * 0.2F, 0.5F, 0);
         }
         case 11 -> { // vampiric
            if (r.nextFloat() <= 0.2F) FXDispatcher.drawGenericParticles(x, y, z, 0, 0, 0, 0.9F + r.nextFloat() * 0.1F, 0.0F, 0.0F, 0.9F, false, 579, 4, 1,
               4 + r.nextInt(4), 0.5F + r.nextFloat() * 0.2F, 0.0F, 0);
         }
         case 12 -> { // infested
            if (r.nextBoolean()) MobFXBridge.INSTANCE.slimeJump(boss, 0);
         }
         case 13 -> // tainted
            FXDispatcher.drawGenericParticles(x, y, z, 0, -0.01, 0, 0.1F + r.nextFloat() * 0.2F, 0.0F, 0.1F + r.nextFloat() * 0.1F, 0.25F, false, 1, 5, 1,
               6 + r.nextInt(6), 2.0F + r.nextFloat(), 0.5F, 1);
         default -> {
         }
      }
   }

   /** LayerTainted: the creature's model again, covered in drifting taint fibres. */
   static final class TaintedLayer<T extends LivingEntity, M extends EntityModel<T>> extends RenderLayer<T, M> {
      TaintedLayer(RenderLayerParent<T, M> parent) {
         super(parent);
      }

      @Override
      public void render(PoseStack pose, MultiBufferSource buffers, int light, T e, float limbSwing, float limbSwingAmount, float pt, float age,
                         float netHeadYaw, float headPitch) {
         if (e.isInvisible() || ChampionModifier.getChampionValue(e) != ChampionModifier.TAINTED) return;
         float f = e.getId();
         float u = Mth.cos(f * 2.5E-4F) % 1.0F;
         float v = f * 0.001F % 1.0F;
         getParentModel().renderToBuffer(pose, buffers.getBuffer(RenderType.energySwirl(TAINT_TEXTURE, u, v)), light, OverlayTexture.NO_OVERLAY,
            FastColor.ARGB32.colorFromFloat(0.66F, 1.0F, 1.0F, 1.0F));
      }
   }
}
