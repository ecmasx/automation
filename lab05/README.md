# IWNO05: Ansible Playbook for Server Configuration

## Project Description

This project implements a comprehensive CI/CD pipeline using Jenkins, Ansible, and Docker for automated server configuration and PHP application deployment. The infrastructure consists of:

- **Jenkins Controller**: Central orchestration point for all CI/CD pipelines
- **SSH Agent**: Executes PHP build and test operations
- **Ansible Agent**: Executes Ansible playbooks for server configuration
- **Test Server**: Target environment for application deployment and testing

The project demonstrates Infrastructure as Code (IaC) principles using Ansible for automated server configuration and Jenkins for pipeline orchestration.

## Prerequisites

- Docker
- Docker Compose
- Git
- Basic understanding of Jenkins, Ansible, and PHP

## Architecture Overview

```Bash
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│  Jenkins        │    │  SSH Agent      │    │  Ansible Agent  │    │  Test Server    │
│  Controller     │◄──►│  (PHP Build)    │◄──►│  (Server Config)│◄──►│  (Apache+PHP)   │
│  (Port 8080)   │    │                 │    │                 │    │  (Port 8081)    │
└─────────────────┘    └─────────────────┘    └─────────────────┘    └─────────────────┘
```

## Setup Instructions

### 1. Start Infrastructure

```bash
cd lab05
docker-compose up -d
```

This will start all services:

- Jenkins Controller: http://localhost:8080
- Test Server: http://localhost:8081

### 2. Configure Jenkins Controller

1. Open http://localhost:8080
2. Get the admin password:
   ```Bash
   docker exec jenkins-controller cat /var/jenkins_home/secrets/initialAdminPassword
   ```
3. Follow the on-screen setup wizard
4. Install required plugins: Docker, Docker Pipeline, GitHub Integration, SSH Agent

### 3. Set up SSH Agent

1. Go to Manage Jenkins → Manage Credentials → System → Global credentials → Add Credentials
2. Add SSH Username with private key:
   - Kind: SSH Username with private key
   - Username: `jenkins`
   - Private Key: Enter directly (paste content of `secrets/jenkins_ssh_agent_key`)
3. Add SSH Agent node:
   - Manage Jenkins → Manage Nodes and Clouds → New Node
   - Node name: `ssh-agent1`
   - Remote root directory: `/home/jenkins/agent`
   - Labels: `php-agent`
   - Launch method: Launch agents via SSH
   - Host: `ssh-agent`
   - Credentials: select the created SSH key
   - Host Key Verification: Non verifying Verification Strategy

### 4. Set up Ansible Agent

1. Add SSH credentials for Ansible agent:
   - Kind: SSH Username with private key
   - Username: `ansible`
   - Private Key: Enter directly (paste content of `secrets/jenkins_ansible_agent_key`)
2. Add Ansible Agent node:
   - Node name: `ansible-agent1`
   - Remote root directory: `/home/ansible`
   - Labels: `ansible-agent`
   - Launch method: Launch agents via SSH
   - Host: `ansible-agent`
   - Credentials: select the created SSH key
   - Host Key Verification: Non verifying Verification Strategy

## Jenkins Pipelines

### 1. PHP Build and Test Pipeline

Create a new Jenkins pipeline job:

- Job name: `PHP-Build-Test`
- Pipeline script from SCM
- Repository URL: Your PHP project GitHub repository
- Script Path: `pipelines/php_build_and_test_pipeline.groovy`

**Stages:**

- Checkout: Clone the PHP repository
- Install Dependencies: Install Composer packages
- Code Analysis: Run PHPStan and PHP CS Fixer
- Unit Tests: Execute PHPUnit tests
- Test Results: Publish test reports and coverage
- Build Artifact: Create deployment package

### 2. Ansible Setup Pipeline

Create a new Jenkins pipeline job:

- Job name: `Ansible-Setup`
- Pipeline script from SCM
- Repository URL: This repository
- Script Path: `pipelines/ansible_setup_pipeline.groovy`

**Stages:**

