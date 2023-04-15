Features:

- [ ] stack trace line numbers
- [ ] write documentation
- [ ] input history for more than one element in documentation
- [ ] if ...: ... elif ...: ... else: ...
- [ ] for ...: ...
- [ ] while ...: ...
- [ ] string output with quotes "", two separate rendering functions (?)
- [ ] fix `??` when printing syntax trees (also in documentation)
- [ ] documentation: info/warning/error boxes
- [ ] cross operator (X ?)
- [ ] rethink `::` operator

To be thought about:

- [ ] custom types in menter lang directly via map; must think more about a useful syntax for this.
- [ ] live preview "safe-mode" in which no global variables can be changed

```
Person = (name, alter) -> {
    private.name = name
    private.alter = alter
    public.getName = () -> private.name
    public.getAlter = () -> private.alter
    public.altern = () -> private.alter = private.alter + 1
    public.setName = name -> private.name = name
    public.execute = (f) -> f(private)
    public
}

yan = Person("Yan", 22)
print(yan.getName())
yan.execute(person -> person.name = "Thomas")
print(yan.getName())
```

```
isPrimeUtil(n, i) {
    if (n == 2) true
    elif (n < 2) false
    elif (n % i == 0) false
    elif (i * i > n) true
    else isPrimeUtil(n, i + 1)
}
isPrime(n) = n >| isPrimeUtil(2)
range(1, 100).filter(isPrime)
range(2, 100).filter(isPrime).size()

# alternative without recursion
isPrime(x) {
    if(x <= 3) return x > 1
    elif (x % 2 == 0 || x % 3 == 0) return false
    else {
        limit = round(x ^^ 0.5)
		if (limit >= 5)
            for (i in range(5, limit, 6)) {
                if (x % i == 0 || x % (i+2) == 0) return false
            }
        return true
    }
}
nextPrime(x) {
    for (i in range(1,100))
        if (isPrime(x + i)) return x + i
}
```

```
plot(space(-10, 10), x -> 0.4*x^3, x -> x^2, x -> 3 * x - 20, x -> 16 * sin(x) + 62, x -> 10 * cos(x * 2) - 37)
```

```
namen = ["Yan", "Nils", "Holger", "Ute", "Thomas", "Jonas", "Eren"]
createPerson(min, max) = {name: namen[floor(random(0, namen.size()))], age: round(random(min, max))}
db ::= range(1, 100).map(x -> createPerson(12, 26))
db ::= range(1, 100).map(x -> createPerson(27, 32))

print("age frequencies:", db.map(x -> x.age).frequency().sort())

print("people above 20:", db.filter(x -> x.age > 20).size())
print("average age of people above 20:", db.filter(x -> x.age > 20).map(x -> x.age).avg())

import cmdplot
cmdplot.table(db)
```

```
for((k,v) in getVariables()) { print("removing", k); removeVariable(k) }
```

```
derivative = (p, f) -> (x -> (f(x+p) - f(x-p)) / (2*p))
plot(space(1, 3*PI), sin, derivative(0.1, sin))
```

```
range(1,6).cross(range(1,6)).filter(x -> x.reduce((+)) > 8).map(x -> x.reduce((+))).sort().frequency()
```

```
data = range(0,1).map(x -> {k: x}).cross(range(1,6).map(x -> {w: x})).map(x -> {x.summe = x.k + x.w; x})
data.map(x -> x.summe).foldl({}, (acc, val) -> { acc[val] += 1; return acc; })
# {2: 2, 3: 2, 4: 2, 5: 2, 6: 2, 7: 1}
```

```
double = x -> x * 2; (([1, 2].map(x -> x + 3) |> x -> [x[0], -x[1]])[1] |> double) + " !"
```

```
clear() = "\n" * 100 |> print
```

on modules:

```
>> import other
>> other.otherVal
(x) -> { x + 4 + test; }
>> other.test
Error: Illegal access on [other.test]: module does not export symbol
        in [repl] at other.test
        Local symbols:  otherVal (function), test (number)
```

## Bugs:

accessors on maps:

```
{test:[1], hey: 4}[0]
```

module returns `null`:

```
>> import hello
>> hello
Error: Cannot invoke "de.yanwittmann.menter.interpreter.structure.value.Value.isReturn()" because "result" is null
```