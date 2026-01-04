Introspect 1 Project

## Overview
- This project deploys most of AWS infrastructure using terraform. Terraform state files *.tfstate files have to be deleted before running terraform apply after lab environment was reset.
- Some of things still are deployed via kubectl (service accounts and deployments in EKS, Dapr PUB/SUB component and Dapr subscription).
- It does not use eksctl.

## Deployment

### Prerequisites
- AWS CLI configured with `cna-lab-1` profile
- Terraform installed
- kubectl installed
- Docker images built and pushed to ECR

### Step 1: Configure AWS Environment
```bash
export AWS_PROFILE_NAME=cna-lab-1
aws sts get-caller-identity --profile $AWS_PROFILE_NAME
export AWS_REGION=us-east-1
```

**PowerShell equivalent:**
```powershell
$env:AWS_PROFILE = "cna-lab-1"
aws sts get-caller-identity --profile cna-lab-1
$env:AWS_DEFAULT_REGION = "us-east-1"
```

### Step 2: Deploy Infrastructure with Terraform
```bash
cd AWS/TERRAFORM
terraform apply -auto-approve
```

This creates:
- VPC with public/private subnets across 4 availability zones
- EKS cluster (v1.31 in Auto Mode)
- IAM roles with IRSA for service accounts
- SQS queues (mkpublisher-queue, mkpublisher-dapr-queue)
- SNS topic (mkpublisher-dapr-topic) with SQS subscription
- ECR repositories
- Dapr installation via Helm

### Step 3: Configure kubectl for EKS
```bash
export AWS_EKS_CLUSTER_NAME=mk-cluster
echo $AWS_EKS_CLUSTER_NAME
aws --profile "$AWS_PROFILE_NAME" --region "$AWS_REGION" eks update-kubeconfig --name "$AWS_EKS_CLUSTER_NAME"
```

**PowerShell equivalent:**
```powershell
$AWS_EKS_CLUSTER_NAME = "mk-cluster"
aws --profile cna-lab-1 --region us-east-1 eks update-kubeconfig --name $AWS_EKS_CLUSTER_NAME
```

### Step 4: Build and Push Docker Images to ECR

#### Set Environment Variables
```bash
export AWS_PROFILE_NAME=cna-lab-1
aws sts get-caller-identity --profile $AWS_PROFILE_NAME
export AWS_REGION=us-east-1
export PUBLISHER_REPOSITORY_NAME=mkpublisher
export CONSUMER_REPOSITORY_NAME=mkconsumer
export AWS_ACCOUNT_ID=872823407497
```

**PowerShell equivalent:**
```powershell
$env:AWS_PROFILE = "cna-lab-1"
aws sts get-caller-identity --profile cna-lab-1
$AWS_REGION = "us-east-1"
$PUBLISHER_REPOSITORY_NAME = "mkpublisher"
$CONSUMER_REPOSITORY_NAME = "mkconsumer"
$AWS_ACCOUNT_ID = "872823407497"
```

#### Authenticate Docker to ECR
```bash
aws --profile "$AWS_PROFILE_NAME" --region "$AWS_REGION" ecr get-login-password | docker login --username AWS --password-stdin 872823407497.dkr.ecr.us-east-1.amazonaws.com
```

**PowerShell equivalent:**
```powershell
aws --profile cna-lab-1 --region $AWS_REGION ecr get-login-password | docker login --username AWS --password-stdin ${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com
```

#### Build and Push MKPublisher
```bash
cd ../../MKPublisherService  # Navigate to publisher service directory

# Build Java application
mvn clean package -DskipTests

# Build Docker image
docker build -t "$PUBLISHER_REPOSITORY_NAME":latest .

# Tag for ECR
docker tag "$PUBLISHER_REPOSITORY_NAME":latest 872823407497.dkr.ecr.us-east-1.amazonaws.com/"$PUBLISHER_REPOSITORY_NAME":latest

# Push to ECR
docker push 872823407497.dkr.ecr.us-east-1.amazonaws.com/"$PUBLISHER_REPOSITORY_NAME":latest
```

**PowerShell equivalent:**
```powershell
cd ..\..\MKPublisherService

# Build Java application
mvn clean package -DskipTests

# Build and push Docker image
docker build -t ${PUBLISHER_REPOSITORY_NAME}:latest .
docker tag ${PUBLISHER_REPOSITORY_NAME}:latest ${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com/${PUBLISHER_REPOSITORY_NAME}:latest
docker push ${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com/${PUBLISHER_REPOSITORY_NAME}:latest
```

#### Build and Push MKConsumer
```bash
cd ../MKConsumerService  # Navigate to consumer service directory

# Build Java application
mvn clean package -DskipTests

# Build Docker image
docker build -t "$CONSUMER_REPOSITORY_NAME":latest .

# Tag for ECR
docker tag "$CONSUMER_REPOSITORY_NAME":latest 872823407497.dkr.ecr.us-east-1.amazonaws.com/"$CONSUMER_REPOSITORY_NAME":latest

# Push to ECR
docker push 872823407497.dkr.ecr.us-east-1.amazonaws.com/"$CONSUMER_REPOSITORY_NAME":latest
```

