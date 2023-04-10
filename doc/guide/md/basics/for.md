# For loop

Similar to Python, Menter does not know traditional indexed 'for' loops, only 'for each' loops.

Syntax:

```static
for (variable in iterable) { ... };;;for (variable : iterable) { ... }
```

With each iteration, the variable is assigned the next value from the iterable. Anything that is iterable can also be
looped over by calling the `forEach` method:

```static
iterable.forEach(x -> { ... })
```

## continue and break

The `continue` and `break` keywords work exactly as in other languages.

`break` will break out of the current loop, `continue` will skip the current iteration and continue with the next one,
if there is one.

```result=[];;;[1, 2, 3, 4, 6, 7]
numbers = [];;;for (i in range(1, 10)) {
  if (i == 5) continue
  if (i == 8) break
  numbers.push(i)
}
numbers
```

To learn more about how this works internally, read the chapter about [value markers](Java_value_markers.html).

## Iterators

Anything that implements an `iterator()` method is iterable.

It is recommended to have a local Menter server running whilst viewing some of the code boxes below, as the print output
cannot be seen otherwise.

Iterators can actually be called manually and return themselves when called again.

```result=<<iterator>>
"str".iterator();;;"str".iterator().forEach(print)
```

Let's go over some of the primitive types that are iterable:

### List

Lists simply iterate over all contained elements:

```result=0;;;3;;;6
sum = 0;;;for (i in [0, 1, 2])
  sum += i;;;for (i in range(0, 2))
  sum += i;;;range(0, 2).forEach(print)
```

If you need the index of the current element alongside the value, simply provide a parameter list with the size 2.

```result=null
for ((index, value) in range(2, 4))
  print(index, value);;;range(2, 4).forEach((index, value) -> print(index, value))
```

### Objects

Since objects and lists are represented by the same data structure internally, their iterable works quite similar. When
providing one variable as iterator parameter, only the values are returned. When providing two, the key is passed
alongside the value.

```result={a: 2, b: 8, c: 64};;;null;;;null
obj = {a: 2, b: 8, c: 64};;;for (value in obj)
  print(value);;;for ((key, value) in obj)
  print(key, value)
```

### String

Strings simply iterate over all their characters, in order of appearance.

```result=null
"Hello".forEach(print)
```

## Making your custom java type iterable

This chapter will assume you know how to create your own java type.
To learn more about custom java types, [see this section](Java_custom_java_types.html).

The only addition that has to be made to a type class is an `iterator` method. As with all function signatures, the
method has to return a `Value`, which means that the iterator has to be wrapped inside a `Value` object before returning
it.

```static---lang=java
@Override
public Value iterator() {
    List<Value> items = Arrays.asList(new Value("hello"), new Value("world"));
    return new Value(items.iterator());
}
```

Or you can simply call the iterator method on another `Value` instance.

```static---lang=java
@Override
public Value iterator() {
    return new Value(myValue).iterator();
}
```

A full class could look like this:

```static---lang=java
@TypeMetaData(typeName = "MyIterableType", moduleName = "test")
public class MyIterableType extends CustomType {

    private List<Value> asStrings = new ArrayList<>();

    public MyIterableType(List<Value> parameters) {
        super(parameters);
    }

    @TypeFunction
    public Value addValue(List<Value> parameters) {
        parameters.forEach(param -> asStrings.add(new Value(param.toString())));
    }

    @Override
    public Value iterator() {
        return new Value(asStrings.iterator());
    }
}
```
