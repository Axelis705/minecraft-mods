#!/usr/bin/env python3
"""
La Fábrica: crea un mod nuevo que usa Axelis Core, listo para compilar y probar en 1.20.1, 1.21.1 y 26.3.

    python3 tools/mkmod.py <mod_id> "<Nombre del mod>" "<descripción>" [ClasePrincipal]

Crea la carpeta <mod_id>/ con:
  - Gradle (ForgeGradle 6 para 1.20.1; las otras versiones las genera tools/mkproject.py) con Axelis Core como dependencia
    (gradle.properties: usa_axelis_core=true). El jar final lleva el Core dentro (tools/jarjar.py).
  - mods.toml que pide axelis_core, pack.mcmeta, idiomas (es_es, es_mx, en_us).
  - Clase principal con su canal de red, registros (objetos, bloques, entidades, sonidos), comando /<mod_id> registrado con el Core,
    eventos del cliente y un escenario de prueba con fotos (cliente-core.yml con mod=<mod_id>).
  - ci_commands.txt (prueba del servidor) y real_commands.txt (prueba en servidor real con el Core dentro).
Además lo agrega a tools/mods_core.txt (la lista que compila el flujo mods-core.yml).
"""
import json
import os
import re
import shutil
import sys

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..")


def camel(mod_id):
    return "".join(p.capitalize() for p in re.split(r"[_\-]", mod_id))


def write(path, text):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as fh:
        fh.write(text)


