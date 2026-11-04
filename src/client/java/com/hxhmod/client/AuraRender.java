package com.hxhmod.client;

import com.hxhmod.HxHMod;
import com.hxhmod.nen.AuraColors;
import com.hxhmod.nen.AuraShow;
import com.hxhmod.nen.ModAttachments;
import com.hxhmod.nen.NenData;
import com.hxhmod.nen.NenRules;
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
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.model.player.PlayerModel;
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
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Desenha a aura como uma chama lenta em volta do que a tecnica reforca: Ten e Ren no corpo, Shu no objeto,
 * Ko no punho, Gyo nos olhos, En na esfera, e o Ryu divide a densidade entre corpo e bracos.
 * O dominio da tecnica muda tudo: pouco dominio da uma chama fina e instavel, muito da volume e brilho.
 * O In some com a aura para os outros (alfa pelo vazamento), menos para quem esta com o Gyo.
 */
public final class AuraRender {
	/**
	 * O que o jogador que esta olhando percebe da aura de um jogador.
	 *
	 * @param preview desenho da aba Aura: o corpo sai junto do modelo, porque ali nao ha mundo em volta
	 */
	public record View(AuraShow show, float visibility, int primary, int secondary, boolean preview) {
	}

	public static final RenderStateDataKey<View> VIEW = RenderStateDataKey.create(() -> "hxh:aura_view");

	private static final AuraShow PREVIEW_SHOW = new AuraShow(AuraShow.FLAMES, 30, -1, -1, -1, 0, 0, 0.5F, 1);
	/** Altura e posicao dos pes no espaco do modelo do jogador. */
	private static final float MODEL_HEIGHT = 1.8F;
	private static final float MODEL_FEET = 1.501F;
	private static final float COAT_STRENGTH = 0.75F;
	private static final float SHINE = 0.5F;
	private static final float DOME_WHITE_MIX = 0.3F;
	private static final int WHITE = 0xFFFFFF;
	private static final float MIN_VISIBLE = 0.03F;
	private static final float MAX_DISTANCE = 64;
	private static final int FULL_BRIGHT = 0xF000F0;
	private static final int DOME_COLUMNS = 48;
	private static final int DOME_ROWS = 24;
	/** Tamanho em blocos de uma repeticao da textura do En: as manchas nao esticam com o raio. */
	private static final float DOME_TILE = 7F;
	private static final float LOW_MASTERY = 0.25F;
	private static final float HIGH_MASTERY = 0.75F;
	private static final float SLOW = 0.028F;
	private static final int VIGNETTE_FRAMES = 3;
	private static final int VIGNETTE_WIDTH = 640;
	private static final int VIGNETTE_HEIGHT = 360;
	private static final float VIGNETTE_FRAME_TICKS = 70;

	private static final Identifier WISP_TEXTURE = texture("wisp");
	private static final Identifier RIM_TEXTURE = texture("rim");
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

	// Tudo tem duas passagens: a que mistura, com a cor certa mesmo contra o ceu claro, e a que soma, de leve, para brilhar
	private static final RenderType RIM = solidColorType("aura_rim", RIM_TEXTURE);
	private static final RenderType RIM_SHINE = glowType("aura_rim_shine", RIM_TEXTURE);
	private static final RenderType WISP = solidColorType("aura_wisp", WISP_TEXTURE);
	private static final RenderType WISP_SHINE = glowType("aura_wisp_shine", WISP_TEXTURE);
	private static final RenderType GLOW = glowType("aura_halo", GLOW_TEXTURE);
	private static final RenderType MOTE = solidColorType("aura_mote", MOTE_TEXTURE);
	private static final RenderType MOTE_SHINE = glowType("aura_mote_shine", MOTE_TEXTURE);
	private static final RenderType COAT = glowType("aura_coat", TextureAtlas.LOCATION_ITEMS);
	private static final RenderType DOME = solidColorType("aura_dome", DOME_TEXTURE);

	/** Fica ligado so enquanto o objeto segurado esta sendo enviado para desenho. */
	private static View coating;
	private static float coatingTime;
	/** Ligado enquanto a aba Aura pede o estado de desenho do personagem. */
	private static View previewing;

	private AuraRender() {
	}

	private static Identifier texture(String name) {
		return HxHMod.id("textures/aura/" + name + ".png");
	}

	private static RenderType glowType(String name, Identifier texture) {
		return RenderType.create(name, RenderSetup.builder(GLOW_PIPELINE).withTexture("Sampler0", texture).createRenderSetup());
	}

	private static RenderType solidColorType(String name, Identifier texture) {
		return RenderType.create(name, RenderSetup.builder(GLASS_PIPELINE).withTexture("Sampler0", texture).createRenderSetup());
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
		if (show == null || !show.any()) {
			return null;
		}
		float visibility = seesAll(target) ? 1 : show.leak();
		if (visibility < MIN_VISIBLE) {
			return null;
		}
		AuraColors colors = target.getAttachedOrElse(ModAttachments.AURA_COLORS, AuraColors.DEFAULT);
		return new View(show, visibility, colors.primary(), colors.secondary(), false);
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
		boolean preview = previewing != null && entity == Minecraft.getInstance().player;
		state.setData(VIEW, preview ? previewing : entity instanceof Player player ? view(player) : null);
	}

	/** O Ko estende o braco principal para a frente, com a aura concentrada no punho. */
	public static void poseArms(PlayerModel model, AvatarRenderState state) {
		View view = state.getData(VIEW);
		if (view != null && view.show().ko() && !state.isUsingItem) {
			var arm = state.mainArm == HumanoidArm.RIGHT ? model.rightArm : model.leftArm;
			arm.xRot = -Mth.HALF_PI - 0.1F + state.xRot * Mth.DEG_TO_RAD;
			arm.yRot = 0;
			arm.zRot = 0;
		}
	}

	public static void beginPreview(AuraColors colors) {
		previewing = new View(PREVIEW_SHOW, 1, colors.primary(), colors.secondary(), true);
	}

	public static void endPreview() {
		previewing = null;
	}

