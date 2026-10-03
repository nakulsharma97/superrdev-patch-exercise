# Patch Notes

## Summary of changes

- **Search SQL precedence (critical).** The title/description `OR` was not
  parenthesized, so `archived = FALSE` and the status filter applied to only one
  branch. Archived tasks leaked into results and status filtering was bypassed.
  Fixed in `TaskRepository`, `db/queries/search_tasks.sql`, and
  `db/oracle/task_search_package.sql`.
- **Artificial latency (high).** Removed a `Thread.sleep` in `TaskController`
  that delayed every request (~1s on the default blank query).
- **Input validation (medium).** Unknown `status` and `page`/`pageSize` < 1 now
  return `400` instead of `500`; `pageSize` is capped at 100.
- **Pagination overflow (medium).** `(page - 1) * pageSize` overflowed `int` for
  large page values, giving a negative `subList` index (500). Now uses `long`
  math before the bounds check.
- **Frontend state (high/medium).** Reset to page 1 on search/status change;
  fixed the infinite "Loading" state and surfaced request errors; debounced
  search (300ms); a stale in-flight response is ignored once inputs change.
- Added 5 backend MockMvc tests and `spring-boot-starter-test` (test scope).

## What I chose not to change

In-memory pagination (loads all matching rows, then slices in Java), unescaped
`LIKE` wildcards, `System.out` logging, and a service layer.

## Biggest remaining risk

Pagination is in-memory: the repository returns every matching row and the
controller slices it. As data grows this is a memory and latency problem; the
proper fix is Spring Data `Pageable`, which changes the repository signature.

## Assumptions

Archived tasks never appear in search; invalid input is a client error (400), not
500; `page < 1` is rejected rather than silently clamped.

## Tools/AI used

Used AI (Codebuff) to explore the code and draft fixes. I reproduced the archived
leak with `curl`, and ran `./mvnw test` (5 passing) and `npm run build` (passing)
this session. I rejected the AI's suggestion to add a service layer. Frontend
race/state changes were build-verified only.
