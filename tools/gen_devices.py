"""Generates the JSON resources of the devices milestone: blockstates and models converted from the 1.12 Forge
blockstates (with their texture overrides and submodels merged into plain models), the thaumatorium OBJ model,
item models, loot tables (crates/urns, components kept by mirrors, brain jars and banners), mineable tags and the
en_us/pt_br lang keys copied from the old tile.*.name keys.

Run from the repo root: python3 tools/gen_devices.py
"""
import collections
import hashlib
import itertools
import json
import os
import shutil

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..")
RES = os.path.join(ROOT, "mod/src/main/resources")
A = os.path.join(RES, "assets/thaumcraft")
D = os.path.join(RES, "data")
ORIG = os.path.join(ROOT, "decompiled/assets/thaumcraft")
NS = "thaumcraft"
COLORS = ["white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray", "light_gray", "cyan", "purple", "blue",
          "brown", "green", "red", "black"]
OLD_COLOR = {"light_blue": "lightblue", "light_gray": "silver"}
# 1.12 vanilla texture names that changed in 1.13+
VANILLA_TEX = {"stone_slab_top": "smooth_stone", "redstone_torch_on": "redstone_torch", "fire_layer_0": "fire_0",
               "nether_brick": "nether_bricks", "planks_oak": "oak_planks", "furnace_top": "furnace_top"}


