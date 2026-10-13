"""Generates the JSON resources of milestone 4 (research: tables, research table, scribing tools, curios, notes).

Run from the repo root: python3 tools/gen_marco4.py
"""
import json, os, collections

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
    return f"{NS}:{n}"


def loot_self(name):
    write(f"{D}/{NS}/loot_table/blocks/{name}.json", {"type": "minecraft:block", "pools": [{"rolls": 1, "bonus_rolls": 0,
          "entries": [{"type": "minecraft:item", "name": tc(name)}], "conditions": [{"condition": "minecraft:survives_explosion"}]}],
          "random_sequence": f"{NS}:blocks/{name}"})


def generated(name, *layers):
    write(f"{A}/models/item/{name}.json", {"parent": "minecraft:item/generated",
                                          "textures": {f"layer{i}": l for i, l in enumerate(layers)}})


# Tables: the original element models, with a parent for the item/display transforms.
for name in ["table_wood", "table_stone", "research_table"]:
    m = json.load(open(f"{ORIG}/models/block/{name}.json"))
    m = {"parent": "minecraft:block/block", **m}
    write(f"{A}/models/block/{name}.json", m)
    write(f"{A}/models/item/{name}.json", {"parent": tc("block/" + name)})
    loot_self(name)
write(f"{A}/blockstates/table_wood.json", {"variants": {"": {"model": tc("block/table_wood")}}})
write(f"{A}/blockstates/table_stone.json", {"variants": {"": {"model": tc("block/table_stone")}}})
write(f"{A}/blockstates/research_table.json", {"variants": {
    "facing=north": {"model": tc("block/research_table")}, "facing=east": {"model": tc("block/research_table"), "y": 90},
    "facing=south": {"model": tc("block/research_table"), "y": 180}, "facing=west": {"model": tc("block/research_table"), "y": 270}}})

generated("scribing_tools", tc("items/scribing_tools"))
for v in ["arcane", "preserved", "ancient", "eldritch", "knowledge", "twisted"]:
    generated(f"curio_{v}", tc(f"items/curio_{v}"))
generated("curio_rites", tc("items/crimson_rites"))
NOTES = ["sun", "stars_1", "stars_2", "stars_3", "stars_4"] + [f"moon_{i}" for i in range(1, 9)]
for n in NOTES:
    generated(f"celestial_notes_{n}", tc("items/celestial/sheet"), tc("items/celestial/" + n.replace("_", "")))

# Recipes (ConfigRecipes.initializeNormalRecipes)
R = f"{D}/{NS}/recipe"
write(f"{R}/tablewood.json", {"type": "minecraft:crafting_shaped", "category": "building", "pattern": ["SSS", "W W"],
                              "key": {"S": {"tag": "minecraft:wooden_slabs"}, "W": {"tag": "minecraft:planks"}},
                              "result": {"id": tc("table_wood"), "count": 1}})
write(f"{R}/tablestone.json", {"type": "minecraft:crafting_shaped", "category": "building", "pattern": ["SSS", "W W"],
                               "key": {"S": {"item": "minecraft:stone_slab"}, "W": {"item": "minecraft:stone"}},
                               "result": {"id": tc("table_stone"), "count": 1}})
write(f"{R}/scribingtoolscraft2.json", {"type": "minecraft:crafting_shapeless", "category": "misc", "group": "thaumcraft:inkwell",
                                        "ingredients": [{"item": "minecraft:glass_bottle"}, {"item": "minecraft:feather"}, {"tag": "c:dyes/black"}],
                                        "result": {"id": tc("scribing_tools"), "count": 1}})
write(f"{R}/scribingtoolsrefill.json", {"type": "minecraft:crafting_shapeless", "category": "misc", "group": "thaumcraft:inkwell",
                                        "ingredients": [{"item": tc("scribing_tools")}, {"tag": "c:dyes/black"}],
                                        "result": {"id": tc("scribing_tools"), "count": 1}})
# minecraft:mineable
for tag, vals in [("mineable/axe", ["table_wood", "research_table"]), ("mineable/pickaxe", ["table_stone"])]:
    p = f"{D}/minecraft/tags/block/{tag}.json"
    d = json.load(open(p)) if os.path.exists(p) else {"replace": False, "values": []}
    for v in vals:
        if tc(v) not in d["values"]:
            d["values"].append(tc(v))
    write(p, d)

# Aspects (ConfigAspects: research table = wood table + 5 cognitio; curios; notes)
p = f"{D}/{NS}/{NS}/aspects/items/thaumcraft.json"
d = json.load(open(p))
keys = {e.get("item") for e in d["entries"]}
extra = [
    {"item": tc("research_table"), "copy": tc("table_wood"), "aspects": {"cognitio": 5}},
]
for e in extra:
    if e["item"] not in keys:
        d["entries"].append(e)
write(p, d)

# pt_br
L = f"{A}/lang/pt_br.json"
pt = json.load(open(L, encoding="utf-8"), object_pairs_hook=collections.OrderedDict)
pt.update({
    "block.thaumcraft.table_wood": "Mesa de Madeira", "block.thaumcraft.table_stone": "Mesa de Pedra",
    "block.thaumcraft.research_table": "Mesa de Pesquisa", "item.thaumcraft.scribing_tools": "Ferramentas de Escrita",
    "item.thaumcraft.curio_arcane": "Curiosidade Arcana", "item.thaumcraft.curio_preserved": "Curiosidade Preservada",
    "item.thaumcraft.curio_ancient": "Curiosidade Antiga", "item.thaumcraft.curio_eldritch": "Curiosidade Sobrenatural",
    "item.thaumcraft.curio_knowledge": "Curiosidade Esclarecedora", "item.thaumcraft.curio_twisted": "Curiosidade Distorcida",
    "item.thaumcraft.curio_rites": "Ritos Carmesins", "item.curio.text": "Que conhecimento isso guarda?",
    "tc.knowledge.gained": "Você ganhou conhecimento", "button.create.theory": "Criar Teoria", "button.complete.theory": "Concluir Teoria",
    "button.scrap.theory": "Descartar Teoria", "tile.researchtable.noink.0": "Sua tinta acabou!", "tile.researchtable.noink.1": "Reabasteça suas ferramentas de escrita.",
    "tile.researchtable.nopaper.0": "Seu papel acabou!", "tc.card.unknown": "Item desconhecido",
})
for n in NOTES:
    pt[f"item.thaumcraft.celestial_notes_{n}"] = "Notas Celestiais"
json.dump(pt, open(L, "w", encoding="utf-8"), indent=1, ensure_ascii=False)

L = f"{A}/lang/en_us.json"
en = json.load(open(L, encoding="utf-8"), object_pairs_hook=collections.OrderedDict)
for v in ["arcane", "preserved", "ancient", "eldritch", "knowledge", "twisted", "rites"]:
    en.setdefault(f"item.thaumcraft.curio_{v}", en.get(f"item.curio.{v}.name", v))
for n in NOTES:
    en.setdefault(f"item.thaumcraft.celestial_notes_{n}", en.get("item.celestial_notes.name", "Celestial Notes"))
json.dump(en, open(L, "w", encoding="utf-8"), indent=1, ensure_ascii=False)
print("ok")
