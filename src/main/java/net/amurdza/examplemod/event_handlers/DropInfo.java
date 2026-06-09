package net.amurdza.examplemod.event_handlers;

import net.minecraft.world.item.Item;

public record DropInfo(
        Item meatItem,
        Item cookedMeatItem,
        Item skinItem,
        Item boneItem
) { }