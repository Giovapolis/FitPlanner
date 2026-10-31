package com.fitplanner.ai.core.designsystem.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.fitplanner.ai.core.model.EquipmentType
import com.fitplanner.ai.core.model.Exercise
import com.fitplanner.ai.core.model.MovementPattern
import kotlin.math.cos
import kotlin.math.sin

private data class Pose(
    val hipX: Float = 0.5f,
    val hipY: Float = 0.62f,
    val torsoLean: Float = 0f,
    val armUpperL: Float = 0f,
    val armLowerL: Float = 0f,
    val armUpperR: Float = 0f,
    val armLowerR: Float = 0f,
    val thighL: Float = 2f,
    val shinL: Float = -2f,
    val thighR: Float = -2f,
    val shinR: Float = 2f
)

private data class AnimationSpec(
    val start: Pose,
    val end: Pose,
    val horizontal: Boolean = false,
    val bench: Boolean = false
)

@Composable
fun ExerciseAnimation(
    exercise: Exercise,
    modifier: Modifier = Modifier
) {
    val spec = remember(exercise) { specFor(exercise) }
    val transition = rememberInfiniteTransition(label = "technique")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "technique-progress"
    )
    val surface = MaterialTheme.colorScheme.surfaceVariant
    val ink = MaterialTheme.colorScheme.onSurface
    val accent = MaterialTheme.colorScheme.primary
    val floor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)

    Column(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(230.dp)
                .background(surface.copy(alpha = 0.55f), RoundedCornerShape(20.dp))
                .padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxWidth().height(206.dp)) {
                val width = size.width
                val height = size.height
                drawLine(
                    color = floor,
                    start = Offset(width * 0.08f, height * 0.89f),
                    end = Offset(width * 0.92f, height * 0.89f),
                    strokeWidth = 2.dp.toPx(),
                    cap = StrokeCap.Round
                )
                if (spec.horizontal) {
                    drawHorizontalFigure(
                        width = width,
                        height = height,
                        progress = progress,
                        color = ink,
                        accent = accent,
                        equipment = exercise.equipment,
                        bench = spec.bench
                    )
                } else {
                    drawStandingFigure(
                        width = width,
                        height = height,
                        pose = spec.start,
                        color = ink.copy(alpha = 0.16f),
                        accent = accent.copy(alpha = 0.16f),
                        equipment = exercise.equipment,
                        ghost = true
                    )
                    drawStandingFigure(
                        width = width,
                        height = height,
                        pose = lerp(spec.start, spec.end, progress),
                        color = ink,
                        accent = accent,
                        equipment = exercise.equipment,
                        ghost = false
                    )
                }
            }
        }
        Text(
            text = "Animación vectorial original · vista lateral simplificada",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 6.dp, start = 4.dp)
        )
    }
}

private fun lerp(start: Pose, end: Pose, t: Float): Pose {
    fun l(a: Float, b: Float) = a + (b - a) * t
    return Pose(
        hipX = l(start.hipX, end.hipX),
        hipY = l(start.hipY, end.hipY),
        torsoLean = l(start.torsoLean, end.torsoLean),
        armUpperL = l(start.armUpperL, end.armUpperL),
        armLowerL = l(start.armLowerL, end.armLowerL),
        armUpperR = l(start.armUpperR, end.armUpperR),
        armLowerR = l(start.armLowerR, end.armLowerR),
        thighL = l(start.thighL, end.thighL),
        shinL = l(start.shinL, end.shinL),
        thighR = l(start.thighR, end.thighR),
        shinR = l(start.shinR, end.shinR)
    )
}

