# Variable scope and Closure

This is a topic that should not bother you, if I have done a good job implementing it in this language (which I hope
so!). It is still useful to know about this topic, as I most certainly did not before writing Menter.

Menter uses the scope system that most languages (_not Python for some reason_) use. A variable is only visible in the
code block that it was created in (which is it's closure), which upon exiting removes that variable. When accessing a
local variable, first the current scope is checked, then up through the parent scopes until the variable is found (or
not).

```result=# server required;;;# server required
fun() {
  a = 1
  print("works:", a) # works
}
fun();;;print("fails:", a) # fails
```

But Menter is a language with first-class citizen functions and therefore:

> A closure is a persistent local variable scope.  
> A closure is a record storing a function together with an environment.

... which means that variables are not truly lost when a code block is closed. If a function is defined in a code block,
that function will remember the local variable scope that the original block had access to. When a function like this is
called from a different scope, the scope will temporarily be replaced by the scope the called function had access to.

What does that mean? An example:

```result=() -> { a = 8; () -> { a; }; };;;() -> { a; };;;4;;;8;;;4
fun() {
  a = 8
  () -> a
};;;val = fun();;;a = 4;;;val();;;a
```

As you can see, the variable `a` should be lost when exiting the scope of the `fun()` function. However, the anonymous
function `() -> a` that is returned by the `fun()` function still remembers the scope where `a` was visible. When
called, `val()` fully replaces the current scope with the one it was defined in for the duration of its evaluation. The
outer scope and `a` are not used by the function.

## Effectively private variables

If you know enough about this concept, you might also know that this introduces an opportunity for effectively private
variables. Menter does not currently support creating custom types purely in Menter
([only via Java](Java_custom_java_types.html), but I plan on changing that in the future), which is why this is
currently the only option to archive this.

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

The `Person` constructor function has two local variables `public` and `private`. The `private` object stores all
attributes that should not be modifiable from the outside, the `public` object contains all functions that should be
used to access that stored data.  
Since the functions in `public` are all created inside the scope of the `Person` function, they can all access the
`private` variables. As soon as the function is exited, only the `public` functions have access to the `private`
variables.

When a `Person` is created, the fields are initialized and `public` is returned. You can now call these functions to
access and modify the local variables.  
The `execute` function, as the comment suggests, allows for accessing the `private` fields as if the scope was inside
the `Person` function, since they are passed to the callback from inside the `Person` function.
