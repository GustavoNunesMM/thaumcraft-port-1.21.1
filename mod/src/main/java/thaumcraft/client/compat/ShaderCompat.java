package thaumcraft.client.compat;

import java.lang.reflect.Method;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

/**
 * Soft detection of shader-pack mods (Iris, and Oculus which exposes the same API). Nothing here hard-depends on them:
 * the Iris API is looked up reflectively once, and every failure simply means "no shader pack".
 *
 * <p>Used where the vanilla rendering path conflicts with a shader pack's own pipeline (post-processing chains).
 */
public final class ShaderCompat {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static boolean initialised;
   private static Object irisApi;
   private static Method isShaderPackInUse;
   private static boolean warned;

   private ShaderCompat() {
   }

   private static void init() {
      initialised = true;
      try {
         Class<?> api = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
         Object instance = api.getMethod("getInstance").invoke(null);
         Method m = api.getMethod("isShaderPackInUse");
         irisApi = instance;
         isShaderPackInUse = m;
         LOGGER.info("Thaumcraft: Iris/Oculus API found, shader-pack compatibility paths enabled");
      } catch (ClassNotFoundException e) {
         // no Iris/Oculus installed
      } catch (Throwable t) {
         LOGGER.warn("Thaumcraft: Iris API present but unusable, assuming no shader pack", t);
      }
   }

   /** True when Iris/Oculus is installed (whether or not a pack is active). */
   public static boolean isIrisLoaded() {
      if (!initialised) init();
      return isShaderPackInUse != null;
   }

   /** True when a shader pack is currently active (Iris/Oculus). Cheap enough to call every frame. */
   public static boolean isShaderPackInUse() {
      if (!initialised) init();
      if (isShaderPackInUse == null) return false;
      try {
         return (Boolean) isShaderPackInUse.invoke(irisApi);
      } catch (Throwable t) {
         if (!warned) {
            warned = true;
            LOGGER.warn("Thaumcraft: IrisApi.isShaderPackInUse failed, assuming no shader pack", t);
         }
         return false;
      }
   }
}