def write(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        json.dump(obj, f, indent=2, ensure_ascii=False)
        f.write("\n")


def tc(n):
    return n if ":" in n else f"{NS}:{n}"


def fix_tex(t):
    if not isinstance(t, str) or t.startswith("#"):
        return t
    if t.startswith("blocks/"):
        n = t[len("blocks/"):]
        return "minecraft:block/" + VANILLA_TEX.get(n, n)
    if t.startswith("items/"):
        return "minecraft:item/" + t[len("items/"):]
    if t.startswith(NS + ":") and not os.path.exists(f"{A}/textures/{t.split(':')[1]}.png"):
        print("WARNING missing texture", t)
    return t


def model_ref(m):
    """A 1.12 model name in a blockstate ('thaumcraft:lamp', 'cube_all') as a 1.21 model id."""
    if ":" not in m:
        return "minecraft:block/" + m
    ns, path = m.split(":")
    return m if path.startswith(("block/", "item/")) else f"{ns}:block/{path}"


def load_orig_model(m):
    """Raw original model by 1.12 name, or None for vanilla ones."""
    if ":" not in m or m.startswith("minecraft:"):
        return None
    path = m.split(":")[1]
    if path.startswith("block/"):
        path = path[len("block/"):]
    return json.load(open(f"{ORIG}/models/block/{path}.json"))


def clean_model(m):
    m = dict(m)
    m.pop("__createdwith", None)
    if "parent" in m:
        p = m["parent"]
        m["parent"] = p if ":" in p else "minecraft:" + p
    elif "elements" in m:
        m = {"parent": "minecraft:block/block", **m}
    m["textures"] = {k: fix_tex(v) for k, v in m.get("textures", {}).items()}
    return m


def model(name, obj, kind="block"):
    write(f"{A}/models/{kind}/{name}.json", obj)


def copy_model(name, src=None, extra=None):
    m = clean_model(load_orig_model(tc(src or name)))
    if extra:
        m.update(extra)
    model(name, m)


def blockstate(name, obj):
    write(f"{A}/blockstates/{name}.json", obj)


def item_parent(name, parent):
    model(name, {"parent": parent}, "item")


def generated_item(name, *layers):
    model(name, {"parent": "minecraft:item/generated", "textures": {f"layer{i}": l for i, l in enumerate(layers)}}, "item")


# ====================================================================== Forge blockstate conversion

COPIED = set()
DERIVED = {}


def ensure_copied(m, render_type):
    """Copies an original thaumcraft model under its own name (once)."""
    if not m.startswith(NS + ":"):
        return model_ref(m)
    path = m.split(":")[1]
    if path.startswith("block/"):
        path = path[len("block/"):]
    if path not in COPIED:
        extra = {"render_type": render_type} if render_type else None
        copy_model(path, extra=extra)
        COPIED.add(path)
    return tc("block/" + path)


def merged_model(base, subs, textures, render_type):
    """A model with the base model's and the submodels' elements (the submodels keep their own textures)."""
    bm = clean_model(load_orig_model(base))
    out_tex = dict(bm.get("textures", {}))
    elements = [dict(e) for e in bm.get("elements", [])]
    for i, s in enumerate(subs):
        sm = clean_model(load_orig_model(s))
        ren = {k: f"s{i}_{k}" for k in sm.get("textures", {})}
        for k, v in sm.get("textures", {}).items():
            if k != "particle":
                out_tex[ren[k]] = v
        for e in sm.get("elements", []):
            e = json.loads(json.dumps(e))
            for f in e.get("faces", {}).values():
                t = f.get("texture", "")
                if t.startswith("#") and t[1:] in ren:
                    f["texture"] = "#" + ren[t[1:]]
            elements.append(e)
    out_tex.update({k: fix_tex(v) for k, v in textures.items()})
    m = {"parent": "minecraft:block/block", "textures": out_tex, "elements": elements}
    if "ambientocclusion" in bm:
        m["ambientocclusion"] = bm["ambientocclusion"]
    if render_type:
        m["render_type"] = render_type
    return m


def resolve(name, v, render_type):
    """A merged forge variant -> a 1.21 model id (creating child/merged models as needed)."""
    base = v.get("model", "cube_all")
    textures = v.get("textures", {})
    subs = v.get("submodels", [])
    if not textures and not subs:
        return ensure_copied(base, render_type)
    key = json.dumps([base, sorted(textures.items()), subs], sort_keys=True)
    if key in DERIVED:
        return DERIVED[key]
    mname = f"{name}_{hashlib.md5(key.encode()).hexdigest()[:6]}"
    if subs:
        model(mname, merged_model(base, subs, textures, render_type))
    else:
        parent = ensure_copied(base, render_type)
        m = {"parent": parent, "textures": {k: fix_tex(t) for k, t in textures.items()}}
        if render_type and not parent.startswith(NS):
            m["render_type"] = render_type
        model(mname, m)
    DERIVED[key] = tc("block/" + mname)
    return DERIVED[key]


def merge(v, add):
    if isinstance(add, list):
        add = add[0] if add else {}
    for k, val in add.items():
        if k == "textures":
            v.setdefault("textures", {}).update(val)
        elif k == "submodel":
            v.setdefault("submodels", []).append(val if isinstance(val, str) else val["model"])
        elif k in ("model", "x", "y"):
            v[k] = val
    return v


def forge_state(name, props, src=None, render_type=None, item=True, rotations=None):
    """props: [(property, [values])] of the 1.21 block. Writes the blockstate and (optionally) the item model."""
    bs = json.load(open(f"{ORIG}/blockstates/{src or name}.json"))
    defaults = bs.get("defaults", {})
    variants = bs["variants"]
    out = {}
    for combo in itertools.product(*[[(p, val) for val in vals] for p, vals in props]):
        v = merge({}, json.loads(json.dumps(defaults)))
        if not combo and "normal" in variants:
            merge(v, variants["normal"])
        for p, val in combo:
            if p in variants and isinstance(variants[p], dict) and val in variants[p]:
                merge(v, variants[p][val])
        key = ",".join(f"{p}={val}" for p, val in sorted(combo))
        if key in variants:
            merge(v, variants[key])
        entry = {"model": resolve(name, v, render_type)}
        if rotations:
            v = dict(v, **rotations.get(key, {}))
        for r in ("x", "y"):
            if v.get(r):
                entry[r] = v[r]
        out[key] = entry
    blockstate(name, {"variants": out})
    if item:
        inv = merge(merge({}, json.loads(json.dumps(defaults))), variants.get("inventory", {}))
        item_parent(name, resolve(name, inv, render_type))


def loot_self(name, components=None, conditions=True):
    entry = {"type": "minecraft:item", "name": tc(name)}
    if components:
        entry["functions"] = [{"function": "minecraft:copy_components", "source": "block_entity", "include": components}]
    pool = {"rolls": 1, "bonus_rolls": 0, "entries": [entry]}
    if conditions:
        pool["conditions"] = [{"condition": "minecraft:survives_explosion"}]
    write(f"{D}/{NS}/loot_table/blocks/{name}.json", {"type": "minecraft:block", "pools": [pool], "random_sequence": f"{NS}:blocks/{name}"})


def loot_item(name, item):
    write(f"{D}/{NS}/loot_table/blocks/{name}.json", {"type": "minecraft:block", "pools": [{"rolls": 1, "bonus_rolls": 0,
          "entries": [{"type": "minecraft:item", "name": item}], "conditions": [{"condition": "minecraft:survives_explosion"}]}],
          "random_sequence": f"{NS}:blocks/{name}"})


def particle_model(name, tex):
    model(name, {"textures": {"particle": tex}})


FACING6 = ("facing", ["down", "up", "north", "south", "west", "east"])
FACING4 = ("facing", ["north", "south", "west", "east"])
ENABLED = ("enabled", ["false", "true"])

# ====================================================================== lamps, levitator, ears, relay...
for lamp in ["lamp_arcane", "lamp_growth", "lamp_fertility"]:
    forge_state(lamp, [FACING6, ENABLED], render_type="minecraft:cutout")
loot_self("lamp_arcane")
loot_self("lamp_growth")
loot_self("lamp_fertility")
forge_state("levitator", [FACING6, ENABLED], render_type="minecraft:cutout")
forge_state("arcane_ear", [FACING6, ENABLED], render_type="minecraft:cutout")
forge_state("arcane_ear_toggle", [FACING6, ENABLED], render_type="minecraft:cutout")
forge_state("redstone_relay", [FACING4, ENABLED], render_type="minecraft:cutout")
forge_state("everfull_urn", [], render_type="minecraft:cutout")
forge_state("paving_stone_travel", [])
forge_state("paving_stone_barrier", [])
forge_state("vis_battery", [("charge", [str(i) for i in range(11)])])
forge_state("recharge_pedestal", [], render_type="minecraft:cutout")
forge_state("dioptra", [ENABLED], render_type="minecraft:cutout")
forge_state("vis_generator", [FACING6, ENABLED], render_type="minecraft:cutout")
forge_state("spa", [])
forge_state("potion_sprayer", [FACING6, ENABLED])
forge_state("pattern_crafter", [FACING4, ENABLED], render_type="minecraft:cutout")
forge_state("brain_box", [FACING6], render_type="minecraft:cutout")
forge_state("flesh_block", [])
forge_state("metal_alchemical", [])
forge_state("metal_alchemical_advanced", [])
for kind in ["crate", "urn"]:
    for r in ["common", "uncommon", "rare"]:
        forge_state(f"loot_{kind}_{r}", [], render_type="minecraft:cutout")
for n in ["levitator", "arcane_ear", "arcane_ear_toggle", "redstone_relay", "everfull_urn", "paving_stone_travel", "paving_stone_barrier",
          "vis_battery", "recharge_pedestal", "dioptra", "vis_generator", "spa", "potion_sprayer", "pattern_crafter", "brain_box",
          "flesh_block", "metal_alchemical", "metal_alchemical_advanced", "hungry_chest"]:
    loot_self(n)

# The infernal furnace model draws the whole 3x3x3 structure around the lava core.
forge_state("infernal_furnace", [FACING4], render_type="minecraft:cutout_mipped", item=False)
model("infernal_furnace", {"parent": tc("block/infernal_furnace"), "display": {"gui": {"translation": [0, 0, 0], "scale": [0.275] * 3,
                                                                                       "rotation": [30, 45, 0]}}}, "item")
for ph, tex in [("placeholder_brick", "minecraft:block/nether_bricks"), ("placeholder_obsidian", "minecraft:block/obsidian")]:
    particle_model(ph, tex)
    blockstate(ph, {"variants": {"": {"model": tc("block/" + ph)}}})
loot_item("placeholder_brick", "minecraft:nether_bricks")
loot_item("placeholder_obsidian", "minecraft:obsidian")

# barrier: invisible
particle_model("barrier", "minecraft:block/glass")
blockstate("barrier", {"variants": {"": {"model": tc("block/barrier")}}})

# hungry chest: drawn by its renderer; the item uses the chest-shaped model.
copy_model("hungry_chest", src="chest")
particle_model("hungry_chest_particle", tc("models/chesthungry"))
blockstate("hungry_chest", {"variants": {"": {"model": tc("block/hungry_chest_particle")}}})
item_parent("hungry_chest", tc("block/hungry_chest"))

# ====================================================================== thaumatorium (OBJ)
shutil.copy(f"{ORIG}/models/block/thaumatorium.obj", f"{A}/models/block/thaumatorium.obj")
with open(f"{A}/models/block/thaumatorium.mtl", "w") as f:
    f.write("newmtl texture\nKd 1.0 1.0 1.0\nmap_Kd #texture\n")
model("thaumatorium", {"parent": "minecraft:block/block", "loader": "neoforge:obj", "model": tc("models/block/thaumatorium.obj"),
                       "flip_v": True, "render_type": "minecraft:cutout",
                       "textures": {"texture": tc("blocks/thaumatorium"), "particle": tc("blocks/thaumatorium")}})
TH_Y = {"west": 0, "south": 270, "north": 90, "east": 180}
blockstate("thaumatorium", {"variants": {f"facing={f}": dict({"model": tc("block/thaumatorium"), "x": 90}, **({"y": y} if y else {}))
                                         for f, y in TH_Y.items()}})
particle_model("thaumatorium_top", tc("blocks/metal_alchemical"))
blockstate("thaumatorium_top", {"variants": {"": {"model": tc("block/thaumatorium_top")}}})
loot_item("thaumatorium", tc("metal_alchemical"))
loot_item("thaumatorium_top", tc("metal_alchemical"))

# ====================================================================== mirrors: the frame is a thin plate, the pane is drawn by the renderer
MIRROR_ROT = {"north": {}, "east": {"y": 90}, "south": {"y": 180}, "west": {"y": 270}, "up": {"x": 270}, "down": {"x": 90}}
for name, frame in [("mirror", "mirrorframe"), ("mirror_essentia", "mirrorframe2")]:
    t = tc("blocks/" + frame)
    model(name, {"parent": "minecraft:block/block", "render_type": "minecraft:cutout",
                 "textures": {"frame": t, "particle": t},
                 "elements": [{"from": [0, 0, 15], "to": [16, 16, 16], "faces": {
                     "north": {"uv": [0, 0, 16, 16], "texture": "#frame"}, "south": {"uv": [16, 0, 0, 16], "texture": "#frame"},
                     "up": {"uv": [0, 15, 16, 16], "texture": "#frame"}, "down": {"uv": [0, 0, 16, 1], "texture": "#frame"},
                     "west": {"uv": [15, 0, 16, 16], "texture": "#frame"}, "east": {"uv": [0, 0, 1, 16], "texture": "#frame"}}}]})
    blockstate(name, {"variants": {f"facing={f}": dict({"model": tc("block/" + name)}, **r) for f, r in MIRROR_ROT.items()}})
    pane_on = "mirrorpaneopen"
    generated_item(name + "_on", t, tc("blocks/" + pane_on))
    model(name, {"parent": "minecraft:item/generated", "textures": {"layer0": t, "layer1": tc("blocks/mirrorpane")},
                 "overrides": [{"predicate": {tc("linked"): 1}, "model": tc("item/" + name + "_on")}]}, "item")
    loot_self(name, components=[tc("mirror_link")])
model("hand_mirror", {"parent": "minecraft:item/handheld", "textures": {"layer0": tc("items/hand_mirror")}}, "item")

# ====================================================================== brain in a jar
copy_model("jar_brain_item", extra={"render_type": "minecraft:translucent"})
blockstate("jar_brain", {"variants": {"": {"model": tc("block/jar_normal")}}})
item_parent("jar_brain", tc("block/jar_brain_item"))
loot_self("jar_brain", components=[tc("brain_xp")])
generated_item("brain", tc("items/brain"))

# ====================================================================== banners
particle_model("banner", tc("blocks/base_wood_grain") if os.path.exists(f"{A}/textures/blocks/base_wood_grain.png") else "minecraft:block/oak_planks")
for c in COLORS + ["crimson_cult"]:
    name = f"banner_{c}"
    blockstate(name, {"variants": {"": {"model": tc("block/banner")}}})
    src = json.load(open(f"{ORIG}/models/item/{'banner_crimson_cult' if c == 'crimson_cult' else 'banner'}.json"))
    m = {"parent": "minecraft:item/generated", "textures": src["textures"]}
    if "display" in src:
        m["display"] = src["display"]
    model(name, m, "item")
    loot_self(name, components=[tc("banner_aspect")])

# ====================================================================== loot crates and urns (WeightedRandomLoot.lootBag*)
POTIONS = ["night_vision", "long_night_vision", "invisibility", "long_invisibility", "leaping", "strong_leaping", "long_leaping",
           "fire_resistance", "long_fire_resistance", "swiftness", "strong_swiftness", "long_swiftness", "slowness", "strong_slowness",
           "long_slowness", "water_breathing", "long_water_breathing", "healing", "strong_healing", "harming", "strong_harming",
           "poison", "strong_poison", "long_poison", "regeneration", "strong_regeneration", "long_regeneration", "strength",
           "strong_strength", "long_strength", "weakness", "long_weakness", "luck", "slow_falling", "long_slow_falling"]


def item(name, weight, count=1, functions=None):
    e = {"type": "minecraft:item", "name": name, "weight": weight}
    fs = list(functions or [])
    if count != 1:
        fs.insert(0, {"function": "minecraft:set_count", "count": count})
    if fs:
        e["functions"] = fs
    return e


def bag(rarity):
    r = rarity
    e = [item("minecraft:gold_nugget", [2500, 2250, 2000][r], r + 1),
         item(tc("salis_mundus"), [3, 6, 9][r]),
         item("minecraft:chorus_fruit", 5), item("minecraft:compass", 5), item("minecraft:cookie", 5),
         item("minecraft:diamond", 10 if r == 0 else 50), item("minecraft:emerald", 15 if r == 0 else 75),
         item("minecraft:gold_ingot", 100), item("minecraft:ender_pearl", 100),
         item("minecraft:experience_bottle", [5, 10, 20][r]),
         item("minecraft:enchanted_golden_apple", [1, 2, 3][r]), item("minecraft:golden_apple", [3, 6, 9][r]),
         item("minecraft:book", 10, functions=[{"function": "minecraft:enchant_with_levels",
                                                 "levels": {"type": "minecraft:uniform", "min": 5, "max": int(5 + r * 0.75 * 17)}}])]
    if r == 2:
        e.append(item("minecraft:nether_star", 1))
    if r >= 1:
        e.append(item(tc("amulet_vis_found"), 6))
    baubles = ["amulet_mundane", "ring_mundane", "girdle_mundane", "ring_apprentice", "amulet_fancy", "ring_fancy", "girdle_fancy"]
    for i, (w, rs) in enumerate([(10, [0]), (10, [0]), (10, [0]), (5, [2]), (5, [1]), (5, [1]), (5, [1])]):
        if r in rs:
            e.append(item(tc(baubles[i]), w))
    for p in POTIONS:
        kinds = ["minecraft:potion", "minecraft:splash_potion"] + (["minecraft:lingering_potion"] if r >= 1 else [])
        for k in kinds:
            e.append(item(k, 2, functions=[{"function": "minecraft:set_potion", "id": "minecraft:" + p}]))
    return e


for kind in ["crate", "urn"]:
    for r, rn in enumerate(["common", "uncommon", "rare"]):
        name = f"loot_{kind}_{rn}"
        write(f"{D}/{NS}/loot_table/blocks/{name}.json", {"type": "minecraft:block", "random_sequence": f"{NS}:blocks/{name}", "pools": [
            {"rolls": 1, "bonus_rolls": 0, "entries": [{"type": "minecraft:alternatives", "children": [
                {"type": "minecraft:item", "name": tc(name), "conditions": [{"condition": "minecraft:match_tool", "predicate": {
                    "predicates": {"minecraft:enchantments": [{"enchantments": "minecraft:silk_touch", "levels": {"min": 1}}]}}}]},
                {"type": "minecraft:loot_table", "value": f"{NS}:loot_bag/{rn}",
                 "functions": [{"function": "minecraft:set_count", "count": 1}]}]}]}]})
        # 1 + rarity + rand(3) rolls of the loot bag (only without silk touch: the table above picks one or the other)
        write(f"{D}/{NS}/loot_table/loot_bag/{rn}.json", {"type": "minecraft:generic", "pools": [
            {"rolls": {"type": "minecraft:uniform", "min": 1 + r, "max": 3 + r}, "bonus_rolls": 0, "entries": bag(r)}]})

# ====================================================================== tags
def add_tag(kind, ns, path, values):
    p = f"{D}/{ns}/tags/{kind}/{path}.json"
    d = json.load(open(p)) if os.path.exists(p) else {"replace": False, "values": []}
    for v in values:
        if v not in d["values"]:
            d["values"].append(v)
    write(p, d)


PICKAXE = ["lamp_arcane", "lamp_growth", "lamp_fertility", "everfull_urn", "paving_stone_travel", "paving_stone_barrier", "vis_battery",
           "recharge_pedestal", "dioptra", "spa", "infernal_furnace", "placeholder_brick", "placeholder_obsidian", "metal_alchemical",
           "metal_alchemical_advanced", "thaumatorium", "thaumatorium_top", "brain_box", "potion_sprayer", "pattern_crafter",
           "mirror", "mirror_essentia", "loot_urn_common", "loot_urn_uncommon", "loot_urn_rare"]
AXE = ["levitator", "hungry_chest", "arcane_ear", "arcane_ear_toggle", "vis_generator", "loot_crate_common", "loot_crate_uncommon",
       "loot_crate_rare"] + [f"banner_{c}" for c in COLORS + ["crimson_cult"]]
add_tag("block", "minecraft", "mineable/pickaxe", [tc(n) for n in PICKAXE])
add_tag("block", "minecraft", "mineable/axe", [tc(n) for n in AXE])
add_tag("block", "minecraft", "mineable/hoe", [tc("flesh_block")])
add_tag("block", "minecraft", "beacon_base_blocks", [tc("metal_alchemical"), tc("metal_alchemical_advanced")])

# ====================================================================== lang
L = f"{A}/lang/en_us.json"
en = json.load(open(L, encoding="utf-8"), object_pairs_hook=collections.OrderedDict)
L2 = f"{A}/lang/pt_br.json"
pt = json.load(open(L2, encoding="utf-8"), object_pairs_hook=collections.OrderedDict)
BLOCKS = ["lamp_arcane", "lamp_growth", "lamp_fertility", "levitator", "hungry_chest", "arcane_ear", "arcane_ear_toggle", "redstone_relay",
          "everfull_urn", "paving_stone_travel", "paving_stone_barrier", "barrier", "vis_battery", "recharge_pedestal", "dioptra",
          "vis_generator", "spa", "infernal_furnace", "placeholder_brick", "placeholder_obsidian", "metal_alchemical",
          "metal_alchemical_advanced", "thaumatorium", "thaumatorium_top", "brain_box", "potion_sprayer", "pattern_crafter", "mirror",
          "mirror_essentia", "jar_brain", "flesh_block", "banner_crimson_cult"] + \
         [f"loot_{k}_{r}" for k in ["crate", "urn"] for r in ["common", "uncommon", "rare"]]
for d in (en, pt):
    keys = {f"block.thaumcraft.{b}": f"tile.{b}.name" for b in BLOCKS}
    keys.update({f"block.thaumcraft.banner_{c}": f"tile.banner_{OLD_COLOR.get(c, c)}.name" for c in COLORS})
    keys.update({"item.thaumcraft.hand_mirror": "item.hand_mirror.name", "item.thaumcraft.brain": "item.brain.name"})
    for k, old in keys.items():
        if old not in d:
            raise SystemExit(f"no lang for {k} ({old})")
        d.setdefault(k, d[old].strip())
    for k in ["mirror", "mirror_essentia", "jar_brain"]:
        d.setdefault(f"item.thaumcraft.{k}", d[f"tile.{k}.name"])
    d.setdefault("thaumcraft.levitator.range", d["tc.levitator"].replace("%d", "%s"))
en.setdefault("thaumcraft.mirror.already_linked", "That mirror is already linked to a valid destination.")
pt.setdefault("thaumcraft.mirror.already_linked", "Esse espelho já está ligado a um destino válido.")
json.dump(en, open(L, "w", encoding="utf-8"), indent=1, ensure_ascii=False)
json.dump(pt, open(L2, "w", encoding="utf-8"), indent=1, ensure_ascii=False)
# The infernal furnace model has no particle texture; the hungry chest item model uses a texture outside the atlas folders.
_p = f"{A}/models/block/infernal_furnace.json"
_m = json.load(open(_p))
_m.setdefault("textures", {}).setdefault("particle", "thaumcraft:blocks/infernal_furnace")
write(_p, _m)
_p = f"{RES}/assets/minecraft/atlases/blocks.json"
_atlas = json.load(open(_p)) if os.path.exists(_p) else {"sources": []}
if {"type": "single", "resource": "thaumcraft:models/chesthungry"} not in _atlas["sources"]:
    _atlas["sources"].append({"type": "single", "resource": "thaumcraft:models/chesthungry"})
write(_p, _atlas)
print("ok")
