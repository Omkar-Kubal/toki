package com.focusfirst.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.focusfirst.data.model.AmbientSound
import com.focusfirst.ui.theme.TokiTypography

private val SheetBackground = Color(0xFF0D0D0D)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SoundSelectorSheet(
    currentSound:    AmbientSound,
    currentVolume:   Float,
    isPro:           Boolean,
    onSoundSelected: (AmbientSound) -> Unit,
    onVolumeChanged: (Float) -> Unit,
    onDismiss:       () -> Unit,
    onUpgradeClick:  () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor   = SheetBackground,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
        ) {
            Text(
                text       = "Ambient Sound",
                style      = TokiTypography.cardTitle,
                color      = Color.White,
            )

            Spacer(Modifier.height(4.dp))

            Text(
                text     = "Plays during focus, pauses on break",
                style    = TokiTypography.supporting,
                color    = Color.White.copy(alpha = 0.5f),
            )

            Spacer(Modifier.height(16.dp))

            val chunks = AmbientSound.entries.chunked(2)
            chunks.forEach { row ->
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    row.forEach { sound ->
                        val isLocked  = sound.isPro && !isPro
                        val isSelected = sound == currentSound
                        SoundCard(
                            sound      = sound,
                            isSelected = isSelected,
                            isLocked   = isLocked,
                            modifier   = Modifier.weight(1f),
                            onClick    = {
                                if (isLocked) onUpgradeClick()
                                else onSoundSelected(sound)
                            },
                        )
                    }
                    if (row.size == 1) {
                        Spacer(Modifier.weight(1f))
                    }
                }
                Spacer(Modifier.height(8.dp))
            }

            if (currentSound != AmbientSound.NONE) {
                Spacer(Modifier.height(8.dp))

                Text(
                    text     = "Volume",
                    style    = TokiTypography.sectionLabel,
                    color    = Color.White.copy(alpha = 0.6f),
                )

                Spacer(Modifier.height(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("\uD83D\uDD08", fontSize = 16.sp)  // 🔈
                    Spacer(Modifier.width(8.dp))
                    Slider(
                        value         = currentVolume,
                        onValueChange = onVolumeChanged,
                        modifier      = Modifier.weight(1f),
                        colors        = SliderDefaults.colors(
                            thumbColor          = Color.White,
                            activeTrackColor    = Color.White,
                            inactiveTrackColor  = Color.White.copy(alpha = 0.2f),
                        ),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("\uD83D\uDD0A", fontSize = 16.sp)  // 🔊
                }
            }
        }
    }
}

@Composable
private fun SoundCard(
    sound:     AmbientSound,
    isSelected: Boolean,
    isLocked:  Boolean,
    modifier:  Modifier,
    onClick:   () -> Unit,
) {
    Surface(
        onClick      = onClick,
        modifier     = modifier,
        color        = if (isSelected) Color.White.copy(alpha = 0.15f) else Color(0xFF111111),
        shape        = RoundedCornerShape(12.dp),
        border       = BorderStroke(
            width = if (isSelected) 1.dp else 0.5.dp,
            color = if (isSelected) Color.White else Color.White.copy(alpha = 0.1f),
        ),
    ) {
        Row(
            modifier          = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(sound.emoji, fontSize = 20.sp)
            Spacer(Modifier.width(8.dp))
            Text(
                text       = sound.displayName,
                style      = TokiTypography.rowTitle,
                color      = if (isLocked) Color.White.copy(alpha = 0.5f) else Color.White,
                modifier   = Modifier.weight(1f),
            )
            if (isLocked) {
                Text("\uD83D\uDD12", fontSize = 12.sp)  // 🔒
            }
            if (isSelected) {
                Icon(
                    imageVector        = Icons.Outlined.CheckCircle,
                    contentDescription = null,
                    tint               = Color.White,
                    modifier           = Modifier.size(24.dp),
                )
            }
        }
    }
}
