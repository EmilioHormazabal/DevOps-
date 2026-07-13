#!/bin/bash
set -euxo pipefail

exec > >(tee /var/log/user-data.log) 2>&1

# Actualizaciones de seguridad y paquetes
yum update -y --security || yum update -y

# Herramientas base
amazon-linux-extras install docker -y
yum install -y git postgresql15-server amazon-cloudwatch-agent

# Habilitar e iniciar Docker
systemctl enable docker
systemctl start docker
usermod -aG docker ec2-user

# Instalar y habilitar PostgreSQL
/usr/bin/postgresql-setup --initdb || /usr/pgsql-15/bin/postgresql-15-setup initdb
systemctl enable postgresql
systemctl start postgresql

# Configurar CloudWatch Agent
mkdir -p /opt/aws/amazon-cloudwatch-agent/etc
cat > /opt/aws/amazon-cloudwatch-agent/etc/amazon-cloudwatch-agent.json << 'EOF'
{
  "agent": {
    "metrics_collection_interval": 60,
    "run_as_user": "root"
  },
  "metrics": {
    "aggregation_dimensions": [["InstanceId"]],
    "append_dimensions": {
      "AutoScalingGroupName": "${aws:AutoScalingGroupName}",
      "ImageId": "${aws:ImageId}",
      "InstanceId": "${aws:InstanceId}",
      "InstanceType": "${aws:InstanceType}"
    },
    "metrics_collected": {
      "cpu": {
        "measurement": ["cpu_usage_idle", "cpu_usage_iowait", "cpu_usage_user", "cpu_usage_system"],
        "metrics_collection_interval": 60,
        "resources": ["*"]
      },
      "disk": {
        "measurement": ["used_percent", "inodes_free"],
        "metrics_collection_interval": 60,
        "resources": ["*"]
      },
      "mem": {
        "measurement": ["mem_used_percent"],
        "metrics_collection_interval": 60
      },
      "netstat": {
        "measurement": ["tcp_established", "tcp_time_wait"],
        "metrics_collection_interval": 60
      }
    }
  },
  "logs": {
    "logs_collected": {
      "files": {
        "collect_list": [
          {
            "file_path": "/var/log/messages",
            "log_group_name": "/innovatech/ec2/syslog",
            "log_stream_name": "{instance_id}",
            "retention_in_days": 7
          },
          {
            "file_path": "/var/log/user-data.log",
            "log_group_name": "/innovatech/ec2/userdata",
            "log_stream_name": "{instance_id}",
            "retention_in_days": 7
          }
        ]
      }
    }
  }
}
EOF

# Iniciar CloudWatch Agent (ignorar errores de permisos IAM en lab Academy)
systemctl enable amazon-cloudwatch-agent || true
/opt/aws/amazon-cloudwatch-agent/bin/amazon-cloudwatch-agent-ctl -a fetch-and-run -m ec2 -c file:/opt/aws/amazon-cloudwatch-agent/etc/amazon-cloudwatch-agent.json || true
