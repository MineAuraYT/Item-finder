package com.mineaura.itemfinder;

import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelExtractionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.StagedVertexBuffer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.Optional;
import java.util.OptionalDouble;

/** Renders a translucent purple highlight around a found container, including through walls. */
public final class ContainerHighlightRenderer {
    private static final RenderPipeline HIGHLIGHT_PIPELINE = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
                    .withLocation(Identifier.fromNamespaceAndPath("itemfinder", "pipeline/container_highlight"))
                    .withDepthStencilState(Optional.empty())
                    .build()
    );

    private static final Vector4f COLOR_MODULATOR = new Vector4f(1f, 1f, 1f, 1f);
    private static final Vector3f MODEL_OFFSET = new Vector3f();
    private static final Matrix4f TEXTURE_MATRIX = new Matrix4f();
    private static final StagedVertexBuffer BUFFER = new StagedVertexBuffer(
            () -> "Item Finder highlight buffer", RenderType.SMALL_BUFFER_SIZE);

    private static BlockPos activePosition;
    private static ResourceKey<Level> activeDimension;
    private static int ticksRemaining;
    private static HighlightState renderState;

    private ContainerHighlightRenderer() { }

    public static void register() {
        LevelExtractionEvents.END_EXTRACTION.register(context -> {
            BlockPos pos = activePosition;
            renderState = pos == null || ticksRemaining <= 0
                    ? null : new HighlightState(pos.getX(), pos.getY(), pos.getZ());
        });
        LevelRenderEvents.AFTER_TRANSLUCENT_TERRAIN.register(ContainerHighlightRenderer::render);
    }

    public static void show(BlockPos position, ResourceKey<Level> dimension) {
        activePosition = position.immutable();
        activeDimension = dimension;
        ticksRemaining = 120; // Six seconds at 20 ticks per second.
    }

    public static void tick(Minecraft client) {
        if (activePosition == null) return;
        if (client.level == null || client.player == null
                || activeDimension == null || !client.level.dimension().equals(activeDimension)
                || --ticksRemaining <= 0) {
            clear();
        }
    }

    public static void clear() {
        activePosition = null;
        activeDimension = null;
        ticksRemaining = 0;
        renderState = null;
    }

    private static void render(LevelRenderContext context) {
        HighlightState state = renderState;
        if (state == null) return;

        VertexFormat format = HIGHLIGHT_PIPELINE.getVertexFormatBinding(0);
        if (format == null) return;

        PrimitiveTopology topology = HIGHLIGHT_PIPELINE.getPrimitiveTopology();
        StagedVertexBuffer.Draw draw = BUFFER.appendDraw(format, topology,
                topology == PrimitiveTopology.QUADS
                        ? RenderSystem.getProjectionType().vertexSorting() : null);

        PoseStack poses = context.poseStack();
        Vec3 camera = context.levelState().cameraRenderState.pos;
        poses.pushPose();
        poses.translate(-camera.x, -camera.y, -camera.z);
        VertexConsumer vertices = BUFFER.getVertexBuilder(draw);

        // A soft outer shell and a brighter inner shell make the container visibly glow.
        addBox(poses.last().pose(), vertices,
                state.x() - 0.055f, state.y() - 0.055f, state.z() - 0.055f,
                state.x() + 1.055f, state.y() + 1.055f, state.z() + 1.055f,
                0.48f, 0.12f, 1.0f, 0.16f);
        addBox(poses.last().pose(), vertices,
                state.x() - 0.018f, state.y() - 0.018f, state.z() - 0.018f,
                state.x() + 1.018f, state.y() + 1.018f, state.z() + 1.018f,
                0.72f, 0.38f, 1.0f, 0.22f);
        poses.popPose();

        BUFFER.upload();
        StagedVertexBuffer.ExecuteInfo info = BUFFER.getExecuteInfo(draw);
        if (info != null) draw(Minecraft.getInstance(), info);
        BUFFER.endFrame();
    }

    private static void addBox(Matrix4fc matrix, VertexConsumer out,
                               float x0, float y0, float z0, float x1, float y1, float z1,
                               float red, float green, float blue, float alpha) {
        // Front
        vertex(out, matrix, x0, y0, z1, red, green, blue, alpha);
        vertex(out, matrix, x1, y0, z1, red, green, blue, alpha);
        vertex(out, matrix, x1, y1, z1, red, green, blue, alpha);
        vertex(out, matrix, x0, y1, z1, red, green, blue, alpha);
        // Back
        vertex(out, matrix, x1, y0, z0, red, green, blue, alpha);
        vertex(out, matrix, x0, y0, z0, red, green, blue, alpha);
        vertex(out, matrix, x0, y1, z0, red, green, blue, alpha);
        vertex(out, matrix, x1, y1, z0, red, green, blue, alpha);
        // Left
        vertex(out, matrix, x0, y0, z0, red, green, blue, alpha);
        vertex(out, matrix, x0, y0, z1, red, green, blue, alpha);
        vertex(out, matrix, x0, y1, z1, red, green, blue, alpha);
        vertex(out, matrix, x0, y1, z0, red, green, blue, alpha);
        // Right
        vertex(out, matrix, x1, y0, z1, red, green, blue, alpha);
        vertex(out, matrix, x1, y0, z0, red, green, blue, alpha);
        vertex(out, matrix, x1, y1, z0, red, green, blue, alpha);
        vertex(out, matrix, x1, y1, z1, red, green, blue, alpha);
        // Top
        vertex(out, matrix, x0, y1, z1, red, green, blue, alpha);
        vertex(out, matrix, x1, y1, z1, red, green, blue, alpha);
        vertex(out, matrix, x1, y1, z0, red, green, blue, alpha);
        vertex(out, matrix, x0, y1, z0, red, green, blue, alpha);
        // Bottom
        vertex(out, matrix, x0, y0, z0, red, green, blue, alpha);
        vertex(out, matrix, x1, y0, z0, red, green, blue, alpha);
        vertex(out, matrix, x1, y0, z1, red, green, blue, alpha);
        vertex(out, matrix, x0, y0, z1, red, green, blue, alpha);
    }

    private static void vertex(VertexConsumer out, Matrix4fc matrix, float x, float y, float z,
                               float red, float green, float blue, float alpha) {
        out.addVertex(matrix, x, y, z).setColor(red, green, blue, alpha);
    }

    private static void draw(Minecraft client, StagedVertexBuffer.ExecuteInfo info) {
        GpuBufferSlice transforms = RenderSystem.getDynamicUniforms().writeTransform(
                RenderSystem.getModelViewMatrixCopy(), COLOR_MODULATOR, MODEL_OFFSET, TEXTURE_MATRIX);
        RenderTarget target = client.gameRenderer.mainRenderTarget();
        GpuTextureView color = target.getColorTextureView();
        if (color == null) return;

        try (RenderPass pass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(
                () -> "Item Finder container highlight", color, Optional.empty(),
                target.getDepthTextureView(), OptionalDouble.empty())) {
            pass.setPipeline(HIGHLIGHT_PIPELINE);
            RenderSystem.bindDefaultUniforms(pass);
            pass.setUniform("DynamicTransforms", transforms);
            pass.setVertexBuffer(0, info.vertexBuffer().slice());
            pass.setIndexBuffer(info.indexBuffer(), info.indexType());
            pass.drawIndexed(info.indexCount(), 1, info.firstIndex(), info.baseVertex(), 0);
        }
    }

    private record HighlightState(int x, int y, int z) { }
}
