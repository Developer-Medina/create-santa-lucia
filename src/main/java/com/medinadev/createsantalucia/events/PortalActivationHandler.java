package com.medinadev.createsantalucia.events;

import com.medinadev.createsantalucia.item.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EndPortalFrameBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.pattern.BlockPattern;
import net.minecraft.world.level.portal.PortalShape;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

import java.util.Optional;

import static com.medinadev.createsantalucia.CreateSantaLucia.MOD_ID;

@EventBusSubscriber(modid = MOD_ID)
public class PortalActivationHandler {

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Level level = event.getLevel();
        if (level.isClientSide) return;

        Player player = event.getEntity();
        ItemStack heldStack = player.getItemInHand(event.getHand());
        BlockPos pos = event.getPos();
        BlockState clickedState = level.getBlockState(pos);

        // Handle Nether Portal activation with Master Key
        if (clickedState.is(Blocks.OBSIDIAN)) {
            if (heldStack.getItem() == ModItems.MASTER_KEY.get()) {
                activateNetherPortal(event, level, pos, player);
            } else if (heldStack.getItem() == Items.FLINT_AND_STEEL) {
                destroyObsidianAndPreventPortal(event, level, pos, player);
            }
            return;
        }

        // Handle End Portal Frame interaction
        if (clickedState.is(Blocks.END_PORTAL_FRAME)) {
            if (heldStack.getItem() == ModItems.MASTER_KEY.get()) {
                // Ativa todos os frames em 10 blocos de raio
                activateEndFramesInRadius(event, level, pos, player);
                // Depois verifica se formou um portal completo
                handleMasterKeyOnEndFrame(event, level, pos, clickedState, player);
            } else if (heldStack.getItem() == Items.ENDER_EYE) {
                // Impede o uso normal do Eye of Ender
                preventEndPortalWithEye(event, level, pos, player, heldStack);
            }
        }
    }

    // Nether Portal activation remains unchanged as requested
    private static void activateNetherPortal(PlayerInteractEvent.RightClickBlock event, Level level, BlockPos pos, Player player) {
        // Try finding portal shape in both X and Z axes
        Optional<PortalShape> portalShape = PortalShape.findEmptyPortalShape(level, pos, Direction.Axis.X)
                .or(() -> PortalShape.findEmptyPortalShape(level, pos.relative(event.getFace()), Direction.Axis.X))
                .or(() -> PortalShape.findEmptyPortalShape(level, pos, Direction.Axis.Z))
                .or(() -> PortalShape.findEmptyPortalShape(level, pos.relative(event.getFace()), Direction.Axis.Z));

        if (portalShape.isPresent()) {
            portalShape.get().createPortalBlocks();
            level.playSound(null, pos, SoundEvents.FLINTANDSTEEL_USE, SoundSource.BLOCKS, 1.0F, level.getRandom().nextFloat() * 0.4F + 0.8F);
            player.displayClientMessage(Component.translatable("message.createsantalucia.nether_portal_activated").withStyle(ChatFormatting.GREEN), true);
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
        } else {
            player.displayClientMessage(Component.translatable("message.createsantalucia.nether_portal_no_frame").withStyle(ChatFormatting.RED), true);
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.FAIL);
        }
    }

    // Obsidian destruction logic remains unchanged
    private static void destroyObsidianAndPreventPortal(PlayerInteractEvent.RightClickBlock event, Level level, BlockPos pos, Player player) {
        final int radius = 6;
        int blocksDestroyed = 0;

        // Scan and destroy obsidian in a 6-block radius
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    BlockPos currentPos = pos.offset(x, y, z);
                    if (level.getBlockState(currentPos).is(Blocks.OBSIDIAN)) {
                        level.destroyBlock(currentPos, true);
                        blocksDestroyed++;
                    }
                }
            }
        }

        level.playSound(null, pos, SoundEvents.STONE_BREAK, SoundSource.BLOCKS, 1.0F, 1.0F);

        // Show appropriate message based on destruction result
        Component message = blocksDestroyed > 0
                ? Component.translatable("message.createsantalucia.obsidian_destroyed", blocksDestroyed).withStyle(ChatFormatting.RED)
                : Component.translatable("message.createsantalucia.no_obsidian_found").withStyle(ChatFormatting.YELLOW);

        player.displayClientMessage(message, true);

        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
    }

    /**
     * Handles Master Key interaction with End Portal Frame
     * - Places eye if frame doesn't have one
     * - Activates portal if all frames have eyes
     */
    /**
     * Ativa todos os End Portal Frames em um raio de 10 blocos
     */
    private static void activateEndFramesInRadius(PlayerInteractEvent.RightClickBlock event, Level level, BlockPos centerPos, Player player) {
        final int radius = 10;
        int framesActivated = 0;

        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    BlockPos currentPos = centerPos.offset(x, y, z);
                    BlockState currentState = level.getBlockState(currentPos);

                    if (currentState.is(Blocks.END_PORTAL_FRAME) && !currentState.getValue(EndPortalFrameBlock.HAS_EYE)) {
                        level.setBlock(currentPos, currentState.setValue(EndPortalFrameBlock.HAS_EYE, true), 3);
                        framesActivated++;
                        level.playSound(null, currentPos, SoundEvents.END_PORTAL_FRAME_FILL, SoundSource.BLOCKS, 0.5F, 1.0F);
                    }
                }
            }
        }

        if (framesActivated > 0) {
            player.displayClientMessage(
                    Component.translatable("message.createsantalucia.frames_activated", framesActivated)
                            .withStyle(ChatFormatting.GREEN),
                    true
            );
        }
    }

    /**
     * Lógica principal de ativação do portal com Master Key
     */
    private static void handleMasterKeyOnEndFrame(PlayerInteractEvent.RightClickBlock event, Level level, BlockPos pos, BlockState state, Player player) {
        event.setCanceled(true);

        BlockPattern.BlockPatternMatch portalMatch = EndPortalFrameBlock.getOrCreatePortalShape().find(level, pos);
        if (portalMatch != null) {
            BlockPos frontTopLeft = portalMatch.getFrontTopLeft();
            Direction forwards = portalMatch.getForwards();
            Direction right = forwards.getClockWise();

            // Verifica se todos os frames têm olhos
            boolean allFramesHaveEyes = true;
            for (int i = 0; i < 3 && allFramesHaveEyes; i++) {
                for (int j = 0; j < 3 && allFramesHaveEyes; j++) {
                    if (i == 1 && j == 1) continue; // Skip center

                    BlockPos framePos = frontTopLeft.relative(forwards, i).relative(right, j);
                    BlockState frameState = level.getBlockState(framePos);

                    if (frameState.is(Blocks.END_PORTAL_FRAME) && !frameState.getValue(EndPortalFrameBlock.HAS_EYE)) {
                        allFramesHaveEyes = false;
                    }
                }
            }

            // Ativa o portal se estiver completo
            if (allFramesHaveEyes) {
                BlockPos centerPos = frontTopLeft.relative(forwards, 1).relative(right, 1);
                level.setBlock(centerPos, Blocks.END_PORTAL.defaultBlockState(), 2);
                level.playSound(null, centerPos, SoundEvents.END_PORTAL_SPAWN, SoundSource.BLOCKS, 1.0F, 1.0F);
                player.displayClientMessage(
                        Component.translatable("message.createsantalucia.end_portal_activated")
                                .withStyle(ChatFormatting.DARK_PURPLE),
                        true
                );
            }
        }

        event.setCancellationResult(InteractionResult.SUCCESS);
    }

    /**
     * Impede ativação com Eye of Ender normal
     */
    private static void preventEndPortalWithEye(PlayerInteractEvent.RightClickBlock event, Level level, BlockPos pos, Player player, ItemStack eyeStack) {
        event.setCanceled(true);
        player.displayClientMessage(
                Component.translatable("message.createsantalucia.end_portal_requires_key")
                        .withStyle(ChatFormatting.RED),
                true
        );
        level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.5F, 1.5F);

        if (!player.getAbilities().instabuild) {
            eyeStack.shrink(1);
        }

        event.setCancellationResult(InteractionResult.CONSUME);
    }
}