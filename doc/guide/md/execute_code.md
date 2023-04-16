# Running Menter Code

This chapter will not cover [how to use the Menter Java library](Java_getting_started.html), but rather the command line
tool.  
In order to run Menter code, you need to have a version of Menter installed on your system.

## Installing Menter

Both the command line tool and the Java library are available via:

- Download from the [GitHub releases page](https://github.com/YanWittmann/menter-lang/releases)
- Build from [source](https://github.com/YanWittmann/menter-lang)

Once you have a version of Menter, you will have to create a script file that you can put on the system's PATH via which
you can call the `jar` file.

An example for a Windows batch file that you could add on the PATH:

```static
@echo off
java -jar C:/.../menter.jar %*
```

## Run Code (Command Line)

### MENTER_HOME

The Menter command line tool will search for Menter source files (`*.mtr`) in:

- The current working directory
- The directory specified by the `MENTER_HOME` environment variable

Menter will scan source files in these directories to detect available modules. These modules are not yet loaded, only
if a loaded module imports them.

### Files

You can run Menter code from files from the command line using the `menter` command.

To run a Menter source file, use the `-f` or `--file` flag followed by the path to the file:

```static
menter -f "path/to/file.mtr"
```

You can also run multiple files at once:

```static
menter -f "path/to/file1.mtr" "path/to/file2.mtr"
```

Menter will load these files in the order they are specified. If these files import modules from other files, Menter
will load them recursively before loading the next file.

### REPL

To get started, it might be easier to use the REPL (Read-Eval-Print Loop). It will launch an interactive shell that will
show you the result of each evaluation immediately. To start it, use the `repl` or `--repl` flag:

```static
menter repl
```

In the REPL, you can enter Menter code one line at a time and see the result of each evaluation immediately.

You can combine the `repl` flag with the `file` flag to load files into the REPL:

```static
menter repl -f "path/to/file.mtr"
```

### Documentation Code Boxes

The code boxes on this documentation page can be made interactive by using the `-gs` or `--guide-server` flag. This will
start a guide server that allows you to execute Menter code in the code boxes.

Possible further options for the guide server are:

- `unsafe`, `us` - Allows importing the `io` module
- a number, e.g. `8080` - The port on which the guide server will run

```static
menter -gs unsafe 8080
```

Once the guide server is running, you may refresh the current page and try out this code box by typing `numbers :: 5` or
`numbers += 5`

```result=[1, 2, 3, 4]
numbers = range(1, 4)
```

### Troubleshooting Code Boxes

If you are having trouble with the code boxes, you can try the following:

- Use a locally hosted version of the documentation:  
  The guide server will provide you with three URLs. Copy the topmost one and paste it into your browser. It will look
  something like this:
  ```static
  Server started on: http://192.168.1.22:8080/docs/introduction?host=192.168.1.22&port=8080
  ```
- Allow via browser settings:  
  Your browser may block requests to the guide server. You can allow these requests by clicking on the lock/shield icon
  in the address bar and then setting the "Improved tracing protection" (or similar prompt) switch to off.
  
  Why does this happen? This documentation page may be hosted on a https server, but the guide server is hosted on
  localhost, which uses http. CORS (Cross-Origin Resource Sharing) will block requests from https to http.
