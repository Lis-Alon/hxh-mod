package com.hxhmod.nen;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.UnaryOperator;

/**
 * Comando /nen. Sem argumentos mostra o status; o resto e para controlar o proprio Nen
 * (precisa de permissao de operador, ou "Permitir comandos" no mundo).
 */
final class NenCommand {
	private static final String ALL = "all";

	private static final SuggestionProvider<CommandSourceStack> SKILL_NAMES =
			(context, builder) -> SharedSuggestionProvider.suggest(skillNames(), builder);
	private static final SuggestionProvider<CommandSourceStack> TYPE_NAMES =
			(context, builder) -> SharedSuggestionProvider.suggest(
					Arrays.stream(NenType.values()).filter(type -> type != NenType.NONE).map(NenType::getSerializedName),
					builder);

	private NenCommand() {
	}

	static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("nen")
				.executes(NenCommand::status)
				.then(Commands.literal("reset")
						.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
						.executes(context -> {
							ServerPlayer player = context.getSource().getPlayerOrException();
							player.setAttached(ModAttachments.NEN, NenData.DEFAULT);
							context.getSource().sendSuccess(() -> Component.translatable("nen.hxh.reset"), false);
							return 1;
						}))
				// /nen awaken [tipo]: desperta na hora (e troca o tipo, se informado)
				.then(Commands.literal("awaken")
						.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
						.executes(context -> awaken(context, null))
						.then(Commands.argument("type", StringArgumentType.word())
								.suggests(TYPE_NAMES)
								.executes(context -> awaken(context, StringArgumentType.getString(context, "type")))))
				// /nen reveal: revela o tipo sem precisar do copo
				.then(Commands.literal("reveal")
						.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
						.executes(context -> {
							ServerPlayer player = context.getSource().getPlayerOrException();
							NenData data = awakenedData(context);
							if (data == null) {
								return 0;
							}
							player.setAttached(ModAttachments.NEN, data.withDivination(100));
							player.sendSystemMessage(data.type().waterReaction());
							player.sendSystemMessage(Component.translatable("nen.hxh.revealed", data.type().displayName()));
							return 1;
						}))
				.then(Commands.literal("xp")
						.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
						.then(Commands.argument("amount", IntegerArgumentType.integer(1))
								.executes(context -> {
									int amount = IntegerArgumentType.getInteger(context, "amount");
									return change(context, data -> data.addXp(amount), null);
								})))
				// /nen level <1-100>: coloca no comeco do nivel informado
				.then(Commands.literal("level")
						.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
						.then(Commands.argument("level", IntegerArgumentType.integer(1, NenRules.MAX_LEVEL))
								.executes(context -> {
									int level = IntegerArgumentType.getInteger(context, "level");
									return change(context, data -> data.withXp(NenData.xpForLevel(level)),
											Component.translatable("nen.hxh.cmd.level", level));
								})))
				// /nen unlock <tecnica|funcionalidade|all> e /nen lock <...>: libera ou bloqueia quando quiser
				.then(Commands.literal("unlock")
						.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
						.then(Commands.argument("skill", StringArgumentType.word())
								.suggests(SKILL_NAMES)
								.executes(context -> unlock(context, StringArgumentType.getString(context, "skill")))))
				.then(Commands.literal("lock")
						.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
						.then(Commands.argument("skill", StringArgumentType.word())
								.suggests(SKILL_NAMES)
								.executes(context -> lock(context, StringArgumentType.getString(context, "skill")))))
				// /nen points add <n>: pontos extras para gastar no painel (negativo tira)
				.then(Commands.literal("points")
						.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
						.then(Commands.literal("add")
								.then(Commands.argument("amount", IntegerArgumentType.integer())
										.executes(context -> {
											int amount = IntegerArgumentType.getInteger(context, "amount");
											NenData data = awakenedData(context);
											if (data == null) {
												return 0;
											}
											NenData result = data.withBonusPoints(data.bonusPoints() + amount);
											return change(context, unused -> result,
													Component.translatable("nen.hxh.cmd.points", amount, result.pointsAvailable()));
										}))))
				// /nen stats reset: devolve os pontos dos atributos
				.then(Commands.literal("stats")
						.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
						.then(Commands.literal("reset")
								.executes(context -> {
									NenData data = awakenedData(context);
									if (data == null) {
										return 0;
									}
									NenData result = data.clearStats();
									return change(context, unused -> result,
											Component.translatable("nen.hxh.cmd.stats_reset", result.pointsAvailable()));
								})))
				// /nen aura: enche a aura
				.then(Commands.literal("aura")
						.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
						.executes(context -> {
							NenData data = awakenedData(context);
							if (data == null) {
								return 0;
							}
							return change(context, unused -> data.withAura(data.maxAura()),
									Component.translatable("nen.hxh.cmd.aura", data.maxAura(), data.maxAura()));
						})));
	}

	private static int status(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		ServerPlayer player = context.getSource().getPlayerOrException();
		NenData data = player.getAttachedOrCreate(ModAttachments.NEN);
		if (!data.awakened()) {
			context.getSource().sendSuccess(() -> Component.translatable("nen.hxh.not_awakened"), false);
		} else {
			Component type = data.revealed() ? data.type().displayName() : Component.literal("???");
			context.getSource().sendSuccess(() -> Component.translatable("nen.hxh.status",
					type, data.level(), data.pointsAvailable(), data.aura(), data.maxAura()), false);
		}
		return 1;
	}

	private static int awaken(CommandContext<CommandSourceStack> context, String typeName) throws CommandSyntaxException {
		ServerPlayer player = context.getSource().getPlayerOrException();
		NenType type = null;
		if (typeName != null) {
			type = Arrays.stream(NenType.values())
					.filter(candidate -> candidate != NenType.NONE && candidate.getSerializedName().equals(typeName))
					.findFirst().orElse(null);
			if (type == null) {
				context.getSource().sendFailure(Component.translatable("nen.hxh.cmd.unknown", typeName));
				return 0;
			}
		}
		NenData data = player.getAttachedOrCreate(ModAttachments.NEN);
		NenType chosen = type != null ? type : NenType.roll(player.getRandom());
		NenData result = data.awakened()
				? data.withType(type != null ? type : data.type())
				: data.withMeditation(NenRules.MEDITATION_SECONDS).awaken(chosen);
		player.setAttached(ModAttachments.NEN, result);
		context.getSource().sendSuccess(() -> data.awakened()
				? Component.translatable("nen.hxh.cmd.type_changed", result.type().displayName())
				: Component.translatable("nen.hxh.awakened"), false);
		return 1;
	}

	private static int unlock(CommandContext<CommandSourceStack> context, String name) throws CommandSyntaxException {
		NenData data = awakenedData(context);
		if (data == null) {
			return 0;
		}
		if (name.equals(ALL)) {
			NenData result = data;
			for (Technique technique : Technique.TABS) {
				result = result.grant(technique);
			}
			for (NenUpgrade upgrade : NenUpgrade.values()) {
				result = result.grant(upgrade);
			}
			NenData finalResult = result;
			return change(context, unused -> finalResult, Component.translatable("nen.hxh.cmd.unlocked_all"));
		}
		Technique technique = technique(name);
		if (technique != null) {
			// Libera junto o que vem antes, para a arvore continuar fazendo sentido
			NenData result = grantWithPrerequisites(data, technique);
			return change(context, unused -> result, Component.translatable("nen.hxh.cmd.unlocked", technique.displayName()));
		}
		NenUpgrade upgrade = upgrade(name);
		if (upgrade != null) {
			NenData result = grantWithPrerequisites(data, upgrade.technique()).grant(upgrade);
			return change(context, unused -> result, Component.translatable("nen.hxh.cmd.unlocked", upgrade.displayName()));
		}
		context.getSource().sendFailure(Component.translatable("nen.hxh.cmd.unknown", name));
		return 0;
	}

	private static NenData grantWithPrerequisites(NenData data, Technique technique) {
		NenData result = data;
		for (Technique prerequisite : technique.prerequisites()) {
			result = grantWithPrerequisites(result, prerequisite);
		}
		return result.grant(technique);
	}

	private static int lock(CommandContext<CommandSourceStack> context, String name) throws CommandSyntaxException {
		NenData data = awakenedData(context);
		if (data == null) {
			return 0;
		}
		if (name.equals(ALL)) {
			NenData result = data;
			for (Technique technique : Technique.TABS) {
				result = result.lock(technique);
			}
			NenData finalResult = result;
			return change(context, unused -> finalResult, Component.translatable("nen.hxh.cmd.locked_all"));
		}
		Technique technique = technique(name);
		if (technique != null) {
			return change(context, current -> current.lock(technique),
					Component.translatable("nen.hxh.cmd.locked", technique.displayName()));
		}
		NenUpgrade upgrade = upgrade(name);
		if (upgrade != null) {
			return change(context, current -> current.lock(upgrade),
					Component.translatable("nen.hxh.cmd.locked", upgrade.displayName()));
		}
		context.getSource().sendFailure(Component.translatable("nen.hxh.cmd.unknown", name));
		return 0;
	}

	/** Os dados do jogador, ou null (com aviso) se o Nen ainda nao despertou. */
	private static NenData awakenedData(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		NenData data = context.getSource().getPlayerOrException().getAttachedOrCreate(ModAttachments.NEN);
		if (!data.awakened()) {
			context.getSource().sendFailure(Component.translatable("nen.hxh.cmd.need_awakening"));
			return null;
		}
		return data;
	}

	private static int change(CommandContext<CommandSourceStack> context, UnaryOperator<NenData> change, Component message)
			throws CommandSyntaxException {
		ServerPlayer player = context.getSource().getPlayerOrException();
		NenData before = player.getAttachedOrCreate(ModAttachments.NEN);
		NenData after = change.apply(before);
		player.setAttached(ModAttachments.NEN, after);
		NenEvents.announceLevelUp(player, before, after);
		if (message != null) {
			context.getSource().sendSuccess(() -> message, false);
		}
		return 1;
	}

	private static Technique technique(String name) {
		return Technique.TABS.stream().filter(technique -> technique.getSerializedName().equals(name)).findFirst().orElse(null);
	}

	private static NenUpgrade upgrade(String name) {
		return Arrays.stream(NenUpgrade.values()).filter(upgrade -> upgrade.getSerializedName().equals(name))
				.findFirst().orElse(null);
	}

	private static List<String> skillNames() {
		List<String> names = new ArrayList<>();
		names.add(ALL);
		Technique.TABS.forEach(technique -> names.add(technique.getSerializedName()));
		Arrays.stream(NenUpgrade.values()).forEach(upgrade -> names.add(upgrade.getSerializedName()));
		return names;
	}
}
