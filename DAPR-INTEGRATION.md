# Dapr Integration Summary

## Changes Made

### 1. Dapr-Enabled Deployments
- **mkpublisher**: Added Dapr annotations to enable sidecar
- **mkconsumer**: Added Dapr annotations to enable sidecar with port 8080

### 2. Dapr Components Created
- **dapr-pubsub-component.yaml**: Configures AWS SNS/SQS as Dapr pubsub backend
- **dapr-subscription.yaml**: Subscribes mkconsumer to mkpublisher-queue topic

### 3. Code Changes

#### MKPublisher
- Created `DaprPublisher.java`: Publishes messages via Dapr HTTP API
- Updated `HttpPublisherServer.java`: Uses DaprPublisher instead of direct SQS calls
- Messages now published to: `http://localhost:3500/v1.0/publish/aws-pubsub/mkpublisher-queue`

#### MKConsumer  
- Created `DaprSubscriptionServer.java`: HTTP server that receives messages from Dapr
- Updated `Main.java`: Starts HTTP server instead of polling SQS
- Exposes endpoints:
  - `/dapr/subscribe`: Tells Dapr about subscriptions
  - `/messages`: Receives messages from Dapr

## Deployment Steps

```powershell
# 1. Apply Dapr components
kubectl apply -f c:\CNA\AWS\dapr-pubsub-component.yaml
kubectl apply -f c:\CNA\AWS\dapr-subscription.yaml

# 2. Rebuild and push images
cd c:\CNA\MKPublisherService
mvn clean package
docker build -t mkpublisher .
docker tag mkpublisher:latest 872823407497.dkr.ecr.us-east-1.amazonaws.com/mkpublisher:latest
docker push 872823407497.dkr.ecr.us-east-1.amazonaws.com/mkpublisher:latest

cd c:\CNA\MKConsumerService
mvn clean package
docker build -t mkconsumer .
docker tag mkconsumer:latest 872823407497.dkr.ecr.us-east-1.amazonaws.com/mkconsumer:latest
docker push 872823407497.dkr.ecr.us-east-1.amazonaws.com/mkconsumer:latest

# 3. Deploy updated applications
kubectl apply -f c:\CNA\AWS\Deployment-mkpublisher.yaml
kubectl apply -f c:\CNA\AWS\Deployment-mkconsumer.yaml

# 4. Verify Dapr sidecars
kubectl get pods
# Should show 2/2 containers for each pod

# 5. Check logs
kubectl logs -l app=mkpublisher -c mkpublisher -f
kubectl logs -l app=mkconsumer -c mkconsumer -f
```

## How It Works Now

```
External Client
    ↓ (HTTP POST)
Load Balancer
    ↓
MKPublisher Pod
    ├─ mkpublisher container → DaprPublisher → localhost:3500
    └─ daprd sidecar → AWS SQS (mkpublisher-queue)
                           ↓
MKConsumer Pod
    ├─ daprd sidecar → polls SQS → POST to localhost:8080/messages
    └─ mkconsumer container → processes message
```

## Benefits
- **Decoupling**: Apps don't need AWS SDK for messaging
- **Portability**: Easy to switch from SQS to Kafka/RabbitMQ
- **Features**: Automatic retries, dead-letter queues via Dapr
- **Observability**: Dapr provides metrics and tracing

## Testing
```powershell
# Test via load balancer (same as before)
curl -X POST "http://<your-lb-hostname>/publish" -d "Test message via Dapr"
```
