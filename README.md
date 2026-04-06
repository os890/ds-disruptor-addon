# DS-AsyncEvents Add-on

A CDI portable extension that provides high-throughput asynchronous event broadcasting backed by the [LMAX Disruptor](https://github.com/LMAX-Exchange/disruptor) ring buffer.

## Overview

Standard CDI events are synchronous. This add-on introduces a `@ObservesAsynchronous` annotation and an injectable `AsynchronousEvent<E>` interface that route events through a Disruptor ring buffer, delivering them to CDI-managed observer beans on dedicated threads while respecting CDI scopes and dependency injection.

## Maven coordinates

```xml
<dependency>
    <groupId>org.os890.ds.addon</groupId>
    <artifactId>ds-async-event-addon</artifactId>
    <version>1.0.0</version>
</dependency>
```

The add-on requires the [LMAX Disruptor](https://github.com/LMAX-Exchange/disruptor) and [Apache DeltaSpike](https://deltaspike.apache.org/) (core + CDI-control) at runtime.

## Usage

### Firing asynchronous events

Inject `AsynchronousEvent<E>` and call `fire()`:

```java
@ApplicationScoped
public class MyService
{
    @Inject
    private AsynchronousEvent<OrderEvent> asyncEvent;

    public void process(OrderEvent event)
    {
        asyncEvent.fire(event);
    }
}
```

### Observing asynchronous events

Annotate a method parameter with `@ObservesAsynchronous`:

```java
@ApplicationScoped
public class OrderEventHandler
{
    public void onOrder(@ObservesAsynchronous OrderEvent event)
    {
        // called on a Disruptor thread
    }
}
```

Multiple observers are supported and are invoked in parallel on separate Disruptor event processors.

### Qualified events

Use CDI qualifiers to dispatch events to specific observers:

```java
// producer side
@Inject
@MyQualifier
private AsynchronousEvent<OrderEvent> qualifiedEvent;

// observer side
public void onQualified(@ObservesAsynchronous @MyQualifier OrderEvent event) { ... }
```

### Custom configuration

Subclass `AsynchronousEventConfig` (e.g. via `@Specializes`) to adjust the ring buffer size, executor, producer type, or wait strategy:

```java
@Specializes
@ApplicationScoped
public class CustomConfig extends AsynchronousEventConfig
{
    @Override
    public int getRingBufferSize()
    {
        return 8192;
    }
}
```

| Setting | Default |
|---------|---------|
| Ring buffer size | 4096 |
| Executor | `Executors.newCachedThreadPool()` |
| Producer type | `ProducerType.MULTI` |
| Wait strategy | `BlockingWaitStrategy` |

## Project structure

```
ds-disruptor-addon/
  addon/     -- the add-on library (jar)
  example/   -- demo web application (war, Jakarta EE migration pending)
```

## Requirements

- Java 25+
- Jakarta CDI 4.1 (e.g. OpenWebBeans 4.x or Weld 5.x)
- Apache DeltaSpike 2.0
- LMAX Disruptor 3.2+

## Building

```bash
mvn clean verify
```

## Testing

Tests use the [Dynamic CDI Test Bean Addon](https://github.com/os890/dynamic-cdi-test-bean-addon) with OpenWebBeans SE for CDI integration testing.

## License

[Apache License, Version 2.0](LICENSE)
