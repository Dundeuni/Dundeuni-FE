package com.dundueni.app.feature.analysisresult

import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.progressSemantics
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import com.dundueni.app.R
import com.dundueni.app.ui.theme.LocalEasyMode
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dundueni.app.ui.theme.Danger
import com.dundueni.app.ui.theme.ProtectiveBlue
import com.dundueni.app.ui.theme.BrandInk
import com.dundueni.app.ui.theme.Caution
import com.dundueni.app.ui.theme.CautionInk
import com.dundueni.app.ui.theme.PageBackground as AppPageBackground
import com.dundueni.app.ui.theme.PrimaryMidnight
import com.dundueni.app.ui.theme.Safe
import com.dundueni.app.ui.theme.SafeInk
import com.dundueni.app.ui.theme.SecondaryInk
import com.dundueni.app.ui.theme.SoftBlue as AppSoftBlue
import com.dundueni.app.ui.theme.SoftCaution
import com.dundueni.app.ui.theme.SoftDanger
import com.dundueni.app.ui.theme.SoftSafe
import com.dundueni.app.feature.analysis.ScreenResultType
import com.dundueni.app.feature.analysis.textDisplayRiskLevel
import com.dundueni.app.feature.history.HistoryType
import com.dundueni.app.data.model.ExtractedPhoneNumber
import com.dundueni.app.data.model.ImageDetailedReason
import com.dundueni.app.data.model.ImageModelDetails
import com.dundueni.app.data.repository.ImageSummaryResult
import com.dundueni.app.ui.component.DundueniButton
import com.dundueni.app.ui.component.DundueniButtonType
import java.util.Locale

private val PageBackground = AppPageBackground
private val SoftBlue = AppSoftBlue
private val SoftRed = SoftDanger
private val Ink = BrandInk
private val Muted = SecondaryInk
private val CardOutline = Color(0xFFEDF0F8)

/** The host supplies the verdict, evidence and actions; this screen only presents them. */
@Composable
fun AnalysisResultScreen(
    result: AnalysisResult,
    onAction: (ResultAction) -> Unit,
    modifier: Modifier = Modifier,
    isDemo: Boolean = false,
    imageAnalysisError: String? = null,
    screenCropError: String? = null,
    imageSummary: ImageSummaryUiState? = null,
    onCheckImageSummary: () -> Unit = {},
    screenResultType: ScreenResultType? = null,
    onInspectPhone: ((String) -> Unit)? = null,
    phoneInspectionInProgress: Boolean = false,
) {
    val title = when {
        result.historyType == HistoryType.SCREEN && screenResultType != null -> "화면 검사 결과"
        !isDemo && result.historyType == HistoryType.TEXT -> "문자 검사 결과"
        !isDemo && result.historyType == HistoryType.IMAGE -> "이미지 검사 결과"
        !isDemo && result.historyType == HistoryType.PHONE -> "전화번호 검사 결과"
        else -> "결과 화면"
    }
    Scaffold(
        modifier = modifier,
        containerColor = PageBackground,
        topBar = { ResultHeader(title) },
    ) { padding ->
        ResultImageLayout(
            title = if (result.historyType == HistoryType.SCREEN) "화면 검사 결과" else title,
            maxWidth = 520.dp,
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            modifier = Modifier.padding(padding),
            actions = { onSave, canSave ->
                Spacer(Modifier.height(2.dp))
                ResultActions(onAction, onSave, canSave)
            },
        ) { forImage ->
            val onCheckSummary = onCheckImageSummary.takeUnless { forImage }
            val selectPhone = onInspectPhone.takeUnless { forImage }
            val phoneNumbers = result.selectablePhoneNumbers(screenResultType)
            when (result.historyType.takeUnless { isDemo }) {
                HistoryType.IMAGE -> ImageInspectionContent(result, imageAnalysisError, imageSummary, onCheckSummary)
                HistoryType.TEXT -> TextInspectionContent(
                    result, phoneNumbers = phoneNumbers, onInspectPhone = selectPhone,
                    phoneInspectionInProgress = phoneInspectionInProgress,
                )
                HistoryType.PHONE -> PhoneInspectionContent(result)
                HistoryType.SCREEN -> {
                    if (screenResultType != ScreenResultType.TEXT) {
                        ImageInspectionContent(result, imageAnalysisError, imageSummary, onCheckSummary)
                    }
                    if (screenResultType != ScreenResultType.IMAGE) {
                        TextInspectionContent(
                            result, screenCropError, screenCrop = true,
                            phoneNumbers = phoneNumbers, onInspectPhone = selectPhone,
                            phoneInspectionInProgress = phoneInspectionInProgress,
                        )
                    }
                }
                else -> {
                    if (isDemo) {
                        Surface(color = Color(0xFFFFF2D8), shape = RoundedCornerShape(12.dp)) {
                            Text("디자인 미리보기 · 샘플 데이터", Modifier.fillMaxWidth().padding(12.dp), color = Color(0xFF795700), fontSize = 12.sp)
                        }
                    }
                    if (!LocalEasyMode.current) result.sourceText?.let { MessageQuote(it) }
                    if (result.historyType == HistoryType.PHONE && !LocalEasyMode.current && result.targetLabel.isNotBlank()) {
                        TargetRow(result.targetLabel)
                    }
                    if (LocalEasyMode.current) {
                        EasyResultContent(result, isDemo)
                    } else when (result.riskLevel) {
                        RiskLevel.SAFE -> {
                            Column(
                                Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                                    .background(Brush.linearGradient(listOf(Color.White, Color(0xFFF0F3FF), Color.White)))
                                    .padding(20.dp),
                                verticalArrangement = Arrangement.spacedBy(18.dp),
                            ) {
                                ResultHero(result)
                                if (result.historyType != HistoryType.PHONE) TargetRow(result.targetLabel)
                            }
                            MetricsRow(result)
                            if (result.evidence.isNotEmpty() || isDemo) EvidencePanel(result, isDemo)
                            LimitationsPanel(result)
                            if (isDemo) SafetyTip()
                        }
                        RiskLevel.CAUTION -> {
                            ResultHero(result)
                            Spacer(Modifier.height(8.dp))
                            if (result.summary.isNotBlank() || isDemo)
                                SummaryBanner(if (isDemo) result.keyFinding ?: result.summary else result.summary, urgent = false)
                            if (isDemo || result.historyType == HistoryType.PHONE ||
                                result.historyType == HistoryType.TEXT && result.textRiskScore != null ||
                                result.imageModelResult && (result.imageGenerationDecision != null || result.imageManipulationDecision != null)) ResultPanel {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Text(when (result.historyType) {
                                        HistoryType.PHONE -> "전화번호 검사 결과"
                                        HistoryType.TEXT -> "텍스트 분류 결과"
                                        else -> "이미지 분석 결과"
                                    }, fontSize = 13.sp, color = Ink)
                                    Text("분석 엔진 결과", fontSize = 10.sp, color = Muted)
                                }
                                MetricsRow(result, nested = true)
                            }
                            if (result.evidence.isNotEmpty() || isDemo) EvidencePanel(result, isDemo)
                            LimitationsPanel(result)
                        }
                        RiskLevel.DANGER -> {
                            ResultHero(result)
                            if (result.summary.isNotBlank() || isDemo)
                                SummaryBanner(if (isDemo) "지금은 절대 돈을 보내지 마세요!" else result.summary, urgent = true)
                            Spacer(Modifier.height(4.dp))
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                MetricsRow(result)
                                if (isDemo) Text("* 사기 위험과 AI 생성·변조 가능성은 각각 독립적으로 분석됩니다.", color = Muted, fontSize = 10.sp, lineHeight = 15.sp)
                            }
                            if (result.evidence.isNotEmpty() || isDemo) EvidencePanel(result, isDemo)
                            LimitationsPanel(result)
                        }
                        RiskLevel.UNKNOWN -> {
                            ResultHero(result)
                            MetricsRow(result)
                            if (result.evidence.isNotEmpty() || isDemo) EvidencePanel(result, isDemo)
                            LimitationsPanel(result)
                        }
                    }
                    if (result.historyType == HistoryType.PHONE) {
                        result.keyFinding?.takeIf { it.isNotBlank() }?.let { finding ->
                            ResultPanel {
                                Text("신고 관련 정보", color = Ink, fontWeight = FontWeight.SemiBold)
                                Text(finding, color = Muted)
                            }
                        }
                    }
                    if (!isDemo && result.guidance.isNotEmpty()) {
                        SafetyTip(result.guidance)
                    }
                }
            }
        }
    }
}

