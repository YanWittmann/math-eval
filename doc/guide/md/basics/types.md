# Types / Classes

> This chapter is about custom types written natively in Menter. See [Java Types](Java_custom_java_types.html) for more
> information about custom types written in Java.

## Understanding `self` and `super`

Before diving into custom types, it's important to understand the use of `self`, `this`, and `super`.

- `self` and `this` can be used interchangeably and are a reference to the current object.
- If you are inside an object, inside another object, `super` is a reference to the parent object.

### Example

`self`

```result=9;;;15;;;11
{fun: x -> self.a + x, a: 7}.fun(2);;;{a: 5, b: x -> { # multiple layers deep inside an object
  for (i in range(1, self.a)) x++
  return x
}}.b(10);;;tmp = [{a: 5}, self[0].a + 6] # and with lists
tmp[1]
```

`super`

```result=3;;;8
{c: 3, d: {e: super.c}}.d.e;;;{a: 5, b: { # accessing layers that are even higher is only
    c: 3,   # possible by creating functions that return super
    souper: () -> super,
    d: { e: super.souper().a + super.c }
}}.b.d.e
```

Note that the elements in an object are evaluated in the order they appear. This means that you can't access a symbol
that will be defined later when creating the initial value of a symbol. Since functions are evaluated only when called,
this will work with functions.

```result={a: 5, b: 5};;;Cannot resolve symbol 'b' on [self.b]
{ a: 5, b: self.a };;;{ a: self.b, b: 5 }
```

## Types

Types are Menter's class system. Types are defined by a function that creates an object.

```static
TypeName = (args, ...) -> { attr: ..., ... }
```

This function can then be called using the `new` keyword to create a new instance. Calling it without `new` will return
`null`.

```static
new TypeName(args, ...)
```

The constructing function behaves a little different from normal functions and objects, but the returned object is a
regular object that can perform all the same operations as any other object.

### `$init` constructor

The `$init` function is called when the type is created using `new`. It is a function that takes no arguments and
returns nothing. The arguments passed from the `new` call are available everywhere in the type on initialization via the
`args` symbol.

```result=Hello, my name is Yan and I am 22 years old.
Person = (name, age) -> {
  $init: () -> {
    print("Hello, my name is", args.name, "and I am", args.age, "years old.")
  }
}
new Person("Yan", 22)
```

This function is called at the end, when all other attributes have been evaluated to ensure that you can access all
attributes from the type.

### `$fields` attributes

The `$fields` attribute is a list of symbols that serves as a shorthand to defining attributes. Each symbol in the list
creates an attribute in the type by checking if there is a constructor argument in `args` with a matching name.

```result={name: Yan, age: 22}
Person = (name, age) -> {
  $fields: [name, age]
}
new Person("Yan", 22)
```

Is equal to

```result={name: Yan, age: 22}
Person = (name, age) -> {
  name: name, age: age
}
new Person("Yan", 22)
```

Is equal to

```result={name: Yan, age: 22}
Person = (name, age) -> {
  name: args.name, age: args.age
}
new Person("Yan", 22)
```

### `$extends` inheritance

Either one or a list of constructor calls can be passed to `$extends` to inherit from other types. The inherited types
are evaluated in order, meaning that the last type in the list will overwrite any attributes that were defined in the
previous types. The super types are also evaluated before the type itself, meaning that you can access attributes from
the super types in the attributes and functions.

Example with a single super type:

```result=(name, age) -> { {$fields: [name, age]} };;;(name, age, departement) -> { {$extends: Person(name, age), $fields: [departement]} };;;{name: Yan, age: 22, departement: Software Development}
Person = (name, age) -> { # super type
  $fields: [name, age]
};;;Manager = (name, age, departement) -> { # sub type
  $extends: Person(name, age),
  $fields: [departement]
};;;yan = new Manager("Yan", 22, "Software Development")
```

Example with multiple super types:

```result=(name, age) -> { {$init: () -> { self.age++ }, $fields: [name, age]} };;;(billingAmount) -> { {$fields: [billingAmount]} };;;(name, age, departement) -> { {$extends: [Person(name, age), Billable(1000)], $fields: [departement]} };;;{name: John, age: 21, billingAmount: 1000, departement: HR}
Person = (name, age) -> { # super type
  $init: () -> { self.age++ }, 
  $fields: [name, age]
};;;Billable = (billingAmount) -> { # super type
  $fields: [billingAmount]
};;;Manager = (name, age, departement) -> { # sub type
  $extends: [Person(name, age), Billable(1000)],
  $fields: [departement]
};;;john = new Manager("John", 20, "HR")
```
