package com.zelkatani

import com.zelkatani.Instruction.*
import com.zelkatani.LabeledInstruction.*
import com.zelkatani.ProgramBuilder.InstructionBuilder
import java.io.File
import java.io.OutputStream
import java.util.*
import com.zelkatani.Token.LINEFEED as LF
import com.zelkatani.Token.SPACE as S
import com.zelkatani.Token.TAB as T

class Program(
        val instructions: List<Instruction>,
        val labels: Map<Int, Label>,
        private val out: OutputStream = System.out
) {
    private val stack = Stack<Int>()
    private val heap = mutableMapOf<Int, Int>()

    fun evaluate() {
        // I'm going to ignore division by zero because the base Java exception is good enough.
        try {
            evaluate(instructions)
        } catch (end: ProgramEnd) {
            // This is when we receive a `EndProgramInstruction`.
            // It's better off to throw a throwable rather than exit the program entirely.
        } catch (empty: EmptyStackException) {
            throw ProgramError("Attempted to pop item from stack, but stack was empty.")
        } catch (nfe: NumberFormatException) {
            throw ProgramError("Asked for a number but did not receive one.")
        }
    }

    private fun evaluate(instrs: List<Instruction>) {
        // A small helper function to call a label
        fun callLabel(labelId: Int) {
            if (labelId !in labels) {
                throw ProgramError("Label with id: $labelId does not exist.")
            }

            evaluate(labels[labelId]!!.instructions)
        }

        instrs.forEach {
            when (it) {
                is PushNumberInstruction -> stack.push(it.number)
                is DuplicateInstruction -> {
                    val top = stack.pop()
                    stack.push(top)
                    stack.push(top)
                }

                is SwapInstruction -> {
                    val first = stack.pop()
                    val second = stack.pop()
                    stack.push(first)
                    stack.push(second)
                }

                is DiscardInstruction -> stack.pop()
                is AdditionInstruction -> stack.push(stack.pop() + stack.pop())
                is SubtractionInstruction -> {
                    val right = stack.pop()
                    val left = stack.pop()
                    stack.push(left - right)
                }

                is MultiplicationInstruction -> stack.push(stack.pop() * stack.pop())
                is IntDivisionInstruction -> {
                    val divisor = stack.pop()
                    val dividend = stack.pop()
                    stack.push(dividend / divisor)
                }

                is ModuloInstruction -> {
                    val divisor = stack.pop()
                    val dividend = stack.pop()
                    stack.push(dividend % divisor)
                }

                is StoreInstruction -> {
                    val value = stack.pop()
                    val address = stack.pop()
                    heap[address] = value
                }

                is RetrieveInstruction -> {
                    val address = stack.pop()
                    val value = heap[address] ?: throw ProgramError("Address `$address` is not stored in the heap.")

                    stack.push(value)
                }

                is CallSubroutineInstruction -> callLabel(it.labelId)
                is JumpInstruction -> callLabel(it.labelId)
                is JumpZeroInstruction -> {
                    val value = stack.pop()
                    if (value == 0) {
                        callLabel(it.labelId)
                    }
                }

                is JumpNegativeInstruction -> {
                    val value = stack.pop()
                    if (value < 0) {
                        callLabel(it.labelId)
                    }
                }

                is EndSubroutineInstruction -> return
                is EndProgramInstruction -> throw ProgramEnd()

                is PrintCharInstruction -> {
                    val toChar = stack.pop().toChar()
                    out.write(toChar.toString().toByteArray())
                }

                is PrintNumberInstruction -> out.write(stack.pop())
                is ReadCharInstruction -> {
                    val address = stack.pop()
                    val char = readLine()!!.toCharArray()[0]
                    heap[address] = char.toInt()
                }

                is ReadNumberInstruction -> {
                    val address = stack.pop()
                    val num = readLine()!!.split(' ').map(String::toInt)[0]
                    heap[address] = num
                }
            }
        }
    }

    private class ProgramEnd : Throwable()

    override fun toString() = buildString {
        instructions.forEach {
            appendln(it)
        }

        labels.values.forEach {
            appendln(it)
        }
    }

    fun toWhitespace() = buildString {
        instructions.forEach {
            append(it.stringForm)
        }

        labels.values.forEach {
            append(it.stringForm)
            it.instructions.forEach {
                append(it.stringForm)
            }
        }
    }
}

class ProgramError(override val message: String? = "Program error") : Exception(message)

fun buildProgram(out: OutputStream = System.out, block: InstructionBuilder.() -> Unit) = ProgramBuilder(out).let {
    it.add(block)
    it.build()
}

class ProgramBuilder(private val out: OutputStream = System.out) {
    private val instructions = mutableListOf<Instruction>()
    private val labels = mutableMapOf<Int, Label>()

    constructor(program: Program, out: OutputStream = System.out) : this(out) {
        instructions.addAll(program.instructions)
        labels.putAll(program.labels)
    }

    fun build() = Program(instructions, labels, out)

    fun add(block: InstructionBuilder.() -> Unit) {
        val instructionBuilder = InstructionBuilder()
        instructionBuilder.block()

        instructions.addAll(instructionBuilder.instructions)
        labels.putAll(instructionBuilder.labels)
    }

    fun exportToFile(path: String): File {
        val file = File(path)
        if (file.exists()) {
            file.writeText(build().toWhitespace())
        }

        return file
    }

    class InstructionBuilder {
        private val _instructions = mutableListOf<Instruction>()
        val instructions: List<Instruction>
            get() = _instructions

        private val _labels = mutableMapOf<Int, Label>()
        val labels: Map<Int, Label>
            get() = _labels

        fun number(number: Int) = _instructions.add(PushNumberInstruction(number))
        fun numbers(vararg numbers: Int) = numbers.forEach {
            number(it)
        }

        fun label(id: Int, block: LabelBuilder.() -> Unit) {
            val instruction = MarkLocationInstruction(id)
            val label = Label(id, instruction.stringForm)

            val labelBuilder = LabelBuilder()
            labelBuilder.block()

            labelBuilder.instructions.forEach {
                label.add(it)
            }

            _labels[id] = label
        }

        operator fun Instruction.unaryPlus() {
            _instructions += this
        }
    }

    class LabelBuilder {
        private val _instructions = mutableListOf<Instruction>()
        val instructions: List<Instruction>
            get() = _instructions

        operator fun Instruction.unaryPlus() {
            _instructions += this
        }
    }
}