	/** Corpo e particulas na previa. A pose vem no espaco do modelo, com y para baixo a partir do quadril. */
	public static void previewBody(PoseStack pose, SubmitNodeCollector collector, View view, float time) {
		pose.pushPose();
		pose.translate(0, MODEL_FEET, 0);
		pose.scale(1, -1, 1);
		body(pose, collector, view, time, 0, MODEL_HEIGHT, 0.8F);
		motes(pose, collector, view, new Quaternionf(), time, 0, false);
		pose.popPose();
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

	/** Densidade da aura no corpo, proporcional a parte da defesa; so foge de 1 com o Ryu. */
	private static float bodyShare(AuraShow show) {
		return density((1 - show.offense()) * 2);
	}

	/** Densidade nos bracos e no objeto, proporcional a parte do ataque. */
	private static float armShare(AuraShow show) {
		return density(show.offense() * 2);
	}

	private static float density(float share) {
		return Mth.lerp(Mth.clamp((share - 0.6F) / 0.8F, 0, 1), 0.55F, 1.45F);
	}

	/** Dominio em tres faixas: 0 fina e instavel, 1 densa e constante, 2 volumosa. */
	private static int tier(float mastery) {
		return mastery <= LOW_MASTERY ? 0 : mastery <= HIGH_MASTERY ? 1 : 2;
	}

	/** Oscilacao de alfa de quem ainda nao controla a tecnica: some ao passar da primeira faixa. */
	private static float flicker(float mastery) {
		return mastery <= LOW_MASTERY ? 0.75F * (1 - mastery / LOW_MASTERY * 0.6F) : 0;
	}

	/** Largura da aura: cresce sem parar com o dominio, ate o triplo da de quem esta comecando. */
	private static float volume(float mastery) {
		return Mth.lerp(mastery, 0.85F, 2.45F);
	}

	/** Altura da aura, no mesmo ritmo; com 100% ela passa de duas vezes a altura do jogador. */
	private static float stature(float mastery) {
		return Mth.lerp(mastery, 0.97F, 2.12F);
	}

	/** O que nao e a silhueta (linguas, ondas, espessura) cresce do tamanho normal ate o dobro. */
	private static float growth(float mastery) {
		return Mth.lerp(mastery, 1, 2);
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
		Quaternionf facing = cameraRotation(camera);
		for (Player player : minecraft.level.players()) {
			View view = view(player);
			Vec3 position = player.getPosition(partial);
			Vec3 relative = position.subtract(camera.pos);
			if (view == null || relative.length() > MAX_DISTANCE + view.show().enRadius()) {
				continue;
			}
			AuraShow show = view.show();
			float seed = (player.getId() * 0.37F) % 6.28F;
			boolean isCamera = player == minecraft.player && camera.isFirstPerson;
			pose.pushPose();
			pose.translate((float) relative.x, (float) relative.y, (float) relative.z);
			if (show.en()) {
				dome(pose, collector, view, relative, time, seed);
			}
			if (show.level() > 0) {
				if (!isCamera && !player.isInvisible()) {
					// De frente o corpo e mais largo do que de lado
					float bodyYaw = player.getPreciseBodyRotation(partial) * Mth.DEG_TO_RAD;
					float side = Math.abs(Mth.sin(bodyYaw) * (float) relative.x - Mth.cos(bodyYaw) * (float) relative.z)
							/ Math.max(0.01F, (float) relative.horizontalDistance());
					body(pose, collector, view, time, seed, player.getBbHeight(), Mth.lerp(side, 0.72F, 1));
				}
				motes(pose, collector, view, facing, time, seed, isCamera);
			}
			if (show.ko()) {
				groundLight(pose, collector, view, player.getPreciseBodyRotation(partial));
			}
			pose.popPose();
		}
		sensedEntities(minecraft, pose, collector, camera, partial, time);
	}

	/** Chama fina em volta de quem o Gyo ou o En de quem joga revelam, por cima do contorno do jogo. */
	private static void sensedEntities(Minecraft minecraft, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera, float partial, float time) {
		View own = ownView();
		if (own == null || (!own.show().gyo() && !own.show().en())) {
			return;
		}
		for (Entity entity : minecraft.level.entitiesForRendering()) {
			if (!GyoControl.sees(entity) && !EnSense.senses(entity)) {
				continue;
			}
			Vec3 relative = entity.getPosition(partial).subtract(camera.pos);
			pose.pushPose();
			pose.translate((float) relative.x, (float) relative.y, (float) relative.z);
			float height = entity.getBbHeight();
			float width = Math.max(0.3F, entity.getBbWidth() * 0.75F);
			float seed = entity.getId() * 0.53F;
			twoPasses(pose, collector, RIM, RIM_SHINE, (entry, buffer, strength) -> {
				for (int layer = 0; layer < 2; layer++) {
					Flame flame = new Flame();
					flame.center.set(0, height / 2, 0);
					flame.halfWidth = width;
					flame.halfHeight = height * 0.56F;
					flame.tongue = height * 0.14F;
					flame.wave = 0.05F;
					flame.rim = 0.06F;
					flame.color = own.primary();
					flame.alpha = 0.7F * strength * (1 - layer * 0.3F);
					flame.seed = seed + layer * 2;
					flame.time = time * SLOW;
					flame.behind = width;
					flame.emit(entry, buffer, false);
				}
			});
			pose.popPose();
		}
	}

	/**
	 * Aura do corpo: contornos de chama atras do modelo, que recortam a silhueta sem cobrir o personagem.
	 * Quanto mais forte a tecnica, mais altas as linguas e mais camadas ciano e douradas.
	 *
	 * @param widthFactor largura vista agora: o corpo visto de lado e mais fino
	 */
	private static void body(PoseStack pose, SubmitNodeCollector collector, View view, float time, float seed, float height, float widthFactor) {
		AuraShow show = view.show();
		int level = show.level();
		float mastery = show.mastery() / 100F;
		float share = bodyShare(show);
		float size = volume(mastery) * Mth.sqrt(share) * widthFactor;
		int tier = tier(mastery);
		int layers = (level == AuraShow.SOFT ? new int[] {2, 3, 4} : level == AuraShow.FLAMES ? new int[] {3, 5, 6} : new int[] {4, 7, 9})[tier];
		float halfWidth = (level == AuraShow.SOFT ? 0.45F : level == AuraShow.FLAMES ? 0.58F : 0.72F) * size;
		float halfHeight = height * 0.53F * stature(mastery);
		float tongue = (level == AuraShow.SOFT ? 0.08F + 0.18F * mastery : level == AuraShow.FLAMES ? 0.55F + 0.5F * mastery : 0.85F + 0.7F * mastery) * height / 1.8F * growth(mastery);
		float wave = (level == AuraShow.SOFT ? 0.035F + 0.05F * mastery : 0.07F + 0.09F * mastery) * growth(mastery);
		// A opacidade sobe com a aura liberada e com o controle, ate o dominio medio; depois o miolo fica mais vazio
		float opacity = (level == AuraShow.SOFT ? 0.56F : level == AuraShow.FLAMES ? 0.68F : 0.78F) * Mth.lerp(Math.min(1, mastery * 2), 0.6F, 1) * view.visibility() * Math.min(1.25F, share);
		float fill = tier == 2 ? 0.5F : 0.8F;
		float speed = SLOW * (1 + 0.8F * mastery);
		float flicker = flicker(mastery);
		int primary = view.primary();
		int secondary = view.secondary();
		if (level >= AuraShow.FLAMES) {
			collector.submitCustomGeometry(pose, GLOW, (entry, buffer) -> {
				Vector3f viewer = viewer(entry, view.preview());
				Vector3f center = new Vector3f(0, halfHeight * 1.1F, 0);
				billboard(entry, buffer, center, viewer, 3.2F * size, primary, 0.16F * opacity);
				billboard(entry, buffer, center.set(0, halfHeight * 0.8F, 0), viewer, 2.3F * size, secondary, 0.16F * opacity);
			});
		}
		twoPasses(pose, collector, RIM, RIM_SHINE, (entry, buffer, strength) -> {
			for (int layer = 0; layer < layers; layer++) {
				float inner = 1 - (float) layer / layers * 0.5F;
				Flame flame = new Flame();
				flame.halfWidth = halfWidth * inner;
				flame.halfHeight = halfHeight * Mth.lerp(inner, 0.86F, 1);
				// Cresce para cima: a base fica nos pes
				flame.center.set(0, flame.halfHeight - height * 0.03F, 0);
				flame.tongue = tongue * (layer % 2 == 0 ? 1 : 0.72F) * inner;
				flame.wave = wave;
				flame.rim = (level == AuraShow.SOFT ? 0.13F : 0.2F) * Mth.lerp(mastery, 0.75F, 2.3F);
				flame.fill = fill;
				flame.color = level != AuraShow.SOFT && layer % 2 == 0 ? secondary : primary;
				flame.alpha = opacity * strength * Mth.lerp(inner, 0.55F, 1);
				flame.seed = seed + layer * 1.7F;
				flame.time = time * speed;
				flame.flicker = flicker;
				flame.behind = 0.36F;
				flame.emit(entry, buffer, view.preview());
			}
		});
	}

	/** Quadradinhos de luz subindo em volta do jogador: poucos com pouco dominio, muitos e rapidos com muito. */
	private static void motes(PoseStack pose, SubmitNodeCollector collector, View view, Quaternionf facing, float time, float seed, boolean isCamera) {
		AuraShow show = view.show();
		int level = show.level();
		float mastery = show.mastery() / 100F;
		int tier = tier(mastery);
		int count = (level == AuraShow.BURST ? new int[] {10, 26, 44} : level == AuraShow.FLAMES ? new int[] {6, 16, 28} : new int[] {3, 8, 14})[tier];
		float reach = (level == AuraShow.BURST ? 1.5F : level == AuraShow.FLAMES ? 1.2F : 0.85F) * volume(mastery);
		float period = Mth.lerp(mastery, 150, 70);
		float alpha = view.visibility() * Mth.lerp(mastery, 0.6F, 1);
		twoPasses(pose, collector, MOTE, MOTE_SHINE, (entry, buffer, strength) -> {
			for (int i = 0; i < count; i++) {
				float life = (time / (period * (0.75F + 0.5F * hash(i, 13))) + hash(i, 17)) % 1;
				float angle = seed + hash(i, 19) * Mth.TWO_PI + life * 1.2F;
				// Na primeira pessoa ficam longe dos olhos: de perto um quadradinho tomaria a tela
				float away = isCamera ? 1.4F + reach * 1.3F * hash(i, 23) : 0.3F + reach * hash(i, 23);
				float x = Mth.cos(angle) * away;
				float z = Mth.sin(angle) * away;
				float y = (hash(i, 29) * 0.8F + life * (level == AuraShow.SOFT ? 1.5F : 2.6F)) * stature(mastery);
				float size = (0.05F + 0.07F * hash(i, 31)) * (isCamera ? 1 : (level == AuraShow.SOFT ? 1.4F : 2) * Mth.lerp(mastery, 1, 1.4F));
				int color = level == AuraShow.SOFT || i % 3 == 0 ? view.primary() : view.secondary();
				billboard(entry, buffer, facing, x, y, z, size, color, Mth.sin(life * Mth.PI) * alpha * strength);
			}
		});
	}

	private static Quaternionf cameraRotation(CameraRenderState camera) {
		return new Quaternionf().rotationYXZ(Mth.PI - camera.yRot * Mth.DEG_TO_RAD, -camera.xRot * Mth.DEG_TO_RAD, 0);
	}

	/** Luz no chao embaixo do punho carregado. */
	private static void groundLight(PoseStack pose, SubmitNodeCollector collector, View view, float bodyYaw) {
		float charge = view.show().koCharge();
		float forward = 0.8F;
		float x = -Mth.sin(bodyYaw * Mth.DEG_TO_RAD) * forward;
		float z = Mth.cos(bodyYaw * Mth.DEG_TO_RAD) * forward;
		float half = 1 + 1.6F * charge;
		float alpha = (0.2F + 0.35F * charge) * view.visibility();
		int color = view.primary();
		collector.submitCustomGeometry(pose, GLOW, (entry, buffer) -> {
			float y = 0.04F;
			vertex(entry, buffer, x - half, y, z - half, color, alpha, 0, 0);
			vertex(entry, buffer, x - half, y, z + half, color, alpha, 0, 1);
			vertex(entry, buffer, x + half, y, z + half, color, alpha, 1, 1);
			vertex(entry, buffer, x + half, y, z - half, color, alpha, 1, 0);
		});
	}

	/** Esfera do En centrada nos pes: a metade de baixo fica sob o chao e sobra a cupula. */
	private static void dome(PoseStack pose, SubmitNodeCollector collector, View view, Vec3 relative, float time, float seed) {
		float mastery = view.show().enMastery() / 100F;
		// Pouco dominio: a superficie treme; muito: ela respira devagar, com mais presenca
		float breathing = 1 + 0.015F * mastery * Mth.sin(time * 0.04F);
		float radius = view.show().enRadius() * breathing;
		float flicker = flicker(mastery) * 0.5F * (0.5F + 0.5F * Mth.sin(time * 0.3F + seed));
		float visibility = view.visibility() * Mth.lerp(mastery, 0.62F, 1.1F) * (1 - flicker);
		// Da camera ate o centro da esfera, para o brilho da borda; por dentro fica mais transparente
		Vector3f eye = new Vector3f((float) -relative.x, (float) -relative.y, (float) -relative.z);
		float inside = eye.length() < radius ? 0.7F : 1;
		float repeatU = Math.max(1, Math.round(Mth.TWO_PI * radius / DOME_TILE));
		// A textura e duas vezes mais larga que alta
		float repeatV = Math.max(1, Math.round(Mth.TWO_PI * radius / DOME_TILE));
		float scroll = time * 0.0006F;
		int tint = mixWhite(view.primary(), DOME_WHITE_MIX);
		collector.submitCustomGeometry(pose, DOME, (entry, buffer) -> {
			for (int row = 0; row < DOME_ROWS; row++) {
				for (int column = 0; column < DOME_COLUMNS; column++) {
					domeVertex(entry, buffer, column, row, radius, eye, repeatU, repeatV, scroll, inside * visibility, tint);
					domeVertex(entry, buffer, column, row + 1, radius, eye, repeatU, repeatV, scroll, inside * visibility, tint);
					domeVertex(entry, buffer, column + 1, row + 1, radius, eye, repeatU, repeatV, scroll, inside * visibility, tint);
					domeVertex(entry, buffer, column + 1, row, radius, eye, repeatU, repeatV, scroll, inside * visibility, tint);
				}
			}
		});
		// Labaredas lentas subindo do chao, junto da borda; mais numerosas com mais dominio
		int flames = Math.max(5, Math.round(radius * Mth.lerp(mastery, 1.6F, 4)));
		twoPasses(pose, collector, WISP, WISP_SHINE, (entry, buffer, strength) -> {
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
				ribbon.phase = hash(i, 47) * Mth.TWO_PI + time * 0.02F;
				ribbon.width = 0.5F;
				ribbon.color = view.primary();
				ribbon.alpha = 0.55F * visibility * inside * strength;
				ribbon.wobble = 0.15F;
				ribbon.time = time * 0.5F + hash(i, 53) * 40;
				ribbon.taper = true;
				ribbon.flow = 0.05F;
				ribbon.emit(entry, buffer);
			}
		});
	}

