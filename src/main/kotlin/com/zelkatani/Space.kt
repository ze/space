package com.zelkatani

import java.io.File

class Space {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            if (args.size != 1) {
                throw IllegalArgumentException("Input file not provided.")
            }

            val file = File(args[0])
            val lexer = Lexer()

            val tokens = lexer lex file
            val parser = Parser()

            val program = parser parse tokens
            program.evaluate()
        }
    }
}
