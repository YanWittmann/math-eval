# Operators

Operators use the precedence from [Java](https://introcs.cs.princeton.edu/java/11precedence/), with the exception of the
power operator, which comes from Python.

Operators can be left-associative or right-associative, indicated by an interval-syntax:

- `( )` parenthesis show that there _is a value_ on that side of the operator
- `[ ]` square brackets show that there _is **no** value_ on that side of the operator

Basically, the round parenthesis indicate where the values are that are transformed by the operator.

Examples for the `-` and `!` operators:

- `-4` is right-associative: `[-) 4` the operator value is on the right side of the operator
- `6 - 4` is left and right-associative: `6 (-) 4` there are values on both sides of the operator
- `!true` --> `[!) true`
- `6!` --> `6 (!]`

Find a full [list of operators here](Core_Language_operators_list.html).

## Operator functions

Using the interval syntax above, operators can be transformed into functions that take either one or two arguments.

For example, the `+` operator can be transformed into a function that takes two arguments, or the `!` operator with one
argument.

```result=<<lambda>>;;;3;;;120
add = (+) # store in function...;;;add(1, 2);;;(!](5) # ...or call directly
```

That way, operators can be passed as arguments to other functions.

```result=10;;;[1, 2, 6, 24]
range(1, 4).reduce((+));;;[1, 2, 3, 4].map((!])
```

## Compound assignment operator

A special feature of Menter is, that the assignment operator can be combined with any other operator.  
This is called a compound assignment operator.

```result=5;;;7;;;343
x = 5;;;x += 2;;;x **= 3
```

```result=[1, 2, 3];;;[1, 2, 3, 4, 5, 6]
x = [1, 2, 3];;;x ::= [4, 5, 6]
```

A quality of life feature regarding this is, that if the assigning variable is not defined, it will be initialized with
the value of the right hand side.

```result=5;;;[2, 3];;;3
a += 5;;;b ::= [2, 3];;;c **= 3
```
