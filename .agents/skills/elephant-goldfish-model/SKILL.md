---
name: elephant-goldfish-model
description: Google's Elephant-Goldfish Model (EGM) for Spec-Driven Development. Splits agent tasks into The Elephant (long-horizon architectural memory & living design specs in docs/designs/) and The Goldfish (zero-context stateless subagents for ruthless, unbiased review), with drift reporting post-implementation.
---

# Google's Elephant-Goldfish Model (EGM)

The **Elephant-Goldfish Model (EGM)** (`google/elephant-goldfish-model`) is Google's open-source framework for **Spec-Driven Development** with AI coding agents.

It solves the fundamental flaw of AI pair programming: **context pollution and confirmation bias**. When an AI engages in prolonged back-and-forth chat, both human and model develop shared blind spots, leading to premature coding and missed edge cases.

EGM decouples the process into two strictly separated cognitive roles:

```
                      THE ELEPHANT (Deep Memory)
                      • Long-running conversational session
                      • Holds vision, architecture & intent
                      • Drafts living specifications in docs/designs/
                      • NEVER writes code directly
                                   │
                                   ▼
                       Living Specification
                     (docs/designs/<feature>.md)
                                   │
                  ┌────────────────┴────────────────┐
                  ▼                                 ▼
         GOLDFISH REVIEWER 1               GOLDFISH REVIEWER 2
      (Zero Conversational Context)     (Zero Conversational Context)
      • Fresh, isolated sub-agent       • Fresh, isolated sub-agent
      • Reads ONLY spec + codebase      • Ruthlessly hunts blind spots
      • Leads with risks & flaws        • Flags unhandled failure modes
                  └────────────────┬────────────────┘
                                   │
                                   ▼
                      Triaged & Approved Spec
                                   │
                                   ▼
                           Implementation
                                   │
                                   ▼
                           EGM Drift Report
                     (Code vs Spec Audit & Sync)
```

---

## 1. The Core Roles

### 🐘 The Elephant (Deep Context & Design)
- **Mindset**: Long-horizon architect, historian, and product designer.
- **Responsibility**: Holds the holistic context of the codebase, business goals, and dependencies. Engages in deep Socratic discussion to define requirements, map data models, and write living specifications.
- **Rule**: Never writes production code directly during the Elephant phase.

### 🐟 The Goldfish (Zero-Context Review Swarm)
- **Mindset**: Highly critical, unbiased external reviewer with zero memory of the Elephant discussion.
- **Responsibility**: Spawned as isolated sub-agents (`goldfish_reviewer`). Reads **only** the written specification and the existing repository code.
- **Rule**: Must lead with risks, missing failure modes, concurrency issues, edge cases, and ambiguities. Zero flattery or empty praise.

---

## 2. EGM Workflow Commands

### Step 1: `/egm-brainstorm`
Clarify abstract requirements through structured Socratic questioning:
- **Scope & Boundaries**: What is strictly in-scope vs. out-of-scope?
- **Failure Modes**: What happens when the network drops mid-stream, disk is full, or user revokes permissions?
- **State Invariants**: What states are impossible and how are they prevented?
- **Performance Budgets**: Memory limits, recomposition targets, network payload size.

### Step 2: `/egm-draft`
Compile brainstormed decisions into a formal specification stored under `docs/designs/<feature-name>.md`:

```markdown
# Design Spec: [Feature Name]

## 1. Context & Business Intent
[Why is this being built? What problem does it solve?]

## 2. Architectural Boundaries & Data Flow
[Which modules/layers are touched? Diagram the state flow.]

## 3. Data Models & State Contracts
```kotlin
// Full data classes for UiState, Actions, Events, Domain models
```

## 4. Error Handling & Edge Cases
- Network drops / timeouts: [Behavior]
- Empty data sets: [Behavior]
- Corrupted cache: [Behavior]
- Migration / Schema updates: [Behavior]

## 5. Security & Privacy
- Sensitive data handling, Keystore encryption, permission validation.

## 6. Implementation Checklist & Verification Gates
- [ ] Task 1: Domain UseCases & Tests
- [ ] Task 2: Repository / Local DB Dao & Tests
- [ ] Task 3: ViewModel & Turbine StateFlow Tests
- [ ] Task 4: Compose UI & Paparazzi Snapshot Tests
```

### Step 3: `/egm-review`
Deploy zero-context Goldfish sub-agents to audit the spec:

In Antigravity, invoke the `goldfish_reviewer` subagent:
```kotlin
invoke_subagent(
    TypeName = "goldfish_reviewer",
    Role = "Goldfish Spec Reviewer",
    Prompt = "Review docs/designs/<feature-name>.md against the codebase. Audit for unhandled edge cases, concurrency deadlocks, missing state transitions, and architectural boundary violations. Lead with risks."
)
```

### Step 4: `/egm-resolve`
The Elephant session reviews the Goldfish findings:
- Address each critique directly in `docs/designs/<feature-name>.md`.
- Refine data models, error states, and testing checklists.
- Lock the specification.

### Step 5: Spec-Driven Implementation
Execute tasks strictly following the approved design specification, paired with the `superpowers-tdd` (Red-Green-Refactor) engine.

### Step 6: `/egm-drift-report`
Post-implementation verification to ensure the code didn't diverge from the spec:
- Compare implemented APIs, models, and behavior against `docs/designs/<feature-name>.md`.
- Flag any undocumented departures or unintentional deviations.
- Either align the code to the spec or update the living spec to reflect approved pragmatic pivots.

