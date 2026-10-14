package thaumcraft.client.gui;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.model.data.ModelData;
import thaumcraft.Thaumcraft;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.crafting.InfusionRecipe;
import thaumcraft.common.crafting.InfusionEnchantmentRecipe;
import thaumcraft.common.crafting.InfusionRunicAugmentRecipe;
import thaumcraft.common.registers.ModBlocks;
import thaumcraft.common.registers.ModDevices;
import thaumcraft.common.registers.ModGear;
import thaumcraft.common.registers.ModGolems;
import thaumcraft.common.registers.ModSounds;
import thaumcraft.common.research.ResearchManager;

/**
 * The Thaumonomicon pages GuiResearchPage drew for infusion recipes (drawInfusionPage), the ConfigRecipes "fake"
 * infusion recipes (IE*FAKE: an infusion enchantment shown on a sample tool; RunicArmorFake0-2: runic augments of a
 * ring), and the multiblock blueprints (ThaumcraftApi.BluePrint, drawCompoundCraftingPage/renderBluePrint) with the
 * layer-by-layer toggle.
 */
@EventBusSubscriber(modid = Thaumcraft.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class BookRecipeViews {
   private static final ResourceLocation TEX1 = ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "textures/gui/gui_researchbook.png");
   private static final ResourceLocation TEX2 = ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "textures/gui/gui_researchbook_overlay.png");

   private BookRecipeViews() {
   }

   private static ResourceLocation id(String path) {
      return ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, path);
   }

   @SubscribeEvent
   public static void setup(FMLClientSetupEvent e) {
      // Infusion recipes from the recipe manager (the enchantment/runic ones have no fixed central item: shown via fakes).
      RecipeCatalog.addConverter(r -> r instanceof InfusionRecipe ir && !ir.central.isEmpty() ? new InfusionView(() -> ir, null) : null);
      // ConfigRecipes.IE*FAKE
      fakeEnchant("iearcingfake", "iearcing", () -> new ItemStack(Items.WOODEN_SWORD));
      fakeEnchant("ieburrowingfake", "ieburrowing", () -> new ItemStack(Items.WOODEN_PICKAXE));
      fakeEnchant("iecollectorfake", "iecollector", () -> new ItemStack(Items.STONE_AXE));
      fakeEnchant("iedestructivefake", "iedestructive", () -> new ItemStack(Items.STONE_PICKAXE));
      fakeEnchant("ieessencefake", "ieessence", () -> new ItemStack(Items.STONE_SWORD));
      fakeEnchant("ielamplightfake", "ielamplight", () -> new ItemStack(Items.GOLDEN_PICKAXE));
      fakeEnchant("ierefiningfake", "ierefining", () -> new ItemStack(Items.IRON_PICKAXE));
      fakeEnchant("iesoundingfake", "iesounding", () -> new ItemStack(Items.GOLDEN_PICKAXE));
      // ConfigRecipes.RunicArmorFake0-2: a mundane ring with 0, 1 and 2 runic charges.
      for (int a = 0; a < 3; a++) {
         final int charge = a;
         RecipeCatalog.addFake(id("runicarmorfake" + a), new InfusionView(() -> real(id("runicarmor")), () -> {
            ItemStack ring = new ItemStack(ModGear.RING_MUNDANE.get());
            if (charge > 0) ring.set(ModGear.RUNIC.get(), charge);
            return ring;
         }));
      }
      blueprints();
   }

   private static void fakeEnchant(String fake, String real, Supplier<ItemStack> tool) {
      RecipeCatalog.addFake(id(fake), new InfusionView(() -> real(id(real)), tool));
   }

   @Nullable
   private static InfusionRecipe real(ResourceLocation rid) {
      var level = Minecraft.getInstance().level;
      if (level == null) return null;
      return level.getRecipeManager().byKey(rid).map(h -> h.value() instanceof InfusionRecipe ir ? ir : null).orElse(null);
   }

   // ------------------------------------------------------------------ infusion

   /** drawInfusionPage. input: the central item shown (null = cycle the recipe's own central ingredient). */
   public record InfusionView(Supplier<InfusionRecipe> recipe, @Nullable Supplier<ItemStack> input) implements RecipeCatalog.RecipeView {
      private ItemStack centralStack() {
         InfusionRecipe r = recipe.get();
         if (input != null) return input.get();
         return r == null ? ItemStack.EMPTY : RecipeCatalog.cycle(r.central, 0);
      }

      private NonNullList<Ingredient> components(InfusionRecipe r, ItemStack in) {
         return r instanceof InfusionRunicAugmentRecipe ra ? ra.getComponents(in) : r.getComponents();
      }

      private static List<ItemStack> firsts(NonNullList<Ingredient> c) {
         List<ItemStack> out = new ArrayList<>();
         for (Ingredient i : c) {
            ItemStack[] items = i.getItems();
            out.add(items.length > 0 ? items[0] : ItemStack.EMPTY);
         }
         return out;
      }

      @Override
      public ItemStack output() {
         InfusionRecipe r = recipe.get();
         if (r == null) return ItemStack.EMPTY;
         ItemStack in = centralStack();
         if (r instanceof InfusionEnchantmentRecipe || r instanceof InfusionRunicAugmentRecipe) {
            return r.getRecipeOutput(Minecraft.getInstance().player, in.copy(), firsts(components(r, in)));
         }
         var level = Minecraft.getInstance().level;
         return level == null ? ItemStack.EMPTY : r.getResultItem(level.registryAccess());
      }

      @Override
      public boolean known(Player player) {
         InfusionRecipe r = recipe.get();
         return r != null && ResearchManager.knowsResearchStrict(player, r.getResearch());
      }

      @Override
      public void render(ResearchPageScreen s, GuiGraphics g, int x, int y, int mx, int my) {
         InfusionRecipe r = recipe.get();
         if (r == null) return;
         Player player = Minecraft.getInstance().player;
         ItemStack in = centralStack();
         NonNullList<Ingredient> components = components(r, in);
         List<ItemStack> comps = firsts(components);
         AspectList aspects = r.getAspects(player, in, comps);
         s.drawCentered(g, Component.translatable("recipe.type.infusion"), x, y - 104);
         GuiDraw.resetColor();
         g.pose().pushPose();
         g.pose().translate(x, y + 20, 0);
         g.pose().scale(2.0F, 2.0F, 1.0F);
         GuiDraw.rect(g, TEX2, -28, -56, 0, 3, 56, 17);
         g.pose().translate(0, 19, 0);
         GuiDraw.rect(g, TEX2, -28, -55, 200, 77, 60, 44);
         g.pose().popPose();

         s.drawStackAt(g, output(), x - 8, y - 85, mx, my, false);
         s.drawStackAt(g, in, x - 8, y - 16, mx, my, true);
         int le = components.size();
         if (le > 0) {
            float pieSlice = 360 / le;
            float currentRot = -90.0F;
            for (int a = 0; a < le; a++) {
               int xx = (int) (Mth.cos(currentRot / 180.0F * (float) Math.PI) * 40.0F) - 8;
               int yy = (int) (Mth.sin(currentRot / 180.0F * (float) Math.PI) * 40.0F) - 8;
               currentRot += pieSlice;
               s.drawStackAt(g, RecipeCatalog.cycle(components.get(a), a).copyWithCount(1), x + xx, y - 8 + yy, mx, my, true);
            }
         }
         int inst = Math.min(5, r.getInstability(player, in, comps) / 2);
         s.drawCentered(g, Component.translatable("tc.inst").append(" ").append(Component.translatable("tc.inst." + inst)), x, y + 94);
         int size = aspects.size();
         int rows = (size - 1) / 5;
         int shift = (5 - size % 5) * 10;
         int sx = x - 48;
         int sy = y + 50 - 10 * rows;
         int total = 0;
         for (Aspect tag : aspects.getAspectsSortedByName()) {
            int m = total / 5 >= rows && (rows > 1 || size < 5) ? 1 : 0;
            s.drawAspectTag(g, tag, aspects.getAmount(tag), sx + total % 5 * 20 + shift * m, sy + total / 5 * 20, mx, my);
            total++;
         }
      }
   }

   // ------------------------------------------------------------------ blueprints

   /** GuiResearchPage.cycleMultiblockLines: whether the layers are peeled off one by one. */
   static boolean cycleLines;
   private static int sliceLine;
   private static long lastSlice;

   /**
    * ThaumcraftApi.BluePrint as the book shows it: the structure's blocks (the "source" parts, layers from the top),
    * the ingredients below and the research it needs.
    */
   public record BlueprintView(String research, Supplier<ItemStack> display, Supplier<BlockState[][][]> layers, Supplier<List<ItemStack>> ingredients)
      implements RecipeCatalog.RecipeView {
      private static final Map<BlockState, BlockEntity> BES = new HashMap<>();

      @Override
      public ItemStack output() {
         return display.get();
      }

      @Override
      public boolean known(Player player) {
         return ResearchManager.knowsResearchStrict(player, research);
      }

      @Override
      public boolean mouseClicked(ResearchPageScreen s, int x, int y, int mx, int my) {
         if (mx >= x + 80 && my >= y + 100 && mx < x + 90 && my < y + 110) {
            Minecraft.getInstance().player.playSound(ModSounds.CLACK.get(), 0.66F, 1.0F);
            cycleLines = !cycleLines;
            sliceLine = 0;
            return true;
         }
         return false;
      }

      @Override
      public void render(ResearchPageScreen s, GuiGraphics g, int x, int y, int mx, int my) {
         BlockState[][][] bp = layers.get();
         int ySize = bp.length, xSize = bp[0].length, zSize = bp[0][0].length;
         s.drawCentered(g, Component.translatable("recipe.type.construct"), x, y - 104);
         long now = System.currentTimeMillis();
         if (now > lastSlice) {
            lastSlice = now + 1000L;
            if (cycleLines) sliceLine = sliceLine >= ySize ? 0 : sliceLine + 1;
         }
         int sz = Math.max(Math.max(xSize, zSize), ySize) * 2;
         float scale = 38 - sz;
         renderStructure(g, bp, x, y, scale, cycleLines ? sliceLine : 0);
         List<ItemStack> ing = ingredients.get();
         for (int a = 0; a < ing.size(); a++) s.drawStackAt(g, ing.get(a), x - 85 + a * 17, y + 90, mx, my, true);
         boolean over = mx >= x + 80 && my >= y + 100 && mx < x + 88 && my < y + 108;
         GuiDraw.color(1, 1, 1, over ? 1.0F : 0.75F);
         g.pose().pushPose();
         g.pose().translate(0, 0, 300);
         GuiDraw.rect(g, TEX1, x + 80, y + 100, cycleLines ? 168 : 160, 224, 8, 8);
         g.pose().popPose();
         GuiDraw.resetColor();
      }

      /** renderBluePrint: an isometric view (25 deg pitch, -45 deg yaw) of the structure, minus the top `slice` layers. */
      private static void renderStructure(GuiGraphics g, BlockState[][][] bp, int x, int y, float scale, int slice) {
         Minecraft mc = Minecraft.getInstance();
         int ySize = bp.length, xSize = bp[0].length, zSize = bp[0][0].length;
         float transX = x - xSize / 2.0F;
         float transY = y - (float) Math.sqrt(ySize * ySize + xSize * xSize + zSize * zSize) / 2.0F;
         PoseStack ps = g.pose();
         ps.pushPose();
         ps.translate(transX, transY, 200.0F);
         ps.scale(scale, -scale, scale);
         ps.mulPose(Axis.XP.rotationDegrees(25.0F));
         ps.mulPose(Axis.YP.rotationDegrees(-45.0F));
         ps.translate(zSize / -2.0F, ySize / -2.0F, xSize / -2.0F);
         Lighting.setupFor3DItems();
         MultiBufferSource.BufferSource buf = g.bufferSource();
         for (int layer = 0; layer < ySize; layer++) {
            int h = ySize - layer - 1; // the blueprint lists layers from the top
            if (h >= ySize - slice) continue;
            for (int l = 0; l < xSize; l++) {
               for (int w = 0; w < zSize; w++) {
                  BlockState state = bp[layer][l][w];
                  if (state == null || state.isAir()) continue;
                  ps.pushPose();
                  ps.translate(l, h, w);
                  renderBlock(mc, ps, buf, state);
                  ps.popPose();
               }
            }
         }
         g.flush();
         ps.popPose();
         Lighting.setupFor3DItems();
      }

      private static void renderBlock(Minecraft mc, PoseStack ps, MultiBufferSource buf, BlockState state) {
         int light = 0xF000F0;
         if (!state.getFluidState().isEmpty() && state.getRenderShape() != RenderShape.MODEL) {
            fluidCube(mc, ps, buf, state);
            return;
         }
         if (state.getRenderShape() == RenderShape.MODEL) {
            mc.getBlockRenderer().renderSingleBlock(state, ps, buf, light, OverlayTexture.NO_OVERLAY, ModelData.EMPTY, null);
         }
         if (state.getBlock() instanceof EntityBlock eb) {
            try {
               BlockEntity be = BES.computeIfAbsent(state, st -> eb.newBlockEntity(BlockPos.ZERO, st));
               if (be != null) {
                  if (be.getLevel() == null && mc.level != null) be.setLevel(mc.level);
                  var renderer = mc.getBlockEntityRenderDispatcher().getRenderer(be);
                  if (renderer != null) renderer.render(be, mc.getTimer().getGameTimeDeltaPartialTick(false), ps, buf, light, OverlayTexture.NO_OVERLAY);
               }
            } catch (Exception ignored) {
               // some block entity renderers need a real world around them
            }
         }
      }

      /** Lava/water: a full cube with the fluid's still texture (1.12 drew it with the world fluid renderer). */
      private static void fluidCube(Minecraft mc, PoseStack ps, MultiBufferSource buf, BlockState state) {
         var fluid = state.getFluidState().getType();
         ResourceLocation still = net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions.of(fluid).getStillTexture();
         TextureAtlasSprite sp = mc.getTextureAtlas(net.minecraft.world.inventory.InventoryMenu.BLOCK_ATLAS).apply(still);
         VertexConsumer vc = buf.getBuffer(RenderType.entityTranslucent(sp.atlasLocation()));
         PoseStack.Pose pose = ps.last();
         float u0 = sp.getU0(), u1 = sp.getU1(), v0 = sp.getV0(), v1 = sp.getV1();
         for (Direction d : Direction.values()) {
            float[][] c = face(d);
            float[][] uv = {{u0, v1}, {u1, v1}, {u1, v0}, {u0, v0}};
            for (int i = 0; i < 4; i++) {
               vc.addVertex(pose, c[i][0], c[i][1], c[i][2]).setColor(-1).setUv(uv[i][0], uv[i][1]).setOverlay(OverlayTexture.NO_OVERLAY)
                  .setLight(0xF000F0).setNormal(pose, d.getStepX(), d.getStepY(), d.getStepZ());
            }
         }
      }

      private static float[][] face(Direction d) {
         return switch (d) {
            case DOWN -> new float[][]{{0, 0, 1}, {0, 0, 0}, {1, 0, 0}, {1, 0, 1}};
            case UP -> new float[][]{{0, 1, 0}, {0, 1, 1}, {1, 1, 1}, {1, 1, 0}};
            case NORTH -> new float[][]{{1, 0, 0}, {0, 0, 0}, {0, 1, 0}, {1, 1, 0}};
            case SOUTH -> new float[][]{{0, 0, 1}, {1, 0, 1}, {1, 1, 1}, {0, 1, 1}};
            case WEST -> new float[][]{{0, 0, 0}, {0, 0, 1}, {0, 1, 1}, {0, 1, 0}};
            case EAST -> new float[][]{{1, 0, 1}, {1, 0, 0}, {1, 1, 0}, {1, 1, 1}};
         };
      }
   }

   private static BlockState[][][] layers(BlockState[][]... l) {
      return l;
   }

   /** ConfigRecipes.initializeCompoundRecipes: the blueprints of the multiblocks made with salis mundus. */
   private static void blueprints() {
      RecipeCatalog.addFake(id("infernalfurnace"), new BlueprintView("INFERNALFURNACE", () -> new ItemStack(ModDevices.INFERNAL_FURNACE_ITEM.get()), () -> {
         BlockState nb = Blocks.NETHER_BRICKS.defaultBlockState(), ob = Blocks.OBSIDIAN.defaultBlockState();
         BlockState ib = Blocks.IRON_BARS.defaultBlockState(), la = Blocks.LAVA.defaultBlockState();
         return layers(
            new BlockState[][]{{nb, ob, nb}, {ob, null, ob}, {nb, ob, nb}},
            new BlockState[][]{{nb, ob, nb}, {ob, la, ob}, {nb, ib, nb}},
            new BlockState[][]{{nb, ob, nb}, {ob, ob, ob}, {nb, ob, nb}});
      }, () -> List.of(new ItemStack(Blocks.NETHER_BRICKS, 12), new ItemStack(Blocks.OBSIDIAN, 12), new ItemStack(Blocks.IRON_BARS),
         new ItemStack(Items.LAVA_BUCKET))));
      altar("infusionaltar", "INFUSION", () -> ModBlocks.STONE_ARCANE.get().defaultBlockState(), () -> ModBlocks.PEDESTAL_ARCANE.get().defaultBlockState());
      altar("infusionaltarancient", "INFUSIONANCIENT", () -> ModBlocks.STONE_ANCIENT.get().defaultBlockState(),
         () -> ModBlocks.PEDESTAL_ANCIENT.get().defaultBlockState());
      altar("infusionaltareldritch", "INFUSIONELDRITCH", () -> ModBlocks.STONE_ELDRITCH_TILE.get().defaultBlockState(),
         () -> ModBlocks.PEDESTAL_ELDRITCH.get().defaultBlockState());
      RecipeCatalog.addFake(id("thaumatorium"), new BlueprintView("THAUMATORIUM", () -> new ItemStack(ModDevices.THAUMATORIUM_ITEM.get()), () -> {
         BlockState m = ModDevices.METAL_ALCHEMICAL.get().defaultBlockState();
         return layers(new BlockState[][]{{m}}, new BlockState[][]{{m}}, new BlockState[][]{{ModBlocks.CRUCIBLE.get().defaultBlockState()}});
      }, () -> List.of(new ItemStack(ModDevices.METAL_ALCHEMICAL.get(), 2), new ItemStack(ModBlocks.CRUCIBLE.get()))));
      RecipeCatalog.addFake(id("golempress"), new BlueprintView("MINDCLOCKWORK", () -> new ItemStack(ModGolems.GOLEM_BUILDER.get()), () -> {
         BlockState bars = Blocks.IRON_BARS.defaultBlockState();
         return layers(
            new BlockState[][]{{null, null}, {bars, null}},
            new BlockState[][]{{Blocks.CAULDRON.defaultBlockState(), Blocks.ANVIL.defaultBlockState()},
               {Blocks.PISTON.defaultBlockState().setValue(PistonBaseBlock.FACING, Direction.UP), ModBlocks.TABLE_STONE.get().defaultBlockState()}});
      }, () -> List.of(new ItemStack(Blocks.IRON_BARS), new ItemStack(Items.CAULDRON), new ItemStack(Blocks.PISTON), new ItemStack(Blocks.ANVIL),
         new ItemStack(ModBlocks.TABLE_STONE.get()))));
   }

   private static void altar(String name, String research, Supplier<BlockState> stone, Supplier<BlockState> pedestal) {
      RecipeCatalog.addFake(id(name), new BlueprintView(research, () -> new ItemStack(ModBlocks.INFUSION_MATRIX.get()), () -> {
         BlockState s = stone.get(), im = ModBlocks.INFUSION_MATRIX.get().defaultBlockState();
         return layers(
            new BlockState[][]{{null, null, null}, {null, im, null}, {null, null, null}},
            new BlockState[][]{{s, null, s}, {null, null, null}, {s, null, s}},
            new BlockState[][]{{s, null, s}, {null, pedestal.get(), null}, {s, null, s}});
      }, () -> List.of(new ItemStack(stone.get().getBlock(), 8), new ItemStack(pedestal.get().getBlock()), new ItemStack(ModBlocks.INFUSION_MATRIX.get()))));
   }
}
