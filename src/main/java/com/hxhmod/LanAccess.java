package com.hxhmod;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Mundo aberto para LAN: o jogo sempre verifica a conta de quem entra e nao tem opcao para desligar.
 * Esta opcao do arquivo de configuracao desliga, como o online-mode de um servidor dedicado.
 * O servidor dedicado continua seguindo o server.properties dele.
 */
public final class LanAccess {
	private static final String KEY = "lan-online-mode";
	private static final String DEFAULT_FILE = """
			# false deixa entrar no mundo aberto para LAN quem nao tem conta original.
			# Com isso o mundo nao confere a conta: qualquer um que alcance a porta entra com o nome que quiser.
			lan-online-mode=true
			""";

	private LanAccess() {
	}

	static void register() {
		ServerLifecycleEvents.SERVER_STARTED.register(LanAccess::apply);
	}

	public static void apply(MinecraftServer server) {
		if (server.isDedicatedServer()) {
			return;
		}
		boolean online = onlineMode();
		server.setUsesAuthentication(online);
		if (!online) {
			HxHMod.LOGGER.warn("Mundo sem verificacao de conta: quem entrar pela LAN nao tem a conta conferida");
		}
	}

	public static Path configFile() {
		return FabricLoader.getInstance().getConfigDir().resolve("hxh-mod.properties");
	}

	/** Na duvida a verificacao fica ligada: so o valor false escrito no arquivo desliga. */
	private static boolean onlineMode() {
		Path file = configFile();
		try {
			if (!Files.exists(file)) {
				Files.createDirectories(file.getParent());
				Files.writeString(file, DEFAULT_FILE);
			}
			Properties properties = new Properties();
			try (Reader reader = Files.newBufferedReader(file)) {
				properties.load(reader);
			}
			return !"false".equalsIgnoreCase(properties.getProperty(KEY, "true").trim());
		} catch (IOException | IllegalArgumentException exception) {
			HxHMod.LOGGER.error("Nao deu para ler {}: a verificacao de conta continua ligada", file, exception);
			return true;
		}
	}
}
