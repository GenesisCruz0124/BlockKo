package com.blockko.app.ui.strings

import androidx.compose.runtime.compositionLocalOf
import com.blockko.app.data.datastore.AppLanguage

val LocalStrings = compositionLocalOf { EnglishStrings }

fun stringsFor(language: AppLanguage): BlockKoStrings = when (language) {
    AppLanguage.EN -> EnglishStrings
    AppLanguage.TL -> TaglishStrings
}
