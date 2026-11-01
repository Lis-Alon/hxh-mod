package com.hxhmod.nen;

import net.minecraft.server.level.ServerPlayer;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Decide quando o jogador fica sentado em posicao de meditacao (pernas cruzadas):
 * com o painel do Nen aberto, meditando para despertar ou concentrado no copo d'agua.
 */
public final class NenPose {
	private static final Set<ServerPlayer> PANEL_OPEN = Collections.newSetFromMap(new WeakHashMap<>());
	private static final Set<ServerPlayer> AWAKENING = Collections.newSetFromMap(new WeakHashMap<>());

	private NenPose() {
	}

	public static void setPanelOpen(ServerPlayer player, boolean open) {
		if (open) {
			PANEL_OPEN.add(player);
		} else {
			PANEL_OPEN.remove(player);
		}
		update(player);
	}

	public static boolean panelOpen(ServerPlayer player) {
		return PANEL_OPEN.contains(player);
	}

	static void setAwakening(ServerPlayer player, boolean meditating) {
		if (meditating) {
			AWAKENING.add(player);
		} else {
			AWAKENING.remove(player);
		}
	}

	/** Atualiza o que os outros jogadores veem, so quando muda. */
	public static void update(ServerPlayer player) {
		boolean meditating = PANEL_OPEN.contains(player) || AWAKENING.contains(player) || Divination.inSession(player);
		if (player.getAttachedOrElse(ModAttachments.MEDITATING, false) != meditating) {
			player.setAttached(ModAttachments.MEDITATING, meditating);
		}
	}
}