private fun DrawScope.drawStandingFigure(
    width: Float,
    height: Float,
    pose: Pose,
    color: Color,
    accent: Color,
    equipment: Set<EquipmentType>,
    ghost: Boolean
) {
    val hip = Offset(width * pose.hipX, height * pose.hipY)
    val torsoLength = height * 0.20f
    val shoulder = hip + polar(180f + pose.torsoLean, torsoLength)
    val headCenter = shoulder + polar(180f + pose.torsoLean, height * 0.075f)
    val headRadius = height * 0.035f
    val stroke = if (ghost) 2.dp.toPx() else 3.2.dp.toPx()

    drawCircle(color = color, radius = headRadius, center = headCenter, style = Stroke(width = stroke))
    drawLine(color, hip, shoulder, strokeWidth = stroke, cap = StrokeCap.Round)

    val leftElbow = shoulder + polar(pose.armUpperL, height * 0.13f)
    val leftHand = leftElbow + polar(pose.armLowerL, height * 0.12f)
    val rightElbow = shoulder + polar(pose.armUpperR, height * 0.13f)
    val rightHand = rightElbow + polar(pose.armLowerR, height * 0.12f)
    drawLine(color, shoulder, leftElbow, stroke, StrokeCap.Round)
    drawLine(color, leftElbow, leftHand, stroke, StrokeCap.Round)
    drawLine(color, shoulder, rightElbow, stroke, StrokeCap.Round)
    drawLine(color, rightElbow, rightHand, stroke, StrokeCap.Round)

    val leftKnee = hip + polar(pose.thighL, height * 0.20f)
    val leftFoot = leftKnee + polar(pose.shinL, height * 0.20f)
    val rightKnee = hip + polar(pose.thighR, height * 0.20f)
    val rightFoot = rightKnee + polar(pose.shinR, height * 0.20f)
    drawLine(color, hip, leftKnee, stroke, StrokeCap.Round)
    drawLine(color, leftKnee, leftFoot, stroke, StrokeCap.Round)
    drawLine(color, hip, rightKnee, stroke, StrokeCap.Round)
    drawLine(color, rightKnee, rightFoot, stroke, StrokeCap.Round)
    drawLine(color, leftFoot, leftFoot + Offset(height * 0.04f, 0f), stroke, StrokeCap.Round)
    drawLine(color, rightFoot, rightFoot + Offset(height * 0.04f, 0f), stroke, StrokeCap.Round)

    if (!ghost) {
        drawEquipmentAtHands(
            leftHand = leftHand,
            rightHand = rightHand,
            equipment = equipment,
            color = accent,
            size = height * 0.02f
        )
    }
}