**PowerShell equivalent:**
```powershell
cd ..\MKConsumerService

# Build Java application
mvn clean package -DskipTests

# Build and push Docker image
docker build -t ${CONSUMER_REPOSITORY_NAME}:latest .
docker tag ${CONSUMER_REPOSITORY_NAME}:latest ${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com/${CONSUMER_REPOSITORY_NAME}:latest
docker push ${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com/${CONSUMER_REPOSITORY_NAME}:latest
```

#### Verify Images in ECR
```bash
aws --profile "$AWS_PROFILE_NAME" --region "$AWS_REGION" ecr list-images --repository-name "$PUBLISHER_REPOSITORY_NAME"
aws --profile "$AWS_PROFILE_NAME" --region "$AWS_REGION" ecr list-images --repository-name "$CONSUMER_REPOSITORY_NAME"
```

**PowerShell equivalent:**
```powershell
aws --profile cna-lab-1 --region $AWS_REGION ecr list-images --repository-name $PUBLISHER_REPOSITORY_NAME
aws --profile cna-lab-1 --region $AWS_REGION ecr list-images --repository-name $CONSUMER_REPOSITORY_NAME
```

### Step 5: Deploy Kubernetes Resources
```bash
cd ../AWS  # Back to AWS directory

# Deploy service accounts with IRSA annotations
kubectl apply -f ServiceAccounts.yaml

# Deploy applications
kubectl apply -f Deployment-mkpublisher.yaml
kubectl apply -f Deployment-mkconsumer.yaml

# Deploy Dapr components
kubectl apply -f dapr-pubsub-component.yaml
kubectl apply -f dapr-subscription.yaml
```

#### Verify Cluster and Deployment Status

Verify that the cluster is accessible and resources are deployed:

```bash
# Check cluster nodes
kubectl get nodes

# Check Dapr system pods
kubectl get pods -n dapr-system

# Watch application pods come up
kubectl get pods -w
```

Expected output:
- Nodes should be in `Ready` state
- Dapr pods (`dapr-operator`, `dapr-sidecar-injector`, `dapr-placement-server`, `dapr-scheduler-server`) should be `Running`
- Application pods (`mkpublisher`, `mkconsumer`) should reach `Running` state with `2/2` containers ready (app + Dapr sidecar)

### Step 6: Restart Deployments (if needed)
If you update Docker images or Dapr configuration:
```bash
kubectl rollout restart deployment mkpublisher
kubectl rollout restart deployment mkconsumer
```

Check deployment status:
```bash
kubectl rollout status deployment mkpublisher
kubectl rollout status deployment mkconsumer
```

### Step 7: Get Load Balancer URL
```bash
kubectl get service mkpublisher-service
```

Look for the `EXTERNAL-IP` (AWS NLB DNS name).

### Testing

#### Test Direct SQS Communication
```bash
export LB_URL=<your-load-balancer-url>

# Publish via direct SQS
curl -X POST http://$LB_URL:8080/publish \
  -H "Content-Type: application/json" \
  -d '{"message": "Test via direct SQS"}'
```

**PowerShell equivalent:**
```powershell
$LB_URL = "your-load-balancer-url"

# Publish via direct SQS
Invoke-WebRequest -Uri "http://${LB_URL}:8080/publish" `
  -Method POST `
  -ContentType "application/json" `
  -Body '{"message": "Test via direct SQS"}'
```

#### Test Dapr Pub/Sub Communication
```bash
# Publish via Dapr (SNS → SQS)
curl -X POST http://$LB_URL:8080/publish-dapr \
  -H "Content-Type: application/json" \
  -d '{"message": "Test via Dapr pub/sub"}'
```

**PowerShell equivalent:**
```powershell
# Publish via Dapr (SNS → SQS)
Invoke-WebRequest -Uri "http://${LB_URL}:8080/publish-dapr" `
  -Method POST `
  -ContentType "application/json" `
  -Body '{"message": "Test via Dapr pub/sub"}'
```

### View Logs
```bash
# MKPublisher logs
kubectl logs -l app=mkpublisher -c mkpublisher --tail=50 -f

# MKConsumer logs
kubectl logs -l app=mkconsumer -c mkconsumer --tail=50 -f

# Dapr sidecar logs
kubectl logs -l app=mkpublisher -c daprd --tail=50 -f
```

### Cleanup
```bash
# Delete Kubernetes resources
kubectl delete -f dapr-subscription.yaml
kubectl delete -f dapr-pubsub-component.yaml
kubectl delete -f Deployment-mkconsumer.yaml
kubectl delete -f Deployment-mkpublisher.yaml
kubectl delete -f ServiceAccounts.yaml
kubectl delete service mkpublisher-service

# Destroy Terraform infrastructure
cd TERRAFORM
terraform destroy -auto-approve
```
 

