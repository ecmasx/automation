# Lab04 - Jenkins CI/CD Setup

## Project Description

Setting up Jenkins for DevOps task automation using Docker Compose. The project includes Jenkins Controller and SSH Agent with PHP-CLI for running CI/CD pipelines for PHP projects.

## Prerequisites

- Docker
- Docker Compose

## Steps for Setting up Jenkins Controller

### 1. Start Jenkins Controller

```bash
cd lab04
docker-compose up -d jenkins-controller
```

### 2. Configure via Web Interface

1. Open `http://localhost:8080`
2. Get the admin password:

```bash
docker exec jenkins-controller cat /var/jenkins_home/secrets/initialAdminPassword
```

1. Paste the password and follow on-screen instructions
2. Install suggested plugins
3. Create admin user account

## Steps for Setting up SSH Agent

### 1. Create SSH Keys

```bash
mkdir -p secrets
cd secrets
ssh-keygen -f jenkins_agent_ssh_key -t rsa -b 4096 -N ""
cd ..
```

### 2. Configure .env File

Create `.env` file and add the public key in a single line:

```env
JENKINS_AGENT_SSH_PUBKEY=ssh-rsa AAAAB3... (full key from jenkins_agent_ssh_key.pub)
```

### 3. Start SSH Agent

```bash
docker-compose up -d ssh-agent
```

## Steps for Creating and Configuring Jenkins Pipeline

### 1. Install SSH Agents Plugin

Manage Jenkins → Manage Plugins → Available → search for "SSH Build Agents Plugin" → Install

### 2. Add SSH Credentials

Manage Jenkins → Manage Credentials → System → Global credentials → Add Credentials:

- Kind: SSH Username with private key
- Username: `jenkins`
- Private Key: Enter directly (paste content of `secrets/jenkins_agent_ssh_key`)

### 3. Add Agent Node

Manage Jenkins → Manage Nodes and Clouds → New Node:

- Node name: `ssh-agent1`
- Type: Permanent Agent
- Remote root directory: `/home/jenkins/agent`
- Labels: `php-agent`
- Launch method: Launch agents via SSH
- Host: `ssh-agent`
- Credentials: select the created SSH key
- Host Key Verification: Non verifying Verification Strategy

### 4. Create Pipeline

New Item → Pipeline:

- Pipeline script from SCM → Git
- Repository URL: your PHP project GitHub repository URL
- Script Path: `Jenkinsfile`

### 5. Run Pipeline

Build Now → check Console Output

## Assignment Questions

### What are the advantages of using Jenkins for DevOps task automation?

1. Free and open-source tool
2. Large number of plugins for integration with different tools
3. Flexible pipeline configuration through code (Pipeline as Code)
4. Distributed builds on multiple agents
5. CI/CD process automation
6. Integration with Git and other version control systems
7. Detailed logs and build reports
8. Support for different platforms (Windows, Linux, macOS)
9. Full control over infrastructure

### What other types of Jenkins agents exist?

1. SSH Agents - connection via SSH
2. JNLP Agents - via Java Network Launch Protocol
3. Docker Agents - agents in Docker containers
4. Kubernetes Agents - agents as pods in Kubernetes
5. Cloud Agents - agents on cloud platforms (AWS, Azure, GCP)
6. Windows Agents - native Windows agents
7. Static Agents - permanent agents
8. Dynamic Agents - created on demand

### What problems did you encounter when setting up Jenkins and how did you solve them?

### Problem 1: SSH Agent won't connect

The agent couldn't connect to Jenkins Controller. Solution: checked that both containers are on the same network (`jenkins-network`), used service name `ssh-agent` instead of localhost, properly formatted the public key in `.env` (single line without line breaks).

### Problem 2: Wrong key format in .env

SSH agent didn't accept the key from environment variable. Solution: made sure the entire key is on one line, used `cat secrets/jenkins_agent_ssh_key.pub | tr -d '\n'` for formatting.

### Problem 3: PHP not installed in agent

PHP commands didn't work in pipeline. Solution: created Dockerfile with PHP-CLI installation, rebuilt the image `docker-compose build ssh-agent`.

### Problem 4: Pipeline can't find agent

Pipeline couldn't find agent with the required label. Solution: checked that label in Jenkinsfile exactly matches the node label (`php-agent`).

## File Structure

```Bash
lab04/
├── docker-compose.yml
├── Dockerfile
├── .env
├── Jenkinsfile
├── .gitignore
├── README.md
└── secrets/
    └── jenkins_agent_ssh_key*
```

## Basic Commands

```bash
# Start all services
docker-compose up -d

# Stop
docker-compose down

# View logs
docker logs jenkins-controller
docker logs ssh-agent

# Rebuild SSH Agent
docker-compose build ssh-agent
```
