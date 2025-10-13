# Lab 03 - Automated Currency Exchange Rate Monitoring with Cron

This laboratory work demonstrates how to configure a task scheduler (cron) for automating script execution in a Docker container environment.

## Objective

Learn how to configure the task scheduler (cron) for automating script execution.

## Project Overview

This project builds upon Laboratory Work No. 2 by adding automated scheduled tasks that periodically fetch currency exchange rates. The automation is achieved using cron jobs running inside a Docker container.

## Scheduled Tasks

The following cron jobs are configured to run automatically:

1. **Daily at 6:00 AM**: Fetch MDL to EUR exchange rate for the previous day
2. **Weekly on Friday at 5:00 PM**: Fetch MDL to USD exchange rate for the previous week

## Project Structure

```
lab03/
├── currency_exchange_rate.py    # Main Python script for fetching exchange rates
├── requirements.txt              # Python dependencies
├── cronjob                       # Cron job definitions
├── entrypoint.sh                 # Docker entrypoint script for cron setup
├── Dockerfile                    # Docker image configuration
├── docker-compose.yml            # Docker Compose configuration
├── README.md                     # This documentation file
└── data/                         # Directory for storing JSON output files
```

### File Descriptions

#### `currency_exchange_rate.py`

The main Python script that interacts with the currency exchange rate API to fetch rates between different currencies on specific dates. It includes:

- Comprehensive error handling
- Logging functionality
- Data validation
- JSON file output

For detailed script documentation, refer to the original lab02 README.

#### `requirements.txt`

Contains the Python dependencies required by the script:

- `requests==2.31.0` - HTTP library for making API requests

#### `cronjob`

Defines the cron tasks to be executed. The file contains two scheduled jobs:

- Daily task at 6:00 AM for MDL to EUR conversion
- Weekly task on Friday at 5:00 PM for MDL to USD conversion

All output is redirected to `/var/log/cron.log` for monitoring.

#### `entrypoint.sh`

Shell script that serves as the Docker container's entrypoint. It performs the following functions:

1. Saves environment variables to `/etc/environment` for cron access
2. Creates and sets permissions for the log file
3. Starts background log monitoring
4. Launches the cron daemon in foreground mode

#### `Dockerfile`

Defines the Docker image based on Python 3.11-slim that:

- Installs cron and necessary system packages
- Installs Python dependencies
- Copies the script, cronjob file, and entrypoint script
- Configures cron with proper permissions
- Sets up the working directory and data folder

#### `docker-compose.yml`

Orchestrates the Docker container deployment with:

- Automatic image building from the Dockerfile
- Volume mounting for data persistence
- Timezone configuration
- Host network access for API connectivity
- Automatic restart policy

## Prerequisites

Before running this project, ensure you have:

1. **Docker** and **Docker Compose** installed on your system
2. The **currency exchange rate API service** running at `http://localhost:8080`
3. Basic understanding of cron syntax and Docker containers

## Installation and Setup

### 1. Start the API Service

The cron jobs require the currency exchange rate API service to be running. Navigate to the API service directory (from lab02prep) and start it:

```bash
cd /path/to/lab02prep
cp sample.env .env
docker-compose up -d
```

Verify the service is running:

```bash
docker-compose ps
```

You should see the `php_apache` container running on port 8080.

### 2. Build and Run the Container

Navigate to the lab03 directory:

```bash
cd lab03
```

Build and start the container using Docker Compose:

```bash
docker-compose up -d
```

This will:

- Build the Docker image from the Dockerfile
- Create and start the container in detached mode
- Set up volume mounts for data persistence
- Configure cron to run the scheduled tasks

### 3. Verify Container is Running

Check that the container is running:

```bash
docker-compose ps
```

You should see the `currency-cron-scheduler` container with status "Up".

## Verification and Monitoring

### View Cron Logs

To verify that cron tasks are executing properly, view the cron log file:

```bash
docker-compose logs -f
```

This will show the real-time output of the cron daemon and scheduled tasks.

Alternatively, you can access the log file directly inside the container:

```bash
docker exec -it currency-cron-scheduler tail -f /var/log/cron.log
```

### Check Cron Job Status

To see the configured cron jobs inside the container:

```bash
docker exec -it currency-cron-scheduler crontab -l
```

### Verify Data Files

The fetched exchange rate data is saved to JSON files in the `data/` directory. Check the files:

```bash
ls -la data/
```

Each successful execution creates a JSON file with the format:

```
exchange_rate_{FROM}_{TO}_{DATE}.json
```

### Manual Testing

To manually test the script inside the container:

```bash
docker exec -it currency-cron-scheduler python currency_exchange_rate.py MDL EUR 2025-01-15
```

### Inspect Container

To enter the container and explore its environment:

