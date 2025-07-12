package com.medinadev.createsantalucia.events;

import com.medinadev.createsantalucia.item.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

import java.util.List;

import static com.medinadev.createsantalucia.CreateSantaLucia.MOD_ID;

@EventBusSubscriber(modid = MOD_ID)
public class EndPortalHandler {

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Level level = event.getLevel();
        BlockState clickedState = level.getBlockState(event.getPos());

        if (clickedState.is(Blocks.END_PORTAL_FRAME)) {
            Player player = event.getEntity();
            ItemStack heldStack = player.getItemInHand(event.getHand());

            if (heldStack.getItem() == ModItems.MASTER_KEY.get()) {
                if (!level.isClientSide) {
                    handleEndPortalActivation(event, level, event.getPos(), player);
                } else {
                    player.swing(event.getHand());
                }
            } else if (heldStack.getItem() == Items.ENDER_EYE) {
                if (!level.isClientSide) {
                    preventEndPortalWithEye(event, player);
                }
            }
        }
    }

    private static void handleEndPortalActivation(PlayerInteractEvent.RightClickBlock event, Level level, BlockPos clickedPos, Player player) {
        event.setCanceled(true);

        for (Direction.Axis axis : new Direction.Axis[]{Direction.Axis.Z, Direction.Axis.X}) {
            BlockPos portalCenter = findPortalCenter(level, clickedPos, axis);

            if (portalCenter != null) {
                transformPortal(level, portalCenter, axis);

                if (level instanceof ServerLevel serverLevel) {
                    LightningBolt lightning = EntityType.LIGHTNING_BOLT.create(serverLevel);
                    if (lightning != null) {
                        lightning.setPos(portalCenter.getX() + 0.5, portalCenter.getY(), portalCenter.getZ() + 0.5);
                        lightning.setVisualOnly(true);
                        serverLevel.addFreshEntity(lightning);
                    }

                    AABB searchArea = new AABB(portalCenter).inflate(20);
                    List<ServerPlayer> playersNearby = serverLevel.getPlayers(nearbyPlayer -> nearbyPlayer.getBoundingBox().intersects(searchArea));

                    for (ServerPlayer p : playersNearby) {
                        p.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 100, 0));
                        p.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 0));
                    }
                }

                player.displayClientMessage(Component.translatable("message.createsantalucia.end_portal_activated").withStyle(ChatFormatting.DARK_PURPLE), true);
                event.setCancellationResult(InteractionResult.SUCCESS);
                return;
            }
        }

        player.displayClientMessage(Component.translatable("message.createsantalucia.nether_portal_no_frame").withStyle(ChatFormatting.RED), true);
        event.setCancellationResult(InteractionResult.FAIL);
    }

    private static BlockPos findPortalCenter(Level level, BlockPos startPos, Direction.Axis axis) {
        for (int i = -4; i <= 4; i++) {
            for (int j = -4; j <= 4; j++) {
                int xOffset = axis == Direction.Axis.X ? i : j;
                int zOffset = axis == Direction.Axis.X ? j : i;
                BlockPos potentialCenter = startPos.offset(xOffset, 0, zOffset);

                if (isPortalFrameValid(level, potentialCenter, axis)) {
                    return potentialCenter;
                }
            }
        }
        return null;
    }

    private static boolean isPortalFrameValid(Level level, BlockPos center, Direction.Axis axis) {
        Direction primaryDir = Direction.get(Direction.AxisDirection.POSITIVE, axis);
        Direction secondaryDir = primaryDir.getClockWise();

        for (int i = -1; i <= 1; i++) {
            if (!level.getBlockState(center.relative(primaryDir, i).relative(secondaryDir, 2)).is(Blocks.END_PORTAL_FRAME)) return false;
            if (!level.getBlockState(center.relative(primaryDir, i).relative(secondaryDir, -2)).is(Blocks.END_PORTAL_FRAME)) return false;
        }
        for (int i = -1; i <= 1; i++) {
            if (!level.getBlockState(center.relative(primaryDir, 2).relative(secondaryDir, i)).is(Blocks.END_PORTAL_FRAME)) return false;
            if (!level.getBlockState(center.relative(primaryDir, -2).relative(secondaryDir, i)).is(Blocks.END_PORTAL_FRAME)) return false;
        }
        return true;
    }

    private static void transformPortal(Level level, BlockPos center, Direction.Axis axis) {
        Direction primaryDir = Direction.get(Direction.AxisDirection.POSITIVE, axis);
        Direction secondaryDir = primaryDir.getClockWise();

        for (int i = -1; i <= 1; i++) {
            for (int j = -1; j <= 1; j++) {
                level.setBlock(center.relative(primaryDir, i).relative(secondaryDir, j), Blocks.END_PORTAL.defaultBlockState(), 2);
            }
        }

        for (int i = -2; i <= 2; i++) {
            for (int j = -2; j <= 2; j++) {
                if (Math.abs(i) == 2 || Math.abs(j) == 2) {
                    level.setBlock(center.relative(primaryDir, i).relative(secondaryDir, j), Blocks.END_PORTAL.defaultBlockState(), 2);
                }
            }
        }
    }

    private static void preventEndPortalWithEye(PlayerInteractEvent.RightClickBlock event, Player player) {
        event.setCanceled(true);
        player.displayClientMessage(
                Component.translatable("message.createsantalucia.end_portal_no_eye")
                        .withStyle(ChatFormatting.RED),
                true
        );
        event.setCancellationResult(InteractionResult.FAIL);
    }
}