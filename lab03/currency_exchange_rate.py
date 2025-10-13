#!/usr/bin/env python3
"""
Currency Exchange Rate API Client
This script interacts with a currency exchange rate API service to fetch
exchange rates between currencies on specific dates.
"""

import argparse
import requests
import json
import os
import sys
from datetime import datetime
import logging


class CurrencyExchangeClient:
    """Client for interacting with the currency exchange rate API."""
    
    def __init__(self, base_url="http://localhost:8080", api_key="EXAMPLE_API_KEY"):
        """
        Initialize the API client.
        
        Args:
            base_url: The base URL of the API service
            api_key: The API key for authentication
        """
        self.base_url = base_url
        self.api_key = api_key
        self.setup_logging()
        self.setup_data_directory()
    
    def setup_logging(self):
        """Setup logging configuration to log errors to error.log file."""
        logging.basicConfig(
            level=logging.INFO,
            format='%(asctime)s - %(levelname)s - %(message)s',
            handlers=[
                logging.FileHandler('error.log'),
                logging.StreamHandler(sys.stdout)
            ]
        )
        self.logger = logging.getLogger(__name__)
    
    def setup_data_directory(self):
        """Create the data directory if it doesn't exist."""
        if not os.path.exists('data'):
            os.makedirs('data')
            self.logger.info("Created 'data' directory")
    
    def validate_date(self, date_str):
        """
        Validate date format and range.
        
        Args:
            date_str: Date string in YYYY-MM-DD format
            
        Returns:
            bool: True if valid, False otherwise
        """
        try:
            date_obj = datetime.strptime(date_str, '%Y-%m-%d')
            min_date = datetime(2025, 1, 1)
            max_date = datetime(2025, 9, 15)
            
            if not (min_date <= date_obj <= max_date):
                self.logger.error(f"Date {date_str} is outside the valid range (2025-01-01 to 2025-09-15)")
                return False
            return True
        except ValueError:
            self.logger.error(f"Invalid date format: {date_str}. Expected format: YYYY-MM-DD")
            return False
    
    def get_exchange_rate(self, from_currency, to_currency, date):
        """
        Get the exchange rate between two currencies on a specific date.
        
        Args:
            from_currency: Source currency code (e.g., 'USD')
            to_currency: Target currency code (e.g., 'EUR')
            date: Date in YYYY-MM-DD format
            
        Returns:
            dict: Response data from the API or None if error occurs
        """
        # Validate date
        if not self.validate_date(date):
            return None
        
        # Prepare request parameters
        url = f"{self.base_url}/"
        params = {
            'from': from_currency.upper(),
            'to': to_currency.upper(),
            'date': date
        }
        data = {
            'key': self.api_key
        }
        
        try:
            self.logger.info(f"Requesting exchange rate: {from_currency} -> {to_currency} on {date}")
            response = requests.post(url, params=params, data=data, timeout=10)
            response.raise_for_status()
            
            result = response.json()
            
            # Check if API returned an error
            if result.get('error'):
                self.logger.error(f"API Error: {result['error']}")
                return None
            
            self.logger.info(f"Successfully retrieved exchange rate")
            return result
            
        except requests.exceptions.ConnectionError:
            self.logger.error("Connection Error: Unable to connect to the API service. Make sure the service is running.")
            return None
        except requests.exceptions.Timeout:
            self.logger.error("Timeout Error: The request took too long to complete")
            return None
        except requests.exceptions.HTTPError as e:
            self.logger.error(f"HTTP Error: {e}")
            return None
        except requests.exceptions.RequestException as e:
            self.logger.error(f"Request Error: {e}")
            return None
        except json.JSONDecodeError:
            self.logger.error("Error: Invalid JSON response from the API")
            return None
        except Exception as e:
            self.logger.error(f"Unexpected Error: {e}")
            return None
    
    def save_to_file(self, data, from_currency, to_currency, date):
        """
        Save the exchange rate data to a JSON file.
        
        Args:
            data: The data to save
            from_currency: Source currency code
            to_currency: Target currency code
            date: Date of the exchange rate
            
        Returns:
            str: Path to the saved file or None if error occurs
        """
        try:
            # Create filename with currencies and date
            filename = f"exchange_rate_{from_currency}_{to_currency}_{date}.json"
            filepath = os.path.join('data', filename)
            
            # Save data to file
            with open(filepath, 'w', encoding='utf-8') as f:
                json.dump(data, f, indent=2, ensure_ascii=False)
            
            self.logger.info(f"Data saved successfully to: {filepath}")
            return filepath
            
        except IOError as e:
            self.logger.error(f"Error saving file: {e}")
            return None
        except Exception as e:
            self.logger.error(f"Unexpected error while saving file: {e}")
            return None
    
    def get_available_currencies(self):
        """
        Get the list of available currencies from the API.
        
        Returns:
            list: List of available currencies or None if error occurs
        """
        url = f"{self.base_url}/"
        params = {'currencies': ''}
        data = {'key': self.api_key}
        
        try:
            response = requests.post(url, params=params, data=data, timeout=10)
            response.raise_for_status()
            result = response.json()
            
            if result.get('error'):
                self.logger.error(f"API Error: {result['error']}")
                return None
            
            return result.get('data', [])
            
        except Exception as e:
            self.logger.error(f"Error fetching currencies: {e}")
            return None


