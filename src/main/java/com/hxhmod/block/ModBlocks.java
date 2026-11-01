package com.hxhmod.block;

import com.hxhmod.HxHMod;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.PushReaction;

import java.util.function.Function;

public final class ModBlocks {
	// Copo d'agua com a folha, colocado no chao para o treino de videncia
	public static final Block WATER_GLASS = register("water_glass", WaterGlassBlock::new,
			BlockBehaviour.Properties.of().strength(0.3F).sound(SoundType.GLASS).noOcclusion()
					.pushReaction(PushReaction.POPPED));

	private ModBlocks() {
	}

	private static Block register(String name, Function<BlockBehaviour.Properties, Block> factory,
			BlockBehaviour.Properties properties) {
		ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, HxHMod.id(name));
		return Registry.register(BuiltInRegistries.BLOCK, key, factory.apply(properties.setId(key)));
	}

	public static void register() {
		// Carrega a classe para registrar os blocos acima
	}
}