@Composable
internal fun ImageInspectionContent(
    result: AnalysisResult,
    error: String? = null,
    summaryState: ImageSummaryUiState? = null,
    onCheckSummary: (() -> Unit)? = null,
) {
    if (error != null) {
        ResultPanel {
            InspectionHeading("이미지 생성·변조 분석")
            InspectionText(error)
        }
        return
    }

    val label = result.imageOverallDecisionLabel?.trim()
    val serverLevel = when (result.imageOverallDecision) {
        "GENERATION_OR_MANIPULATION_SUSPECTED" -> RiskLevel.DANGER
        "INCONCLUSIVE" -> RiskLevel.CAUTION
        "NO_CLEAR_SIGNAL" -> RiskLevel.SAFE
        else -> RiskLevel.entries.firstOrNull { it != RiskLevel.UNKNOWN && it.label == label }
    }
    val level = serverLevel ?: result.riskLevel.takeUnless { result.imageModelResult }
    val style = when (level) {
        RiskLevel.DANGER -> InspectionSummaryStyle.RED
        RiskLevel.CAUTION -> InspectionSummaryStyle.YELLOW
        RiskLevel.SAFE -> InspectionSummaryStyle.BLUE
        else -> InspectionSummaryStyle.NEUTRAL
    }
    val title = serverLevel?.label
        ?: label?.takeIf { it == "판단 불가" || !result.imageModelResult && it.isNotEmpty() }
        ?: "이미지 생성·변조 분석"
    val badge = when {
        serverLevel != null -> "이미지 종합 판정"
        level != null && level != RiskLevel.UNKNOWN -> "위험도 · ${level.label}"
        else -> null
    }
    val state = summaryState ?: result.initialImageSummary()?.let {
        ImageSummaryUiState(result = it, checking = it == ImageSummaryResult.Pending)
    }
    val completed = (state?.result as? ImageSummaryResult.Completed)?.details
    val summary = completed?.summary ?: result.summary.takeUnless { result.imageModelResult }

    InspectionSummaryCard(title, badge, style) {
        ImageSummaryContent(summary, state, onCheckSummary, showFailure = !result.imageModelResult)
    }
    ImageInspectionMetrics(result)
    val useLegacyDetails = result.imageSummary == null && completed == null
    ImageInspectionEvidence(result, completed?.detailedReasons.orEmpty(), useLegacyDetails)

    val guides = completed?.actionGuides.orEmpty().filter { it.title.isNotBlank() || it.description.isNotBlank() }
    if (guides.isNotEmpty()) {
        InspectionGuidance("대응 수칙") {
            guides.forEach { InspectionGuidanceItem(it.description, title = it.title) }
        }
    } else if (useLegacyDetails) {
        InspectionGuidance(result.guidance, title = "이미지 대응 안내")
    }
    completed?.reassurance?.takeIf(String::isNotBlank)?.let { reassurance ->
        ResultPanel(color = SoftBlue) {
            InspectionHeading("안심 조언")
            InspectionText(reassurance)
        }
    }
}

@Composable
private fun ImageSummaryContent(
    summary: String?,
    state: ImageSummaryUiState?,
    onCheck: (() -> Unit)?,
    showFailure: Boolean,
) {
    val text = summary?.takeIf(String::isNotBlank) ?: when (state?.result) {
        ImageSummaryResult.Pending, ImageSummaryResult.LookupError -> when {
            state.checking && state.showRetry -> "요약 상태를 확인하고 있어요."
            state.lookupFailed || state.result == ImageSummaryResult.LookupError -> "요약 상태를 확인하지 못했어요."
            else -> "요약을 준비하고 있어요."
        }
        ImageSummaryResult.Failed -> "요약을 만들지 못했어요.".takeIf { showFailure }
        else -> null
    } ?: return

    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        InspectionHeading("분석 요약")
        InspectionText(text)
        if (state?.showRetry == true && onCheck != null) {
            DundueniButton("다시 확인", onCheck, type = DundueniButtonType.SECONDARY, enabled = !state.checking)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ImageInspectionMetrics(result: AnalysisResult) {
    val generationDetails = result.imageGenerationDetails
    val manipulationDetails = result.imageManipulationDetails
    val generationScore = result.imageGenerationDisplayScore.takeUnless { generationDetails?.status == "FAILED" }
    val manipulationScore = result.imageManipulationDisplayScore.takeUnless { manipulationDetails?.status == "FAILED" }
    val showUnanalyzed = result.imageSummary != null && result.aiScore == null
    val generation = imageInspectionDecision(
        generationDetails, result.imageGenerationDecision, generationScore, generation = true, showUnanalyzed = showUnanalyzed,
    )
    val manipulation = imageInspectionDecision(
        manipulationDetails, result.imageManipulationDecision, manipulationScore, generation = false, showUnanalyzed = showUnanalyzed,
    )
    if (generation == null && manipulation == null && result.aiScore == null) return

    ResultPanel {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ResultGlyph(ResultIcon.AI, ProtectiveBlue)
            InspectionHeading("AI 탐지 세부 지표")
        }
        val stackCards = LocalEasyMode.current || LocalDensity.current.fontScale >= 1.3f
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            maxItemsInEachRow = if (stackCards) 1 else 2,
        ) {
            ImageInspectionMetric(
                "AI 생성 분석", generation, generationScore, Modifier.weight(1f),
                showMissingScore = generationDetails?.status == "SUCCEEDED",
            )
            ImageInspectionMetric(
                "이미지 변조 분석", manipulation, manipulationScore, Modifier.weight(1f),
                showMissingScore = manipulationDetails?.status == "SUCCEEDED",
            )
        }
        result.aiScore?.let { InspectionText("AI 생성 점수 ${formatScore(it)}/100") }
    }
}

