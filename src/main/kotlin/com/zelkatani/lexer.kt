package com.zelkatani

import com.zelkatani.Token.*
import java.io.File

enum class Token {
    SPACE, TAB, LINEFEED
}

class Lexer {
    infix fun lex(text: String): List<Token> {
        val lexed = mutableListOf<Token>()

        text.forEach {
            when (it) {
                ' ' -> lexed.add(SPACE)
                '\t' -> lexed.add(TAB)
                '\n' -> lexed.add(LINEFEED)
            }
        }

        return lexed
    }

    infix fun lex(file: File): List<Token> {
        if (file.isDirectory) {
            throw IllegalArgumentException("Expected file, but got a directory.")
        }

        val text = file.readText()
        return lex(text)
    }
}