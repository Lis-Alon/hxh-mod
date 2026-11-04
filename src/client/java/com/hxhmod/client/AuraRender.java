package com.hxhmod.client;

import com.hxhmod.HxHMod;
import com.hxhmod.nen.AuraShow;
import com.hxhmod.nen.ModAttachments;
import com.hxhmod.nen.NenData;
import com.hxhmod.nen.NenRules;
import com.hxhmod.nen.ShuCoat;
import com.hxhmod.nen.Technique;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.QuadInstance;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.renderpearl.api.pipeline.BlendFunction;
import com.mojang.renderpearl.api.pipeline.ColorTargetState;
import com.mojang.renderpearl.api.pipeline.CompareOp;
import com.mojang.renderpearl.api.pipeline.DepthStencilState;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityRenderLayerRegistrationCallback;
import net.fabricmc.fabric.api.client.rendering.v1.RenderStateDataKey;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.ArmedEntityRenderState;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.geometry.ItemQuads;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Desenha a aura. Nada aqui vem de particulas do jogo: as faixas sao curvas calculadas a cada quadro,
 * somadas a cena para brilhar. Niveis em AuraShow: suave, chamas e explosao; En e Shu vem por cima.
 * O In some com a aura para os outros (alfa pelo vazamento), menos para quem esta com o Gyo.
 */
public final class AuraRender {
	/** O que o jogador que esta olhando percebe da aura de um jogador. */
	public record View(AuraShow show, float visibility) {
	}

	public static final RenderStateDataKey<View> VIEW = RenderStateDataKey.create(() -> "hxh:aura_view");

	private static final int CYAN = 0x38B4FF;
	private static final int GOLD = 0xFFA41E;
	private static final int EN_FLAME = 0x58B8FF;
	private static final int COAT_COLOR = 0xFF1C8CD2;
	private static final float MIN_VISIBLE = 0.03F;
	private static final float MAX_DISTANCE = 64;
	private static final int FULL_BRIGHT = 0xF000F0;
	private static final int DOME_COLUMNS = 48;
	private static final int DOME_ROWS = 24;
	/** Tamanho em blocos de uma repeticao da textura do En: as manchas nao esticam com o raio. */
	private static final float DOME_TILE = 7F;

	private static final Identifier WISP_TEXTURE = texture("wisp");
	private static final Identifier FLAME_TEXTURE = texture("flame");
	private static final Identifier GLOW_TEXTURE = texture("glow");
	private static final Identifier MOTE_TEXTURE = texture("mote");
	private static final Identifier DOME_TEXTURE = texture("en_dome");

