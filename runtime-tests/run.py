#!/usr/bin/env python3
"""Execute a loader-integrated probe inside an isolated Minecraft server."""
import argparse,json,os,shutil,subprocess,signal
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
def run(mc, loader, strategy, output, rinlib, eula, packaged=None, client=False, server_dir=None):
    cfg=json.loads((ROOT/'profiles.json').read_text())[mc]
    instance=output/(f'{mc}-{loader}-client' if client else f'{mc}-{loader}-{strategy.lower()}')
    instance.mkdir(parents=True,exist_ok=True)
    shutil.copyfile(eula,instance/'eula.txt')
    (instance/'server.properties').write_text('server-ip=127.0.0.1\nserver-port=' + str(25960 + ['1.21.1','26.1.2','26.2'].index(mc)*2 + (loader=='neoforge')) + '\nonline-mode=false\nlevel-type=minecraft:flat\ngenerate-structures=false\nview-distance=2\nsimulation-distance=2\nmax-tick-time=0\nspawn-protection=0\n')
    config=instance/'config';config.mkdir(exist_ok=True)
    lore=json.loads((ROOT/'common/src/main/resources/data/kill_confirmed/lore/default.json').read_text())
    for key in ['probe:field','probe:java']:
        lore.append({'when':key,'component':{'placeholder':key}})
    (config/'kill_confirmed.json').write_text(json.dumps({'drop_strategy':strategy,'lore_template':lore}))
    pack=instance/'world/datapacks/probe'
    (pack/'data/probe/function').mkdir(parents=True,exist_ok=True)
    (pack/'pack.mcmeta').write_text(json.dumps({'pack':{'description':'Runtime probe','pack_format':48} if mc=='1.21.1' else {'description':'Runtime probe','min_format':[88,0],'max_format':[107,1]}}))
    (pack/'data/kill_confirmed/tags/function').mkdir(parents=True,exist_ok=True)
    (pack/'data/kill_confirmed/tags/function/placeholder_providers.json').write_text(json.dumps({'values':['probe:provider']}))
    (pack/'data/probe/function/provider.mcfunction').write_text('function probe:nested\nexecute if data storage kill_confirmed:probe_checks {invalid:1} run data modify storage kill_confirmed:scratch output append value {id:"probe:bad",component:"INVALID JSON"}\n')
    (pack/'data/probe/function/nested.mcfunction').write_text("data modify storage kill_confirmed:scratch output append value {id:\"probe:field\",component:'{\"text\":\"nested-function\"}'}\n")
    (pack/'data/probe/function/death.mcfunction').write_text('kill @s\ndata modify storage kill_confirmed:probe_checks outer_continued set value 1\n')
    marker=instance/('client.pass' if client else 'runtime.pass');marker.unlink(missing_ok=True)
    if client: (instance/'options.txt').write_text('onboardAccessibility:false\nlang:en_us\nmaxFps:30\nrenderDistance:2\nsimulationDistance:2\n')
    (instance/'packaged.expected').unlink(missing_ok=True)
    if packaged: (instance/'packaged.expected').write_text(str(packaged))
    command=[str(ROOT/'gradlew'),'-p',str(ROOT/'profiles'),('runClient' if loader=='fabric' else 'runProbeClient') if client else ('runServer' if loader=='fabric' else 'runProbeServer'),f'-Pprofile={mc}',f'-Ploader={loader}','--project-cache-dir',str(ROOT/'build/project-cache'/f'{mc}-{loader}'),f'-PrinlibJar={rinlib}', '-I',str(ROOT/'runtime-tests/probe.init.gradle'),f'-Dprobe.instance={instance}','--no-configuration-cache','--max-workers=2','--console=plain','-Dorg.gradle.internal.http.socketTimeout=15000','-Dorg.gradle.internal.http.connectionTimeout=15000']
    if packaged: command.append('-Dprobe.jar='+str(packaged))
    if packaged and not client: command.append('-Dprobe.bundled=true')
    env=os.environ.copy()
    env['JAVA_HOME']=env.get(f'JAVA_{cfg["java"]}_HOME') or env.get('JAVA_HOME') or str(next((Path.home()/'.gradle/jdks').glob(f'eclipse_adoptium-{cfg["java"]}-*/jdk-*/Contents/Home')))
    if server_dir:
        # Normal servers preserve nested jars; Loom strips them from development dependencies.
        probe_task = 'remapRuntimeProbeJar' if loader == 'fabric' and not mc.startswith('26.') else 'runtimeProbeJar'
        command[command.index('runServer' if loader == 'fabric' else 'runProbeServer')] = probe_task
        with (instance/'probe-build.log').open('w') as log:
            subprocess.run(command, cwd=ROOT, env=env, stdout=log, stderr=subprocess.STDOUT, check=True, timeout=600)
        mods=instance/'mods';mods.mkdir(exist_ok=True)
        shutil.copy2(packaged, mods/packaged.name)
        classifier='-remapped' if probe_task == 'remapRuntimeProbeJar' else ''
        properties=dict(line.split('=',1) for line in (ROOT/'gradle.properties').read_text().splitlines() if '=' in line)
        probe_jar=ROOT/f'build/profiles/{mc}-{loader}/libs/kill_confirmed-runtime-probe-{properties["mod_version"]}+{mc}{classifier}.jar'
        shutil.copy2(probe_jar, mods/probe_jar.name)
        for name in ['libraries','versions']:
            source=server_dir/name
            if source.exists() and not (instance/name).exists(): (instance/name).symlink_to(source.resolve(), target_is_directory=True)
        java=str(Path(env['JAVA_HOME'])/'bin/java')
        if loader == 'fabric':
            for name in ['fabric-server-launch.jar','fabric-server-launcher.properties','server.jar']:
                shutil.copy2(server_dir/name,instance/name)
            api=next((server_dir/'mods').glob('fabric-api-*.jar'))
            shutil.copy2(api,mods/api.name)
            command=[java,'-Xmx1G','-jar','fabric-server-launch.jar','nogui']
        else:
            command=[java,'-Xmx1G',f'@libraries/net/neoforged/neoforge/{cfg["neo"]}/unix_args.txt','nogui']
    log_path=instance/'run.log'
    with log_path.open('w') as log:
        process=subprocess.Popen(command,cwd=instance if server_dir else ROOT,env=env,stdout=log,stderr=subprocess.STDOUT,start_new_session=True)
        try: code=process.wait(timeout=600)
        except subprocess.TimeoutExpired:
            os.killpg(process.pid,signal.SIGTERM)
            try:code=process.wait(timeout=10)
            except subprocess.TimeoutExpired:
                os.killpg(process.pid,signal.SIGKILL);code=process.wait()
    text=log_path.read_text()
    result={'minecraft':mc,'loader':loader,'strategy':strategy,'client':client,'packaged':packaged is not None,'passed':code==0 and marker.exists() and ('KC_CLIENT_PASS' if client else 'KC_RUNTIME_PASS') in text and ('KC_CLIENT_FAIL' if client else 'KC_RUNTIME_FAIL') not in text,'exit_code':code,'assertions':text.count('KC_ASSERT_PASS'),'log':str(log_path)}
    (instance/'result.json').write_text(json.dumps(result,indent=2)+'\n')
    print(json.dumps(result),flush=True)
    return result['passed']
if __name__=='__main__':
    p=argparse.ArgumentParser();p.add_argument('--minecraft',required=True);p.add_argument('--loader',required=True);p.add_argument('--strategy',default='AT_DEATH_POSITION');p.add_argument('--output',type=Path,required=True);p.add_argument('--rinlib',type=Path,required=True);p.add_argument('--eula',type=Path,required=True);p.add_argument('--packaged',type=Path);p.add_argument('--client',action='store_true');p.add_argument('--server-dir',type=Path,help='Installed normal server for packaged jar-in-jar checks');a=p.parse_args()
    if a.server_dir and (not a.packaged or a.client):p.error('--server-dir requires a packaged dedicated server test')
    if a.packaged and a.loader=='fabric' and not a.client and not a.server_dir:p.error('Fabric nested jar tests need --server-dir pointing to an installed normal server')
    if 'eula=true' not in a.eula.read_text():p.error('Use an existing accepted EULA file')
    raise SystemExit(0 if run(a.minecraft,a.loader,a.strategy,a.output.resolve(),a.rinlib.resolve(),a.eula.resolve(),a.packaged.resolve() if a.packaged else None,a.client,a.server_dir.resolve() if a.server_dir else None) else 1)
