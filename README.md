![current build status](https://github.com/gugugiyu/MagicWebServer/actions/workflows/maven-push.yml/badge.svg)
![stable version](https://img.shields.io/badge/version-1.2.0-blue)

# MagicWebServer
A hobby web server written in Java

## Why
I'm learning how socket communication works, and decide to rewrite my own web server for the sake of learning from (quite) literally scratch

## Features
Check out my [todo list](./TodoList.md) for more infos.

## Installation

The jar package is currently available under Github Packages

**Maven:**

```xml
<dependency>
  <groupId>com.github.magic</groupId>
  <artifactId>magic-ws</artifactId>
  <version>1.2.0</version>
</dependency>
```

```shell
mvn install
```

**Gradle:**

```sh
dependencies {
  implementation 'com.github.magic:magic-ws:1.2.0'
}
```

## Get started
Magic Web Server's syntax was inspired by Nodejs, therefore, you'll see the common http method 
verb-based approach when assigning the handler for each path

### Simple server

```java
//Initialize the server      
Server app = new Server();

//Serve "root" text at the root path
app.get("/", (req, res) -> res.send("root"));

//Listen on port 80
app.listen();
```

### Learn more from the examples
I've also prepared the [examples](./examples) folder with code snippet to demonstrate some cool
features this web server has to offer. Make sure to check them out.

And in case you wanna add another example, feel free to make a PR and I'll have a look at it.

## Documentation

WIP