	private static final RenderPipeline GLOW_PIPELINE = RenderPipelines.register(RenderPipeline.builder(RenderPipelines.ENTITY_NO_LIGHTMAP_SNIPPET, RenderPipelines.EYES_SNIPPET)
			.withLocation(HxHMod.id("pipeline/aura_glow"))
			.withColorTargetState(new ColorTargetState(BlendFunction.LIGHTNING))
			.withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, false))
			.withCull(false)
			.build());
	private static final RenderPipeline GLASS_PIPELINE = RenderPipelines.register(RenderPipeline.builder(RenderPipelines.ENTITY_NO_LIGHTMAP_SNIPPET, RenderPipelines.EYES_SNIPPET)
			.withLocation(HxHMod.id("pipeline/aura_glass"))
			.withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
			.withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, false))
			.withCull(false)
			.build());

	private static final RenderType WISP = glowType("aura_wisp", WISP_TEXTURE);
	private static final RenderType GLOW = glowType("aura_halo", GLOW_TEXTURE);
	private static final RenderType FLAME = glowType("aura_flame", FLAME_TEXTURE);
	private static final RenderType MOTE = glowType("aura_mote", MOTE_TEXTURE);
	private static final RenderType COAT = glowType("aura_coat", TextureAtlas.LOCATION_ITEMS);
	private static final RenderType DOME = RenderType.create("aura_dome",
			RenderSetup.builder(GLASS_PIPELINE).withTexture("Sampler0", DOME_TEXTURE).createRenderSetup());

	/** Fica ligado so enquanto o objeto revestido esta sendo enviado para desenho. */
	private static View coating;
	private static float coatingTime;

	private AuraRender() {
	}

	private static Identifier texture(String name) {
		return HxHMod.id("textures/aura/" + name + ".png");
	}

	private static RenderType glowType(String name, Identifier texture) {
		return RenderType.create(name, RenderSetup.builder(GLOW_PIPELINE).withTexture("Sampler0", texture).createRenderSetup());
	}

	public static void register() {
		LevelRenderEvents.COLLECT_SUBMITS.register(AuraRender::world);
		LivingEntityRenderLayerRegistrationCallback.EVENT.register((entityType, renderer, helper, context) -> {
			if (renderer instanceof AvatarRenderer<?> avatar) {
				@SuppressWarnings("unchecked")
				RenderLayerParent<AvatarRenderState, PlayerModel> parent = (RenderLayerParent<AvatarRenderState, PlayerModel>) (Object) avatar;
				helper.register(new AuraLayer(parent));
			}
		});
	}

	// ---- O que cada jogador percebe ----

	/** A aura de um jogador como quem esta jogando a percebe, ou null se nao ha nada para ver. */
	private static View view(Player target) {
		AuraShow show = target.getAttached(ModAttachments.AURA_SHOW);
		if (show == null || show.level() == 0) {
			return null;
		}
		float visibility = seesAll(target) ? 1 : show.leak();
		return visibility < MIN_VISIBLE ? null : new View(show, visibility);
	}

	private static boolean seesAll(Player target) {
		LocalPlayer local = Minecraft.getInstance().player;
		if (local == null || local == target) {
			return true;
		}
		NenData own = local.getAttached(ModAttachments.NEN);
		return own != null && own.isActive(Technique.GYO)
				&& local.distanceTo(target) <= NenRules.GYO_REVEAL_RADIUS
				&& local.hasLineOfSight(target);
	}

	public static void extract(Avatar entity, AvatarRenderState state) {
		state.setData(VIEW, entity instanceof Player player ? view(player) : null);
	}

	private static float now() {
		Minecraft minecraft = Minecraft.getInstance();
		long ticks = minecraft.level == null ? 0 : minecraft.level.getGameTime() % 24000;
		return ticks + minecraft.getDeltaTracker().getGameTimeDeltaPartialTick(false);
	}

	private static View ownView() {
		LocalPlayer player = Minecraft.getInstance().player;
		return player == null ? null : view(player);
	}

	// ---- Mundo: corpo, En e particulas ----

	private static void world(LevelRenderContext context) {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.level == null) {
			return;
		}
		CameraRenderState camera = context.levelState().cameraRenderState;
		float partial = minecraft.getDeltaTracker().getGameTimeDeltaPartialTick(false);
		float time = now();
		PoseStack pose = context.poseStack();
		SubmitNodeCollector collector = context.submitNodeCollector();
		for (Player player : minecraft.level.players()) {
			View view = view(player);
			Vec3 position = player.getPosition(partial);
			Vec3 relative = position.subtract(camera.pos);
			if (view == null || relative.length() > MAX_DISTANCE + view.show().enRadius()) {
				continue;
			}
			float seed = (player.getId() * 0.37F) % 6.28F;
			pose.pushPose();
			pose.translate((float) relative.x, (float) relative.y, (float) relative.z);
			if (view.show().enRadius() > 0) {
				dome(pose, collector, view, relative, time, seed);
			}
			boolean isCamera = player == minecraft.player && camera.isFirstPerson;
			if (!isCamera && !player.isInvisible()) {
				body(pose, collector, view, camera, time, seed, player.getBbHeight());
			}
			motes(pose, collector, view, camera, time, seed, isCamera);
			pose.popPose();
		}
	}

	/** Em volta do corpo: veu suave no nivel basico, chamas altas e abertas nos niveis fortes. */
	private static void body(PoseStack pose, SubmitNodeCollector collector, View view, CameraRenderState camera, float time, float seed, float height) {
		int level = view.show().level();
		float alpha = view.visibility();
		if (level >= AuraShow.FLAMES) {
			Quaternionf facing = cameraRotation(camera);
			float size = level == AuraShow.BURST ? 3.3F : 2.6F;
			collector.submitCustomGeometry(pose, GLOW, (entry, buffer) -> {
				billboard(entry, buffer, facing, 0, height * 0.55F, 0, size, CYAN, 0.22F * alpha);
				billboard(entry, buffer, facing, 0, height * 0.42F, 0, size * 0.72F, GOLD, 0.2F * alpha);
			});
		}
		float tall = level == AuraShow.BURST ? 1.5F : level == AuraShow.FLAMES ? 1.22F : 1.04F;
		float radius = level == AuraShow.BURST ? 0.62F : level == AuraShow.FLAMES ? 0.52F : 0.42F;
		if (level >= AuraShow.FLAMES) {
			// Corpo da chama: lencois largos e macios; o dourado por dentro e o ciano por fora
			int sheets = level == AuraShow.BURST ? 20 : 12;
			float width = level == AuraShow.BURST ? 1.15F : 0.85F;
			float strength = level == AuraShow.BURST ? 0.3F : 0.26F;
			collector.submitCustomGeometry(pose, FLAME, (entry, buffer) ->
					strands(entry, buffer, sheets, height * tall, radius, width, strength * alpha, time, seed, 0.085F, 0.42F, 0.9F, 0.2F, true, 0));
		}
		int lines = level == AuraShow.BURST ? 16 : level == AuraShow.FLAMES ? 10 : 8;
		float lineWidth = level == AuraShow.SOFT ? 0.26F : 0.3F;
		float lineAlpha = (level == AuraShow.SOFT ? 0.6F : 0.62F) * alpha;
		float speed = level == AuraShow.SOFT ? 0.035F : 0.085F;
		float turns = level == AuraShow.SOFT ? 0.8F : 0.5F;
		float flare = level == AuraShow.SOFT ? 0.25F : 0.8F;
		float wobble = level == AuraShow.SOFT ? 0.1F : 0.2F;
		collector.submitCustomGeometry(pose, WISP, (entry, buffer) ->
				strands(entry, buffer, lines, height * tall, radius * 1.08F, lineWidth, lineAlpha, time, seed, speed, turns, flare, wobble, level != AuraShow.SOFT, 100));
	}

	private static void strands(PoseStack.Pose entry, VertexConsumer buffer, int count, float length, float radius, float width, float alpha,
			float time, float seed, float speed, float turns, float flare, float wobble, boolean taper, int salt) {
		for (int i = 0; i < count; i++) {
			float shift = hash(i, 7 + salt);
			boolean gold = i % 5 >= 3;
			Ribbon ribbon = new Ribbon();
			ribbon.start.set(0, taper ? -0.02F : 0.05F, 0);
			ribbon.axis.set(0, 1, 0);
			ribbon.side.set(1, 0, 0);
			ribbon.length = length * (gold ? 0.55F + 0.3F * hash(i, 3 + salt) : 0.68F + 0.42F * hash(i, 3 + salt));
			ribbon.radius = radius * (0.75F + 0.45F * hash(i, 5 + salt)) * (gold ? 0.72F : 1);
			ribbon.flare = flare;
			ribbon.turns = turns * (i % 2 == 0 ? 1 : -1);
			ribbon.phase = seed + shift * Mth.TWO_PI + time * speed * (i % 2 == 0 ? 1 : -1);
			ribbon.width = width * (0.7F + 0.5F * hash(i, 11 + salt));
			ribbon.color = gold ? GOLD : CYAN;
			ribbon.alpha = alpha;
			ribbon.wobble = wobble;
			ribbon.time = time + shift * 40;
			ribbon.taper = taper;
			ribbon.flow = speed * 1.2F;
			ribbon.emit(entry, buffer);
		}
	}

	/** Quadradinhos de luz subindo em volta do jogador. */
	private static void motes(PoseStack pose, SubmitNodeCollector collector, View view, CameraRenderState camera, float time, float seed, boolean isCamera) {
		int level = view.show().level();
		int count = level == AuraShow.BURST ? 44 : level == AuraShow.FLAMES ? 26 : 16;
		float reach = level == AuraShow.BURST ? 1.5F : 1.1F;
		float alpha = view.visibility();
		Quaternionf facing = cameraRotation(camera);
		collector.submitCustomGeometry(pose, MOTE, (entry, buffer) -> {
			for (int i = 0; i < count; i++) {
				float period = 50 + 60 * hash(i, 13);
				float life = (time / period + hash(i, 17)) % 1;
				float angle = seed + hash(i, 19) * Mth.TWO_PI + life * 1.6F;
				// Na primeira pessoa ficam longe dos olhos: de perto um quadradinho tomaria a tela
				float away = isCamera ? 1.4F + reach * 1.3F * hash(i, 23) : 0.35F + reach * hash(i, 23);
				float x = Mth.cos(angle) * away;
				float z = Mth.sin(angle) * away;
				float y = hash(i, 29) * 0.8F + life * 2.3F;
				float size = (0.07F + 0.09F * hash(i, 31)) * (isCamera ? 1.3F : 2.2F);
				float fade = Mth.sin(life * Mth.PI);
				billboard(entry, buffer, facing, x, y, z, size, i % 3 == 0 ? CYAN : GOLD, fade * alpha);
			}
		});
	}

	private static Quaternionf cameraRotation(CameraRenderState camera) {
		return new Quaternionf().rotationYXZ(Mth.PI - camera.yRot * Mth.DEG_TO_RAD, -camera.xRot * Mth.DEG_TO_RAD, 0);
	}

	/** Esfera do En centrada nos pes: a metade de baixo fica sob o chao e sobra a cupula. */
	private static void dome(PoseStack pose, SubmitNodeCollector collector, View view, Vec3 relative, float time, float seed) {
		float radius = view.show().enRadius();
		float visibility = view.visibility();
		// Da camera ate o centro da esfera, para o brilho da borda; por dentro fica mais transparente
		Vector3f eye = new Vector3f((float) -relative.x, (float) -relative.y, (float) -relative.z);
		float inside = eye.length() < radius ? 0.5F : 1;
		float repeatU = Math.max(1, Math.round(Mth.TWO_PI * radius / DOME_TILE));
		// A textura e duas vezes mais larga que alta
		float repeatV = Math.max(1, Math.round(Mth.TWO_PI * radius / DOME_TILE));
		float scroll = time * 0.0009F;
		collector.submitCustomGeometry(pose, DOME, (entry, buffer) -> {
			for (int row = 0; row < DOME_ROWS; row++) {
				for (int column = 0; column < DOME_COLUMNS; column++) {
					domeVertex(entry, buffer, column, row, radius, eye, repeatU, repeatV, scroll, inside * visibility);
					domeVertex(entry, buffer, column, row + 1, radius, eye, repeatU, repeatV, scroll, inside * visibility);
					domeVertex(entry, buffer, column + 1, row + 1, radius, eye, repeatU, repeatV, scroll, inside * visibility);
					domeVertex(entry, buffer, column + 1, row, radius, eye, repeatU, repeatV, scroll, inside * visibility);
				}
			}
		});
		// Labaredas azuis subindo do chao, junto da borda
		int flames = Math.max(8, Math.round(radius * 3.5F));
		collector.submitCustomGeometry(pose, WISP, (entry, buffer) -> {
			for (int i = 0; i < flames; i++) {
				float angle = seed + hash(i, 37) * Mth.TWO_PI;
				float ring = radius * (0.7F + 0.26F * hash(i, 41));
				Ribbon ribbon = new Ribbon();
				ribbon.start.set(Mth.cos(angle) * ring, -0.1F, Mth.sin(angle) * ring);
				ribbon.axis.set(0, 1, 0);
				ribbon.side.set(-Mth.sin(angle), 0, Mth.cos(angle));
				ribbon.length = 0.9F + Math.min(2.2F, radius * 0.3F) * hash(i, 43);
				ribbon.radius = 0.12F;
				ribbon.flare = 0.5F;
				ribbon.turns = 0.3F;
				ribbon.phase = hash(i, 47) * Mth.TWO_PI + time * 0.05F;
				ribbon.width = 0.5F;
				ribbon.color = EN_FLAME;
				ribbon.alpha = 0.55F * visibility * inside;
				ribbon.wobble = 0.15F;
				ribbon.time = time + hash(i, 53) * 40;
				ribbon.taper = true;
				ribbon.flow = 0.05F;
				ribbon.emit(entry, buffer);
			}
		});
	}

	private static void domeVertex(PoseStack.Pose entry, VertexConsumer buffer, int column, int row, float radius, Vector3f eye,
			float repeatU, float repeatV, float scroll, float alpha) {
		float u = (float) column / DOME_COLUMNS;
		float v = (float) row / DOME_ROWS;
		float theta = u * Mth.TWO_PI;
		float phi = v * Mth.PI;
		float nx = Mth.sin(phi) * Mth.cos(theta);
		float ny = Mth.cos(phi);
		float nz = Mth.sin(phi) * Mth.sin(theta);
		float x = nx * radius;
		float y = ny * radius;
		float z = nz * radius;
		float toEyeX = eye.x - x;
		float toEyeY = eye.y - y;
		float toEyeZ = eye.z - z;
		float distance = Math.max(0.001F, Mth.sqrt(toEyeX * toEyeX + toEyeY * toEyeY + toEyeZ * toEyeZ));
		float facing = Math.abs(nx * toEyeX + ny * toEyeY + nz * toEyeZ) / distance;
		float rim = 1 - facing;
		float shade = 0.42F + 0.58F * rim * rim;
		vertex(entry, buffer, x, y, z, 0xFFFFFF, Math.min(1, shade * alpha), u * repeatU + scroll, v * repeatV);
	}

	// ---- Bracos ----

	/**
	 * Faixas em volta de um braco. A pose ja esta no osso do braco, com o eixo y descendo pelo braco.
	 *
	 * @param side 1 para o direito, -1 para o esquerdo
	 */
	public static void arm(PoseStack pose, SubmitNodeCollector collector, View view, float time, int side) {
		int level = view.show().level();
		int strands = level == AuraShow.BURST ? 9 : level == AuraShow.FLAMES ? 7 : 5;
		float alpha = view.visibility() * (level == AuraShow.SOFT ? 0.55F : 0.68F);
		float center = -side / 16F;
		collector.submitCustomGeometry(pose, WISP, (entry, buffer) -> {
			for (int i = 0; i < strands; i++) {
				float shift = hash(i, 59 + side);
				Ribbon ribbon = new Ribbon();
				ribbon.start.set(center, -0.2F, 0);
				ribbon.axis.set(0, 1, 0);
				ribbon.side.set(1, 0, 0);
				ribbon.length = 0.95F;
				ribbon.radius = 0.17F + 0.11F * hash(i, 61);
				ribbon.flare = 0.3F;
				ribbon.turns = (0.9F + 0.7F * hash(i, 67)) * (i % 2 == 0 ? 1 : -1);
				ribbon.phase = shift * Mth.TWO_PI + time * 0.06F * (i % 2 == 0 ? 1 : -1);
				ribbon.width = level == AuraShow.SOFT ? 0.17F : 0.25F;
				ribbon.color = i % 5 < 3 ? CYAN : GOLD;
				ribbon.alpha = alpha;
				ribbon.wobble = 0.05F;
				ribbon.time = time + shift * 30;
				ribbon.taper = false;
				ribbon.flow = 0.05F;
				ribbon.emit(entry, buffer);
			}
		});
		collector.submitCustomGeometry(pose, MOTE, (entry, buffer) -> {
			Quaternionf none = new Quaternionf();
			for (int i = 0; i < 6; i++) {
				float life = (time / (40 + 30 * hash(i, 71)) + hash(i, 73)) % 1;
				float angle = hash(i, 79) * Mth.TWO_PI + life * 3;
				float out = 0.2F + 0.22F * hash(i, 83);
				billboard(entry, buffer, none, center + Mth.cos(angle) * out, 0.65F - life * 0.95F, Mth.sin(angle) * out,
						0.1F + 0.07F * hash(i, 89), i % 3 == 0 ? CYAN : GOLD, Mth.sin(life * Mth.PI) * view.visibility());
			}
		});
	}

	public static void firstPersonArm(PoseStack pose, SubmitNodeCollector collector, int side) {
		View view = ownView();
		if (view == null) {
			return;
		}
		pose.pushPose();
		// Mesma posicao do osso do braco no modelo do jogador
		pose.translate(-side * 5 / 16F, 2 / 16F, 0);
		arm(pose, collector, view, now(), side);
		pose.popPose();
	}

	/** Com a aura ligada a mao aparece junto do objeto, menos quando o braco atrapalharia (mapa, besta, item em uso). */
	public static boolean showsBareArm(ItemStack stack, boolean usingThisHand) {
		return ownView() != null && !usingThisHand && !stack.has(DataComponents.MAP_ID) && !stack.is(Items.CROSSBOW);
	}

	// ---- Shu: objeto revestido ----

	/** Primeira pessoa. */
	public static void coatHeld(boolean mainHand, ItemStack stack) {
		View view = ownView();
		boolean shieldInOffhand = !mainHand && stack.is(Items.SHIELD);
		coating = view != null && view.show().shu() && !stack.isEmpty() && (mainHand || shieldInOffhand) ? view : null;
		coatingTime = now();
	}

	/** Terceira pessoa. */
	public static void coatHeld(ArmedEntityRenderState state, HumanoidArm arm, ItemStack stack) {
		View view = state instanceof AvatarRenderState avatar ? avatar.getData(VIEW) : null;
		boolean held = arm == state.mainArm || stack.is(Items.SHIELD);
		coating = view != null && view.show().shu() && held ? view : null;
		coatingTime = state.ageInTicks;
	}

	public static void endCoat() {
		coating = null;
	}

	/** Chamado na pose do modelo do objeto, que ocupa o cubo de 0 a 1. */
	public static void coat(PoseStack pose, SubmitNodeCollector collector, ItemQuads quads) {
		View view = coating;
		if (view == null) {
			return;
		}
		float time = coatingTime;
		float visibility = view.visibility();
		if (!quads.isEmpty()) {
			collector.submitCustomGeometry(pose, COAT, (entry, buffer) -> {
				QuadInstance instance = new QuadInstance();
				instance.setColor(Mth.ceil(visibility * 255) << 24 | COAT_COLOR & 0xFFFFFF);
				instance.setLightCoords(FULL_BRIGHT);
				instance.setOverlayCoords(OverlayTexture.NO_OVERLAY);
				for (BakedQuad quad : quads.all()) {
					if (TextureAtlas.LOCATION_ITEMS.equals(quad.materialInfo().sprite().atlasLocation())) {
						buffer.putBakedQuad(entry, quad, instance);
					}
				}
			});
		}
		collector.submitCustomGeometry(pose, WISP, (entry, buffer) -> {
			for (int i = 0; i < 7; i++) {
				float shift = hash(i, 97);
				Ribbon ribbon = new Ribbon();
				// Da empunhadura ate a ponta, na diagonal em que as armas sao desenhadas
				ribbon.start.set(0.08F, 0.08F, 0.5F);
				ribbon.axis.set(0.7071F, 0.7071F, 0);
				ribbon.side.set(0, 0, 1);
				ribbon.length = 1.45F;
				ribbon.radius = 0.13F + 0.11F * hash(i, 101);
				ribbon.flare = 0.5F;
				ribbon.turns = (0.8F + 0.5F * hash(i, 103)) * (i % 2 == 0 ? 1 : -1);
				ribbon.phase = shift * Mth.TWO_PI + time * 0.07F * (i % 2 == 0 ? 1 : -1);
				ribbon.width = 0.19F;
				ribbon.color = i % 5 < 3 ? CYAN : GOLD;
				ribbon.alpha = 0.85F * visibility;
				ribbon.wobble = 0.05F;
				ribbon.time = time + shift * 30;
				ribbon.taper = true;
				ribbon.flow = 0.06F;
				ribbon.emit(entry, buffer);
			}
		});
	}

	// ---- Geometria ----

	/**
	 * Faixa que sobe por um eixo girando em volta dele, deitada sobre o cilindro.
	 * Os campos ficam abertos: cada efeito acima ajusta so os que importam para ele.
	 */
	private static final class Ribbon {
		private static final int SEGMENTS = 22;

		final Vector3f start = new Vector3f();
		final Vector3f axis = new Vector3f(0, 1, 0);
		final Vector3f side = new Vector3f(1, 0, 0);
		float length;
		float radius;
		/** Quanto a faixa abre no meio da altura, em fracao do raio. */
		float flare;
		float turns;
		float phase;
		float width;
		int color;
		float alpha;
		float wobble;
		float time;
		/** Chama: larga na base e terminando em ponta. Sem isso, as duas pontas afinam igual. */
		boolean taper;
		float flow;

		void emit(PoseStack.Pose entry, VertexConsumer buffer) {
			Vector3f other = new Vector3f(axis).cross(side);
			float[] previous = point(0, other);
			float scrolled = time * flow;
			for (int i = 1; i <= SEGMENTS; i++) {
				float[] next = point((float) i / SEGMENTS, other);
				vertex(entry, buffer, previous[0], previous[1], previous[2], color, previous[6], previous[7] - scrolled, 0);
				vertex(entry, buffer, previous[3], previous[4], previous[5], color, previous[6], previous[7] - scrolled, 1);
				vertex(entry, buffer, next[3], next[4], next[5], color, next[6], next[7] - scrolled, 1);
				vertex(entry, buffer, next[0], next[1], next[2], color, next[6], next[7] - scrolled, 0);
				previous = next;
			}
		}

		/** As duas bordas da faixa neste ponto, alfa e coordenada da textura. */
		private float[] point(float s, Vector3f other) {
			float wave = Mth.sin(s * 5.5F + time * 0.11F + phase) * wobble;
			float r = radius * (1 + flare * Mth.sin(s * Mth.PI)) + wave;
			if (taper) {
				// Pontas das chamas voltam para o eixo
				r *= 1 - 0.55F * s * s * s;
			}
			float angle = phase + turns * Mth.TWO_PI * s + Mth.sin(s * 3.1F + time * 0.07F) * 0.5F;
			float cos = Mth.cos(angle);
			float sin = Mth.sin(angle);
			float radialX = side.x * cos + other.x * sin;
			float radialY = side.y * cos + other.y * sin;
			float radialZ = side.z * cos + other.z * sin;
			float along = s * length;
			float cx = start.x + axis.x * along + radialX * r;
			float cy = start.y + axis.y * along + radialY * r;
			float cz = start.z + axis.z * along + radialZ * r;

			// Largura: perpendicular ao caminho, sobre a superficie do cilindro
			float tangentScale = r * turns * Mth.TWO_PI;
			float tx = axis.x * length + (-side.x * sin + other.x * cos) * tangentScale;
			float ty = axis.y * length + (-side.y * sin + other.y * cos) * tangentScale;
			float tz = axis.z * length + (-side.z * sin + other.z * cos) * tangentScale;
			float wx = ty * radialZ - tz * radialY;
			float wy = tz * radialX - tx * radialZ;
			float wz = tx * radialY - ty * radialX;
			float size = Math.max(0.0001F, Mth.sqrt(wx * wx + wy * wy + wz * wz));
			float shape = taper
					? Mth.clamp(s * 7, 0, 1) * (float) Math.pow(1 - s, 0.7)
					: (float) Math.pow(Mth.sin(s * Mth.PI), 0.7);
			float half = width * shape / 2 / size;
			float fade = alpha * Mth.clamp(s * 6, 0, 1) * Mth.clamp((1 - s) * 4, 0, 1);
			return new float[]{cx - wx * half, cy - wy * half, cz - wz * half, cx + wx * half, cy + wy * half, cz + wz * half,
					fade, s * length * 0.9F + phase};
		}
	}

	/** Quadrado virado para a camera (na pose do mundo) ou para a rotacao pedida. */
	private static void billboard(PoseStack.Pose entry, VertexConsumer buffer, Quaternionf rotation, float x, float y, float z, float size, int color, float alpha) {
		Vector3f right = rotation.transform(new Vector3f(size / 2, 0, 0));
		Vector3f up = rotation.transform(new Vector3f(0, size / 2, 0));
		vertex(entry, buffer, x - right.x - up.x, y - right.y - up.y, z - right.z - up.z, color, alpha, 0, 1);
		vertex(entry, buffer, x + right.x - up.x, y + right.y - up.y, z + right.z - up.z, color, alpha, 1, 1);
		vertex(entry, buffer, x + right.x + up.x, y + right.y + up.y, z + right.z + up.z, color, alpha, 1, 0);
		vertex(entry, buffer, x - right.x + up.x, y - right.y + up.y, z - right.z + up.z, color, alpha, 0, 0);
	}

	private static void vertex(PoseStack.Pose entry, VertexConsumer buffer, float x, float y, float z, int color, float alpha, float u, float v) {
		buffer.addVertex(entry, x, y, z)
				.setColor((Mth.clamp((int) (alpha * 255), 0, 255) << 24) | (color & 0xFFFFFF))
				.setUv(u, v)
				.setOverlay(OverlayTexture.NO_OVERLAY)
				.setLight(FULL_BRIGHT)
				.setNormal(entry, 0, 1, 0);
	}

	/** Numero fixo entre 0 e 1 para cada faixa e cada propriedade: a aura muda sem precisar guardar estado. */
	private static float hash(int index, int salt) {
		float value = Mth.sin(index * 12.9898F + salt * 78.233F) * 43758.547F;
		return value - Mth.floor(value);
	}
}
