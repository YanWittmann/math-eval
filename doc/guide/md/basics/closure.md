# Variable scope and Closure

Variable scope and closure are important concepts in programming, and Menter uses them to manage local variables and
functions. In this documentation entry, we'll explain how Menter implements variable scope and closure, and how you can
use it to create effectively private variables.

## Variable Scope

Menter uses the standard scope system found in most languages, where a variable is only visible in the scope that
it was created in (which is its closure). When accessing a local variable, Menter checks the current scope, then up
through the parent scopes until the variable is found (or not).

New scopes are created on:

- Function calls
- Loop iterations
- If statements

but **NOT** on:

- Code blocks

Here's an example:

```result=# server required;;;# server required
fun() {
  a = 1
  print("works:", a) # works
}
fun();;;print("fails:", a) # fails
```

In this example, `a` is only visible inside the `fun()` function, and attempting to access it outside of that function
will result in an error.

## Closure

Menter is a language with first-class citizen functions, which means that it also supports closures. A closure is a
persistent local variable scope, and it is created when a function is defined inside a code block. The function will
remember the local variable scope that the original block had access to, and when the function is called from a
different scope, the scope will temporarily be replaced by the scope the called function had access to.

Here's an example:

```result=() -> { a = 8; () -> { a } };;;() -> { a };;;4;;;8;;;4
fun() {
  a = 8
  () -> a
};;;val = fun();;;a = 4;;;val();;;a
```

In this example, the anonymous function `() -> a` remembers the scope where `a` was visible, even though a should be
lost when exiting the `fun()` function. When called, `val()` fully replaces the current scope with the one it was
defined in for the duration of its evaluation. The outer scope and `a` are not used by the function.

## Effectively private variables

Menter does not currently support creating custom types purely in Menter ([only via Java](Java_custom_java_types.html),
but we plan on changing that in the future), but you can still use closures to create effectively private variables.

Here's an example:

```result=# server required;;;# server required;;;Yan;;;23;;;Thomas
Person = (name, age) -> {
    private.name = name
    private.age = age

    public.getName = () -> private.name
    public.getAge = () -> private.age
    public.age = () -> private.age = private.age + 1
    public.setName = name -> private.name = name
    public.execute = (f) -> f(private) # defeats whole 'private' purpose, only for demo

    public
};;;yan = Person("Yan", 22);;;yan.getName();;;yan.age();;;yan.execute(person -> person.name = "Thomas")
```

So, what is happening here?

The `Person` constructor function has two local variables, `public` and `private`. The `private` object stores all
attributes that should not be modifiable from the outside, the `public` object contains all functions that should be
used to access that stored data.  
Since the functions in `public` are all created inside the scope of the `Person` function, they can all access the
`private` variables. As soon as the function is exited, only the `public` functions have access to the `private`
variables.

When a `Person` is created, the fields are initialized and `public` is returned. You can now call these functions to
access and modify the local variables.  
The `execute` function, as the comment suggests, allows for accessing the `private` fields as if the scope was inside
the `Person` function, since they are passed to the callback from inside the `Person` function.

This pattern of using closures to create effectively private variables is quite common in programming. It allows for
encapsulating data and functions to create a clean and organized interface for interacting with that data. However, it's
important to note that this does not provide true security or privacy as the data can still be accessed and modified if
someone really wants to. It's more of a convention for organizing code and preventing accidental modification of data.
