# Java NATS Demos

These are the examples we went over during the Indianapolis Java User Group
on June 18, 2025.

## Running Demos

These demos run in your terminal/console. Most require you to have 3 different terminal sessions
open: one to run the NATS server/broker, one to run the program that writes/publishes messages, and
another that consumes those messages. All of these are managed by a `make` command.

### Basic Pub/Sub

A simple scenario where one service publishes “order.placed” events, and another remote service receives messages when that happens.

```shell
# In terminal A
make nats

# In terminal B
make demo-pubsub-publisher

# In terminal C
make demo-pubsub-subscriber
```

### Key/Value Store Basics

A very crude rate-limiting scenario where one service stores request counts in the cache, and another periodically reads/reports on those totals. Data is ephemeral.

```shell
# In terminal A
make nats

# In terminal B
make demo-kv-cache-writer

# In terminal C
make demo-kv-cache-reader
```

### Key/Value Store Watchers

You have settings/configs that need to be known at startup, but may change at runtime. The writer makes periodic changes to the state while the reader listens for and reacts to those changes.

```shell
# In terminal A
make nats

# In terminal B
make demo-kv-settings-writer

# In terminal C
make demo-kv-settings-reader
```

### Object Store Basics

You’re saving database records that have some large file associated with them. Rather than storing those in the DB, keep the files in an object store with metadata that links back to the record. The reader app downloads the stored file to data/out.

```shell
# In terminal A
make nats

# In terminal B
make demo-objects-writer-a
make demo-objects-reader-a

# Or...
make demo-objects-writer-b
make demo-objects-reader-b

# Or...
make demo-objects-writer-c
make demo-objects-reader-c
```

### Event Streaming & Consumer Groups

An order processing/fulfillment program that creates/cancels orders. Those events trigger other workflow tasks in a durable, load-balanced fashion.

```shell
# In terminal A
make nats

# In terminal B
make demo-stream-publisher

# In terminal C, D, and E
make demo-stream-consumer-confirm
make demo-stream-consumer-fulfill
make demo-stream-consumer-trash
```

### Sample Multi-Service App

A basic ordering platform with multiple services. Some rely on HTTP/API calls - others rely on events to invoke them.

Workflows like cancelling an order are loosely coupled, so the order service doesn’t know about the confirmation email or the payment refund.

```shell
# In terminal A
make nats

# In terminal B
make demo-app

# Or in separate terminals to flex event-based load balancing
make demo-app-api
make demo-app-events
make demo-app-events
```

## Additional Resources

NATS Docs and Examples  
https://docs.nats.io  
https://natsbyexample.com

Examples Source Code  
https://github.com/bridgekit-io/jug-nats-demo

Go Framework Based on NATS  
https://github.com/bridgekit-io/frodo