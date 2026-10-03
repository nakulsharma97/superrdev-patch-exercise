# Patch Notes

## Summary of changes

- **Search SQL precedence (critical).** The title/description `OR` was not
  parenthesized, so `archived = FALSE` and the status filter only applied to one
  branch. Archived tasks leaked into results and status filtering was bypassed.
  Fixed in `TaskRepository`, `db/queries/search_tasks.sql`, and
  `db/oracle/task_search_package.sql`.
- **Artificial latency (high).** Removed a `Thread.sleep` in `TaskController`
  that delayed every request (~1s on the default blank query).
- **Input validation (medium).** Unknown `status` and `page`/`pageSize` < 1 now
  return `400` instead of `500`.
- **Frontend state (high/medium).** Reset to page 1 when search/status changes;
  stopped the infinite "Loading" state and surfaced request errors; debounced
  search (300ms).
- Added 4 backend MockMvc regression tests.

## What I chose not to change

In-memory pagination (loads all matching rows, then slices in Java), unbounded
`pageSize`, unescaped `LIKE` wildcards, `System.out` debug logging, and adding a
service layer.

## Biggest remaining risk

Pagination is in-memory: the repository returns every matching row and the
controller slices it. As data grows this is a memory and latency problem. The
proper fix is Spring Data `Pageable`, which changes the repository signature.

## Assumptions

Archived tasks should never appear in search; invalid input is a client error
(400), not 500; `page < 1` is rejected rather than silently clamped.

## Tools/AI used

Used AI (Codebuff) to explore the code and draft fixes. I verified the SQL fix by
reproducing the archived leak with `curl` against the running app and ran the
test suite. I rejected the AI's suggestion to add a service layer. One
AI-proposed test expects `page=0` to be clamped to `200`, which contradicts the
400 contract; it currently fails and is left visible rather than hidden.
