space
=====

_space_ is an interpreter for the [Whitespace](https://en.wikipedia.org/wiki/Whitespace_\(programming_language\)) esoteric programming language.

## Basic Running
```kotlin
val ws = File(path)
// This can also be a string as well

val lexer = Lexer()
val lexed = lexer lex ws

val parser = Parser()
// You can choose to define any OutputStream, with the default being System.out

// If you want to use any form of input, just do System.setIn(...)
val program = parser parse lexed
program.evaluate()
```

## Program Builder DSL
```kotlin
val program = buildProgram {
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
```

Which outputs:

`Hello world!`

## Exporting to File
If you want to export the generated program to a file, you can choose to either
write the program contents to a file, or create a `ProgramBuilder` and to file there.

```kotlin
    // With the same program from above
    val wsText = program.toWhitespace()

    val file = File(path)
    file.writeText(wsText)
```

or

```kotlin
val builder = ProgramBuilder()
builder.add {
    ...
}

val file = builder.exportToFile(path)
```

## Printing a Whitespace Program
After parsing a Whitespace program, it may also be of interest to see
a more readable program structure. Unless you have some form of syntax
highlighting for `.ws` files, there is nothing to see. That's why I allowed for any program to be printed in a beautiful, easy to understand way.

```kotlin
 // Printing the program that outputs "Hello world!"
println(program)
```

Which outputs:

```
push 0
push 33
push 100
push 108
push 114
push 111
push 119
push 32
push 111
push 108
push 108
push 101
push 72
call 0
label 0:
    dup
    jumpzero 1
    printchar
    jump 0

label 1:
    discard
    end
```

Which looks much more readable than:

```
    
   	    	
   		  	  
   		 		  
   			  	 
   		 				
   			 			
   	     
   		 				
   		 		  
   		 		  
   		  	 	
   	  	   

 	  

    
 
 
	  	
	
  
 	  

   	
 




```