- Checkout Ansible Playbooks: Access Ansible configurations
- Verify Ansible Setup: Test connectivity and installation
- Validate Ansible Playbook: Check syntax and dry run
- Execute Ansible Playbook: Configure test server
- Verify Configuration: Test Apache2 and PHP setup
- Test PHP Application: Verify web server functionality

### 3. PHP Deploy Pipeline

Create a new Jenkins pipeline job:

- Job name: `PHP-Deploy`
- Pipeline script from SCM
- Repository URL: This repository
- Script Path: `pipelines/php_deploy_pipeline.groovy`

**Stages:**

- Checkout PHP Application: Get application source code
- Prepare Deployment: Create deployment package
- Create Backup: Backup current deployment
- Deploy Application: Copy files to test server
- Configure Application: Set up configuration and security
- Verify Deployment: Test application functionality
- Health Check: Verify service status

## File Structure

```Bash
lab05/
├── compose.yaml                 # Docker Compose configuration
├── .env                         # Environment variables with SSH keys
├── Dockerfile.ssh_agent         # SSH agent with PHP tools
├── Dockerfile.ansible_agent     # Ansible agent configuration
├── Dockerfile.test_server       # Test server configuration
├── ansible/                     # Ansible configurations
│   ├── hosts.ini               # Inventory file
│   ├── setup_test_server.yml   # Server configuration playbook
│   └── php-app.conf.j2       # Apache virtual host template
├── pipelines/                   # Jenkins pipeline definitions
│   ├── php_build_and_test_pipeline.groovy
│   ├── ansible_setup_pipeline.groovy
│   └── php_deploy_pipeline.groovy
├── secrets/                     # SSH keys and credentials
└── readme.md                    # This file
```

## Testing the Deployment

1. Start all services: `docker-compose up -d`
2. Configure Jenkins Controller and agents
3. Run the Ansible Setup pipeline to configure the test server
4. Run the PHP Build and Test pipeline to create application artifact
5. Run the PHP Deploy pipeline to deploy the application
6. Access the deployed application at http://localhost:8081

## Assignment Questions

### What are the advantages of using Ansible for server configuration?

1. **Infrastructure as Code**: Server configurations are defined in code, making them versionable, testable, and repeatable
2. **Idempotency**: Ansible ensures the desired state is achieved regardless of the current state
3. **Agentless Architecture**: No additional software needs to be installed on target servers (only SSH and Python)
4. **Simple YAML Syntax**: Human-readable configuration files that are easy to understand and modify
5. **Extensive Module Library**: Pre-built modules for managing various services and platforms
6. **Rollback Capabilities**: Easy to revert to previous configurations
7. **Scalability**: Can manage thousands of servers from a single control machine
8. **Integration**: Works well with CI/CD pipelines and other DevOps tools
9. **Security**: Uses SSH for secure communication and supports various authentication methods
10. **Cost-Effective**: Open-source with no licensing costs

### What other Ansible modules exist for configuration management?

1. **Package Management**:
   - `apt`: Manage Debian/Ubuntu packages
   - `yum`/`dnf`: Manage RedHat/CentOS packages
   - `pip`: Manage Python packages
   - `npm`: Manage Node.js packages

2. **Service Management**:
   - `systemd`: Manage systemd services
   - `service`: Manage traditional services
   - `docker_container`: Manage Docker containers

3. **File Management**:
   - `copy`: Copy files to remote servers
   - `template`: Process template files with variables
   - `file`: Manage file properties and directories
   - `lineinfile`: Manage individual lines in files

4. **User and Security**:
   - `user`: Manage user accounts
   - `group`: Manage user groups
   - `authorized_key`: Manage SSH keys
   - `selinux`: Manage SELinux policies

5. **Database Management**:
   - `mysql_db`: Manage MySQL databases
   - `postgresql_db`: Manage PostgreSQL databases
   - `mongodb_db`: Manage MongoDB databases

6. **Cloud and Network**:
   - `ec2`: Manage AWS EC2 instances
   - `azure_rm`: Manage Azure resources
   - `gcp_compute`: Manage Google Cloud resources
   - `net_interface`: Manage network interfaces

