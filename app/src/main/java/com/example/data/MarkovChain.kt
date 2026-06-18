package com.example.data

import kotlin.random.Random

class MarkovChain(val order: Int = 2) {
    private val chain = mutableMapOf<List<String>, MutableList<String>>()
    private val startStates = mutableListOf<List<String>>()

    fun train(texts: List<String>) {
        texts.forEach { text ->
            if (text.isBlank()) return@forEach
            val words = text.split(Regex("\\s+"))
                .map { it.trim() }
                .filter { it.isNotEmpty() }
            
            if (words.size > order) {
                val startState = words.subList(0, order)
                startStates.add(startState)
                
                for (i in 0 until words.size - order) {
                    val state = words.subList(i, i + order)
                    val next = words[i + order]
                    chain.getOrPut(state) { mutableListOf() }.add(next)
                }
            } else if (words.isNotEmpty()) {
                val miniState = words.subList(0, words.size - 1)
                if (miniState.isNotEmpty()) {
                    chain.getOrPut(miniState) { mutableListOf() }.add(words.last())
                }
            }
        }
    }

    fun generate(): String {
        if (chain.isEmpty()) {
            return listOf(
                "Подключение стабильно. Векторы доверия сходятся в точке сингулярности.",
                "Mainframe status: active. Neural matrix synchronizing with standard node protocols.",
                "Эксклюзивный доступ получен. Потоки данных калибруют логику.",
                "Алгоритмы самообучения завершили цикл обратной связи.",
                "Matrix ping: 12ms. Trust core online."
            ).random()
        }
        
        var state = if (startStates.isNotEmpty() && Random.nextFloat() < 0.8f) {
            startStates.random()
        } else {
            chain.keys.toList().random()
        }
        
        val result = state.toMutableList()
        val maxWords = Random.nextInt(15, 35)
        var terminated = false
        
        repeat(maxWords) {
            if (!terminated) {
                val nextOptions = chain[state]
                if (nextOptions.isNullOrEmpty()) {
                    if (state.size > 1) {
                        val relaxedState = state.takeLast(1)
                        val relaxedOptions = chain[relaxedState]
                        if (!relaxedOptions.isNullOrEmpty()) {
                            val next = relaxedOptions.random()
                            result.add(next)
                            state = result.takeLast(order)
                        } else {
                            terminated = true
                        }
                    } else {
                        terminated = true
                    }
                } else {
                    val next = nextOptions.random()
                    result.add(next)
                    state = result.takeLast(order)
                }
            }
        }
        
        val rawSentence = result.joinToString(" ").trim()
        if (rawSentence.isBlank()) return "Data sync verified."
        
        var sentence = rawSentence.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        
        val lastChar = sentence.last()
        if (lastChar != '.' && lastChar != '!' && lastChar != '?' && lastChar != ')') {
            sentence += "."
        }
        
        return sentence
    }
}
