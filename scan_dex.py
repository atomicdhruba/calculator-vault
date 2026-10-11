import glob
import re
from loguru import logger
logger.remove()
from androguard.core.dex import DEX

url_pattern = re.compile(r'https?://[^\s"\'<>]+')
dex_files = sorted(glob.glob('extracted_apk/*.dex'))

found_urls = set()
for df in dex_files:
    with open(df, 'rb') as f:
        d = DEX(f.read())
        for s in d.get_strings():
            for u in url_pattern.findall(s):
                found_urls.add((df, u))

print("=== Found URLs ===")
for df, u in sorted(found_urls):
    if not any(ign in u for ign in ['schemas.android.com', 'w3.org', 'android.com/tools', 'apache.org', 'kotlinlang.org', 'googletagmanager', 'google.com/policies']):
        print(f"{df}: {u}")
