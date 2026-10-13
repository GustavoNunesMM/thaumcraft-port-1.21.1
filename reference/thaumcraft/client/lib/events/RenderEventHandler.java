package thaumcraft.client.lib.events;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.shader.ShaderGroup;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityNote;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.RayTraceResult.Type;
import net.minecraft.world.World;
import net.minecraftforge.client.event.DrawBlockHighlightEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.client.event.EntityViewRenderEvent.RenderFogEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent.ElementType;
import net.minecraftforge.client.event.RenderGameOverlayEvent.Pre;
import net.minecraftforge.client.event.RenderTooltipEvent.PostBackground;
import net.minecraftforge.event.entity.living.LivingEvent.LivingUpdateEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.fml.client.FMLClientHandler;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import net.minecraftforge.fml.common.gameevent.TickEvent.PlayerTickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.RenderTickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;
import thaumcraft.api.ThaumcraftApiHelper;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectHelper;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.aspects.IAspectContainer;
import thaumcraft.api.golems.ISealDisplayer;
import thaumcraft.api.golems.seals.ISealConfigArea;
import thaumcraft.api.golems.seals.ISealEntity;
import thaumcraft.api.golems.seals.SealPos;
import thaumcraft.api.items.IArchitect;
import thaumcraft.api.items.IGogglesDisplayExtended;
import thaumcraft.client.fx.FXDispatcher;
import thaumcraft.client.lib.UtilsFX;
import thaumcraft.codechicken.lib.raytracer.RayTracer;
import thaumcraft.common.config.ModConfig;
import thaumcraft.common.entities.monster.mods.ChampionModifier;
import thaumcraft.common.golems.seals.SealEntity;
import thaumcraft.common.golems.seals.SealHandler;
import thaumcraft.common.lib.crafting.ThaumcraftCraftingManager;
import thaumcraft.common.lib.events.EssentiaHandler;
import thaumcraft.common.lib.network.PacketHandler;
import thaumcraft.common.lib.network.misc.PacketNote;
import thaumcraft.common.lib.utils.EntityUtils;
import thaumcraft.common.tiles.crafting.TileInfusionMatrix;
import thaumcraft.common.tiles.devices.TileArcaneEar;
import thaumcraft.common.tiles.devices.TileRedstoneRelay;

@EventBusSubscriber({Side.CLIENT})
public class RenderEventHandler {
   public static RenderEventHandler INSTANCE = new RenderEventHandler();
   @SideOnly(Side.CLIENT)
   public static HudHandler hudHandler = new HudHandler();
   @SideOnly(Side.CLIENT)
   public static WandRenderingHandler wandHandler = new WandRenderingHandler();
   @SideOnly(Side.CLIENT)
   static ShaderHandler shaderhandler = new ShaderHandler();
   public static List blockTags = new ArrayList();
   public static float tagscale = 0.0F;
   public static int tickCount = 0;
   static boolean checkedDate = false;
   private Random random = new Random();
   public static boolean resetShaders = false;
   private static int oldDisplayWidth = 0;
   private static int oldDisplayHeight = 0;
   public static Entity thaumTarget = null;
   static final ResourceLocation CFRAME = new ResourceLocation("thaumcraft", "textures/misc/frame_corner.png");
   static final ResourceLocation MIDDLE = new ResourceLocation("thaumcraft", "textures/misc/seal_area.png");
   static EnumFacing[][] rotfaces = new EnumFacing[][]{
      {EnumFacing.DOWN, EnumFacing.NORTH, EnumFacing.WEST},
      {EnumFacing.UP, EnumFacing.NORTH, EnumFacing.WEST},
      {EnumFacing.DOWN, EnumFacing.NORTH, EnumFacing.EAST},
      {EnumFacing.UP, EnumFacing.NORTH, EnumFacing.EAST},
      {EnumFacing.DOWN, EnumFacing.SOUTH, EnumFacing.EAST},
      {EnumFacing.UP, EnumFacing.SOUTH, EnumFacing.EAST},
      {EnumFacing.DOWN, EnumFacing.SOUTH, EnumFacing.WEST},
      {EnumFacing.UP, EnumFacing.SOUTH, EnumFacing.WEST}
   };
   static int[][] rotmat = new int[][]{{0, 270, 0}, {270, 180, 270}, {90, 0, 90}, {180, 90, 180}, {180, 180, 0}, {90, 270, 270}, {270, 90, 90}, {0, 0, 180}};
   public static HashMap<Integer, ShaderGroup> shaderGroups = new HashMap<>();
   public static boolean fogFiddled = false;
   public static float fogTarget = 0.0F;
   public static int fogDuration = 0;
   public static float prevVignetteBrightness = 0.0F;
   public static float targetBrightness = 1.0F;
   protected static final ResourceLocation vignetteTexPath = new ResourceLocation("thaumcraft", "textures/misc/vignette.png");

