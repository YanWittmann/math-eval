# debug.mtr

```static
{{ file: menter-lang/src/main/resources/src/debug.mtr }}
```

The `debug` module provides functions for debugging purposes. It is not meant to be used in production code.

This module is still a work in progress. It is not very useful yet at the moment. More information on debugging will be
added in the future.

### breakFlow

The `breakFlow` function is used to break the execution flow of the program. Seeing as there is no visual debugger, this
happens on the command line. Calling this function will activate debugger mode.

> Do not call this method in the code boxes below, as this will halt the execution of all code boxes without the option
> to reactivate them.

```
# breakFlow()
```