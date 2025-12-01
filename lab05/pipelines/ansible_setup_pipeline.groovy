pipeline {
    agent {
        label 'ansible-agent'
    }
    
    environment {
        ANSIBLE_INVENTORY = '/ansible/hosts.ini'
        ANSIBLE_PLAYBOOK = '/ansible/setup_test_server.yml'
        WORKSPACE_DIR = '/home/ansible'
    }
    
    stages {
        stage('Checkout Ansible Playbooks') {
            steps {
                script {
                    // Clean workspace
                    cleanWs()
                    
                    // Clone repository containing Ansible playbooks
                    // For this example, we assume playbooks are already mounted in /ansible
                    echo 'Using Ansible playbooks from mounted volume...'
                    
                    // Verify Ansible playbook exists
                    sh """
                        ls -la ${ANSIBLE_PLAYBOOK}
                        cat ${ANSIBLE_INVENTORY}
                    """
                }
            }
        }
        
        stage('Verify Ansible Setup') {
            steps {
                script {
                    echo 'Verifying Ansible installation and configuration...'
                    
                    // Check Ansible version
                    sh 'ansible --version'
                    
                    // Test connectivity to test server
                    sh """
                        ansible all -i ${ANSIBLE_INVENTORY} -m ping
                    """
                    
                    // Gather facts from test server
                    sh """
                        ansible all -i ${ANSIBLE_INVENTORY} -m setup | head -20
                    """
                }
            }
        }
        
        stage('Validate Ansible Playbook') {
            steps {
                script {
                    echo 'Validating Ansible playbook syntax...'
                    
                    // Check playbook syntax
                    sh """
                        ansible-playbook ${ANSIBLE_PLAYBOOK} -i ${ANSIBLE_INVENTORY} --syntax-check
                    """
                    
                    // Dry run to check for potential issues
                    sh """
                        ansible-playbook ${ANSIBLE_PLAYBOOK} -i ${ANSIBLE_INVENTORY} --check
                    """
                }
            }
        }
        
        stage('Execute Ansible Playbook') {
            steps {
                script {
                    echo 'Executing Ansible playbook for test server configuration...'
                    
                    // Run the playbook with verbose output
                    sh """
                        ansible-playbook ${ANSIBLE_PLAYBOOK} -i ${ANSIBLE_INVENTORY} -v
                    """
                }
            }
        }
        
        stage('Verify Configuration') {
            steps {
                script {
                    echo 'Verifying server configuration...'
                    
                    // Check Apache2 status
                    sh """
                        ansible test-servers -i ${ANSIBLE_INVENTORY} -m systemd -a "name=apache2 state=started"
                    """
                    
                    // Check PHP installation
                    sh """
                        ansible test-servers -i ${ANSIBLE_INVENTORY} -m command -a "php --version"
                    """
                    
                    // Check web directory
                    sh """
                        ansible test-servers -i ${ANSIBLE_INVENTORY} -m file -a "path=/var/www/html/php-app state=directory"
                    """
                    
                    // Test web server response
                    sh """
                        ansible test-servers -i ${ANSIBLE_INVENTORY} -m uri -a "url=http://localhost method=GET"
                    """
                }
            }
        }
        
        stage('Test PHP Application') {
            steps {
                script {
                    echo 'Testing PHP application functionality...'
                    
                    // Test PHP info page
                    sh """
                        ansible test-servers -i ${ANSIBLE_INVENTORY} -m uri -a "url=http://localhost/info.php method=GET return_content=yes" | grep -i "php version"
                    """
                    
                    // Test index page
                    sh """
                        ansible test-servers -i ${ANSIBLE_INVENTORY} -m uri -a "url=http://localhost/index.php method=GET return_content=yes" | grep -i "php application is running"
                    """
                }
            }
        }
    }
    
    post {
        always {
            echo 'Ansible setup pipeline completed.'
            
            // Generate Ansible execution report
            script {
                try {
                    sh """
                        echo "=== Ansible Execution Summary ===" > ansible-report.txt
                        echo "Build: ${BUILD_NUMBER}" >> ansible-report.txt
                        echo "Date: $(date)" >> ansible-report.txt
                        echo "Commit: ${GIT_COMMIT}" >> ansible-report.txt
                        echo "Branch: ${GIT_BRANCH}" >> ansible-report.txt
                        echo "" >> ansible-report.txt
                        echo "=== Server Information ===" >> ansible-report.txt
                        ansible test-servers -i ${ANSIBLE_INVENTORY} -m setup | grep -E "(ansible_distribution|ansible_kernel)" >> ansible-report.txt
                        echo "" >> ansible-report.txt
                        echo "=== Service Status ===" >> ansible-report.txt
                        ansible test-servers -i ${ANSIBLE_INVENTORY} -m systemd -a "name=apache2" >> ansible-report.txt
                        echo "" >> ansible-report.txt
                        echo "=== PHP Information ===" >> ansible-report.txt
                        ansible test-servers -i ${ANSIBLE_INVENTORY} -m command -a "php --version" >> ansible-report.txt
                    """
                    archiveArtifacts artifacts: 'ansible-report.txt', allowEmptyArchive: true
                } catch (Exception e) {
                    echo "Failed to generate Ansible report: ${e.getMessage()}"
                }
            }
            
            cleanWs()
        }
        success {
            echo 'Test server configuration completed successfully!'
            
            // Send success notification (optional)
            script {
                if (env.BUILD_NOTIFICATIONS?.toLowerCase() == 'true') {
                    echo 'Sending success notification...'
                    // Add notification logic here
                }
            }
        }
        failure {
            echo 'Test server configuration failed!'
            
            // Send failure notification (optional)
            script {
                if (env.BUILD_NOTIFICATIONS?.toLowerCase() == 'true') {
                    echo 'Sending failure notification...'
                    // Add notification logic here
                }
            }
            
            // Attempt to gather diagnostic information
            script {
                try {
                    sh """
                        echo "=== Diagnostic Information ===" > diagnostic.txt
                        echo "Ansible Version:" >> diagnostic.txt
                        ansible --version >> diagnostic.txt
                        echo "" >> diagnostic.txt
                        echo "Inventory:" >> diagnostic.txt
                        cat ${ANSIBLE_INVENTORY} >> diagnostic.txt
                        echo "" >> diagnostic.txt
                        echo "Connectivity Test:" >> diagnostic.txt
                        ansible all -i ${ANSIBLE_INVENTORY} -m ping >> diagnostic.txt 2>&1
                    """
                    archiveArtifacts artifacts: 'diagnostic.txt', allowEmptyArchive: true
                } catch (Exception e) {
                    echo "Failed to gather diagnostic information: ${e.getMessage()}"
                }
            }
        }
        unstable {
            echo 'Test server configuration completed with warnings.'
        }
    }
}