private fun imageInspectionDecision(
    details: ImageModelDetails?,
    decision: String?,
    score: Double?,
    generation: Boolean,
    showUnanalyzed: Boolean,
): String? {
    if (details?.status == "FAILED") return "분석 실패"
    if (decision == "INCONCLUSIVE") return "판단 불가"
    return imageDecisionLabel(decision, generation) ?: when {
        details != null || decision != null || score != null -> "판정 정보 없음"
        showUnanalyzed -> "미분석"
        else -> null
    }
}

@Composable
private fun ImageInspectionMetric(
    title: String,
    verdict: String?,
    score: Double?,
    modifier: Modifier,
    showMissingScore: Boolean,
) {
    if (verdict == null && score == null) return
    Surface(modifier = modifier, color = SoftBlue, shape = RoundedCornerShape(12.dp)) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            InspectionText(title)
            verdict?.let { InspectionHeading(it) }
            if (score != null) {
                InspectionText("표시 점수 ${score.toString().removeSuffix(".0")}/100")
                Box(Modifier.fillMaxWidth().height(6.dp).clip(CircleShape).background(Color.White)) {
                    Box(Modifier.fillMaxWidth((score / 100).toFloat()).fillMaxHeight().background(ProtectiveBlue))
                }
            } else if (showMissingScore) {
                InspectionText("점수 미제공")
            }
        }
    }
}

@Composable
private fun ImageInspectionEvidence(
    result: AnalysisResult,
    detailedReasons: List<ImageDetailedReason>,
    useLegacyEvidence: Boolean,
) {
    val reasons = detailedReasons.filter { it.title.isNotBlank() || it.description.isNotBlank() }
    val generation = result.imageGenerationDetails.takeIf { useLegacyEvidence }
    val manipulation = result.imageManipulationDetails.takeIf { useLegacyEvidence }
    val modelReasons = generation?.reasons.orEmpty() + manipulation?.reasons.orEmpty()
    val evidence = result.evidence.takeIf { useLegacyEvidence }.orEmpty()
        .map { it.description }.filterNot(modelReasons::contains)
    val generationLimitations = generation?.limitations.orEmpty().takeUnless { result.imageModelResult }.orEmpty()
    val manipulationLimitations = manipulation?.limitations.orEmpty().takeUnless { result.imageModelResult }.orEmpty()
    if (reasons.isEmpty() && evidence.isEmpty() && modelReasons.isEmpty() &&
        generationLimitations.isEmpty() && manipulationLimitations.isEmpty()
    ) return

    ResultPanel {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ResultGlyph(ResultIcon.SHIELD_OUTLINE, ProtectiveBlue)
            InspectionHeading("이미지 분석 근거")
        }
        reasons.forEach { reason ->
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                when (reason.type) {
                    "GENERATION" -> InspectionText("AI 생성 분석")
                    "MANIPULATION" -> InspectionText("이미지 변조 분석")
                }
                if (reason.title.isNotBlank()) InspectionHeading(reason.title)
                if (reason.description.isNotBlank()) InspectionText(reason.description)
            }
        }
        evidence.forEach { InspectionText(it) }
        InspectionFindings("AI 생성 판단 근거", generation?.reasons.orEmpty())
        InspectionFindings("AI 생성 분석 한계", generationLimitations)
        InspectionFindings("이미지 변조 판단 근거", manipulation?.reasons.orEmpty())
        InspectionFindings("이미지 변조 분석 한계", manipulationLimitations)
    }
}

@Composable
internal fun PhoneInspectionContent(result: AnalysisResult) {
    if (LocalEasyMode.current) {
        EasyResultContent(result, isDemo = false)
    } else {
        PhoneInspectionSummary(result)
        result.sourceText?.let { MessageQuote(it) }
        if (result.evidence.isNotEmpty()) EvidencePanel(result, isDemo = false)
        LimitationsPanel(result)
    }
    InspectionGuidance(result.guidance)
}

@Composable
private fun PhoneInspectionSummary(result: AnalysisResult) {
    val (title, style) = when (result.riskLevel) {
        RiskLevel.SAFE -> "현재는 안전해 보여요" to InspectionSummaryStyle.BLUE
        RiskLevel.CAUTION -> "조금 더 확인해보세요" to InspectionSummaryStyle.YELLOW
        RiskLevel.DANGER -> "위험할 가능성이 높아요" to InspectionSummaryStyle.RED
        RiskLevel.UNKNOWN -> "결과 정보를 확인해주세요" to InspectionSummaryStyle.NEUTRAL
    }
    val grade = if (result.riskLevel == RiskLevel.UNKNOWN) "정보 부족" else result.riskLevel.label
    InspectionSummaryCard(title, "전화번호 위험 등급 · $grade", style) {
        if (result.targetLabel.isNotBlank()) {
            Text(
                result.targetLabel, color = Ink, textAlign = TextAlign.Center,
                fontWeight = FontWeight.SemiBold, fontSize = if (LocalEasyMode.current) 22.sp else 16.sp,
            )
        }
        if (result.summary.isNotBlank()) InspectionText(result.summary)
        result.keyFinding?.takeIf { it.isNotBlank() }?.let { finding ->
            Surface(color = Color.White, shape = RoundedCornerShape(10.dp)) {
                Box(Modifier.fillMaxWidth().padding(12.dp)) { InspectionText(finding) }
            }
        }
    }
}

private enum class InspectionSummaryStyle(val tint: Color, val badgeInk: Color, val mascot: Int) {
    BLUE(SoftBlue, Ink, R.drawable.dundeuni_mascot_confident),
    YELLOW(SoftCaution, CautionInk, R.drawable.dundeuni_mascot_suspicious),
    RED(SoftDanger, Danger, R.drawable.dundeuni_mascot_danger),
    NEUTRAL(Color(0xFFF0F1F4), Muted, R.drawable.dundeuni_mascot_search),
}

