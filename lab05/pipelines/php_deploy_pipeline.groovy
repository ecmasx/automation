pipeline {
    agent {
        label 'ansible-agent'
    }
    
    environment {
        ANSIBLE_INVENTORY = '/ansible/hosts.ini'
        DEPLOY_PATH = '/var/www/html/php-app'
        BACKUP_PATH = '/var/www/html/backups'
        WORKSPACE_DIR = '/home/ansible'
    }
    
    stages {
        stage('Checkout PHP Application') {
            steps {
                script {
                    // Clean workspace
                    cleanWs()
                    
                    // Clone PHP application repository
                    git branch: 'main', 
                        url: 'https://github.com/your-username/your-php-project.git',
                        changelog: false,
                        poll: false
                    
                    echo 'PHP application source code checked out successfully'
                }
            }
        }
        
        stage('Prepare Deployment') {
            steps {
                script {
                    echo 'Preparing deployment package...'
                    
                    // Create deployment directory structure
                    sh '''
                        mkdir -p deploy
                        rsync -av --exclude='.git' \
                              --exclude='tests/' \
                              --exclude='node_modules/' \
                              --exclude='*.log' \
                              --exclude='coverage.xml' \
                              --exclude='.phpunit.result.cache' \
                              --exclude='deploy/' \
                              ./ deploy/
                    '''
                    
                    // Create deployment info file
                    sh """
                        echo "Deployment Information" > deploy/deployment-info.txt
                        echo "Build: ${BUILD_NUMBER}" >> deploy/deployment-info.txt
                        echo "Date: $(date)" >> deploy/deployment-info.txt
                        echo "Commit: ${GIT_COMMIT}" >> deploy/deployment-info.txt
                        echo "Branch: ${GIT_BRANCH}" >> deploy/deployment-info.txt
                        echo "Deployed by: Jenkins" >> deploy/deployment-info.txt
                    """
                    
                    // Verify deployment package
                    sh 'ls -la deploy/'
                }
            }
        }
        
        stage('Create Backup') {
            steps {
                script {
                    echo 'Creating backup of current deployment...'
                    
                    // Create backup directory on test server
                    sh """
                        ansible test-servers -i ${ANSIBLE_INVENTORY} -m file -a "path=${BACKUP_PATH} state=directory mode=0755"
                    """
                    
                    // Backup current application if it exists
                    sh """
                        ansible test-servers -i ${ANSIBLE_INVENTORY} -m shell -a "
                            if [ -d '${DEPLOY_PATH}' ] && [ \"\$(ls -A ${DEPLOY_PATH})\" ]; then
                                BACKUP_DIR=\"${BACKUP_PATH}/backup-\$(date +%Y%m%d-%H%M%S)\"
                                mkdir -p \$BACKUP_DIR
                                cp -r ${DEPLOY_PATH}/* \$BACKUP_DIR/
                                echo \"Backup created at \$BACKUP_DIR\"
                            else
                                echo \"No existing application to backup\"
                            fi
                        "
                    """
                }
            }
        }
        
        stage('Deploy Application') {
            steps {
                script {
                    echo 'Deploying PHP application to test server...'
                    
                    // Deploy application files using Ansible copy module
                    sh """
                        ansible test-servers -i ${ANSIBLE_INVENTORY} -m copy -a "
                            src=deploy/ 
                            dest=${DEPLOY_PATH}/
                            mode=0755
                            owner=ansible
                            group=ansible
                        "
                    """
                    
                    // Set proper permissions
                    sh """
                        ansible test-servers -i ${ANSIBLE_INVENTORY} -m file -a "
                            path=${DEPLOY_PATH}
                            state=directory
                            mode=0755
                            owner=ansible
                            group=ansible
                            recurse=yes
                        "
                    """
                }
            }
        }
        
        stage('Configure Application') {
            steps {
                script {
                    echo 'Configuring PHP application...'
                    
                    // Create/update configuration file if needed
                    sh """
                        ansible test-servers -i ${ANSIBLE_INVENTORY} -m template -a "
                            src=config.php.j2
                            dest=${DEPLOY_PATH}/config.php
                            mode=0644
                            owner=ansible
                            group=ansible
                        " || echo 'No configuration template found, skipping...'
                    """
                    
                    // Set up log directory
                    sh """
                        ansible test-servers -i ${ANSIBLE_INVENTORY} -m file -a "
                            path=${DEPLOY_PATH}/logs
                            state=directory
                            mode=0755
                            owner=ansible
                            group=ansible
                        "
                    """
                    
                    // Create .htaccess file for security
                    sh """
                        ansible test-servers -i ${ANSIBLE_INVENTORY} -m copy -a "
                            content='
# Security headers
<IfModule mod_headers.c>
    Header always set X-Content-Type-Options nosniff
    Header always set X-Frame-Options DENY
    Header always set X-XSS-Protection \"1; mode=block\"
    Header always set Referrer-Policy \"strict-origin-when-cross-origin\"
</IfModule>

# Hide .htaccess file
<Files .htaccess>
    Order allow,deny
    Deny from all
</Files>

# Disable directory listing
Options -Indexes

# Custom error pages
ErrorDocument 404 /error404.php
ErrorDocument 500 /error500.php
                            '
                            dest=${DEPLOY_PATH}/.htaccess
                            mode=0644
                            owner=ansible
                            group=ansible
                        "
                    """
                }
            }
        }
        
        stage('Verify Deployment') {
            steps {
                script {
                    echo 'Verifying deployment...'
                    
                    // Check if files are deployed correctly
                    sh """
                        ansible test-servers -i ${ANSIBLE_INVENTORY} -m shell -a "
                            ls -la ${DEPLOY_PATH}/
                            echo 'Deployment info:'
                            cat ${DEPLOY_PATH}/deployment-info.txt
                        "
                    """
                    
                    // Test web server response
                    sh """
                        ansible test-servers -i ${ANSIBLE_INVENTORY} -m uri -a "
                            url=http://localhost/
                            method=GET
                            status_code=200
                        "
                    """
                    
                    // Test PHP functionality
                    sh """
                        ansible test-servers -i ${ANSIBLE_INVENTORY} -m uri -a "
                            url=http://localhost/index.php
                            method=GET
                            return_content=yes
                        " | grep -i 'php application'
                    """
                }
            }
        }
        
        stage('Health Check') {
            steps {
                script {
                    echo 'Performing health checks...'
                    
                    // Check Apache2 status
                    sh """
                        ansible test-servers -i ${ANSIBLE_INVENTORY} -m systemd -a "
                            name=apache2
                        " | grep -i 'active: active'
                    """
                    
                    // Check PHP error log
                    sh """
                        ansible test-servers -i ${ANSIBLE_INVENTORY} -m shell -a "
                            if [ -f /var/log/apache2/error.log ]; then
                                echo 'Recent Apache errors:'
                                tail -10 /var/log/apache2/error.log
                            else
                                echo 'No Apache error log found'
                            fi
                        "
                    """
                    
                    // Test database connection if applicable
                    sh """
                        ansible test-servers -i ${ANSIBLE_INVENTORY} -m shell -a "
                            if [ -f ${DEPLOY_PATH}/config.php ]; then
                                echo 'Configuration file exists'
                                # Add database connection test here if needed
                            else
                                echo 'No configuration file found'
                            fi
                        "
                    """
                }
            }
        }
    }
    
    post {
        always {
            echo 'Deployment pipeline completed.'
            
            // Generate deployment report
            script {
                try {
                    sh """
                        echo "=== Deployment Report ===" > deployment-report.txt
                        echo "Build: ${BUILD_NUMBER}" >> deployment-report.txt
                        echo "Date: $(date)" >> deployment-report.txt
                        echo "Commit: ${GIT_COMMIT}" >> deployment-report.txt
                        echo "Branch: ${GIT_BRANCH}" >> deployment-report.txt
                        echo "" >> deployment-report.txt
                        echo "=== Deployment Status ===" >> deployment-report.txt
                        ansible test-servers -i ${ANSIBLE_INVENTORY} -m shell -a "
                            echo 'Application directory:'
                            ls -la ${DEPLOY_PATH}/ | head -10
                            echo ''
                            echo 'Web server status:'
                            systemctl is-active apache2
                            echo ''
                            echo 'Recent deployments:'
                            ls -la ${BACKUP_PATH}/ | tail -5
                        " >> deployment-report.txt
                    """
                    archiveArtifacts artifacts: 'deployment-report.txt', allowEmptyArchive: true
                } catch (Exception e) {
                    echo "Failed to generate deployment report: ${e.getMessage()}"
                }
            }
            
            cleanWs()
        }
        success {
            echo 'PHP application deployed successfully!'
            
            // Send success notification (optional)
            script {
                if (env.BUILD_NOTIFICATIONS?.toLowerCase() == 'true') {
                    echo 'Sending deployment success notification...'
                    // Add notification logic here
                }
            }
            
            // Trigger smoke tests if available
            script {
                try {
                    build job: 'PHP-Smoke-Tests', wait: false
                } catch (Exception e) {
                    echo "Smoke tests job not found or failed to trigger: ${e.getMessage()}"
                }
            }
        }
        failure {
            echo 'PHP application deployment failed!'
            
            // Send failure notification (optional)
            script {
                if (env.BUILD_NOTIFICATIONS?.toLowerCase() == 'true') {
                    echo 'Sending deployment failure notification...'
                    // Add notification logic here
                }
            }
            
            // Attempt rollback if deployment failed
            script {
                try {
                    echo 'Attempting rollback to previous version...'
                    sh """
                        ansible test-servers -i ${ANSIBLE_INVENTORY} -m shell -a "
                            LATEST_BACKUP=\$(ls -t ${BACKUP_PATH}/ | head -1)
                            if [ -n \"\$LATEST_BACKUP\" ]; then
                                echo \"Rolling back to \$LATEST_BACKUP\"
                                rm -rf ${DEPLOY_PATH}/*
                                cp -r ${BACKUP_PATH}/\$LATEST_BACKUP/* ${DEPLOY_PATH}/
                                echo \"Rollback completed\"
                            else
                                echo \"No backup found for rollback\"
                            fi
                        "
                    """
                } catch (Exception e) {
                    echo "Rollback failed: ${e.getMessage()}"
                }
            }
        }
        unstable {
            echo 'Deployment completed with warnings.'
        }
    }
}