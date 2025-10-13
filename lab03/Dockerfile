# Use official Python image as base
FROM python:3.11-slim

# Set working directory
WORKDIR /app

# Install cron and other necessary packages
RUN apt-get update && \
    apt-get install -y cron && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

# Copy requirements file and install Python dependencies
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# Copy the Python script
COPY currency_exchange_rate.py .

# Copy the cronjob file
COPY cronjob /etc/cron.d/currency-cronjob

# Set proper permissions for the cronjob file
RUN chmod 0644 /etc/cron.d/currency-cronjob

# Apply the cron job
RUN crontab /etc/cron.d/currency-cronjob

# Copy and set permissions for entrypoint script
COPY entrypoint.sh /entrypoint.sh
RUN chmod +x /entrypoint.sh

# Create data directory
RUN mkdir -p /app/data

# Set entrypoint
ENTRYPOINT ["/entrypoint.sh"]