   @SideOnly(Side.CLIENT)
   @SubscribeEvent
   public static void playerTick(PlayerTickEvent event) {
      Minecraft mc = Minecraft.getMinecraft();
      if (event.side != Side.SERVER && event.player.getEntityId() == mc.player.getEntityId()) {
         if (event.phase == Phase.START) {
            try {
               shaderhandler.checkShaders(event, mc);
               if (ShaderHandler.warpVignette > 0) {
                  ShaderHandler.warpVignette--;
                  targetBrightness = 0.0F;
               } else {
                  targetBrightness = 1.0F;
               }

               if (fogFiddled) {
                  if (fogDuration < 100) {
                     fogTarget = 0.1F * (fogDuration / 100.0F);
                  } else if (fogTarget < 0.1F) {
                     fogTarget += 0.001F;
                  }

                  fogDuration--;
                  if (fogDuration < 0) {
                     fogFiddled = false;
                  }
               }
            } catch (Exception var3) {
            }
         }
      }
   }

   @SideOnly(Side.CLIENT)
   @SubscribeEvent
   public static void clientWorldTick(ClientTickEvent event) {
      if (event.side != Side.SERVER) {
         Minecraft mc = FMLClientHandler.instance().getClient();
         World world = mc.world;
         if (event.phase == Phase.START) {
            tickCount++;

            for (String fxk : EssentiaHandler.sourceFX.keySet().toArray(new String[0])) {
               EssentiaHandler.EssentiaSourceFX fx = EssentiaHandler.sourceFX.get(fxk);
               if (world != null) {
                  int mod = 0;
                  TileEntity tile = world.getTileEntity(fx.start);
                  if (tile != null && tile instanceof TileInfusionMatrix) {
                     mod = -1;
                  }

                  FXDispatcher.INSTANCE.essentiaTrailFx(fx.end, fx.start.up(mod), tickCount, fx.color, 0.1F, fx.ext);
                  EssentiaHandler.sourceFX.remove(fxk);
               }
            }
         } else {
            LinkedBlockingQueue<HudHandler.KnowledgeGainTracker> temp = new LinkedBlockingQueue<>();
            if (hudHandler.knowledgeGainTrackers.isEmpty()) {
               if (hudHandler.kgFade > 0.0F) {
                  hudHandler.kgFade--;
               }
            } else {
               hudHandler.kgFade += 10.0F;
               if (hudHandler.kgFade > 40.0F) {
                  hudHandler.kgFade = 40.0F;
               }

               while (!hudHandler.knowledgeGainTrackers.isEmpty()) {
                  HudHandler.KnowledgeGainTracker current = hudHandler.knowledgeGainTrackers.poll();
                  if (current != null && current.progress > 0) {
                     current.progress--;
                     temp.offer(current);
                  }
               }

               while (!temp.isEmpty()) {
                  hudHandler.knowledgeGainTrackers.offer(temp.poll());
               }
            }

            if (mc.world != null && !checkedDate) {
               checkedDate = true;
               Calendar calendar = mc.world.getCurrentDate();
               if (calendar.get(2) + 1 == 10 && calendar.get(5) == 31) {
                  ModConfig.isHalloween = true;
               }
            }
         }
      }
   }