	private static void domeVertex(PoseStack.Pose entry, VertexConsumer buffer, int column, int row, float radius, Vector3f eye,
			float repeatU, float repeatV, float scroll, float alpha, int tint) {
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
		vertex(entry, buffer, x, y, z, tint, Math.min(1, shade * alpha), u * repeatU + scroll, v * repeatV);
	}

	// ---- Bracos, punho e olhos ----

	/**
	 * Tudo que fica no osso do braco, com o eixo y descendo pelo braco.
	 *
	 * @param side 1 para o direito, -1 para o esquerdo
	 */
	public static void arm(PoseStack pose, SubmitNodeCollector collector, View view, float time, int side, boolean main, boolean firstPerson) {
		AuraShow show = view.show();
		float center = -side / 16F;
		if (show.ko()) {
			if (main) {
				fist(pose, collector, view, time, center, firstPerson);
			}
			return;
		}
		int level = show.level();
		if (level == 0) {
			return;
		}
		float mastery = show.mastery() / 100F;
		float share = armShare(show);
		int layers = new int[] {2, 3, 4}[tier(mastery)];
		float opacity = (level == AuraShow.SOFT ? 0.62F : 0.78F) * Mth.lerp(Math.min(1, mastery * 2), 0.6F, 1) * view.visibility() * Math.min(1.3F, share);
		float speed = SLOW * (1 + 0.8F * mastery);
		float flicker = flicker(mastery);
		float size = volume(mastery) * Mth.sqrt(share) * (firstPerson ? 1.25F : 1);
		twoPasses(pose, collector, RIM, RIM_SHINE, (entry, buffer, strength) -> {
			for (int layer = 0; layer < layers; layer++) {
				float inner = 1 - (float) layer / layers * 0.35F;
				Flame flame = new Flame();
				flame.center.set(center, 0.3F, 0);
				flame.halfWidth = (level == AuraShow.SOFT ? 0.17F : 0.22F) * size * inner;
				flame.halfHeight = 0.5F * Mth.lerp(inner, 0.92F, 1) * Mth.lerp(mastery, 1, 1.5F);
				flame.tongue = 0;
				flame.wave = (0.035F + 0.03F * mastery) * growth(mastery);
				flame.rim = (firstPerson ? 0.11F : 0.07F) * Mth.lerp(mastery, 0.8F, 2.4F);
				flame.fill = tier(mastery) == 2 ? 0.55F : 0.8F;
				flame.color = level != AuraShow.SOFT && layer % 2 == 0 ? view.secondary() : view.primary();
				flame.alpha = opacity * strength * Mth.lerp(inner, 0.6F, 1);
				flame.seed = side * 3 + layer * 2.1F;
				flame.time = time * speed;
				flame.flicker = flicker;
				flame.behind = firstPerson ? 0.05F : 0.14F;
				flame.emit(entry, buffer, view.preview());
			}
		});
		if (level >= AuraShow.FLAMES) {
			// O anel que gira devagar em volta do antebraco
			swirl(pose, collector, view, center, -0.1F, 0.85F, 0.22F * size, 3, time, 59 + side);
		}
		if (firstPerson) {
			armWisps(pose, collector, view, center, size, opacity * (1 - flicker * 0.5F), time, tier(mastery) + 3);
		}
		armMotes(pose, collector, view, center, time, firstPerson ? tier(mastery) + 1 : tier(mastery) * 2 + 2, firstPerson ? 0.3F : 0.22F, firstPerson ? 0.45F : 1);
	}

