"""Converts the 1.12 ModelRenderer entity models of the eldritch creatures (ModelEldritchGuardian, ModelEldritchGolem,
ModelEldritchCrab) into 1.21 LayerDefinitions: client/renderers/models/EldritchModelLayers.java (generated, don't edit).

Every `new ModelRenderer(this, u, v)` + addBox/mirror/setRotationPoint/setRotation becomes a CubeListBuilder and a
PartPose; `a.addChild(b)` nests b under a (the child's rotation point stays relative to the parent, as in 1.12).
The mirror flag is taken as it was when each box was added. For every model an `index` method maps the 1.12 field
names to the baked ModelParts, so the hand written models can animate them by name.

Top level parts that the 1.12 render() never draws unconditionally are reported (the model hides them itself).

Run from the repo root: python3 tools/convert_eldritch_models.py
"""
import os
import re

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..")
SRC = os.path.join(ROOT, "reference/thaumcraft/client/renderers/models/entity")
OUT = os.path.join(ROOT, "mod/src/main/java/thaumcraft/client/renderers/models/EldritchModelLayers.java")
MODELS = {"guardian": "ModelEldritchGuardian", "golem": "ModelEldritchGolem", "crab": "ModelEldritchCrab"}


def fl(s):
    s = s.strip().replace("(float)", "").replace("Math.PI", "3.141592653589793")
    s = re.sub(r"(\d)[Ff]\b", r"\1", s)
    v = float(eval(s))
    t = f"{v:.7g}"
    return (t if "." in t or "e" in t else t + ".0") + "F"


def body_of(text, sig):
    start = text.index(sig)
    i = text.index("{", start) + 1
    depth, j = 1, i
    while depth:
        depth += {"{": 1, "}": -1}.get(text[j], 0)
        j += 1
    return text[i:j - 1]


def convert(key, cls):
    text = open(os.path.join(SRC, cls + ".java")).read()
    body = body_of(text, f"public {cls}()")
    tw = int(re.search(r"textureWidth = (\d+)", body).group(1))
    th = int(re.search(r"textureHeight = (\d+)", body).group(1))
    parts = {}
    order = []
    parent = {}
    for st in body.split(";"):
        st = st.strip()
        m = re.match(r"this\.(\w+) = new ModelRenderer\(this, (\d+), (\d+)\)", st)
        if m:
            parts[m.group(1)] = {"uv": (int(m.group(2)), int(m.group(3))), "mirror": False, "boxes": [], "pos": ["0.0F"] * 3,
                                 "rot": ["0.0F"] * 3}
            order.append(m.group(1))
            continue
        m = re.match(r"this\.(\w+)\.addBox\((.*)\)$", st, re.S)
        if m:
            p = parts[m.group(1)]
            a = [x.strip() for x in m.group(2).split(",")]
            p["boxes"].append((p["uv"], p["mirror"], [fl(x) for x in a[:3]], [fl(x) for x in a[3:6]], fl(a[6]) if len(a) > 6 else None))
            continue
        m = re.match(r"this\.(\w+)\.mirror = (true|false)", st)
        if m:
            parts[m.group(1)]["mirror"] = m.group(2) == "true"
            continue
        m = re.match(r"this\.(\w+)\.setRotationPoint\((.*)\)$", st, re.S)
        if m:
            parts[m.group(1)]["pos"] = [fl(x) for x in m.group(2).split(",")]
            continue
        m = re.match(r"this\.setRotation\(this\.(\w+), (.*)\)$", st, re.S)
        if m:
            parts[m.group(1)]["rot"] = [fl(x) for x in m.group(2).split(",")]
            continue
        m = re.match(r"this\.(\w+)\.rotateAngle([XYZ]) = (.*)$", st, re.S)
        if m:
            parts[m.group(1)]["rot"]["XYZ".index(m.group(2))] = fl(m.group(3))
            continue
        m = re.match(r"this\.(\w+)\.addChild\(this\.(\w+)\)", st)
        if m:
            parent[m.group(2)] = m.group(1)
            continue
        if st and not re.match(r"this\.(\w+)\.setTextureSize|this\.texture(Width|Height)", st):
            raise SystemExit(f"{cls}: unhandled statement {st!r}")

    render = body_of(text, "public void render(")
    drawn = set(re.findall(r"this\.(\w+)\.render\(f5\)", render))
    cond = re.findall(r"if \([^)]*\)[^{]*\{([^}]*)\}", render)
    hidden = [n for n in order if n not in parent and n not in drawn]
    conditional = [n for n in order if n not in parent and any(f"this.{n}.render" in c for c in cond)]

    lines = [f"   /** {cls} ({tw}x{th}). Not drawn unconditionally in 1.12: {', '.join(sorted(set(hidden + conditional))) or 'none'}. */",
             f"   public static LayerDefinition {key}() {{",
             "      MeshDefinition mesh = new MeshDefinition();",
             "      PartDefinition root = mesh.getRoot();"]
    idx = [f"   public static Map<String, ModelPart> {key}Parts(ModelPart root) {{",
           "      Map<String, ModelPart> m = new HashMap<>();"]

    def emit(name, holder, path):
        p = parts[name]
        cubes = "CubeListBuilder.create()"
        for (u, v), mir, o, s, scale in p["boxes"]:
            cubes += f".texOffs({u}, {v}).mirror({'true' if mir else 'false'})"
            if scale and scale != "0.0F":
                cubes += f".addBox({', '.join(o)}, {', '.join(s)}, new CubeDeformation({scale}))"
            else:
                cubes += f".addBox({', '.join(o)}, {', '.join(s)})"
        pose = f"PartPose.offsetAndRotation({', '.join(p['pos'])}, {', '.join(p['rot'])})"
        kids = [c for c in order if parent.get(c) == name]
        var = f"p{name}" if kids else None
        call = f'{holder}.addOrReplaceChild("{name}", {cubes}, {pose});'
        lines.append(f"      {'PartDefinition ' + var + ' = ' if var else ''}{call}")
        access = path + f'.getChild("{name}")'
        idx.append(f'      m.put("{name}", {access});')
        for c in kids:
            emit(c, var, access)

    for n in order:
        if n not in parent:
            emit(n, "root", "root")
    lines.append(f"      return LayerDefinition.create(mesh, {tw}, {th});")
    lines.append("   }")
    idx.append("      return m;")
    idx.append("   }")
    return lines + [""] + idx, hidden + conditional


out = ["package thaumcraft.client.renderers.models;", "",
       "import java.util.HashMap;", "import java.util.Map;", "import net.minecraft.client.model.geom.ModelPart;",
       "import net.minecraft.client.model.geom.PartPose;", "import net.minecraft.client.model.geom.builders.CubeDeformation;",
       "import net.minecraft.client.model.geom.builders.CubeListBuilder;", "import net.minecraft.client.model.geom.builders.LayerDefinition;",
       "import net.minecraft.client.model.geom.builders.MeshDefinition;", "import net.minecraft.client.model.geom.builders.PartDefinition;", "",
       "/** GENERATED by tools/convert_eldritch_models.py from the 1.12 eldritch creature models. Do not edit. */",
       "@SuppressWarnings(\"unused\")",
       "public final class EldritchModelLayers {",
       "   private EldritchModelLayers() {", "   }", ""]
for key, cls in MODELS.items():
    lines, hidden = convert(key, cls)
    out += lines + [""]
    print(cls, "hidden/conditional:", hidden)
out.append("}")
os.makedirs(os.path.dirname(OUT), exist_ok=True)
with open(OUT, "w") as f:
    f.write("\n".join(out) + "\n")
print("wrote", OUT)
