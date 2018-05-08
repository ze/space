package com.zelkatani

import com.zelkatani.Instruction.*
import com.zelkatani.LabeledInstruction.*
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class MainTest {
    private val lexer = Lexer()

    private val outputStream = ByteArrayOutputStream()
    private val parser = Parser(outputStream)

    init {
        System.setIn(ByteArrayInputStream("Z".toByteArray()))
    }

    @BeforeEach
    private fun wipeOutputStream() {
        outputStream.reset()
    }

    @AfterAll
    private fun closeOutputStream() {
        outputStream.close()
    }

    private fun getWSFile(name: String): File {
        val classLoader = javaClass.classLoader
        return File(classLoader.getResource("$name.ws").file)
    }

    @Test
    fun `Test Lexer`() {
        val helloWorldFile = getWSFile("hworld")

        val lexed = lexer lex helloWorldFile
        assertNotNull(lexed)
    }

    @Test
    fun `Test Parser`() {
        val helloWorldFile = getWSFile("hworld")

        val lexed = lexer lex helloWorldFile

        val program = parser parse lexed
        program.evaluate()

        checkOutputStream("Hello world!")
    }

    @Test
    fun `Test Numbers`() {
        val numberFile = getWSFile("numbers")
        val tokens = lexer lex numberFile
        assertTrue(tokens.size == 9)

        val program = parser parse tokens
        Assertions.assertTrue {
            val instruction = program.instructions[0]

            instruction is PushNumberInstruction &&
                    instruction.number == 19 &&
                    program.instructions.size == 1
        }
    }

    @Test
    fun `Test Program File Export`() {
        val helloWorldFile = getWSFile("hworld")
        val lexed = lexer lex helloWorldFile

        val program = parser parse lexed
        val builder = ProgramBuilder(program)

        val tempFile = File.createTempFile("space-", ".ws")
        builder.exportToFile(tempFile.absolutePath)

        val exportLexed = lexer lex tempFile
        val exportProgram = parser parse exportLexed

        exportProgram.evaluate()

        checkOutputStream("Hello world!")

        tempFile.delete()
    }

    @Test
    fun `Test Program Generation`() {
        val program = buildProgram(outputStream) {
            numbers(0, 33, 100, 108, 114, 111, 119, 32, 111, 108, 108, 101, 72)

            +CallSubroutineInstruction(0)

            label(0) {
                +DuplicateInstruction()
                +JumpZeroInstruction(1)
                +PrintCharInstruction()
                +JumpInstruction(0)
            }

            label(1) {
                +DiscardInstruction()
                +EndProgramInstruction()
            }
        }

        program.evaluate()

        checkOutputStream("Hello world!")
    }


    @Test
    fun `Test Input`() {
        val program = buildProgram(outputStream) {
            number(0)
            +ReadCharInstruction()

            number(0)
            +RetrieveInstruction()
            +PrintCharInstruction()
            +EndProgramInstruction()
        }

        program.evaluate()

        checkOutputStream("Z")
    }

    @Test
    fun `Test Program to Whitespace`() {
        val stringProgram = "    \n" + // push 0
                "   \t    \t\n" + // push 33
                "   \t\t  \t  \n" + // push 100
                "   \t\t \t\t  \n" + // push 108
                "   \t\t\t  \t \n" + // push 114
                "   \t\t \t\t\t\t\n" + // push 111
                "   \t\t\t \t\t\t\n" + // push 119
                "   \t     \n" + // push 32
                "   \t\t \t\t\t\t\n" + // push 111
                "   \t\t \t\t  \n" + // push 108
                "   \t\t \t\t  \n" + // push 108
                "   \t\t  \t \t\n" + // push 101
                "   \t  \t   \n" + // push 72
                "\n \t  \n" + // call label 0
                "\n    \n" + // define label 0
                " \n " + // duplicate
                "\n\t  \t\n" + // if zero jump to label 1
                "\t\n  " + // print char
                "\n \t  \n" + // jump to label 0
                "\n   \t\n" + // define label 1
                " \n\n" + // discard
                "\n\n\n" // end program

        val lexed = lexer lex stringProgram
        val program = parser parse lexed

        assertEquals(stringProgram.toVisible(), program.toWhitespace().toVisible())
    }

    @Test
    fun `Test Nonexistent Label`() {
        // We ignore MarkLocationInstruction because it is internal and won't be usable outside the module (exc. Test Module)
        val labeledInstructions = listOf(CallSubroutineInstruction(0), JumpInstruction(0),
                JumpZeroInstruction(0), JumpNegativeInstruction(0))

        for (label: LabeledInstruction in labeledInstructions) {
            val program = buildProgram {
                +label
            }

            assertProgramError(program)
        }
    }

    @Test
    fun `Test Empty Stack`() {
        val program = buildProgram {
            +DuplicateInstruction()
        }

        assertProgramError(program)
    }

    @Test
    fun `Test Retrieve Nonexistent Address`() {
        val program = buildProgram {
            number(10)
            +RetrieveInstruction()
        }

        assertProgramError(program)
    }

    @Test
    fun `Test Read Number Instruction`() {
        System.setIn(ByteArrayInputStream("Zoop".toByteArray()))

        val program = buildProgram(outputStream) {
            number(0)
            +ReadNumberInstruction()

            number(0)
            +RetrieveInstruction()
            +PrintCharInstruction()
            +EndProgramInstruction()
        }

        assertProgramError(program)
    }

    private fun assertProgramError(program: Program) {
        assertThrows(ProgramError::class.java) {
            program.evaluate()
        }
    }

    private fun checkOutputStream(expect: String) {
        assertEquals(expect, outputStream.toString())
    }

    private fun String.toVisible() = replace(' ', 'S').replace('\t', 'T').replace("\n", "L\n")
    private fun Int.toVisibleWhitespace() = toWhitespace().toVisible()

    @Test
    fun `Test Int to Whitespace`() {
        assertEquals("STSTSL\n", (10).toVisibleWhitespace())
        assertEquals("STTSSTSSL\n", (100).toVisibleWhitespace())
    }
}