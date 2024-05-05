# Expo

A multiplayer procedural sandbox game created with [libGDX](https://libgdx.com) and [OpenGL](https://opengl.org).

# Important note

The menu GUI is not working yet, so you will have to make use of the ingame console (F1) to connect to a server or to open a singleplayer instance.

To get an overview of possible commands, type `/help` in the console.

Launch a singleplayer world with `/world <name> [seed]`.
Connect to a multiplayer server with `/connect <ip>`.

## Used frameworks/libraries

- `libGDX`: https://github.com/libgdx/libgdx
- `Steamworks4J`: https://github.com/code-disaster/steamworks4j
- `KryoNet`: https://github.com/EsotericSoftware/kryonet
- `MakeSomeNoise`: https://github.com/tommyettinger/make-some-noise
- `JBump`: https://github.com/implicit-invocation/jbump
- `Oshi`: https://github.com/oshi/oshi
- `ImGui-Java`: https://github.com/SpaiR/imgui-java

## Modules

- `assets` and `assets_shared`: Shaders, audio and database files.
- `core`: Platform independent client code.
- `lwjgl3`: Desktop specific launch code.
- `server`: Dedicated server code.
- `shared`: Contains useful cross-module utility classes.

## Screenshots

![Screenshot 1](/assets/s1.png "Screenshot 1")
![Screenshot 2](/assets/s2.png "Screenshot 2")