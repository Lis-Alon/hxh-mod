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

	private ModAttachments() {
	}

	public static void register() {
		// Carrega a classe para registrar os campos acima
	}
}
