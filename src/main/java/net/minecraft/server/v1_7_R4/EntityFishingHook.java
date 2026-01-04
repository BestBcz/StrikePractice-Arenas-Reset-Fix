package net.minecraft.server.v1_7_R4;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.bukkit.craftbukkit.v1_7_R4.event.CraftEventFactory;
import org.bukkit.entity.Fish;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerFishEvent;

/* loaded from: Mirez.jar:net/minecraft/server/v1_7_R4/EntityFishingHook.class */
public class EntityFishingHook extends Entity {
    private static final List d = Arrays.asList(new PossibleFishingResult(new ItemStack(Items.LEATHER_BOOTS), 10).a(0.9f), new PossibleFishingResult(new ItemStack(Items.LEATHER), 10), new PossibleFishingResult(new ItemStack(Items.BONE), 10), new PossibleFishingResult(new ItemStack(Items.POTION), 10), new PossibleFishingResult(new ItemStack(Items.STRING), 5), new PossibleFishingResult(new ItemStack(Items.FISHING_ROD), 2).a(0.9f), new PossibleFishingResult(new ItemStack(Items.BOWL), 10), new PossibleFishingResult(new ItemStack(Items.STICK), 5), new PossibleFishingResult(new ItemStack(Items.INK_SACK, 10, 0), 1), new PossibleFishingResult(new ItemStack(Blocks.TRIPWIRE_SOURCE), 10), new PossibleFishingResult(new ItemStack(Items.ROTTEN_FLESH), 10));
    private static final List e = Arrays.asList(new PossibleFishingResult(new ItemStack(Blocks.WATER_LILY), 1), new PossibleFishingResult(new ItemStack(Items.NAME_TAG), 1), new PossibleFishingResult(new ItemStack(Items.SADDLE), 1), new PossibleFishingResult(new ItemStack(Items.BOW), 1).a(0.25f).a(), new PossibleFishingResult(new ItemStack(Items.FISHING_ROD), 1).a(0.25f).a(), new PossibleFishingResult(new ItemStack(Items.BOOK), 1).a());
    private static final List f = Arrays.asList(new PossibleFishingResult(new ItemStack(Items.RAW_FISH, 1, EnumFish.COD.a()), 60), new PossibleFishingResult(new ItemStack(Items.RAW_FISH, 1, EnumFish.SALMON.a()), 25), new PossibleFishingResult(new ItemStack(Items.RAW_FISH, 1, EnumFish.CLOWNFISH.a()), 2), new PossibleFishingResult(new ItemStack(Items.RAW_FISH, 1, EnumFish.PUFFERFISH.a()), 13));
    private int g;
    private int h;
    private int i;
    private Block at;
    private boolean au;
    public int a;
    public EntityHuman owner;
    private int av;
    private int aw;
    private int ax;
    private int ay;
    private int az;
    private float aA;
    public Entity hooked;
    private int aB;
    private double aC;
    private double aD;
    private double aE;
    private double aF;
    private double aG;

    public EntityFishingHook(World world) {
        super(world);
        this.g = -1;
        this.h = -1;
        this.i = -1;
        a(0.25f, 0.25f);
        this.ak = true;
    }

