---
name: frontend-design
description: Rapid declarative UI engineering for Jetpack Compose. Specializes in luxury glassmorphic surfaces, neon glow accents, fluid micro-interactions, responsive window size classes, and Material 3 Expressive design tokens.
---

# Jetpack Compose Modern UI Design System

This skill guides the rapid creation of high-end, responsive, visually striking Android UIs using modern Jetpack Compose techniques.

---

## 1. Glassmorphism & Translucent Surfaces

Create frosted glass surfaces with subtle borders, blur effects, and depth gradients:

```kotlin
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(24.dp),
    backgroundColor: Color = Color(0x33101E3C), // Translucent deep navy
    borderColor: Color = Color(0x406EA8FF),     // Translucent neon cyan/blue
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .clip(shape)
            .background(backgroundColor)
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        borderColor.copy(alpha = 0.6f),
                        borderColor.copy(alpha = 0.1f)
                    )
                ),
                shape = shape
            )
            .padding(16.dp),
        content = content
    )
}
```

---

## 2. Neon Accents & Glowing Badges

Elevate file categories and active states with glowing neon gradients:

```kotlin
val NeonBlueGradient = Brush.horizontalGradient(
    colors = listOf(Color(0xFF38BDF8), Color(0xFF3B82F6))
)

val NeonPurpleGradient = Brush.horizontalGradient(
    colors = listOf(Color(0xFFA855F7), Color(0xFF6366F1))
)

val NeonAmberGradient = Brush.horizontalGradient(
    colors = listOf(Color(0xFFFBBF24), Color(0xFFF97316))
)

@Composable
fun CategoryPill(
    label: String,
    icon: ImageVector,
    gradient: Brush,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(if (isSelected) gradient else Brush.linearGradient(listOf(Color(0x22FFFFFF), Color(0x11FFFFFF))))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) Color.White else Color(0xFF94A3B8),
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                color = if (isSelected) Color.White else Color(0xFF94A3B8),
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}
```

---

## 3. Floating Bottom Navigation Dock

Floating dock with rounded pill shape, inner blur, and elevated shadow:

```kotlin
@Composable
fun FloatingDock(
    items: List<DockItem>,
    selectedIndex: Int,
    onItemSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(36.dp))
                .background(Color(0xDD0D1B2A))
                .border(1.dp, Color(0x33415A77), RoundedCornerShape(36.dp))
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEachIndexed { index, item ->
                val isSelected = index == selectedIndex
                IconButton(onClick = { onItemSelected(index) }) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.label,
                        tint = if (isSelected) Color(0xFF38BDF8) else Color(0xFF778DA9),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}
```

---

## 4. Multi-Window & Adaptive Screen Scaling

Adapt layouts based on window size classes (Compact, Medium, Expanded):

```kotlin
@Composable
fun AdaptiveResponsiveLayout(
    compactContent: @Composable () -> Unit,
    expandedContent: @Composable () -> Unit
) {
    BoxWithConstraints {
        if (maxWidth < 600.dp) {
            // Single-column Phone Layout
            compactContent()
        } else {
            // Two-pane Tablet / Foldable / Desktop Layout
            expandedContent()
        }
    }
}
```

