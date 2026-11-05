package com.hxhmod;

import com.hxhmod.block.ModBlocks;
import com.hxhmod.item.ModItems;
import com.hxhmod.nen.ModAttachments;
import com.hxhmod.nen.NenEvents;
import com.hxhmod.network.NenPayloads;
import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HxHMod implements ModInitializer {
	public static final String MOD_ID = "hxh";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		ModBlocks.register();
		ModItems.register();
		ModAttachments.register();
		NenEvents.register();
		NenPayloads.register();
		LanAccess.register();
		LOGGER.info("Hunter x Hunter Mod carregado!");
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
