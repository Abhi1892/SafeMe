package com.abhishek.appriskanalyzer.model

enum class RiskLevel(val label: String, val color: Long) {
    LOW("LOW", 0xFF4CAF50),
    MEDIUM("MEDIUM", 0xFFFF9800),
    HIGH("HIGH", 0xFFf44336),
    CRITICAL("CRITICAL", 0xFF9C27B0)
}