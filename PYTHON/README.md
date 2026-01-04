# Telemetry Analysis Tool

This tool uses **AWS Bedrock (Claude)** to analyze your microservice code and suggest missing telemetry points for better observability.

## What It Does

Analyzes Java microservice code to identify missing:
- **Metrics** - Request counts, latencies, error rates, resource utilization
- **Logs** - Error handling, debugging info, business events
- **Traces** - Distributed tracing spans for external calls
- **Health Checks** - Readiness/liveness probe telemetry

## Prerequisites

### 1. Python Environment
```powershell
# Install required packages
pip install boto3
```

### 2. AWS Credentials
Ensure your AWS profile (`cna-lab-1`) has Bedrock permissions:
```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "bedrock:InvokeModel"
      ],
      "Resource": "arn:aws:bedrock:us-east-1::foundation-model/anthropic.claude-3-5-sonnet-20241022-v2:0"
    }
  ]
}
```

Configure AWS credentials:
```powershell
aws configure --profile cna-lab-1
# Or set environment variables:
$env:AWS_PROFILE = "cna-lab-1"
$env:AWS_DEFAULT_REGION = "us-east-1"
```

### 3. Bedrock Model Access
Enable Claude 3.5 Sonnet in AWS Bedrock console:
1. Go to AWS Console → Bedrock → Model access
2. Request access to `anthropic.claude-3-5-sonnet-20241022-v2:0`
3. Wait for approval (usually instant)

## How to Run

### From PowerShell (Windows)
```powershell
# Set AWS profile
$env:AWS_PROFILE = "cna-lab-1"

# Navigate to PYTHON folder
cd C:\CNA\PYTHON

# Run the analysis
python analyze-telemetry.py
```

### From Command Prompt
```cmd
set AWS_PROFILE=cna-lab-1
cd C:\CNA\PYTHON
python analyze-telemetry.py
```

### Using Specific Profile
```powershell
# Set AWS credentials via environment
$env:AWS_PROFILE = "cna-lab-1"
$env:AWS_DEFAULT_REGION = "us-east-1"

python analyze-telemetry.py
```

## Output

The script will analyze these files:
- `MKPublisherService/src/main/java/com/example/mkpublisher/DaprPublisher.java`
- `MKPublisherService/src/main/java/com/example/mkpublisher/HttpPublisherServer.java`
- `MKPublisherService/src/main/java/com/example/mkpublisher/SqsPublisher.java`
- `MKConsumerService/src/main/java/com/example/mkconsumer/Main.java`
- `MKConsumerService/src/main/java/com/example/mkconsumer/SqsConsumer.java`
- `MKConsumerService/src/main/java/com/example/mkconsumer/DaprSubscriptionServer.java`

For each file, it provides:
- Specific method/line references
- Suggested metric names (e.g., `dapr.publish.latency.ms`)
- Log message examples
- Distributed trace span suggestions
- Priority levels (HIGH/MEDIUM/LOW)

## Example Output

```
================================================================================
Analyzing: MKPublisherService/src/main/java/com/example/mkpublisher/DaprPublisher.java
================================================================================

HIGH PRIORITY - Missing Metrics:
1. Method: publish()
   - Add latency metric: "dapr.publish.latency.ms"
   - Add success counter: "dapr.publish.success.count"
   - Add error counter: "dapr.publish.error.count"

MEDIUM PRIORITY - Missing Logs:
1. Line 20: Log Dapr endpoint URL on initialization
2. Line 30: Add structured logging for HTTP response details

LOW PRIORITY - Distributed Tracing:
1. Add trace span around HTTP request: "dapr.publish"
...
```

## Customization

### Analyze Different Files
Edit `files_to_analyze` list in the script:
```python
files_to_analyze = [
    os.path.join(base_dir, 'MyService', 'src', 'Main.java'),
    # Add more files...
]
```

### Change Bedrock Model
Modify `modelId` in `analyze_file()`:
```python
modelId='amazon.titan-text-premier-v1:0'  # Use Amazon Titan instead
```

### Adjust Analysis Depth
Change `max_tokens` for longer/shorter responses:
```python
"max_tokens": 4000  # Increase for more detailed suggestions
```

## Troubleshooting

### Error: "Could not connect to Bedrock"
- Verify AWS credentials: `aws sts get-caller-identity --profile cna-lab-1`
- Check region: `$env:AWS_DEFAULT_REGION = "us-east-1"`
- Verify Bedrock access in AWS Console

### Error: "Model not found"
- Request model access in Bedrock console
- Verify model ID is correct for your region

### Error: "File not found"
- Ensure you're running from `C:\CNA\PYTHON` directory
- Check that Java source files exist in parent directories

## Cost Estimate

AWS Bedrock Claude 3.5 Sonnet pricing:
- Input: $0.003 per 1K tokens
- Output: $0.015 per 1K tokens

Analyzing 6 files (~2K tokens each):
- Input: 12K tokens = $0.036
- Output: ~24K tokens = $0.36
- **Total per run: ~$0.40**

## Integration with CI/CD

Save suggestions to file:
```powershell
python analyze-telemetry.py > telemetry-report.txt
```

Run in GitHub Actions:
```yaml
- name: Analyze Telemetry
  env:
    AWS_ACCESS_KEY_ID: ${{ secrets.AWS_ACCESS_KEY_ID }}
    AWS_SECRET_ACCESS_KEY: ${{ secrets.AWS_SECRET_ACCESS_KEY }}
    AWS_DEFAULT_REGION: us-east-1
  run: |
    cd PYTHON
    python analyze-telemetry.py
```
