package com.hxhmod.network;

import com.hxhmod.HxHMod;
import com.hxhmod.nen.GyoApplications;
import com.hxhmod.nen.NenActions;
import com.hxhmod.nen.NenPose;
import com.hxhmod.nen.NenStat;
import com.hxhmod.nen.NenUpgrade;
import com.hxhmod.nen.Technique;
import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.Map;

/** Pedidos que o painel do Nen manda para o servidor. O servidor sempre confere se o pedido e valido. */
public final class NenPayloads {
	private NenPayloads() {
	}

	/** Liga a tecnica, ou desliga se ja estiver ativa. */
	public record ToggleTechnique(Technique technique) implements CustomPacketPayload {
		public static final Type<ToggleTechnique> TYPE = new Type<>(HxHMod.id("toggle_technique"));
		public static final StreamCodec<ByteBuf, ToggleTechnique> CODEC =
				Technique.STREAM_CODEC.map(ToggleTechnique::new, ToggleTechnique::technique);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	public record BuyUpgrade(NenUpgrade upgrade) implements CustomPacketPayload {
		public static final Type<BuyUpgrade> TYPE = new Type<>(HxHMod.id("buy_upgrade"));
		public static final StreamCodec<ByteBuf, BuyUpgrade> CODEC =
				NenUpgrade.STREAM_CODEC.map(BuyUpgrade::new, BuyUpgrade::upgrade);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	/** Os pontos distribuidos na aba Nivel, enviados de uma vez ao confirmar. */
	public record AllocateStats(Map<NenStat, Integer> points) implements CustomPacketPayload {
		public static final Type<AllocateStats> TYPE = new Type<>(HxHMod.id("allocate_stats"));
		public static final StreamCodec<ByteBuf, AllocateStats> CODEC =
				ByteBufCodecs.fromCodec(Codec.unboundedMap(NenStat.CODEC, Codec.INT))
						.map(AllocateStats::new, AllocateStats::points);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	/** Painel aberto ou fechado: com ele aberto o personagem senta em posicao de meditacao. */
	/** Proxima acao com o Gyo preparado; a direcao so vale para o dash. */
	public record GyoApplication(GyoApplications.Action action, float directionX, float directionZ) implements CustomPacketPayload {
		public static final Type<GyoApplication> TYPE = new Type<>(HxHMod.id("gyo_application"));
		public static final StreamCodec<ByteBuf, GyoApplication> CODEC = StreamCodec.composite(
				GyoApplications.Action.STREAM_CODEC, GyoApplication::action,
				ByteBufCodecs.FLOAT, GyoApplication::directionX,
				ByteBufCodecs.FLOAT, GyoApplication::directionZ,
				GyoApplication::new);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	public record ResetUpgrades() implements CustomPacketPayload {
		public static final Type<ResetUpgrades> TYPE = new Type<>(HxHMod.id("reset_upgrades"));
		public static final StreamCodec<ByteBuf, ResetUpgrades> CODEC = StreamCodec.unit(new ResetUpgrades());

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	public record PanelOpen(boolean open) implements CustomPacketPayload {
		public static final Type<PanelOpen> TYPE = new Type<>(HxHMod.id("panel_open"));
		public static final StreamCodec<ByteBuf, PanelOpen> CODEC = ByteBufCodecs.BOOL.map(PanelOpen::new, PanelOpen::open);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	public static void register() {
		PayloadTypeRegistry.serverboundPlay().register(ToggleTechnique.TYPE, ToggleTechnique.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(BuyUpgrade.TYPE, BuyUpgrade.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(AllocateStats.TYPE, AllocateStats.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(GyoApplication.TYPE, GyoApplication.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(ResetUpgrades.TYPE, ResetUpgrades.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(PanelOpen.TYPE, PanelOpen.CODEC);

		ServerPlayNetworking.registerGlobalReceiver(ToggleTechnique.TYPE,
				(payload, context) -> NenActions.toggle(context.player(), payload.technique()));
		ServerPlayNetworking.registerGlobalReceiver(BuyUpgrade.TYPE,
				(payload, context) -> NenActions.buy(context.player(), payload.upgrade()));
		ServerPlayNetworking.registerGlobalReceiver(AllocateStats.TYPE,
				(payload, context) -> NenActions.allocate(context.player(), payload.points()));
		ServerPlayNetworking.registerGlobalReceiver(GyoApplication.TYPE, (payload, context) ->
				GyoApplications.use(context.player(), payload.action(), payload.directionX(), payload.directionZ()));
		ServerPlayNetworking.registerGlobalReceiver(ResetUpgrades.TYPE,
				(payload, context) -> NenActions.resetUpgrades(context.player()));
		ServerPlayNetworking.registerGlobalReceiver(PanelOpen.TYPE,
				(payload, context) -> NenPose.setPanelOpen(context.player(), payload.open()));
	}
}
