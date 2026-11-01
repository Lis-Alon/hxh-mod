package com.hxhmod.item;

import com.hxhmod.nen.ModAttachments;
import com.hxhmod.nen.NenData;
import com.hxhmod.nen.NenRules;
import com.hxhmod.nen.Technique;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.level.Level;

/**
 * Treino de videncia: segure o botao direito para envolver o copo com Ren.
 * Cada sessao completa gasta aura e avanca um pouco a leitura, ate revelar o tipo de Nen.
 */
public class WaterGlassItem extends Item {
	public WaterGlassItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		NenData data = player.getAttachedOrCreate(ModAttachments.NEN);
		Component problem = trainingProblem(data);
		if (problem != null) {
			if (!level.isClientSide()) {
				player.sendOverlayMessage(problem);
			}
			return InteractionResult.FAIL;
		}
		player.startUsingItem(hand);
		return InteractionResult.CONSUME;
	}

	/** Motivo para nao poder treinar agora, ou null se estiver tudo certo. */
	private static Component trainingProblem(NenData data) {
		if (!data.awakened()) {
			return Component.translatable("nen.hxh.divination.need_awakening");
		}
		if (data.revealed()) {
			return Component.translatable("nen.hxh.already_known", data.type().displayName());
		}
		if (!Technique.REN.isUnlocked(data)) {
			return Component.translatable("nen.hxh.divination.need_ren");
		}
		if (data.aura() < NenRules.DIVINATION_AURA_COST) {
			return Component.translatable("nen.hxh.divination.need_aura", NenRules.DIVINATION_AURA_COST);
		}
		return null;
	}

	@Override
	public int getUseDuration(ItemStack stack, LivingEntity entity) {
		return NenRules.DIVINATION_USE_TICKS;
	}

	@Override
	public ItemUseAnimation getUseAnimation(ItemStack stack) {
		return ItemUseAnimation.BOW;
	}

	@Override
	public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
		if (!(entity instanceof ServerPlayer player)) {
			return stack;
		}
		NenData data = player.getAttachedOrCreate(ModAttachments.NEN);
		Component problem = trainingProblem(data);
		if (problem != null) {
			player.sendOverlayMessage(problem);
			return stack;
		}

		int gain = NenRules.DIVINATION_MIN_GAIN
				+ player.getRandom().nextInt(NenRules.DIVINATION_MAX_GAIN - NenRules.DIVINATION_MIN_GAIN + 1);
		NenData result = data.withAura(data.aura() - NenRules.DIVINATION_AURA_COST)
				.addXp(Technique.REN, NenRules.DIVINATION_REN_XP)
				.withDivination(data.divination() + gain);
		player.setAttached(ModAttachments.NEN, result);

		if (result.revealed()) {
			player.sendSystemMessage(result.type().waterReaction());
			player.sendSystemMessage(Component.translatable("nen.hxh.revealed", result.type().displayName()));
			level.playSound(null, player.getX(), player.getY(), player.getZ(),
					SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0F, 1.2F);
		} else {
			Component hint = result.divination() >= NenRules.DIVINATION_HINT_AT
					? result.type().waterHint()
					: Component.translatable("nen.hxh.divination.nothing");
			player.sendSystemMessage(hint);
			player.sendOverlayMessage(Component.translatable("nen.hxh.divination.progress", result.divination()));
			level.playSound(null, player.getX(), player.getY(), player.getZ(),
					SoundEvents.BOTTLE_FILL, SoundSource.PLAYERS, 0.6F, 1.0F);
		}
		return stack;
	}
}
