package com.klein.ictcomputing.ui

object SubjectLabels {
    fun display(subject: String): String = when (subject) {
        "openEuler" -> "openEuler"
        "openGauss" -> "openGauss"
        "Kunpeng" -> "Kunpeng"
        else -> subject
    }
}