7. **Monitoring and Logging**:
   - `monit`: Manage Monit services
   - `nrpe`: Manage NRPE monitoring
   - `syslog`: Manage syslog configuration

8. **Configuration Management**:
   - `git_config`: Manage Git configuration
   - `cron`: Manage cron jobs
   - `hostname`: Manage system hostname
   - `timezone`: Manage system timezone

### What problems did you encounter when creating the Ansible playbook and how did you solve them?

#### Problem 1: SSH Key Authentication Issues

**Problem**: Ansible couldn't connect to the test server due to SSH key permissions and configuration.
**Solution**:

- Ensured proper file permissions (600 for private keys, 644 for public keys)
- Configured SSH to disable password authentication and enable key-based authentication
- Used `StrictHostKeyChecking=no` in Ansible configuration to avoid host key verification issues

#### Problem 2: Apache Virtual Host Configuration

**Problem**: Apache wasn't serving PHP files correctly after configuration.
**Solution**:

- Created a proper virtual host template with correct directives
- Enabled required Apache modules (php7.4, rewrite)
- Ensured proper file permissions and ownership for web directories
- Disabled default site and enabled the PHP application site

#### Problem 3: PHP Module Compatibility

**Problem**: Different PHP versions had different module names and availability.
**Solution**:

- Used conditional logic in the playbook to handle different PHP versions
- Created a list of common PHP extensions and installed them using the package manager
- Added error handling to gracefully handle missing modules

#### Problem 4: Idempotency Issues

**Problem**: Running the playbook multiple times caused unexpected behavior.
**Solution**:

- Used Ansible's built-in idempotency features
- Added proper state management (present/absent) for all resources
- Implemented handlers for service restarts to avoid unnecessary restarts

#### Problem 5: Docker Network Communication

**Problem**: Containers couldn't communicate with each other using service names.
**Solution**:

- Ensured all services were on the same Docker network
- Used service names instead of IP addresses for inter-container communication
- Verified network connectivity using `docker network inspect`

#### Problem 6: Volume Mount Permissions

**Problem**: Ansible couldn't access mounted volumes due to permission issues.
**Solution**:

- Set correct ownership and permissions for mounted volumes
- Used the same user (ansible) across containers and volumes
- Configured proper umask settings in Docker containers

#### Problem 7: Jenkins Agent Connection Issues

**Problem**: Jenkins agents couldn't connect to the controller due to SSH key configuration and container networking.
**Solution**:

- Generated separate SSH key pairs for each agent type
- Configured proper environment variables for public key injection
- Set up Jenkins credentials with correct private keys
- Used service names for host resolution within Docker network
- Implemented proper SSH configuration in agent Dockerfiles

#### Problem 8: Container Startup Dependencies

**Problem**: Services failed to start due to missing dependencies or incorrect startup order.
**Solution**:

- Added proper dependency management in Docker Compose
- Implemented health checks and restart policies
- Used proper command chaining in container startup scripts
- Added volume mounts for persistent data storage

## Troubleshooting

### Common Issues and Solutions

1. **Jenkins Agent Connection Failed**
   - Check SSH key permissions
   - Verify network connectivity between containers
   - Ensure correct service names in configuration

2. **Ansible Playbook Fails**
   - Check syntax with `ansible-playbook --syntax-check`
   - Verify inventory file format
   - Test connectivity with `ansible all -m ping`

3. **PHP Application Not Working**
   - Check Apache error logs: `docker exec test-server tail -f /var/log/apache2/error.log`
   - Verify PHP installation: `docker exec test-server php --version`
   - Check file permissions in web directory

4. **Docker Container Issues**
   - View logs: `docker logs [container-name]`
   - Check container status: `docker ps -a`
   - Restart containers: `docker-compose restart`

## Conclusion

This project demonstrates a complete CI/CD pipeline using modern DevOps tools and practices. The combination of Jenkins for orchestration, Ansible for configuration management, and Docker for containerization provides a robust, scalable, and maintainable infrastructure for PHP application deployment.

The implementation follows Infrastructure as Code principles, making the entire setup reproducible and version-controlled. The modular design allows for easy extension and modification to support different applications and deployment scenarios.
