"""Writes a GameTest structure template: a stone floor of WxD with H blocks of room above it.

Usage: python3 tools/make_structure.py <out.nbt> <W> <H> <D>
"""
import gzip, struct, sys


def tag_header(t, name):
    n = name.encode()
    return bytes([t]) + struct.pack(">H", len(n)) + n


def string(name, v):
    b = v.encode()
    return tag_header(8, name) + struct.pack(">H", len(b)) + b


def int_(name, v):
    return tag_header(3, name) + struct.pack(">i", v)


def int_list(name, vals):
    return tag_header(9, name) + bytes([3]) + struct.pack(">i", len(vals)) + b"".join(struct.pack(">i", v) for v in vals)


def compound_list(name, compounds):
    return tag_header(9, name) + bytes([10]) + struct.pack(">i", len(compounds)) + b"".join(c + b"\x00" for c in compounds)


out, w, h, d = sys.argv[1], int(sys.argv[2]), int(sys.argv[3]), int(sys.argv[4])
blocks = []
for x in range(w):
    for z in range(d):
        blocks.append(int_list("pos", [x, 0, z]) + int_("state", 0))
root = (int_("DataVersion", 3955)  # 1.21.1
        + int_list("size", [w, h, d])
        + compound_list("palette", [string("Name", "minecraft:stone")])
        + compound_list("blocks", blocks)
        + compound_list("entities", []))
with gzip.open(out, "wb") as f:
    f.write(tag_header(10, "") + root + b"\x00")
