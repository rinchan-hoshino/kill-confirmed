#!/usr/bin/env python3
"""Build the supported releases with isolated Gradle state and verified manifests."""
import argparse, hashlib, json, os, shutil, subprocess
from pathlib import Path
ROOT = Path(__file__).resolve().parents[1]

def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--minecraft')
    parser.add_argument('--loader', choices=['fabric','neoforge'])
    parser.add_argument('--rinlib-dir', type=Path, help='Optional matching RinLib matrix output for pre-release builds')
    parser.add_argument('--output', type=Path, default=ROOT/'build/matrix')
    args = parser.parse_args()
    targets = json.loads((ROOT/'profiles.json').read_text())
    properties = dict(line.split('=',1) for line in (ROOT/'gradle.properties').read_text().splitlines() if '=' in line)
    selected = [(mc, loader) for mc in targets for loader in ['fabric','neoforge']
                if (args.minecraft is None or mc==args.minecraft) and (args.loader is None or loader==args.loader)]
    if not selected: parser.error('No matching supported target')
    output=args.output.resolve();output.mkdir(parents=True,exist_ok=True)
    results=[]
    for mc,loader in selected:
        key=f'{mc}-{loader}'
        command=[str(ROOT/'gradlew'),'-p',str(ROOT/'profiles'),'build',f'-Pprofile={mc}',f'-Ploader={loader}',
                 '--project-cache-dir',str(ROOT/'build/project-cache'/key),'--no-configuration-cache',
                 '--max-workers=2','--console=plain','-Dorg.gradle.internal.http.socketTimeout=15000',
                 '-Dorg.gradle.internal.http.connectionTimeout=15000']
        if args.rinlib_dir:
            dependency=args.rinlib_dir.resolve()/key/f'rinlib-{loader}-{properties["rinlib_version"]}+{mc}.jar'
            if not dependency.is_file(): raise FileNotFoundError(dependency)
            command.append('-PrinlibJar='+str(dependency))
        env=os.environ.copy()
        java=targets[mc]['java']
        if env.get(f'JAVA_{java}_HOME'):env['JAVA_HOME']=env[f'JAVA_{java}_HOME']
        with (output/f'{key}.log').open('w') as log:
            subprocess.run(command,cwd=ROOT,env=env,stdout=log,stderr=subprocess.STDOUT,check=True,timeout=600)
        name=f'kill_confirmed-{loader}-{properties["mod_version"]}+{mc}.jar'
        source=ROOT/'build/profiles'/key/'libs'/name
        target=output/name;shutil.copy2(source,target)
        binary=target.read_bytes()
        results.append(dict(minecraft=mc,loader=loader,file=str(target),sha256=hashlib.sha256(binary).hexdigest(),sha512=hashlib.sha512(binary).hexdigest()))
        (output/'manifest.json').write_text(json.dumps(results,indent=2)+'\n')
        print(key+': built',flush=True)
if __name__=='__main__':main()
