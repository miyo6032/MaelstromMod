package com.barribob.MaelstromMod.entity.entities.gauntlet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import com.barribob.MaelstromMod.entity.ai.AIAerialTimedAttack;
import com.barribob.MaelstromMod.entity.ai.FlyingMoveHelper;
import com.barribob.MaelstromMod.entity.entities.EntityLeveledMob;
import com.barribob.MaelstromMod.entity.entities.EntityMaelstromMob;
import com.barribob.MaelstromMod.entity.util.IAttack;
import com.barribob.MaelstromMod.init.ModBBAnimations;
import com.barribob.MaelstromMod.util.ModDamageSource;
import com.barribob.MaelstromMod.util.ModRandom;
import com.barribob.MaelstromMod.util.ModUtils;

import net.minecraft.block.Block;
import net.minecraft.block.BlockFence;
import net.minecraft.block.BlockFenceGate;
import net.minecraft.block.BlockWall;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.crash.CrashReport;
import net.minecraft.crash.CrashReportCategory;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.IEntityMultiPart;
import net.minecraft.entity.MoverType;
import net.minecraft.entity.MultiPartEntityPart;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.init.SoundEvents;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.pathfinding.PathNavigateFlying;
import net.minecraft.util.DamageSource;
import net.minecraft.util.ReportedException;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class EntityMaelstromGauntlet extends EntityMaelstromMob implements IAttack, IEntityMultiPart
{
    // We keep track of the look ourselves because minecraft's look is clamped
    protected static final DataParameter<Float> LOOK = EntityDataManager.<Float>createKey(EntityLeveledMob.class, DataSerializers.FLOAT);
    private MultiPartEntityPart[] hitboxParts;
    private float boxSize = 0.8f;
    private MultiPartEntityPart eye = new MultiPartEntityPart(this, "eye", boxSize, boxSize);
    private MultiPartEntityPart bottomPalm = new MultiPartEntityPart(this, "bottomPalm", 1.2f, 1.2f);
    private MultiPartEntityPart upLeftPalm = new MultiPartEntityPart(this, "upLeftPalm", boxSize, boxSize);
    private MultiPartEntityPart upRightPalm = new MultiPartEntityPart(this, "upRightPalm", boxSize, boxSize);
    private MultiPartEntityPart rightPalm = new MultiPartEntityPart(this, "rightPalm", boxSize, boxSize);
    private MultiPartEntityPart leftPalm = new MultiPartEntityPart(this, "leftPalm", boxSize, boxSize);
    private MultiPartEntityPart fingers = new MultiPartEntityPart(this, "fingers", 1.2f, 1.2f);
    private MultiPartEntityPart fist = new MultiPartEntityPart(this, "fist", 0, 0);
    private Consumer<EntityLivingBase> prevAttack;
    private boolean isPunching;
    private Vec3d targetPos;

    private final Consumer<EntityLivingBase> punch = (target) -> {
	ModBBAnimations.animation(this, "gauntlet.punch", false);
	this.targetPos = target.getPositionVector().add(ModUtils.yVec(1));
	this.addVelocity(0, 0.5, 0);
	this.addEvent(() -> {
	    this.isPunching = true;
	    this.fist.width = 2.5f;
	    this.fist.height = 2f;
	    this.height = 2;
	}, 20);
	this.addEvent(() -> {
	    this.isPunching = false;
	}, 40);
	this.addEvent(() -> {
	    this.fist.width = 0;
	    this.fist.height = 0;
	    this.height = 4;
	}, 50);
    };

    public EntityMaelstromGauntlet(World worldIn)
    {
	super(worldIn);
	this.moveHelper = new FlyingMoveHelper(this);
	this.navigator = new PathNavigateFlying(this, worldIn);
	this.hitboxParts = new MultiPartEntityPart[] { eye, bottomPalm, upLeftPalm, upRightPalm, rightPalm, leftPalm, fingers, fist };
	this.setSize(2, 4);
	this.noClip = true;
	this.isImmuneToFire = true;
    }

    @Override
    protected void applyEntityAttributes()
    {
	super.applyEntityAttributes();
	this.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(250);
	this.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(0.26f);
	this.getEntityAttribute(SharedMonsterAttributes.FOLLOW_RANGE).setBaseValue(64);
	this.getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).setBaseValue(9f);
	this.getEntityAttribute(SharedMonsterAttributes.KNOCKBACK_RESISTANCE).setBaseValue(1);
    }

    @Override
    protected void initEntityAI()
    {
	super.initEntityAI();
	this.tasks.addTask(4, new AIAerialTimedAttack<EntityMaelstromGauntlet>(this, 1.0f, 60, 20, 0.8f));
    }

    @Override
    public int startAttack(EntityLivingBase target, float distanceSq, boolean strafingBackwards)
    {
	List<Consumer<EntityLivingBase>> attacks = new ArrayList<Consumer<EntityLivingBase>>(Arrays.asList(punch));
	double[] weights = {
		1
	};
	this.prevAttack = ModRandom.choice(attacks, rand, weights).next();
	this.prevAttack.accept(target);
	return 100;
    }

    @Override
    public boolean attackEntityFromPart(MultiPartEntityPart part, DamageSource source, float damage)
    {
	if (part == this.eye)
	{
	    return this.attackEntityFrom(source, damage);
	}

	if (damage > 0.0F && !source.isUnblockable())
	{
	    if (!source.isProjectile())
	    {
		Entity entity = source.getImmediateSource();

		if (entity instanceof EntityLivingBase)
		{
		    this.blockUsingShield((EntityLivingBase) entity);
		}
	    }
	    this.playSound(SoundEvents.ENTITY_BLAZE_HURT, 1.0f, 0.6f + ModRandom.getFloat(0.2f));

	    return false;
	}

	return false;
    }

    @Override
    public void onLivingUpdate()
    {
	super.onLivingUpdate();
	Vec3d[] avec3d = new Vec3d[this.hitboxParts.length];
	for (int j = 0; j < this.hitboxParts.length; ++j)
	{
	    avec3d[j] = new Vec3d(this.hitboxParts[j].posX, this.hitboxParts[j].posY, this.hitboxParts[j].posZ);
	}

	/**
	 * Set the hitbox pieces based on the entity's rotation so that even large pitch rotations don't mess up the hitboxes
	 */
	Vec3d lookVec = ModUtils.getLookVec(this.getLook(), this.rotationYaw);
	Vec3d rotationVector = ModUtils.rotateVector(lookVec, lookVec.crossProduct(new Vec3d(0, 1, 0)), 90);

	Vec3d eyePos = this.getPositionEyes(1).add(rotationVector.scale(-0.5)).add(ModUtils.getAxisOffset(lookVec, new Vec3d(-0.2, 0, 0)));
	this.eye.setLocationAndAngles(eyePos.x, eyePos.y, eyePos.z, this.rotationYaw, this.rotationPitch);

	Vec3d palmPos = this.getPositionEyes(1).add(rotationVector.scale(0.5)).add(ModUtils.getAxisOffset(lookVec, new Vec3d(0, 0, 0.5)));
	this.upLeftPalm.setLocationAndAngles(palmPos.x, palmPos.y, palmPos.z, this.rotationYaw, this.rotationPitch);

	palmPos = this.getPositionEyes(1).add(rotationVector.scale(0.5)).add(ModUtils.getAxisOffset(lookVec, new Vec3d(0, 0, -0.5)));
	this.upRightPalm.setLocationAndAngles(palmPos.x, palmPos.y, palmPos.z, this.rotationYaw, this.rotationPitch);

	palmPos = this.getPositionEyes(1).add(rotationVector.scale(-1.7));
	this.bottomPalm.setLocationAndAngles(palmPos.x, palmPos.y, palmPos.z, this.rotationYaw, this.rotationPitch);

	palmPos = this.getPositionEyes(1).add(rotationVector.scale(-0.4)).add(ModUtils.getAxisOffset(lookVec, new Vec3d(0, 0, 0.7)));
	this.leftPalm.setLocationAndAngles(palmPos.x, palmPos.y, palmPos.z, this.rotationYaw, this.rotationPitch);

	palmPos = this.getPositionEyes(1).add(rotationVector.scale(-0.4)).add(ModUtils.getAxisOffset(lookVec, new Vec3d(0, 0, -0.7)));
	this.rightPalm.setLocationAndAngles(palmPos.x, palmPos.y, palmPos.z, this.rotationYaw, this.rotationPitch);

	palmPos = this.getPositionEyes(1).add(rotationVector.scale(1.3));
	this.fingers.setLocationAndAngles(palmPos.x, palmPos.y, palmPos.z, this.rotationYaw, this.rotationPitch);

	Vec3d fistPos = this.getPositionVector().subtract(ModUtils.yVec(0.5));
	this.fist.setLocationAndAngles(fistPos.x, fistPos.y, fistPos.z, this.rotationYaw, this.rotationPitch);

	for (int l = 0; l < this.hitboxParts.length; ++l)
	{
	    this.hitboxParts[l].prevPosX = avec3d[l].x;
	    this.hitboxParts[l].prevPosY = avec3d[l].y;
	    this.hitboxParts[l].prevPosZ = avec3d[l].z;
	}

	if (this.isPunching)
	{
	    ModUtils.destroyBlocksInAABB(this.fist.getEntityBoundingBox(), world, this);
	    Vec3d dir = this.targetPos.subtract(this.getPositionVector()).normalize().scale(0.2);
	    this.addVelocity(dir.x, dir.y, dir.z);
	    ModUtils.handleAreaImpact(1.3f, (e) -> this.getAttack(), this, this.getPositionEyes(1), ModDamageSource.causeElementalMeleeDamage(this, this.getElement()), 1.5f, 0, false);
	}
    }

    @Override
    public float getEyeHeight()
    {
	return 1.6f;
    }

    public EntityLeveledMob setLook(Vec3d look)
    {
	float prevLook = this.getLook();
	float newLook = (float) ModUtils.toPitch(look);
	float deltaLook = 1;
	float clampedLook = MathHelper.clamp(newLook, prevLook - deltaLook, prevLook + deltaLook);
	this.dataManager.set(LOOK, clampedLook);
	return this;
    }

    public float getLook()
    {
	return this.dataManager == null ? 0 : this.dataManager.get(LOOK);
    }

    @Override
    public void travel(float strafe, float vertical, float forward)
    {
	if (this.isInWater())
	{
	    this.moveRelative(strafe, vertical, forward, 0.02F);
	    this.move(MoverType.SELF, this.motionX, this.motionY, this.motionZ);
	    this.motionX *= 0.800000011920929D;
	    this.motionY *= 0.800000011920929D;
	    this.motionZ *= 0.800000011920929D;
	}
	else if (this.isInLava())
	{
	    this.moveRelative(strafe, vertical, forward, 0.02F);
	    this.move(MoverType.SELF, this.motionX, this.motionY, this.motionZ);
	    this.motionX *= 0.5D;
	    this.motionY *= 0.5D;
	    this.motionZ *= 0.5D;
	}
	else
	{
	    float f = 0.91F;

	    if (this.onGround)
	    {
		BlockPos underPos = new BlockPos(MathHelper.floor(this.posX), MathHelper.floor(this.getEntityBoundingBox().minY) - 1, MathHelper.floor(this.posZ));
		IBlockState underState = this.world.getBlockState(underPos);
		f = underState.getBlock().getSlipperiness(underState, this.world, underPos, this) * 0.91F;
	    }

	    float f1 = 0.16277136F / (f * f * f);
	    this.moveRelative(strafe, vertical, forward, this.onGround ? 0.1F * f1 : 0.02F);
	    f = 0.91F;

	    if (this.onGround)
	    {
		BlockPos underPos = new BlockPos(MathHelper.floor(this.posX), MathHelper.floor(this.getEntityBoundingBox().minY) - 1, MathHelper.floor(this.posZ));
		IBlockState underState = this.world.getBlockState(underPos);
		f = underState.getBlock().getSlipperiness(underState, this.world, underPos, this) * 0.91F;
	    }

	    this.move(MoverType.SELF, this.motionX, this.motionY, this.motionZ);
	    this.motionX *= f;
	    this.motionY *= f;
	    this.motionZ *= f;
	}

	this.prevLimbSwingAmount = this.limbSwingAmount;
	double d1 = this.posX - this.prevPosX;
	double d0 = this.posZ - this.prevPosZ;
	float f2 = MathHelper.sqrt(d1 * d1 + d0 * d0) * 4.0F;

	if (f2 > 1.0F)
	{
	    f2 = 1.0F;
	}

	this.limbSwingAmount += (f2 - this.limbSwingAmount) * 0.4F;
	this.limbSwing += this.limbSwingAmount;
    }

    @Override
    protected void entityInit()
    {
	this.dataManager.register(LOOK, Float.valueOf(0));
	super.entityInit();
    }

    @Override
    public World getWorld()
    {
	return world;
    }

    @Override
    public Entity[] getParts()
    {
	return this.hitboxParts;
    }

    @Override
    public void fall(float distance, float damageMultiplier)
    {
    }

    @Override
    protected void updateFallState(double y, boolean onGroundIn, IBlockState state, BlockPos pos)
    {
    }

    @Override
    public boolean isOnLadder()
    {
	return false;
    }

    @Override
    public void attackEntityWithRangedAttack(EntityLivingBase target, float distanceFactor)
    {
    }

    @Override
    public boolean canBeCollidedWith()
    {
	return false;
    }

    /**
     * This is overriden because we do want the main hitbox to clip with blocks while still not clipping with anything else
     */
    @Override
    public void move(MoverType type, double x, double y, double z)
    {
	this.world.profiler.startSection("move");

	if (this.isInWeb)
	{
	    this.isInWeb = false;
	    x *= 0.25D;
	    y *= 0.05000000074505806D;
	    z *= 0.25D;
	    this.motionX = 0.0D;
	    this.motionY = 0.0D;
	    this.motionZ = 0.0D;
	}

	double d2 = x;
	double d3 = y;
	double d4 = z;

	List<AxisAlignedBB> list1 = this.world.getCollisionBoxes(this, this.getEntityBoundingBox().expand(x, y, z));
	AxisAlignedBB axisalignedbb = this.getEntityBoundingBox();

	if (y != 0.0D)
	{
	    int k = 0;

	    for (int l = list1.size(); k < l; ++k)
	    {
		y = list1.get(k).calculateYOffset(this.getEntityBoundingBox(), y);
	    }

	    this.setEntityBoundingBox(this.getEntityBoundingBox().offset(0.0D, y, 0.0D));
	}

	if (x != 0.0D)
	{
	    int j5 = 0;

	    for (int l5 = list1.size(); j5 < l5; ++j5)
	    {
		x = list1.get(j5).calculateXOffset(this.getEntityBoundingBox(), x);
	    }

	    if (x != 0.0D)
	    {
		this.setEntityBoundingBox(this.getEntityBoundingBox().offset(x, 0.0D, 0.0D));
	    }
	}

	if (z != 0.0D)
	{
	    int k5 = 0;

	    for (int i6 = list1.size(); k5 < i6; ++k5)
	    {
		z = list1.get(k5).calculateZOffset(this.getEntityBoundingBox(), z);
	    }

	    if (z != 0.0D)
	    {
		this.setEntityBoundingBox(this.getEntityBoundingBox().offset(0.0D, 0.0D, z));
	    }
	}

	boolean flag = this.onGround || d3 != y && d3 < 0.0D;

	if (this.stepHeight > 0.0F && flag && (d2 != x || d4 != z))
	{
	    double d14 = x;
	    double d6 = y;
	    double d7 = z;
	    AxisAlignedBB axisalignedbb1 = this.getEntityBoundingBox();
	    this.setEntityBoundingBox(axisalignedbb);
	    y = this.stepHeight;
	    List<AxisAlignedBB> list = this.world.getCollisionBoxes(this, this.getEntityBoundingBox().expand(d2, y, d4));
	    AxisAlignedBB axisalignedbb2 = this.getEntityBoundingBox();
	    AxisAlignedBB axisalignedbb3 = axisalignedbb2.expand(d2, 0.0D, d4);
	    double d8 = y;
	    int j1 = 0;

	    for (int k1 = list.size(); j1 < k1; ++j1)
	    {
		d8 = list.get(j1).calculateYOffset(axisalignedbb3, d8);
	    }

	    axisalignedbb2 = axisalignedbb2.offset(0.0D, d8, 0.0D);
	    double d18 = d2;
	    int l1 = 0;

	    for (int i2 = list.size(); l1 < i2; ++l1)
	    {
		d18 = list.get(l1).calculateXOffset(axisalignedbb2, d18);
	    }

	    axisalignedbb2 = axisalignedbb2.offset(d18, 0.0D, 0.0D);
	    double d19 = d4;
	    int j2 = 0;

	    for (int k2 = list.size(); j2 < k2; ++j2)
	    {
		d19 = list.get(j2).calculateZOffset(axisalignedbb2, d19);
	    }

	    axisalignedbb2 = axisalignedbb2.offset(0.0D, 0.0D, d19);
	    AxisAlignedBB axisalignedbb4 = this.getEntityBoundingBox();
	    double d20 = y;
	    int l2 = 0;

	    for (int i3 = list.size(); l2 < i3; ++l2)
	    {
		d20 = list.get(l2).calculateYOffset(axisalignedbb4, d20);
	    }

	    axisalignedbb4 = axisalignedbb4.offset(0.0D, d20, 0.0D);
	    double d21 = d2;
	    int j3 = 0;

	    for (int k3 = list.size(); j3 < k3; ++j3)
	    {
		d21 = list.get(j3).calculateXOffset(axisalignedbb4, d21);
	    }

	    axisalignedbb4 = axisalignedbb4.offset(d21, 0.0D, 0.0D);
	    double d22 = d4;
	    int l3 = 0;

	    for (int i4 = list.size(); l3 < i4; ++l3)
	    {
		d22 = list.get(l3).calculateZOffset(axisalignedbb4, d22);
	    }

	    axisalignedbb4 = axisalignedbb4.offset(0.0D, 0.0D, d22);
	    double d23 = d18 * d18 + d19 * d19;
	    double d9 = d21 * d21 + d22 * d22;

	    if (d23 > d9)
	    {
		x = d18;
		z = d19;
		y = -d8;
		this.setEntityBoundingBox(axisalignedbb2);
	    }
	    else
	    {
		x = d21;
		z = d22;
		y = -d20;
		this.setEntityBoundingBox(axisalignedbb4);
	    }

	    int j4 = 0;

	    for (int k4 = list.size(); j4 < k4; ++j4)
	    {
		y = list.get(j4).calculateYOffset(this.getEntityBoundingBox(), y);
	    }

	    this.setEntityBoundingBox(this.getEntityBoundingBox().offset(0.0D, y, 0.0D));

	    if (d14 * d14 + d7 * d7 >= x * x + z * z)
	    {
		x = d14;
		y = d6;
		z = d7;
		this.setEntityBoundingBox(axisalignedbb1);
	    }
	}

	this.world.profiler.endSection();
	this.world.profiler.startSection("rest");
	this.resetPositionToBB();
	this.collidedHorizontally = d2 != x || d4 != z;
	this.collidedVertically = d3 != y;
	this.onGround = this.collidedVertically && d3 < 0.0D;
	this.collided = this.collidedHorizontally || this.collidedVertically;
	int j6 = MathHelper.floor(this.posX);
	int i1 = MathHelper.floor(this.posY - 0.20000000298023224D);
	int k6 = MathHelper.floor(this.posZ);
	BlockPos blockpos = new BlockPos(j6, i1, k6);
	IBlockState iblockstate = this.world.getBlockState(blockpos);

	if (iblockstate.getMaterial() == Material.AIR)
	{
	    BlockPos blockpos1 = blockpos.down();
	    IBlockState iblockstate1 = this.world.getBlockState(blockpos1);
	    Block block1 = iblockstate1.getBlock();

	    if (block1 instanceof BlockFence || block1 instanceof BlockWall || block1 instanceof BlockFenceGate)
	    {
		iblockstate = iblockstate1;
		blockpos = blockpos1;
	    }
	}

	this.updateFallState(y, this.onGround, iblockstate, blockpos);

	if (d2 != x)
	{
	    this.motionX = 0.0D;
	}

	if (d4 != z)
	{
	    this.motionZ = 0.0D;
	}

	Block block = iblockstate.getBlock();

	if (d3 != y)
	{
	    block.onLanded(this.world, this);
	}

	try
	{
	    this.doBlockCollisions();
	}
	catch (Throwable throwable)
	{
	    CrashReport crashreport = CrashReport.makeCrashReport(throwable, "Checking entity block collision");
	    CrashReportCategory crashreportcategory = crashreport.makeCategory("Entity being checked for collision");
	    this.addEntityCrashInfo(crashreportcategory);
	    throw new ReportedException(crashreport);
	}

	this.world.profiler.endSection();
    }
}
