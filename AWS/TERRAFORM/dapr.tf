# Create dapr-system namespace
resource "kubernetes_namespace_v1" "dapr_system" {
  metadata {
    name = "dapr-system"
  }

  depends_on = [
    aws_eks_cluster.mk_cluster,
    null_resource.wait_for_cluster
  ]
}

# Install Dapr using Helm
resource "helm_release" "dapr" {
  name       = "dapr"
  repository = "https://dapr.github.io/helm-charts/"
  chart      = "dapr"
  namespace  = kubernetes_namespace_v1.dapr_system.metadata[0].name
  version    = "1.14.4"  # Specify version or use latest

  wait          = true
  wait_for_jobs = true
  timeout       = 600

  # Add tolerations for ALL Dapr components
  values = [
    yamlencode({
      global = {
        tolerations = [
          {
            key      = "CriticalAddonsOnly"
            operator = "Exists"
            effect   = "NoSchedule"
          }
        ]
      }
    })
  ]

  depends_on = [
    kubernetes_namespace_v1.dapr_system,
    kubernetes_storage_class_v1.gp3_auto_mode
  ]
}

# Output Dapr status
output "dapr_version" {
  description = "Dapr Helm chart version installed"
  value       = helm_release.dapr.version
}

output "dapr_status" {
  description = "Dapr installation status"
  value       = helm_release.dapr.status
}
