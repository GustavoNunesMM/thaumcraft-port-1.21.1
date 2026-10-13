package thaumcraft.client.golems;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import thaumcraft.Thaumcraft;

/**
 * A Wavefront OBJ split into its groups, drawn part by part with whatever transform and texture the caller set up
 * (WavefrontObject/AdvancedModelLoader of 1.12, which the golem and press renderers used for renderPart).
 */
public final class ObjModel {
   private static final Map<ResourceLocation, ObjModel> CACHE = new HashMap<>();

   /** Each face as its vertices: x, y, z, u, v (v as in the file, 0 at the bottom). */
   private final Map<String, List<float[][]>> groups = new LinkedHashMap<>();

   private ObjModel() {
   }

   public static ObjModel get(ResourceLocation loc) {
      return CACHE.computeIfAbsent(loc, ObjModel::load);
   }

   public static void clearCache() {
      CACHE.clear();
   }

   public Set<String> getPartNames() {
      return groups.keySet();
   }

   private static ObjModel load(ResourceLocation loc) {
      ObjModel m = new ObjModel();
      List<float[]> verts = new ArrayList<>();
      List<float[]> uvs = new ArrayList<>();
      String group = "default";
      try (var in = Minecraft.getInstance().getResourceManager().open(loc);
           BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
         String line;
         while ((line = r.readLine()) != null) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("#")) continue;
            String[] t = line.split("\\s+");
            switch (t[0]) {
               case "v" -> verts.add(new float[]{Float.parseFloat(t[1]), Float.parseFloat(t[2]), Float.parseFloat(t[3])});
               case "vt" -> uvs.add(new float[]{Float.parseFloat(t[1]), t.length > 2 ? Float.parseFloat(t[2]) : 0.0F});
               case "g", "o" -> group = t.length > 1 ? t[1] : "default";
               case "f" -> {
                  float[][] face = new float[t.length - 1][];
                  for (int i = 1; i < t.length; i++) {
                     String[] idx = t[i].split("/");
                     float[] v = verts.get(index(idx[0], verts.size()));
                     float[] uv = idx.length > 1 && !idx[1].isEmpty() ? uvs.get(index(idx[1], uvs.size())) : new float[]{0, 0};
                     face[i - 1] = new float[]{v[0], v[1], v[2], uv[0], uv[1]};
                  }
                  m.groups.computeIfAbsent(group, k -> new ArrayList<>()).add(face);
               }
               default -> {
               }
            }
         }
      } catch (Exception e) {
         Thaumcraft.LOGGER.error("Could not load OBJ model {}", loc, e);
      }
      return m;
   }

   private static int index(String s, int size) {
      int i = Integer.parseInt(s);
      return i < 0 ? size + i : i - 1;
   }

   /**
    * Draws one group. flipV: 1 - v, as WavefrontObject read the coordinates (the press renderer flipped the texture
    * matrix back, so it draws them as they are).
    */
   public void renderPart(String part, PoseStack pose, VertexConsumer vc, int light, int overlay, int argb, boolean flipV) {
      List<float[][]> faces = groups.get(part);
      if (faces == null) return;
      PoseStack.Pose last = pose.last();
      Matrix4f mat = last.pose();
      Vector3f n = new Vector3f();
      for (float[][] face : faces) {
         // Face normal from the first three vertices (calculateFaceNormal).
         Vector3f a = new Vector3f(face[1][0] - face[0][0], face[1][1] - face[0][1], face[1][2] - face[0][2]);
         Vector3f b = new Vector3f(face[2][0] - face[1][0], face[2][1] - face[1][1], face[2][2] - face[1][2]);
         a.cross(b);
         if (a.lengthSquared() > 0) a.normalize();
         n.set(a);
         last.transformNormal(n, n);
         // Triangles are sent as degenerate quads (the entity render types draw quads).
         int count = face.length;
         int[] order = count == 3 ? new int[]{0, 1, 2, 2} : count == 4 ? new int[]{0, 1, 2, 3} : null;
         if (order == null) {
            for (int i = 1; i + 1 < count; i++) emitQuad(vc, mat, face, new int[]{0, i, i + 1, i + 1}, n, light, overlay, argb, flipV);
         } else {
            emitQuad(vc, mat, face, order, n, light, overlay, argb, flipV);
         }
      }
   }

   private static void emitQuad(VertexConsumer vc, Matrix4f mat, float[][] face, int[] order, Vector3f n, int light, int overlay, int argb, boolean flipV) {
      for (int i : order) {
         float[] v = face[i];
         vc.addVertex(mat, v[0], v[1], v[2]).setColor(argb).setUv(v[3], flipV ? 1.0F - v[4] : v[4]).setOverlay(overlay)
            .setLight(light).setNormal(n.x(), n.y(), n.z());
      }
   }

   public void renderAll(PoseStack pose, VertexConsumer vc, int light, int overlay, int argb, boolean flipV) {
      for (String g : groups.keySet()) renderPart(g, pose, vc, light, overlay, argb, flipV);
   }

   public void renderAllExcept(String except, PoseStack pose, VertexConsumer vc, int light, int overlay, int argb, boolean flipV) {
      for (String g : groups.keySet()) {
         if (!g.equals(except)) renderPart(g, pose, vc, light, overlay, argb, flipV);
      }
   }
}
