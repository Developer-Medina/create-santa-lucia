package com.medinadev.createsantalucia.events;

import com.medinadev.createsantalucia.item.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
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
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.portal.PortalShape;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

import java.util.Objects;
import java.util.Optional;

import static com.medinadev.createsantalucia.CreateSantaLucia.MOD_ID;

@EventBusSubscriber(modid = MOD_ID)
public class NetherPortalHandler {

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Level level = event.getLevel();
        BlockState clickedState = level.getBlockState(event.getPos());

        if (clickedState.is(Blocks.OBSIDIAN)) {
            if (level.isClientSide) return;

            Player player = event.getEntity();
            ItemStack heldStack = player.getItemInHand(event.getHand());

            if (heldStack.getItem() == ModItems.MASTER_KEY.get()) {
                player.swing(event.getHand());
                activateNetherPortal(event, level, event.getPos(), player);
            } else if (heldStack.getItem() == Items.FLINT_AND_STEEL) {
                punishPlayer(event, level, event.getPos(), player);
            }
        }
    }

    private static void activateNetherPortal(PlayerInteractEvent.RightClickBlock event, Level level, BlockPos pos, Player player) {
        Optional<PortalShape> portalShape = PortalShape.findEmptyPortalShape(level, pos, Direction.Axis.X)
                .or(() -> PortalShape.findEmptyPortalShape(level, pos.relative(Objects.requireNonNull(event.getFace())), Direction.Axis.X))
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

    private static void punishPlayer(PlayerInteractEvent.RightClickBlock event, Level level, BlockPos pos, Player player) {
        if (level instanceof ServerLevel serverLevel) {
            LightningBolt lightning = EntityType.LIGHTNING_BOLT.create(serverLevel);
            if (lightning != null) {
                lightning.setPos(player.getX(), player.getY(), player.getZ());
                lightning.setVisualOnly(true);
                serverLevel.addFreshEntity(lightning);
            }
        }

        player.addEffect(new MobEffectInstance(MobEffects.WITHER, 100, 2));

        final int radius = 4;
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    BlockPos currentPos = pos.offset(x, y, z);
                    if (level.getBlockState(currentPos).is(Blocks.OBSIDIAN)) {
                        level.setBlock(currentPos, Blocks.AIR.defaultBlockState(), 3);
                    }
                }
            }
        }

        level.setBlock(pos, Blocks.SPRUCE_SIGN.defaultBlockState(), 3);
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof SignBlockEntity sign) {
            SignText signText = new SignText();

            signText = signText.setMessage(0, Component.literal("Você não possui").withStyle(ChatFormatting.RED));
            signText = signText.setMessage(1, Component.literal("a grande").withStyle(ChatFormatting.RED));
            signText = signText.setMessage(2, Component.literal("CHAVE MESTRA!").withStyle(ChatFormatting.RED));

            sign.setText(signText, true);
        }


        player.displayClientMessage(Component.translatable("message.createsantalucia.nether_portal_needs_key").withStyle(ChatFormatting.RED), true);
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
    }
}