---
name: compose-motion-design
description: Natural spring animations, transitions, touch feedback and reduced-motion support
---

# Compose Motion Design System

Enforces fluid iOS-calibrated physics springs and responsive micro-interactions in Jetpack Compose.

## Motion Standards
1. **Spring Motion**:
   - Use `spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow)` for interactive scale and sheet transitions.
2. **Screen Transitions**:
   - Elegant fade & slide transitions with `slideInHorizontally` + `fadeIn` and `slideOutHorizontally` + `fadeOut`.
3. **Touch Feedback**:
   - Gentle scale bounces on interactive cards (e.g. `scale` pressed effect to `0.97f`).
   - Subtle glowing ripple effects tailored to glass surfaces.
4. **Accessibility**:
   - Honor system reduced-motion settings by dampening spring stiffness or snapping when reduced motion is preferred.
