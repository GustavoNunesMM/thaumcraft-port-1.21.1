"""Generates the JSON resources of milestone 6 (crucible, nitor, alchemy items) and the arcane + crucible recipes.

Run from the repo root: python3 tools/gen_marco6.py  (after gen_marco5)
"""
import json, os, collections, subprocess, sys

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..")
RES = os.path.join(ROOT, "mod/src/main/resources")
A = os.path.join(RES, "assets/thaumcraft")
D = os.path.join(RES, "data")
ORIG = os.path.join(ROOT, "decompiled/assets/thaumcraft")
NS = "thaumcraft"
COLORS = ["white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray", "light_gray", "cyan", "purple", "blue",
          "brown", "green", "red", "black"]
OLD = {"light_blue": "lightblue", "light_gray": "silver"}
PT = {"white": "Branco", "orange": "Laranja", "magenta": "Magenta", "light_blue": "Azul-claro", "yellow": "Amarelo", "lime": "Verde-limão",
      "pink": "Rosa", "gray": "Cinza", "light_gray": "Cinza-claro", "cyan": "Ciano", "purple": "Roxo", "blue": "Azul", "brown": "Marrom",
      "green": "Verde", "red": "Vermelho", "black": "Preto"}


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
    write(f"{A}/models/item/{name}.json", {"parent": "minecraft:item/generated", "textures": {f"layer{i}": l for i, l in enumerate(layers)}})


def add_tag(kind, ns, path, values):
    p = f"{D}/{ns}/tags/{kind}/{path}.json"
    d = json.load(open(p)) if os.path.exists(p) else {"replace": False, "values": []}
    for v in values:
        if v not in d["values"]:
            d["values"].append(v)
    write(p, d)


# Crucible: the original element model.
m = json.load(open(f"{ORIG}/models/block/crucible.json"))
write(f"{A}/models/block/crucible.json", {"parent": "minecraft:block/block", **m})
write(f"{A}/models/item/crucible.json", {"parent": tc("block/crucible")})
write(f"{A}/blockstates/crucible.json", {"variants": {"": {"model": tc("block/crucible")}}})
loot_self("crucible")
add_tag("block", "minecraft", "mineable/pickaxe", [tc("crucible")])

# Nitor: invisible block (particles only); the item is the flame tinted by the dye, with the white core on top.
write(f"{A}/models/block/nitor.json", {"textures": {"particle": tc("blocks/nitor")}})
nitors = [f"nitor_{c}" for c in COLORS]
for n in nitors:
    write(f"{A}/blockstates/{n}.json", {"variants": {"": {"model": tc("block/nitor")}}})
    generated(n, tc("blocks/nitor"), tc("blocks/nitor_core"))
    loot_self(n)
add_tag("block", NS, "nitor", [tc(n) for n in nitors])
add_tag("item", NS, "nitor", [tc(n) for n in nitors])

generated("tallow", tc("items/tallow"))
generated("alumentum", tc("items/alumentum"))

# Aspects (ConfigAspects: crucible = cauldron + 20 fabrico + 20 alkimia; the rest come from their crucible recipes)
p = f"{D}/{NS}/{NS}/aspects/items/thaumcraft.json"
d = json.load(open(p))
keys = {e.get("item") for e in d["entries"]}
for e in [{"item": tc("crucible"), "copy": "minecraft:cauldron", "aspects": {"fabrico": 20, "alkimia": 20}}]:
    if e["item"] not in keys:
        d["entries"].append(e)
write(p, d)

# Lang: the 1.12 nitor keys used lightblue/silver.
L = f"{A}/lang/en_us.json"
en = json.load(open(L, encoding="utf-8"), object_pairs_hook=collections.OrderedDict)
en.setdefault("entity.thaumcraft.alumentum", "Alumentum")
en.setdefault("entity.thaumcraft.special_item", "Item")
for c in COLORS:
    old = en.get(f"block.thaumcraft.nitor_{OLD.get(c, c)}")
    if old:
        en[f"block.thaumcraft.nitor_{c}"] = old
json.dump(en, open(L, "w", encoding="utf-8"), indent=1, ensure_ascii=False)
L = f"{A}/lang/pt_br.json"
pt = json.load(open(L, encoding="utf-8"), object_pairs_hook=collections.OrderedDict)
pt.update({"block.thaumcraft.crucible": "Crisol", "item.thaumcraft.tallow": "Sebo Mágico", "item.thaumcraft.alumentum": "Alumentum",
           "recipe.type.crucible": "Crisol", "entity.thaumcraft.alumentum": "Alumentum", "entity.thaumcraft.special_item": "Item"})
for c in COLORS:
    pt[f"block.thaumcraft.nitor_{c}"] = f"Nitor {PT[c]}"
json.dump(pt, open(L, "w", encoding="utf-8"), indent=1, ensure_ascii=False)

subprocess.run([sys.executable, os.path.join(ROOT, "tools/convert_recipes.py"), "arcane", "crucible"], check=True)
