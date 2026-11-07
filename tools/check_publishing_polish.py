"""Check shipped legal texts and share-file permissions; UI behavior is tested on Android."""
import hashlib
import json
from pathlib import Path
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / 'android/app-telefon/src/main/assets'
entries = json.loads((ASSETS / 'licenses/index.json').read_text())
assert len(entries) == 6 and len({row['id'] for row in entries}) == 6
expected = {
    'gecko': ('Mozilla Public License', '2.0'),
    'androidx': ('Apache License', 'END OF TERMS AND CONDITIONS'),
    'kotlin': ('Apache License', 'END OF TERMS AND CONDITIONS'),
    'wireguard': ('Apache License', 'Permission is hereby granted', 'Redistribution and use'),
    'zxing': ('Apache License', 'NOTICES FOR BARCODE4J', 'NOTICES FOR JCOMMANDER'),
    'desugar': ('GNU General Public License', '"CLASSPATH" EXCEPTION TO THE GPL', 'independent modules'),
}
for row in entries:
    assert row['id'] in expected and row['asset'] == f"licenses/{row['id']}.txt"
    data = (ASSETS / row['asset']).read_bytes()
    assert len(data) > 10_000 and hashlib.sha256(data).hexdigest() == row['sha256'], row['id']
    text = data.decode()
    assert all(fragment in text for fragment in expected[row['id']]), row['id']
assert 'Classpath' in next(row['license'] for row in entries if row['id'] == 'desugar')
gecko = (ASSETS / 'licenses/gecko.txt').read_text()
assert len(gecko) > 200_000 and 'LICENSE_FOXIT' in gecko and 'LICENSE_LIBERATION' in gecko
original = (ASSETS / 'licenses/gecko-original.html').read_text()
assert '<html' in original and 'Mozilla Public License 2.0' in original
paths = ET.parse(ROOT / 'android/app-telefon/src/main/res/xml/browser_update_file_paths.xml').getroot()
share = [node for node in paths if node.attrib.get('name') == 'shares']
assert len(share) == 1 and share[0].tag == 'cache-path' and share[0].attrib['path'] == 'shares/'
for language in ('tr', 'en'):
    strings = json.loads((ROOT / f'shared/i18n/{language}.json').read_text())
    assert all('github' not in value.lower() for value in strings.values())
    assert strings['feedback_body'].count('%1$s') == 1
print('Offline license integrity, attribution and scoped QR sharing paths passed.')
