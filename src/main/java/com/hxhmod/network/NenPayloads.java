package com.hxhmod.network;

import com.hxhmod.HxHMod;
import com.hxhmod.nen.AuraColors;
import com.hxhmod.nen.GyoApplications;
import com.hxhmod.nen.HatsuActions;
import com.hxhmod.nen.KoStrike;
import com.hxhmod.nen.ModAttachments;
import com.hxhmod.nen.NenActions;
import com.hxhmod.nen.RyuFlow;
import com.hxhmod.nen.NenPose;
import com.hxhmod.nen.NenType;
import com.hxhmod.nen.NenUpgrade;
import com.hxhmod.nen.Technique;
import io.netty.buffer.ByteBuf;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;


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

	public record CycleIntensity(Technique technique) implements CustomPacketPayload {
		public static final Type<CycleIntensity> TYPE = new Type<>(HxHMod.id("cycle_intensity"));
		public static final StreamCodec<ByteBuf, CycleIntensity> CODEC =
				Technique.STREAM_CODEC.map(CycleIntensity::new, CycleIntensity::technique);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	/** Tecla do Ko apertada ou solta. */
	public record KoHold(boolean holding) implements CustomPacketPayload {
		public static final Type<KoHold> TYPE = new Type<>(HxHMod.id("ko_hold"));
		public static final StreamCodec<ByteBuf, KoHold> CODEC = ByteBufCodecs.BOOL.map(KoHold::new, KoHold::holding);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	/** Ataque no ar com o Ko carregado. */
	public record KoMiss() implements CustomPacketPayload {
		public static final Type<KoMiss> TYPE = new Type<>(HxHMod.id("ko_miss"));
		public static final StreamCodec<ByteBuf, KoMiss> CODEC = StreamCodec.unit(new KoMiss());

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	public record SetRyuStance(RyuFlow.Stance stance) implements CustomPacketPayload {
		public static final Type<SetRyuStance> TYPE = new Type<>(HxHMod.id("ryu_stance"));
		public static final StreamCodec<ByteBuf, SetRyuStance> CODEC =
				RyuFlow.Stance.STREAM_CODEC.map(SetRyuStance::new, SetRyuStance::stance);

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

	/** Cores escolhidas na aba Aura. */
	public record SetAuraColors(int primary, int secondary) implements CustomPacketPayload {
		public static final Type<SetAuraColors> TYPE = new Type<>(HxHMod.id("set_aura_colors"));
		public static final StreamCodec<ByteBuf, SetAuraColors> CODEC = StreamCodec.composite(
				ByteBufCodecs.INT, SetAuraColors::primary,
				ByteBufCodecs.INT, SetAuraColors::secondary,
				SetAuraColors::new);

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

	/** Tecla do Hatsu: lanca o que estiver equipado. */
	public record UseHatsu() implements CustomPacketPayload {
		public static final Type<UseHatsu> TYPE = new Type<>(HxHMod.id("use_hatsu"));
		public static final StreamCodec<ByteBuf, UseHatsu> CODEC = StreamCodec.unit(new UseHatsu());

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	/** Clique num Hatsu aprendido da aba Hatsu. */
	public record EquipHatsu(NenType branch) implements CustomPacketPayload {
		public static final Type<EquipHatsu> TYPE = new Type<>(HxHMod.id("equip_hatsu"));
		public static final StreamCodec<ByteBuf, EquipHatsu> CODEC =
				ByteBufCodecs.VAR_INT.map(i -> new EquipHatsu(NenType.values()[i]), payload -> payload.branch().ordinal());

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	public static void register() {
		PayloadTypeRegistry.serverboundPlay().register(ToggleTechnique.TYPE, ToggleTechnique.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(BuyUpgrade.TYPE, BuyUpgrade.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(GyoApplication.TYPE, GyoApplication.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(CycleIntensity.TYPE, CycleIntensity.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(KoHold.TYPE, KoHold.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(KoMiss.TYPE, KoMiss.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(SetRyuStance.TYPE, SetRyuStance.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(ResetUpgrades.TYPE, ResetUpgrades.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(PanelOpen.TYPE, PanelOpen.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(SetAuraColors.TYPE, SetAuraColors.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(UseHatsu.TYPE, UseHatsu.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(EquipHatsu.TYPE, EquipHatsu.CODEC);

		ServerPlayNetworking.registerGlobalReceiver(ToggleTechnique.TYPE,
				(payload, context) -> NenActions.toggle(context.player(), payload.technique()));
		ServerPlayNetworking.registerGlobalReceiver(BuyUpgrade.TYPE,
				(payload, context) -> NenActions.buy(context.player(), payload.upgrade()));
		ServerPlayNetworking.registerGlobalReceiver(GyoApplication.TYPE, (payload, context) ->
				GyoApplications.use(context.player(), payload.action(), payload.directionX(), payload.directionZ()));
		ServerPlayNetworking.registerGlobalReceiver(CycleIntensity.TYPE, (payload, context) ->
				NenActions.cycleIntensity(context.player(), payload.technique()));
		ServerPlayNetworking.registerGlobalReceiver(KoHold.TYPE, (payload, context) ->
				KoStrike.hold(context.player(), payload.holding()));
		ServerPlayNetworking.registerGlobalReceiver(KoMiss.TYPE, (payload, context) -> KoStrike.miss(context.player()));
		ServerPlayNetworking.registerGlobalReceiver(SetRyuStance.TYPE, (payload, context) ->
				RyuFlow.setStance(context.player(), payload.stance()));
		ServerPlayNetworking.registerGlobalReceiver(ResetUpgrades.TYPE,
				(payload, context) -> NenActions.resetUpgrades(context.player()));
		ServerPlayNetworking.registerGlobalReceiver(SetAuraColors.TYPE, (payload, context) -> context.player().setAttached(
				ModAttachments.AURA_COLORS, new AuraColors(payload.primary(), payload.secondary()).sanitized()));
		ServerPlayNetworking.registerGlobalReceiver(UseHatsu.TYPE, (payload, context) -> HatsuActions.use(context.player()));
		ServerPlayNetworking.registerGlobalReceiver(EquipHatsu.TYPE, (payload, context) -> HatsuActions.equip(context.player(), payload.branch()));
		ServerPlayNetworking.registerGlobalReceiver(PanelOpen.TYPE,
				(payload, context) -> NenPose.setPanelOpen(context.player(), payload.open()));
	}
}
