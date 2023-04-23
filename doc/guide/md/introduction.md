# Menter Guide

## Introduction

Menter is a programming language that is **built in Java**, meaning that it can be run on any platform that supports
Java. It has a strong emphasis on simplicity and ease of use.

This guide will:

- Teach you the fundamentals of Menter.
- Show you how to use the Menter as a command line tool and in your Java application.

By the end, you should be able to create your own programs in Menter.

## A Quick Sample

Here's a small program that creates an array, maps them to new values and filters them:

```result=[1, 2, 3, 4];;;(x) -> { x > 4 };;;[6, 8]
numbers = range(1, 4);;;filterFunction = x -> x > 4;;;numbers.map(x -> x * 2).filter(filterFunction)
```

Did you know that you can make these code boxes interactive by running a local Menter server?
[Find out how here](execute_code.html)!

## Getting Started

### Download/Build

Both the command line tool and the Java library are available via:

- Download from the [GitHub releases page](https://github.com/YanWittmann/menter-lang/releases)
- Build from [source](https://github.com/YanWittmann/menter-lang)

### Java Library

After building and installing the Java library yourself using maven, you can add this dependency to your project:

```static---lang=xml
<dependency>
    <groupId>de.yanwittmann</groupId>
    <artifactId>menter-lang</artifactId>
    <version>version</version>
</dependency>
```

Navigate between pages using the arrow keys or the bar on the left.
