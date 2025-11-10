package com.raremartial.aiac.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.raremartial.aiac.data.model.Temperature

@Composable
fun TemperatureSelector(
    selectedTemperature: Temperature,
    onTemperatureSelected: (Temperature) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Температура:",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TemperatureChip(
                temperature = Temperature.LOW,
                label = "0.0",
                description = "Низкая",
                isSelected = selectedTemperature == Temperature.LOW,
                onSelect = { onTemperatureSelected(Temperature.LOW) }
            )
            
            TemperatureChip(
                temperature = Temperature.MEDIUM,
                label = "0.5",
                description = "Средняя",
                isSelected = selectedTemperature == Temperature.MEDIUM,
                onSelect = { onTemperatureSelected(Temperature.MEDIUM) }
            )
            
            TemperatureChip(
                temperature = Temperature.HIGH,
                label = "1.0",
                description = "Высокая",
                isSelected = selectedTemperature == Temperature.HIGH,
                onSelect = { onTemperatureSelected(Temperature.HIGH) }
            )
        }
    }
}

@Composable
private fun TemperatureChip(
    temperature: Temperature,
    label: String,
    description: String,
    isSelected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    FilterChip(
        selected = isSelected,
        onClick = onSelect,
        label = { 
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        },
        modifier = modifier,
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    )
}

