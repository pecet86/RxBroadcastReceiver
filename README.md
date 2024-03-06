# ReactiveBroadcast
Simple RxJava3 binding for Android BroadcastReceiver

[![Build Status](https://travis-ci.org/karczews/RxBroadcastReceiver.svg?branch=master)](https://travis-ci.org/karczews/RxBroadcastReceiver)
[![codecov](https://codecov.io/gh/karczews/RxBroadcastReceiver/branch/master/graph/badge.svg)](https://codecov.io/gh/karczews/RxBroadcastReceiver)
[![Maven Central](https://img.shields.io/maven-central/v/com.github.karczews/rx2-broadcast-receiver.svg?style=flat)](https://repo.maven.apache.org/maven2/com/github/karczews/rx2-broadcast-receiver/) 
[![Nexus Snapshots](https://img.shields.io/nexus/s/https/oss.sonatype.org/com.github.karczews/rx2-broadcast-receiver.svg)](https://oss.sonatype.org/content/repositories/snapshots/com/github/karczews/rx2-broadcast-receiver/)
[![License](https://img.shields.io/badge/license-Apache%202.0-blue.svg)](https://github.com/karczews/RxBroadcastReceiver/blob/master/LICENSE)


Usage
--------
```java
RxBroadcastReceivers.fromIntentFilter(context, intentFilter)
        .subscribe(intent -> {
            // do something with broadcast
        });
```


What's new:
- 1.0.2 library utilizes [Context#registerReceiver(BroadcastReceiver, filter, broadcastPermission, scheduler)](https://goo.gl/ytDVGb) method when subscription occurs on background thread looper.
- 1.0.5 fixed problem with manifest merger.

Download
--------

To use library with Gradle

```groovy
dependencies {
  implementation 'com.github.karczews:rx2-broadcast-receiver:1.0.6'
}
```

or using Maven:

```xml
<dependency>
    <groupId>com.github.karczews</groupId>
    <artifactId>rx2-broadcast-receiver</artifactId>
    <version>1.0.6</version>
</dependency>
```
