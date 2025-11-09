package com.raremartial.aiac.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.raremartial.aiac.data.model.SolutionMethod

@Composable
fun SolutionMethodSelector(
    selectedMethods: Set<SolutionMethod>,
    onMethodToggle: (SolutionMethod) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Способы решения:",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SolutionMethodChip(
                method = SolutionMethod.DIRECT,
                label = "Прямой",
                isSelected = selectedMethods.contains(SolutionMethod.DIRECT),
                onToggle = { onMethodToggle(SolutionMethod.DIRECT) }
            )
            
            SolutionMethodChip(
                method = SolutionMethod.STEP_BY_STEP,
                label = "Пошагово",
                isSelected = selectedMethods.contains(SolutionMethod.STEP_BY_STEP),
                onToggle = { onMethodToggle(SolutionMethod.STEP_BY_STEP) }
            )
            
            SolutionMethodChip(
                method = SolutionMethod.EXPERT_PANEL,
                label = "Эксперты",
                isSelected = selectedMethods.contains(SolutionMethod.EXPERT_PANEL),
                onToggle = { onMethodToggle(SolutionMethod.EXPERT_PANEL) }
            )
        }
    }
}

@Composable
private fun SolutionMethodChip(
    method: SolutionMethod,
    label: String,
    isSelected: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    FilterChip(
        selected = isSelected,
        onClick = onToggle,
        label = { Text(label) },
        modifier = modifier,
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    )
}

