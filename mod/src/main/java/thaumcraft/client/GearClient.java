package thaumcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.DyedItemColor;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import thaumcraft.Thaumcraft;
import thaumcraft.api.items.IRechargable;
import thaumcraft.api.items.RechargeHelper;
import thaumcraft.client.renderers.models.gear.GearArmorModel;
import thaumcraft.client.renderers.models.gear.GearModelLayers;
import thaumcraft.common.events.GearEvents;
import thaumcraft.common.items.armor.FortressArmorItem;
import thaumcraft.common.items.armor.RobeArmorItem;
import thaumcraft.common.items.baubles.VerdantCharmItem;
import thaumcraft.common.lib.GearClientBridge;
import thaumcraft.common.lib.enchantment.EnumInfusionEnchantment;
import thaumcraft.common.registers.ModGear;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.client.CuriosRendererRegistry;
import top.theillusivec4.curios.api.client.ICurioRenderer;

/** Client side of milestone 10: armor models and dye colors, item properties, curio rendering, tooltips, HUDs. */
@EventBusSubscriber(modid = Thaumcraft.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class GearClient {
   static ModelLayerLocation layer(String name, String kind) {
      return new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, name), kind);
   }

   /** model1 (f = 1.0: chest and boots) and model2 (f = 0.5: helmet and leggings) of each 1.12 armor model. */
   public static final ModelLayerLocation FORTRESS_1 = layer("fortress_armor", "outer");
   public static final ModelLayerLocation FORTRESS_2 = layer("fortress_armor", "inner");
   public static final ModelLayerLocation ROBE_1 = layer("robe_armor", "outer");
   public static final ModelLayerLocation ROBE_2 = layer("robe_armor", "inner");
   public static final ModelLayerLocation KNIGHT_1 = layer("knight_armor", "outer");
   public static final ModelLayerLocation KNIGHT_2 = layer("knight_armor", "inner");
   public static final ModelLayerLocation LEADER_1 = layer("leader_armor", "outer");
   public static final ModelLayerLocation LEADER_2 = layer("leader_armor", "inner");

   public static final ResourceLocation GOGGLES_WORN = ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "item/goggles_bauble");
   public static final ResourceLocation BAND_WORN = ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "item/curiosity_band_worn");

   private GearClient() {
   }

   @SubscribeEvent
   public static void layers(EntityRenderersEvent.RegisterLayerDefinitions e) {
      e.registerLayerDefinition(FORTRESS_1, () -> GearModelLayers.createFortress(1.0F));
      e.registerLayerDefinition(FORTRESS_2, () -> GearModelLayers.createFortress(0.5F));
      e.registerLayerDefinition(ROBE_1, () -> GearModelLayers.createRobe(1.0F));
      e.registerLayerDefinition(ROBE_2, () -> GearModelLayers.createRobe(0.5F));
      e.registerLayerDefinition(KNIGHT_1, () -> GearModelLayers.createKnight(1.0F));
      e.registerLayerDefinition(KNIGHT_2, () -> GearModelLayers.createKnight(0.5F));
      e.registerLayerDefinition(LEADER_1, () -> GearModelLayers.createLeader(1.0F));
      e.registerLayerDefinition(LEADER_2, () -> GearModelLayers.createLeader(0.5F));
   }

   enum Kind {
      NONE, FORTRESS, ROBE, KNIGHT, LEADER
   }

   /** getArmorModel + CustomArmorHelper.getCustomArmorModel, the render() animations, and the robes' default color. */
   static final class ArmorExtensions implements IClientItemExtensions {
      final Kind kind;
      final int defaultColor;
      GearArmorModel model1;
      GearArmorModel model2;

      ArmorExtensions(Kind kind, int defaultColor) {
         this.kind = kind;
         this.defaultColor = defaultColor;
      }

      private GearArmorModel bake(ModelLayerLocation loc, String[][] layout) {
         return new GearArmorModel(Minecraft.getInstance().getEntityModels().bakeLayer(loc), layout);
      }

      private void bake() {
         if (model1 != null) return;
         switch (kind) {
            case FORTRESS -> {
               model1 = bake(FORTRESS_1, GearModelLayers.FORTRESS_PARTS);
               model2 = bake(FORTRESS_2, GearModelLayers.FORTRESS_PARTS);
            }
            case ROBE -> {
               model1 = bake(ROBE_1, GearModelLayers.ROBE_PARTS);
               model2 = bake(ROBE_2, GearModelLayers.ROBE_PARTS);
            }
            case KNIGHT -> {
               model1 = bake(KNIGHT_1, GearModelLayers.KNIGHT_PARTS);
               model2 = bake(KNIGHT_2, GearModelLayers.KNIGHT_PARTS);
            }
            case LEADER -> {
               model1 = bake(LEADER_1, GearModelLayers.LEADER_PARTS);
               model2 = bake(LEADER_2, GearModelLayers.LEADER_PARTS);
            }
            default -> {
            }
         }
      }

      @Override
      public HumanoidModel<?> getHumanoidArmorModel(LivingEntity entity, ItemStack stack, EquipmentSlot slot, HumanoidModel<?> original) {
         if (kind == Kind.NONE) return original;
         bake();
         GearArmorModel m = slot == EquipmentSlot.CHEST || slot == EquipmentSlot.FEET ? model1 : model2;
         if (kind == Kind.FORTRESS) {
            ItemStack helm = entity.getItemBySlot(EquipmentSlot.HEAD);
            boolean fortressHelm = helm.getItem() instanceof FortressArmorItem;
            m.setupFortress(FortressArmorItem.countSet(entity), fortressHelm ? FortressArmorItem.getMask(helm) : -1,
               fortressHelm && FortressArmorItem.hasGoggles(helm));
         }
         return m;
      }

      @Override
      public void setupModelAnimations(LivingEntity entity, ItemStack stack, EquipmentSlot slot, Model model, float limbSwing, float limbSwingAmount,
                                       float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
         if (!(model instanceof GearArmorModel g)) return;
         switch (kind) {
            case ROBE -> g.animateRobe(limbSwing, limbSwingAmount);
            case KNIGHT -> g.animateKnight(limbSwing, limbSwingAmount);
            case LEADER -> g.animateLeader(limbSwing, limbSwingAmount);
            default -> {
            }
         }
      }

      @Override
      public int getDefaultDyeColor(ItemStack stack) {
         return defaultColor >= 0 ? FastColor.ARGB32.opaque(DyedItemColor.getOrDefault(stack, defaultColor)) : 0xFFFFFFFF;
      }
   }

   @SubscribeEvent
   public static void clientExtensions(RegisterClientExtensionsEvent e) {
      e.registerItem(new ArmorExtensions(Kind.FORTRESS, -1), ModGear.FORTRESS_HELM.get(), ModGear.FORTRESS_CHEST.get(), ModGear.FORTRESS_LEGS.get());
      e.registerItem(new ArmorExtensions(Kind.ROBE, RobeArmorItem.DEFAULT_COLOR), ModGear.VOID_ROBE_HELM.get(), ModGear.VOID_ROBE_CHEST.get(),
         ModGear.VOID_ROBE_LEGS.get());
      e.registerItem(new ArmorExtensions(Kind.ROBE, -1), ModGear.CRIMSON_ROBE_HELM.get(), ModGear.CRIMSON_ROBE_CHEST.get(), ModGear.CRIMSON_ROBE_LEGS.get());
      e.registerItem(new ArmorExtensions(Kind.KNIGHT, -1), ModGear.CRIMSON_PLATE_HELM.get(), ModGear.CRIMSON_PLATE_CHEST.get(),
         ModGear.CRIMSON_PLATE_LEGS.get());
      e.registerItem(new ArmorExtensions(Kind.LEADER, -1), ModGear.CRIMSON_PRAETOR_HELM.get(), ModGear.CRIMSON_PRAETOR_CHEST.get(),
         ModGear.CRIMSON_PRAETOR_LEGS.get());
      e.registerItem(new ArmorExtensions(Kind.NONE, RobeArmorItem.DEFAULT_COLOR), ModGear.CLOTH_CHEST.get(), ModGear.CLOTH_LEGS.get(),
         ModGear.CLOTH_BOOTS.get());
   }

   /** itemArmorColourHandler: the first layer takes the dye. */
   @SubscribeEvent
   public static void itemColors(RegisterColorHandlersEvent.Item e) {
      e.register((stack, tint) -> tint > 0 ? -1 : DyedItemColor.getOrDefault(stack, FastColor.ARGB32.opaque(RobeArmorItem.DEFAULT_COLOR)),
         ModGear.VOID_ROBE_HELM.get(), ModGear.VOID_ROBE_CHEST.get(), ModGear.VOID_ROBE_LEGS.get(), ModGear.CLOTH_CHEST.get(), ModGear.CLOTH_LEGS.get(),
         ModGear.CLOTH_BOOTS.get());
   }

   @SubscribeEvent
   public static void additionalModels(ModelEvent.RegisterAdditional e) {
      e.register(ModelResourceLocation.standalone(GOGGLES_WORN));
      e.register(ModelResourceLocation.standalone(BAND_WORN));
   }

   @SubscribeEvent
   public static void guiLayers(RegisterGuiLayersEvent e) {
      e.registerAboveAll(SanityHud.ID, SanityHud::render);
   }

   @SubscribeEvent
   public static void clientSetup(FMLClientSetupEvent e) {
      GearClientBridge.isLocalPlayer = p -> p == Minecraft.getInstance().player;
      GearClientBridge.jumpPressed = () -> Minecraft.getInstance().options.keyJump.consumeClick();
      e.enqueueWork(() -> {
         ItemProperties.register(ModGear.VERDANT_CHARM.get(), ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "type"),
            (stack, level, entity, seed) -> VerdantCharmItem.getType(stack));
      });
      CuriosRendererRegistry.register(ModGear.GOGGLES_ITEM.get(), () -> new FaceCurioRenderer(GOGGLES_WORN));
      CuriosRendererRegistry.register(ModGear.CURIOSITY_BAND.get(), () -> new FaceCurioRenderer(BAND_WORN));
   }

   /** IRenderBauble HEAD of the goggles and the curiosity band: their worn texture in front of the face. */
   static final class FaceCurioRenderer implements ICurioRenderer {
      final ModelResourceLocation model;

      FaceCurioRenderer(ResourceLocation model) {
         this.model = ModelResourceLocation.standalone(model);
      }

      @Override
      public <T extends LivingEntity, M extends EntityModel<T>> void render(ItemStack stack, SlotContext ctx, PoseStack pose, RenderLayerParent<T, M> parent,
                                                                           MultiBufferSource buffer, int light, float limbSwing, float limbSwingAmount,
                                                                           float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
         if (!(parent.getModel() instanceof HumanoidModel<?> hm)) return;
         boolean armor = !ctx.entity().getItemBySlot(EquipmentSlot.HEAD).isEmpty();
         pose.pushPose();
         hm.head.translateAndRotate(pose);
         pose.translate(0.0F, -0.25F, armor ? -0.37F : -0.3F);
         pose.scale(0.55F, -0.55F, -0.55F);
         var baked = Minecraft.getInstance().getModelManager().getModel(model);
         Minecraft.getInstance().getItemRenderer().render(stack, ItemDisplayContext.NONE, false, pose, buffer, light, OverlayTexture.NO_OVERLAY, baked);
         pose.popPose();
      }
   }

   /** PlayerEvents.tooltipEvent: runic shielding, warp, vis discount, vis charge and the infusion enchantments. */
   @EventBusSubscriber(modid = Thaumcraft.MODID, value = Dist.CLIENT)
   public static final class Game {
      private Game() {
      }

      @SubscribeEvent
      public static void tooltip(ItemTooltipEvent event) {
         ItemStack stack = event.getItemStack();
         var tip = event.getToolTip();
         int charge = GearEvents.getRunicCharge(stack);
         if (charge > 0) tip.add(Component.translatable("item.runic.charge").append(" +" + charge).withStyle(ChatFormatting.GOLD));
         int warp = GearEvents.getFinalWarp(stack, event.getEntity());
         if (warp > 0) tip.add(Component.translatable("item.warping").append(" " + warp).withStyle(ChatFormatting.DARK_PURPLE));
         int al = GearEvents.getFinalDiscount(stack, event.getEntity());
         if (al > 0) tip.add(Component.translatable("tc.visdiscount").append(": " + al + "%").withStyle(ChatFormatting.DARK_PURPLE));
         if (stack.getItem() instanceof IRechargable) {
            int c = RechargeHelper.getCharge(stack);
            if (c >= 0) tip.add(Component.translatable("tc.charge").append(" " + c).withStyle(ChatFormatting.YELLOW));
         }
         for (EnumInfusionEnchantment ench : EnumInfusionEnchantment.getInfusionEnchantments(stack)) {
            int l = EnumInfusionEnchantment.getInfusionEnchantmentLevel(stack, ench);
            var s = Component.translatable("enchantment.infusion." + ench.id());
            if (ench.maxLevel > 1) s = s.append(" ").append(Component.translatable("enchantment.level." + l));
            tip.add(Math.min(1, tip.size()), s.withStyle(ChatFormatting.GOLD));
         }
      }
   }
}
