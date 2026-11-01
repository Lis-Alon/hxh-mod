package com.hxhmod.item;

import com.hxhmod.nen.Divination;
import com.hxhmod.nen.ModAttachments;
import com.hxhmod.nen.NenData;
import com.hxhmod.nen.NenRules;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

/**
 * Treino de videncia: segure o botao direito para envolver o copo com Ren.
 * Cada sessao completa gasta aura e avanca um pouco a leitura, ate revelar o tipo de Nen.
 * Clicando num bloco, o copo e colocado no chao (veja {@link com.hxhmod.block.WaterGlassBlock}).
 */
public class WaterGlassItem extends BlockItem {
	public WaterGlassItem(Block block, Properties properties) {
		super(block, properties);
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		NenData data = player.getAttachedOrCreate(ModAttachments.NEN);
		Component problem = Divination.problem(data);
		if (problem != null) {
			if (!level.isClientSide()) {
				player.sendOverlayMessage(problem);
			}
			return InteractionResult.FAIL;
		}
		player.startUsingItem(hand);
		return InteractionResult.CONSUME;
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
		if (entity instanceof ServerPlayer player) {
			Divination.perform(player);
		}
		return stack;
	}
}
