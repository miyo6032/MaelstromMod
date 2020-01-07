package com.barribob.MaelstromMod.entity.render;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderItem;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.entity.Entity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * 
 * Makes a projectile registered with this class render with the given item
 * 
 **/
@SideOnly(Side.CLIENT)
public class RenderProjectile<T extends Entity> extends Render<T>
{
    private final RenderItem itemRenderer;
    private final Item itemToRender;

    public RenderProjectile(RenderManager renderManagerIn, RenderItem itemRendererIn, Item item)
    {
	super(renderManagerIn);
	this.itemRenderer = itemRendererIn;
	this.itemToRender = item;
    }

    /**
     * Renders the desired {@code T} type Entity.
     */
    @Override
    public void doRender(T entity, double x, double y, double z, float entityYaw, float partialTicks)
    {
	GlStateManager.pushMatrix();
	GlStateManager.translate((float) x, (float) y, (float) z);
	GlStateManager.enableRescaleNormal();
	GlStateManager.rotate(-this.renderManager.playerViewY, 0.0F, 1.0F, 0.0F);
	GlStateManager.rotate((this.renderManager.options.thirdPersonView == 2 ? -1 : 1) * this.renderManager.playerViewX, 1.0F, 0.0F, 0.0F);
	GlStateManager.rotate(180.0F, 0.0F, 1.0F, 0.0F);
	this.bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);

	this.itemRenderer.renderItem(new ItemStack(this.itemToRender), ItemCameraTransforms.TransformType.GROUND);

	GlStateManager.disableRescaleNormal();
	GlStateManager.popMatrix();
	super.doRender(entity, x, y, z, entityYaw, partialTicks);
    }

    /**
     * Returns the location of an entity's texture. Doesn't seem to be called unless
     * you call Render.bindEntityTexture.
     */
    @Override
    protected ResourceLocation getEntityTexture(Entity entity)
    {
	return TextureMap.LOCATION_BLOCKS_TEXTURE;
    }
}