JAR_FILE=build/libs/java-nats-1.0-SNAPSHOT-all.jar

#
#
# Runs the NATS JetStream server w/ mostly default configuration - only customization
# is directing persistent data to be written to [java-nats]/data/nats directory.
#
#
nats: clear-screen
	@ tools/nats --store_dir data/nats --jetstream

#
#
# Compiles all classes and creates our executable fat jar.
#
#
build:
	@ ./gradlew shadowJar

#
#
# Simple, ephemeral fire-and-forget pub/sub
#
#
demo-pubsub-publisher: build clear-screen
	@ java -cp $(JAR_FILE) io.bridgekit.nats.pubsub.Publisher

demo-pubsub-subscriber: build clear-screen
	@ java -cp $(JAR_FILE) io.bridgekit.nats.pubsub.Subscriber


#
#
# Using the Key/Value store as a cache
#
#
demo-kv-cache-writer: build clear-screen
	@ java -cp $(JAR_FILE) io.bridgekit.nats.kvcache.CacheWriter

demo-kv-cache-reader: build clear-screen
	@ java -cp $(JAR_FILE) io.bridgekit.nats.kvcache.CacheReader


#
#
# Using the Key/Value store for centralized config
#
#
demo-kv-settings-writer: build clear-screen
	@ java -cp $(JAR_FILE) io.bridgekit.nats.kvsettings.SettingsWriter

demo-kv-settings-reader: build clear-screen
	@ java -cp $(JAR_FILE) io.bridgekit.nats.kvsettings.SettingsReader


#
#
# Kafka-like event streaming with consumer groups
#
#
demo-stream-publisher: build clear-screen
	@ java -cp $(JAR_FILE) io.bridgekit.nats.eventstream.StreamPublisher

demo-stream-consumer-confirm: build clear-screen
	@ java -cp $(JAR_FILE) io.bridgekit.nats.eventstream.StreamConsumer CONFIRM

demo-stream-consumer-fulfill: build clear-screen
	@ java -cp $(JAR_FILE) io.bridgekit.nats.eventstream.StreamConsumer FULFILL

demo-stream-consumer-trash: build clear-screen
	@ java -cp $(JAR_FILE) io.bridgekit.nats.eventstream.StreamConsumer TRASH


#
#
# Local object-store to replace S3
# Each variant accesses [java-nats]/data/file-X.jpg
#
#
demo-objects-writer-a: build clear-screen
	@ java -cp $(JAR_FILE) io.bridgekit.nats.objectstore.ObjectWriter file-a.jpg

demo-objects-writer-b: build clear-screen
	@ java -cp $(JAR_FILE) io.bridgekit.nats.objectstore.ObjectWriter file-b.jpg

demo-objects-writer-c: build clear-screen
	@ java -cp $(JAR_FILE) io.bridgekit.nats.objectstore.ObjectWriter file-c.jpg

demo-objects-reader-a: build clear-screen
	@ java -cp $(JAR_FILE) io.bridgekit.nats.objectstore.ObjectReader file-a.jpg

demo-objects-reader-b: build clear-screen
	@ java -cp $(JAR_FILE) io.bridgekit.nats.objectstore.ObjectReader file-b.jpg

demo-objects-reader-c: build clear-screen
	@ java -cp $(JAR_FILE) io.bridgekit.nats.objectstore.ObjectReader file-c.jpg


#
#
# The larger demo app with multiple services using NATS to communicate.
#
#
demo-app: build clear-screen
	@ java -cp $(JAR_FILE) io.bridgekit.nats.sampleapp.Main

demo-app-api: build clear-screen
	@ java -cp $(JAR_FILE) io.bridgekit.nats.sampleapp.Main API

demo-app-events: build clear-screen
	@ java -cp $(JAR_FILE) io.bridgekit.nats.sampleapp.Main EVENTS

#
#
# Wipes the directory where NATS stores all persistence info, effectively resetting all demos.
#
#
clean: clear-screen
	@ \
	rm -rf data/nats && \
	rm -rf data/downloads && \
	echo "Project squeaky clean!"

#
# Gets a nice clean terminal with nothing above it.
#
clear-screen:
	@ clear

.PHONY: build