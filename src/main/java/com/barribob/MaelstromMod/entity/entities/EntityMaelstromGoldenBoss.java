package com.barribob.MaelstromMod.entity.entities;

import com.barribob.MaelstromMod.entity.action.ActionFireball;
import com.barribob.MaelstromMod.entity.action.ActionMaelstromRing;
import com.barribob.MaelstromMod.entity.action.ActionSpawnEnemy;
import com.barribob.MaelstromMod.entity.ai.EntityAIRangedAttack;
import com.barribob.MaelstromMod.entity.animation.AnimationMegaMissile;
import com.barribob.MaelstromMod.entity.animation.AnimationOctoMissiles;
import com.barribob.MaelstromMod.entity.animation.AnimationRuneSummon;
import com.barribob.MaelstromMod.entity.util.ComboAttack;
import com.barribob.MaelstromMod.init.ModEntities;
import com.barribob.MaelstromMod.util.ModRandom;
import com.barribob.MaelstromMod.util.ModUtils;
import com.barribob.MaelstromMod.util.handlers.LootTableHandler;
import com.barribob.MaelstromMod.util.handlers.ParticleManager;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.DamageSource;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.BossInfo;
import net.minecraft.world.BossInfoServer;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class EntityMaelstromGoldenBoss extends EntityMaelstromMob
{
    private final BossInfoServer bossInfo = (BossInfoServer) (new BossInfoServer(this.getDisplayName(), BossInfo.Color.YELLOW, BossInfo.Overlay.NOTCHED_6));
    private ComboAttack attackHandler = new ComboAttack();
    private byte spawnEnemy = 4;
    private byte blackFireball = 5;
    private byte runes = 6;

    public EntityMaelstromGoldenBoss(World worldIn)
    {
	super(worldIn);
	this.setLevel(2.5f);
	this.attackHandler.addAttack(spawnEnemy, new ActionSpawnEnemy(() -> new EntityGoldenShade(worldIn)), () -> new AnimationOctoMissiles());
	this.attackHandler.addAttack(blackFireball, new ActionFireball(), () -> new AnimationMegaMissile());
	this.attackHandler.addAttack(runes, new ActionMaelstromRing(), () -> new AnimationRuneSummon());
	this.setSize(1.5f, 3.2f);
	this.experienceValue = ModEntities.BOSS_EXPERIENCE;
    }
    
    @Override
    protected boolean canDespawn()
    {
	return false;
    }

    @Override
    protected void initEntityAI()
    {
	super.initEntityAI();
	this.tasks.addTask(4, new EntityAIRangedAttack<EntityMaelstromGoldenBoss>(this, 1.0f, 40, 20.0f, 0.4f));
    }
    
    @Override
    protected void applyEntityAttributes()
    {
	super.applyEntityAttributes();
	this.getEntityAttribute(SharedMonsterAttributes.FOLLOW_RANGE).setBaseValue(30.0D);
    }

    @Override
    public void attackEntityWithRangedAttack(EntityLivingBase target, float distanceFactor)
    {
	this.attackHandler.getCurrentAttackAction().performAction(this, target);
    }
    
    @Override
    protected ResourceLocation getLootTable()
    {
	return LootTableHandler.GOLDEN_BOSS;
    }

    @Override
    public void onUpdate()
    {
	super.onUpdate();
	this.bossInfo.setPercent(getHealth() / getMaxHealth());
	if (!world.isRemote)
	{
	    if (this.isSwingingArms() && attackHandler.getCurrentAttack() == blackFireball)
	    {
		Vec3d look = this.getVectorForRotation(0, this.rotationYaw);
		ParticleManager.spawnDarkFlames(world, rand, this.getPositionVector().add(ModRandom.randVec().scale(0.5)).add(ModUtils.yVec(this.getEyeHeight())).add(look));
	    }
	}
    }

    @Override
    public void setSwingingArms(boolean swingingArms)
    {
	super.setSwingingArms(swingingArms);
	if (swingingArms)
	{
	    Byte[] attack = { spawnEnemy, blackFireball, runes };
	    attackHandler.setCurrentAttack(ModRandom.choice(attack));
	    world.setEntityState(this, attackHandler.getCurrentAttack());
	}
    }

    @SideOnly(Side.CLIENT)
    public void handleStatusUpdate(byte id)
    {
	if (id >= 4 && id <= 6)
	{
	    currentAnimation = attackHandler.getAnimation(id);
	    currentAnimation.startAnimation();
	}
	else
	{
	    super.handleStatusUpdate(id);
	}
    }

    @Override
    protected void updateAttributes()
    {
	this.setBaseMaxHealth(200);
	this.setBaseAttack(5);
    }
    
    public void setCustomNameTag(String name)
    {
	super.setCustomNameTag(name);
	this.bossInfo.setName(this.getDisplayName());
    }

    public void addTrackingPlayer(EntityPlayerMP player)
    {
	super.addTrackingPlayer(player);
	this.bossInfo.addPlayer(player);
    }

    public void removeTrackingPlayer(EntityPlayerMP player)
    {
	super.removeTrackingPlayer(player);
	this.bossInfo.removePlayer(player);
    }
    
    @Override
    protected SoundEvent getHurtSound(DamageSource damageSourceIn)
    {
	return SoundEvents.BLOCK_METAL_PLACE;
    }

    @Override
    protected SoundEvent getDeathSound()
    {
	return SoundEvents.BLOCK_METAL_BREAK;
    }
}
