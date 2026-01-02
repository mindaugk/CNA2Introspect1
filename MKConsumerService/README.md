# MKConsumer — AWS SQS Consumer (Java)

## Overview
- Simple Java service that consumes messages from AWS SQS using AWS SDK v2.
- Logs consumed messages at INFO level.
- Automatically deletes processed messages from the queue.

## Prerequisites
- Java 17+ and Maven installed.
- AWS credentials configured (environment variables, `~/.aws/credentials`, or EC2/ECS role).

## Build
```bash
mvn compile
```

Or to build a runnable JAR:
```bash
mvn clean package
```

## Run

### Using compiled classes:
```bash
mvn exec:java -Dexec.mainClass="com.example.mkconsumer.Main"
```

### Using JAR (after `mvn package`):
```bash
# Using default queue from config.properties
java -jar target/mkconsumer-1.0.0.jar

# Specifying queue URL and region
java -jar target/mkconsumer-1.0.0.jar https://sqs.us-east-1.amazonaws.com/872823407497/mkpublisher-queue us-east-1
```

## Configuration

Create `config.properties` in the project root or next to the JAR:

```properties
# Default SQS queue URL (optional)
QUEUE_URL=https://sqs.us-east-1.amazonaws.com/872823407497/mkpublisher-queue

# Default region (optional)
REGION=us-east-1

# Polling settings
POLL_INTERVAL_SECONDS=20
MAX_MESSAGES=10
WAIT_TIME_SECONDS=20
```

You can also set these via environment variables:
- `QUEUE_URL` - The SQS queue URL to consume from
- `AWS_REGION` - AWS region (defaults to `us-east-1`)
- `AWS_ACCESS_KEY_ID` and `AWS_SECRET_ACCESS_KEY` - AWS credentials

## Polling Configuration

- **POLL_INTERVAL_SECONDS**: Time to wait between polling cycles (default: 20)
- **MAX_MESSAGES**: Maximum number of messages to retrieve per poll (1-10, default: 10)
- **WAIT_TIME_SECONDS**: Long polling wait time in seconds (0-20, default: 20)

## Logging

The consumer uses SLF4J with simple logging. Messages are logged at INFO level:
```
[main] INFO com.example.mkconsumer.SqsConsumer - Received 3 message(s) from queue
[main] INFO com.example.mkconsumer.SqsConsumer - Processing message - MessageId: abc123...
[main] INFO com.example.mkconsumer.SqsConsumer - Message Body: Hello from MKPublisher
```

To adjust log level, create `simplelogger.properties` in `src/main/resources/`:
```properties
org.slf4j.simpleLogger.defaultLogLevel=info
org.slf4j.simpleLogger.log.com.example.mkconsumer=debug
```

## Docker

### Build the Docker image:
```bash
docker build -t mkconsumer:latest .
```

### Run the container:
```bash
docker run \
  -e QUEUE_URL=https://sqs.us-east-1.amazonaws.com/872823407497/mkpublisher-queue \
  -e AWS_REGION=us-east-1 \
  -e AWS_ACCESS_KEY_ID=... \
  -e AWS_SECRET_ACCESS_KEY=... \
  mkconsumer:latest
```

Or with IAM role (when running in AWS):
```bash
docker run \
  -e QUEUE_URL=https://sqs.us-east-1.amazonaws.com/872823407497/mkpublisher-queue \
  -e AWS_REGION=us-east-1 \
  mkconsumer:latest
```

## Notes
- The consumer runs continuously until interrupted (Ctrl+C).
- Messages are automatically deleted from the queue after successful processing.
- Credentials follow the AWS SDK default provider chain.
- Long polling is enabled by default (20 seconds) to reduce empty responses and costs.
- The service implements graceful shutdown on SIGTERM/SIGINT.

## Testing with MKPublisher

1. Start the consumer:
   ```bash
   java -jar target/mkconsumer-1.0.0.jar
   ```

2. In another terminal, send a message using MKPublisher:
   ```bash
   cd ../MKPublisherService
   java -jar target/mkpublisher-1.0.0.jar "Test message from publisher"
   ```

3. Observe the consumer logs showing the received message.
