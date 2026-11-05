package com.hxhmod.nen;

import com.hxhmod.HxHMod;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.network.codec.ByteBufCodecs;

public final class ModAttachments {
	// Salvo no mundo, mantido ao morrer e enviado so para o proprio jogador (para o HUD)
	public static final AttachmentType<NenData> NEN = AttachmentRegistry.create(HxHMod.id("nen"),
			builder -> builder
					.initializer(() -> NenData.DEFAULT)
					.persistent(NenData.CODEC)
					.copyOnDeath()
					.syncWith(NenData.STREAM_CODEC, AttachmentSyncPredicate.targetOnly()));

	// Se o jogador esta na pose de meditacao; nao e salvo, e todos por perto recebem para desenhar a pose
	public static final AttachmentType<Boolean> MEDITATING = AttachmentRegistry.create(HxHMod.id("meditating"),
			builder -> builder
					.initializer(() -> false)
					.syncWith(ByteBufCodecs.BOOL, AttachmentSyncPredicate.all()));

	// O que os outros precisam para desenhar a aura; nao e salvo, sai do NenData a cada tick
	public static final AttachmentType<AuraShow> AURA_SHOW = AttachmentRegistry.create(HxHMod.id("aura_show"),
			builder -> builder
					.initializer(() -> AuraShow.NONE)
					.syncWith(AuraShow.STREAM_CODEC, AttachmentSyncPredicate.all()));

	// Cores escolhidas na aba Aura; salvas no mundo, mantidas ao morrer e enviadas a todos, que precisam delas para desenhar
	public static final AttachmentType<AuraColors> AURA_COLORS = AttachmentRegistry.create(HxHMod.id("aura_colors"),
			builder -> builder
					.initializer(() -> AuraColors.DEFAULT)
					.persistent(AuraColors.CODEC)
					.copyOnDeath()
					.syncWith(AuraColors.STREAM_CODEC, AttachmentSyncPredicate.all()));

	// Recarga das aplicacoes do Gyo; salva para nao zerar saindo do mundo ou morrendo, e o cliente precisa dela para o HUD e as teclas
	public static final AttachmentType<GyoCooldowns> GYO_COOLDOWNS = AttachmentRegistry.create(HxHMod.id("gyo_cooldowns"),
			builder -> builder
					.initializer(() -> GyoCooldowns.NONE)
					.persistent(GyoCooldowns.CODEC)
					.copyOnDeath()
					.syncWith(GyoCooldowns.STREAM_CODEC, AttachmentSyncPredicate.targetOnly()));

	// Hatsu equipado na tecla e recarga dele; salvo no mundo e enviado so para o proprio jogador
	public static final AttachmentType<HatsuState> HATSU_STATE = AttachmentRegistry.create(HxHMod.id("hatsu_state"),
			builder -> builder
					.initializer(() -> HatsuState.NONE)
					.persistent(HatsuState.CODEC)
					.copyOnDeath()
					.syncWith(HatsuState.STREAM_CODEC, AttachmentSyncPredicate.targetOnly()));

	private ModAttachments() {
	}

	public static void register() {
		// Carrega a classe para registrar os campos acima
	}
}