```bash
docker exec -it currency-cron-scheduler /bin/sh
```

Inside the container, you can:

- View cron configuration: `cat /etc/cron.d/currency-cronjob`
- Check running processes: `ps aux`
- View environment variables: `cat /etc/environment`
- Check logs: `cat /var/log/cron.log`

## Cron Schedule Format

The cron jobs use the standard cron syntax:

```
* * * * * command
│ │ │ │ │
│ │ │ │ └─── Day of week (0-7, where 0 and 7 are Sunday)
│ │ │ └───── Month (1-12)
│ │ └─────── Day of month (1-31)
│ └───────── Hour (0-23)
└─────────── Minute (0-59)
```

### Configured Jobs

1. **Daily at 6:00 AM**:

   ```
   0 6 * * *
   ```

   Fetches MDL to EUR exchange rate for the previous day.

2. **Weekly on Friday at 5:00 PM**:
   ```
   0 17 * * 5
   ```
   Fetches MDL to USD exchange rate for the previous week (7 days ago).

## Troubleshooting

### Container Won't Start

If the container fails to start, check the logs:

```bash
docker-compose logs
```

Common issues:

- Port conflicts (if API service is not running)
- Permission issues with cronjob file
- Syntax errors in entrypoint.sh

### Cron Jobs Not Executing

1. **Check cron daemon is running**:

   ```bash
   docker exec -it currency-cron-scheduler ps aux | grep cron
   ```

2. **Verify crontab is loaded**:

   ```bash
   docker exec -it currency-cron-scheduler crontab -l
   ```

3. **Check log file permissions**:

   ```bash
   docker exec -it currency-cron-scheduler ls -la /var/log/cron.log
   ```

4. **Inspect cron logs**:
   ```bash
   docker exec -it currency-cron-scheduler cat /var/log/cron.log
   ```

### API Connection Errors

If cron jobs fail to connect to the API:

1. Verify the API service is running:

   ```bash
   curl http://localhost:8080
   ```

2. Check the `extra_hosts` configuration in `docker-compose.yml` allows access to `host.docker.internal`

3. Test API connectivity from inside the container:
   ```bash
   docker exec -it currency-cron-scheduler curl http://host.docker.internal:8080
   ```

### Date Calculation Issues

The cron jobs use `date` command to calculate previous day/week:

- `date -d "yesterday" +%Y-%m-%d` - Gets yesterday's date
- `date -d "7 days ago" +%Y-%m-%d` - Gets date from 7 days ago

If you encounter issues, you can manually verify these commands:

```bash
docker exec -it currency-cron-scheduler date -d "yesterday" +%Y-%m-%d
```

## Stopping and Cleaning Up

### Stop the Container

To stop the running container:

```bash
docker-compose down
```

### Remove Container and Volumes

To completely remove the container and volumes:

```bash
docker-compose down -v
```

### Keep Data Files

To preserve the data files in the `data/` directory, do not use the `-v` flag:

```bash
docker-compose down
```

The `data/` directory is mounted as a volume, so files will persist even after the container is removed.

## Testing the Setup

### Test 1: Immediate Execution

To test the cron setup without waiting for scheduled times, you can temporarily modify the cronjob file to run every minute:

1. Edit the cronjob file:

   ```bash
   # Test cron - runs every minute
   * * * * * cd /app && /usr/local/bin/python currency_exchange_rate.py MDL EUR $(date -d "yesterday" +\%Y-\%m-\%d) --url http://host.docker.internal:8080 >> /var/log/cron.log 2>&1
   ```

2. Rebuild and restart:

   ```bash
   docker-compose down
   docker-compose up -d --build
   ```

3. Watch the logs:

   ```bash
   docker-compose logs -f
   ```

4. After verification, restore the original schedule.

### Test 2: Manual Script Execution

Execute the script manually to ensure it works correctly:

```bash
docker exec -it currency-cron-scheduler python currency_exchange_rate.py MDL EUR 2025-01-15 --url http://host.docker.internal:8080
```

Check if the JSON file was created:

```bash
ls -la data/
```

## Additional Notes

- The container uses `Europe/Chisinau` timezone by default. Modify the `TZ` environment variable in `docker-compose.yml` if needed.
- Cron output (stdout and stderr) is redirected to `/var/log/cron.log` for easier debugging.
- The `restart: unless-stopped` policy ensures the container automatically restarts after system reboot.
- The data directory is mounted as a volume to persist exchange rate data between container restarts.

## Dependencies

- **Docker** (version 20.10 or higher recommended)
- **Docker Compose** (version 2.0 or higher recommended)
- **Currency Exchange Rate API Service** (from lab02prep)

## License

This project is created for educational purposes as part of the "Automatizare si scripting" course.

## Author

Created for Lab 03 (Laboratory Work 03) assignment.
