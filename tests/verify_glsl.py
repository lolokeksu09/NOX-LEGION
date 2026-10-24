"""Compile the actual Kotlin-embedded NOX GLSL shaders with glslangValidator.

Android's Gradle compilation does not compile OpenGL shader source.
This check catches GLES 100 syntax/semantic issues before delivering debug APK.
It is NOT a substitute for compiling on the target phone's GPU driver.
"""
from pathlib import Path
from tempfile import TemporaryDirectory
import re
import subprocess
import sys

ROOT=Path(__file__).resolve().parents[1]
SOURCE=(ROOT/"core/renderer/src/main/java/studio/nox/renderer/GlBackend.kt").read_text()

def extract(name):
    match=re.search(r'private const val '+name+r' = """\s*(.*?)\s*"""',SOURCE,re.S)
    if not match:
        raise RuntimeError(f"Missing shader source {name}")
    return "#version 100\n"+match.group(1).strip()+"\n"

def main():
    with TemporaryDirectory() as directory:
        for name,ext,stage in (("VERTEX","vert","vert"),("FRAGMENT","frag","frag")):
            path=Path(directory)/f"nox.{ext}"
            path.write_text(extract(name),encoding="utf-8")
            result=subprocess.run(["glslangValidator","-S",stage,str(path)],
                capture_output=True,text=True,check=False)
            if result.returncode!=0:
                print(result.stdout+result.stderr,file=sys.stderr)
                raise RuntimeError(f"{name} GLSL validation failed")
            print(f"{name} GLES 100 GLSL syntax: PASS")

if __name__=="__main__":
    main()
