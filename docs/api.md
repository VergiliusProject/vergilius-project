# Vergilius API v1

The API is a public, read-only JSON interface to the kernel type database. No
authentication; no write endpoints. The user-facing surface is exactly these
five live routes plus static bulk dumps.

## Live endpoints (all under `/api/v1`)

| Endpoint | Purpose |
|---|---|
| `GET /api/v1/kernels/{arch}` | Families for an architecture (`x86`/`x64`) |
| `GET /api/v1/kernels/{arch}/{familySlug}` | OS builds in a family, sorted by build number |
| `GET /api/v1/kernels/{arch}/{familySlug}/{osSlug}` | One build summary |
| `GET .../{osSlug}/types?kind=&search=&page=&size=` | Paginated type list |
| `GET .../{osSlug}/types/{name}` | One type: JSON field tree + C declaration |
| `GET /api/v1/types/search?q=` | Cross-build type-name search |
| `GET /api/v1/types/{name}/diff?from=&to=` | Compare a structure across two builds |

### Cross-build diff

`GET /api/v1/types/{name}/diff?from=x64/windows-10/1607&to=x64/windows-11/21h2`
(each side is `arch/familySlug/osSlug`; old-name aliases resolve to canonical
slugs). Returns the per-field change list — `MOVED` (with offset delta),
`ADDED`, `REMOVED`, `TYPE_CHANGED`, `BITFIELD_CHANGED`, `VALUE_CHANGED` (enum
constants) — plus `sizeDelta` and generated **pseudocode** describing
build-aware access: insertion analysis ("a new field at 0x8 shifted every
later field uniformly by +8 bytes") and per-build accessor branches:

```c
if (build == x64/windows-11/21h2 (10.0.22000.0)) {
    imageBase = *(typeof(ImageBase)*)(base + 0x10);
} else if (build == x64/windows-10/1607 (10.0.14393.0)) {
    imageBase = *(typeof(ImageBase)*)(base + 0x8);
}
```

If the type exists in only one build, the diff reports a whole-type
ADDED/REMOVED. Matching is by field name (anonymous members by position), so
renames surface as REMOVE+ADD pairs — inspect offsets to distinguish a rename
from a true removal.

### Range / multi-build timeline

`GET /api/v1/types/{name}/timeline?from=x64/windows-10/1507&to=x64/windows-11/21h2`
diffs the structure across **every build** of that architecture whose build
number falls between the two endpoints (inclusive, sorted ascending) — e.g.
"first Windows 10 build to last Windows 11 build". An explicit list is also
supported: `?builds=x64/windows-10/1507,x64/windows-11/21h2,...`.

The response contains:

- `builds` — the resolved ordered range
- `presentIn` — builds where the type exists (a type absent mid-range shows
  whole-type REMOVE/ADD transitions)
- `transitions` — consecutive pairwise change lists, only where something
  changed
- `fieldHistories` — per field: offset change points (stable runs collapsed),
  which build added/removed it, and any type changes
- `pseudocode` — a chronological change log plus a multi-branch build
  dispatch:

```c
// MYSTRUCT evolution across 3 builds
//   x64/windows-10/1507 (10.0.10240.0) -> x64/windows-11/21h2 (10.0.22000.0)

// In 1607:
//   ~ A moved +4 (now @0x4)

if (build == x64/windows-11/21h2 (10.0.22000.0)) {
    a = *(typeof(A)*)(base + 0x4);
}
else if (build == x64/windows-10/1607 (10.0.14393.0)) {
    a = *(typeof(A)*)(base + 0x4);
}
else {  // earliest build
    a = *(typeof(A)*)(base + 0x0);
}

// Stable across all builds:
//   Header @0x0
```

Fields stable across the whole range get single-line accessors instead of
branches. `from`/`to` must target the same architecture (400 otherwise).



- `kind` is repeatable, one of `struct`, `union`, `enum` (400 otherwise).
- `page` is 0-based; `size` defaults to 50, capped at 200.
- Old family/OS names 301-redirect to canonical slugs (same alias machinery as
  the website).
- 404s are RFC 7807 `application/problem+json`.
- Type detail responses carry a scalar-key ETag (`If-None-Match` → 304) and
  `Cache-Control: public, max-age=86400, stale-while-revalidate=604800`;
  kernel lists and search get `max-age=300`.

### Type tree representation

- Named non-root structs/unions/enums serialize as `REF` nodes with an `href`;
  clients follow the href for deeper expansion. This is the same seam the
  website uses, and it cuts cyclic type graphs structurally.
- Anonymous types expand inline, with `anon:N` ids scoped to the containing
  document.
- Enums inline their constants (value taken from the field's stored offset);
  bitfields parse `Name:width` into `name` + `bitWidth`; array element counts
  and function return types/parameters are first-class fields.
- A 1024-node budget backstop emits `"truncated": true` instead of erroring.
- `cSource` is the plain C declaration (HTML-link-stripped) of the type —
  directly consumable by IDA's `parse_decls`.

## Static bulk dumps

Generated from the database (never from the import YAML):

- `GET /api/index.json` — manifest: every build + a per-type-name reverse index
  (`{typeName: [{arch, family, os, buildnumber, file, href}]}`) + `format`
  version field.
- `GET /api/os/{arch}/{osSlug}.json` — one build's complete named type set:
  walker trees, cSource, and a per-type `usedIn` reverse index.

Files are written to the `vergilius.dump.dir` directory (default `api-dump`)
and served as static resources under `/api/`. Output is deterministic — two
generations produce byte-identical files.

### Freshness contract

Dumps regenerate automatically (async) after every admin YAML import, and on
startup with `--dump-regenerate` (add `--dump-regenerate-and-exit` for CI).
Live endpoints always read the database; static dumps can lag by at most one
regeneration cycle if regeneration fails (logged, never breaks the upload).
`GET /api/v1/types/search` reads the manifest, so a fresh import is searchable
after the async regeneration completes.

## Implementation notes

- `TypeLoader` resolves a type graph with chunked `WHERE id IN (...)` queries
  — no per-node lookups in any request path (`findByOpersysAndId1..5` are not
  used by the API).
- `TypeTreeBuilder` decodes type semantics once for both live endpoints and
  the dump generator; `WalkerFieldBuilderParityTest` proves byte-for-byte
  parity with the website renderer over the committed golden corpus (Gate 2).
- The types list slices the second-level-cached per-build list in memory.
  Bridge: replace with a paged COUNT/LIMIT query if list items ever need
  field-level data or p95 at cache hit exceeds 100 ms.
- Known renderer quirk preserved as-is: `FieldBuilder.getModifier()` never
  renders `const` (only `volatile`). The golden corpus documents it; fix
  separately if desired.
- Out of scope for v1 (deliberately absent): `?expand=depth` parameters, rate
  limiting, OpenAPI/Swagger, content negotiation, `usedIn` in live responses,
  any admin or mutation API.

## Quick start

```sh
./gradlew bootRun
./gradlew bootRun --args="--dump-regenerate"   # (re)generate static dumps

curl -si localhost:8080/api/v1/kernels/x64
curl -si localhost:8080/api/v1/kernels/x64/windows-10
curl -si "localhost:8080/api/v1/kernels/x64/windows-10/1607/types?kind=struct&search=list"
curl -si localhost:8080/api/v1/kernels/x64/windows-10/1607/types/LIST_ENTRY
curl -si -H "If-None-Match: <etag>" localhost:8080/api/v1/kernels/x64/windows-10/1607/types/LIST_ENTRY
curl -si "localhost:8080/api/v1/types/search?q=PEB"
curl -si localhost:8080/api/index.json
```
