#!/usr/bin/env python3
"""Real fixture test for the Play-only manifest permission transformation."""
import importlib.util
import sys
import tempfile
from pathlib import Path
from unittest.mock import patch

ROOT=Path(__file__).resolve().parents[1]
path=ROOT/"tools/prepare_google_play_manifest.py"
spec=importlib.util.spec_from_file_location("play_manifest_prep_test_subject", path)
module=importlib.util.module_from_spec(spec)
spec.loader.exec_module(module)
original=module.MANIFEST.read_text(encoding="utf-8")
with tempfile.TemporaryDirectory() as tmp:
    fixture=Path(tmp)/"AndroidManifest.xml"
    fixture.write_text(original,encoding="utf-8")
    module.MANIFEST=fixture
    with patch.object(sys,"argv",[str(path),"--apply"]):
        module.main()
    changed=fixture.read_text(encoding="utf-8")
    assert changed!=original and "android.permission.REQUEST_INSTALL_PACKAGES" not in changed
    assert 'android.permission.INTERNET' in changed
    assert 'android.permission.FOREGROUND_SERVICE' in changed
    with patch.object(sys,"argv",[str(path),"--verify"]):
        module.main()
    with patch.object(sys,"argv",[str(path),"--apply"]):
        try: module.main()
        except SystemExit: pass
        else: raise AssertionError("Repeated or ambiguous permission rewrite must fail")
    assert module.MANIFEST.read_text(encoding="utf-8")==changed
assert (ROOT/"android/app-telefon/src/main/AndroidManifest.xml").read_text(encoding="utf-8")==original
print("Play manifest permission isolation tests passed without modifying production sources.")