	/** Em primeira pessoa o braco vem quase na direcao dos olhos: fios lentos deitados sobre ele mostram a aura. */
	private static void armWisps(PoseStack pose, SubmitNodeCollector collector, View view, float center, float size, float opacity, float time, int count) {
		twoPasses(pose, collector, WISP, WISP_SHINE, (entry, buffer, strength) -> {
			for (int i = 0; i < count; i++) {
				Ribbon ribbon = new Ribbon();
				ribbon.start.set(center, -0.25F, 0);
				ribbon.axis.set(0, 1, 0);
				ribbon.side.set(1, 0, 0);
				ribbon.length = 1;
				ribbon.radius = 0.19F * size;
				ribbon.flare = 0.2F;
				ribbon.turns = (0.25F + 0.2F * hash(i, 149)) * (i % 2 == 0 ? 1 : -1);
				ribbon.phase = hash(i, 151) * Mth.TWO_PI + time * 0.012F;
				ribbon.width = 0.28F * Mth.sqrt(size);
				ribbon.color = view.show().level() > AuraShow.SOFT && i % 2 == 1 ? view.secondary() : view.primary();
				ribbon.alpha = 0.6F * opacity * strength;
				ribbon.wobble = 0.05F;
				ribbon.time = time * 0.5F + hash(i, 157) * 30;
				ribbon.taper = false;
				ribbon.flow = 0.03F;
				ribbon.emit(entry, buffer);
			}
		});
	}