def main(mod_id, name, desc, cls=None):
    if not re.fullmatch(r"[a-z][a-z0-9_]{1,40}", mod_id):
        sys.exit("el id del mod debe ser minúsculas, números y _ (por ejemplo: pesca, escape_rooms)")
    cls = cls or camel(mod_id)
    pkg = "com." + mod_id.replace("_", "")
    base = os.path.join(ROOT, mod_id)
    if os.path.exists(base):
        sys.exit(f"ya existe la carpeta {mod_id}/")
    tpl = os.path.join(ROOT, "core_prueba")
    os.makedirs(base)
    for f in ("gradlew", "gradlew.bat", "settings.gradle"):
        shutil.copy(os.path.join(tpl, f), os.path.join(base, f))
    shutil.copytree(os.path.join(tpl, "gradle"), os.path.join(base, "gradle"))
    os.chmod(os.path.join(base, "gradlew"), 0o755)
    shutil.copy(os.path.join(tpl, "build.gradle"), os.path.join(base, "build.gradle"))
    core_ver = "1.0.0"
    with open(os.path.join(ROOT, "axelis_core", "gradle.properties"), encoding="utf-8") as fh:
        m = re.search(r"^mod_version=(.+)$", fh.read(), re.M)
        if m:
            core_ver = m.group(1).strip()
    write(os.path.join(base, "gradle.properties"), f"""org.gradle.jvmargs=-Xmx3G
org.gradle.daemon=false

minecraft_version=1.20.1
minecraft_version_range=[1.20.1,1.21)
forge_version=47.4.10
forge_version_range=[47,)
loader_version_range=[47,)
mapping_channel=official
mapping_version=1.20.1

mod_id={mod_id}
mod_name={name}
mod_license=All Rights Reserved
mod_version=1.0.0
mod_group_id={pkg}
mod_authors=Axelis
mod_description={desc}
# usa Axelis Core (tools/mkproject.py lo agrega como dependencia en 1.21.1 y 26.3; en 1.20.1 lo hace build.gradle)
usa_axelis_core=true
axelis_core_version={core_ver}
""")
    res = os.path.join(base, "src", "main", "resources")
    write(os.path.join(res, "META-INF", "mods.toml"), """modLoader="javafml"
loaderVersion="${loader_version_range}"
license="${mod_license}"

[[mods]]
modId="${mod_id}"
version="${mod_version}"
displayName="${mod_name}"
authors="${mod_authors}"
displayTest="MATCH_VERSION"
description='''${mod_description}'''

[[dependencies.${mod_id}]]
    modId="forge"
    mandatory=true
    versionRange="${forge_version_range}"
    ordering="NONE"
    side="BOTH"
[[dependencies.${mod_id}]]
    modId="minecraft"
    mandatory=true
    versionRange="${minecraft_version_range}"
    ordering="NONE"
    side="BOTH"
[[dependencies.${mod_id}]]
    modId="axelis_core"
    mandatory=true
    versionRange="[""" + core_ver + """,)"
    ordering="AFTER"
    side="BOTH"
""")
    shutil.copy(os.path.join(tpl, "src", "main", "resources", "pack.mcmeta"), os.path.join(res, "pack.mcmeta"))
    lang = {f"itemGroup.{mod_id}": name, f"{mod_id}.hola": f"{name} funciona con %s"}
    for f in ("es_es", "es_mx"):
        write(os.path.join(res, "assets", mod_id, "lang", f + ".json"), json.dumps(lang, ensure_ascii=False, indent=2))
    write(os.path.join(res, "assets", mod_id, "lang", "en_us.json"),
          json.dumps({f"itemGroup.{mod_id}": name, f"{mod_id}.hola": f"{name} works with %s"}, ensure_ascii=False, indent=2))

    java = os.path.join(base, "src", "main", "java", *pkg.split("."))
    write(os.path.join(java, cls + ".java"), f"""package {pkg};

import com.axeliscore.AxelisCore;
import com.axeliscore.plat.Net;
import com.mojang.logging.LogUtils;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
//#if MC >= 26.1
//$$ import net.minecraftforge.eventbus.api.bus.BusGroup;
//#else
import net.minecraftforge.eventbus.api.IEventBus;
//#endif
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

/** {name}: {desc} */
@Mod({cls}.MODID)
public class {cls} {{
    public static final String MODID = "{mod_id}";
    public static final Logger LOGGER = LogUtils.getLogger();
    /** Canal de mensajes del mod: si cambias qué mensajes hay, sube el número. */
    public static final Net NET = new Net(MODID, "main", 1);

    public {cls}(FMLJavaModLoadingContext context) {{
        AxelisCore.use(MODID);
        //#if MC >= 26.1
        //$$ BusGroup bus = context.getModBusGroup();
        //$$ FMLCommonSetupEvent.getBus(bus).addListener(this::commonSetup);
        //#else
        IEventBus bus = context.getModEventBus();
        bus.addListener(this::commonSetup);
        //#endif
        {pkg}.registry.ModRegistry.register(bus);
        AxelisCore.commands(d -> d.register(Commands.literal(MODID).executes(c -> {{
            c.getSource().sendSuccess(() -> Component.translatable(MODID + ".hola", AxelisCore.version()), false);
            LOGGER.info("PRUEBA_{mod_id.upper()}_OK {{}}", AxelisCore.version());
            return 1;
        }})));
    }}

    private void commonSetup(final FMLCommonSetupEvent event) {{
        event.enqueueWork(() -> {{
            // aquí se registran los mensajes de red: NET.register(Mensaje.class, Mensaje::write, Mensaje::read, (m, jugador) -> ...);
            NET.done();
        }});
    }}
}}
""")
    write(os.path.join(java, "registry", "ModRegistry.java"), f"""package {pkg}.registry;

import {pkg}.{cls};
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
//#if MC >= 26.1
//$$ import net.minecraftforge.eventbus.api.bus.BusGroup;
//#else
import net.minecraftforge.eventbus.api.IEventBus;
//#endif
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Registros del mod. Para crear cosas usa las ayudas del Core, que ya ponen el id como pide cada versión:
 * <pre>
 *     public static final RegistryObject&lt;Item&gt; ANZUELO = ITEMS.register("anzuelo", () -> new Item(Reg.item(MODID, "anzuelo")));
 * </pre>
 */
public final class ModRegistry {{
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, {cls}.MODID);
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, {cls}.MODID);
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, {cls}.MODID);
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, {cls}.MODID);

    private ModRegistry() {{
    }}

    //#if MC >= 26.1
    //$$ public static void register(BusGroup bus) {{
    //#else
    public static void register(IEventBus bus) {{
    //#endif
        ITEMS.register(bus);
        BLOCKS.register(bus);
        ENTITIES.register(bus);
        SOUNDS.register(bus);
    }}
}}
""")
    write(os.path.join(java, "client", "ModClient.java"), f"""package {pkg}.client;

import {pkg}.{cls};
import com.axeliscore.test.ClientTest;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

/** Lo del cliente: renderizadores, pantallas y el escenario de la prueba con fotos. */
@Mod.EventBusSubscriber(modid = {cls}.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ModClient {{
    private ModClient() {{
    }}

    @SubscribeEvent
    public static void onRenderers(EntityRenderersEvent.RegisterRenderers e) {{
        // e.registerEntityRenderer(ModRegistry.ALGO.get(), c -> new com.axeliscore.client.SimpleGeoRenderer<>(c, {cls}.MODID, "algo", 0.5F, 1F));
    }}

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent e) {{
        // prueba con fotos: cliente-core.yml con mod={mod_id} y escenario={mod_id}
        ClientTest.scenario({cls}.MODID, t -> {{
            t.add("preparar", 2000, () -> {{
                t.cmd("gamemode creative @a");
                t.cmd("reload");
            }});
            t.add("comando", 1500, () -> t.cmd({cls}.MODID));
            t.photo("inicio", 1500, () -> t.lookFrom(0.5, -60, -4.5, 0.5, -59, 0.5));
        }});
    }}
}}
""")
    write(os.path.join(base, "ci_commands.txt"), f"""axeliscore
{mod_id}
#expect PRUEBA_{mod_id.upper()}_OK Axelis Core
#expect usuarios=[{mod_id}]
""")
    shutil.copy(os.path.join(base, "ci_commands.txt"), os.path.join(base, "real_commands.txt"))
    write(os.path.join(base, "docs", mod_id + ".md"), f"# {name}\n\n{desc}\n\nUsa Axelis Core (va dentro del jar).\n\n## Etapas\n")
    lst = os.path.join(ROOT, "tools", "mods_core.txt")
    mods = []
    if os.path.exists(lst):
        with open(lst, encoding="utf-8") as fh:
            mods = [l.strip() for l in fh if l.strip() and not l.startswith("#")]
    if mod_id not in [m.split()[0] for m in mods]:
        mods.append(mod_id)
    write(lst, "# mods que usan Axelis Core (los compila y prueba .github/workflows/mods-core.yml en 1.20.1, 1.21.1 y 26.3;\n"
          "# con versiones después del nombre, solo en esas: por ejemplo \"evento 1.20.1\")\n" + "\n".join(mods) + "\n")
    # el flujo de los mods viejos (build.yml) no tiene que correr cuando solo cambia este mod
    wf = os.path.join(ROOT, ".github", "workflows", "build.yml")
    if os.path.exists(wf):
        with open(wf, encoding="utf-8") as fh:
            y = fh.read()
        line = f"      - '{mod_id}/**'\n"
        anchor = "      - 'core_prueba/**'\n"
        if line not in y and anchor in y:
            with open(wf, "w", encoding="utf-8") as fh:
                fh.write(y.replace(anchor, anchor + line, 1))
    print(f"mod {mod_id} creado en {mod_id}/ (clase {pkg}.{cls})")


if __name__ == "__main__":
    if len(sys.argv) < 4:
        sys.exit(__doc__)
    main(*sys.argv[1:])
