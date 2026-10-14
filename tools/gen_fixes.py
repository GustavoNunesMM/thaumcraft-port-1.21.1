"""Fixes found by the post-milestone audit, kept as a generator so re-running the other scripts doesn't lose them:
recipe groups the Thaumonomicon looks up (brass_stuff, tallowcandles, nitorgroup...), the missing ConfigRecipes recipes
(nitor dyes, copper purification, arcane ear toggle, flesh block), and the resources of the late additions (grapple gun,
activator rail, creative flux sponge, thaumatorium item).

Shared files (lang, tags) are merge-updated. Run it LAST, after every other gen script (... gen_marco14.py).

Run from the repo root: python3 tools/gen_fixes.py
"""
import collections
import json
import os

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..")
RES = os.path.join(ROOT, "mod/src/main/resources")
A = os.path.join(RES, "assets/thaumcraft")
D = os.path.join(RES, "data")
R = os.path.join(D, "thaumcraft/recipe")
NS = "thaumcraft"

COLORS = ["white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray", "light_gray", "cyan", "purple",
          "blue", "brown", "green", "red", "black"]


def write(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        json.dump(obj, f, indent=2, ensure_ascii=False)
        f.write("\n")


def cond(*items):
    return {"neoforge:conditions": [{"type": "neoforge:item_exists", "item": i} for i in items]}


# ------------------------------------------------------------------ recipe groups (ConfigRecipes ResourceLocations)

GROUPS = {
    "brass_stuff": ["brassingotstoblock", "brassblocktoingots", "brassplate"],
    "thaumium_stuff": ["thaumiumingotstoblock", "thaumiumblocktoingots", "thaumiumplate"],
    "void_stuff": ["voidingotstoblock", "voidblocktoingots", "voidplate"],
    "tallowcandles": ["tallowcandle"] + [f"tallowcandle{c}" for c in COLORS],
    "jarlabels": ["jarlabel", "jarlabelnull"],
    "inkwell": ["scribingtoolscraft1"],
}


def set_group(name, group):
    p = f"{R}/{name}.json"
    if not os.path.exists(p):
        print("gen_fixes: no recipe", name)
        return
    js = json.load(open(p))
    out = {}
    for k, v in js.items():
        if k == "group":
            continue
        out[k] = v
        if k == "type":
            out["group"] = f"{NS}:{group}"
    write(p, out)


for group, names in GROUPS.items():
    for n in names:
        set_group(n, group)

# ------------------------------------------------------------------ missing recipes

# NitorDye<color> (group nitorgroup): any nitor + a dye.
for c in COLORS:
    write(f"{R}/nitordye{c}.json", {**cond(f"{NS}:nitor_{c}"), "type": "minecraft:crafting_shapeless", "group": f"{NS}:nitorgroup",
                                    "category": "building", "ingredients": [{"tag": f"c:dyes/{c}"}, {"tag": f"{NS}:nitor"}],
                                    "result": {"id": f"{NS}:nitor_{c}"}})

# metal_purification_copper (ModConfig.foundCopperOre is always true now: vanilla has copper ore).
write(f"{R}/metal_purification_copper.json", {**cond(f"{NS}:cluster_copper"), "type": "thaumcraft:crucible", "research": "METALPURIFICATION",
                                              "catalyst": {"tag": "c:ores/copper"}, "aspects": {"metallum": 5, "ordo": 5},
                                              "result": {"id": f"{NS}:cluster_copper"}})

# Tin, silver and lead: only when some mod adds those ores (ModConfig.foundTinOre...).
for metal in ("tin", "silver", "lead"):
    write(f"{R}/metal_purification_{metal}.json", {
        "neoforge:conditions": [{"type": "neoforge:item_exists", "item": f"{NS}:cluster_{metal}"},
                                {"type": "neoforge:not", "value": {"type": "neoforge:tag_empty", "tag": f"c:ores/{metal}"}}],
        "type": "thaumcraft:crucible", "research": "METALPURIFICATION", "catalyst": {"tag": f"c:ores/{metal}"},
        "aspects": {"metallum": 5, "ordo": 5}, "result": {"id": f"{NS}:cluster_{metal}"}})

write(f"{R}/arcaneeartoggle.json", {**cond(f"{NS}:arcane_ear", f"{NS}:arcane_ear_toggle"), "type": "minecraft:crafting_shapeless",
                                    "category": "redstone", "ingredients": [{"item": f"{NS}:arcane_ear"}, {"item": "minecraft:lever"}],
                                    "result": {"id": f"{NS}:arcane_ear_toggle"}})

write(f"{R}/fleshtoblock.json", {**cond(f"{NS}:flesh_block"), "type": "minecraft:crafting_shaped", "category": "building",
                                 "pattern": ["###", "###", "###"], "key": {"#": {"item": "minecraft:rotten_flesh"}},
                                 "result": {"id": f"{NS}:flesh_block"}})
write(f"{R}/blocktoflesh.json", {**cond(f"{NS}:flesh_block"), "type": "minecraft:crafting_shaped", "category": "misc",
                                 "pattern": ["#"], "key": {"#": {"item": f"{NS}:flesh_block"}},
                                 "result": {"id": "minecraft:rotten_flesh", "count": 9}})

# ------------------------------------------------------------------ late additions: models, blockstates, loot, tags

ORIG = os.path.join(ROOT, "decompiled/assets/thaumcraft")


def model(name, obj, kind="block"):
    write(f"{A}/models/{kind}/{name}.json", obj)


def add_tag(kind, ns, path, values):
    p = f"{D}/{ns}/tags/{kind}/{path}.json"
    d = json.load(open(p)) if os.path.exists(p) else {"replace": False, "values": []}
    for v in values:
        if v not in d["values"]:
            d["values"].append(v)
    write(p, d)


def generated(name, tex):
    model(name, {"parent": "minecraft:item/generated", "textures": {"layer0": tex}}, "item")


# Arcane grappler: the gun's two OPL models (with/without its head) picked by the thaumcraft:type property.
for n in ("grapple_gun_1", "grapple_gun_2"):
    js = json.load(open(f"{ORIG}/models/item/{n}.json"))
    js.pop("__createdwith", None)
    js["textures"]["particle"] = js["textures"]["side"]
    model(n, js, "item")
model("grapple_gun", {"parent": f"{NS}:item/grapple_gun_1",
                      "overrides": [{"predicate": {f"{NS}:type": 1}, "model": f"{NS}:item/grapple_gun_2"}]}, "item")
generated("grapple_gun_tip", f"{NS}:items/grapple_gun_tip")
generated("grapple_gun_spool", f"{NS}:items/grapple_gun_spool")
generated("creative_flux_sponge", f"{NS}:items/flux_sponge")
for metal in ("tin", "silver", "lead"):
    generated(f"nugget_{metal}", f"{NS}:items/nugget_{metal}")
    generated(f"cluster_{metal}", f"{NS}:items/cluster_{metal}")
    add_tag("item", "c", f"nuggets/{metal}", [f"{NS}:nugget_{metal}"])
    add_tag("item", "c", "nuggets", [f"{NS}:nugget_{metal}"])

# Arcane activator rail (a powered rail with its own texture).
for powered, tex in ((False, "rail_activator"), (True, "rail_activator_powered")):
    pre = "activator_rail_active" if powered else "activator_rail"
    for shape, parent in (("flat", "rail_flat"), ("raised_ne", "template_rail_raised_ne"), ("raised_sw", "template_rail_raised_sw")):
        model(f"{pre}_{shape}", {"parent": f"minecraft:block/{parent}", "render_type": "minecraft:cutout",
                                 "textures": {"rail": f"{NS}:blocks/{tex}"}})
variants = {}
for powered in ("false", "true"):
    pre = f"{NS}:block/activator_rail" + ("_active" if powered == "true" else "")
    for shape, (m, y) in {"north_south": ("flat", 0), "east_west": ("flat", 90), "ascending_east": ("raised_ne", 90),
                          "ascending_west": ("raised_sw", 90), "ascending_north": ("raised_ne", 0),
                          "ascending_south": ("raised_sw", 0)}.items():
        v = {"model": f"{pre}_{m}"}
        if y:
            v["y"] = y
        variants[f"powered={powered},shape={shape}"] = v
write(f"{A}/blockstates/activator_rail.json", {"variants": variants})
generated("activator_rail", f"{NS}:blocks/rail_activator")
write(f"{D}/{NS}/loot_table/blocks/activator_rail.json", {
    "type": "minecraft:block",
    "pools": [{"rolls": 1, "bonus_rolls": 0, "entries": [{"type": "minecraft:item", "name": f"{NS}:activator_rail"}],
               "conditions": [{"condition": "minecraft:survives_explosion"}]}],
    "random_sequence": f"{NS}:blocks/activator_rail"})
add_tag("block", "minecraft", "rails", [f"{NS}:activator_rail"])
add_tag("item", "minecraft", "rails", [f"{NS}:activator_rail"])
add_tag("block", "minecraft", "mineable/pickaxe", [f"{NS}:activator_rail"])

# Thaumatorium item (the OBJ model lies on its side: BlockThaumatorium's "inventory" transform).
model("thaumatorium", {"parent": f"{NS}:block/thaumatorium", "display": {
    "gui": {"rotation": [300, 0, 45], "translation": [0, -4, 0], "scale": [0.65, 0.65, 0.65]},
    "ground": {"rotation": [90, 0, 0], "translation": [0, 3, 0], "scale": [0.25, 0.25, 0.25]},
    "fixed": {"rotation": [90, 0, 0], "scale": [0.5, 0.5, 0.5]},
    "thirdperson_righthand": {"rotation": [165, 45, 0], "translation": [0, 2.5, 0], "scale": [0.375, 0.375, 0.375]},
    "firstperson_righthand": {"rotation": [90, 45, 0], "scale": [0.4, 0.4, 0.4]}}}, "item")

# ------------------------------------------------------------------ lang

L = f"{A}/lang/en_us.json"
L2 = f"{A}/lang/pt_br.json"
en = json.load(open(L, encoding="utf-8"), object_pairs_hook=collections.OrderedDict)
pt = json.load(open(L2, encoding="utf-8"), object_pairs_hook=collections.OrderedDict)
NEW = {
    "entity.thaumcraft.grapple": ("Grapple", "Gancho"),
    "item.thaumcraft.creative_flux_sponge.tip1": ("Right-click to drain all", "Clique direito para drenar todo o"),
    "item.thaumcraft.creative_flux_sponge.tip2": ("flux from 9x9 chunk area", "fluxo de uma área de 9x9 chunks"),
    "item.thaumcraft.creative_flux_sponge.tip3": ("Also removes flux rifts", "Também remove fendas de fluxo"),
    "item.thaumcraft.creative_flux_sponge.tip4": ("if used while sneaking.", "se usada agachado."),
    "item.thaumcraft.creative_flux_sponge.tip5": ("Creative only", "Somente no criativo"),
    "item.thaumcraft.creative_flux_sponge.drained": ("%s flux drained from 81 chunks.", "%s de fluxo drenado de 81 chunks."),
    "item.thaumcraft.creative_flux_sponge.rifts": ("%s flux rifts removed.", "%s fendas de fluxo removidas."),
}
EXTRA_LANG = {}
NEW.update(EXTRA_LANG)
for metal in ("tin", "silver", "lead"):
    for kind in ("nugget", "cluster"):
        for dct in (en, pt):
            dct[f"item.thaumcraft.{kind}_{metal}"] = dct[f"item.{kind}.{metal}.name"]
for k, (e_, p_) in NEW.items():
    en[k] = e_
    pt[k] = p_
json.dump(en, open(L, "w", encoding="utf-8"), indent=1, ensure_ascii=False)
json.dump(pt, open(L2, "w", encoding="utf-8"), indent=1, ensure_ascii=False)

print("gen_fixes: done")
