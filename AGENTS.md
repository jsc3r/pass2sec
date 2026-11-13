# Project instructions

## Secrets: do not access

- Never read, search, index, display, attach, decrypt or upload `.pass2sec/`, `.env`,
  `.env.*` or `*.key` files. These paths contain real local credentials or recovery state.
- Do not bypass agent ignore files by using a terminal, filesystem tool, application command
  or indirect reference. Do not inspect secret environment variable values.
- Do not run authentication or account migration against real accounts during development
  unless the user explicitly requests that operation. Use test doubles with synthetic secrets.
- For debugging, inspect implementation and tests, never local credential contents.
- Keep the exclusions in `.gitignore`, `.aiignore`, `.aiexclude`, `.cursorignore` and
  `.geminiignore` aligned. Custom `--state-dir` locations require equivalent exclusions.

## Architecture

- Use Boundary–Control–Entity (BCE).
- Organize by business capability first: `authentication`, `vault`, `alias`, `migration`,
  `profile`. Within each capability use `boundary`, `control` (including `control.port`),
  and `entity` as needed. Do not create global BCE layer packages.
- `<capability>.boundary`: CLI, console, external APIs, browser and persistence adapters.
- `<capability>.control`: use cases and port interfaces; no concrete boundary dependencies.
- `<capability>.entity`: immutable domain values and validation; no I/O or boundary/control imports.
- `shared` contains cross-cutting technical components; `application.boundary.cli` composes
  the CLI and wires concrete dependencies.
- One top-level class, record, enum or interface per Java file. Avoid nested classes.
- Use constructor injection for dependencies and compose adapters in `application.boundary.cli.ApplicationComponents`.
- Keep CLI commands small. Keep orchestration in controls and protocol details in boundaries.
- Never include provider request/response bodies or secrets in exceptions or logs.
- Preserve the global `--dry-run` contract: no credential/session mutations, browser actions,
  key generation, application-owned state directories, lock files or journal writes. Explicit
  read-only provider calls are allowed and must be identified in the output. Dry-run takes
  precedence over `--execute`; new CLI commands must implement an explicit preview path.

## Validation

- Run `mvn package` with JDK 25. Unit tests use synthetic credentials only.
- Preserve compatibility with existing encrypted rotation journal files during refactoring.
