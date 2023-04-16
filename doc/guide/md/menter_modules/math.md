# math.mtr

```static
{{ file: menter-lang/src/main/resources/src/math.mtr }}
```

### range

The range method will create a list of integers from a start value to an end value. If a third parameter is given, the
list will only include every n-th value.

```result=[1, 2, 3, 4, 5, 6, 7, 8, 9, 10];;;[1, 3, 5, 7, 9]
range(1, 10);;;range(1, 10, 2)
```

### space

Similar to the range method, the space method will create a list of integers from a start value to an end value.
However, the list will contain evenly spaced values. The default number of values is 120, but can be changed by passing
a third parameter.

```result=120;;;[1, 25.75, 50.5, 75.25, 100]
space(1, 10).size();;;space(1, 100, 5)
```

### sin, cos, tan, asin, acos, atan

These methods will calculate the sine, cosine, tangent, arcsine, arccosine and arctangent of a value in radians.

```result=0.8414709848078965;;;1;;;1;;;1.5707963267948966;;;1.5707963267948966;;;0.7853981633974483
sin(1);;;cos(PI * 2);;;tan(PI / 4);;;asin(1);;;acos(0);;;atan(1)
```

### random

The random method will return a random number between 0 and 1. The bounds of the random number can be changed by passing
a start and end value as parameters. Both bounds are inclusive.

If you need integers, you can use the floor method to round down to the nearest integer. In this case, the upper bound
will be exclusive, since you are rounding down.

```result=0.37280036118180193;;;6.5920401949397968;;;{1: 1048, 2: 1026, 3: 974, 4: 942, 5: 958, 6: 1052, 7: 969, 8: 996, 9: 1031, 10: 1004}
random();;;random(1, 10);;;range(1, 10000)
  .map(x -> floor(random(1, 11)))
  .frequency()
  .sortKey() # or: sortKey((a, b) -> a - b)
```

### round, floor, ceil

The rounding methods work as you would expect. The round method will round to the nearest integer, the floor method will
round down to the nearest integer and the ceil method will round up to the nearest integer.

```result=3;;;3;;;4
round(3.14);;;floor(3.14);;;ceil(3.14)
```

### abs

The abs method will return the absolute value of a number.

```result=3;;;-3
abs(3);;;abs(-3)
```

### sqrt, root

The sqrt method will calculate the square root of a number. The root method will calculate the n-th root of a number.

```result=3;;;2;;;2
sqrt(9);;;root(8, 3);;;root(16, 4)
```

### log, ln

The log method will calculate the logarithm of a number with a given base. The ln method will calculate the natural
logarithm of a number.

```result=2;;;1;;;1
log(100, 10);;;log(10, 10);;;ln(E)
```

### Constants

The math module also contains the constants PI and E.

```result=3.1415926535897932384626433832795028841971;;;2.71828182845904523536028747135266249775724709369995
PI;;;E
```
