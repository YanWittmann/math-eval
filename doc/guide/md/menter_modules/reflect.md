# reflect.mtr

```static
{{ file: menter-lang/src/main/resources/src/reflect.mtr }}
```

The `reflect` module allows you to access the internals of the Menter interpreter.
It is not recommended to use this module unless you know what you're doing, as features may be removed or changed at
any time.

### inherit

Usually, when assigning a value to a variable, the variable is re-assigned to the new value in the current context,
no matter if the variable already exists in a parent context.

The inherit function allows you to set the value of a `Value` instance that is referenced by a variable.
This might not seem to make a big difference, so let me show you an example.

The same result would be possible with a regular assignment:

```result=null;;;1;;;10;;;10
import reflect inline;;;a = 1;;;inherit(a, 10);;;a
```

... in contrast to this one:

```result=null;;;1;;;(x) -> { inherit(x, 10); };;;10;;;10
import reflect inline;;;a = 1;;;fun = x -> inherit(x, 10);;;fun(a);;;a
```

This example took the reference to the variable `a` and passed it to a function.
This function then set the value referenced by the variable to the value _of the value_ `10`.

This also defeats the purpose of the effectively private variables created using closures:

```result=null;;;(name) -> { private.name = name; public.getName = () -> { private.name; }; public; };;;{getName: () -> { private.name; }};;;Thomas;;;Thomas;;;Cannot assign to yan.getName()
import reflect inline;;;Person = name -> {
    private.name = name
    public.getName = () -> private.name
    public
};;;yan = Person("Yan");;;# effectively assigns value of a value instance to returned value instance
inherit(yan.getName(), "Thomas");;;yan.getName();;;# would normally fail
yan.getName() = "Yan"
```

### access

The access function allows calling the `public Value access(Value identifier)` method in the `Value` class.
This leads to the same result as using the `[]` operator.
If you need to set the variable, use the inherit function from above.
There is no reason to use this function if you code normally, but is useful when doing meta-programming.

```result=null;;;[];;;value;;;value;;;other value
import reflect inline;;;map = {};;;map["key"] = "value";;;access(map, "key");;;inherit(access(map, "key"), "other value")
```

### setVariable

The setVariable function allows you to set a variable in the current context.
It calls the same function as the `=` assignment operator would.

The difference to the assignment operator is that the variable name is a string instead of an identifier.
This allows the programmatic creation of variable names.

```result=null;;;1;;;null;;;10
import reflect inline;;;ab = 1;;;setVariable("a" + "b", 10);;;ab
```

### getVariable

The getVariable function allows you to get the value of a variable. The same applies as above, the variable name is a
string.

```result=null;;;1;;;1
import reflect inline;;;ab = 1;;;getVariable("a" + "b")
```

### getVariables

The getVariables function returns a map of all variables and their values in the current context.
The values returned are the actual values, not some shallow copy,
meaning they could be modified using the `inherit` method.

```result=null;;;1;;;2;;;{a: 1, b: 2}
import reflect inline;;;a = 1;;;b = 2;;;getVariables()
```

### removeVariable

The removeVariable function allows you to remove a variable from the current context.

This can be very destructive, so be careful when using this function. You can remove all kinds of symbols, even system
symbols.

```result=null;;;1;;;null;;;Cannot resolve symbol 'a' on [a]
import reflect inline;;;a = 1;;;removeVariable("a");;;a
```

This instruction would remove all local variables from the current context.

```static
import reflect inline;;;getVariables().forEach((k, v) -> removeVariable(k));;;removeVariable("reflect")
```

### getContextName

The getContextName function returns the name of the current context. In this case, the context is the code box in which
the function is called.

```result=null;;;codebox-318239
import reflect inline;;;getContextName()
```

### getImports

The getImports function returns a map of all imports in the current context.

```result=null;;;{system: {print: <<lambda>>, getProperty: <<lambda>>, getEnv: <<lambda>>, sleep: <<lambda>>}, ...;;;[system, math, reflect];;;1
import reflect inline;;;imports = getImports();;;imports.keys();;;imports.math.abs(-1)
```

### getModules

The getModules function returns a map of all exported modules from the current context.
The array of exported symbols is only a string list, meaning you can't access the actual symbols via that list.

```result=null;;;4;;;null;;;[reflectionModuleTest]---id=reflectionModuleTest-1
import reflect inline;;;a = 4;;;export [a] as reflectionModuleTest;;;modules = getModules()
```

### callFunctionByName

The callFunctionByName function allows you to call a function by its name.
The first parameter is the name of the function, the second one is a list of arguments.

```result=null;;;<<lambda>>;;;24
import reflect inline;;;fun = (!];;;callFunctionByName("fun", [4])
```

### getStackTrace

The getStackTrace function returns a list of function names that are currently on the stack.

```result=null;;;() -> { ((x) -> { getStackTrace(); })(2); };;;[fun, ((x) -> { getStackTrace(); })]
import reflect inline;;;fun = () -> { (x -> getStackTrace())(2) };;;fun()
```

### printStackTrace

The printStackTrace function prints the stack trace to the default print stream.

This is most useful when debugging, as it allows you to see the call stack.

```result=null;;;() -> { ((x) -> { printStackTrace(); })(2); };;;[fun, ((x) -> { printStackTrace(); })]
import reflect inline;;;fun = () -> { (x -> printStackTrace())(2) };;;fun()
```
