"""Generates the JSON resources of milestone 10 (tools, armor and baubles/Curios): item models, lang keys, item tags
(curios slots, dyeable, enchantable, tool/armor tags), the Curios player slots, the vanilla crafting recipes of
ConfigRecipes.initializeNormalRecipes (thaumium/void tools and armor, baubles), the infusion recipes whose 1.12 output was
NBT on the central item (fortress goggles and masks, verdant charm types), the runic augment, and the infusion
enchantments carried by the elemental tools and primal crusher results.

Run from the repo root: python3 tools/gen_marco10.py  (after gen_marco8, which re-converts the infusion recipes)
"""
import collections
import json
import os

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..")
RES = os.path.join(ROOT, "mod/src/main/resources")
A = os.path.join(RES, "assets/thaumcraft")
D = os.path.join(RES, "data")
R = os.path.join(D, "thaumcraft/recipe")
ORIG = os.path.join(ROOT, "decompiled/assets/thaumcraft")
NS = "thaumcraft"


def write(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        json.dump(obj, f, indent=2, ensure_ascii=False)
        f.write("\n")


def tc(n):
    return n if ":" in n else f"{NS}:{n}"


def add_tag(kind, ns, path, values):
    p = f"{D}/{ns}/tags/{kind}/{path}.json"
    d = json.load(open(p)) if os.path.exists(p) else {"replace": False, "values": []}
    for v in values:
        if v not in d["values"]:
            d["values"].append(v)
    write(p, d)


THAUMIUM_TOOLS = ["thaumium_axe", "thaumium_sword", "thaumium_shovel", "thaumium_pick", "thaumium_hoe"]
VOID_TOOLS = ["void_axe", "void_sword", "void_shovel", "void_pick", "void_hoe"]
ELEMENTAL_TOOLS = ["elemental_axe", "elemental_sword", "elemental_shovel", "elemental_pick", "elemental_hoe"]
OTHER_TOOLS = ["primal_crusher", "crimson_blade", "resonator", "sanity_checker"]
ARMOR = {
    "head": ["goggles", "thaumium_helm", "fortress_helm", "void_helm", "void_robe_helm", "crimson_plate_helm", "crimson_robe_helm",
             "crimson_praetor_helm"],
    "chest": ["thaumium_chest", "cloth_chest", "fortress_chest", "void_chest", "void_robe_chest", "crimson_plate_chest", "crimson_robe_chest",
              "crimson_praetor_chest"],
    "leg": ["thaumium_legs", "cloth_legs", "fortress_legs", "void_legs", "void_robe_legs", "crimson_plate_legs", "crimson_robe_legs",
            "crimson_praetor_legs"],
    "foot": ["thaumium_boots", "cloth_boots", "traveller_boots", "void_boots", "crimson_boots"],
}
BAUBLES = ["amulet_mundane", "ring_mundane", "girdle_mundane", "ring_apprentice", "amulet_fancy", "ring_fancy", "girdle_fancy"]
CURIOS = {"necklace": ["amulet_mundane", "amulet_fancy", "amulet_vis_found", "amulet_vis_crafted"],
          "ring": ["ring_mundane", "ring_apprentice", "ring_fancy", "cloud_ring"],
          "belt": ["girdle_mundane", "girdle_fancy"],
          "charm": ["verdant_charm", "voidseer_charm", "charm_undying"],
          "head": ["goggles", "curiosity_band"]}
ALL_ARMOR = [i for v in ARMOR.values() for i in v]
ALL = THAUMIUM_TOOLS + VOID_TOOLS + ELEMENTAL_TOOLS + OTHER_TOOLS + ALL_ARMOR + BAUBLES + [
    "amulet_vis_found", "amulet_vis_crafted", "verdant_charm", "curiosity_band", "voidseer_charm", "cloud_ring", "charm_undying"]

# ====================================================================== item models
MODELS = f"{A}/models/item"


def item_model(name, obj):
    write(f"{MODELS}/{name}.json", obj)


def generated(*layers, parent="minecraft:item/generated"):
    return {"parent": parent, "textures": {f"layer{i}": tc(l) for i, l in enumerate(layers)}}


for name in ALL:
    src = f"{ORIG}/models/item/{name}.json"
    if name in BAUBLES:
        item_model(name, generated(f"items/{name}"))
    elif name == "amulet_vis_found":
        item_model(name, generated("items/amulet_vis_stone"))
    elif name == "amulet_vis_crafted":
        item_model(name, generated("items/amulet_vis"))
    elif name == "verdant_charm":
        for t in range(3):
            m = json.load(open(f"{ORIG}/models/item/verdant_charm_{t}.json"))
            item_model(f"verdant_charm_{t}", generated(*[m["textures"][k] for k in sorted(m["textures"])]))
        base = generated("items/verdant_charm", "items/verdant_charm_over_0")
        base["overrides"] = [{"predicate": {"thaumcraft:type": t}, "model": tc(f"item/verdant_charm_{t}")} for t in (1, 2)]
        item_model(name, base)
    else:
        m = json.load(open(src))
        parent = m.get("parent", "item/generated")
        parent = parent if ":" in parent else "minecraft:" + parent
        item_model(name, {"parent": parent, "textures": {k: tc(v) for k, v in m["textures"].items()}})
# The textures worn on the face (IRenderBauble), rendered as extruded sprites by the curio renderer.
item_model("goggles_bauble", generated("items/goggles_bauble"))
item_model("curiosity_band_worn", generated("items/curiosity_band_worn"))

# ====================================================================== tags
add_tag("item", "minecraft", "swords", [tc(n) for n in ["thaumium_sword", "void_sword", "elemental_sword", "crimson_blade"]])
add_tag("item", "minecraft", "axes", [tc(n) for n in ["thaumium_axe", "void_axe", "elemental_axe"]])
add_tag("item", "minecraft", "pickaxes", [tc(n) for n in ["thaumium_pick", "void_pick", "elemental_pick", "primal_crusher"]])
add_tag("item", "minecraft", "shovels", [tc(n) for n in ["thaumium_shovel", "void_shovel", "elemental_shovel", "primal_crusher"]])
add_tag("item", "minecraft", "hoes", [tc(n) for n in ["thaumium_hoe", "void_hoe", "elemental_hoe"]])
for slot, items in ARMOR.items():
    add_tag("item", "minecraft", f"{slot}_armor", [tc(n) for n in items])
    add_tag("item", "minecraft", f"enchantable/{slot}_armor", [tc(n) for n in items])
add_tag("item", "minecraft", "enchantable/armor", [tc(n) for n in ALL_ARMOR])
add_tag("item", "minecraft", "enchantable/equippable", [tc(n) for n in ALL_ARMOR])
TOOLS = THAUMIUM_TOOLS + VOID_TOOLS + ELEMENTAL_TOOLS + ["primal_crusher", "crimson_blade"]
SWORDS = ["thaumium_sword", "void_sword", "elemental_sword", "crimson_blade"]
DIGGERS = [t for t in TOOLS if t not in SWORDS]
add_tag("item", "minecraft", "enchantable/durability", [tc(n) for n in TOOLS + ALL_ARMOR])
add_tag("item", "minecraft", "enchantable/mining", [tc(n) for n in DIGGERS])
add_tag("item", "minecraft", "enchantable/mining_loot", [tc(n) for n in DIGGERS])
add_tag("item", "minecraft", "enchantable/sword", [tc(n) for n in SWORDS])
add_tag("item", "minecraft", "enchantable/sharp_weapon", [tc(n) for n in SWORDS + ["thaumium_axe", "void_axe", "elemental_axe"]])
add_tag("item", "minecraft", "enchantable/weapon", [tc(n) for n in SWORDS + ["thaumium_axe", "void_axe", "elemental_axe"]])
add_tag("item", "minecraft", "enchantable/fire_aspect", [tc(n) for n in SWORDS])
add_tag("item", "minecraft", "dyeable", [tc(n) for n in ["cloth_chest", "cloth_legs", "cloth_boots", "void_robe_helm", "void_robe_chest",
                                                          "void_robe_legs"]])
add_tag("item", "c", "tools/melee_weapon", [tc(n) for n in SWORDS])
add_tag("item", "c", "tools/mining_tool", [tc(n) for n in DIGGERS])
add_tag("item", "c", "tools", [tc(n) for n in TOOLS])
add_tag("item", "c", "armors", [tc(n) for n in ALL_ARMOR])
for slot, items in CURIOS.items():
    add_tag("item", "curios", slot, [tc(n) for n in items])
# ItemPrimalCrusher: a pickaxe and a shovel at once.
add_tag("block", NS, "mineable/primal_crusher", ["#minecraft:mineable/pickaxe", "#minecraft:mineable/shovel"])

# Curios: the slots the 1.12 Baubles inventory had (amulet, two rings, belt, head, charm).
write(f"{D}/{NS}/curios/entities/thaumcraft_player.json", {"entities": ["player"], "slots": ["necklace", "ring", "belt", "charm", "head"]})
write(f"{D}/{NS}/curios/slots/ring.json", {"size": 2})

# ====================================================================== vanilla crafting (initializeNormalRecipes)


def cond(*items):
    return {"neoforge:conditions": [{"type": "neoforge:item_exists", "item": tc(i)} for i in items]}


def shaped(name, group, result, pattern, key, category="equipment"):
    items = [result] + [v["item"] for v in key.values() if "item" in v and v["item"].startswith(NS)]
    write(f"{R}/{name}.json", {**cond(*items), "type": "minecraft:crafting_shaped", "group": f"{NS}:{group}", "category": category,
                               "pattern": pattern, "key": key, "result": {"id": tc(result)}})


STICK = {"tag": "c:rods/wooden"}
for metal, group, prefix in (("thaumium", "thaumium_stuff", "thaumium"), ("void", "void_stuff", "void")):
    I = {"tag": f"c:ingots/{metal}"}
    shaped(f"{prefix}helm", group, f"{prefix}_helm", ["III", "I I"], {"I": I})
    shaped(f"{prefix}chest", group, f"{prefix}_chest", ["I I", "III", "III"], {"I": I})
    shaped(f"{prefix}legs", group, f"{prefix}_legs", ["III", "I I", "I I"], {"I": I})
    shaped(f"{prefix}boots", group, f"{prefix}_boots", ["I I", "I I"], {"I": I})
    shaped(f"{prefix}shovel", group, f"{prefix}_shovel", ["I", "S", "S"], {"I": I, "S": STICK})
    shaped(f"{prefix}pick", group, f"{prefix}_pick", ["III", " S ", " S "], {"I": I, "S": STICK})
    shaped(f"{prefix}axe", group, f"{prefix}_axe", ["II", "SI", "S "], {"I": I, "S": STICK})
    shaped(f"{prefix}hoe", group, f"{prefix}_hoe", ["II", "S ", "S "], {"I": I, "S": STICK})
    shaped(f"{prefix}sword", group, f"{prefix}_sword", ["I", "I", "S"], {"I": I, "S": STICK})
STRING, LEATHER, DIAMOND = {"tag": "c:strings"}, {"item": "minecraft:leather"}, {"tag": "c:gems/diamond"}
shaped("babuleamulet", "baubles_stuff", "amulet_mundane", [" S ", "S S", " I "], {"S": STRING, "I": {"tag": "c:ingots/brass"}})
shaped("babulering", "baubles_stuff", "ring_mundane", ["NNN", "N N", "NNN"], {"N": {"tag": "c:nuggets/brass"}})
shaped("babulegirdle", "baubles_stuff", "girdle_mundane", [" L ", "L L", " I "], {"L": LEATHER, "I": {"tag": "c:ingots/brass"}})
shaped("babuleamuletfancy", "baubles_stuff", "amulet_fancy", [" S ", "SGS", " I "], {"S": STRING, "G": DIAMOND, "I": {"tag": "c:ingots/gold"}})
shaped("babuleringfancy", "baubles_stuff", "ring_fancy", ["NGN", "N N", "NNN"], {"G": DIAMOND, "N": {"tag": "c:nuggets/gold"}})
shaped("babulegirdlefancy", "baubles_stuff", "girdle_fancy", [" L ", "LGL", " I "], {"L": LEATHER, "G": DIAMOND, "I": {"tag": "c:ingots/gold"}})
# RecipesRobeArmorDyes / RecipesVoidRobeArmorDyes: the robes are in minecraft:dyeable, so vanilla's armor dye recipe covers them.

# ====================================================================== infusion recipes with NBT outputs


def crystal(aspect):
    return {"type": "neoforge:components", "items": tc("crystal_essence"), "components": {"thaumcraft:essentia": {aspect: 1}}, "strict": False}


def potion(p):
    return {"type": "neoforge:components", "items": "minecraft:potion", "components": {"minecraft:potion_contents": {"potion": p}}, "strict": False}


def infusion_data(name, research, instability, aspects, central, components, patch, needs):
    write(f"{R}/{name}.json", {**cond(*needs), "type": tc("infusion_data"), "research": research, "instability": instability,
                               "aspects": aspects, "central": {"item": tc(central)}, "components": components, "patch": patch})


PLATE_IRON = {"tag": "c:plates/iron"}
infusion_data("helmgoggles", "FORTRESSMASK", 5, {"sensus": 40, "auram": 20, "praemunio": 20}, "fortress_helm",
              [{"item": "minecraft:slime_ball"}, {"item": tc("goggles")}], {"thaumcraft:goggles": {}}, ["fortress_helm", "goggles", "infusion_matrix"])
infusion_data("maskgrinningdevil", "FORTRESSMASK", 8, {"cognitio": 80, "victus": 80, "praemunio": 20}, "fortress_helm",
              [{"tag": "c:dyes/black"}, PLATE_IRON, LEATHER, {"item": tc("shimmerleaf")}, {"item": tc("brain")}, PLATE_IRON],
              {"thaumcraft:mask": 0}, ["fortress_helm", "shimmerleaf", "brain", "infusion_matrix"])
infusion_data("maskangryghost", "FORTRESSMASK", 8, {"perditio": 80, "mortuus": 80, "praemunio": 20}, "fortress_helm",
              [{"tag": "c:dyes/white"}, PLATE_IRON, LEATHER, {"item": "minecraft:poisonous_potato"}, {"item": "minecraft:wither_skeleton_skull"},
               PLATE_IRON], {"thaumcraft:mask": 1}, ["fortress_helm", "infusion_matrix"])
infusion_data("masksippingfiend", "FORTRESSMASK", 8, {"exanimis": 80, "victus": 80, "praemunio": 20}, "fortress_helm",
              [{"tag": "c:dyes/red"}, PLATE_IRON, LEATHER, {"item": "minecraft:ghast_tear"}, {"item": "minecraft:milk_bucket"}, PLATE_IRON],
              {"thaumcraft:mask": 2}, ["fortress_helm", "infusion_matrix"])
infusion_data("verdantheartlife", "VERDANTCHARMS", 5, {"victus": 80, "humanus": 80}, "verdant_charm",
              [{"item": "minecraft:golden_apple"}, crystal("victus"), potion("minecraft:strong_healing"), crystal("humanus")],
              {"thaumcraft:verdant_type": 1}, ["verdant_charm", "crystal_essence", "infusion_matrix"])
infusion_data("verdantheartsustain", "VERDANTCHARMS", 5, {"desiderium": 80, "aer": 80}, "verdant_charm",
              [{"item": tc("triple_meat_treat")}, crystal("desiderium"), potion("minecraft:strong_regeneration"), crystal("aer")],
              {"thaumcraft:verdant_type": 2}, ["verdant_charm", "triple_meat_treat", "crystal_essence", "infusion_matrix"])
# InfusionRunicAugmentRecipe: one more point of runic shielding on any armor piece or curio.
write(f"{R}/runicarmor.json", {**cond("infusion_matrix", "salis_mundus"), "type": tc("infusion_runic_augment"), "research": "RUNICSHIELDING"})

# The elemental tools and primal crusher come out of the altar already carrying infusion enchantments.
RESULT_ENCH = {"elementalaxe": {"collector": 1, "burrowing": 1}, "elementalpick": {"refining": 1, "sounding": 2},
               "elementalsword": {"arcing": 2}, "elementalshovel": {"destructive": 1}, "primalcrusher": {"destructive": 1, "refining": 1}}
for rid, ench in RESULT_ENCH.items():
    p = f"{R}/{rid}.json"
    d = json.load(open(p), object_pairs_hook=collections.OrderedDict)
    d["result"]["components"] = {"thaumcraft:infusion_enchantments": ench}
    write(p, d)

# ====================================================================== lang
L = f"{A}/lang/en_us.json"
en = json.load(open(L, encoding="utf-8"), object_pairs_hook=collections.OrderedDict)
L2 = f"{A}/lang/pt_br.json"
pt = json.load(open(L2, encoding="utf-8"), object_pairs_hook=collections.OrderedDict)
OLD = {n: f"item.baubles.{n}.name" for n in BAUBLES}
OLD.update({"amulet_vis_found": "item.amulet_vis.found.name", "amulet_vis_crafted": "item.amulet_vis.crafted.name"})
for d in (en, pt):
    for n in ALL:
        k = f"item.{NS}.{n}"
        old = OLD.get(n, f"item.{n}.name")
        if k not in d:
            if old not in d:
                raise SystemExit(f"no lang for {n} ({old})")
            d[k] = d[old]
json.dump(en, open(L, "w", encoding="utf-8"), indent=1, ensure_ascii=False)
json.dump(pt, open(L2, "w", encoding="utf-8"), indent=1, ensure_ascii=False)
print("ok")
