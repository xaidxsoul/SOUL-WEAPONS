# SoulWeapons 1.0.0

A first playable foundation for Paper 1.21.11.

## Requirements
- Paper 1.21.11
- Java 21
- Maven 3.9+

## Build
Run `mvn clean package`. The plugin JAR will be created in `target/SoulWeapons-1.0.0.jar`.

## Install
Copy the JAR into your server's `plugins` folder and restart the server.

## Commands
- `/sw list`
- `/sw give <player> <soulblade|stormaxe|inferno|reaper|titanhammer>`
- `/sw reload`

Admin permission: `soulweapons.admin` (default: op)

## First-version abilities
- Soul Blade: forward dash
- Storm Axe: lightning visual effect around nearby mobs
- Inferno: flame burst, ignition and damage
- Reaper: heals its wielder on hit
- Titan Hammer: area damage and knockback

Ability cooldown and ability tuning values are in `plugins/SoulWeapons/config.yml`.

This is source code, not a precompiled JAR. Build it with Maven and Java 21.