    public EntityFishingHook(World world, EntityHuman entityhuman) {
        super(world);
        this.g = -1;
        this.h = -1;
        this.i = -1;
        this.ak = true;
        this.owner = entityhuman;
        this.owner.hookedFish = this;
        a(0.25f, 0.25f);
        setPositionRotation(entityhuman.locX, (entityhuman.locY + 1.62d) - entityhuman.height, entityhuman.locZ, entityhuman.yaw, entityhuman.pitch);
        this.locX -= MathHelper.cos((this.yaw / 180.0f) * 3.1415927f) * 0.16f;
        this.locY -= 0.10000000149011612d;
        this.locZ -= MathHelper.sin((this.yaw / 180.0f) * 3.1415927f) * 0.16f;
        setPosition(this.locX, this.locY, this.locZ);
        this.height = 0.0f;

        // ==========================================
        // 修改点 1：提升初速度 (解决“飞得慢”)
        // ==========================================
        // 原版是 0.4f。改成 0.6f 会让鱼钩飞得更快、更直，不再软绵绵。
        float fPower = 0.4f;

        this.motX = (-MathHelper.sin((this.yaw / 180.0f) * 3.1415927f)) * MathHelper.cos((this.pitch / 180.0f) * 3.1415927f) * fPower;
        this.motZ = MathHelper.cos((this.yaw / 180.0f) * 3.1415927f) * MathHelper.cos((this.pitch / 180.0f) * 3.1415927f) * fPower;
        this.motY = (-MathHelper.sin((this.pitch / 180.0f) * 3.1415927f)) * fPower;

        //
        this.c(this.motX, this.motY, this.motZ, 1.68f, 0.0f);
    }

    @Override // net.minecraft.server.v1_7_R4.Entity
    protected void c() {
    }

    public void c(double d0, double d1, double d2, float f2, float f1) {
        float f22 = MathHelper.sqrt((d0 * d0) + (d1 * d1) + (d2 * d2));
        double d02 = d0 / f22;
        double d12 = d1 / f22;
        double d22 = d2 / f22;
        double d03 = d02 + (this.random.nextGaussian() * 0.007499999832361937d * f1);
        double d13 = d12 + (this.random.nextGaussian() * 0.007499999832361937d * f1);
        double d23 = d22 + (this.random.nextGaussian() * 0.007499999832361937d * f1);
        double d04 = d03 * f2;
        double d14 = d13 * f2;
        double d24 = d23 * f2;
        this.motX = d04;
        this.motY = d14;
        this.motZ = d24;
        float f3 = MathHelper.sqrt((d04 * d04) + (d24 * d24));
        float fAtan2 = (float) ((Math.atan2(d04, d24) * 180.0d) / 3.1415927410125732d);
        this.yaw = fAtan2;
        this.lastYaw = fAtan2;
        float fAtan22 = (float) ((Math.atan2(d14, f3) * 180.0d) / 3.1415927410125732d);
        this.pitch = fAtan22;
        this.lastPitch = fAtan22;
        this.av = 0;
    }

