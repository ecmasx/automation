#!/bin/sh

create_log_file() {
    echo "Creating log file..."
    touch /var/log/cron.log
    chmod 666 /var/log/cron.log
    echo "Log file created at /var/log/cron.log"
}

monitor_logs() {
    echo "=== Monitoring cron logs ==="
    tail -f /var/log/cron.log
}

run_cron() {
    echo "=== Starting cron daemon ==="
    exec cron -f
}

# Save environment variables to /etc/environment for cron
env > /etc/environment

# Create the log file
create_log_file

# Monitor logs in background
monitor_logs &

# Run cron in foreground
run_cron

