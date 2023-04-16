# system.mtr

```static
{{ file: menter-lang/src/main/resources/src/system.mtr }}
```

### print

The print method will write a list of values, separated by a space, to the standard output.

```result=null
print("Value:", 5)
```

### getProperty

The getProperty method will return the value of a system property.

```result=1.8.0_302
getProperty("java.version")
```

### getEnv

The getEnv method will return the value of an environment variable.

```result=C:\Program Files\Java\jdk1.8.0_302
getEnv("JAVA_HOME")
```

### sleep

The sleep method will pause the execution of the program for a given amount of milliseconds. Refresh this page and watch
the box below to see the effect.

```result=500;;;5000
sleep(500);;;sleep(5000)
```
