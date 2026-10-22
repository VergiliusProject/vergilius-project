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
