resource "aws_sqs_queue" "mkpublisher_queue" {
  name                       = "mkpublisher-queue"
  delay_seconds              = 0
  max_message_size           = 32768    # 1 MB
  message_retention_seconds  = 345600   # 4 days
  receive_wait_time_seconds  = 0        # No long polling (change to 20 for long polling)
  visibility_timeout_seconds = 30
  sqs_managed_sse_enabled    = true     # AWS managed Server-Side Encryption

  tags = {
    Name        = "mkpublisher-queue"
    Environment = "dev"
    ManagedBy   = "terraform"
  }
}

output "queue_url" {
  description = "URL of the SQS queue"
  value       = aws_sqs_queue.mkpublisher_queue.url
}

output "queue_arn" {
  description = "ARN of the SQS queue"
  value       = aws_sqs_queue.mkpublisher_queue.arn
}
