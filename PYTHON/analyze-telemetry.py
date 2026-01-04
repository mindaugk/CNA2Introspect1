import boto3
import json
import os

def analyze_file(bedrock_client, file_path):
    """Analyze a single file for missing telemetry points using AWS Bedrock."""
    if not os.path.exists(file_path):
        print(f"Warning: File not found: {file_path}")
        return None
    
    with open(file_path, 'r', encoding='utf-8') as f:
        code = f.read()
    
    prompt = f"""Analyze this microservice code for missing telemetry and observability:

**Focus Areas:**
1. Metrics - Request counts, latencies, error rates, queue depths
2. Logging - Error scenarios, key business operations, debugging info
3. Distributed Tracing - Trace spans for external calls, async operations
4. Health Checks - Readiness/liveness probe telemetry

**File: {os.path.basename(file_path)}**

```java
{code}
```

Provide actionable suggestions with:
- Specific line/method references
- Suggested metric names (e.g., "dapr.publish.success.count")
- Log message examples
- Trace span names
- Severity/priority (HIGH, MEDIUM, LOW)
"""

    body = json.dumps({
        "anthropic_version": "bedrock-2023-05-31",
        "max_tokens": 4000,
        "messages": [{
            "role": "user",
            "content": prompt
        }],
        "temperature": 0.7
    })
    
    try:
        response = bedrock_client.invoke_model(
            modelId='anthropic.claude-3-sonnet-20240229-v1:0',
            body=body
        )
        
        result = json.loads(response['body'].read())
        return result['content'][0]['text']
    except Exception as e:
        print(f"Error calling Bedrock: {e}")
        return None


def main():
    # Initialize Bedrock client
    print("Initializing AWS Bedrock client...")
    
    # Use AWS profile if AWS_PROFILE is set, otherwise use default credentials
    session = boto3.Session(profile_name=os.environ.get('AWS_PROFILE', 'cna-lab-1'))
    bedrock = session.client('bedrock-runtime', region_name='us-east-1')
    
    # Define files to analyze using relative paths
    script_dir = os.path.dirname(os.path.abspath(__file__))
    base_dir = os.path.join(script_dir, '..')
    
    files_to_analyze = [
        os.path.join(base_dir, 'MKPublisherService', 'src', 'main', 'java', 'com', 'example', 'mkpublisher', 'DaprPublisher.java'),
        os.path.join(base_dir, 'MKPublisherService', 'src', 'main', 'java', 'com', 'example', 'mkpublisher', 'HttpPublisherServer.java'),
        os.path.join(base_dir, 'MKPublisherService', 'src', 'main', 'java', 'com', 'example', 'mkpublisher', 'SqsPublisher.java'),
        os.path.join(base_dir, 'MKConsumerService', 'src', 'main', 'java', 'com', 'example', 'mkconsumer', 'Main.java'),
        os.path.join(base_dir, 'MKConsumerService', 'src', 'main', 'java', 'com', 'example', 'mkconsumer', 'SqsConsumer.java'),
        os.path.join(base_dir, 'MKConsumerService', 'src', 'main', 'java', 'com', 'example', 'mkconsumer', 'DaprSubscriptionServer.java'),
    ]
    
    print(f"\nAnalyzing {len(files_to_analyze)} files for telemetry gaps...\n")
    
    # Analyze each file
    for file_path in files_to_analyze:
        rel_path = os.path.relpath(file_path, base_dir)
        print('=' * 80)
        print(f"Analyzing: {rel_path}")
        print('=' * 80)
        
        suggestions = analyze_file(bedrock, file_path)
        
        if suggestions:
            print(suggestions)
            print('\n')
        else:
            print("No suggestions generated.\n")
    
    print('=' * 80)
    print("Analysis complete!")
    print('=' * 80)


if __name__ == '__main__':
    main()
