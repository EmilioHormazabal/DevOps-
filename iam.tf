data "aws_iam_role" "lab_role" {
  name = "LabRole"
}

# Instance Profile para que las EC2 asuman el LabRole
resource "aws_iam_instance_profile" "ec2" {
  name = "${var.project_name}-ec2-profile"
  role = data.aws_iam_role.lab_role.name
}

