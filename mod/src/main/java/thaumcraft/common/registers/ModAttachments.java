package thaumcraft.common.registers;

import java.util.function.Supplier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import thaumcraft.Thaumcraft;
import thaumcraft.common.research.PlayerKnowledge;
import thaumcraft.common.research.PlayerWarp;
import thaumcraft.common.world.aura.AuraChunk;
import thaumcraft.common.world.aura.AuraHandler;

public final class ModAttachments {
   public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
      DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, Thaumcraft.MODID);

   public static final Supplier<AttachmentType<PlayerKnowledge>> KNOWLEDGE = ATTACHMENT_TYPES.register(
      "knowledge", () -> AttachmentType.builder(PlayerKnowledge::new).serialize(PlayerKnowledge.CODEC).copyOnDeath().build()
   );

   public static final Supplier<AttachmentType<PlayerWarp>> WARP = ATTACHMENT_TYPES.register(
      "warp", () -> AttachmentType.builder(PlayerWarp::new).serialize(PlayerWarp.CODEC).copyOnDeath().build()
   );

   /** Aura of a chunk; generated from the biomes the first time a server chunk is asked for it. */
   public static final Supplier<AttachmentType<AuraChunk>> AURA = ATTACHMENT_TYPES.register(
      "aura", () -> AttachmentType.<AuraChunk>builder(holder -> holder instanceof LevelChunk c && c.getLevel() instanceof ServerLevel sl
            ? AuraHandler.generate(sl, c.getPos(), sl.random) : AuraChunk.ungenerated())
         .serialize(AuraChunk.CODEC, AuraChunk::isGenerated).build()
   );

   private ModAttachments() {
   }
}
