#!/usr/bin/env python3
"""Inspect public signatures from the exact pinned AAR resolved by Gradle."""
from pathlib import Path
import subprocess
import tempfile
import zipfile

cache = Path.home() / '.gradle/caches/modules-2/files-2.1/org.mozilla.geckoview/geckoview'
aars = list(cache.rglob('*.aar'))
assert aars, 'Resolved GeckoView AAR missing'
classes = (
    'GeckoSession', 'GeckoSession$Loader', 'GeckoSession$PermissionDelegate',
    'GeckoSession$PromptDelegate', 'GeckoView', 'GeckoRuntimeSettings',
    'ContentBlocking$Settings', 'ContentBlocking$CookieBannerMode',
    'Autocomplete$StorageDelegate', 'Autocomplete$LoginEntry', 'Autocomplete$LoginEntry$Builder',
    'Autocomplete$LoginSaveOption', 'Autocomplete$LoginSelectOption',
    'WebExtensionController$PromptDelegate', 'WebExtension$PermissionPromptResponse',
    'WebExtension$MetaData', 'TranslationsController$SessionTranslation',
    'TranslationsController$RuntimeTranslation', 'StorageController$ClearFlags',
)
out = Path('gecko-api-report');out.mkdir(exist_ok=True)
with tempfile.TemporaryDirectory() as temp:
    jar = Path(temp) / 'classes.jar'
    with zipfile.ZipFile(aars[0]) as archive:
        jar.write_bytes(archive.read('classes.jar'))
    for name in classes:
        result = subprocess.run(['javap', '-public', '-classpath', str(jar),
            'org.mozilla.geckoview.' + name], capture_output=True, text=True)
        (out / (name + '.txt')).write_text(result.stdout + result.stderr)
print(f'Inspected pinned GeckoView: {aars[0].parent.parent.name}')