private fun DrawScope.drawHorizontalFigure(
    width: Float,
    height: Float,
    progress: Float,
    color: Color,
    accent: Color,
    equipment: Set<EquipmentType>,
    bench: Boolean
) {
    val stroke = 3.2.dp.toPx()
    val floorY = height * 0.78f
    val shoulder = Offset(width * 0.68f, height * 0.58f + progress * height * 0.05f)
    val hip = Offset(width * 0.33f, height * 0.60f + progress * height * 0.02f)
    val head = shoulder + Offset(width * 0.06f, -height * 0.02f)
    val hand = Offset(width * 0.71f, floorY)
    val elbow = Offset(width * 0.73f, height * 0.69f + progress * height * 0.04f)
    val knee = Offset(width * 0.20f, height * 0.66f)
    val foot = Offset(width * 0.11f, floorY)

    if (bench) {
        drawRoundRect(
            color = accent.copy(alpha = 0.22f),
            topLeft = Offset(width * 0.28f, height * 0.66f),
            size = Size(width * 0.48f, height * 0.035f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(height * 0.015f)
        )
        drawLine(accent.copy(alpha = 0.4f), Offset(width * 0.34f, height * 0.695f), Offset(width * 0.34f, floorY), 2.dp.toPx())
        drawLine(accent.copy(alpha = 0.4f), Offset(width * 0.70f, height * 0.695f), Offset(width * 0.70f, floorY), 2.dp.toPx())
    }

    drawCircle(color, height * 0.035f, head, style = Stroke(stroke))
    drawLine(color, shoulder, hip, stroke, StrokeCap.Round)
    drawLine(color, shoulder, elbow, stroke, StrokeCap.Round)
    drawLine(color, elbow, hand, stroke, StrokeCap.Round)
    drawLine(color, hip, knee, stroke, StrokeCap.Round)
    drawLine(color, knee, foot, stroke, StrokeCap.Round)
    drawLine(color, foot, foot + Offset(width * 0.03f, 0f), stroke, StrokeCap.Round)
    drawEquipmentAtHands(hand, hand, equipment, accent, height * 0.02f)
}

private fun DrawScope.drawEquipmentAtHands(
    leftHand: Offset,
    rightHand: Offset,
    equipment: Set<EquipmentType>,
    color: Color,
    size: Float
) {
    when {
        EquipmentType.BARBELL in equipment -> drawLine(
            color = color,
            start = leftHand + Offset(-size * 2f, 0f),
            end = rightHand + Offset(size * 2f, 0f),
            strokeWidth = 3.dp.toPx(),
            cap = StrokeCap.Round
        )
        EquipmentType.DUMBBELLS in equipment || EquipmentType.KETTLEBELLS in equipment -> {
            drawCircle(color, size, leftHand)
            drawCircle(color, size, rightHand)
        }
        EquipmentType.RESISTANCE_BANDS in equipment -> {
            drawLine(color.copy(alpha = 0.7f), leftHand, leftHand + Offset(0f, size * 3f), 2.dp.toPx())
            drawLine(color.copy(alpha = 0.7f), rightHand, rightHand + Offset(0f, size * 3f), 2.dp.toPx())
        }
    }
}

private fun polar(angleDegrees: Float, length: Float): Offset {
    val rad = Math.toRadians(angleDegrees.toDouble())
    return Offset((sin(rad) * length).toFloat(), (cos(rad) * length).toFloat())
}

private fun specFor(exercise: Exercise): AnimationSpec {
    if (exercise.id in setOf("push-up", "plank", "bird-dog", "dead-bug", "cat-cow")) {
        return AnimationSpec(
            start = Pose(hipY = 0.62f),
            end = Pose(hipY = 0.62f),
            horizontal = true,
            bench = false
        )
    }
    if (exercise.id == "dumbbell-bench-press" || exercise.id == "machine-chest-press") {
        return AnimationSpec(
            start = Pose(hipY = 0.62f),
            end = Pose(hipY = 0.62f),
            horizontal = true,
            bench = true
        )
    }
    return when (exercise.pattern) {
        MovementPattern.SQUAT -> AnimationSpec(
            start = Pose(hipY = 0.58f, thighL = 4f, shinL = -4f, thighR = -4f, shinR = 4f),
            end = Pose(hipY = 0.72f, torsoLean = 14f, thighL = 68f, shinL = -62f, thighR = 58f, shinR = -52f, armUpperL = 95f, armUpperR = 95f)
        )
        MovementPattern.HINGE -> AnimationSpec(
            start = Pose(),
            end = Pose(hipY = 0.65f, torsoLean = 62f, thighL = 10f, shinL = -10f, thighR = -8f, shinR = 8f, armUpperL = 2f, armUpperR = 2f)
        )
        MovementPattern.VERTICAL_PUSH -> AnimationSpec(
            start = Pose(armUpperL = 52f, armLowerL = 175f, armUpperR = -52f, armLowerR = 175f),
            end = Pose(armUpperL = 8f, armLowerL = 4f, armUpperR = -8f, armLowerR = 4f)
        )
        MovementPattern.HORIZONTAL_PUSH -> AnimationSpec(
            start = Pose(armUpperL = 84f, armLowerL = 4f, armUpperR = 96f, armLowerR = 4f),
            end = Pose(armUpperL = 65f, armLowerL = 120f, armUpperR = 115f, armLowerR = 120f)
        )
        MovementPattern.VERTICAL_PULL -> AnimationSpec(
            start = Pose(armUpperL = 175f, armLowerL = 170f, armUpperR = -175f, armLowerR = 170f),
            end = Pose(armUpperL = 115f, armLowerL = 40f, armUpperR = -115f, armLowerR = 40f)
        )
        MovementPattern.HORIZONTAL_PULL -> AnimationSpec(
            start = Pose(armUpperL = 78f, armLowerL = 8f, armUpperR = 102f, armLowerR = 8f),
            end = Pose(armUpperL = 42f, armLowerL = 140f, armUpperR = 138f, armLowerR = 140f)
        )
        MovementPattern.LUNGE -> AnimationSpec(
            start = Pose(),
            end = Pose(hipY = 0.67f, torsoLean = 5f, thighL = 62f, shinL = -58f, thighR = -55f, shinR = 55f, armUpperL = 20f, armUpperR = -20f)
        )
        MovementPattern.CARDIO -> AnimationSpec(
            start = Pose(thighL = 24f, shinL = -18f, thighR = -18f, shinR = 8f, armUpperL = -25f, armUpperR = 25f),
            end = Pose(thighL = -18f, shinL = 8f, thighR = 24f, shinR = -18f, armUpperL = 25f, armUpperR = -25f)
        )
        MovementPattern.MOBILITY -> AnimationSpec(
            start = Pose(torsoLean = -12f, armUpperL = 150f, armUpperR = -150f),
            end = Pose(torsoLean = 18f, armUpperL = 195f, armUpperR = -160f)
        )
        MovementPattern.CORE -> AnimationSpec(
            start = Pose(hipY = 0.62f),
            end = Pose(hipY = 0.62f),
            horizontal = true
        )
        MovementPattern.CARRY -> AnimationSpec(
            start = Pose(armUpperL = 4f, armUpperR = -4f),
            end = Pose(hipY = 0.64f, torsoLean = 6f, thighL = 18f, shinL = -16f, thighR = -10f, shinR = 8f, armUpperL = 4f, armUpperR = -4f)
        )
    }
}
