<div align="center">
<img alt="Menter Thumbnail" src="https://github.com/YanWittmann/menter-lang/raw/main/doc/menter_logo.webp" width="200"/>

# Menter

A functional programming language written in Java.
</div>

<div>&nbsp;</div>

<table align="center">
<tr>
<td align="center">
<a href="https://yanwittmann.github.io/menter-lang-docs">
<img src="doc/book-open.svg" alt="Introduction" width="70"/>

<span>Menter Guide</span>
</a>
</td>
<td align="center">
<a href="https://github.com/YanWittmann/menter-lang#download-build">
<img src="doc/github.svg" alt="GitHub" width="70"/>

<span>Download/Build</span>
</a>
</td>
<td align="center">
<a href="https://yanwittmann.de">
<img src="doc/user.svg" alt="Yan Wittmann" width="70"/>

<span>Yan Wittmann</span>
</a>
</td>
</tr>
</table>

Menter is a programming language that is built in Java, meaning that it can be run on any platform that supports Java.
It has a strong emphasis on simplicity and ease of use.

## A Quick Sample

Here's a small program that creates an array, maps them to new values and filters them:

```javascript
numbers = range(1, 4)
  -> [1, 2, 3, 4]
filterFunction = x -> x > 4
  -> (x) -> { x > 4 }
numbers.map(x -> x * 2).filter(filterFunction)
  -> [6, 8]
```

## Build / Download

### Command line tool

Run this command in the root directory of the project using [Mavens](https://maven.apache.org/download.cgi) `mvn`
command:

```bash
mvn package -Pcmd-jar
```

You can then find the built jar in the `target` directory. Or simply download the latest release from the
[GitHub releases page](https://github.com/YanWittmann/menter-lang/releases).

### Java Library

If you want to use it as a library, build it using the following command:

```bash
mvn install
```

and use it using the following dependency:

```xml
<dependency>
    <groupId>de.yanwittmann</groupId>
    <artifactId>menter-lang</artifactId>
    <version>1.0.0</version>
</dependency>
```

## What is this project?

The main reason this language exists is to provide a simple, but powerful expression evaluator for the
[LaunchAnything Bar](https://github.com/YanWittmann/launch-anything) project. It is designed to be easy to use and to be
able to be embedded into other projects.

Another reason is, that I have always been interested in compilers/interpreters and wanted to finally try and dip my
toes into the topic by creating a simple programming language.
Seeing as I mainly am a Java developer, I decided to write it in Java. This was also supported by the fact that the
Launch Anything project is also written in Java.

But to speak in buzzwords, Menter is a:
functional, single threaded, dynamically typed, interpreted, object oriented, garbage collected, statically scoped,
expression based, programming language, that can be extended and adjusted for your needs with Java code. 