	private static void swirl(PoseStack pose, SubmitNodeCollector collector, View view, float x, float y, float length, float radius, int count, float time, int salt) {
		twoPasses(pose, collector, WISP, WISP_SHINE, (entry, buffer, strength) -> {
			for (int i = 0; i < count; i++) {
				float shift = hash(i, salt);
				Ribbon ribbon = new Ribbon();
				ribbon.start.set(x, y, 0);
				ribbon.axis.set(0, 1, 0);
				ribbon.side.set(1, 0, 0);
				ribbon.length = length;
				ribbon.radius = radius * (0.85F + 0.4F * hash(i, 61));
				ribbon.flare = 0.25F;
				ribbon.turns = (0.8F + 0.5F * hash(i, 67)) * (i % 2 == 0 ? 1 : -1);
				ribbon.phase = shift * Mth.TWO_PI + time * 0.025F * (i % 2 == 0 ? 1 : -1);
				ribbon.width = 0.22F;
				ribbon.color = i % 2 == 0 ? view.primary() : view.secondary();
				ribbon.alpha = 0.42F * view.visibility() * strength;
				ribbon.wobble = 0.04F;
				ribbon.time = time * 0.5F + shift * 30;
				ribbon.taper = false;
				ribbon.flow = 0.03F;
				ribbon.emit(entry, buffer);
			}
		});
	}

	private static void armMotes(PoseStack pose, SubmitNodeCollector collector, View view, float center, float time, int count, float out, float scale) {
		twoPasses(pose, collector, MOTE, MOTE_SHINE, (entry, buffer, strength) -> {
			Vector3f viewer = viewer(entry, view.preview());
			Vector3f position = new Vector3f();
			for (int i = 0; i < count; i++) {
				float life = (time / (90 + 60 * hash(i, 71)) + hash(i, 73)) % 1;
				float angle = hash(i, 79) * Mth.TWO_PI + life * 2;
				float away = out + out * hash(i, 83);
				position.set(center + Mth.cos(angle) * away, 0.65F - life * 0.95F, Mth.sin(angle) * away);
				billboard(entry, buffer, position, viewer, (0.05F + 0.05F * hash(i, 89)) * scale, view.primary(), Mth.sin(life * Mth.PI) * view.visibility() * strength);
			}
		});
	}

	/** Ko: toda a aura numa esfera de energia no punho, que cresce com a carga. */
	private static void fist(PoseStack pose, SubmitNodeCollector collector, View view, float time, float center, boolean firstPerson) {
		float charge = view.show().koCharge();
		float mastery = view.show().mastery() / 100F;
		int tier = tier(mastery);
		float flicker = flicker(mastery);
		// Perto dos olhos ela cresce menos, para nao tampar a mira
		float radius = firstPerson ? (0.14F + 0.2F * charge) * Mth.lerp(mastery, 0.85F, 1.9F) : (0.2F + 0.36F * charge) * volume(mastery);
		// Logo depois do punho: o miolo branco nao fica escondido atras da mao
		float y = 0.66F + radius * 0.75F;
		int primary = view.primary();
		int secondary = view.secondary();
		// Pouco dominio: a esfera treme e perde forca; muito: firme, densa e cheia de faiscas
		float visibility = view.visibility() * Mth.lerp(Math.min(1, mastery * 2), 0.6F, 1) * (1 - flicker * 0.5F * (0.5F + 0.5F * Mth.sin(time * 0.45F)));
		float pulse = 1 + (0.05F + flicker * 0.14F) * Mth.sin(time * 0.12F * (1 + flicker * 3));
		collector.submitCustomGeometry(pose, GLOW, (entry, buffer) -> {
			Vector3f viewer = viewer(entry, view.preview());
			Vector3f middle = new Vector3f(center, y, 0);
			billboard(entry, buffer, middle, viewer, radius * 5.5F * pulse, primary, 0.5F * visibility);
			billboard(entry, buffer, middle, viewer, radius * 3.2F * pulse, mixWhite(primary, 0.5F), 0.6F * visibility);
			billboard(entry, buffer, middle, viewer, radius * 2F * pulse, WHITE, 0.85F * visibility);
		});
		twoPasses(pose, collector, RIM, RIM_SHINE, (entry, buffer, strength) -> {
			for (int layer = 0; layer < 2 + tier; layer++) {
				Flame flame = new Flame();
				flame.center.set(center, y, 0);
				flame.halfWidth = radius * (1 - layer * 0.14F);
				flame.halfHeight = radius * (1 - layer * 0.14F);
				flame.tongue = 0;
				flame.round = true;
				flame.wave = radius * 0.12F;
				flame.rim = radius * 0.34F;
				flame.color = layer % 2 == 0 ? secondary : primary;
				flame.alpha = 0.6F * visibility * strength;
				flame.seed = layer * 2.3F;
				flame.time = time * SLOW * 2.2F;
				flame.flicker = flicker;
				flame.emit(entry, buffer, view.preview());
			}
		});
		twoPasses(pose, collector, WISP, WISP_SHINE, (entry, buffer, strength) -> {
			for (int i = 0; i < 2 + tier * 3; i++) {
				float tilt = hash(i, 107) * Mth.TWO_PI;
				Ribbon ribbon = new Ribbon();
				ribbon.axis.set(Mth.cos(tilt), 0.6F, Mth.sin(tilt)).normalize();
				ribbon.start.set(center, y, 0).sub(new Vector3f(ribbon.axis).mul(radius));
				ribbon.side.set(ribbon.axis).cross(0.2F, 1, 0.1F).normalize();
				ribbon.length = radius * 2;
				ribbon.radius = radius * 0.62F;
				ribbon.flare = 0.5F;
				ribbon.turns = 1.2F * (i % 2 == 0 ? 1 : -1);
				ribbon.phase = hash(i, 109) * Mth.TWO_PI + time * 0.06F * (i % 2 == 0 ? 1 : -1);
				ribbon.width = radius * 0.4F;
				ribbon.color = i % 2 == 0 ? mixWhite(primary, 0.4F) : secondary;
				ribbon.alpha = 0.55F * visibility * strength;
				ribbon.wobble = radius * 0.08F;
				ribbon.time = time;
				ribbon.taper = false;
				ribbon.flow = 0.05F;
				ribbon.emit(entry, buffer);
			}
		});
		twoPasses(pose, collector, MOTE, MOTE_SHINE, (entry, buffer, strength) -> {
			Vector3f viewer = viewer(entry, view.preview());
			Vector3f position = new Vector3f();
			int count = Math.round((8 + charge * 14) * Mth.lerp(mastery, 0.4F, 1.3F));
			for (int i = 0; i < count; i++) {
				float life = (time / (35 + 35 * hash(i, 113)) + hash(i, 127)) % 1;
				float yaw = hash(i, 131) * Mth.TWO_PI;
				float pitch = hash(i, 137) * Mth.PI;
				float away = radius * (1 + life * 1.8F);
				position.set(center + Mth.cos(yaw) * Mth.sin(pitch) * away, y + Mth.cos(pitch) * away, Mth.sin(yaw) * Mth.sin(pitch) * away);
				billboard(entry, buffer, position, viewer, 0.05F + 0.05F * hash(i, 139), i % 3 == 0 ? primary : secondary,
						Mth.sin(life * Mth.PI) * visibility * strength);
			}
		});
	}

