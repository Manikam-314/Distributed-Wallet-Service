import dateparser.search
from datetime import datetime

msg = "i pay it to you at after 15 second later"
# Clean it like the app does
msg_clean = "i pay it to you at after 15 second later" 

dates = dateparser.search.search_dates(msg_clean, settings={'TIMEZONE': 'UTC', 'PREFER_DATES_FROM': 'future'})
print(f"DATES: {dates}")
if dates:
    print(f"ISO: {dates[0][1].isoformat()}")
