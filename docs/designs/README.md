# Living Design Specifications (EGM)

This directory stores formal, living architectural specifications generated via Google's **Elephant-Goldfish Model (EGM)**.

## Workflow
1. **Drafting (Elephant)**: Spec drafted under `docs/designs/<feature-name>.md` using `/egm-draft`.
2. **Review (Goldfish)**: Zero-context audit by independent `goldfish_reviewer` subagents using `/egm-review`.
3. **Approval**: Triage and resolve critiques before coding begins.
4. **Drift Audit**: Post-implementation verification using `/egm-drift-report`.

