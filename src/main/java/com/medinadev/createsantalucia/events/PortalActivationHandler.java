package com.medinadev.createsantalucia.events;

import com.medinadev.createsantalucia.item.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.portal.PortalShape;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.AABB;
import java.util.List;
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

        // Lógica do portal do Nether (inalterada)
        if (clickedState.is(Blocks.OBSIDIAN)) {
            if (heldStack.getItem() == ModItems.MASTER_KEY.get()) {
                player.swing(event.getHand());
                activateNetherPortal(event, level, pos, player);
            } else if (heldStack.getItem() == Items.FLINT_AND_STEEL) {
                destroyObsidianAndPreventPortal(event, level, pos, player);
            }
            return;
        }

        // Nova lógica do portal do End
        if (clickedState.is(Blocks.END_PORTAL_FRAME)) {
            if (heldStack.getItem() == ModItems.MASTER_KEY.get()) {
                // A animação agora é tratada no lado do cliente para evitar atrasos
                if (level.isClientSide) {
                    player.swing(event.getHand());
                } else {
                    handleEndPortalTransformation(event, level, pos, player);
                }
            } else if (heldStack.getItem() == Items.ENDER_EYE) {
                if (!level.isClientSide) {
                    preventEndPortalWithEye(event, player);
                }
            }
        }
    }

    private static void activateNetherPortal(PlayerInteractEvent.RightClickBlock event, Level level, BlockPos pos, Player player) {
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

    private static void destroyObsidianAndPreventPortal(PlayerInteractEvent.RightClickBlock event, Level level, BlockPos pos, Player player) {
        final int radius = 6;
        int blocksDestroyed = 0;

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

        Component message = blocksDestroyed > 0
                ? Component.translatable("message.createsantalucia.obsidian_destroyed", blocksDestroyed).withStyle(ChatFormatting.RED)
                : Component.translatable("message.createsantalucia.no_obsidian_found").withStyle(ChatFormatting.YELLOW);

        player.displayClientMessage(message, true);

        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
    }


    /**
     * Lógica final com verificação manual da estrutura do portal.
     */
    private static void handleEndPortalTransformation(PlayerInteractEvent.RightClickBlock event, Level level, BlockPos clickedPos, Player player) {
        event.setCanceled(true);

        for (Direction.Axis axis : new Direction.Axis[]{Direction.Axis.Z, Direction.Axis.X}) {
            BlockPos portalCenter = findPortalCenter(level, clickedPos, axis);

            if (portalCenter != null) {
                // ESTRUTURA VÁLIDA ENCONTRADA!
                transformPortal(level, portalCenter, axis);

                if (level instanceof ServerLevel serverLevel) {
                    // 1. Cria o raio
                    LightningBolt lightning = EntityType.LIGHTNING_BOLT.create(serverLevel);
                    if (lightning != null) {
                        lightning.setPos(portalCenter.getX() + 0.5, portalCenter.getY(), portalCenter.getZ() + 0.5);
                        lightning.setVisualOnly(true);
                        serverLevel.addFreshEntity(lightning);
                    }

                    // 2. Encontra todos os jogadores em um raio de 20 blocos
                    AABB searchArea = new AABB(portalCenter).inflate(20);
                    // --- CORREÇÃO AQUI ---
                    // A variável foi renomeada para "nearbyPlayer" para evitar o conflito.
                    List<ServerPlayer> playersNearby = serverLevel.getPlayers(nearbyPlayer -> nearbyPlayer.getBoundingBox().intersects(searchArea));

                    // 3. Aplica os efeitos em cada jogador encontrado
                    for (ServerPlayer p : playersNearby) {
                        p.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 100, 0));
                        p.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 0));
                    }

                    // 4. Lógica das partículas
                    ItemParticleOption particleEffect = new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(Items.ENDER_EYE));
                    double x = portalCenter.getX() + 0.5;
                    double y = portalCenter.getY() + 1.0;
                    double z = portalCenter.getZ() + 0.5;
                    int particleCount = 40;
                    double xOffset = 1.5D;
                    double yOffset = 1.5D;
                    double zOffset = 1.5D;
                    double speed = 0.02D;
                    serverLevel.sendParticles(particleEffect, x, y, z, particleCount, xOffset, yOffset, zOffset, speed);
                }

                player.displayClientMessage(Component.translatable("message.createsantalucia.end_portal_activated").withStyle(ChatFormatting.DARK_PURPLE), true);
                event.setCancellationResult(InteractionResult.SUCCESS);
                return;
            }
        }

        player.displayClientMessage(Component.translatable("message.createsantalucia.nether_portal_no_frame").withStyle(ChatFormatting.RED), true);
        event.setCancellationResult(InteractionResult.FAIL);
    }

    /**
     * Procura pelo centro de um portal válido a partir de um bloco clicado.
     * Retorna o BlockPos do centro se o portal for válido, caso contrário, retorna null.
     */
    private static BlockPos findPortalCenter(Level level, BlockPos startPos, Direction.Axis axis) {
        // O centro do portal 3x3 está sempre 2 blocos distante da moldura
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

    /**
     * Verifica se os 12 blocos da moldura estão corretos ao redor de um centro potencial.
     */
    private static boolean isPortalFrameValid(Level level, BlockPos center, Direction.Axis axis) {
        Direction primaryDir = Direction.get(Direction.AxisDirection.POSITIVE, axis); // Z+ ou X+
        Direction secondaryDir = primaryDir.getClockWise(); // X+ ou Z-

        // Verifica a linha de cima e de baixo
        for (int i = -1; i <= 1; i++) {
            if (!level.getBlockState(center.relative(primaryDir, i).relative(secondaryDir, 2)).is(Blocks.END_PORTAL_FRAME)) return false;
            if (!level.getBlockState(center.relative(primaryDir, i).relative(secondaryDir, -2)).is(Blocks.END_PORTAL_FRAME)) return false;
        }
        // Verifica as laterais
        for (int i = -1; i <= 1; i++) {
            if (!level.getBlockState(center.relative(primaryDir, 2).relative(secondaryDir, i)).is(Blocks.END_PORTAL_FRAME)) return false;
            if (!level.getBlockState(center.relative(primaryDir, -2).relative(secondaryDir, i)).is(Blocks.END_PORTAL_FRAME)) return false;
        }
        return true;
    }

    /**
     * Transforma a estrutura verificada em um portal ativo.
     */
    private static void transformPortal(Level level, BlockPos center, Direction.Axis axis) {
        Direction primaryDir = Direction.get(Direction.AxisDirection.POSITIVE, axis);
        Direction secondaryDir = primaryDir.getClockWise();

        // Transforma o centro 3x3
        for (int i = -1; i <= 1; i++) {
            for (int j = -1; j <= 1; j++) {
                level.setBlock(center.relative(primaryDir, i).relative(secondaryDir, j), Blocks.END_PORTAL.defaultBlockState(), 2);
            }
        }

        // Transforma a moldura 5x5
        for (int i = -2; i <= 2; i++) {
            for (int j = -2; j <= 2; j++) {
                if (Math.abs(i) == 2 || Math.abs(j) == 2) { // Apenas a borda
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