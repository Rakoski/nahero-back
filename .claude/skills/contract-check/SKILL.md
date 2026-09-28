---
name: contract-check
description: >-
  Find every place in ../nahero-front that breaks when a nahero-back API response,
  request, endpoint path, enum, or error code changes. Use whenever a *Response or
  *Request class is added, renamed, or has a field changed; when a controller route
  changes; when an enum gains or loses a value; when GlobalExceptionHandler or an
  error code changes; or before opening a PR that touches any of those. Reports the
  breaks and proposes the frontend diff.
---

# contract-check

`CLAUDE.md` says breaking response shapes requires a coordinated frontend change.
Nothing enforces that — there is no generated client and no shared schema. The
frontend hand-mirrors these shapes in TypeScript, so a renamed field compiles
fine on both sides and fails silently in the browser as `undefined`.

This skill is the missing enforcement. It is read-only on the backend and
proposes, but does not apply, frontend edits — that is a separate repo.

## Where the frontend duplicates the contract

Four places, all under `/home/mateus/Projetos/livres/nahero/nahero-front`:

1. **`src/lib/dtos.d.ts`** — shared interfaces, annotated with comments like
   `matches ListPracticeExamsResponse.java`. Those comments are the index: grep
   them for the Java class name first.
2. **`src/services/<domain>/<call>.ts`** — one file per API call, most declaring
   their own `export interface XResponse` inline next to the axios call. This is
   the larger surface and the one that gets missed. `src/services/subscription/get-status.ts`
   is the reference shape.
3. **`src/constants/nahero-api.ts`** — every endpoint path, as string literals or
   path-builder functions. A changed controller route breaks here.
4. **`src/lib/api-manager.ts`** — the axios instance. Its response interceptor
   branches on HTTP status plus `errorCode` from `BackendErrorResponse`
   (`src/types/api-error.ts`), so changing an error code or the error body shape
   changes auth behaviour, not just a message.

Browser calls go through `/api/proxy` (`src/app/api`), server-side calls go
straight to `NEXT_PUBLIC_API_URL_JAVA`. The backend context path is `/api/v1`,
so frontend paths are relative to that.

## Procedure

1. **Establish what changed.** Diff the backend side:
   `git diff -- 'src/main/java/**/*Response.java' 'src/main/java/**/*Request.java' 'src/main/java/**/controllers/**'`
   For each changed type, list the JSON keys before and after. Java records
   serialize component names verbatim, so renaming a component renames the JSON
   key. Adding a component is additive and safe; renaming, removing, or changing
   nullability is not.

2. **Grep the frontend for each affected name.** Search for the Java class name,
   then for each changed field name, across `src/lib/dtos.d.ts`,
   `src/services/`, `src/hooks/`, and `src/components/`. Field names are the part
   that matters — the TS interface is often named differently from the Java class.

3. **Follow the field to its consumers.** A changed field usually has a chain:
   service interface → TanStack Query hook in `src/hooks/` → component render.
   Report the whole chain, not just the interface, because the component is where
   the `undefined` shows up.

4. **Check the type translations.** These are the ones that silently drift:

   | Backend | Frontend |
   |---|---|
   | `Instant`, `LocalDateTime` | `string` (ISO), usually nullable |
   | `Integer`, boxed types | `number` or `null` — TS is strict, no `any` |
   | `boolean` primitive | `boolean`, never nullable |
   | enum (`SubscriptionStatus`, `PaymentProviderName`) | string-literal union, per the frontend convention of unions over enums |
   | `Page<T>` | `PageableResponse<T>` in `dtos.d.ts` |

   An added enum value is a real break: the union in the frontend will not
   include it and strict TS will reject or mis-handle it.

5. **Report, then propose.** Output a table of `frontend file:line → what breaks →
   required change`. Then write the proposed frontend diff. Do not apply it
   unless the user asks — and if they do, remember any new user-facing string
   must be added to both `src/dictionaries/en.ts` and `src/dictionaries/pt.ts`.

## Also a contract change, though it does not look like one

- A new endpoint that the frontend will call needs an entry in
  `nahero-api.ts` and a new file under the right `src/services/<domain>/`.
- A route added to `WhiteListConfig` changes which calls work unauthenticated.
- A new or changed `errorCode` from `GlobalExceptionHandler` may need handling
  in `api-manager.ts` or in `src/utils/error-utils.ts`.
- Changing the HTTP status for an existing failure changes whether
  `api-manager.ts` signs the user out. `CREDENTIAL_ENDPOINTS` in that file is an
  explicit carve-out for endpoints where 401 means bad credentials rather than a
  dead session — a new endpoint of that kind must be added to the set.

## Clean result

If nothing in the frontend references the changed shape, say so explicitly and
name what you grepped for. "No consumers found" is a useful answer; silence is
not.
