# RocksDB with Java bindings on CentOS 7

# Build from CentOS 7
FROM centos:7

# Install EPEL, updates, and dependences
RUN rm -rf /var/cache/yum \
 && yum install -y epel-release \
 && yum update -y \
 && yum install -y \
    bzip2 \
    bzip2-devel \
    gcc-c++ \
    gflags \
    git \
    java-1.8.0-openjdk \
    java-1.8.0-openjdk-devel \
    libzstd \
    make \
    snappy \
    snappy-devel \
    which \
    zlib \
    zlib-devel \
    zstd \
 && yum clean all

# Download, build, and install RocksDB and the Java bindings
ENV JAVA_HOME /usr/lib/jvm/java-1.8.0
RUN cd /usr/local/src && \
    git clone https://github.com/facebook/rocksdb.git && \
    cd rocksdb && \
    export DEBUG_LEVEL=0 && \
    make clean static_lib install-shared && \
    make jclean rocksdbjava && \
    cp java/target/rocksdbjni-*.jar /usr/lib/java && \
    cp java/target/librocksdbjni-*.so /usr/local/lib64 && \
    cd /usr/lib/java && \
    ln -s rocksdbjni-*.jar rocksdbjni.jar && \
    rm -rf /usr/local/src/rocksdb

# Mount the run script
COPY scripts/docker-entrypoint.sh /

# Default to intercepting any java command and injecting the RocksDB 
# java bindings jar into the classpath.
ENTRYPOINT ["/docker-entrypoint.sh"]