@Composable
private fun InspectionSummaryCard(
    title: String,
    badge: String?,
    style: InspectionSummaryStyle,
    content: @Composable ColumnScope.() -> Unit,
) {
    val easy = LocalEasyMode.current
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, CardOutline),
    ) {
        Column(
            Modifier.fillMaxWidth()
                .background(Brush.linearGradient(listOf(style.tint, Color.White, style.tint)))
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(if (easy) 16.dp else 10.dp),
        ) {
            Image(painterResource(style.mascot), contentDescription = null, modifier = Modifier.size(if (easy) 112.dp else 84.dp))
            Text(
                title, color = Ink, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center,
                fontSize = if (easy) 28.sp else 20.sp,
                lineHeight = if (easy) 38.sp else 28.sp,
            )
            badge?.let {
                Surface(shape = CircleShape, color = style.tint) {
                    Text(
                        it, Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                        color = style.badgeInk, fontSize = if (easy) 18.sp else 13.sp,
                        fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center,
                    )
                }
            }
            content()
        }
    }
}

@Composable
private fun InspectionGuidance(guidance: List<String>, title: String = "대응 안내") {
    if (guidance.isEmpty()) return
    InspectionGuidance(title) {
        guidance.forEach { InspectionGuidanceItem(it) }
    }
}

@Composable
private fun InspectionGuidance(title: String, content: @Composable ColumnScope.() -> Unit) {
    ResultPanel {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ResultGlyph(ResultIcon.CHECKLIST, ProtectiveBlue)
            InspectionHeading(title)
        }
        content()
    }
}

@Composable
private fun InspectionGuidanceItem(description: String, title: String = "") {
    Surface(color = SoftBlue, shape = RoundedCornerShape(10.dp)) {
        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            if (title.isNotBlank()) InspectionHeading(title)
            if (description.isNotBlank()) InspectionText(description)
        }
    }
}

internal fun AnalysisResult.selectablePhoneNumbers(screenResultType: ScreenResultType? = null): List<ExtractedPhoneNumber> =
    if (historyType == HistoryType.TEXT ||
        historyType == HistoryType.SCREEN && screenResultType != ScreenResultType.IMAGE
    ) extractedPhoneNumbers.take(10) else emptyList()

@Composable
internal fun TextInspectionContent(
    result: AnalysisResult,
    error: String? = null,
    screenCrop: Boolean = false,
    fromHistory: Boolean = false,
    phoneNumbers: List<ExtractedPhoneNumber> = emptyList(),
    onInspectPhone: ((String) -> Unit)? = null,
    phoneInspectionInProgress: Boolean = false,
) {
    val title = if (screenCrop) "화면 문자 피싱 분석" else "텍스트 피싱 분석"
    // A combined Screen result stores the image summary and guides in these shared fields.
    val summary = result.summary.takeUnless { screenCrop }.orEmpty()
    if (error == null) {
        TextInspectionSummary(result.textDecision, result.keyFinding ?: title, summary)
    }
    if (!screenCrop) result.sourceText?.let { text ->
        MessageQuote(text, label = if (fromHistory) "저장된 메시지 미리보기" else "분석된 메시지 내용")
    }
    if (error != null) {
        ResultPanel {
            InspectionHeading(title)
            InspectionText(error)
        }
        return
    }

    if (onInspectPhone != null && phoneNumbers.isNotEmpty()) {
        ExtractedPhoneNumbersPanel(phoneNumbers, onInspectPhone, enabled = !phoneInspectionInProgress)
    }
    val evidence = if (screenCrop) result.screenTextEvidence else result.evidence
    if (evidence.isNotEmpty()) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ResultGlyph(ResultIcon.SHIELD_OUTLINE, ProtectiveBlue)
            InspectionHeading("텍스트 분석 근거")
        }
        evidence.forEach { item ->
            ResultPanel {
                InspectionHeading(item.title)
                InspectionText(item.description)
            }
        }
    }
    result.textRiskScore?.let { TextInspectionScore(it) }
    if (result.textLimitations.isNotEmpty()) {
        ResultPanel { InspectionFindings("텍스트 분석 주의사항", result.textLimitations) }
    }

    val existingAdvice = when {
        result.textDecision == "phishing" ->
            "송금하거나 개인정보를 보내기 전에 발신자를 공식 연락처로 확인해 주세요."
        result.textNeedsReview == true || result.textDecision == "defer" ->
            "문자 내용과 발신자를 한 번 더 확인해 주세요."
        else -> null
    }
    val guidance = result.guidance.takeUnless { screenCrop }.orEmpty() + listOfNotNull(existingAdvice)
    if (guidance.isNotEmpty()) {
        ResultPanel {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ResultGlyph(ResultIcon.CHECKLIST, ProtectiveBlue)
                InspectionHeading("대응 안내")
            }
            guidance.distinct().forEach { instruction ->
                Surface(color = SoftBlue, shape = RoundedCornerShape(10.dp)) {
                    Box(Modifier.fillMaxWidth().padding(12.dp)) { InspectionText(instruction) }
                }
            }
        }
    }
}

@Composable
private fun ExtractedPhoneNumbersPanel(
    phoneNumbers: List<ExtractedPhoneNumber>,
    onInspectPhone: (String) -> Unit,
    enabled: Boolean,
) {
    ResultPanel {
        InspectionHeading("발견된 전화번호")
        phoneNumbers.forEach { phone ->
            Surface(color = SoftBlue, shape = RoundedCornerShape(10.dp)) {
                Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    InspectionHeading(phone.displayNumber)
                    DundueniButton(
                        "전화번호 검사",
                        onClick = { onInspectPhone(phone.phoneNumber) },
                        modifier = Modifier.fillMaxWidth(),
                        type = DundueniButtonType.SECONDARY,
                        enabled = enabled,
                    )
                }
            }
        }
    }
}

@Composable
private fun TextInspectionSummary(decision: String?, title: String, summary: String) {
    val (style, badge) = when (textDisplayRiskLevel(decision)) {
        RiskLevel.SAFE -> InspectionSummaryStyle.BLUE to "텍스트 판정 · 일반"
        RiskLevel.CAUTION -> InspectionSummaryStyle.YELLOW to "텍스트 판정 · 의심"
        else -> InspectionSummaryStyle.NEUTRAL to (if (decision == "defer") "텍스트 판정 · 보류" else null)
    }
    InspectionSummaryCard(title, badge, style) {
        if (summary.isNotBlank()) {
            InspectionHeading("AI 요약")
            Text(
                summary, color = Muted, textAlign = TextAlign.Center,
                fontSize = if (LocalEasyMode.current) 18.sp else 14.sp,
                lineHeight = if (LocalEasyMode.current) 26.sp else 22.sp,
            )
        }
    }
}