	/** Gyo visto de fora: chama pequena em volta da cabeca. A pose ja esta no osso da cabeca. */
	public static void head(PoseStack pose, SubmitNodeCollector collector, View view, float time) {
		float mastery = view.show().gyoMastery() / 100F;
		float size = volume(mastery);
		float flicker = flicker(mastery);
		twoPasses(pose, collector, RIM, RIM_SHINE, (entry, buffer, strength) -> {
			for (int layer = 0; layer < 2; layer++) {
				Flame flame = new Flame();
				flame.center.set(0, -0.25F, 0);
				flame.axis.set(0, -1, 0);
				flame.halfWidth = 0.3F * size * (1 - layer * 0.15F);
				flame.halfHeight = 0.3F * size * (1 - layer * 0.15F);
				flame.tongue = (0.1F + 0.1F * mastery) * growth(mastery);
				flame.wave = (0.02F + 0.02F * mastery) * growth(mastery);
				flame.rim = 0.05F * growth(mastery);
				flame.color = view.primary();
				flame.alpha = 0.7F * view.visibility() * strength;
				flame.seed = layer * 1.9F;
				flame.time = time * SLOW;
				flame.flicker = flicker;
				flame.behind = 0.2F;
				flame.emit(entry, buffer, view.preview());
			}
		});
	}

	/** Gyo em primeira pessoa: a aura nos olhos vira chamas lentas nas bordas da tela. */
	public static void gyoVignette(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
		Minecraft minecraft = Minecraft.getInstance();
		View view = ownView();
		if (view == null || !view.show().gyo() || !minecraft.options.getCameraType().isFirstPerson() || minecraft.gui.screen() != null) {
			return;
		}
		int width = graphics.guiWidth();
		int height = graphics.guiHeight();
		float mastery = view.show().gyoMastery() / 100F;
		float opacity = Mth.lerp(mastery, 0.5F, 1) * (1 - flicker(mastery) * 0.4F * (0.5F + 0.5F * Mth.sin(now() * 0.25F)));
		// Tres quadros que vao se misturando: a fumaca muda de forma devagar, sem quadros visiveis
		float position = now() / VIGNETTE_FRAME_TICKS % VIGNETTE_FRAMES;
		int first = (int) position;
		float blend = position - first;
		float spread = Mth.lerp(mastery, 1.15F, 1);
		blitVignette(graphics, first, width, height, spread, view.primary(), opacity * (1 - blend));
		blitVignette(graphics, (first + 1) % VIGNETTE_FRAMES, width, height, spread, view.primary(), opacity * blend);
	}

	private static void blitVignette(GuiGraphicsExtractor graphics, int frame, int width, int height, float spread, int color, float alpha) {
		int tint = Mth.clamp((int) (alpha * 255), 0, 255) << 24 | mixWhite(color, 0.15F);
		int drawnWidth = Math.round(width * spread);
		int drawnHeight = Math.round(height * spread);
		graphics.blit(RenderPipelines.GUI_TEXTURED, texture("vignette_" + frame), (width - drawnWidth) / 2, (height - drawnHeight) / 2, 0, 0,
				drawnWidth, drawnHeight, VIGNETTE_WIDTH, VIGNETTE_HEIGHT, VIGNETTE_WIDTH, VIGNETTE_HEIGHT, tint);
	}

	public static boolean ownKo() {
		View view = ownView();
		return view != null && view.show().ko();
	}

	public static void firstPersonArm(PoseStack pose, SubmitNodeCollector collector, int side) {
		View view = ownView();
		LocalPlayer player = Minecraft.getInstance().player;
		if (view == null || player == null) {
			return;
		}
		boolean main = (player.getMainArm() == HumanoidArm.RIGHT) == (side == 1);
		pose.pushPose();
		// Mesma posicao do osso do braco no modelo do jogador
		pose.translate(-side * 5 / 16F, 2 / 16F, 0);
		arm(pose, collector, view, now(), side, main, true);
		pose.popPose();
	}

	/** Com a aura ligada a mao aparece junto do objeto, menos quando o braco atrapalharia (mapa, besta, item em uso). */
	public static boolean showsBareArm(ItemStack stack, boolean usingThisHand) {
		return ownView() != null && !usingThisHand && !stack.has(DataComponents.MAP_ID) && !stack.is(Items.CROSSBOW);
	}

	// ---- Objeto segurado ----

	/** Primeira pessoa. */
	public static void coatHeld(boolean mainHand, ItemStack stack) {
		View view = ownView();
		boolean held = !stack.isEmpty() && (mainHand || stack.is(Items.SHIELD));
		coating = view != null && held && coats(view.show()) ? view : null;
		coatingTime = now();
	}

	/** Terceira pessoa. */
	public static void coatHeld(ArmedEntityRenderState state, HumanoidArm arm, ItemStack stack) {
		View view = state instanceof AvatarRenderState avatar ? avatar.getData(VIEW) : null;
		boolean held = arm == state.mainArm || stack.is(Items.SHIELD);
		coating = view != null && held && coats(view.show()) ? view : null;
		coatingTime = state.ageInTicks;
	}

	private static boolean coats(AuraShow show) {
		return !show.ko() && (show.shu() || show.level() > 0);
	}

	public static void endCoat() {
		coating = null;
	}

