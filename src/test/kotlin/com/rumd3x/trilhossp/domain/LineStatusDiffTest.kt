package com.rumd3x.trilhossp.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LineStatusDiffTest {
    private fun status(situation: String) =
        LineStatus(
            situation = situation,
            classification = "",
            isNormal = true,
            updatedAt = "",
        )

    private fun diff(
        old: String,
        new: String,
    ) = LineStatusDiff(
        oldStatus = status(old),
        newStatus = status(new),
    )

    // isNeutral

    @Test fun `isNeutral when operation closes for the night`() = assertTrue(diff("Operação Normal", "Encerrada").isNeutral())

    @Test fun `isNeutral when operation resumes after overnight closure`() = assertTrue(diff("Encerrada", "Operação Normal").isNeutral())

    @Test fun `isNeutral is case insensitive`() = assertTrue(diff("Operação Normal", "ENCERRADA").isNeutral())

    @Test fun `isNeutral false when recovering from disruption`() = assertFalse(diff("Lentidão", "Operação Normal").isNeutral())

    // isPositive

    @Test fun `isPositive when recovering from disruption`() = assertTrue(diff("Lentidão", "Operação Normal").isPositive())

    @Test fun `isPositive false when resuming after overnight closure`() = assertFalse(diff("Encerrada", "Operação Normal").isPositive())

    @Test fun `isPositive false when new status is not normal`() = assertFalse(diff("Operação Normal", "Lentidão").isPositive())

    // isNegative

    @Test fun `isNegative for slowdown`() = assertTrue(diff("Operação Normal", "Lentidão").isNegative())

    @Test fun `isNegative false for positive change`() = assertFalse(diff("Lentidão", "Operação Normal").isNegative())

    @Test fun `isNegative false for neutral change`() = assertFalse(diff("Operação Normal", "Encerrada").isNegative())

    @Test fun `isNegative false for unknown`() = assertFalse(diff("Operação Normal", "Dados Indisponíveis").isNegative())

    // isReallyBad

    @Test fun `isReallyBad for paralisada`() = assertTrue(diff("Operação Normal", "Paralisada").isReallyBad())

    @Test fun `isReallyBad for paralizad typo variant`() = assertTrue(diff("Operação Normal", "Paralizado").isReallyBad())

    @Test fun `isReallyBad for operacao parcial`() = assertTrue(diff("Operação Normal", "Operação Parcial").isReallyBad())

    @Test fun `isReallyBad false for slowdown`() = assertFalse(diff("Operação Normal", "Lentidão").isReallyBad())

    // level

    @Test fun `level 0 for neutral`() = assertEquals(0, diff("Encerrada", "Operação Normal").level)

    @Test fun `level 1 for positive`() = assertEquals(1, diff("Lentidão", "Operação Normal").level)

    @Test fun `level 2 for negative`() = assertEquals(2, diff("Operação Normal", "Lentidão").level)

    @Test fun `level 3 for really bad`() = assertEquals(3, diff("Operação Normal", "Paralisada").level)

    @Test fun `level 4 for dados indisponiveis`() = assertEquals(4, diff("Operação Normal", "Dados Indisponíveis").level)

    @Test fun `isUnknown is case sensitive - lowercase falls through to negative`() =
        assertEquals(2, diff("Operação Normal", "dados indisponíveis").level)
}
