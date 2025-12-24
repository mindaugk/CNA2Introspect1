MKPublisher — AWS SQS Publisher (Java)

Overview
- Simple Java CLI service that publishes messages to AWS SQS using AWS SDK v2.

Prerequisites
- Java 17+ and Maven installed.
- AWS credentials configured (environment variables, `~/.aws/credentials`, or EC2/ECS role).

Build
```bash
mvn clean package
```

Run
- Build:
```powershell
mvn clean package
```

- CLI example (provide queue URL and message):
```powershell
java -jar target/mkpublisher-1.0.0.jar https://sqs.us-east-1.amazonaws.com/123456789012/my-queue "Hello from MKPublisher" us-east-1
```
- Server mode (accepts POST):
```powershell
# start server on port 8080 (default)
java -jar target/mkpublisher-1.0.0.jar server 8080

# publish via HTTP POST: queueUrl in query string, raw body is message
curl -X POST "http://localhost:8080/publish?queueUrl=https://sqs.us-east-1.amazonaws.com/123456789012/my-queue&region=us-east-1" -d "Hello from HTTP"
```

- If `region` is omitted, `AWS_REGION` env var or `us-east-1` default is used.

Configuration
- You can set a default queue URL so you don't need to pass it on the CLI or HTTP requests. Create `config.properties` next to the jar or in the project root with:
```
QUEUE_URL=https://sqs.us-east-1.amazonaws.com/123456789012/my-queue
REGION=us-east-1
```
The app will also read `QUEUE_URL` and `AWS_REGION` from environment variables if present.

Notes
- Credentials follow the AWS SDK default provider chain.
- This is a minimal example. Add retries, metrics, and configuration as needed.

Docker
- Build the Docker image (from project root):
```bash
docker build -t mkpublisher:latest .
```
- Run the HTTP server container exposing port 8080 and passing any necessary env vars (e.g. `QUEUE_URL` or AWS creds):
```bash
# simple run, server listens on 8080 inside container
docker run -p 8080:8080 \
	-e QUEUE_URL=https://sqs.us-east-1.amazonaws.com/123456789012/my-queue \
	-e AWS_REGION=us-east-1 \
	-e AWS_ACCESS_KEY_ID=... -e AWS_SECRET_ACCESS_KEY=... \
	mkpublisher:latest
```

Notes:
- The container image uses a multi-stage build: Maven builds a shaded jar, then a slim JRE image runs it.
- Provide AWS credentials via the environment, mounted `~/.aws`, or an IAM role when running in AWS.
- To override the server port or run other CLI modes, pass arguments to the container, e.g. `docker run mkpublisher:latest server 9090`.
