package com.dundueni.app.feature.analysisresult

/** Fictional examples only. Kept in debug sources so they are excluded from release builds. */
object AnalysisResultSamples {
    fun forLevel(level: RiskLevel): AnalysisResult = when (level) {
        RiskLevel.SAFE -> AnalysisResult(
            id = "demo-safe", riskLevel = level, fraudLevel = level,
            summary = "확인된 뚜렷한 위험 요소나 사기 패턴이 없어요.", fraudScore = 4.0,
            aiLikelihood = AiLikelihood.LOW, aiScore = 0.8,
            evidence = listOf(
                RiskEvidence("의심스러운 금융·송금 요구 없음", "자금 이체, 기프트카드, 가상자산 관련 유도 문맥이 검출되지 않았습니다."),
                RiskEvidence("피싱 및 스미싱 악성 링크 없음", "제공된 점검 결과에서 알려진 악성 링크가 발견되지 않았습니다."),
                RiskEvidence("비정상 발신자 및 사칭 패턴 없음", "공공기관, 금융사, 택배사를 사칭하는 전형적 화법이 포함되지 않았습니다."),
            ),
        )
        RiskLevel.CAUTION -> AnalysisResult(
            id = "demo-caution", riskLevel = level, fraudLevel = level,
            keyFinding = "알 수 없는 단축 URL이 포함되어 있어 연결 사이트를 주의해야 해요.",
            summary = "불명확한 발신자가 포함되어 있어요.", fraudScore = 64.0,
            aiLikelihood = AiLikelihood.MEDIUM, aiScore = 28.0,
            sourceUrl = "https://bit.ly/example-preview",
            evidence = listOf(
                RiskEvidence("단축 링크(bit.ly 등)로 실제 연결 주소가 숨겨져 있어요", "클릭 시 피싱 사이트나 악성 앱 설치 페이지로 우회될 수 있습니다."),
                RiskEvidence("최근 등록된 지 3일 미만인 신규 도메인이에요", "생성된 지 얼마 안 된 도메인은 임시 피싱 페이지로 자주 활용됩니다."),
                RiskEvidence("공식 고객센터 문의 양식이 아니에요", "공공기관 및 금융사는 개인 번호나 비공식 메신저로 링크를 보내지 않습니다."),
            ),
        )
        RiskLevel.DANGER -> AnalysisResult(
            id = "demo-danger", riskLevel = level, fraudLevel = level,
            summary = "가족을 사칭하면서 급하게 송금을 요구하고 있어요.", fraudScore = 96.0,
            aiLikelihood = AiLikelihood.LOW,
            sourceText = "엄마 나 액정 깨져서 수리 맡겼어… 계좌로 95만원 송금해줘",
            evidence = listOf(
                RiskEvidence("급하게 송금을 재촉하고 있어요", "수리비 결제 등 시간 압박을 주어 정상적인 판단을 방해합니다."),
                RiskEvidence("평소 사용하던 연락처가 아닌 번호예요", "휴대폰 고장을 핑계로 다른 번호나 링크 접속을 유도하는 전형적 수법입니다."),
                RiskEvidence("본인 확인 전 인증번호·송금을 유도해요", "직접 통화하지 않고 텍스트로만 타인 명의 계좌 송금을 요구하고 있습니다."),
            ),
        )
    }
}
