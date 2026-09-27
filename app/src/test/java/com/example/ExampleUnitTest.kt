package com.example

import com.example.core.ai.DeterministicEvaluator
import com.example.core.model.Question
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun testExactMatch() {
        val question = Question(
            id = "q1",
            type = "fill_blank",
            questionText = "What is the capital of Bangladesh?",
            answer = "Dhaka",
            acceptedAnswers = listOf("Dhaka", "ঢাকা")
        )
        val result = DeterministicEvaluator.evaluate(question, "Dhaka")
        assertNotNull(result)
        assertEquals("CORRECT", result?.status)
    }

    @Test
    fun testBengaliNumeralAndTextNormalization() {
        val question = Question(
            id = "q2",
            type = "fill_blank",
            questionText = "২ + ২ কত হয়?",
            answer = "4",
            acceptedAnswers = listOf("4", "৪", "চার", "four")
        )

        // Bengali numeral ৪
        val res1 = DeterministicEvaluator.evaluate(question, "৪")
        assertNotNull(res1)
        assertEquals("CORRECT", res1?.status)

        // English text "four"
        val res2 = DeterministicEvaluator.evaluate(question, "four")
        assertNotNull(res2)
        assertEquals("CORRECT", res2?.status)

        // Bengali word "চার"
        val res3 = DeterministicEvaluator.evaluate(question, "চার")
        assertNotNull(res3)
        assertEquals("CORRECT", res3?.status)
    }

    @Test
    fun testMcqIncorrectOption() {
        val question = Question(
            id = "q3",
            type = "mcq",
            questionText = "নিচের কোনটি অমূলদ সংখ্যা?",
            options = listOf("√4", "√9", "√2", "4.5"),
            answer = "√2",
            acceptedAnswers = listOf("√2")
        )
        val result = DeterministicEvaluator.evaluate(question, "√4")
        assertNotNull(result)
        assertEquals("INCORRECT", result?.status)
    }
}
