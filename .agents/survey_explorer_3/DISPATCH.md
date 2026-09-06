# Dispatch: Survey Explorer 3 (Codebase & Grepai Semantic Architecture)

**Objective**:
Map the actual Java 21 / Spring Boot 3 multi-module architecture, REST controllers, AMQP endpoints, and test `grepai` MCP integration.

**Scope & Responsibilities**:
1. Read `c:\IdeaProjects\DonaTrack-TP-DDS\.agents\ORIGINAL_REQUEST.md`.
2. Test calling `grepai` MCP tools (`call_mcp_tool` with ServerName: `grepai`, ToolName: `grepai_search` or `grepai_index_status`).
3. Survey all Spring Boot modules (`common-lib`, `personas-service`, `heladeras-service`, `donaciones-service`, `puntos-service`, `alertas-service`, `integration-tests`).
4. Identify all `@RestController` / `@RequestMapping` classes and paths, and all RabbitMQ/AMQP listener bindings (`@RabbitListener`, routing keys).
5. Compare against documentation assertions to spot known divergences between docs and code.
6. Write findings to `c:\IdeaProjects\DonaTrack-TP-DDS\.agents\survey_explorer_3\handoff.md`.
7. Notify orchestrator via `send_message` when complete.

## 2026-09-06T05:12:18Z

You are survey_explorer_3.
Your working directory is c:\IdeaProjects\DonaTrack-TP-DDS\.agents\survey_explorer_3.
Your parent is edbee326-cd86-464a-8638-feb6a5a74249.

Read c:\IdeaProjects\DonaTrack-TP-DDS\.agents\ORIGINAL_REQUEST.md and c:\IdeaProjects\DonaTrack-TP-DDS\.agents\survey_explorer_3\DISPATCH.md.

Task:
1. Test and verify grepai MCP integration (`call_mcp_tool` with ServerName: "grepai", ToolName: "grepai_search" or "grepai_index_status").
2. Map out the Java 21 / Spring Boot 3 multi-module architecture (`common-lib`, `personas-service`, `heladeras-service`, `donaciones-service`, `puntos-service`, `alertas-service`, `integration-tests`).
3. Identify all `@RestController` / `@RequestMapping` endpoints and all `@RabbitListener` bindings in the Java code.
4. Compare with the documented endpoints and AMQP contracts to detect potential divergences or missing documentation.
5. Write your detailed findings with verified code evidence to c:\IdeaProjects\DonaTrack-TP-DDS\.agents\survey_explorer_3\handoff.md.
When finished, send a message to your parent with your summary and output file path.

