import csv, re, os, sys
m = {}
for f in ("fields.csv", "methods.csv", "params.csv"):
    with open(f"tools/mcp/{f}") as fh:
        for row in csv.DictReader(fh):
            m[row["searge"] if "searge" in row else row["param"]] = row["name"]
pat = re.compile(r"\b(field_\d+_[a-zA-Z_]+|func_\d+_[a-zA-Z_]+|p_\d+_\d+_)\b")
src, dst = sys.argv[1], sys.argv[2]
n = 0
for root, _, files in os.walk(src):
    for fn in files:
        if not fn.endswith(".java"): continue
        p = os.path.join(root, fn); out = os.path.join(dst, os.path.relpath(p, src))
        os.makedirs(os.path.dirname(out), exist_ok=True)
        t = open(p, encoding="utf-8").read()
        t2 = pat.sub(lambda x: m.get(x.group(1), x.group(1)), t)
        open(out, "w", encoding="utf-8").write(t2); n += 1
print(n, "files")