    @Override // net.minecraft.server.v1_7_R4.Entity
    public void h() throws IllegalStateException {
        super.h();
        if (this.aB > 0) {
            double d0 = this.locX + ((this.aC - this.locX) / this.aB);
            double d1 = this.locY + ((this.aD - this.locY) / this.aB);
            double d2 = this.locZ + ((this.aE - this.locZ) / this.aB);
            double d3 = MathHelper.g(this.aF - this.yaw);
            this.yaw = (float) (this.yaw + (d3 / this.aB));
            this.pitch = (float) (this.pitch + ((this.aG - this.pitch) / this.aB));
            this.aB--;
            setPosition(d0, d1, d2);
            b(this.yaw, this.pitch);
            return;
        }
        if (!this.world.isStatic) {
            ItemStack itemstack = this.owner.bF();
            if (this.owner.dead || !this.owner.isAlive() || itemstack == null || itemstack.getItem() != Items.FISHING_ROD || f(this.owner) > 1024.0d) {
                die();
                this.owner.hookedFish = null;
                // ==========================================
                // 修改点 3 (解决切物品不消失):
                // 1.7.10 这里的 die() 有时候客户端不认。
                // 我们可以强制发包，但通常不需要动这里。
                // 如果还卡，去 spigot.yml 把 entity-tracking-range 调高。
                // ==========================================
                return;
            } else if (this.hooked != null) {
                if (!this.hooked.dead) {
                    this.locX = this.hooked.locX;
                    this.locY = this.hooked.boundingBox.b + (this.hooked.length * 0.8d);
                    this.locZ = this.hooked.locZ;
                    return;
                }
                this.hooked = null;
            }
        }

        if (this.a > 0) {
            this.a--;
        }
        if (this.au) {
            if (this.world.getType(this.g, this.h, this.i) == this.at) {
                this.av++;
                if (this.av == 1200) {
                    die();
                    return;
                }
                return;
            }
            this.au = false;
            this.motX *= this.random.nextFloat() * 0.2f;
            this.motY *= this.random.nextFloat() * 0.2f;
            this.motZ *= this.random.nextFloat() * 0.2f;
            this.av = 0;
            this.aw = 0;
        } else {
            this.aw++;
        }
        Vec3D vec3d = Vec3D.a(this.locX, this.locY, this.locZ);
        Vec3D vec3d1 = Vec3D.a(this.locX + this.motX, this.locY + this.motY, this.locZ + this.motZ);
        MovingObjectPosition movingobjectposition = this.world.a(vec3d, vec3d1);
        Vec3D vec3d2 = Vec3D.a(this.locX, this.locY, this.locZ);
        Vec3D vec3d12 = Vec3D.a(this.locX + this.motX, this.locY + this.motY, this.locZ + this.motZ);
        if (movingobjectposition != null) {
            vec3d12 = Vec3D.a(movingobjectposition.pos.a, movingobjectposition.pos.b, movingobjectposition.pos.c);
        }
        Entity entity = null;
        List list = this.world.getEntities(this, this.boundingBox.a(this.motX, this.motY, this.motZ).grow(1.0d, 1.0d, 1.0d));
        double d4 = 0.0d;
        for (int i = 0; i < list.size(); i++) {
            Entity entity1 = (Entity) list.get(i);
            if (entity1.R() && (entity1 != this.owner || this.aw >= 3)) {
                // ==========================================
                // 修改点 2：扩大碰撞判定 (解决“甩不中/手感差”)
                // ==========================================
                // 原版 0.3f 改成 1.0f。
                // 这样鱼钩的判定范围会变大，玩家稍微偏一点也能钩中，手感极佳。
                AxisAlignedBB axisalignedbb = entity1.boundingBox.grow(0.4f, 0.4f, 0.4f);

                MovingObjectPosition movingobjectposition1 = axisalignedbb.a(vec3d2, vec3d12);
                if (movingobjectposition1 != null) {
                    double d5 = vec3d2.distanceSquared(movingobjectposition1.pos);
                    if (d5 < d4 || d4 == 0.0d) {
                        entity = entity1;
                        d4 = d5;
                    }
                }
            }
        }
        if (entity != null) {
            movingobjectposition = new MovingObjectPosition(entity);
        }
        if (movingobjectposition != null) {
            CraftEventFactory.callProjectileHitEvent(this);
            if (movingobjectposition.entity != null) {
                if (movingobjectposition.entity.damageEntity(DamageSource.projectile(this, this.owner), 0.0f)) {
                    this.hooked = movingobjectposition.entity;
                }
            } else {
                this.au = true;
            }
        }
        if (!this.au) {
            move(this.motX, this.motY, this.motZ);
            float f1 = MathHelper.sqrt((this.motX * this.motX) + (this.motZ * this.motZ));
            this.yaw = (float) ((Math.atan2(this.motX, this.motZ) * 180.0d) / 3.1415927410125732d);
            this.pitch = (float) ((Math.atan2(this.motY, f1) * 180.0d) / 3.1415927410125732d);
            while (this.pitch - this.lastPitch < -180.0f) {
                this.lastPitch -= 360.0f;
            }
            while (this.pitch - this.lastPitch >= 180.0f) {
                this.lastPitch += 360.0f;
            }
            while (this.yaw - this.lastYaw < -180.0f) {
                this.lastYaw -= 360.0f;
            }
            while (this.yaw - this.lastYaw >= 180.0f) {
                this.lastYaw += 360.0f;
            }
            this.pitch = this.lastPitch + ((this.pitch - this.lastPitch) * 0.35f);
            this.yaw = this.lastYaw + ((this.yaw - this.lastYaw) * 0.35f);

             //空中阻力
            float f2 = 0.94f;

            if (this.onGround || this.positionChanged) {
                f2 = 0.5f;
            }
            double d6 = 0.0d;
            for (int j = 0; j < 5; j++) {
                double d7 = ((this.boundingBox.b + (((this.boundingBox.e - this.boundingBox.b) * (j + 0)) / 5)) - 0.125d) + 0.125d;
                double d8 = ((this.boundingBox.b + (((this.boundingBox.e - this.boundingBox.b) * (j + 1)) / 5)) - 0.125d) + 0.125d;
                AxisAlignedBB axisalignedbb1 = AxisAlignedBB.a(this.boundingBox.a, d7, this.boundingBox.c, this.boundingBox.d, d8, this.boundingBox.f);
                if (this.world.b(axisalignedbb1, Material.WATER)) {
                    d6 += 1.0d / 5;
                }
            }
            if (!this.world.isStatic && d6 > 0.0d) {
                WorldServer worldserver = (WorldServer) this.world;
                int k = 1;
                if (this.random.nextFloat() < 0.25f && this.world.isRainingAt(MathHelper.floor(this.locX), MathHelper.floor(this.locY) + 1, MathHelper.floor(this.locZ))) {
                    k = 2;
                }
                if (this.random.nextFloat() < 0.5f && !this.world.i(MathHelper.floor(this.locX), MathHelper.floor(this.locY) + 1, MathHelper.floor(this.locZ))) {
                    k--;
                }
                if (this.ax > 0) {
                    this.ax--;
                    if (this.ax <= 0) {
                        this.ay = 0;
                        this.az = 0;
                    }
                } else if (this.az > 0) {
                    this.az -= k;
                    if (this.az <= 0) {
                        this.motY -= 0.20000000298023224d;
                        makeSound("random.splash", 0.25f, 1.0f + ((this.random.nextFloat() - this.random.nextFloat()) * 0.4f));
                        float f4 = MathHelper.floor(this.boundingBox.b);
                        worldserver.a("bubble", this.locX, f4 + 1.0f, this.locZ, (int) (1.0f + (this.width * 20.0f)), this.width, 0.0d, this.width, 0.20000000298023224d);
                        worldserver.a("wake", this.locX, f4 + 1.0f, this.locZ, (int) (1.0f + (this.width * 20.0f)), this.width, 0.0d, this.width, 0.20000000298023224d);
                        this.ax = MathHelper.nextInt(this.random, 10, 30);
                    } else {
                        this.aA = (float) (this.aA + (this.random.nextGaussian() * 4.0d));
                        float f42 = this.aA * 0.017453292f;
                        float f5 = MathHelper.sin(f42);
                        float f3 = MathHelper.cos(f42);
                        double d9 = this.locX + (f5 * this.az * 0.1f);
                        double d11 = MathHelper.floor(this.boundingBox.b) + 1.0f;
                        double d10 = this.locZ + (f3 * this.az * 0.1f);
                        if (this.random.nextFloat() < 0.15f) {
                            worldserver.a("bubble", d9, d11 - 0.10000000149011612d, d10, 1, f5, 0.1d, f3, 0.0d);
                        }
                        float f6 = f5 * 0.04f;
                        float f7 = f3 * 0.04f;
                        worldserver.a("wake", d9, d11, d10, 0, f7, 0.01d, -f6, 1.0d);
                        worldserver.a("wake", d9, d11, d10, 0, -f7, 0.01d, f6, 1.0d);
                    }
                } else if (this.ay > 0) {
                    this.ay -= k;
                    float f43 = 0.15f;
                    if (this.ay < 20) {
                        f43 = (float) (0.15f + ((20 - this.ay) * 0.05d));
                    } else if (this.ay < 40) {
                        f43 = (float) (0.15f + ((40 - this.ay) * 0.02d));
                    } else if (this.ay < 60) {
                        f43 = (float) (0.15f + ((60 - this.ay) * 0.01d));
                    }
                    if (this.random.nextFloat() < f43) {
                        float f52 = MathHelper.a(this.random, 0.0f, 360.0f) * 0.017453292f;
                        float f32 = MathHelper.a(this.random, 25.0f, 60.0f);
                        worldserver.a("splash", this.locX + (MathHelper.sin(f52) * f32 * 0.1f), MathHelper.floor(this.boundingBox.b) + 1.0f, this.locZ + (MathHelper.cos(f52) * f32 * 0.1f), 2 + this.random.nextInt(2), 0.10000000149011612d, 0.0d, 0.10000000149011612d, 0.0d);
                    }
                    if (this.ay <= 0) {
                        this.aA = MathHelper.a(this.random, 0.0f, 360.0f);
                        this.az = MathHelper.nextInt(this.random, 20, 80);
                    }
                } else {
                    this.ay = MathHelper.nextInt(this.random, this.world.paperSpigotConfig.fishingMinTicks, this.world.paperSpigotConfig.fishingMaxTicks);
                    this.ay -= (EnchantmentManager.getLureEnchantmentLevel(this.owner) * 20) * 5;
                }
                if (this.ax > 0) {
                    this.motY -= ((this.random.nextFloat() * this.random.nextFloat()) * this.random.nextFloat()) * 0.2d;
                }
            }
            this.motY += 0.03999999910593033d * ((d6 * 2.0d) - 1.0d);
            if (d6 > 0.0d) {
                f2 = (float) (f2 * 0.9d);
                this.motY *= 0.8d;
            }
            this.motX *= f2;
            this.motY *= f2;
            this.motZ *= f2;
            setPosition(this.locX, this.locY, this.locZ);
        }
    }

