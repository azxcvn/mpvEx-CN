package app.marlboroadvance.mpvex.ui.browser.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.WavyProgressIndicatorDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.marlboroadvance.mpvex.utils.media.CopyPasteOps

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun FileOperationProgressDialog(
  isOpen: Boolean,
  operationType: CopyPasteOps.OperationType,
  progress: CopyPasteOps.FileOperationProgress,
  onCancel: () -> Unit,
  onDismiss: () -> Unit,
  modifier: Modifier = Modifier,
) {
  if (!isOpen) return

  val operationName =
    when (operationType) {
      is CopyPasteOps.OperationType.Copy -> "复制"
      is CopyPasteOps.OperationType.Move -> "移动"
    }

  val isOperationComplete = progress.isComplete || progress.isCancelled || progress.error != null

  AlertDialog(
    onDismissRequest = {
      if (isOperationComplete) {
        onDismiss()
      }
    },
    title = {
      Text(
        text = "正在${operationName}文件",
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.Bold,
      )
    },
    text = {
      Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
      ) {
        // Status Messages
        when {
          progress.error != null -> {
            StatusCard(
              message = progress.error,
              containerColor = MaterialTheme.colorScheme.errorContainer,
              contentColor = MaterialTheme.colorScheme.onErrorContainer,
            )
          }
          progress.isComplete -> {
            StatusCard(
              message = "操作完成！",
              containerColor = MaterialTheme.colorScheme.primaryContainer,
              contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            )
          }
          progress.isCancelled -> {
            StatusCard(
              message = "操作已取消",
              containerColor = MaterialTheme.colorScheme.secondaryContainer,
              contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            )
          }
        }

        // Progress Section (only show during operation)
        if (!isOperationComplete) {
          Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            // Current File Info
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
              Text(
                text = "第 ${progress.currentFileIndex} 个文件，共 ${progress.totalFiles} 个",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
              Text(
                text = progress.currentFile,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface,
              )
            }

            // Current File Progress
            ProgressSection(
              label = "当前文件",
              progress = progress.currentFileProgress,
            )

            // Overall Progress
            ProgressSection(
              label = "总进度",
              progress = progress.overallProgress,
            )

            // Size Information
            Text(
              text = "${CopyPasteOps.formatBytes(
                progress.bytesProcessed,
              )} of ${CopyPasteOps.formatBytes(progress.totalBytes)}",
              style = MaterialTheme.typography.bodyLarge,
              fontWeight = FontWeight.Medium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.fillMaxWidth(),
            )
          }
        }

        // Summary (when complete)
        if (isOperationComplete) {
          Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SummaryRow(
              label = "已处理文件",
              value = "${progress.currentFileIndex} / ${progress.totalFiles}",
            )
            SummaryRow(
              label = "总大小",
              value = CopyPasteOps.formatBytes(progress.totalBytes),
            )
          }
        }
      }
    },
    confirmButton = {
      if (isOperationComplete) {
        Button(
          onClick = onDismiss,
          colors =
            ButtonDefaults.buttonColors(
              containerColor = MaterialTheme.colorScheme.primary,
            ),
          shape = MaterialTheme.shapes.extraLarge,
        ) {
          Text("完成", fontWeight = FontWeight.Bold)
        }
      } else {
        TextButton(
          onClick = onCancel,
          shape = MaterialTheme.shapes.extraLarge,
        ) {
          Icon(
            imageVector = Icons.Default.Cancel,
            contentDescription = "取消",
            modifier = Modifier.padding(end = 4.dp),
          )
          Text("取消", fontWeight = FontWeight.Medium)
        }
      }
    },
    containerColor = MaterialTheme.colorScheme.surface,
    tonalElevation = 6.dp,
    shape = MaterialTheme.shapes.extraLarge,
    modifier = modifier,
  )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun LoadingDialog(
  isOpen: Boolean,
  message: String = "加载中...",
  onDismissRequest: () -> Unit = {},
) {
  if (!isOpen) return

  AlertDialog(
    onDismissRequest = onDismissRequest,
    title = null,
    text = {
      Column(
        modifier =
          Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
      ) {
        Text(
          text = message,
          style = MaterialTheme.typography.bodyLarge,
          fontWeight = FontWeight.Medium,
          color = MaterialTheme.colorScheme.onSurface,
          textAlign = TextAlign.Center,
        )
      }
    },
    confirmButton = {},
    containerColor = MaterialTheme.colorScheme.surface,
    tonalElevation = 6.dp,
    shape = MaterialTheme.shapes.extraLarge,
  )
}

@Composable
private fun StatusCard(
  message: String,
  containerColor: androidx.compose.ui.graphics.Color,
  contentColor: androidx.compose.ui.graphics.Color,
) {
  Card(
    colors =
      CardDefaults.cardColors(
        containerColor = containerColor,
      ),
    shape = MaterialTheme.shapes.extraLarge,
  ) {
    Text(
      text = message,
      style = MaterialTheme.typography.bodyLarge,
      color = contentColor,
      fontWeight = FontWeight.Bold,
      modifier = Modifier.padding(20.dp),
    )
  }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ProgressSection(
  label: String,
  progress: Float,
) {
  Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Text(
        text = label,
        style = MaterialTheme.typography.bodyLarge,
        fontWeight = FontWeight.Medium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Text(
        text = "${(progress * 100).toInt()}%",
        style = MaterialTheme.typography.bodyLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
      )
    }
    LinearWavyProgressIndicator(
      modifier =
        Modifier
          .fillMaxWidth()
          .padding(horizontal = 8.dp),
      color = MaterialTheme.colorScheme.primary,
      trackColor = MaterialTheme.colorScheme.surfaceVariant,
      stroke = WavyProgressIndicatorDefaults.linearIndicatorStroke,
      trackStroke = WavyProgressIndicatorDefaults.linearTrackStroke,
      gapSize = WavyProgressIndicatorDefaults.LinearIndicatorTrackGapSize,
      amplitude = { 0.5f },
      wavelength = WavyProgressIndicatorDefaults.LinearIndeterminateWavelength,
      waveSpeed = WavyProgressIndicatorDefaults.LinearIndeterminateWavelength,
      progress = {
        progress
      },
    )
  }
}

@Composable
private fun SummaryRow(
  label: String,
  value: String,
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.bodyLarge,
      fontWeight = FontWeight.Medium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Text(
      text = value,
      style = MaterialTheme.typography.bodyLarge,
      fontWeight = FontWeight.Bold,
      color = MaterialTheme.colorScheme.onSurface,
    )
  }
}
