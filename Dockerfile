# RocksDB with Java bindings on CentOS 7

# Build from CentOS 7
FROM centos:7

# Install EPEL and update all to latest
RUN yum install -y epel-release && yum clean all && yum update -y

# Install build tools
RUN yum install -y \
    bzip2 \
    bzip2-devel \
    gcc-c++ \
    gflags \
    git \
    libzstd \
    make \
    snappy \
    snappy-devel \
    which \
    zlib \
    zlib-devel \
    zstd

# Download and install the Java Runtime, then set JAVA_HOME
RUN yum install -y java-1.8.0-openjdk java-1.8.0-openjdk-devel
ENV JAVA_HOME /usr/lib/jvm/java-1.8.0

# Download, build, and install RocksDB and the Java bindings
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
RUN chmod +x /docker-entrypoint.sh

# Default to intercepting any java command and injecting the RocksDB 
# java bindings jar into the classpath.
ENTRYPOINT ["/docker-entrypoint.sh"]