   @SideOnly(Side.CLIENT)
   @SubscribeEvent
   public static void renderTick(RenderTickEvent event) {
      if (event.phase == Phase.START) {
         UtilsFX.sysPartialTicks = event.renderTickTime;
      } else {
         Minecraft mc = FMLClientHandler.instance().getClient();
         if (Minecraft.getMinecraft().getRenderViewEntity() instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer)Minecraft.getMinecraft().getRenderViewEntity();
            long time = System.currentTimeMillis();
            if (player != null) {
               hudHandler.renderHuds(mc, event.renderTickTime, player, time);
            }

            if (player.getHeldItemMainhand() != null && player.getHeldItemMainhand().getItem() instanceof IArchitect
               || player.getHeldItemOffhand() != null && player.getHeldItemOffhand().getItem() instanceof IArchitect) {
               ItemStack stack = player.getHeldItemMainhand() != null && player.getHeldItemMainhand().getItem() instanceof IArchitect
                  ? player.getHeldItemMainhand()
                  : player.getHeldItemOffhand();
               if (!((IArchitect)stack.getItem()).useBlockHighlight(stack)) {
                  RayTraceResult target2 = ((IArchitect)stack.getItem()).getArchitectMOP(stack, player.world, player);
                  if (target2 != null) {
                     wandHandler.handleArchitectOverlay(stack, player, event.renderTickTime, player.ticksExisted, target2);
                  }
               }
            }
         }
      }
   }

   @SideOnly(Side.CLIENT)
   @SubscribeEvent
   public static void tooltipEvent(ItemTooltipEvent event) {
      Minecraft mc = FMLClientHandler.instance().getClient();
      GuiScreen gui = mc.currentScreen;
      if (gui instanceof GuiContainer && GuiScreen.isShiftKeyDown() != ModConfig.CONFIG_GRAPHICS.showTags && !Mouse.isGrabbed() && event.getItemStack() != null) {
         AspectList tags = ThaumcraftCraftingManager.getObjectTags(event.getItemStack());
         int index = 0;
         if (tags != null && tags.size() > 0) {
            for (Aspect tag : tags.getAspects()) {
               if (tag != null) {
                  index++;
               }
            }
         }

         int width = index * 18;
         if (width > 0) {
            double sw = mc.fontRenderer.getStringWidth(" ");
            int t = MathHelper.ceil(width / sw);
            int l = MathHelper.ceil(18.0 / mc.fontRenderer.FONT_HEIGHT);

            for (int a = 0; a < l; a++) {
               event.getToolTip()
                  .add(
                     "                                                                                                                                            "
                        .substring(0, Math.min(120, t))
                  );
            }
         }
      }
   }

   @SideOnly(Side.CLIENT)
   @SubscribeEvent
   public static void tooltipEvent(PostBackground event) {
      Minecraft mc = FMLClientHandler.instance().getClient();
      GuiScreen gui = mc.currentScreen;
      if (gui instanceof GuiContainer && GuiScreen.isShiftKeyDown() != ModConfig.CONFIG_GRAPHICS.showTags && !Mouse.isGrabbed()) {
         int bot = event.getHeight();
         if (!event.getLines().isEmpty()) {
            for (int a = event.getLines().size() - 1; a >= 0; a--) {
               if (event.getLines().get(a) != null && !((String)event.getLines().get(a)).contains("    ")) {
                  bot -= 10;
               } else if (a > 0 && event.getLines().get(a - 1) != null && ((String)event.getLines().get(a - 1)).contains("    ")) {
                  hudHandler.renderAspectsInGui((GuiContainer)gui, mc.player, event.getStack(), bot, event.getX(), event.getY());
                  break;
               }
            }
         }
      }
   }

   @SideOnly(Side.CLIENT)
   @SubscribeEvent
   public static void renderOverlay(RenderGameOverlayEvent event) {
      Minecraft mc = Minecraft.getMinecraft();
      long time = System.nanoTime() / 1000000L;
      if (event.getType() == ElementType.TEXT) {
         wandHandler.handleFociRadial(mc, time, event);
      }

      if (event.getType() == ElementType.PORTAL) {
         renderVignette(targetBrightness, event.getResolution().getScaledWidth_double(), event.getResolution().getScaledHeight_double());
      }
   }

   @SideOnly(Side.CLIENT)
   @SubscribeEvent
   public static void renderShaders(Pre event) {
      if (!ModConfig.CONFIG_GRAPHICS.disableShaders && event.getType() == ElementType.ALL) {
         Minecraft mc = Minecraft.getMinecraft();
         if (OpenGlHelper.shadersSupported && shaderGroups.size() > 0) {
            updateShaderFrameBuffers(mc);
            GL11.glMatrixMode(5890);
            GL11.glLoadIdentity();

            for (ShaderGroup sg : shaderGroups.values()) {
               GL11.glPushMatrix();

               try {
                  sg.render(event.getPartialTicks());
               } catch (Exception var5) {
               }

               GL11.glPopMatrix();
            }

            mc.getFramebuffer().bindFramebuffer(true);
         }
      }
   }

   private static void updateShaderFrameBuffers(Minecraft mc) {
      if (resetShaders || mc.displayWidth != oldDisplayWidth || oldDisplayHeight != mc.displayHeight) {
         for (ShaderGroup sg : shaderGroups.values()) {
            sg.createBindFramebuffers(mc.displayWidth, mc.displayHeight);
         }

         oldDisplayWidth = mc.displayWidth;
         oldDisplayHeight = mc.displayHeight;
         resetShaders = false;
      }
   }

   @SideOnly(Side.CLIENT)
   @SubscribeEvent
   public static void blockHighlight(DrawBlockHighlightEvent event) {
      int ticks = event.getPlayer().ticksExisted;
      RayTraceResult target = event.getTarget();
      if (blockTags.size() > 0) {
         int x = (Integer)blockTags.get(0);
         int y = (Integer)blockTags.get(1);
         int z = (Integer)blockTags.get(2);
         AspectList ot = (AspectList)blockTags.get(3);
         EnumFacing dir = EnumFacing.VALUES[blockTags.get(4)];
         if (x == target.getBlockPos().getX() && y == target.getBlockPos().getY() && z == target.getBlockPos().getZ()) {
            if (tagscale < 0.5F) {
               tagscale = tagscale + (0.031F - tagscale / 10.0F);
            }

            drawTagsOnContainer(
               target.getBlockPos().getX() + dir.getXOffset() / 2.0F,
               target.getBlockPos().getY() + dir.getYOffset() / 2.0F,
               target.getBlockPos().getZ() + dir.getZOffset() / 2.0F,
               ot,
               220,
               dir,
               event.getPartialTicks()
            );
         }
      }

      if (target != null && target.getBlockPos() != null) {
         TileEntity te = event.getPlayer().world.getTileEntity(target.getBlockPos());
         if (te != null && te instanceof TileRedstoneRelay) {
            RayTraceResult hit = RayTracer.retraceBlock(event.getPlayer().world, event.getPlayer(), target.getBlockPos());
            if (hit != null) {
               if (hit.subHit == 0) {
                  drawTextInAir(
                     target.getBlockPos().getX(),
                     target.getBlockPos().getY() + 0.3,
                     target.getBlockPos().getZ(),
                     event.getPartialTicks(),
                     "Out: " + ((TileRedstoneRelay)te).getOut()
                  );
               } else if (hit.subHit == 1) {
                  drawTextInAir(
                     target.getBlockPos().getX(),
                     target.getBlockPos().getY() + 0.3,
                     target.getBlockPos().getZ(),
                     event.getPartialTicks(),
                     "In: " + ((TileRedstoneRelay)te).getIn()
                  );
               }
            }
         }

         if (EntityUtils.hasGoggles(event.getPlayer())) {
            float to = 0.0F;
            if (te instanceof IGogglesDisplayExtended) {
               GL11.glDisable(2929);
               Vec3d v = ((IGogglesDisplayExtended)te).getIGogglesTextOffset();
               String[] sa = ((IGogglesDisplayExtended)te).getIGogglesText();

               for (String s : sa) {
                  drawTextInAir(
                     target.getBlockPos().getX() + v.x,
                     target.getBlockPos().getY() + v.y - (to - sa.length / 2.0F) / 5.5F,
                     target.getBlockPos().getZ() + v.z,
                     event.getPartialTicks(),
                     s
                  );
                  to++;
               }

               GL11.glEnable(2929);
            } else {
               Block b = event.getPlayer().world.getBlockState(target.getBlockPos()).getBlock();
               if (b instanceof IGogglesDisplayExtended) {
                  GL11.glDisable(2929);
                  Vec3d v = ((IGogglesDisplayExtended)b).getIGogglesTextOffset();
                  String[] sa = ((IGogglesDisplayExtended)b).getIGogglesText();

                  for (String s : sa) {
                     drawTextInAir(
                        target.getBlockPos().getX() + v.x,
                        target.getBlockPos().getY() + v.y + (to - sa.length / 2.0F) / 5.5F,
                        target.getBlockPos().getZ() + v.z,
                        event.getPartialTicks(),
                        s
                     );
                     to++;
                  }

                  GL11.glEnable(2929);
               }
            }

            boolean spaceAbove = event.getPlayer().world.isAirBlock(target.getBlockPos().up());
            if (te != null) {
               int note = -1;
               if (te instanceof TileEntityNote) {
                  note = ((TileEntityNote)te).note;
               } else if (te instanceof TileArcaneEar) {
                  note = ((TileArcaneEar)te).note;
               } else if (te instanceof IAspectContainer && ((IAspectContainer)te).getAspects() != null && ((IAspectContainer)te).getAspects().size() > 0) {
                  float shift = 0.0F;
                  if (tagscale < 0.3F) {
                     tagscale = tagscale + (0.031F - tagscale / 10.0F);
                  }

                  drawTagsOnContainer(
                     target.getBlockPos().getX(),
                     target.getBlockPos().getY() + (spaceAbove ? 0.4F : 0.0F) + shift,
                     target.getBlockPos().getZ(),
                     ((IAspectContainer)te).getAspects(),
                     220,
                     spaceAbove ? EnumFacing.UP : event.getTarget().sideHit,
                     event.getPartialTicks()
                  );
               }

               if (note >= 0) {
                  if (ticks % 5 == 0) {
                     PacketHandler.INSTANCE
                        .sendToServer(
                           new PacketNote(
                              target.getBlockPos().getX(),
                              target.getBlockPos().getY(),
                              target.getBlockPos().getZ(),
                              event.getPlayer().world.provider.getDimension()
                           )
                        );
                  }

                  drawTextInAir(
                     target.getBlockPos().getX(),
                     target.getBlockPos().getY() + 1,
                     target.getBlockPos().getZ(),
                     event.getPartialTicks(),
                     "Note: " + note
                  );
               }
            }
         }
      }

      if (target.typeOfHit == Type.BLOCK
         && (
            event.getPlayer().getHeldItemMainhand() != null && event.getPlayer().getHeldItemMainhand().getItem() instanceof IArchitect
               || event.getPlayer().getHeldItemOffhand() != null && event.getPlayer().getHeldItemOffhand().getItem() instanceof IArchitect
         )) {
         ItemStack stack = event.getPlayer().getHeldItemMainhand() != null && event.getPlayer().getHeldItemMainhand().getItem() instanceof IArchitect
            ? event.getPlayer().getHeldItemMainhand()
            : event.getPlayer().getHeldItemOffhand();
         if (((IArchitect)stack.getItem()).useBlockHighlight(stack)) {
            RayTraceResult target2 = ((IArchitect)stack.getItem()).getArchitectMOP(stack, event.getPlayer().world, event.getPlayer());
            if (target2 != null
               && wandHandler.handleArchitectOverlay(stack, event.getPlayer(), event.getPartialTicks(), event.getPlayer().ticksExisted, target2)) {
               event.setCanceled(true);
            }
         }
      }
   }

   @SideOnly(Side.CLIENT)
   @SubscribeEvent
   public static void renderLast(RenderWorldLastEvent event) {
      if (tagscale > 0.0F) {
         tagscale -= 0.005F;
      }

      float partialTicks = event.getPartialTicks();
      Minecraft mc = Minecraft.getMinecraft();
      if (Minecraft.getMinecraft().getRenderViewEntity() instanceof EntityPlayer) {
         EntityPlayer player = (EntityPlayer)mc.getRenderViewEntity();
         if (player.getHeldItemMainhand() != null && player.getHeldItemMainhand().getItem() instanceof ISealDisplayer) {
            drawSeals(partialTicks, player);
         } else if (player.getHeldItemOffhand() != null && player.getHeldItemOffhand().getItem() instanceof ISealDisplayer) {
            drawSeals(partialTicks, player);
         }

         if (player.getHeldItemMainhand() != null && player.getHeldItemMainhand().getItem() instanceof IArchitect) {
            RayTraceResult target = ((IArchitect)player.getHeldItemMainhand().getItem()).getArchitectMOP(player.getHeldItemMainhand(), player.world, player);
            wandHandler.handleArchitectOverlay(player.getHeldItemMainhand(), player, partialTicks, player.ticksExisted, target);
         } else if (player.getHeldItemOffhand() != null && player.getHeldItemOffhand().getItem() instanceof IArchitect) {
            RayTraceResult target = ((IArchitect)player.getHeldItemOffhand().getItem()).getArchitectMOP(player.getHeldItemOffhand(), player.world, player);
            wandHandler.handleArchitectOverlay(player.getHeldItemOffhand(), player, partialTicks, player.ticksExisted, target);
         }

         if (thaumTarget != null) {
            AspectList ot = AspectHelper.getEntityAspects(thaumTarget);
            if (ot != null && !ot.aspects.isEmpty()) {
               if (tagscale < 0.5F) {
                  tagscale = tagscale + (0.031F - tagscale / 10.0F);
               }

               double iPX = thaumTarget.prevPosX + (thaumTarget.posX - thaumTarget.prevPosX) * partialTicks;
               double iPY = thaumTarget.prevPosY + (thaumTarget.posY - thaumTarget.prevPosY) * partialTicks;
               double iPZ = thaumTarget.prevPosZ + (thaumTarget.posZ - thaumTarget.prevPosZ) * partialTicks;
               drawTagsOnContainer(iPX, iPY + thaumTarget.height, iPZ, ot, 220, null, event.getPartialTicks());
            }
         }
      }
   }

   private static void drawSeals(float partialTicks, EntityPlayer player) {
      ConcurrentHashMap<SealPos, SealEntity> seals = SealHandler.sealEntities.get(player.world.provider.getDimension());
      if (seals != null && seals.size() > 0) {
         GL11.glPushMatrix();
         if (player.isSneaking()) {
            GL11.glDisable(2929);
         }

         GL11.glEnable(3042);
         GL11.glBlendFunc(770, 771);
         GL11.glDisable(2884);
         double iPX = player.prevPosX + (player.posX - player.prevPosX) * partialTicks;
         double iPY = player.prevPosY + (player.posY - player.prevPosY) * partialTicks;
         double iPZ = player.prevPosZ + (player.posZ - player.prevPosZ) * partialTicks;
         GL11.glTranslated(-iPX, -iPY, -iPZ);

         for (ISealEntity seal : seals.values()) {
            double dis = player.getDistanceSqToCenter(seal.getSealPos().pos);
            if (dis <= 256.0) {
               float alpha = 1.0F - (float)(dis / 256.0);
               boolean ia = false;
               if (seal.isStoppedByRedstone(player.world)) {
                  ia = true;
                  if (player.world.rand.nextFloat() < partialTicks / 12.0F) {
                     FXDispatcher.INSTANCE
                        .spark(
                           seal.getSealPos().pos.getX() + 0.5F + seal.getSealPos().face.getXOffset() * 0.66F,
                           seal.getSealPos().pos.getY() + 0.5F + seal.getSealPos().face.getYOffset() * 0.66F,
                           seal.getSealPos().pos.getZ() + 0.5F + seal.getSealPos().face.getZOffset() * 0.66F,
                           2.0F,
                           0.8F - player.world.rand.nextFloat() * 0.2F,
                           0.0F,
                           0.0F,
                           1.0F
                        );
                     ia = false;
                  }
               }

               renderSeal(
                  seal.getSealPos().pos.getX(),
                  seal.getSealPos().pos.getY(),
                  seal.getSealPos().pos.getZ(),
                  alpha,
                  seal.getSealPos().face,
                  seal.getSeal().getSealIcon(),
                  ia
               );
               drawSealArea(player, seal, alpha, partialTicks);
            }
         }

         GL11.glDisable(3042);
         GL11.glEnable(2884);
         if (player.isSneaking()) {
            GL11.glEnable(2929);
         }

         GL11.glPopMatrix();
      }
   }

   private static void drawSealArea(EntityPlayer player, ISealEntity seal, float alpha, float partialTicks) {
      GL11.glPushMatrix();
      float r = 0.0F;
      float g = 0.0F;
      float b = 0.0F;
      if (seal.getColor() > 0) {
         Color c = new Color(EnumDyeColor.byMetadata(seal.getColor() - 1).getColorValue());
         r = c.getRed() / 255.0F;
         g = c.getGreen() / 255.0F;
         b = c.getBlue() / 255.0F;
      } else {
         r = 0.7F + MathHelper.sin((player.ticksExisted + partialTicks + seal.getSealPos().pos.getX()) / 4.0F) * 0.1F;
         g = 0.7F + MathHelper.sin((player.ticksExisted + partialTicks + seal.getSealPos().pos.getY()) / 5.0F) * 0.1F;
         b = 0.7F + MathHelper.sin((player.ticksExisted + partialTicks + seal.getSealPos().pos.getZ()) / 6.0F) * 0.1F;
      }

      GL11.glPushMatrix();
      GL11.glTranslated(seal.getSealPos().pos.getX() + 0.5, seal.getSealPos().pos.getY() + 0.5, seal.getSealPos().pos.getZ() + 0.5);
      GL11.glRotatef(90.0F, -seal.getSealPos().face.getYOffset(), seal.getSealPos().face.getXOffset(), -seal.getSealPos().face.getZOffset());
      if (seal.getSealPos().face.getZOffset() < 0) {
         GL11.glTranslated(0.0, 0.0, -0.51F);
      } else {
         GL11.glTranslated(0.0, 0.0, 0.51F);
      }

      GL11.glRotatef(player.ticksExisted % 360 + partialTicks, 0.0F, 0.0F, 1.0F);
      UtilsFX.renderQuadCentered(MIDDLE, 0.9F, r, g, b, 200, 771, alpha * 0.8F);
      GL11.glPopMatrix();
      if (seal.getSeal() instanceof ISealConfigArea) {
         GL11.glDepthMask(false);
         AxisAlignedBB area = new AxisAlignedBB(
               seal.getSealPos().pos.getX(),
               seal.getSealPos().pos.getY(),
               seal.getSealPos().pos.getZ(),
               seal.getSealPos().pos.getX() + 1,
               seal.getSealPos().pos.getY() + 1,
               seal.getSealPos().pos.getZ() + 1
            )
            .offset(seal.getSealPos().face.getXOffset(), seal.getSealPos().face.getYOffset(), seal.getSealPos().face.getZOffset())
            .expand(
               seal.getSealPos().face.getXOffset() != 0 ? (seal.getArea().getX() - 1) * seal.getSealPos().face.getXOffset() : 0.0,
               seal.getSealPos().face.getYOffset() != 0 ? (seal.getArea().getY() - 1) * seal.getSealPos().face.getYOffset() : 0.0,
               seal.getSealPos().face.getZOffset() != 0 ? (seal.getArea().getZ() - 1) * seal.getSealPos().face.getZOffset() : 0.0
            )
            .grow(
               seal.getSealPos().face.getXOffset() == 0 ? seal.getArea().getX() - 1 : 0.0,
               seal.getSealPos().face.getYOffset() == 0 ? seal.getArea().getY() - 1 : 0.0,
               seal.getSealPos().face.getZOffset() == 0 ? seal.getArea().getZ() - 1 : 0.0
            );
         double[][] locs = new double[][]{
            {area.minX, area.minY, area.minZ},
            {area.minX, area.maxY - 1.0, area.minZ},
            {area.maxX - 1.0, area.minY, area.minZ},
            {area.maxX - 1.0, area.maxY - 1.0, area.minZ},
            {area.maxX - 1.0, area.minY, area.maxZ - 1.0},
            {area.maxX - 1.0, area.maxY - 1.0, area.maxZ - 1.0},
            {area.minX, area.minY, area.maxZ - 1.0},
            {area.minX, area.maxY - 1.0, area.maxZ - 1.0}
         };
         int q = 0;

         for (double[] loc : locs) {
            GL11.glPushMatrix();
            GL11.glTranslated(loc[0] + 0.5, loc[1] + 0.5, loc[2] + 0.5);
            int w = 0;

            for (EnumFacing face : rotfaces[q]) {
               GL11.glPushMatrix();
               GL11.glRotatef(90.0F, -face.getYOffset(), face.getXOffset(), -face.getZOffset());
               if (face.getZOffset() < 0) {
                  GL11.glTranslated(0.0, 0.0, -0.49F);
               } else {
                  GL11.glTranslated(0.0, 0.0, 0.49F);
               }

               GL11.glRotatef(90.0F, 0.0F, 0.0F, -1.0F);
               GL11.glRotatef(rotmat[q][w], 0.0F, 0.0F, 1.0F);
               UtilsFX.renderQuadCentered(CFRAME, 1.0F, r, g, b, 200, 771, alpha * 0.7F);
               GL11.glPopMatrix();
               w++;
            }

            GL11.glPopMatrix();
            q++;
         }

         GL11.glDepthMask(true);
      }

      GL11.glPopMatrix();
   }

   static void renderSeal(int x, int y, int z, float alpha, EnumFacing face, ResourceLocation resourceLocation, boolean ia) {
      GL11.glPushMatrix();
      GL11.glColor4f(ia ? 0.5F : 1.0F, ia ? 0.5F : 1.0F, ia ? 0.5F : 1.0F, alpha);
      translateSeal(x, y, z, face.ordinal(), -0.05F);
      GL11.glScalef(0.5F, 0.5F, 0.5F);
      UtilsFX.renderItemIn2D(resourceLocation.toString(), Minecraft.getMinecraft().getRenderViewEntity().isSneaking() ? 0.0F : 0.1F);
      GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
      GL11.glPopMatrix();
   }

   private static void translateSeal(float x, float y, float z, int orientation, float off) {
      if (orientation == 1) {
         GL11.glTranslatef(x + 0.25F, y + 1.0F, z + 0.75F);
         GL11.glRotatef(-90.0F, 1.0F, 0.0F, 0.0F);
      } else if (orientation == 0) {
         GL11.glTranslatef(x + 0.25F, y, z + 0.25F);
         GL11.glRotatef(90.0F, 1.0F, 0.0F, 0.0F);
      } else if (orientation == 3) {
         GL11.glTranslatef(x + 0.25F, y + 0.25F, z + 1.0F);
      } else if (orientation == 2) {
         GL11.glTranslatef(x + 0.75F, y + 0.25F, z);
         GL11.glRotatef(180.0F, 0.0F, 1.0F, 0.0F);
      } else if (orientation == 5) {
         GL11.glTranslatef(x + 1.0F, y + 0.25F, z + 0.75F);
         GL11.glRotatef(90.0F, 0.0F, 1.0F, 0.0F);
      } else if (orientation == 4) {
         GL11.glTranslatef(x, y + 0.25F, z + 0.25F);
         GL11.glRotatef(-90.0F, 0.0F, 1.0F, 0.0F);
      }

      GL11.glTranslatef(0.0F, 0.0F, -off);
   }

   @SideOnly(Side.CLIENT)
   @SubscribeEvent
   public static void fogDensityEvent(RenderFogEvent event) {
      if (fogFiddled && fogTarget > 0.0F) {
         GL11.glFogi(2917, 2048);
         GL11.glFogf(2914, fogTarget);
      }
   }

   @SubscribeEvent
   public static void livingTick(LivingUpdateEvent event) {
      if (event.getEntity().world.isRemote && event.getEntity() instanceof EntityCreature && !event.getEntity().isDead) {
         EntityCreature mob = (EntityCreature)event.getEntity();
         if (mob.getEntityAttribute(ThaumcraftApiHelper.CHAMPION_MOD) != null) {
            Integer t = (int)mob.getEntityAttribute(ThaumcraftApiHelper.CHAMPION_MOD).getAttributeValue();
            if (t != null && t >= 0 && t < ChampionModifier.mods.length) {
               ChampionModifier.mods[t].effect.showFX(mob);
            }
         }
      }
   }

   @SubscribeEvent
   public static void renderLivingPre(net.minecraftforge.client.event.RenderLivingEvent.Pre event) {
      if (event.getEntity().world.isRemote && event.getEntity() instanceof EntityCreature && !event.getEntity().isDead) {
         EntityCreature mob = (EntityCreature)event.getEntity();
         if (mob.getEntityAttribute(ThaumcraftApiHelper.CHAMPION_MOD) != null) {
            Integer t = (int)mob.getEntityAttribute(ThaumcraftApiHelper.CHAMPION_MOD).getAttributeValue();
            if (t != null && t >= 0 && t < ChampionModifier.mods.length) {
               ChampionModifier.mods[t].effect.preRender(mob, event.getRenderer());
            }
         }
      }
   }

   public static void drawTagsOnContainer(double x, double y, double z, AspectList tags, int bright, EnumFacing dir, float partialTicks) {
      if (Minecraft.getMinecraft().getRenderViewEntity() instanceof EntityPlayer && tags != null && tags.size() > 0) {
         int fox = 0;
         int foy = 0;
         int foz = 0;
         if (dir != null) {
            fox = dir.getXOffset();
            foy = dir.getYOffset();
            foz = dir.getZOffset();
         } else {
            x -= 0.5;
            z -= 0.5;
         }

         EntityPlayer player = (EntityPlayer)Minecraft.getMinecraft().getRenderViewEntity();
         double iPX = player.prevPosX + (player.posX - player.prevPosX) * partialTicks;
         double iPY = player.prevPosY + (player.posY - player.prevPosY) * partialTicks;
         double iPZ = player.prevPosZ + (player.posZ - player.prevPosZ) * partialTicks;
         int rowsize = 5;
         int current = 0;
         float shifty = 0.0F;
         int left = tags.size();

         for (Aspect tag : tags.getAspects()) {
            int div = Math.min(left, rowsize);
            if (current >= rowsize) {
               current = 0;
               shifty -= tagscale * 1.05F;
               left -= rowsize;
               if (left < rowsize) {
                  div = left % rowsize;
               }
            }

            float shift = (current - div / 2.0F + 0.5F) * tagscale * 4.0F;
            shift *= tagscale;
            Color color = new Color(tag.getColor());
            GL11.glPushMatrix();
            GL11.glDisable(2929);
            GL11.glTranslated(-iPX + x + 0.5 + tagscale * 2.0F * fox, -iPY + y - shifty + 0.5 + tagscale * 2.0F * foy, -iPZ + z + 0.5 + tagscale * 2.0F * foz);
            float xd = (float)(iPX - (x + 0.5));
            float zd = (float)(iPZ - (z + 0.5));
            float rotYaw = (float)(Math.atan2(xd, zd) * 180.0 / Math.PI);
            GL11.glRotatef(rotYaw + 180.0F, 0.0F, 1.0F, 0.0F);
            GL11.glTranslated(shift, 0.0, 0.0);
            GL11.glRotatef(90.0F, 0.0F, 0.0F, 1.0F);
            GL11.glScalef(tagscale, tagscale, tagscale);
            UtilsFX.renderQuadCentered(tag.getImage(), 1.0F, color.getRed() / 255.0F, color.getGreen() / 255.0F, color.getBlue() / 255.0F, bright, 771, 0.75F);
            if (tags.getAmount(tag) >= 0) {
               GL11.glRotatef(90.0F, 0.0F, 0.0F, 1.0F);
               String am = "" + tags.getAmount(tag);
               GL11.glScalef(0.04F, 0.04F, 0.04F);
               GL11.glTranslated(0.0, 6.0, -0.1);
               int sw = Minecraft.getMinecraft().fontRenderer.getStringWidth(am);
               GL11.glEnable(3042);
               GL11.glBlendFunc(770, 771);
               Minecraft.getMinecraft().fontRenderer.drawString(am, 14 - sw, 1, 1118481);
               GL11.glTranslated(0.0, 0.0, -0.1);
               Minecraft.getMinecraft().fontRenderer.drawString(am, 13 - sw, 0, 16777215);
            }

            GL11.glEnable(2929);
            GL11.glPopMatrix();
            current++;
         }
      }
   }

   public static void drawTextInAir(double x, double y, double z, float partialTicks, String text) {
      if (Minecraft.getMinecraft().getRenderViewEntity() instanceof EntityPlayer) {
         EntityPlayer player = (EntityPlayer)Minecraft.getMinecraft().getRenderViewEntity();
         double iPX = player.prevPosX + (player.posX - player.prevPosX) * partialTicks;
         double iPY = player.prevPosY + (player.posY - player.prevPosY) * partialTicks;
         double iPZ = player.prevPosZ + (player.posZ - player.prevPosZ) * partialTicks;
         GL11.glPushMatrix();
         GL11.glTranslated(-iPX + x + 0.5, -iPY + y + 0.5, -iPZ + z + 0.5);
         float xd = (float)(iPX - (x + 0.5));
         float zd = (float)(iPZ - (z + 0.5));
         float rotYaw = (float)(Math.atan2(xd, zd) * 180.0 / Math.PI);
         GL11.glRotatef(rotYaw + 180.0F, 0.0F, 1.0F, 0.0F);
         GL11.glRotatef(180.0F, 0.0F, 0.0F, 1.0F);
         GL11.glScalef(0.0125F, 0.0125F, 0.0125F);
         int sw = Minecraft.getMinecraft().fontRenderer.getStringWidth(text);
         GL11.glEnable(3042);
         GL11.glBlendFunc(770, 771);
         Minecraft.getMinecraft().fontRenderer.drawString(text, 1 - sw / 2, 1.0F, 16777215, true);
         GL11.glPopMatrix();
      }
   }

   protected static void renderVignette(float brightness, double sw, double sh) {
      int k = (int)sw;
      int l = (int)sh;
      brightness = 1.0F - brightness;
      prevVignetteBrightness = (float)(prevVignetteBrightness + (brightness - prevVignetteBrightness) * 0.01);
      if (prevVignetteBrightness > 0.0F) {
         float b = prevVignetteBrightness * (1.0F + MathHelper.sin(Minecraft.getMinecraft().player.ticksExisted / 2.0F) * 0.1F);
         GL11.glPushMatrix();
         GL11.glClear(256);
         GL11.glMatrixMode(5889);
         GL11.glLoadIdentity();
         GL11.glOrtho(0.0, sw, sh, 0.0, 1000.0, 3000.0);
         Minecraft.getMinecraft().getTextureManager().bindTexture(vignetteTexPath);
         GL11.glMatrixMode(5888);
         GL11.glLoadIdentity();
         GL11.glTranslatef(0.0F, 0.0F, -2000.0F);
         GL11.glDisable(2929);
         GL11.glDepthMask(false);
         OpenGlHelper.glBlendFunc(0, 769, 1, 0);
         GL11.glColor4f(b, b, b, 1.0F);
         Tessellator tessellator = Tessellator.getInstance();
         tessellator.getBuffer().begin(7, DefaultVertexFormats.POSITION_TEX);
         tessellator.getBuffer().pos(0.0, l, -90.0).tex(0.0, 1.0).endVertex();
         tessellator.getBuffer().pos(k, l, -90.0).tex(1.0, 1.0).endVertex();
         tessellator.getBuffer().pos(k, 0.0, -90.0).tex(1.0, 0.0).endVertex();
         tessellator.getBuffer().pos(0.0, 0.0, -90.0).tex(0.0, 0.0).endVertex();
         tessellator.draw();
         GL11.glDepthMask(true);
         GL11.glEnable(2929);
         GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
         OpenGlHelper.glBlendFunc(770, 771, 1, 0);
         GL11.glPopMatrix();
      }
   }

   @SideOnly(Side.CLIENT)
   @SubscribeEvent
   public static void textureStitchEventPre(net.minecraftforge.client.event.TextureStitchEvent.Pre event) {
      event.getMap().registerSprite(new ResourceLocation("thaumcraft", "research/quill"));
      event.getMap().registerSprite(new ResourceLocation("thaumcraft", "blocks/crystal"));
      event.getMap().registerSprite(new ResourceLocation("thaumcraft", "blocks/taint_growth_1"));
      event.getMap().registerSprite(new ResourceLocation("thaumcraft", "blocks/taint_growth_2"));
      event.getMap().registerSprite(new ResourceLocation("thaumcraft", "blocks/taint_growth_3"));
      event.getMap().registerSprite(new ResourceLocation("thaumcraft", "blocks/taint_growth_4"));
   }

   public static class ChargeEntry {
      public long time;
      public long tickTime;
      public ItemStack item;
      float charge = 0.0F;
      byte diff = 0;

      public ChargeEntry(long time, ItemStack item, float charge) {
         this.time = time;
         this.item = item;
         this.charge = charge;
      }
   }
}
