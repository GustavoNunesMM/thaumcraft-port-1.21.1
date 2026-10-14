"""Generates the resources of milestone 12 (flux and taint): mob effect icons (cut from textures/misc/potions.png),
blockstates/models of the fluids, taint blocks, condenser, lattice and void siphon, item models (buckets use the NeoForge
fluid container model), loot tables, tags (mineable, logs, entity and damage type tags), the taint/dissolve damage
types, lang keys (en_us + pt_br copied from the 1.12 keys) and the aspects of the new blocks, items and creatures.

Run from the repo root: python3 tools/gen_marco12.py
"""
import collections
import json
import os

from PIL import Image

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..")
RES = os.path.join(ROOT, "mod/src/main/resources")
A = os.path.join(RES, "assets/thaumcraft")
D = os.path.join(RES, "data")
ORIG = os.path.join(ROOT, "decompiled/assets/thaumcraft")
NS = "thaumcraft"


def write(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        json.dump(obj, f, indent=2, ensure_ascii=False)
        f.write("\n")


def tc(n):
    return n if ":" in n else f"{NS}:{n}"


def model_ref(m):
    if ":" not in m:
        return "minecraft:block/" + m
    ns, path = m.split(":")
    return m if path.startswith(("block/", "item/")) else f"{ns}:block/{path}"


def model(name, obj, kind="block"):
    write(f"{A}/models/{kind}/{name}.json", obj)


def blockstate(name, obj):
    write(f"{A}/blockstates/{name}.json", obj)


def copy_model(name, extra=None, kind="block", src=None):
    m = json.load(open(f"{ORIG}/models/{kind}/{src or name}.json"))
    m.pop("__createdwith", None)
    if "parent" not in m and "elements" in m:
        m = {"parent": "minecraft:block/block", **m}
    elif "parent" in m:
        p = m["parent"]
        m["parent"] = model_ref(p) if kind == "block" or p.startswith(NS) else (p if ":" in p else "minecraft:" + p)
    for k, v in m.get("textures", {}).items():
        if isinstance(v, str) and v.startswith("blocks/"):
            m["textures"][k] = "minecraft:block/" + v[len("blocks/"):]
    if extra:
        m.update(extra)
    model(name, m, kind)


def block_item(name, parent=None):
    model(name, {"parent": tc("block/" + (parent or name))}, "item")


def generated(name, tex=None):
    model(name, {"parent": "minecraft:item/generated", "textures": {"layer0": tex or f"{NS}:items/{name}"}}, "item")


def loot(name, pools):
    write(f"{D}/{NS}/loot_table/blocks/{name}.json", {"type": "minecraft:block", "pools": pools, "random_sequence": f"{NS}:blocks/{name}"})


def loot_self(name):
    loot(name, [{"rolls": 1, "bonus_rolls": 0, "entries": [{"type": "minecraft:item", "name": tc(name)}],
                 "conditions": [{"condition": "minecraft:survives_explosion"}]}])


SILK = {"condition": "minecraft:match_tool", "predicate": {"predicates": {"minecraft:enchantments": [
    {"enchantments": "minecraft:silk_touch", "levels": {"min": 1}}]}}}
CRYSTAL = {"type": "minecraft:item", "name": tc("crystal_essence"),
           "functions": [{"function": "minecraft:set_components", "components": {f"{NS}:essentia": {"vitium": 1}}}]}


def loot_silk(name, extra_pools=()):
    loot(name, [{"rolls": 1, "bonus_rolls": 0, "entries": [{"type": "minecraft:item", "name": tc(name)}], "conditions": [SILK]}, *extra_pools])


def add_tag(kind, ns, path, values):
    p = f"{D}/{ns}/tags/{kind}/{path}.json"
    d = json.load(open(p)) if os.path.exists(p) else {"replace": False, "values": []}
    for v in values:
        if v not in d["values"]:
            d["values"].append(v)
    write(p, d)


# ====================================================================== mob effect icons
EFFECTS = {"flux_taint": (3, 1), "vis_exhaust": (5, 1), "infectious_vis_exhaust": (6, 1), "unnatural_hunger": (7, 1),
           "warp_ward": (3, 2), "death_gaze": (4, 2), "blurred_vision": (5, 2), "sun_scorned": (6, 2), "thaumarhia": (7, 2)}
sheet = Image.open(f"{A}/textures/misc/potions.png").convert("RGBA")
os.makedirs(f"{A}/textures/mob_effect", exist_ok=True)
for name, (x, y) in EFFECTS.items():
    sheet.crop((x * 18, 198 + y * 18, x * 18 + 18, 198 + y * 18 + 18)).save(f"{A}/textures/mob_effect/{name}.png")


# ====================================================================== 1.12 forge blockstates -> 1.21
def convert_forge_state(name, skip=("inventory",)):
    """Forge-marker blockstates with inline textures: every (model, textures) pair becomes a model of its own."""
    bs = json.load(open(f"{ORIG}/blockstates/{name}.json"))
    defaults = bs.get("defaults", {})
    made = {}

    def entry(e):
        m = e.get("model", defaults.get("model"))
        tex = dict(defaults.get("textures", {}))
        tex.update(e.get("textures", {}))
        key = (m, json.dumps(tex, sort_keys=True))
        if key not in made:
            if tex:
                mname = f"{name}_{len(made)}"
                model(mname, {"parent": model_ref(m), "textures": tex})
                made[key] = tc("block/" + mname)
            else:
                made[key] = model_ref(m)
        out = {"model": made[key]}
        for k in ("x", "y", "weight", "uvlock"):
            if k in e:
                out[k] = e[k]
        return out

    variants = {}
    for k, v in bs["variants"].items():
        if k in skip:
            continue
        vs = v if isinstance(v, list) else [v]
        vs = [entry(e) for e in vs]
        variants["" if k == "normal" else k] = vs if len(vs) > 1 else vs[0]
    blockstate(name, {"variants": variants})
    return made


# ====================================================================== fluids
for lvl in range(8):
    model(f"flux_goo_{lvl}", {"parent": "minecraft:block/block", "render_type": "minecraft:translucent",
                              "textures": {"particle": f"{NS}:blocks/flux_goo", "goo": f"{NS}:blocks/flux_goo"},
                              "elements": [{"from": [0, 0, 0], "to": [16, 2 * (lvl + 1), 16],
                                            "faces": {f: {"texture": "#goo", **({"cullface": f} if f != "up" or lvl == 7 else {})}
                                                      for f in ("north", "south", "east", "west", "up", "down")}}]})
blockstate("flux_goo", {"variants": {f"level={lvl}": {"model": tc(f"block/flux_goo_{lvl}")} for lvl in range(8)}})
model("liquid_death", {"textures": {"particle": f"{NS}:blocks/animatedglow"}})
blockstate("liquid_death", {"variants": {"": {"model": tc("block/liquid_death")}}})
model("purifying_fluid", {"textures": {"particle": "minecraft:block/water_still"}})
blockstate("purifying_fluid", {"variants": {"": {"model": tc("block/purifying_fluid")}}})

generated("bucket_death")
for name, fluid in {"bucket_pure": "purifying_fluid", "bucket_goo": "flux_goo"}.items():
    model(name, {"parent": "neoforge:item/bucket", "loader": "neoforge:fluid_container", "fluid": tc(fluid)}, "item")

# ====================================================================== taint
for m in ["taint_fibre", "taint_growth_1", "taint_growth_2", "taint_growth_3", "taint_growth_4", "taint_orb_0", "taint_orb_1", "taint_orb_2",
          "log_taintwood"]:
    copy_model(m, {"render_type": "minecraft:cutout"} if m != "log_taintwood" else None)
orig_fibre = json.load(open(f"{ORIG}/blockstates/taint_fibre.json"))
blockstate("taint_fibre", {"multipart": [
    {"when": {k: str(v).lower() for k, v in p["when"].items()}, "apply": dict(p["apply"], model=model_ref(p["apply"]["model"]))}
    for p in orig_fibre["multipart"]]})
generated("taint_fibre", f"{NS}:blocks/taint_fibres")

for name in ["taint_crust", "taint_soil", "taint_rock", "taint_geyser", "taint_feature", "taint_log"]:
    made = convert_forge_state(name)
    first = next(iter(made.values()))
    model(name, {"parent": first}, "item")

# ====================================================================== devices
copy_model("condenser")
model("condenser_off", {"parent": tc("block/condenser"), "textures": {"side": f"{NS}:blocks/condenser_off"}})
blockstate("condenser", {"variants": {"enabled=true": {"model": tc("block/condenser")}, "enabled=false": {"model": tc("block/condenser_off")}}})
block_item("condenser")
for m in ["condenser_lattice_core", "condenser_lattice_core_dirty", "condenser_lattice_side"]:
    copy_model(m)
for name in ["condenser_lattice", "condenser_lattice_dirty"]:
    bs = json.load(open(f"{ORIG}/blockstates/{name}.json"))
    blockstate(name, {"multipart": [dict({"apply": dict(p["apply"], model=model_ref(p["apply"]["model"]))},
                                         **({"when": {k: str(v).lower() for k, v in p["when"].items()}} if "when" in p else {}))
                                    for p in bs["multipart"]]})
    copy_model(name, kind="item")
copy_model("void_siphon", {"render_type": "minecraft:cutout"})
blockstate("void_siphon", {"variants": {f"enabled={e}": {"model": tc("block/void_siphon")} for e in ("true", "false")}})
block_item("void_siphon")

for name in ["bottle_taint", "causality_collapser", "sanity_soap", "bath_salts"]:
    generated(name)

# ====================================================================== loot tables
for name in ["condenser", "condenser_lattice", "condenser_lattice_dirty", "void_siphon"]:
    loot_self(name)
for name in ["taint_crust", "taint_soil", "taint_geyser", "taint_feature", "taint_log"]:
    loot_silk(name)
# BlockTaint.getDrops: tainted rock drops a vitium crystal when nextInt(15) + fortune > 13.
loot_silk("taint_rock", [{"rolls": 1, "bonus_rolls": 0, "entries": [CRYSTAL], "conditions": [
    {"condition": "minecraft:inverted", "term": SILK},
    {"condition": "minecraft:table_bonus", "enchantment": "minecraft:fortune", "chances": [1 / 15, 2 / 15, 3 / 15, 4 / 15]}]}])
# BlockTaintFibre: the glowing growth drops a crystal when nextInt(5) <= fortune.
loot("taint_fibre", [{"rolls": 1, "bonus_rolls": 0, "entries": [CRYSTAL], "conditions": [
    {"condition": "minecraft:block_state_property", "block": tc("taint_fibre"), "properties": {"growth3": "true"}},
    {"condition": "minecraft:table_bonus", "enchantment": "minecraft:fortune", "chances": [0.2, 0.4, 0.6, 0.8]}]}])

# ====================================================================== tags
add_tag("block", "minecraft", "mineable/pickaxe", [tc(n) for n in ["condenser", "condenser_lattice", "condenser_lattice_dirty", "void_siphon",
                                                                   "taint_rock", "taint_crust"]])
add_tag("block", "minecraft", "mineable/shovel", [tc("taint_soil")])
add_tag("block", "minecraft", "mineable/axe", [tc("taint_log")])
add_tag("block", "minecraft", "mineable/hoe", [tc("taint_fibre"), tc("taint_feature")])
add_tag("block", "minecraft", "logs", [tc("taint_log")])
add_tag("item", "minecraft", "logs", [tc("taint_log")])
add_tag("block", "minecraft", "replaceable", [tc("taint_fibre"), tc("flux_goo"), tc("liquid_death"), tc("purifying_fluid")])
add_tag("fluid", "minecraft", "water", [tc("purifying_fluid"), tc("flowing_purifying_fluid")])
add_tag("entity_type", "minecraft", "arthropod", [tc("taint_crawler")])
add_tag("entity_type", "minecraft", "sensitive_to_bane_of_arthropods", [tc("taint_crawler")])
add_tag("entity_type", "minecraft", "can_breathe_under_water", [tc("taint_seed"), tc("taint_seed_prime")])
add_tag("entity_type", "minecraft", "fall_damage_immune", [tc("taint_swarm")])
add_tag("damage_type", "minecraft", "bypasses_armor", [tc("taint"), tc("dissolve")])
add_tag("damage_type", "c", "is_magic", [tc("taint")])

for name in ["taint", "dissolve"]:
    write(f"{D}/{NS}/damage_type/{name}.json", {"message_id": name, "exhaustion": 0.0, "scaling": "when_caused_by_living_non_player"})

# ====================================================================== aspects (ConfigAspects)
write(f"{D}/{NS}/{NS}/aspects/items/flux.json", {"priority": 1, "entries": [
    {"item": tc("taint_fibre"), "aspects": {"herba": 5, "vitium": 10}},
    {"item": tc("taint_crust"), "aspects": {"victus": 5, "vitium": 5}},
    {"item": tc("taint_soil"), "aspects": {"terra": 5, "vitium": 5}},
    {"item": tc("taint_geyser"), "aspects": {"auram": 5, "aqua": 5, "vitium": 10}},
    {"item": tc("taint_rock"), "aspects": {"terra": 10, "vitium": 5}},
    {"item": tc("taint_feature"), "aspects": {"auram": 5, "bestia": 5, "vitium": 10}},
    {"item": tc("taint_log"), "aspects": {"herba": 5, "vitium": 5}},
]})
write(f"{D}/{NS}/{NS}/aspects/entities/flux.json", {"entries": [
    {"entity": tc("flux_rift"), "aspects": {"vitium": 20, "alienis": 20, "auram": 20}},
    {"entity": tc("taintacle"), "aspects": {"vitium": 15, "bestia": 10}},
    {"entity": tc("taint_seed"), "aspects": {"vitium": 20, "auram": 10, "herba": 5}},
    {"entity": tc("taint_seed_prime"), "aspects": {"vitium": 25, "auram": 15, "herba": 5}},
    {"entity": tc("taintacle_tiny"), "aspects": {"vitium": 5, "bestia": 5}},
    {"entity": tc("taint_swarm"), "aspects": {"vitium": 15, "aer": 5}},
    {"entity": tc("taint_crawler"), "aspects": {"vitium": 5, "bestia": 5}},
]})

# ====================================================================== spawn eggs
EGGS = ["taint_crawler", "taintacle", "taint_swarm", "taint_seed", "taint_seed_prime"]
for e in EGGS:
    model(f"{e}_spawn_egg", {"parent": "minecraft:item/template_spawn_egg"}, "item")

# ItemPrimalCrusher.isEffective lists the taint blocks too.
add_tag("block", NS, "mineable/primal_crusher", [tc(n) for n in ["taint_crust", "taint_rock", "taint_soil", "taint_feature", "taint_log",
                                                                 "taint_fibre"]])

# ====================================================================== warp potion shaders (ShaderHandler), GLSL 150
SH = f"{A}/shaders"
UNI_BASE = [
    {"name": "ProjMat", "type": "matrix4x4", "count": 16, "values": [1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0]},
    {"name": "InSize", "type": "float", "count": 2, "values": [1.0, 1.0]},
    {"name": "OutSize", "type": "float", "count": 2, "values": [1.0, 1.0]}]
BLEND = {"func": "add", "srcrgb": "one", "dstrgb": "zero"}


def program(name):
    write(f"{SH}/program/{name}.json", {"blend": BLEND, "vertex": "sobel", "fragment": tc(name), "attributes": ["Position"],
                                        "samplers": [{"name": "DiffuseSampler"}], "uniforms": UNI_BASE})


def text(path, body):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as fh:
        fh.write(body)


def two_pass(first, second=None):
    return {"targets": ["swap"], "passes": [dict({"intarget": "minecraft:main", "outtarget": "swap"}, **first),
                                            dict({"intarget": "swap", "outtarget": "minecraft:main"}, **(second or {"name": "blit"}))]}


write(f"{SH}/post/desaturatetc.json", two_pass({"name": "color_convolve", "uniforms": [{"name": "Saturation", "values": [0.8]}]}))
write(f"{SH}/post/hunger.json", two_pass({"name": "color_convolve", "uniforms": [{"name": "ColorScale", "values": [1.0, 0.8, 0.8]},
                                                                                   {"name": "Saturation", "values": [1.1]}]}))
write(f"{SH}/post/blurtc.json", two_pass({"name": "box_blur", "use_linear_filter": True, "uniforms": [{"name": "BlurDir", "values": [1.0, 0.0]},
                                                                                                      {"name": "Radius", "values": [10.0]}]},
                                         {"name": "box_blur", "use_linear_filter": True, "uniforms": [{"name": "BlurDir", "values": [0.0, 1.0]},
                                                                                                      {"name": "Radius", "values": [10.0]}]}))
write(f"{SH}/post/sunscorned.json", two_pass({"name": tc("bloomcolor")}, {"name": tc("bloom2")}))
program("bloomcolor")
program("bloom2")
text(f"{SH}/program/bloom2.fsh", """#version 150

uniform sampler2D DiffuseSampler;

in vec2 texCoord;
in vec2 oneTexel;

out vec4 fragColor;

// width of steps, number of steps and bloom brightness (bloom2.fsh)
const float blurWidth = 0.002;
const float blurSteps = 0.1;
const float amount = 5.0;

void main() {
    vec4 color = texture(DiffuseSampler, texCoord);
    float brightness = (color.r + color.g + color.b) / 3.0;
    vec4 sum = vec4(0.0);
    const float foo = 10.0 / (((blurSteps * 2.0) + 1.0) * 4.0);
    for (float i = -blurSteps; i < blurSteps; i++) {
        sum += texture(DiffuseSampler, texCoord + vec2(i, i) * blurWidth * 0.7);
        sum += texture(DiffuseSampler, texCoord + vec2(i, -i) * blurWidth * 0.7);
        sum += texture(DiffuseSampler, texCoord + vec2(0.0, i) * blurWidth);
        sum += texture(DiffuseSampler, texCoord + vec2(i, 0.0) * blurWidth);
    }
    sum *= foo;
    vec4 modifier = sum * sum * (0.015 - (brightness * 0.01));
    vec4 outputColor = modifier * amount + color;
    fragColor = vec4((outputColor * 0.9).rgb, 1.0);
}
""")
text(f"{SH}/program/bloomcolor.fsh", """#version 150

uniform sampler2D DiffuseSampler;

in vec2 texCoord;
in vec2 oneTexel;

out vec4 fragColor;

vec4 RGBtoHSL(vec4 col) {
    float red = col.r;
    float green = col.g;
    float blue = col.b;
    float minc = min(min(col.r, col.g), col.b);
    float maxc = max(max(col.r, col.g), col.b);
    float delta = maxc - minc;
    float lum = (minc + maxc) * 0.5;
    float sat = 0.0;
    float hue = 0.0;
    if (lum > 0.0 && lum < 1.0) {
        float mul = (lum < 0.5) ? (lum) : (1.0 - lum);
        sat = delta / (mul * 2.0);
    }
    vec3 masks = vec3(
        (maxc == red && maxc != green) ? 1.0 : 0.0,
        (maxc == green && maxc != blue) ? 1.0 : 0.0,
        (maxc == blue && maxc != red) ? 1.0 : 0.0);
    vec3 adds = vec3((green - blue) / delta, 2.0 + ((blue - red) / delta), 4.0 + ((red - green) / delta));
    float deltaGtz = (delta > 0.0) ? 1.0 : 0.0;
    hue += dot(adds, masks);
    hue *= deltaGtz;
    hue /= 6.0;
    if (hue < 0.0) hue += 1.0;
    return vec4(hue, sat, lum, col.a);
}

vec4 HSLtoRGB(vec4 col) {
    const float onethird = 1.0 / 3.0;
    const float twothird = 2.0 / 3.0;
    const float rcpsixth = 6.0;
    float hue = col.x;
    float sat = col.y;
    float lum = col.z;
    vec3 xt = vec3(rcpsixth * (hue - twothird), 0.0, rcpsixth * (1.0 - hue));
    if (hue < twothird) {
        xt.r = 0.0;
        xt.g = rcpsixth * (twothird - hue);
        xt.b = rcpsixth * (hue - onethird);
    }
    if (hue < onethird) {
        xt.r = rcpsixth * (onethird - hue);
        xt.g = rcpsixth * hue;
        xt.b = 0.0;
    }
    xt = min(xt, 1.0);
    float sat2 = 2.0 * sat;
    float satinv = 1.0 - sat;
    float luminv = 1.0 - lum;
    float lum2m1 = (2.0 * lum) - 1.0;
    vec3 ct = (sat2 * xt) + satinv;
    vec3 rgb;
    if (lum >= 0.5) rgb = (luminv * ct) + lum2m1;
    else rgb = lum * ct;
    return vec4(rgb, col.a);
}

void main() {
    // the colour averaged over 81 points of the screen
    vec4 averageColor = vec4(0.0);
    for (int x = 1; x <= 9; x++) {
        for (int y = 1; y <= 9; y++) {
            averageColor += texture(DiffuseSampler, vec2(float(x) * 0.1, float(y) * 0.1));
        }
    }
    averageColor /= 81.0;

    vec4 inputColor = texture(DiffuseSampler, texCoord);
    vec4 inputHSL = RGBtoHSL(inputColor);
    // desaturate dark things
    float brightness = min(0.2, inputHSL.b) * 5.0;
    vec4 finalHSL = inputHSL * vec4(1.0, 0.5 + (brightness * 0.5), 1.0, 1.0);
    vec4 satAjustedColor = HSLtoRGB(finalHSL);
    // HDR
    float averageBrightness = RGBtoHSL(averageColor).b;
    vec4 averagedColor = satAjustedColor * min(max(0.3 / averageBrightness, 0.7), 4.0);
    fragColor = vec4((averagedColor * 0.4 + satAjustedColor * 0.6).rgb, 1.0);
}
""")

# ====================================================================== lang
L = f"{A}/lang/en_us.json"
L2 = f"{A}/lang/pt_br.json"
en = json.load(open(L, encoding="utf-8"), object_pairs_hook=collections.OrderedDict)
pt = json.load(open(L2, encoding="utf-8"), object_pairs_hook=collections.OrderedDict)
COPY = {f"effect.thaumcraft.{k}": f"potion.{v}" for k, v in {
    "flux_taint": "flux_taint", "vis_exhaust": "vis_exhaust", "infectious_vis_exhaust": "infvisexhaust", "unnatural_hunger": "unhunger",
    "warp_ward": "warpward", "death_gaze": "deathgaze", "blurred_vision": "blurred", "sun_scorned": "sunscorned", "thaumarhia": "thaumarhia"}.items()}
COPY.update({f"entity.thaumcraft.{k}": f"entity.{v}.name" for k, v in {
    "flux_rift": "FluxRift", "falling_taint": "FallingTaint", "bottle_taint": "BottleTaint", "taint_crawler": "TaintCrawler",
    "taintacle": "Taintacle", "taintacle_tiny": "TaintacleTiny", "taint_seed": "TaintSeed", "taint_seed_prime": "TaintSeedPrime",
    "taint_swarm": "TaintSwarm"}.items()})
COPY["entity.thaumcraft.causality_collapser"] = "item.thaumcraft.causality_collapser"
for old in ["flux_goo", "liquid_death", "purifying_fluid"]:
    COPY[f"fluid_type.thaumcraft.{old}"] = f"block.thaumcraft.{old}"
for name in ["taint_fibre", "taint_crust", "taint_soil", "taint_rock", "taint_geyser", "taint_feature", "taint_log", "condenser",
             "condenser_lattice", "condenser_lattice_dirty", "void_siphon", "flux_goo", "purifying_fluid", "liquid_death"]:
    COPY[f"block.thaumcraft.{name}"] = f"tile.{name}.name"
for name in ["bath_salts", "bottle_taint", "sanity_soap", "causality_collapser", "void_seed"]:
    COPY[f"item.thaumcraft.{name}"] = f"item.{name}.name"
for d in (en, pt):
    for k, old in COPY.items():
        if k not in d:
            if old not in d:
                raise SystemExit(f"no lang for {k} ({old})")
            d[k] = d[old]
BUCKETS = {"bucket_death": "liquid_death", "bucket_pure": "purifying_fluid", "bucket_goo": "flux_goo"}
for item, fluid in BUCKETS.items():
    en.setdefault(f"item.thaumcraft.{item}", "Bucket of " + en[f"block.thaumcraft.{fluid}"])
    pt.setdefault(f"item.thaumcraft.{item}", "Balde de " + pt[f"block.thaumcraft.{fluid}"])
for e in EGGS:
    en.setdefault(f"item.thaumcraft.{e}_spawn_egg", en[f"entity.thaumcraft.{e}"] + " Spawn Egg")
    pt.setdefault(f"item.thaumcraft.{e}_spawn_egg", "Ovo Gerador de " + pt[f"entity.thaumcraft.{e}"])
en.setdefault("container.thaumcraft.void_siphon", en["block.thaumcraft.void_siphon"])
pt.setdefault("container.thaumcraft.void_siphon", pt["block.thaumcraft.void_siphon"])
json.dump(en, open(L, "w", encoding="utf-8"), indent=1, ensure_ascii=False)
json.dump(pt, open(L2, "w", encoding="utf-8"), indent=1, ensure_ascii=False)
print("ok")
