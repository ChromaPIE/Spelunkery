package com.ordana.underground_overhaul.items;

import dev.architectury.injectables.annotations.ExpectPlatform;
import dev.architectury.injectables.annotations.PlatformOnly;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.List;

public class DepthGaugeItem extends Item {
    public DepthGaugeItem(Properties properties) {
        super(properties);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level levelIn, Entity entityIn, int itemSlot, boolean isSelected) {
        super.inventoryTick(stack, levelIn, entityIn, itemSlot, isSelected);

        if (!levelIn.isClientSide()){
            if (entityIn instanceof ServerPlayer player){
                this.setYLevel(stack, player.getBlockY());

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
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag context) {
        tooltip.add(Component.translatable("tooltip.underground_overhaul.depth_gauge_depth", getYLevel(stack)).setStyle(Style.EMPTY.applyFormat(ChatFormatting.DARK_GREEN)));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, @NotNull Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        player.displayClientMessage(Component.translatable("tooltip.underground_overhaul.depth_gauge_depth", getYLevel(stack)).setStyle(Style.EMPTY.applyFormat(ChatFormatting.DARK_GREEN)), true);
        return new InteractionResultHolder<>(InteractionResult.SUCCESS, stack);
    }

    public void setYLevel(ItemStack stack, int amount) {
        stack.getOrCreateTag().putInt("depth", amount);
    }

    public int getYLevel(ItemStack stack) {
        return stack.getOrCreateTag().getInt("depth");
    }
}