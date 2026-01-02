# IAM Role for EKS Node Group
resource "aws_iam_role" "eks_node_role" {
  name                 = "eksNodeRole"
  description          = "EKS node role"
  max_session_duration = 3600

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Principal = {
          Service = "ec2.amazonaws.com"
        }
        Action = "sts:AssumeRole"
      }
    ]
  })

  tags = {
    Name        = "eksNodeRole"
    Environment = "dev"
    ManagedBy   = "terraform"
  }
}

# Attach AmazonEC2ContainerRegistryReadOnly
resource "aws_iam_role_policy_attachment" "eks_node_ecr_policy" {
  role       = aws_iam_role.eks_node_role.name
  policy_arn = "arn:aws:iam::aws:policy/AmazonEC2ContainerRegistryReadOnly"
}

# Attach SQS Full Access for MKPublisher/Consumer
resource "aws_iam_role_policy_attachment" "eks_node_sqs_policy" {
  role       = aws_iam_role.eks_node_role.name
  policy_arn = "arn:aws:iam::aws:policy/AmazonSQSFullAccess"
}

# Outputs
output "eks_node_role_arn" {
  description = "ARN of the EKS node role"
  value       = aws_iam_role.eks_node_role.arn
}

output "eks_node_role_name" {
  description = "Name of the EKS node role"
  value       = aws_iam_role.eks_node_role.name
}
