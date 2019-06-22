package com.barribob.MaelstromMod.entity.action;

import com.barribob.MaelstromMod.entity.entities.EntityLeveledMob;
import com.barribob.MaelstromMod.entity.projectile.Projectile;
import com.barribob.MaelstromMod.entity.projectile.ProjectileGoldenBullet;
import com.barribob.MaelstromMod.util.ModUtils;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class ActionGoldenMissles extends Action
{
    public static final float PROJECTILE_INACCURACY = 4.0f;
    public static final float PROJECTILE_SPEED = 1.2f;
    
    @Override
    public void performAction(EntityLeveledMob actor, EntityLivingBase target)
    {
	Vec3d look = ModUtils.getVectorForRotation(0, actor.renderYawOffset);
	Vec3d right = look.rotateYaw((float) Math.PI * -0.5f).scale(0.5);
	Vec3d left = look.rotateYaw((float) Math.PI * 0.5f).scale(0.5);
	launch(actor, target, right);
	launch(actor, target, left);
    }
    
    private void launch(EntityLeveledMob actor, EntityLivingBase target, Vec3d offset)
    {
	ProjectileGoldenBullet projectile = new ProjectileGoldenBullet(actor.world, actor, actor.getAttack(), null);
	projectile.posY = actor.posY + actor.getEyeHeight() - 0.5; // Raise pos y to summon the projectile above the head
	projectile.posX += offset.x;
	projectile.posZ += offset.z;
	double d0 = target.posY + (double) target.getEyeHeight() - 0.5;
	double x = target.posX - projectile.posX;
	double y = d0 - projectile.posY;
	double z = target.posZ - projectile.posZ;
	float f = MathHelper.sqrt(x * x + z * z) * 0.2F;
	projectile.shoot(x, y + (double) f, z, this.PROJECTILE_SPEED, this.PROJECTILE_INACCURACY);
	actor.playSound(SoundEvents.ENTITY_BLAZE_SHOOT, 1.0F, 1.3F / (actor.world.rand.nextFloat() * 0.4F + 0.8F));
	actor.world.spawnEntity(projectile);
    }
}