def main():
    """Main function to handle command-line arguments and execute the script."""
    parser = argparse.ArgumentParser(
        description='Get currency exchange rates from the API service',
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
Examples:
  python currency_exchange_rate.py USD EUR 2025-01-15
  python currency_exchange_rate.py MDL USD 2025-05-20
  python currency_exchange_rate.py --url http://localhost:8080 --key YOUR_API_KEY USD EUR 2025-01-15
        """
    )
    
    parser.add_argument('from_currency', help='Source currency code (e.g., USD, EUR)')
    parser.add_argument('to_currency', help='Target currency code (e.g., USD, EUR)')
    parser.add_argument('date', help='Date in YYYY-MM-DD format (between 2025-01-01 and 2025-09-15)')
    parser.add_argument('--url', default='http://localhost:8080', help='API base URL (default: http://localhost:8080)')
    parser.add_argument('--key', default='EXAMPLE_API_KEY', help='API key for authentication (default: EXAMPLE_API_KEY)')
    parser.add_argument('--list-currencies', action='store_true', help='List available currencies and exit')
    
    args = parser.parse_args()
    
    # Create API client
    client = CurrencyExchangeClient(base_url=args.url, api_key=args.key)
    
    # Handle list currencies option
    if args.list_currencies:
        currencies = client.get_available_currencies()
        if currencies:
            print("Available currencies:")
            for currency in currencies:
                print(f"  - {currency}")
        sys.exit(0 if currencies else 1)
    
    # Get exchange rate
    result = client.get_exchange_rate(args.from_currency, args.to_currency, args.date)
    
    if result is None:
        print("Failed to retrieve exchange rate. Check error.log for details.")
        sys.exit(1)
    
    # Display result
    if 'data' in result:
        rate_data = result['data']
        print(f"\n{'='*60}")
        print(f"Exchange Rate Information")
        print(f"{'='*60}")
        print(f"From:     {rate_data['from']}")
        print(f"To:       {rate_data['to']}")
        print(f"Rate:     {rate_data['rate']:.6f}")
        print(f"Date:     {rate_data['date']}")
        print(f"{'='*60}\n")
    
    # Save to file
    filepath = client.save_to_file(result, args.from_currency.upper(), args.to_currency.upper(), args.date)
    
    if filepath:
        print(f"✓ Data saved to: {filepath}")
        sys.exit(0)
    else:
        print("✗ Failed to save data to file. Check error.log for details.")
        sys.exit(1)


if __name__ == '__main__':
    main()
