# cmdplot.mtr

```static
{{ file: menter-lang/src/main/resources/src/cmdplot.mtr }}
```

The cmdplot module allows you to display plots and tables in the console.

It is highly reccomended to view this chapter with the [local Menter server](execute_code.html) running.

### plot

The `plot` function takes a width, a height, a list of x values and functions to calculate y values from x values. It
then prints a plot of the functions to the default print stream.

Alternatively, a list of x values and a list of y values can be passed. In this case, no values will be calculated from
the x values.

If not enough values are passed to fill every x position, a linearly interpolated line will be used to fill the gaps.

```result=12;;;# requires server
import cmdplot inline
width = 90
height = 12;;;plot(
  width, height,                 # actual dimensions of the plot on the console
  space(-2 * PI, 2 * PI, width), # x values
  sin                            # multiple functions to calculate y values from x values
)
```

With a list of x values and a list of y values:

```result=12;;;# requires server
import cmdplot inline
width = 90
height = 12;;;plot(
  width, height,
  [1, 3, 6, -4], # x values
  [-4, 3, 7, 5]   # y values
)
```


#### Some more examples

Two functions in one plot:

```result=25;;;# requires server
import cmdplot inline
width = 90
height = 25;;;plot(
  width, height,
  space(-14, 23, width),
  x -> (x - 1) * (x + 9) * ((x - 15)^^2),
  x -> -100 * ((x- 6)^^2) + 30000
)
```

Calculating the approximate derivative of a function:

```result=25;;;# requires server
import cmdplot inline
derivative = (p, f) -> (x -> (f(x+p) - f(x-p)) / (2*p));;;plot(
  90, 20,
  space(1, 3 * PI),
  sin,
  derivative(0.1, sin)
);;;plot(
  90, 20,
  space(-5, 5),
  x -> x^^3,
  derivative(0.1, x -> x^^3)
)
```

A beautiful circle:

```result=null;;;(r ) -> { p.x = []; p.y = []; for (i : space(0, 2 * pi, 360)) { p.x = p.x :: (r * cos(i)); p.y = p.y :: (r * sin(i)); }; return p; };;;{x: [10, 9.998476951563913, 9.993908270190958, 9.986295347545738, 9.975640502598242, 9.961946980917455, 9.945218953682733, 9.9254615164132
import cmdplot;;;generate_points = r -> {
    p.x = []
    p.y = []
    for (i in space(0, 2 * PI, 360)) {
        p.x = p.x :: (r * cos(i))
        p.y = p.y :: (r * sin(i))
    }
    return p
};;;points = generate_points(10);;;cmdplot.plot(62, 25, points.x, points.y)
```

### table

The `table` function takes an object and prints attributes in a formatted table to the default print stream.

This table has two modes, depending on the type of the object that is passed to it:

- List of objects: Collects all keys of all objects and prints them as rows.
- Object of lists: Prints key-value pairs as columns.

List of objects:

```result=# requires server
import cmdplot inline;;;table([
  { name: "Yan",    age: 23 },
  { name: "Thomas", age: 24 }
])
```

Object of lists/values:

```result=# requires server
import cmdplot inline;;;table({A: [1, 2, 3], B: [4, 5], C: 6})
```

Here is a more complicated example with an object:

```result=# requires server
import cmdplot inline;;;names = ["Yan", "Thomas", "John", "Mira", "Daniel", "Viola"];;;pick(list) = list[floor(random(0, names.size()))];;;createPerson(min, max) = {
  name: pick(names),
  age: round(random(min, max))
};;;people ::= range(1, 100).map(x -> createPerson(12, 26));;;people = people.reduce({}, (acc, val) -> {
  acc[val.name].total += val.age
  acc[val.name].count += 1
  acc
});;;people = people.map(x -> round(x.total / x.count, 2));;;table(people)
```