@Composable
private fun TextInspectionScore(score: Double) {
    ResultPanel {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ResultGlyph(ResultIcon.AI, ProtectiveBlue)
            InspectionHeading("텍스트 분류 점수")
        }
        Surface(color = SoftBlue, shape = RoundedCornerShape(12.dp)) {
            Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    score.toString(), color = Ink, fontWeight = FontWeight.SemiBold,
                    fontSize = if (LocalEasyMode.current) 24.sp else 20.sp,
                )
                if (score in 0.0..1.0) {
                    Box(Modifier.fillMaxWidth().height(6.dp).clip(CircleShape).background(Color.White)) {
                        Box(Modifier.fillMaxWidth(score.toFloat()).fillMaxHeight().background(ProtectiveBlue))
                    }
                }
                InspectionText("0~1 범위의 미보정 분류 점수이며 사기 확률이 아닙니다.")
            }
        }
    }
}

@Composable
private fun InspectionFindings(title: String, items: List<String>) {
    if (items.isEmpty()) return
    InspectionHeading(title)
    items.forEach { InspectionText(it) }
}

@Composable
private fun InspectionHeading(text: String) {
    Text(text, color = Ink, fontWeight = FontWeight.SemiBold, fontSize = if (LocalEasyMode.current) 20.sp else 16.sp)
}

@Composable
private fun InspectionText(text: String) {
    Text(text, color = Muted, fontSize = if (LocalEasyMode.current) 18.sp else 14.sp)
}