	/**
	 * Chama em volta do objeto, chamado na pose do modelo dele, que ocupa o cubo de 0 a 1.
	 * Ten e Ren so passam de leve pelo objeto; com Shu ele brilha na cor principal e a chama fica densa.
	 */
	public static void coat(PoseStack pose, SubmitNodeCollector collector, ItemQuads quads) {
		View view = coating;
		if (view == null) {
			return;
		}
		float time = coatingTime;
		AuraShow show = view.show();
		float visibility = view.visibility();
		boolean shu = show.shu();
		float mastery = (shu ? show.shuMastery() : show.mastery()) / 100F;
		if (shu && !quads.isEmpty()) {
			float tintAlpha = visibility * Mth.lerp(mastery, 0.6F, 1);
			collector.submitCustomGeometry(pose, COAT, (entry, buffer) -> {
				QuadInstance instance = new QuadInstance();
				instance.setColor(Mth.ceil(tintAlpha * 255) << 24 | scaled(view.primary(), COAT_STRENGTH));
				instance.setLightCoords(FULL_BRIGHT);
				instance.setOverlayCoords(OverlayTexture.NO_OVERLAY);
				for (BakedQuad quad : quads.all()) {
					if (TextureAtlas.LOCATION_ITEMS.equals(quad.materialInfo().sprite().atlasLocation())) {
						buffer.putBakedQuad(entry, quad, instance);
					}
				}
			});
		}
		int layers = shu ? new int[] {2, 3, 4}[tier(mastery)] : 2;
		float size = volume(mastery) * (shu ? 1.1F : 0.9F) * Mth.sqrt(armShare(show));
		float opacity = (shu ? 0.85F : 0.5F) * visibility * Mth.lerp(Math.min(1, mastery * 2), 0.6F, 1) * Math.min(1.3F, armShare(show));
		float flicker = flicker(mastery);
		twoPasses(pose, collector, RIM, RIM_SHINE, (entry, buffer, strength) -> {
			for (int layer = 0; layer < layers; layer++) {
				float inner = 1 - (float) layer / layers * 0.35F;
				Flame flame = new Flame();
				// Da empunhadura ate a ponta, na diagonal em que as armas sao desenhadas
				flame.center.set(0.56F, 0.56F, 0.5F);
				flame.axis.set(0.7071F, 0.7071F, 0);
				flame.halfWidth = 0.24F * size * inner;
				flame.halfHeight = 0.72F * Mth.lerp(inner, 0.92F, 1) * Mth.lerp(mastery, 1, 1.5F);
				flame.round = true;
				flame.wave = (0.03F + 0.03F * mastery) * growth(mastery);
				flame.rim = 0.11F * Mth.lerp(mastery, 0.8F, 2);
				flame.fill = 0.8F;
				flame.color = !shu && show.level() > AuraShow.SOFT && layer == 0 ? view.secondary() : view.primary();
				flame.alpha = opacity * strength * Mth.lerp(inner, 0.6F, 1);
				flame.seed = layer * 2.7F;
				flame.time = time * SLOW * (1 + 0.8F * mastery);
				flame.flicker = flicker;
				flame.behind = 0.08F;
				flame.emit(entry, buffer, view.preview());
			}
		});
	}

	// ---- Geometria ----

	private interface Pass {
		void draw(PoseStack.Pose entry, VertexConsumer buffer, float strength);
	}

	/** A mesma geometria com a cor certa e, por cima, somada de leve, para brilhar sem lavar a cor. */
	private static void twoPasses(PoseStack pose, SubmitNodeCollector collector, RenderType solid, RenderType shine, Pass pass) {
		collector.submitCustomGeometry(pose, solid, (entry, buffer) -> pass.draw(entry, buffer, 1));
		collector.submitCustomGeometry(pose, shine, (entry, buffer) -> pass.draw(entry, buffer, SHINE));
	}

	/** Posicao de quem olha, no espaco da pose: na previa da aba Aura a camera fica no eixo z, longe. */
	private static Vector3f viewer(PoseStack.Pose entry, boolean preview) {
		Matrix4f inverse = entry.pose().invert(new Matrix4f());
		Vector3f origin = inverse.transformPosition(new Vector3f(0, 0, 0));
		return preview ? origin.add(inverse.transformDirection(new Vector3f(0, 0, 100))) : origin;
	}

	/**
	 * Contorno de chama num plano virado para quem olha, girando so em volta do proprio eixo. Fica um pouco atras
	 * do que envolve, entao o modelo cobre o miolo e sobra o contorno em volta da silhueta.
	 */
	private static final class Flame {
		private static final int SEGMENTS = 120;

		final Vector3f center = new Vector3f();
		final Vector3f axis = new Vector3f(0, 1, 0);
		float halfWidth;
		float halfHeight;
		/** Altura das linguas na parte de cima. */
		float tongue;
		float wave;
		/** Espessura da borda para fora da curva. */
		float rim;
		/** Quanto da curva a faixa ocupa para dentro: mais vazio no miolo com valor menor. */
		float fill = 0.7F;
		/** Esfera: mantem o circulo, sem ponta para cima. */
		boolean round;
		int color;
		float alpha;
		float seed;
		/** Tempo ja multiplicado pela velocidade: quanto menor, mais lenta a chama. */
		float time;
		float flicker;
		float behind;
		private float narrowing = 1;

		void emit(PoseStack.Pose entry, VertexConsumer buffer, boolean preview) {
			Vector3f viewer = viewer(entry, preview);
			Vector3f toViewer = new Vector3f(viewer).sub(center);
			Vector3f right = new Vector3f(axis).cross(toViewer);
			if (right.lengthSquared() < 1.0E-6F) {
				right.set(1, 0, 0);
			}
			right.normalize();
			toViewer.normalize();
			Vector3f middle = new Vector3f(center).sub(new Vector3f(toViewer).mul(behind));
			// O que aponta para quem olha fica curto: a largura acompanha, para a chama nao abrir em leque
			float pointing = axis.dot(toViewer);
			narrowing = 1 - 0.5F * pointing * pointing;

			float[] x = new float[SEGMENTS];
			float[] y = new float[SEGMENTS];
			float[] breadth = new float[SEGMENTS];
			float[] fade = new float[SEGMENTS];
			for (int i = 0; i < SEGMENTS; i++) {
				curve(i, x, y, breadth, fade);
			}
			float keep = 1 - fill * 0.55F;
			float[] previous = band(0, x, y, breadth, keep, right, middle);
			for (int i = 1; i <= SEGMENTS; i++) {
				int index = i % SEGMENTS;
				int before = i - 1;
				float[] next = band(index, x, y, breadth, keep, right, middle);
				float uBefore = (float) before / SEGMENTS * 3 - time * 0.12F;
				float uNext = (float) i / SEGMENTS * 3 - time * 0.12F;
				vertex(entry, buffer, previous[0], previous[1], previous[2], color, fade[before] * fill * 0.7F, uBefore, 0);
				vertex(entry, buffer, previous[3], previous[4], previous[5], color, fade[before], uBefore, 1);
				vertex(entry, buffer, next[3], next[4], next[5], color, fade[index], uNext, 1);
				vertex(entry, buffer, next[0], next[1], next[2], color, fade[index] * fill * 0.7F, uNext, 0);
				previous = next;
			}
		}

