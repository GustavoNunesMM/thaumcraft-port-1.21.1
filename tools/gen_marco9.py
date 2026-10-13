"""Generates the JSON resources of milestone 9 (casters and foci): focus items and pouch, the focal manipulator
(wand_workbench), the sap field left by curse foci, the Curios belt slot for the pouch, and lang keys.

Run from the repo root: python3 tools/gen_marco9.py  (after gen_marco8)
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
    return n if ":" in n else f"{NS}:{n}"


def model(name, obj, kind="block"):
    write(f"{A}/models/{kind}/{name}.json", obj)


def loot_self(name):
    write(f"{D}/{NS}/loot_table/blocks/{name}.json", {"type": "minecraft:block", "pools": [{"rolls": 1, "bonus_rolls": 0,
          "entries": [{"type": "minecraft:item", "name": tc(name)}], "conditions": [{"condition": "minecraft:survives_explosion"}]}],
          "random_sequence": f"{NS}:blocks/{name}"})


def add_tag(kind, ns, path, values):
    p = f"{D}/{ns}/tags/{kind}/{path}.json"
    d = json.load(open(p)) if os.path.exists(p) else {"replace": False, "values": []}
    for v in values:
        if v not in d["values"]:
            d["values"].append(v)
    write(p, d)


# ====================================================================== items
for name in ["focus_1", "focus_2", "focus_3", "focus_pouch"]:
    model(name, {"parent": "minecraft:item/generated", "textures": {"layer0": f"{NS}:items/{name}"}}, "item")
# caster_basic (+ its focus overlay override) was generated with milestone 7.

# ====================================================================== focal manipulator
m = json.load(open(f"{ORIG}/models/block/wand_workbench.json"))
m["parent"] = tc("block/arcane_workbench")
model("wand_workbench", m)
write(f"{A}/blockstates/wand_workbench.json", {"variants": {"": {"model": tc("block/wand_workbench")}}})
model("wand_workbench", {"parent": tc("block/wand_workbench")}, "item")
loot_self("wand_workbench")
add_tag("block", "minecraft", "mineable/pickaxe", [tc("wand_workbench")])

# ====================================================================== sap field (invisible)
model("effect_sap", {"textures": {"particle": "minecraft:block/purple_stained_glass"}})
write(f"{A}/blockstates/effect_sap.json", {"variants": {"": {"model": tc("block/effect_sap")}}})

# ====================================================================== curios: the pouch goes in the belt slot (enabled for players by the gear milestone)
add_tag("item", "curios", "belt", [tc("focus_pouch")])

# ====================================================================== lang
L = f"{A}/lang/en_us.json"
en = json.load(open(L, encoding="utf-8"), object_pairs_hook=collections.OrderedDict)
L2 = f"{A}/lang/pt_br.json"
pt = json.load(open(L2, encoding="utf-8"), object_pairs_hook=collections.OrderedDict)
COPY = {"item.thaumcraft.focus_1": "item.focus_1.name", "item.thaumcraft.focus_2": "item.focus_2.name",
        "item.thaumcraft.focus_3": "item.focus_3.name", "item.thaumcraft.focus_pouch": "item.focus_pouch.name",
        "block.thaumcraft.wand_workbench": "tile.wand_workbench.name", "block.thaumcraft.effect_sap": "tile.effect_sap.name",
        "entity.thaumcraft.focus_projectile": "entity.FocusProjectile.name", "entity.thaumcraft.focus_cloud": "entity.FocusCloud.name",
        "entity.thaumcraft.focus_mine": "entity.FocusMine.name", "entity.thaumcraft.spell_bat": "entity.Spellbat.name"}
for d in (en, pt):
    for k, old in COPY.items():
        if old in d and k not in d:
            d[k] = d[old]
NEW = {
    "key.thaumcraft.change_focus": ("Change Caster Focus", "Trocar Foco do Conjurador"),
    "key.thaumcraft.misc_toggle": ("Misc Caster Toggle", "Alternância Diversa do Conjurador"),
    "ROOT.name": ("Root", "Raiz"),
    "ROOT.text": ("The caster: where you stand and where you are looking.", "O conjurador: onde você está e para onde está olhando."),
}
for k, (e, p) in NEW.items():
    en.setdefault(k, e)
    pt.setdefault(k, p)
for k in COPY:
    assert k in en and k in pt, k
json.dump(en, open(L, "w", encoding="utf-8"), indent=1, ensure_ascii=False)
json.dump(pt, open(L2, "w", encoding="utf-8"), indent=1, ensure_ascii=False)
print("ok")