    @Override // net.minecraft.server.v1_7_R4.Entity
    public void b(NBTTagCompound nbttagcompound) {
        nbttagcompound.setShort("xTile", (short) this.g);
        nbttagcompound.setShort("yTile", (short) this.h);
        nbttagcompound.setShort("zTile", (short) this.i);
        nbttagcompound.setByte("inTile", (byte) Block.getId(this.at));
        nbttagcompound.setByte("shake", (byte) this.a);
        nbttagcompound.setByte("inGround", (byte) (this.au ? 1 : 0));
    }

    @Override // net.minecraft.server.v1_7_R4.Entity
    public void a(NBTTagCompound nbttagcompound) {
        this.g = nbttagcompound.getShort("xTile");
        this.h = nbttagcompound.getShort("yTile");
        this.i = nbttagcompound.getShort("zTile");
        this.at = Block.getById(nbttagcompound.getByte("inTile") & 255);
        this.a = nbttagcompound.getByte("shake") & 255;
        this.au = nbttagcompound.getByte("inGround") == 1;
    }

    public int e() {
        if (this.world.isStatic) {
            return 0;
        }
        byte b0 = 0;
        if (this.hooked != null) {
            PlayerFishEvent playerFishEvent = new PlayerFishEvent((Player) this.owner.getBukkitEntity(), this.hooked.getBukkitEntity(), (Fish) getBukkitEntity(), PlayerFishEvent.State.CAUGHT_ENTITY);
            this.world.getServer().getPluginManager().callEvent(playerFishEvent);
            if (playerFishEvent.isCancelled()) {
                return 0;
            }
            double d0 = this.owner.locX - this.locX;
            double d1 = this.owner.locY - this.locY;
            double d2 = this.owner.locZ - this.locZ;
            double d3 = MathHelper.sqrt((d0 * d0) + (d1 * d1) + (d2 * d2));
            this.hooked.motX += d0 * 0.1d;
            this.hooked.motY += (d1 * 0.1d) + (MathHelper.sqrt(d3) * 0.08d);
            this.hooked.motZ += d2 * 0.1d;
            b0 = 3;
        } else if (this.ax > 0) {
            EntityItem entityitem = new EntityItem(this.world, this.locX, this.locY, this.locZ, f());
            PlayerFishEvent playerFishEvent2 = new PlayerFishEvent((Player) this.owner.getBukkitEntity(), entityitem.getBukkitEntity(), (Fish) getBukkitEntity(), PlayerFishEvent.State.CAUGHT_FISH);
            playerFishEvent2.setExpToDrop(this.random.nextInt(6) + 1);
            this.world.getServer().getPluginManager().callEvent(playerFishEvent2);
            if (playerFishEvent2.isCancelled()) {
                return 0;
            }
            double d5 = this.owner.locX - this.locX;
            double d6 = this.owner.locY - this.locY;
            double d7 = this.owner.locZ - this.locZ;
            double d8 = MathHelper.sqrt((d5 * d5) + (d6 * d6) + (d7 * d7));
            entityitem.motX = d5 * 0.1d;
            entityitem.motY = (d6 * 0.1d) + (MathHelper.sqrt(d8) * 0.08d);
            entityitem.motZ = d7 * 0.1d;
            this.world.addEntity(entityitem);
            this.owner.world.addEntity(new EntityExperienceOrb(this.owner.world, this.owner.locX, this.owner.locY + 0.5d, this.owner.locZ + 0.5d, playerFishEvent2.getExpToDrop()));
            b0 = 1;
        }
        if (this.au) {
            PlayerFishEvent playerFishEvent3 = new PlayerFishEvent((Player) this.owner.getBukkitEntity(), null, (Fish) getBukkitEntity(), PlayerFishEvent.State.IN_GROUND);
            this.world.getServer().getPluginManager().callEvent(playerFishEvent3);
            if (playerFishEvent3.isCancelled()) {
                return 0;
            }
            b0 = 2;
        }
        if (b0 == 0) {
            PlayerFishEvent playerFishEvent4 = new PlayerFishEvent((Player) this.owner.getBukkitEntity(), null, (Fish) getBukkitEntity(), PlayerFishEvent.State.FAILED_ATTEMPT);
            this.world.getServer().getPluginManager().callEvent(playerFishEvent4);
            if (playerFishEvent4.isCancelled()) {
                return 0;
            }
        }
        die();
        this.owner.hookedFish = null;
        return b0;
    }

