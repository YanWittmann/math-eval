# Operator list

Operators are listed in the order of precedence, from highest to lowest.

Understandably, you may skip over most of this. I would recommend however that you at least check out the bottom part of
the page, where some of the more interesting parts like `(::)`, `(->)` and `(|>)` are explained.

### `(++]` (150) and `(--]` (150)

The `(++]`/`(--]` operators are used to in/decrement a value by one.
These operators are the postfix versions of the `++`/`--` operators, meaning that the value is in/decremented after the
expression is evaluated.

```result=1;;;1;;;2
x = 1;;;x++;;;x
```

The `(--]` operator acts the same, but decrements the value by one.

### `(**)` (145)

The power operator is used to calculate the power of a number. It has two variants that are equivalent: `^` and `^^`.

```result=8;;;81
2 ** 3;;;3 ^^ 4
```

### `[++)` (140) and `[--)` (140)

The `[++)` and `(--)` operators are used to in/decrement a value by one.
These operators are the prefix versions of the `++`/`--` operators, meaning that the value is in/decremented before the
expression is evaluated.

```result=1;;;2;;;2
x = 1;;;++x;;;x
```

### `[-)` (140) and `(+]` (140)

`-` negates a number. `+` is the identity operator for numbers, it does nothing.

```result=-1;;;1
-1;;;+1
```

### `[!)` (140)

The `!` operator is the logical negation operator. It negates a boolean value.

```result=false;;;true
!true;;;!false
```

### `(!]` (140)

The `!` operator is the factorial operator. It calculates the factorial of a number.

```result=120;;;3628800
5!;;;10!
```

### `[~)` (140)

The `~` operator is the bitwise negation operator for numbers.

```result=-2;;;-3
~1;;;~2
```

### `(*)` (120)

The `*` operator is the multiplication operator.

```result=6
2 * 3
```

### `(/)` (120)

The `/` operator is the division operator. It will return a floating point number, if the result is not an integer.

```result=2;;;2.5
6 / 3;;;5 / 2
```

### `(%)` (120)

The `%` operator is the programming language modulo operator. It returns the remainder of a division.

```result=1;;;-2
5 % 2;;;-6 % 4
```

### `(%%)` (110)

The `%%` operator is the mathematical modulo operator. It returns the smallest positive representative of the rest
class of a division.

```result=1;;;2
5 %% 2;;;-6 %% 4
```

### `(+)` (110)

The `+` operator is the addition operator.

```result=5
2 + 3
```

### `(-)` (110)

The `-` operator is the subtraction operator.

```result=-1
2 - 3
```

### `(<<)` (100) and `(>>)` (100)

The `<<` and `>>` operators are the bitwise shift operators. They shift the bits of a number to the left or right.

```result=8;;;2
4 << 1;;;8 >> 2
```

### `(<)` (90) and `(>)` (90)

The `<` and `>` operators are the less than and greater than operators. They compare two numbers and return a boolean
value.

```result=true;;;false
2 < 3;;;1 > 2
```

### `(<=)` (90) and `(>=)` (90)

The `<=` and `>=` operators are the less than or equal to and greater than or equal to operators. They compare two
numbers and return a boolean value.

```result=true;;;false
2 <= 2;;;1 >= 2
```

### `(==)` (80) and `(!=)` (80)

The `==` and `!=` operators are the equality and inequality operators. They compare two values and return a boolean
value.

```result=true;;;true
2 == 2;;;1 != 2
```

### `(&&)` (40)

The `&&` operator is the logical and operator. It returns true if both values are true. In contrast to other languages,
both values are evaluated, no matter if the first value is `true` or `false`.

```result=false;;;false
a = x -> { print("a"); !a }
b = x -> { print("b"); !b }
a(true);;;a(true) && b(true)
```

### `(||)` (30)

The `||` operator is the logical or operator. It returns true if one of the values is true. The same as with the `&&`
operator, both values are evaluated, no matter if the first value is `true` or `false`.

```result=true;;;false
true || false;;;false || false
```

### `(::)` (20)

The `::` operator is the concatenation operator.

- When concatenating two lists, the second list is appended to the first list.
- When concatenating a value and a list, the value is pre/appended to the list.
- When merging to objects, the second object overwrites the values of the first object.

```result=[1, 2, 3, 4];;;[1, 2, 3];;;[1, 2, 3];;;{a: 1, b: 2, c: 3, d: 4}
[1, 2] :: [3, 4];;;1 :: [2, 3];;;[1, 2] :: 3;;;{a: 1, b: 2, c: 100} :: {c: 3, d: 4}
```

### `(=)` (10)

The `=` operator is the assignment operator. It assigns a value to a variable.

```result=1;;;2
x = 1;;;x = 2
```

Learn more about [compound assignment operators here](Core_Language_operators.html).

### `(->)` (5)

The `->` operator is used to construct an anonymous function. Left of the `->` is the parameter list, right of the `->`
is the function body.

The parameter list may look like this:

- `x` - A single parameter
- `(x, y)` - A tuple of parameters
- `()` - No parameters

The body may look like this:

- `x` - A single expression
- `{ x }` - A block of expressions

```result=(x) -> { x + 1 };;;(a, b) -> { c = a + b; c + 4 };;;() -> { 1 }
x -> x + 1;;;(a, b) -> { c = a + b; c + 4 };;;() -> 1
```

### `(|>)` (0) and `(>|)` (0)

The `|>` and `>|` operators are called pipeline operators and are used to pipe the result of an expression into the
parameter list of a function.  
The two variants differ in that they insert the value at the beginning `>|` or at the end `|>` of the parameter list.

```result=(a, b) -> { a - b };;;-3;;;3
sub = (a, b) -> a - b;;;5 |> sub(2);;;5 >| sub(2)
```

This can be chained to create a pipeline of functions where inline functions can be called as well.

```result=<<lambda>>;;;(x) -> { x * 2 };;;14
add = (+);;;double = x -> x * 2;;;1 |> add(2) |> double |> ((x, y) -> x + y)(3) |> x -> x + 5
```