		/** Ponto da curva no plano da chama, com a largura da borda e o alfa dele. */
		private void curve(int index, float[] outX, float[] outY, float[] outBreadth, float[] outFade) {
			float theta = (float) index / SEGMENTS * Mth.TWO_PI;
			float cos = Mth.cos(theta);
			float sin = Mth.sin(theta);
			// Curva fechada: so frequencias inteiras do angulo, para o comeco encontrar o fim
			float noise = (Mth.sin(3 * theta + time * 1.1F + seed) + 0.6F * Mth.sin(5 * theta - time * 0.8F + seed * 2)
					+ 0.35F * Mth.sin(9 * theta + time * 1.6F + seed * 3)) / 1.95F;
			float up = (sin + 1) / 2;
			float x = halfWidth * narrowing * shaped(cos);
			float y = halfHeight * shaped(sin);
			float dissolve = 0;
			if (round) {
				float radius = 1 + noise * wave / Math.max(0.001F, halfWidth);
				x *= radius;
				y *= 1 + noise * wave / Math.max(0.001F, halfHeight);
			} else {
				float push = wave * noise * (0.4F + 0.6F * up);
				x += cos * push;
				y += sin * push;
				// Afina para cima, como chama, e sobe nas linguas
				x *= 1 - 0.28F * up * up;
				if (tongue != 0) {
					float peaks = Mth.sin(5 * theta + seed - time * 0.5F);
					float tip = peaks > 0 ? peaks * peaks : 0;
					y += tongue * up * up * up * (0.3F + 0.7F * tip);
					// A ponta da lingua vai sumindo, em vez de terminar num risco
					dissolve = tip * up * up * up;
				}
				// As linguas balancam devagar, curvando conforme sobem
				x += Mth.sin(y * 1.6F - time * 1.2F + seed) * (wave * 0.6F + tongue * 0.1F) * up;
			}
			float gaps = 0.62F + 0.38F * Mth.sin(2 * theta + time * 0.6F + seed * 1.3F);
			// A base some aos poucos: a chama sobe do chao, sem uma linha reta onde ela encontra o terreno
			float bottom = round ? 1 : Mth.clamp((y + halfHeight) / (halfHeight * 0.35F), 0, 1);
			float unstable = 1 - flicker * (0.5F + 0.5F * Mth.sin(7 * theta + time * 9 + seed));
			outX[index] = x;
			outY[index] = y;
			outBreadth[index] = rim * (0.7F + 0.3F * noise) * (1 - 0.45F * dissolve);
			outFade[index] = Mth.clamp(alpha * gaps * bottom * unstable * (1 - 0.7F * dissolve), 0, 1);
		}

		/** A faixa neste ponto: de dentro (0-2) ate fora (3-5), com a borda perpendicular a curva, mesmo nas linguas. */
		private float[] band(int index, float[] x, float[] y, float[] breadth, float keep, Vector3f right, Vector3f middle) {
			int before = (index + SEGMENTS - 1) % SEGMENTS;
			int after = (index + 1) % SEGMENTS;
			float tangentX = x[after] - x[before];
			float tangentY = y[after] - y[before];
			float length = Math.max(1.0E-5F, Mth.sqrt(tangentX * tangentX + tangentY * tangentY));
			float outerX = x[index] + tangentY / length * breadth[index];
			float outerY = y[index] - tangentX / length * breadth[index];
			float innerX = x[index] * keep;
			float innerY = y[index] * keep;
			return new float[] {
					middle.x + right.x * innerX + axis.x * innerY,
					middle.y + right.y * innerX + axis.y * innerY,
					middle.z + right.z * innerX + axis.z * innerY,
					middle.x + right.x * outerX + axis.x * outerY,
					middle.y + right.y * outerX + axis.y * outerY,
					middle.z + right.z * outerX + axis.z * outerY};
		}

		/** Deixa a curva mais cheia que uma elipse nos lados. */
		private static float shaped(float value) {
			return Math.signum(value) * (float) Math.pow(Math.abs(value), 0.8);
		}
	}

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

	/** Quadrado virado para quem olha, numa pose qualquer. */
	private static void billboard(PoseStack.Pose entry, VertexConsumer buffer, Vector3f center, Vector3f viewer, float size, int color, float alpha) {
		Vector3f normal = new Vector3f(viewer).sub(center).normalize();
		Vector3f right = new Vector3f(0, 1, 0).cross(normal);
		if (right.lengthSquared() < 1.0E-6F) {
			right.set(1, 0, 0);
		}
		right.normalize().mul(size / 2);
		Vector3f up = new Vector3f(normal).cross(right).normalize().mul(size / 2);
		quad(entry, buffer, center.x, center.y, center.z, right, up, color, alpha);
	}

	/** Quadrado com a rotacao da camera, na pose do mundo. */
	private static void billboard(PoseStack.Pose entry, VertexConsumer buffer, Quaternionf rotation, float x, float y, float z, float size, int color, float alpha) {
		quad(entry, buffer, x, y, z, rotation.transform(new Vector3f(size / 2, 0, 0)), rotation.transform(new Vector3f(0, size / 2, 0)), color, alpha);
	}

	private static void quad(PoseStack.Pose entry, VertexConsumer buffer, float x, float y, float z, Vector3f right, Vector3f up, int color, float alpha) {
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

	private static int mixWhite(int color, float amount) {
		return channels(color, value -> value + (255 - value) * amount);
	}

	private static int scaled(int color, float amount) {
		return channels(color, value -> value * amount);
	}

	private static int channels(int color, Channel channel) {
		int red = (int) channel.apply(color >> 16 & 0xFF);
		int green = (int) channel.apply(color >> 8 & 0xFF);
		int blue = (int) channel.apply(color & 0xFF);
		return red << 16 | green << 8 | blue;
	}

	private interface Channel {
		float apply(float value);
	}

	/** Numero fixo entre 0 e 1 para cada faixa e cada propriedade: a aura muda sem precisar guardar estado. */
	private static float hash(int index, int salt) {
		float value = Mth.sin(index * 12.9898F + salt * 78.233F) * 43758.547F;
		return value - Mth.floor(value);
	}
}
