# IAM Role for Service Account (IRSA) - SQS Access
# This allows pods with the service account to access SQS without needing IMDS

data "tls_certificate" "eks_oidc" {
  url = aws_eks_cluster.mk_cluster.identity[0].oidc[0].issuer
}

resource "aws_iam_openid_connect_provider" "eks_oidc" {
  client_id_list  = ["sts.amazonaws.com"]
  thumbprint_list = [data.tls_certificate.eks_oidc.certificates[0].sha1_fingerprint]
  url             = aws_eks_cluster.mk_cluster.identity[0].oidc[0].issuer

  tags = {
    Name        = "mk-cluster-oidc"
    Environment = "dev"
    ManagedBy   = "terraform"
  }
}

# IAM Role for MKPublisher Service Account
resource "aws_iam_role" "mkpublisher_irsa" {
  name = "mkpublisher-irsa-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Principal = {
          Federated = aws_iam_openid_connect_provider.eks_oidc.arn
        }
        Action = "sts:AssumeRoleWithWebIdentity"
        Condition = {
          StringEquals = {
            "${replace(aws_eks_cluster.mk_cluster.identity[0].oidc[0].issuer, "https://", "")}:sub" = "system:serviceaccount:default:mkpublisher-sa"
            "${replace(aws_eks_cluster.mk_cluster.identity[0].oidc[0].issuer, "https://", "")}:aud" = "sts.amazonaws.com"
          }
        }
      }
    ]
  })

  tags = {
    Name        = "mkpublisher-irsa-role"
    Environment = "dev"
    ManagedBy   = "terraform"
  }
}

# Inline policy for MKPublisher IRSA role - least privilege
resource "aws_iam_role_policy" "mkpublisher_policy" {
  name = "mkpublisher-sqs-sns-policy"
  role = aws_iam_role.mkpublisher_irsa.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Action = [
          "sqs:SendMessage",
          "sqs:GetQueueUrl",
          "sqs:GetQueueAttributes"
        ]
        Resource = [
          "arn:aws:sqs:us-east-1:872823407497:mkpublisher-queue",
          "arn:aws:sqs:us-east-1:872823407497:mkpublisher-dapr-queue"
        ]
      },
      {
        Effect = "Allow"
        Action = [
          "sns:Publish",
          "sns:GetTopicAttributes"
        ]
        Resource = "arn:aws:sns:us-east-1:872823407497:mkpublisher-dapr-topic"
      }
    ]
  })
}

# IAM Role for MKConsumer Service Account
resource "aws_iam_role" "mkconsumer_irsa" {
  name = "mkconsumer-irsa-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Principal = {
          Federated = aws_iam_openid_connect_provider.eks_oidc.arn
        }
        Action = "sts:AssumeRoleWithWebIdentity"
        Condition = {
          StringEquals = {
            "${replace(aws_eks_cluster.mk_cluster.identity[0].oidc[0].issuer, "https://", "")}:sub" = "system:serviceaccount:default:mkconsumer-sa"
            "${replace(aws_eks_cluster.mk_cluster.identity[0].oidc[0].issuer, "https://", "")}:aud" = "sts.amazonaws.com"
          }
        }
      }
    ]
  })

  tags = {
    Name        = "mkconsumer-irsa-role"
    Environment = "dev"
    ManagedBy   = "terraform"
  }
}

# Inline policy for MKConsumer IRSA role - least privilege
resource "aws_iam_role_policy" "mkconsumer_policy" {
  name = "mkconsumer-sqs-sns-policy"
  role = aws_iam_role.mkconsumer_irsa.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Action = [
          "sqs:ReceiveMessage",
          "sqs:DeleteMessage",
          "sqs:GetQueueUrl",
          "sqs:GetQueueAttributes"
        ]
        Resource = [
          "arn:aws:sqs:us-east-1:872823407497:mkpublisher-queue",
          "arn:aws:sqs:us-east-1:872823407497:mkpublisher-dapr-queue"
        ]
      },
      {
        Effect = "Allow"
        Action = [
          "sns:Subscribe",
          "sns:GetTopicAttributes"
        ]
        Resource = "arn:aws:sns:us-east-1:872823407497:mkpublisher-dapr-topic"
      }
    ]
  })
}

# Output the IAM role ARNs for use in Kubernetes service accounts
output "mkpublisher_irsa_role_arn" {
  value       = aws_iam_role.mkpublisher_irsa.arn
  description = "IAM role ARN for MKPublisher service account"
}

output "mkconsumer_irsa_role_arn" {
  value       = aws_iam_role.mkconsumer_irsa.arn
  description = "IAM role ARN for MKConsumer service account"
}
