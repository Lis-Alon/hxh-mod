package com.hxhmod.network;

import com.hxhmod.HxHMod;
import com.hxhmod.nen.Technique;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Enviado pelo painel do Nen quando o jogador liga ou desliga uma tecnica. */
public record ToggleTechniquePayload(Technique technique) implements CustomPacketPayload {
	public static final Type<ToggleTechniquePayload> TYPE = new Type<>(HxHMod.id("toggle_technique"));
	public static final StreamCodec<ByteBuf, ToggleTechniquePayload> CODEC =
			Technique.STREAM_CODEC.map(ToggleTechniquePayload::new, ToggleTechniquePayload::technique);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
