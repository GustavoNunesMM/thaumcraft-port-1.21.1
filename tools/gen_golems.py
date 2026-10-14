"""Generates the resources of the golems milestone (Milestone 11): the golem and golem press OBJ models (copied from the
1.12 assets for the entity and block entity renderers), item models for the golem, the bell and the seals, the press
and placeholder blockstates, their loot tables and mineable tags, the golemancer's bell recipe, the golem's entity
aspects and the en_us/pt_br lang keys (copied from the old item.*.name keys, plus a few new ones).

Merge-updates the shared files (lang, tags, entity aspects), so it can be re-run after merges.
Run from the repo root: python3 tools/gen_golems.py
"""
import collections
import glob
import json
import os
import shutil

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..")
RES = os.path.join(ROOT, "mod/src/main/resources")
A = os.path.join(RES, "assets/thaumcraft")
D = os.path.join(RES, "data")
ORIG = os.path.join(ROOT, "decompiled/assets/thaumcraft")
NS = "thaumcraft"

SEALS = ["pickup", "pickup_advanced", "fill", "fill_advanced", "empty", "empty_advanced", "harvest", "butcher", "guard",
         "guard_advanced", "lumber", "breaker", "use", "provider", "stock", "breaker_advanced"]
PLACEHOLDERS = {"placeholder_bars": "minecraft:iron_bars", "placeholder_anvil": "minecraft:anvil",
                "placeholder_cauldron": "minecraft:cauldron", "placeholder_table": "thaumcraft:table_stone"}


