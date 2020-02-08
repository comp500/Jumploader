# Jumploader
Jumploader is a mod that allows you to use Fabric mods in Twitch modpacks, by loading Fabric as if it were a Forge mod. It works on both the client and server, however it's only necessary on the server if you want to launch the game using tools that only support Forge. Jumploader is fully configurable to download any Minecraft version and mod loader, but by default it will download the Fabric loader and library versions corresponding to your current version of the game. It only loads if your game was launched from Forge/ModLauncher, so it won't do anything if your Minecraft launcher already supports Fabric.

## What it does
- Loads Fabric (or a different configured mod loader) instead of Forge, from a Forge installation.
- Automatically downloads the latest Fabric loader version for your version of the game, with no required configuration.
- If necessary, downloads the unpatched Minecraft game, so that Fabric can be used with it.
- Ensures that Fabric and Mixin don't see the classes loaded by ModLauncher.

## What it doesn't do
- Jumploader doesn't let you run Fabric mods in tandem with Forge mods - it hijacks the launching process so that Forge never loads. See [Patchwork](https://github.com/PatchworkMC) for a project aiming to do this.
- Jumploader doesn't currently work very well on the client for loading a Minecraft game with a different major version (including snapshots) to the currently loaded game, as supplementary assets (such as sounds) are downloaded separately by the game launcher.

## Requirements
Requires a Forge version that uses ModLauncher (or ModLauncher on it's own, without Forge) - this usually means 1.13 or newer.