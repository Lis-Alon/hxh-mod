package com.hxhmod.item;

import com.hxhmod.HxHMod;
import com.hxhmod.block.ModBlocks;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;

import java.util.function.Function;

public final class ModItems {
	public static final Item HUNTER_LICENSE = register("hunter_license", Item::new,
			new Item.Properties().stacksTo(1).rarity(Rarity.RARE));
	public static final Item WATER_GLASS = register("water_glass", properties -> new WaterGlassItem(ModBlocks.WATER_GLASS, properties),
			new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON));

	private ModItems() {
	}

	private static Item register(String name, Function<Item.Properties, Item> factory, Item.Properties properties) {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, HxHMod.id(name));
		return Registry.register(BuiltInRegistries.ITEM, key, factory.apply(properties.setId(key)));
	}

	// Aba "Hunter x Hunter" no inventario do criativo, com todos os itens do mod
	public static final CreativeModeTab TAB = Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, HxHMod.id("items"),
			FabricCreativeModeTab.builder()
					.title(Component.translatable("itemGroup.hxh.items"))
					.icon(() -> new ItemStack(HUNTER_LICENSE))
					.displayItems((parameters, output) -> {
						output.accept(HUNTER_LICENSE);
						output.accept(WATER_GLASS);
					})
					.build());

	public static void register() {
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES)
				.register(output -> {
					output.accept(HUNTER_LICENSE);
					output.accept(WATER_GLASS);
				});
	}
}
