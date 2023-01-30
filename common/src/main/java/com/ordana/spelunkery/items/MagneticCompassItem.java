package com.ordana.spelunkery.items;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.DataResult;
import com.ordana.spelunkery.configs.ClientConfigs;
import com.ordana.spelunkery.events.ModEvents;
import com.ordana.spelunkery.reg.ModBlocks;
import com.ordana.spelunkery.reg.ModGameEvents;
import dev.architectury.injectables.annotations.PlatformOnly;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.behavior.PoiCompetitorScan;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class MagneticCompassItem extends Item implements Vanishable {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final String TAG_LODESTONE_POS = "LodestonePos";
    public static final String TAG_LODESTONE_DIMENSION = "LodestoneDimension";
    public static final String TAG_LODESTONE_TRACKED = "LodestoneTracked";

    public MagneticCompassItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, @javax.annotation.Nullable Level level, List<Component> tooltip, TooltipFlag context) {
        if (ClientConfigs.ENABLE_TOOLTIPS.get()) {
            CompoundTag compoundTag = stack.getOrCreateTag();
            if (compoundTag.contains("magnetitePos")) {
                BlockPos blockPos = NbtUtils.readBlockPos(compoundTag.getCompound("magnetitePos"));
                tooltip.add(Component.translatable("tooltip.spelunkery.magnetic_compass_1", blockPos.getX(), blockPos.getZ()).setStyle(Style.EMPTY.applyFormat(ChatFormatting.DARK_GREEN)));
            }
            if (!Screen.hasShiftDown()) {
                tooltip.add(Component.translatable("tooltip.spelunkery.hold_crouch").setStyle(Style.EMPTY.applyFormat(ChatFormatting.GOLD)));
            }
            if (Screen.hasShiftDown()) {
                tooltip.add(Component.translatable("tooltip.spelunkery.magnetic_compass_2").setStyle(Style.EMPTY.applyFormat(ChatFormatting.GRAY)));
                tooltip.add(Component.translatable("tooltip.spelunkery.magnetic_compass_3").setStyle(Style.EMPTY.applyFormat(ChatFormatting.GRAY)));
                tooltip.add(Component.translatable("tooltip.spelunkery.magnetic_compass_4").setStyle(Style.EMPTY.applyFormat(ChatFormatting.GRAY)));
            }
        }
    }

    //Override
    @PlatformOnly(PlatformOnly.FORGE)
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return false;
    }

    //Override
    @PlatformOnly(PlatformOnly.FABRIC)
    public boolean allowNbtUpdateAnimation(Player player, InteractionHand hand, ItemStack originalStack, ItemStack updatedStack) {
        return false;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, @NotNull Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        CompoundTag compoundTag = stack.getOrCreateTag();
        if (!compoundTag.contains("magnetitePos")) level.gameEvent(player, ModGameEvents.COMPASS_PING_EVENT.get(), player.blockPosition());
        if (player.isSecondaryUseActive()) compoundTag.remove("magnetitePos");
        return new InteractionResultHolder<>(InteractionResult.SUCCESS, stack);
    }

    private static Optional<ResourceKey<Level>> getDimension(CompoundTag compoundTag) {
        return Level.RESOURCE_KEY_CODEC.parse(NbtOps.INSTANCE, compoundTag.get("magnetiteDimension")).result();
    }

    private int tickCounter = 0;
    public int setTickCounter(int tick) {
        return tickCounter = tick;
    }

    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        if (!level.isClientSide) {
            CompoundTag compoundTag = stack.getOrCreateTag();
            tickCounter++;
            if (tickCounter == 100) {
                if (!compoundTag.contains("magnetitePos")) level.gameEvent(entity, ModGameEvents.COMPASS_PING_EVENT.get(), entity.blockPosition());
                setTickCounter(0);
            }

            Optional<ResourceKey<Level>> optional = getDimension(compoundTag);
            if (optional.isPresent() && optional.get() == level.dimension() && compoundTag.contains("magnetitePos")) {
                BlockPos blockPos = NbtUtils.readBlockPos(compoundTag.getCompound("magnetitePos"));
                if (!level.isInWorldBounds(blockPos) || !level.getBlockState(blockPos).is(ModBlocks.MAGNETITE.get())) {
                    compoundTag.remove("magnetitePos");
                    level.gameEvent(entity, ModGameEvents.COMPASS_PING_EVENT.get(), entity.blockPosition());
                }
            }

        }
    }

    public boolean isFoil(ItemStack stack) {
        return isMagnetiteNearby(stack) || super.isFoil(stack);
    }

    public static void addMagnetiteTags(ResourceKey<Level> lodestoneDimension, BlockPos pos, CompoundTag compoundTag) {
        if (!compoundTag.contains("magnetitePos")) compoundTag.put("magnetitePos", NbtUtils.writeBlockPos(pos));
        DataResult<Tag> var10000 = Level.RESOURCE_KEY_CODEC.encodeStart(NbtOps.INSTANCE, lodestoneDimension);
        Logger var10001 = LOGGER;
        Objects.requireNonNull(var10001);
        var10000.resultOrPartial(var10001::error).ifPresent((tag) -> {
            compoundTag.put("magnetiteDimension", tag);
        });

    }

    public static boolean isMagnetiteNearby(ItemStack stack) {
        CompoundTag compoundTag = stack.getTag();
        return compoundTag != null && compoundTag.contains("magnetitePos");
    }

    @Nullable
    public static GlobalPos getNorthPosition(Level level) {
        return level.dimensionType().natural() ? GlobalPos.of(level.dimension(), new BlockPos(0, 0, -10000000)) : null;
    }

    @Nullable
    public static GlobalPos getMagnetitePos(CompoundTag compoundTag) {
        boolean bl = compoundTag.contains("magnetitePos");
        boolean bl2 = compoundTag.contains("magnetiteDimension");
        if (bl && bl2) {
            Optional<ResourceKey<Level>> optional = getDimension(compoundTag);
            if (optional.isPresent()) {
                BlockPos blockPos = NbtUtils.readBlockPos(compoundTag.getCompound("magnetitePos"));
                return GlobalPos.of(optional.get(), blockPos);
            }
        }

        return null;
    }
}