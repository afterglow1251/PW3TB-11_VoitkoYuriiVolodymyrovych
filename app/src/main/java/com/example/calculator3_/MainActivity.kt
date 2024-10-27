package com.example.calculator3_

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.calculator3_.ui.theme.Calculator3_Theme
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.sqrt

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Calculator3_Theme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Calculator(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@SuppressLint("DefaultLocale")
@Composable
fun Calculator(modifier: Modifier = Modifier) {
    var inputValues by remember { mutableStateOf(mapOf<String, String>()) }
    var inputErrors by remember { mutableStateOf(mapOf<String, String>()) }
    var resultText by remember { mutableStateOf("") }
    var showResult by remember { mutableStateOf(false) }

    // Функція для валідації введеного значення
    fun validateInput(value: String): String {
        return if (value.isBlank() || value.toDoubleOrNull() == null || value.toDouble() < 0) {
            "Enter a non-negative number"
        } else {
            ""
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Top
    ) {
        // Поля введення
        val inputs = mapOf(
            "Pc" to "Середньодобова потужність, (МВт)",
            "Sigma" to "Cередньоквадратичне відхилення, (МВт)",
            "B" to "Вартість електроенергії, (грн/кВт*год)"
        )

        inputs.forEach { (label, displayLabel) ->
            val inputValue = inputValues[label] ?: ""
            val inputError = inputErrors[label] ?: ""

            OutlinedTextField(
                value = inputValue,
                onValueChange = { newValue ->
                    // Оновлення тільки поточного значення
                    inputValues = inputValues.toMutableMap().apply { put(label, newValue) }

                    // Оновлення тільки помилки для певного поля
                    val error = validateInput(newValue)
                    inputErrors = inputErrors.toMutableMap().apply { put(label, error) }

                    // Приховування результату лише у разі зміни значення або наявності помилки
                    if (error.isNotEmpty() || newValue != inputValue) {
                        showResult = false
                    }
                },
                label = { Text(displayLabel) },
                keyboardOptions = KeyboardOptions.Default.copy(
                    keyboardType = KeyboardType.Decimal,
                ),
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(text = "Введіть значення", color = Color.Gray)
                },
                supportingText = {
                    if (inputError.isNotEmpty()) {
                        Text(text = inputError, color = Color.Red)
                    }
                },
            )
        }

        // Кнопка "Обчислити"
        Button(
            onClick = {
                // Перевірка на наявність помилок у кожному полі
                val errors = inputs.keys.associateWith { validateInput(inputValues[it] ?: "") }
                inputErrors = errors

                // Якщо немає помилок, виконуються обчислення
                if (errors.values.all { it.isEmpty() }) {
                    val Pc = inputValues["Pc"]?.toDouble() ?: 0.0
                    val Sigma = inputValues["Sigma"]?.toDouble() ?: 0.0
                    val B = inputValues["B"]?.toDouble() ?: 0.0

                    // Частка енергії, що генерується без небалансів
                    val balancedEnergyShare = integrateEnergyShare(
                        function = { power -> calculateNormalDistribution(power, Pc, Sigma) },
                        averagePower = Pc,
                        totalSteps = 10000
                    )

                    val revenue = Pc * 24 * balancedEnergyShare * B
                    val fine = Pc * 24 * (1 - balancedEnergyShare) * B
                    val profit = revenue - fine


                    // Форматування результату з округленням до десятих
                    resultText = """
                    Дохід: ${String.format("%.1f", revenue)} тис. грн
                    Штраф: ${String.format("%.1f", fine)} тис. грн
                    Прибуток${if (profit < 0) " (збиток)" else ""}: ${String.format("%.1f", profit)} тис. грн 
                    """.trimIndent()

                    showResult = true
                }

            },
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(top = 16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Black
            )
        ) {
            Text(text = "Обчислити")
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Виведення результатів
        if (showResult) {
            Text(text = resultText, color = Color.Black)
        }
    }
}

// Нормальний закон розподілу потужності
fun calculateNormalDistribution(
    power: Double,
    averagePower: Double,
    standardDeviation: Double
): Double {
    return (1 / (standardDeviation * sqrt(2 * PI))) * exp(
        -((power - averagePower).pow(2)) / (2 * standardDeviation.pow(
            2
        ))
    )
}

// Обчислення інтегралу функції за допомогою методу трапецій
fun integrateEnergyShare(
    function: (Double) -> Double,
    averagePower: Double,
    totalSteps: Int,
    deviationFactor: Double = 0.05
): Double {
    val lowerLimit = averagePower * (1 - deviationFactor)
    val upperLimit = averagePower * (1 + deviationFactor)
    val stepSize = (upperLimit - lowerLimit) / totalSteps
    var result = 0.0

    for (i in 0 until totalSteps) {
        val currentPoint = lowerLimit + i * stepSize
        val nextPoint = currentPoint + stepSize
        result += 0.5 * (function(currentPoint) + function(nextPoint)) * stepSize
    }

    return result
}


