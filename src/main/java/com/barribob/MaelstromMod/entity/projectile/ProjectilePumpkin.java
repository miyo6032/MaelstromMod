package com.barribob.MaelstromMod.entity.projectile;

import com.barribob.MaelstromMod.util.ModColors;
import com.barribob.MaelstromMod.util.ModRandom;
import com.barribob.MaelstromMod.util.ModUtils;
import com.barribob.MaelstromMod.util.handlers.ParticleManager;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * 
 * The projectile for the maelstrom cannon item
 *
 */
public class ProjectilePumpkin extends ProjectileGun
{
    private static final int PARTICLE_AMOUNT = 1;
    private static final int IMPACT_PARTICLE_AMOUNT = 50;
    private static final int EXPOSION_AREA_FACTOR = 4;
    private int rings;
    private int maxRings;
    Vec3d prevPos;

    public ProjectilePumpkin(World worldIn, EntityLivingBase throwerIn, float baseDamage, ItemStack stack)
    {
	super(worldIn, throwerIn, baseDamage, stack);
	this.prevPos = this.getPositionVector();
    }

    public ProjectilePumpkin(World worldIn)
    {
	super(worldIn);
	this.prevPos = this.getPositionVector();
    }

    public ProjectilePumpkin(World worldIn, double x, double y, double z)
    {
	super(worldIn, x, y, z);
    }

    /**
     * Called every update to spawn particles
     * 
     * @param world
     */
    @Override
    protected void spawnParticles()
    {

	maxRings = ModRandom.range(4, 7);
	float tailWidth = 0.25f;
	for (int i = 0; i < 5; i++)
	{
	    ParticleManager.spawnFirework(world,
		    new Vec3d(this.posX, this.posY, this.posZ).add(new Vec3d(ModRandom.getFloat(tailWidth), ModRandom.getFloat(tailWidth), ModRandom.getFloat(tailWidth))),
		    new Vec3d(0.9, 0.9, 0.5));
	}
	if (this.rings < this.maxRings)
	{
	    float circleSize = Math.max(0, ModRandom.getFloat(1.5f) + 1);
	    Vec3d vel = this.getPositionVector().subtract(this.prevPos).normalize();
	    float f1 = MathHelper.sqrt(vel.x * vel.x + vel.z * vel.z);
	    ParticleManager.spawnParticlesInCircle(circleSize, 30, (pos) -> {

		// Conversion code taken from projectile shoot method
		Vec3d outer = pos.rotatePitch((float) (MathHelper.atan2(vel.y, f1))).rotateYaw((float) (MathHelper.atan2(vel.x, vel.z)))
			.add(getPositionVector());
		ParticleManager.spawnEffect(world, outer, ModColors.YELLOW);
	    });
	    this.rings++;
	}

	this.prevPos = this.getPositionVector();
    }

    @Override
    protected void onHit(RayTraceResult result)
    {
	ModUtils.handleBulletImpact(result.entityHit, this, (float) (this.getGunDamage(result.entityHit) * this.getDistanceTraveled()),
		DamageSource.causeThrownDamage(this, this.shootingEntity), this.getKnockback());
	super.onHit(result);
    }
}