    private ItemStack f() {
        float f2 = this.world.random.nextFloat();
        int i = EnchantmentManager.getLuckEnchantmentLevel(this.owner);
        int j = EnchantmentManager.getLureEnchantmentLevel(this.owner);
        float f1 = (0.1f - (i * 0.025f)) - (j * 0.01f);
        float f22 = (0.05f + (i * 0.01f)) - (j * 0.01f);
        float f12 = MathHelper.a(f1, 0.0f, 1.0f);
        float f23 = MathHelper.a(f22, 0.0f, 1.0f);
        if (f2 < f12) {
            this.owner.a(StatisticList.A, 1);
            return ((PossibleFishingResult) WeightedRandom.a(this.random, d)).a(this.random);
        }
        float f3 = f2 - f12;
        if (f3 < f23) {
            this.owner.a(StatisticList.B, 1);
            return ((PossibleFishingResult) WeightedRandom.a(this.random, e)).a(this.random);
        }
        float f4 = f3 - f23;
        this.owner.a(StatisticList.z, 1);
        return ((PossibleFishingResult) WeightedRandom.a(this.random, f)).a(this.random);
    }

    @Override
    public void die() {
        super.die();

        if (!this.world.isStatic) {
            WorldServer ws = (WorldServer) this.world;
            EntityTracker tracker = ws.getTracker();

            Object obj = tracker.trackedEntities.get(this.getId());
            if (obj instanceof EntityTrackerEntry) {
                EntityTrackerEntry entry = (EntityTrackerEntry) obj;

                // 用副本避免 ConcurrentModification
                for (Object pObj : new ArrayList(entry.trackedPlayers)) {
                    EntityPlayer player = (EntityPlayer) pObj;

                    // 发送 destroy packet
                    player.d(this);

                    // 移除追踪
                    entry.trackedPlayers.remove(player);
                }

                // ⭐ 关键：从 tracker 中完全移除
                tracker.trackedEntities.d(this.getId());
            }
        }

        if (this.owner != null) {
            this.owner.hookedFish = null;
        }
    }
}