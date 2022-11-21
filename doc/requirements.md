Expression evaluator v3 requirements:

- literals
	- boolean (true, false)
	- string ("")
	- regex (/regex/flags)
	- numeric (isNumeric() == true):
		- integers (1)
		- FP, floating point (1.0)
		- hex (0x1)
		- binary (0b1)
		- octals (0o1)
	- list initialier (\[X1, ..., Xn])
	- map initializer ({key1: value1, ..., keyn: valuen})

- operators
	- \+ addition
		- with 2 operands, order does not matter:
			- string X any\list: string concentration
			- regex X any: exception
			- isNumeric X isNumeric: add numbers
			- list X any: perform operation on every element in list
		  the type of the left operand is chosen as resulting type
		- with 1 operand right:
			- isNumeric: unchanged
			- any: exception
		  the type of the operand is the resulting type
	- - subtraction/negate
		- with 2 operands:
			- string X string: remove any occurences of second string in first string
			- regex X any: exception
			- isNumeric X isNumeric: subtract right from left
			- list X any: perform operation on every element in list
		  the type of the left operand is chosen as resulting type
		- with 1 operand right:
			- isNumeric: negate operand
			- string: reverse character order
			- any: exception
		  the type of the operand is the resulting type
	- * multiplication
	- / division
	- % modulo
	- ! inversion
	- X cartesian product
	- a\["X"], a.X map accessor
	- = assignment

- order of operations:
	() > ...

- other requirements:
	- numbers of any length
	- FP precision:
		- FP precision is 20
		- user can set FP precision
	- operators:
		- operators must support differenting between different types (int X int, int X string, ...)
		- user can override operators
	- variables/functions:
		- functions are first class citicens, can be assigned to variables
		- user can define functions with any name with any amount of parameters with any name
		- functions can be overwritten and overloaded
		- variables/functions are only visible in the scope of the block the assignment has happened in
		- functions can be defined in other functions
		- functions can be passed as parameter
		- lambda functions are supported as 'inline functions'
		- functions can be called inside the call of another function (foo(bar(2), 4)
		- functions implemented by the runtime must be declared 'native'. in this case, the runtime is asked to resolve the function.
		  native functions are defined in the core module.
	- usually, multiple statements must be seperated by a ';', but it may automatically be placed if the next line does not start with a space character
	  that indicates that the line is not done yet
	- literals must remember their initial type
		- for this, the literals cannot be stored as simple values, but must be wrapped by a wrapper class that stores the data type
		- if two values of different types are combined, the operator must specify the result type of the operation
	- there is no type safety, but several checking methods may be implemented to give verbose error messages in case a type error occurs
	- maps:
		- maps can contain functions
	- runtime:
		- a runtime stores all variables and functions that have been initialized in the global scope
		- a runtime may be restarted at any time, resetting all non-built-in variables and functions
		- the runtime keeps track of any call-stack frames with their individual variables and functions
	- modules:
		- modules can be created by using the 'export \[S1, ..., Sn] as NAME' syntax
		- to import everything in a module, use 'import NAME \[as NAME|inline]'
		- the runtime will read all files in the module paths that have been specified and find any exports statements to parse the related functions before the program starts
		- the variables and functions will be accessable via the . or [] syntax on the name it has been imported as
		- if import is inline, the name does not need to be specified in order for the access to happen

syntax:
	- functions:
		- defining a function, the last expression is the return value: foo(X1, ..., Xn) = expr   OR   foo(X1, ..., Xn) = {expr1; ...; exprn}
		- lambda functions: {X1, ..., Xn -> expr}   OR   {X1, ..., Xn -> {expr1; ...; exprn}}
		- calling a function: foo(1, ..., 3)
		- calling a function with functions: foo({x, ..., z -> x + z}, bar)


examples:

TestModule.me
```
myFunction(x, y, z) = x + pow(z, 2)
    - y
myVariable = 34
export [myFunction, myVariable] as TestModule
```

ImportModule1.me
```
import TestModule inline
myFunction(myVariable, 3, 6)
```

ImportModule2.me
```
import TestModule
TestModule.myFunction(TestModule.myVariable, 3, 6)
TestModule["myFunction"](TestModule["myVariable"], 3, 6)
```
