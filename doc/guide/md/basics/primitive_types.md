# Basic Types and Values

Menter includes a variety of basic data types such as numbers, strings, booleans, lists and maps/dictionaries.  
Values are the smallest building block everything else is made of: Every expression can be evaluated into a single
value, even control structures like `if`, `for`, and so on.

```result=42
40 + if (true) 2 else 10
```

For a list of all operators that can be applied to values, see [Operators](Core_Language_operators.html).

## Numbers --> `number`

Unlike most languages, numbers in Menter can grow as large as you need them to be, as they are internally represented by
`BigDecimal` instances.

```result=42;;;265252859812191058636308480000000
42;;;30!
```

## Strings --> `string`

Strings are sequences of characters. They are written by enclosing the characters in double quotes (`""`) and can be
concatenated using the `+` operator.

```result="Hello World"
"Hello " + "World"
```


## Functions --> `function`, `value_function`, `native_function`, `reflective_function`

A function is a way to transform values. There are multiple ways functions can be defined in Menter:

```
add(a, b) { a + b }
```
