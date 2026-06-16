import dateparser
import dateparser.search
from datetime import datetime

msg = "i pay it to you at tomorrow"
dates = dateparser.search.search_dates(msg, settings={'TIMEZONE': 'UTC', 'PREFER_DATES_FROM': 'future'})
print(f"SEARCH DATES: {dates}")

date = dateparser.parse(msg, settings={'PREFER_DATES_FROM': 'future'})
print(f"PARSE: {date}")

# Try without 'at'
msg2 = "tomorrow"
dates2 = dateparser.search.search_dates(msg2, settings={'TIMEZONE': 'UTC', 'PREFER_DATES_FROM': 'future'})
print(f"SEARCH DATES (clean): {dates2}")
