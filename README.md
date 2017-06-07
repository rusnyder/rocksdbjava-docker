# rocksdbjava-docker

[![Docker Stats](http://dockeri.co/image/rusnyder/rocksdbjava)](https://hub.docker.com/r/rusnyder/rocksdbjava/)

Dockerfile for [RocksJava](https://github.com/facebook/rocksdb/wiki/RocksJava-Basics), the [RocksDB](http://rocksdb.org/) Java bindings

The image is available on [Docker Hub](https://hub.docker.com/r/rusnyder/rocksdbjava/)


## Supported Docker Versions

The images have been tested on Docker 17.03.1-ce.


## Usage

### CLI

If you have your java binaries built by something else, you can run your program in a JVM that contains RocksDB JNI bindings by invoking any java command directly:

```bash
docker run \
  -v $(pwd)/my-jar.jar:/usr/lib/java/my-jar.jar \
  rusnyder/rocksdbjava \
  java -cp /usr/lib/java/my-jar.jar com.example.Main
```

### Dockerfile

Because construction of java commands (e.g. - classpath, etc.) and arrangement of files can quickly become a little more complex than tenable from the command line, it can make much more sense to use this image as the basis of a Dockerfile of Docker Compose file.  To make use in a Dockerfile, you'd likely just copy the classes/jars you needed over, set the classpath, then run java:

_Example borrowed from the [OpenJDK Docker Hub Repo](https://hub.docker.com/_/openjdk/)_

```Dockerfile
FROM rusnyder/rocksdbjava
COPY . /usr/src/myapp
WORKDIR /usr/src/myapp
RUN javac Main.java
CMD ["java", "Main"]
```

### Docker Compose

An even simpler way, and one that lends itself more to, well, composition, is [Docker Compose](https://docs.docker.com/compose/).  There are two "primary" usage modes I see with this Dockerfile, particularly when using Docker Compose:

1. Mount your entire project

    ```yaml
    ---
    version: '3'
    services:
      myapp:
        image: redowl/rocksdbjava
        volumes:
          - ./:/usr/src/myapp
        working_dir: /usr/src/myapp
        ports:
          - 8080:8080
        command: java Main
    ```

2. Mount your lib directory (containining jars) and put them all on the classpath

    ```yaml
    ---
    version: '3'
    services:
      myapp:
        image: redowl/rocksdbjava
        volumes:
          - ./libs/:/usr/lib/java/
        environment:
          CLASSPATH: /usr/lib/java/*.jar
        ports:
          - 8080:8080
        command: java Main
    ```

## Notes

### Classpath

There is some nuance here when dealing with and specifying the classpath.  Here are
a few things, in no particular order, to keep in mind:

  * You cannot use `-cp/-classpath` and `-jar` in the same command, as the
    classpath will only be loaded from one or ther other, and whichever is
    the last option to occur in the command-line string takes precedence.
  * You can add your own jars to the classpath of any java command run in this
    container in one of two ways (Note that using both simultaneously is not
    supported and may result in unpredictable behavior):
      1. Adding a `-cp/-classpath` option to your `CMD` (or `command` for Docker Compose)
      2. Defining the `CLASSPATH` environment variable

### RocksDB Storage

If you want to make your RocksDB storage persist from one set of containers to the
next (i.e. - in a dev workflow that involves frequent `docker-compose up/down` calls),
you may want to consider making sure that the path your application uses for its
RocksDB path is mounted as a volume.