def write(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        json.dump(obj, f, indent=2, ensure_ascii=False)
        f.write("\n")


def tc(n):
    return n if ":" in n else f"{NS}:{n}"


def check_tex(t):
    ns, path = t.split(":")
    if ns == NS and not os.path.exists(f"{A}/textures/{path}.png"):
        raise SystemExit(f"missing texture {t}")
    return t


def model(name, obj, kind="block"):
    write(f"{A}/models/{kind}/{name}.json", obj)


def generated_item(name, *layers, parent="minecraft:item/generated"):
    model(name, {"parent": parent, "textures": {f"layer{i}": check_tex(l) for i, l in enumerate(layers)}}, "item")


# ====================================================================== OBJ models (drawn by GolemRenderer / GolemBuilderRenderer)
os.makedirs(f"{A}/models/obj", exist_ok=True)
for src in sorted(glob.glob(f"{ORIG}/models/obj/golem_*.obj")):
    shutil.copyfile(src, f"{A}/models/obj/{os.path.basename(src)}")
for ext in ["obj", "mtl"]:
    shutil.copyfile(f"{ORIG}/models/block/golembuilder.{ext}", f"{A}/models/block/golembuilder.{ext}")

# ====================================================================== item models
generated_item("golem", tc("items/golem"))
generated_item("golem_bell", tc("items/golem_bell"), parent="minecraft:item/handheld")
generated_item("seal_blank", tc("items/seals/seal_blank"))
for s in SEALS:
    generated_item(f"seal_{s}", tc(f"items/seals/seal_{s}"))

# ====================================================================== blockstates (the press and the placeholders are drawn
# by the press renderer; their models only give the break particles)
model("golem_builder", {"textures": {"particle": check_tex(tc("blocks/base_stone"))}})
write(f"{A}/blockstates/golem_builder.json", {"variants": {"": {"model": tc("block/golem_builder")}}})
for p in PLACEHOLDERS:
    model(p, {"textures": {"particle": check_tex(tc("blocks/base_metal"))}})
    write(f"{A}/blockstates/{p}.json", {"variants": {"": {"model": tc(f"block/{p}")}}})


# ====================================================================== loot tables: the press drops its piston, the
# placeholders the block they stand for (BlockGolemBuilder/BlockPlaceholder.getItemDropped)
def loot_item(block, item):
    write(f"{D}/{NS}/loot_table/blocks/{block}.json", {
        "type": "minecraft:block",
        "pools": [{"rolls": 1, "bonus_rolls": 0, "entries": [{"type": "minecraft:item", "name": item}],
                   "conditions": [{"condition": "minecraft:survives_explosion"}]}],
        "random_sequence": f"{NS}:blocks/{block}"})


loot_item("golem_builder", "minecraft:piston")
for p, item in PLACEHOLDERS.items():
    loot_item(p, item)


# ====================================================================== tags
def add_tag(kind, ns, path, values):
    p = f"{D}/{ns}/tags/{kind}/{path}.json"
    d = json.load(open(p)) if os.path.exists(p) else {"replace": False, "values": []}
    for v in values:
        if v not in d["values"]:
            d["values"].append(v)
    write(p, d)


add_tag("block", "minecraft", "mineable/pickaxe", [tc(n) for n in ["golem_builder"] + list(PLACEHOLDERS)])

# ====================================================================== recipes
# ConfigRecipes: oreDictRecipe("GolemBell", " QQ", " QQ", "S  ") with sticks and quartz.
write(f"{D}/{NS}/recipe/golembell.json", {
    "type": "minecraft:crafting_shaped", "category": "misc",
    "pattern": [" QQ", " QQ", "S  "],
    "key": {"S": {"tag": "c:rods/wooden"}, "Q": {"tag": "c:gems/quartz"}},
    "result": {"id": tc("golem_bell"), "count": 1}})

# ====================================================================== entity aspects (ConfigAspects: Thaumcraft.Golem)
EA = f"{D}/{NS}/{NS}/aspects/entities/thaumcraft.json"
ea = json.load(open(EA, encoding="utf-8")) if os.path.exists(EA) else {"entries": []}
if not any(e.get("entity") == tc("golem") for e in ea["entries"]):
    ea["entries"].append({"entity": tc("golem"), "aspects": {"machina": 10, "humanus": 10, "motus": 10}})
json.dump(ea, open(EA, "w", encoding="utf-8"), indent=1, ensure_ascii=False)

# ====================================================================== lang
L = f"{A}/lang/en_us.json"
en = json.load(open(L, encoding="utf-8"), object_pairs_hook=collections.OrderedDict)
L2 = f"{A}/lang/pt_br.json"
pt = json.load(open(L2, encoding="utf-8"), object_pairs_hook=collections.OrderedDict)
for d in (en, pt):
    keys = {"item.thaumcraft.golem": "item.golem.name", "item.thaumcraft.golem_bell": "item.golem_bell.name",
            "item.thaumcraft.seal_blank": "item.seal.blank.name", "block.thaumcraft.golem_builder": "tile.golem_builder.name",
            "entity.thaumcraft.golem": "item.golem.name"}
    keys.update({f"item.thaumcraft.seal_{s}": f"item.seal.{s}.name" for s in SEALS})
    keys.update({f"block.thaumcraft.{p}": f"tile.{p}.name" for p in PLACEHOLDERS})
    for k, old in keys.items():
        if old not in d:
            raise SystemExit(f"no lang for {k} ({old})")
        d.setdefault(k, d[old].strip())
NEW = {
    "tc.logistics": ("Golem Logistics", "Logística dos Golems"),
    "entity.thaumcraft.golem_dart": ("Golem Dart", "Dardo de Golem"),
}
for k, (e, p) in NEW.items():
    en.setdefault(k, e)
    pt.setdefault(k, p)
# Keys the golem code and screens use: they must exist (copied from 1.12) in both languages.
USED = ["golem.prop.color", "golem.prop.colorall", "golem.prop.priority", "golem.prop.owner", "golem.prop.lock", "golem.prop.unlock",
        "golem.prop.redon", "golem.prop.redoff", "tc.logistics.search", "tc.logistics.request", "golem.rank", "tc.notowned",
        "golem.follow", "golem.stay", "button.bl", "button.wl", "button.caption.x", "button.caption.y", "button.caption.z",
        "button.caption.required", "button.caption.forbidden"] + [f"button.category.{i}" for i in range(5)]
# ...and the seal toggle names, read from the seal sources.
import re
for src in glob.glob(f"{ROOT}/mod/src/main/java/thaumcraft/common/golems/seals/*.java"):
    USED += re.findall(r'"(golem\.prop\.[a-z.]+)"', open(src, encoding="utf-8").read())
for k in USED:
    for name, d in (("en_us", en), ("pt_br", pt)):
        if k not in d:
            raise SystemExit(f"{name} lacks {k}")
json.dump(en, open(L, "w", encoding="utf-8"), indent=1, ensure_ascii=False)
json.dump(pt, open(L2, "w", encoding="utf-8"), indent=1, ensure_ascii=False)
print("ok")
