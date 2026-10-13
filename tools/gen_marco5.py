"""Generates the JSON resources of milestone 5 (arcane workbench + charger) and the arcane recipes.

Run from the repo root: python3 tools/gen_marco5.py  (after gen_marco2/gen_marco4; it also runs convert_recipes.py)
"""
import json, os, collections, shutil, subprocess, sys

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


# Arcane workbench: the original element model.
m = json.load(open(f"{ORIG}/models/block/arcane_workbench.json"))
m = {"parent": "minecraft:block/block", **m}

write(f"{A}/models/block/arcane_workbench.json", m)
write(f"{A}/models/item/arcane_workbench.json", {"parent": tc("block/arcane_workbench")})
write(f"{A}/blockstates/arcane_workbench.json", {"variants": {"": {"model": tc("block/arcane_workbench")}}})
loot_self("arcane_workbench")

# Charger: OBJ model.
shutil.copy(f"{ORIG}/models/block/arcane_workbench_charger.obj", f"{A}/models/block/arcane_workbench_charger.obj")
with open(f"{A}/models/block/arcane_workbench_charger.mtl", "w") as f:
    f.write("newmtl texture\nKd 1.0 1.0 1.0\nmap_Kd #texture\n")
write(f"{A}/models/block/arcane_workbench_charger.json", {
    "parent": "minecraft:block/block", "loader": "neoforge:obj", "model": tc("models/block/arcane_workbench_charger.obj"), "flip_v": True,
    "render_type": "minecraft:cutout", "textures": {"texture": tc("blocks/arcane_workbench_charger"), "particle": tc("blocks/arcane_workbench_charger")}})
write(f"{A}/models/item/arcane_workbench_charger.json", {"parent": tc("block/arcane_workbench_charger")})
write(f"{A}/blockstates/arcane_workbench_charger.json", {"variants": {"": {"model": tc("block/arcane_workbench_charger")}}})
loot_self("arcane_workbench_charger")

# minecraft:mineable
p = f"{D}/minecraft/tags/block/mineable/axe.json"
d = json.load(open(p))
for v in ["arcane_workbench", "arcane_workbench_charger"]:
    if tc(v) not in d["values"]:
        d["values"].append(tc(v))
write(p, d)

# Aspects (ConfigAspects: workbench = crafting table + 5 praecantatio + 5 auram)
p = f"{D}/{NS}/{NS}/aspects/items/thaumcraft.json"
d = json.load(open(p))
keys = {e.get("item") for e in d["entries"]}
for e in [{"item": tc("arcane_workbench"), "copy": "minecraft:crafting_table", "aspects": {"praecantatio": 5, "auram": 5}}]:
    if e["item"] not in keys:
        d["entries"].append(e)
write(p, d)

# pt_br
L = f"{A}/lang/pt_br.json"
pt = json.load(open(L, encoding="utf-8"), object_pairs_hook=collections.OrderedDict)
pt.update({
    "block.thaumcraft.arcane_workbench": "Bancada Arcana", "block.thaumcraft.arcane_workbench_charger": "Carregador de Bancada",
    "workbench.available": "disponível", "workbench.discount": "desconto", "workbench.cost": "vis",
    "recipe.type.workbench": "Bancada", "recipe.type.workbenchshapeless": "Bancada (sem forma)",
    "recipe.type.arcane": "Bancada Arcana", "recipe.type.arcane.shapeless": "Bancada Arcana (sem forma)", "wandtable.text1": "Custo de Vis",
})
json.dump(pt, open(L, "w", encoding="utf-8"), indent=1, ensure_ascii=False)

# Arcane recipes from ConfigRecipes (only those whose items exist load, see the conditions).
subprocess.run([sys.executable, os.path.join(ROOT, "tools/convert_recipes.py"), "arcane"], check=True)
