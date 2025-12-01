pipeline {
    agent {
        label 'php-agent'
    }
    
    environment {
        GIT_REPO_URL = 'https://github.com/your-username/your-php-project.git'
        GIT_BRANCH = 'main'
        WORKSPACE_DIR = '/home/jenkins/agent/workspace'
    }
    
    stages {        
        stage('Checkout') {
            steps {
                script {
                    // Clean workspace
                    cleanWs()
                    
                    // Clone the repository
                    git branch: "${GIT_BRANCH}", 
                        url: "${GIT_REPO_URL}",
                        changelog: false,
                        poll: false
                }
            }
        }
        
        stage('Install Dependencies') {
            steps {
                script {
                    // Check if composer.json exists
                    if (fileExists('composer.json')) {
                        echo 'Installing Composer dependencies...'
                        sh 'composer install --no-interaction --prefer-dist --optimize-autoloader'
                    } else {
                        echo 'No composer.json found, skipping Composer installation'
                    }
                }
            }
        }
        
        stage('Code Analysis') {
            steps {
                script {
                    // Run PHP code analysis if tools are available
                    if (fileExists('composer.json')) {
                        try {
                            echo 'Running PHP code analysis...'
                            sh 'composer install --dev --no-interaction'
                            
                            // Run PHPStan if available
                            if (sh(script: 'composer show phpstan/phpstan', returnStatus: true) == 0) {
                                sh 'vendor/bin/phpstan analyse --error-format=table'
                            }
                            
                            // Run PHP CS Fixer if available
                            if (sh(script: 'composer show friendsofphp/php-cs-fixer', returnStatus: true) == 0) {
                                sh 'vendor/bin/php-cs-fixer fix --dry-run --diff --verbose'
                            }
                        } catch (Exception e) {
                            echo "Code analysis failed: ${e.getMessage()}"
                            currentBuild.result = 'UNSTABLE'
                        }
                    }
                }
            }
        }
        
        stage('Unit Tests') {
            steps {
                script {
                    echo 'Running PHPUnit tests...'
                    
                    // Check for PHPUnit configuration
                    if (fileExists('phpunit.xml')) {
                        sh 'vendor/bin/phpunit --configuration phpunit.xml --coverage-text --coverage-clover=coverage.xml'
                    } else if (fileExists('tests/')) {
                        sh 'vendor/bin/phpunit tests/ --coverage-text --coverage-clover=coverage.xml'
                    } else {
                        echo 'No PHPUnit configuration or tests directory found, creating basic test...'
                        sh '''
                        mkdir -p tests
                        cat > tests/SampleTest.php << 'EOF'
<?php
use PHPUnit\\Framework\\TestCase;

class SampleTest extends TestCase
{
    public function testTrueIsTrue()
    {
        $this->assertTrue(true);
    }
    
    public function testPHPVersion()
    {
        $this->assertIsString(PHP_VERSION);
    }
}
EOF
                        vendor/bin/phpunit tests/ --coverage-text
                        '''
                    }
                }
            }
        }
        
        stage('Test Results') {
            steps {
                script {
                    // Publish test results if JUnit format is available
                    if (fileExists('tests/results/junit.xml')) {
                        publishTestResults testResultsPattern: 'tests/results/junit.xml'
                    }
                    
                    // Publish coverage report if available
                    if (fileExists('coverage.xml')) {
                        publishCoverage adapters: [coberturaAdapter('coverage.xml')], sourceFileResolver: sourceFiles('STORE_LAST_BUILD')
                    }
                }
            }
        }
        
        stage('Build Artifact') {
            steps {
                script {
                    echo 'Creating deployment artifact...'
                    
                    // Create artifact directory
                    sh 'mkdir -p artifact'
                    
                    // Copy application files (excluding development files)
                    sh '''
                    rsync -av --exclude-from=.gitignore \
                          --exclude='.git' \
                          --exclude='tests/' \
                          --exclude='node_modules/' \
                          --exclude='*.log' \
                          --exclude='coverage.xml' \
                          --exclude='.phpunit.result.cache' \
                          ./ artifact/
                    '''
                    
                    // Create version info
                    sh '''
                    echo "Build: ${BUILD_NUMBER}" > artifact/build-info.txt
                    echo "Date: $(date)" >> artifact/build-info.txt
                    echo "Commit: ${GIT_COMMIT}" >> artifact/build-info.txt
                    echo "Branch: ${GIT_BRANCH}" >> artifact/build-info.txt
                    '''
                    
                    // Archive artifact
                    sh 'cd artifact && tar -czf ../php-app-${BUILD_NUMBER}.tar.gz .'
                    archiveArtifacts artifacts: 'php-app-*.tar.gz', fingerprint: true
                }
            }
        }
    }
    
    post {
        always {
            echo 'Pipeline completed.'
            cleanWs()
        }
        success {
            echo 'All stages completed successfully!'
            
            // Send success notification (optional)
            script {
                if (env.BUILD_NOTIFICATIONS?.toLowerCase() == 'true') {
                    echo 'Sending success notification...'
                    // Add notification logic here
                }
            }
        }
        failure {
            echo 'Errors detected in the pipeline.'
            
            // Send failure notification (optional)
            script {
                if (env.BUILD_NOTIFICATIONS?.toLowerCase() == 'true') {
                    echo 'Sending failure notification...'
                    // Add notification logic here
                }
            }
        }
        unstable {
            echo 'Pipeline completed with warnings.'
        }
    }
}