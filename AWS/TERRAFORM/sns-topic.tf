# SNS Topic for Dapr Pub/Sub
resource "aws_sns_topic" "mkpublisher_dapr" {
  name = "mkpublisher-dapr-topic"

  tags = {
    Name        = "mkpublisher-dapr-topic"
    Environment = "dev"
    ManagedBy   = "terraform"
    Purpose     = "Dapr pub/sub messaging"
  }
}

# Subscribe the SQS queue to the SNS topic
resource "aws_sns_topic_subscription" "mkpublisher_dapr_sqs" {
  topic_arn = aws_sns_topic.mkpublisher_dapr.arn
  protocol  = "sqs"
  endpoint  = aws_sqs_queue.mkpublisher_dapr_queue.arn
}

# Allow SNS to send messages to the SQS queue
resource "aws_sqs_queue_policy" "mkpublisher_dapr_queue_policy" {
  queue_url = aws_sqs_queue.mkpublisher_dapr_queue.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Principal = {
          Service = "sns.amazonaws.com"
        }
        Action   = "sqs:SendMessage"
        Resource = aws_sqs_queue.mkpublisher_dapr_queue.arn
        Condition = {
          ArnEquals = {
            "aws:SourceArn" = aws_sns_topic.mkpublisher_dapr.arn
          }
        }
      }
    ]
  })
}

# Output the SNS topic ARN for Dapr component configuration
output "mkpublisher_dapr_topic_arn" {
  value       = aws_sns_topic.mkpublisher_dapr.arn
  description = "SNS topic ARN for Dapr pub/sub"
}
