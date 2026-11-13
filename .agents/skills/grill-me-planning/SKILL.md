---
name: grill-me-planning
description: Relentless planning and requirements interviewing workflow. Forces the AI agent into an active discovery mode to question edge cases, establish strict PRDs, and construct verified markdown plans before writing code.
---

# Grill Me & Planning with Files Workflow

This skill prevents premature, unaligned code generation by enforcing a strict architectural discovery and requirements verification phase ("Grill Me").

---

## 1. The "Grill Me" Interview Protocol

Before creating or editing code for any major feature, architectural shift, or refactoring:
1. **Never write code immediately on ambiguous requirements.**
2. **Interview the developer** with targeted, high-impact architectural questions:
   - **State Boundaries**: What is the single source of truth? Is offline persistence required?
   - **Edge Cases & Failure Modes**: How should empty states, network timeouts, revoked permissions, or corrupted files be handled?
   - **Scale & Performance**: What are the expected data volumes (e.g., 100 items vs 100,000 items)? Are there memory or battery constraints?
   - **User Experience**: How should loading states, optimistic updates, and animations behave?
   - **Compatibility & Migration**: What minSdk, API levels, or database schema versions are impacted?

---

## 2. File-Based Planning Protocol

Once requirements are clarified, generate a persistent planning artifact before writing code:

### Standard Plan Artifact Structure:
1. **Problem Statement & Scope**: Clear description of what is in-scope and explicitly out-of-scope.
2. **Architecture Impact & Layer Boundaries**: Which modules, layers (UI / Domain / Data), and interfaces will be introduced or modified.
3. **Data Schema & State Models**: Complete Kotlin data classes for `UiState`, `Action`, `Event`, `DomainModel`, and `Entity`.
4. **Step-by-Step Task Checklist**: Ordered execution steps with verification gates.
5. **Testing & Verification Matrix**: Unit tests, UI tests, and manual verification criteria.

---

## 3. Architecture Decision Record (ADR) Template

When deciding between alternative libraries, storage engines, or patterns, document an ADR:

```markdown
# ADR-001: [Title of Decision]

## Context
[What problem are we solving? What are the constraints?]

## Options Considered
1. Option A (e.g., Room DB with Paging 3)
   - Pros: Built-in Compose integration, memory efficient.
   - Cons: Additional boilerplate, complex test doubles.
2. Option B (e.g., In-memory cache + DataStore)
   - Pros: Simple, fast.
   - Cons: Cannot handle 50,000+ items without memory pressure.

## Decision
[Which option was chosen and why?]

## Consequences
[What becomes easier? What trade-offs or technical debt are accepted?]
```

---

## 4. Execution Checkpoints & Verification Gates

During plan execution:
- Update task checklist progress after each discrete milestone.
- Verify changes with `./gradlew compileDebugKotlin` or unit tests before moving to the next task.
- Stop and re-interview the user if an unforeseen architectural conflict or design ambiguity emerges.