@Composable
private fun ResultHeader(title: String) {
    Surface(color = PageBackground, shadowElevation = 1.dp) {
        Text(
            text = title,
            modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 20.dp, vertical = 16.dp),
            color = Ink,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun ResultHero(result: AnalysisResult) {
    if (result.riskLevel == RiskLevel.UNKNOWN) {
        val hasTextDecision = (result.historyType == HistoryType.TEXT || result.historyType == HistoryType.SCREEN) && result.keyFinding != null
        val imageDecision = result.imageOverallDecisionLabel?.takeIf { result.imageModelResult && it.isNotBlank() }
        Column(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.size(80.dp).background(SoftBlue, CircleShape), contentAlignment = Alignment.Center) {
                Image(painterResource(R.drawable.dundeuni_mascot_search), null, Modifier.size(80.dp))
            }
            Surface(color = Color(0xFFE8EEFF), shape = CircleShape) {
                Text(if (result.imageModelResult) "이미지 종합 판정" else if (hasTextDecision) "분석 완료 · 텍스트 판정" else "분석 완료",
                    Modifier.padding(horizontal = 12.dp, vertical = 5.dp), color = Ink, fontSize = 11.sp)
            }
            Text(imageDecision ?: if (result.imageModelResult) "이미지 분석 완료"
                else if (hasTextDecision) result.keyFinding!! else "결과 정보를 확인해주세요",
                color = Ink, fontSize = 24.sp, lineHeight = 32.sp,
                fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            if (result.summary.isNotBlank()) Text(result.summary, color = Muted, fontSize = 13.sp, lineHeight = 20.sp, textAlign = TextAlign.Center)
        }
        return
    }
    val safe = result.riskLevel == RiskLevel.SAFE
    val statusColor = when (result.riskLevel) {
        RiskLevel.SAFE -> Safe
        RiskLevel.CAUTION -> Caution
        RiskLevel.DANGER -> Danger
        RiskLevel.UNKNOWN -> ProtectiveBlue
    }
    Column(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(92.dp), contentAlignment = Alignment.Center) {
            Box(Modifier.size(80.dp).background(when (result.riskLevel) {
                RiskLevel.SAFE -> SoftSafe
                RiskLevel.CAUTION -> SoftCaution
                else -> SoftRed
            }, CircleShape), contentAlignment = Alignment.Center) {
                Image(painterResource(when (result.riskLevel) {
                    RiskLevel.SAFE -> R.drawable.dundeuni_mascot_default
                    RiskLevel.CAUTION -> R.drawable.dundeuni_mascot_suspicious
                    RiskLevel.DANGER -> R.drawable.dundeuni_mascot_danger
                    RiskLevel.UNKNOWN -> R.drawable.dundeuni_mascot_search
                }), null, Modifier.size(80.dp))
            }
            if (!safe) {
                Surface(modifier = Modifier.align(Alignment.TopEnd), color = statusColor, shape = CircleShape, border = BorderStroke(2.dp, PageBackground)) {
                    Row(Modifier.padding(horizontal = 8.dp, vertical = 3.dp), horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.CenterVertically) {
                        ResultGlyph(ResultIcon.ALERT, if (result.riskLevel == RiskLevel.CAUTION) Ink else Color.White, 10.dp)
                        Text(result.riskLevel.label, color = if (result.riskLevel == RiskLevel.CAUTION) Ink else Color.White,
                            fontSize = 10.sp, lineHeight = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        if (result.riskLevel != RiskLevel.DANGER) {
            Spacer(Modifier.height(12.dp))
            Surface(shape = CircleShape, color = if (safe) SoftSafe else SoftCaution) {
                Row(Modifier.padding(horizontal = 12.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(Modifier.size(6.dp).background(statusColor, CircleShape))
                    Text(if (safe) "보안 정밀 분석 완료" else "주의 단계 경고", fontSize = 11.sp, lineHeight = 15.sp, color = if (safe) Ink else CautionInk)
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(when (result.riskLevel) {
            RiskLevel.SAFE -> "현재는 안전해 보여요"
            RiskLevel.CAUTION -> "조금 더 확인해보세요"
            RiskLevel.DANGER -> "위험할 가능성이 높아요"
            RiskLevel.UNKNOWN -> "결과 정보를 확인해주세요"
        }, color = Ink, fontSize = 24.sp, lineHeight = 32.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Spacer(Modifier.height(6.dp))
        if (result.summary.isNotBlank()) Text(result.summary, color = Muted, fontSize = 13.sp, lineHeight = 20.sp, textAlign = TextAlign.Center)
    }
}

@Composable
private fun TargetRow(label: String) {
    Surface(color = SoftBlue, shape = RoundedCornerShape(10.dp)) {
        Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.size(30.dp).background(Color(0xFFE3E8F5), RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                ResultGlyph(ResultIcon.MESSAGE, Muted, 17.dp)
            }
            Column(Modifier.weight(1f)) {
                Text("분석 대상", fontSize = 10.sp, lineHeight = 15.sp, color = Muted)
                Text(label, fontSize = 12.sp, lineHeight = 18.sp, color = Ink)
            }
        }
    }
}

@Composable
private fun SummaryBanner(message: String, urgent: Boolean) {
    val tint = if (urgent) Danger else CautionInk
    Surface(color = if (urgent) SoftRed else SoftCaution, shape = RoundedCornerShape(14.dp)) {
        Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            if (urgent) ResultGlyph(ResultIcon.WARNING, Danger, 23.dp)
            else Box(Modifier.size(34.dp).background(SoftCaution, CircleShape), contentAlignment = Alignment.Center) {
                ResultGlyph(ResultIcon.LINK, tint, 20.dp)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(if (urgent) "긴급 조치 권고" else "점검 핵심 요약", fontSize = 11.sp, lineHeight = 15.sp, color = tint)
                Text(message, fontSize = if (urgent) 16.sp else 14.sp, lineHeight = 21.sp,
                    fontWeight = if (urgent) FontWeight.Bold else FontWeight.Medium, color = if (urgent) Danger else Ink)
            }
        }
    }
}

@Composable
private fun MessageQuote(message: String, label: String = "분석된 메시지 내용") {
    ResultPanel(color = SoftBlue) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            ResultGlyph(ResultIcon.MESSAGE, Muted, 16.dp)
            Text(label, color = Muted, fontSize = 11.sp)
        }
        Surface(color = Color.White, shape = RoundedCornerShape(10.dp)) {
            Text("“$message”", Modifier.fillMaxWidth().padding(12.dp), color = Ink, fontSize = 14.sp, lineHeight = 22.sp)
        }
    }
}

@Composable
private fun MetricsRow(result: AnalysisResult, nested: Boolean = false) {
    if (result.historyType == HistoryType.PHONE) {
        LevelCard("전화번호 위험 등급", if (result.riskLevel == RiskLevel.UNKNOWN) "정보 부족" else result.riskLevel.label)
        return
    }
    if (result.historyType == HistoryType.TEXT) {
        result.textRiskScore?.let { TextClassifierScoreCard(it) }
        return
    }
    if (result.imageModelResult) {
        if (result.imageGenerationDecision != null || result.imageManipulationDecision != null) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                result.imageGenerationDecision?.let {
                    ImageModelCard("AI 생성 판정", it, result.imageGenerationDisplayScore, true, Modifier.weight(1f))
                }
                result.imageManipulationDecision?.let {
                    ImageModelCard("이미지 변조 판정", it, result.imageManipulationDisplayScore, false, Modifier.weight(1f))
                }
            }
        }
        return
    }
    if (result.historyType == HistoryType.SCREEN) return
    val fraudLabel = when (result.fraudLevel) {
        RiskLevel.SAFE -> "낮음"
        RiskLevel.CAUTION -> "주의"
        RiskLevel.DANGER -> "높음"
        RiskLevel.UNKNOWN -> "판정 미확정"
        null -> "정보 없음"
    }
    val fraudTint = when (result.fraudLevel) {
        RiskLevel.SAFE -> SafeInk
        RiskLevel.CAUTION -> CautionInk
        RiskLevel.DANGER -> Danger
        else -> ProtectiveBlue
    }
    val danger = result.riskLevel == RiskLevel.DANGER
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        MetricCard(if (danger) "사기 위험" else "사기 위험도", fraudLabel, result.fraudScore, fraudTint,
            if (danger) ResultIcon.ALERT else ResultIcon.SHIELD_OUTLINE, Modifier.weight(1f), nested, result.riskLevel,
            isAi = false)
        MetricCard(if (result.riskLevel == RiskLevel.SAFE) "AI 변조 가능성" else "AI 생성·변조", result.aiRiskLabel ?: result.aiLikelihood.label,
            result.aiScore, if (danger) Ink else ProtectiveBlue, ResultIcon.AI, Modifier.weight(1f), nested, result.riskLevel,
            isAi = true)
    }
}

@Composable
private fun LevelCard(title: String, label: String) {
    ResultPanel {
        Text(title, color = Muted, fontSize = 12.sp)
        Text(label, color = Ink, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun TextClassifierScoreCard(score: Double) {
    ResultPanel {
        Text("텍스트 분류 점수", color = Muted, fontSize = 12.sp)
        Text(score.toString(), color = Ink, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
        Text("0~1 범위의 미보정 분류 점수이며 사기 확률이 아닙니다.", color = Muted, fontSize = 11.sp)
    }
}

@Composable
private fun ImageModelCard(
    title: String, decision: String?, displayScore: Double?, generation: Boolean, modifier: Modifier,
) {
    Surface(modifier = modifier, color = Color.White, shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, CardOutline)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, color = Muted, fontSize = 11.sp, lineHeight = 16.sp)
            imageDecisionLabel(decision, generation)?.let {
                Text(it, color = Ink, fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold)
            }
            displayScore?.let {
                Text("표시 점수 ${formatScore(it)}/100", color = Muted, fontSize = 11.sp, lineHeight = 16.sp)
            }
        }
    }
}

private fun imageDecisionLabel(decision: String?, generation: Boolean): String? =
    when (decision?.trim()?.uppercase(Locale.ROOT)) {
        "AI_GENERATED_SUSPECTED" -> if (generation) "AI 생성 의심" else null
        "NO_CLEAR_GENERATION_SIGNAL" -> if (generation) "뚜렷한 AI 생성 신호 없음" else null
        "MANIPULATION_SUSPECTED" -> if (generation) null else "이미지 변조 의심"
        "NO_CLEAR_MANIPULATION_SIGNAL" -> if (generation) null else "뚜렷한 이미지 변조 신호 없음"
        "INCONCLUSIVE" -> "판정 미확정"
        else -> null
    }

@Composable
private fun MetricCard(
    title: String, label: String, score: Double?, tint: Color, icon: ResultIcon, modifier: Modifier,
    nested: Boolean, level: RiskLevel, isAi: Boolean,
) {
    val danger = level == RiskLevel.DANGER
    Surface(modifier = modifier, color = if (nested) SoftBlue else Color.White, shape = RoundedCornerShape(14.dp),
        border = if (nested) null else BorderStroke(1.dp, CardOutline)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(title, Modifier.weight(1f), fontSize = 11.sp, lineHeight = 16.sp, color = Muted)
                ResultGlyph(icon, if (level == RiskLevel.SAFE) Muted else tint, 16.dp)
            }
            if (danger) {
                Surface(shape = CircleShape, color = if (isAi) Color(0xFFE4E8F4) else SoftRed) {
                    Row(Modifier.padding(horizontal = 10.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        if (!isAi) Box(Modifier.size(6.dp).background(tint, CircleShape))
                        Text(label, color = tint, fontSize = 18.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
                Text(score?.let { "분석 점수 ${formatScore(it)}%" } ?: "점수 정보 없음", color = Muted, fontSize = 10.sp, lineHeight = 15.sp)
            } else {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(label, color = tint, fontSize = 20.sp, lineHeight = 26.sp, fontWeight = FontWeight.SemiBold)
                    Text(when {
                        level == RiskLevel.SAFE && !isAi -> if (label == "정보 없음") "" else "Safe"
                        score == null -> "점수 정보 없음"
                        level == RiskLevel.SAFE -> "${formatScore(score)}%"
                        level == RiskLevel.UNKNOWN -> "분석 점수 ${formatScore(score)}%"
                        isAi -> "변조 점수 ${formatScore(score)}%"
                        else -> "위험 점수 ${formatScore(score)}%"
                    }, Modifier.weight(1f).padding(bottom = 2.dp), color = Muted, fontSize = 10.sp, lineHeight = 14.sp,
                        textAlign = if (nested) TextAlign.End else TextAlign.Start)
                }
                if (score != null) {
                    Box(Modifier.fillMaxWidth().height(5.dp).clip(CircleShape).background(Color(0xFFE4E9F7))
                        .progressSemantics((score / 100).toFloat())) {
                        Box(Modifier.fillMaxWidth((score / 100).toFloat()).fillMaxHeight().clip(CircleShape).background(tint))
                    }
                }
            }
        }
    }
}

internal fun formatScore(score: Double): String = if (score % 1.0 == 0.0) score.toInt().toString() else String.format(Locale.ROOT, "%.1f", score)

@Composable
private fun EvidencePanel(result: AnalysisResult, isDemo: Boolean) {
    val safe = result.riskLevel == RiskLevel.SAFE
    val danger = result.riskLevel == RiskLevel.DANGER
    ResultPanel {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            if (!safe) ResultGlyph(if (danger) ResultIcon.SHIELD_OUTLINE else ResultIcon.SEARCH, if (danger) Danger else Ink, 18.dp)
            Text(when (result.riskLevel) {
                RiskLevel.SAFE -> "세부 항목 검증 내역"
                RiskLevel.CAUTION -> "발견된 주의 사항"
                RiskLevel.DANGER -> "왜 위험한가요?"
                RiskLevel.UNKNOWN -> "분석 근거"
            }, Modifier.weight(1f), fontSize = if (safe) 13.sp else 16.sp, lineHeight = 22.sp,
                fontWeight = if (safe) FontWeight.Medium else FontWeight.SemiBold, color = Ink)
            if (result.evidence.isNotEmpty() && (safe || danger)) {
                Surface(color = if (safe) Color(0xFFE7EEFF) else Color(0xFFE4E8F4), shape = CircleShape) {
                    Text(if (!isDemo) "${result.evidence.size}개 근거" else if (safe) "${result.evidence.size}건 모두 통과" else "${result.evidence.size}가지 단서 감지",
                        Modifier.padding(horizontal = 8.dp, vertical = 2.dp), color = if (safe) ProtectiveBlue else Muted,
                        fontSize = 10.sp, lineHeight = 14.sp)
                }
            }
            if (safe) ResultGlyph(ResultIcon.CHECKLIST, Muted, 17.dp)
        }
        if (result.evidence.isEmpty()) {
            Text("세부 근거가 제공되지 않았어요. 요약과 대응 행동을 확인해주세요.", color = Muted, fontSize = 13.sp, lineHeight = 20.sp)
        }
        result.evidence.forEachIndexed { index, item ->
            Surface(color = if (safe) Color.White else SoftBlue, shape = RoundedCornerShape(10.dp)) {
                Row(Modifier.fillMaxWidth().padding(vertical = if (safe) 10.dp else 12.dp, horizontal = if (safe) 0.dp else 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    Box(Modifier.size(if (safe) 24.dp else 30.dp).background(if (danger) Color(0xFFFFD9D5) else Color(0xFFE3EBFC), CircleShape), contentAlignment = Alignment.Center) {
                        when {
                            safe -> ResultGlyph(ResultIcon.CHECK, ProtectiveBlue, 15.dp)
                            danger -> ResultGlyph(when (index % 3) {
                                0 -> ResultIcon.CLOCK
                                1 -> ResultIcon.FLAG
                                else -> ResultIcon.HISTORY
                            }, Danger, 17.dp)
                            else -> Text("${index + 1}", fontSize = 12.sp, color = ProtectiveBlue, fontWeight = FontWeight.Medium)
                        }
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(item.title, color = Ink, fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium)
                        Text(item.description, color = Muted, fontSize = 12.sp, lineHeight = 19.sp)
                    }
                }
            }
            if (safe && index < result.evidence.lastIndex) HorizontalDivider(color = CardOutline)
        }
    }
}

@Composable
private fun LimitationsPanel(result: AnalysisResult) {
    if (result.textLimitations.isEmpty()) return
    ResultPanel {
        Text("텍스트 분석 주의사항", color = Ink, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        result.textLimitations.forEach { Text(it, color = Muted, fontSize = 13.sp, lineHeight = 20.sp) }
    }
}

@Composable
private fun SafetyTip(guidance: List<String>? = null) {
    ResultPanel(color = SoftBlue) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.size(30.dp).background(Color(0xFFE3E8F5), CircleShape), contentAlignment = Alignment.Center) {
                ResultGlyph(ResultIcon.TIP, Ink, 17.dp)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(if (guidance == null) "든든이 보안 수칙" else "대응 안내", color = Ink, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                Text(guidance?.takeIf { it.isNotEmpty() }?.joinToString("\n")
                    ?: if (guidance != null) "서버에서 대응 안내를 제공하지 않았어요."
                    else "안전하더라도 개인정보나 비밀번호, 계좌 인증번호를 입력하라는 요청은 항상 주의하세요. 공식 창구를 재확인하는 것이 가장 안전합니다.", color = Muted, fontSize = 12.sp, lineHeight = 20.sp)
            }
        }
    }
}

@Composable
private fun ResultActions(onAction: (ResultAction) -> Unit, onSave: () -> Unit, canSave: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ActionButton("원래 화면으로 돌아가기", ResultIcon.BACK, PrimaryMidnight) {
            onAction(ResultAction.RETURN)
        }
        ActionButton("든든이 홈으로", ResultIcon.SHIELD, ProtectiveBlue) {
            onAction(ResultAction.HOME)
        }
        ActionButton(SaveResultImageLabel, null, SaveResultImageColor, enabled = canSave, onClick = onSave)
    }
}

@Composable
private fun ActionButton(
    text: String,
    icon: ResultIcon?,
    background: Color,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
        enabled = enabled,
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(containerColor = background, contentColor = Color.White),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 14.dp),
    ) {
        val easy = LocalEasyMode.current
        if (icon != null) {
            ResultGlyph(icon, Color.White, 16.dp)
            Spacer(Modifier.width(6.dp))
        }
        Text(
            text = text,
            fontSize = if (easy) 18.sp else 13.sp,
            lineHeight = if (easy) 26.sp else 18.sp,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun ResultPanel(color: Color = Color.White, content: @Composable ColumnScope.() -> Unit) {
    Surface(color = color, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth(),
        border = if (color == Color.White) BorderStroke(1.dp, CardOutline) else null) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp), content = content)
    }
}

/** Same service result, with a larger summary, independent metrics and readable evidence. */
@Composable
private fun EasyResultContent(result: AnalysisResult, isDemo: Boolean) {
    val tint = when (result.riskLevel) {
        RiskLevel.SAFE -> ProtectiveBlue
        RiskLevel.CAUTION -> CautionInk
        RiskLevel.DANGER -> Danger
        RiskLevel.UNKNOWN -> Ink
    }
    val badgeLabel = if (result.imageModelResult) {
        result.imageOverallDecisionLabel?.takeIf { it.isNotBlank() } ?: "이미지 분석 완료"
    } else if (result.historyType == HistoryType.TEXT || result.historyType == HistoryType.SCREEN) {
        result.keyFinding ?: "텍스트 분석 완료"
    } else result.riskLevel.label
    if (result.historyType == HistoryType.PHONE && !isDemo) {
        PhoneInspectionSummary(result)
    } else ResultPanel {
        Column(Modifier.fillMaxWidth().padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Surface(color = SoftBlue, shape = CircleShape) {
                Text("분석 결과 · $badgeLabel", Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    color = tint, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            }
            Image(painterResource(when (result.riskLevel) {
                RiskLevel.SAFE -> R.drawable.dundeuni_mascot_default
                RiskLevel.CAUTION -> R.drawable.dundeuni_mascot_suspicious
                RiskLevel.DANGER -> R.drawable.dundeuni_mascot_danger
                RiskLevel.UNKNOWN -> R.drawable.dundeuni_mascot_search
            }), null, Modifier.size(112.dp))
            Text(result.imageOverallDecisionLabel?.takeIf { result.imageModelResult && it.isNotBlank() }
                ?: if (result.imageModelResult) "이미지 분석 완료"
                else if (result.historyType == HistoryType.TEXT || result.historyType == HistoryType.SCREEN)
                    result.keyFinding ?: "텍스트 분석 완료"
                else when (result.riskLevel) {
                    RiskLevel.SAFE -> "지금 보신 내용은\n안전해 보여요"
                    RiskLevel.CAUTION -> "조금 더\n확인해보세요"
                    RiskLevel.DANGER -> "위험할 가능성이\n높아요"
                    RiskLevel.UNKNOWN -> "결과 정보를\n확인해주세요"
                }, color = tint, fontSize = 28.sp, lineHeight = 38.sp,
                fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            if (result.summary.isNotBlank()) Text(result.summary, color = Ink, fontSize = 20.sp, lineHeight = 30.sp, textAlign = TextAlign.Center)
            result.keyFinding?.takeIf {
                result.historyType != HistoryType.PHONE && it.isNotBlank() && it != result.summary
            }?.let {
                Text(it, color = Ink, fontSize = 20.sp, lineHeight = 30.sp, textAlign = TextAlign.Center)
            }
            if (result.targetLabel.isNotBlank()) {
                Text("분석 대상: ${result.targetLabel}", color = Muted, fontSize = 16.sp, lineHeight = 24.sp)
            }
        }
    }
    val fraudLabel = when (result.fraudLevel) {
        RiskLevel.SAFE -> "낮음"
        RiskLevel.CAUTION -> "주의"
        RiskLevel.DANGER -> "높음"
        RiskLevel.UNKNOWN -> "판정 미확정"
        null -> "정보 없음"
    }
    if (result.historyType == HistoryType.PHONE) {
        if (isDemo) {
            LevelCard("전화번호 위험 등급", if (result.riskLevel == RiskLevel.UNKNOWN) "정보 부족" else result.riskLevel.label)
        }
    } else if (result.historyType == HistoryType.TEXT) {
        result.textRiskScore?.let { TextClassifierScoreCard(it) }
    } else if (result.imageModelResult) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            result.imageGenerationDecision?.let {
                ImageModelCard("AI 생성 판정", it, result.imageGenerationDisplayScore, true, Modifier.weight(1f))
            }
            result.imageManipulationDecision?.let {
                ImageModelCard("이미지 변조 판정", it, result.imageManipulationDisplayScore, false, Modifier.weight(1f))
            }
        }
    } else if (isDemo) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            EasyMetric("사기 위험도", fraudLabel, result.fraudScore, Modifier.weight(1f))
            EasyMetric("AI 생성·변조 가능성", result.aiRiskLabel ?: result.aiLikelihood.label,
                result.aiScore, Modifier.weight(1f))
        }
    }
    if (result.evidence.isNotEmpty() || isDemo) ResultPanel {
        Text("분석에서 확인한 내용", color = Ink, fontSize = 22.sp, lineHeight = 30.sp, fontWeight = FontWeight.Bold)
        if (result.evidence.isEmpty()) {
            Text("세부 근거가 제공되지 않았어요. 위의 요약과 아래 안내를 확인해주세요.",
                color = Muted, fontSize = 18.sp, lineHeight = 28.sp)
        }
        result.evidence.forEachIndexed { index, evidence ->
            Surface(color = SoftBlue, shape = RoundedCornerShape(12.dp)) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("${index + 1}. ${evidence.title}", color = Ink, fontSize = 20.sp,
                        lineHeight = 28.sp, fontWeight = FontWeight.SemiBold)
                    Text(evidence.description, color = Muted, fontSize = 18.sp, lineHeight = 28.sp)
                }
            }
        }
    }
    LimitationsPanel(result)
    result.sourceText?.takeIf { it.isNotBlank() }?.let {
        ResultPanel {
            Text("분석된 내용", color = Ink, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text(it, color = Ink, fontSize = 18.sp, lineHeight = 28.sp)
        }
    }
    if (isDemo) ResultPanel(color = SoftBlue) {
        Text("이렇게 확인하세요", color = Ink, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text(when (result.riskLevel) {
            RiskLevel.SAFE -> "낯선 요청은 한 번 더 확인하세요. 비밀번호나 인증번호는 알려주지 마세요."
            RiskLevel.CAUTION -> "의심스러운 링크는 누르지 말고, 공식 연락처나 믿을 수 있는 사람에게 확인하세요."
            RiskLevel.DANGER -> "송금을 멈추고, 가족이나 기관의 공식 연락처로 직접 확인하세요."
            RiskLevel.UNKNOWN -> "판정 정보가 충분하지 않아요. 요청한 사람이나 기관의 공식 연락처로 확인하세요."
        }, color = Ink, fontSize = 20.sp, lineHeight = 30.sp)
        Text("분석은 참고 정보이며 확정 판정이 아니에요. 사기 위험과 AI 생성·변조 가능성은 서로 다른 지표예요.",
            color = Muted, fontSize = 18.sp, lineHeight = 28.sp)
    }
}

@Composable
private fun EasyMetric(title: String, label: String, score: Double?, modifier: Modifier) {
    Surface(modifier = modifier, color = Color.White, shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, color = Muted, fontSize = 16.sp, lineHeight = 24.sp)
            Text(label, color = Ink, fontSize = 24.sp, lineHeight = 32.sp, fontWeight = FontWeight.Bold)
            Text(score?.let { "분석 점수 ${formatScore(it)}%" } ?: "점수 정보 없음",
                color = Muted, fontSize = 16.sp, lineHeight = 24.sp)
        }
    }
}
