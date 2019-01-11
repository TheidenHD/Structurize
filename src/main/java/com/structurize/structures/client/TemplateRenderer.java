package com.structurize.structures.client;

import com.structurize.coremod.Structurize;
import com.structurize.structures.lib.RenderUtil;
import com.structurize.structures.lib.TemplateUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Vector3d;
import net.minecraft.client.renderer.texture.ITextureObject;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.entity.Entity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Mirror;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.gen.structure.template.Template;

import java.util.List;

import static org.lwjgl.opengl.GL11.*;

/**
 * The renderer for templates.
 * Holds all information required to render a Template.
 */
public class TemplateRenderer
{
    private static final float HALF_PERCENT_SHRINK               = 0.995F;

    private final TemplateBlockAccess blockAccess;
    private final List<TileEntity>    tileEntities;
    private final List<Entity>        entities;
    private final TemplateTessellator tessellator;
    private final BlockPos            primaryBlockOffset;

    /**
     * Static factory utility method to handle the extraction of the values from the template.
     *
     * @param template The template to create an instance for.
     * @return The renderer.
     */
    public static TemplateRenderer buildRendererForTemplate(Template template)
    {
        final TemplateBlockAccess blockAccess = new TemplateBlockAccess(template);
        final List<TileEntity> tileEntities = TemplateUtils.instantiateTileEntities(template, blockAccess);
        final List<Entity> entities = TemplateUtils.instantiateEntities(template, blockAccess);
        final TemplateTessellator templateTessellator = new TemplateTessellator();
        final BlockPos primaryBlockOffset = TemplateUtils.getPrimaryBlockOffset(template);

        return new TemplateRenderer(blockAccess, tileEntities, entities, templateTessellator, primaryBlockOffset);
    }

    private TemplateRenderer(
      final TemplateBlockAccess blockAccess,
      final List<TileEntity> tileEntities,
      final List<Entity> entities,
      final TemplateTessellator tessellator,
      final BlockPos primaryBlockOffset)
    {
        this.blockAccess = blockAccess;
        this.tileEntities = tileEntities;
        this.entities = entities;
        this.tessellator = tessellator;
        this.primaryBlockOffset = primaryBlockOffset;

        this.setup();
    }

    /**
     * Sets up the renders VBO
     */
    private void setup()
    {
        tessellator.startBuilding();

        blockAccess.getTemplate().blocks.stream()
          .map(b -> TemplateBlockInfoTransformHandler.getInstance().Transform(b))
          .forEach(b -> Minecraft.getMinecraft().getBlockRendererDispatcher().renderBlock(b.blockState, b.pos, blockAccess, tessellator.getBuilder()));

        tessellator.finishBuilding();
    }

    /**
     * Draws an instance of the template at the given position, with the given rotation, and mirroring.
     *
     * @param rotation The rotation.
     * @param mirror The mirroring.
     * @param drawingOffset The drawing offset.
     * @param partialTicks The partial tick time.
     * @param pos The position.
     */
    public void draw(final Rotation rotation, final Mirror mirror, final Vector3d drawingOffset, final float partialTicks, final BlockPos pos)
    {
        //Handle things like mirror, rotation and offset.
        preTemplateDraw(rotation, mirror, drawingOffset, primaryBlockOffset);

        //Draw normal blocks.
        tessellator.draw();

        //Draw tile entities.
        tileEntities.forEach(tileEntity -> TileEntityRendererDispatcher.instance.render(tileEntity, partialTicks, 0));

        //Draw entities
        entities.forEach(entity -> Minecraft.getMinecraft().getRenderManager().renderEntityStatic(entity, partialTicks, true));

        postTemplateDraw();
    }

    private static void preTemplateDraw(final Rotation rotation, final Mirror mirror, final Vector3d drawingOffset, final BlockPos inTemplateOffset)
    {
        final ITextureObject textureObject = Minecraft.getMinecraft().getTextureMapBlocks();
        GlStateManager.bindTexture(textureObject.getGlTextureId());

        GlStateManager.pushMatrix();
        GlStateManager.translate(drawingOffset.x, drawingOffset.y, drawingOffset.z);

        final BlockPos rotateInTemplateOffset = inTemplateOffset.rotate(rotation);
        GlStateManager.translate(-rotateInTemplateOffset.getX(), -rotateInTemplateOffset.getY(), -rotateInTemplateOffset.getZ());

        RenderUtil.applyRotationToYAxis(rotation);
        RenderUtil.applyMirror(mirror, inTemplateOffset);

        GlStateManager.scale(HALF_PERCENT_SHRINK, HALF_PERCENT_SHRINK, HALF_PERCENT_SHRINK);
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        GlStateManager.resetColor();
        GlStateManager.pushMatrix();
    }

    private static void postTemplateDraw()
    {
        GlStateManager.popMatrix();
        GlStateManager.resetColor();
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }

    public TemplateBlockAccess getBlockAccess()
    {
        return blockAccess;
    }

    public List<TileEntity> getTileEntities()
    {
        return tileEntities;
    }

    public TemplateTessellator getTessellator()
    {
        return tessellator;
    }

    public BlockPos getPrimaryBlockOffset()
    {
        return primaryBlockOffset;
    }

}
