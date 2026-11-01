package com.hxhmod.nen;

import com.hxhmod.HxHMod;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;

public final class ModAttachments {
	// Salvo no mundo, mantido ao morrer e enviado so para o proprio jogador (para o HUD)
	public static final AttachmentType<NenData> NEN = AttachmentRegistry.create(HxHMod.id("nen"),
			builder -> builder
					.initializer(() -> NenData.DEFAULT)
					.persistent(NenData.CODEC)
					.copyOnDeath()
					.syncWith(NenData.STREAM_CODEC, AttachmentSyncPredicate.targetOnly()));

	private ModAttachments() {
	}

	public static void register() {
		// Carrega a classe para registrar os campos acima
	}
}
