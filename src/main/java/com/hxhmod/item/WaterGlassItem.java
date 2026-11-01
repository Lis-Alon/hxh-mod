package com.hxhmod.item;

import com.hxhmod.nen.ModAttachments;
import com.hxhmod.nen.NenData;
import com.hxhmod.nen.NenType;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

/** O teste do copo de agua com uma folha: revela o tipo de Nen do jogador. */
public class WaterGlassItem extends Item {
	private static final int SPECIALIZATION_CHANCE = 20; // 1 em 20

	private static final NenType[] COMMON_TYPES = {NenType.ENHANCEMENT, NenType.TRANSMUTATION,
			NenType.EMISSION, NenType.CONJURATION, NenType.MANIPULATION};

	public WaterGlassItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		if (level.isClientSide()) {
			return InteractionResult.SUCCESS;
		}

		NenData data = player.getAttachedOrCreate(ModAttachments.NEN);
		if (data.awakened()) {
			player.sendOverlayMessage(Component.translatable("nen.hxh.already_known", data.type().displayName()));
			return InteractionResult.SUCCESS_SERVER;
		}

		NenType type = rollType(level.getRandom());
		player.setAttached(ModAttachments.NEN, new NenData(type, NenData.STARTING_MAX_AURA, NenData.STARTING_MAX_AURA));

		player.sendSystemMessage(type.waterReaction());
		player.sendSystemMessage(Component.translatable("nen.hxh.revealed", type.displayName()));
		level.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0F, 1.2F);
		return InteractionResult.SUCCESS_SERVER;
	}

	private static NenType rollType(RandomSource random) {
		if (random.nextInt(SPECIALIZATION_CHANCE) == 0) {
			return NenType.SPECIALIZATION;
		}
		return COMMON_TYPES[random.nextInt(COMMON_TYPES.length)];
	}
}
