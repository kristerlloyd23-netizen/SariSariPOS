package com.sarisari.pos.domain

object Categories {
    const val SNACKS = "Snacks"
    const val DRINKS = "Drinks"
    const val CANNED = "Canned"
    const val TOILETRIES = "Toiletries"
    const val CIGARETTES = "Cigarettes"
    const val OTHERS = "Others"

    val all: List<String> = listOf(SNACKS, DRINKS, CANNED, TOILETRIES, CIGARETTES, OTHERS)

    fun emoji(category: String): String = when (category) {
        SNACKS -> "🍿"
        DRINKS -> "🥤"
        CANNED -> "🥫"
        TOILETRIES -> "🧼"
        CIGARETTES -> "🚬"
        else -> "🛒"
    }
}
