package com.akah.blocklings.entity.blockling.goal.goals.misc;

import com.akah.blocklings.entity.blockling.BlocklingEntity;
import com.akah.blocklings.entity.blockling.goal.BlocklingGoal;
import com.akah.blocklings.entity.blockling.task.BlocklingTasks;
import com.akah.blocklings.entity.blockling.task.config.range.IntRangeProperty;
import com.akah.blocklings.util.BlocklingsComponent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.pathfinder.BlockPathTypes;

import javax.annotation.Nonnull;
import java.util.EnumSet;
import java.util.UUID;

/**
 * Follows the blockling's owner when out of range.
 */
public class BlocklingFollowGoal extends BlocklingGoal
{
    /**
     * The speed modifier.
     */
    private final double speedModifier = 1.0;

    /**
     * The distance at which the blockling stops following (arrives close enough).
     */
    @Nonnull
    private final IntRangeProperty stopDistance;

    /**
     * The distance at which the blockling starts following.
     */
    @Nonnull
    private final IntRangeProperty startDistance;

    /**
     * The navigator used for pathing.
     */
    @Nonnull
    private final PathNavigation navigation;

    /**
     * The owner of the blockling.
     */
    private LivingEntity owner;

    /**
     * The counter used to work out when to recalc the path.
     */
    private int timeToRecalcPath;

    /**
     * The malus from water.
     */
    private float oldWaterCost;

    /**
     * @param id the id associated with the goal's task.
     * @param blockling the blockling.
     * @param tasks the blockling tasks.
     */
    public BlocklingFollowGoal(@Nonnull UUID id, @Nonnull BlocklingEntity blockling, @Nonnull BlocklingTasks tasks)
    {
        super(id, blockling, tasks);

        this.navigation = blockling.getNavigation();

        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));

        // Rangos originales de la versión 1.16: mínimo 1, máximo 20, valor por defecto 4.
        // El rango máximo fue expandido a 64 en algún port posterior, pero eso rompió
        // la funcionalidad porque FOLLOW_RANGE (atributo de pathfinding) está fijado en 48.
        // Cuando startDistance > 48 el pathfinder no puede llegar al owner, navigation.isDone()
        // devuelve true y el goal muere en el primer tick — haciendo que el blockling ignore
        // el valor configurado y use el valor por defecto (4), por eso seguía desde ~5 bloques.
        properties.add(startDistance = new IntRangeProperty(
                "590fb919-6ac7-4af7-98ec-6e01919782c1", this,
                new BlocklingsComponent("task.property.follow_start_range.name"),
                new BlocklingsComponent("task.property.follow_start_range.desc"),
                1, 20, 4));

        properties.add(stopDistance = new IntRangeProperty(
                "99d39a22-3abe-4109-b493-dcb922f0c08a", this,
                new BlocklingsComponent("task.property.follow_stop_range.name"),
                new BlocklingsComponent("task.property.follow_stop_range.desc"),
                1, 20, 2));
    }

    @Override
    public boolean canUse()
    {
        if (blockling.isOrderedToSit())
        {
            return false;
        }

        if (!super.canUse())
        {
            return false;
        }

        LivingEntity owner = blockling.getOwner();

        if (owner == null)
        {
            return false;
        }
        else if (owner.isSpectator())
        {
            return false;
        }

        // El goal solo arranca cuando el jugador está MÁS LEJOS que startDistance.
        if (blockling.distanceToSqr(owner) < (double) (startDistance.getValue() * startDistance.getValue()))
        {
            return false;
        }

        this.owner = owner;
        return true;
    }

    @Override
    public boolean canContinueToUse()
    {
        if (blockling.isOrderedToSit())
        {
            return false;
        }

        if (!super.canContinueToUse())
        {
            return false;
        }

        if (owner == null)
        {
            return false;
        }

        // BUG 1 ELIMINADO — navigation.isDone():
        // En la versión 1.16, isDone() devolvía true solo cuando el blockling
        // HABÍA LLEGADO al destino (path completado). En 1.20.1 el comportamiento
        // cambió: isDone() devuelve true también cuando NO hay ningún path activo,
        // lo que ocurre justo después de llamar navigation.stop() en tick().
        //
        // Flujo roto en 1.20.1:
        //   Tick N   → goal arranca, start() → timeToRecalcPath = 0
        //   Tick N   → tick() → navigation.stop() [isDone = TRUE] → navigation.moveTo()
        //   Tick N+1 → canContinueToUse() → navigation.isDone() = TRUE → goal MUERTO
        //
        // Resultado: el goal arrancaba y moría en 2 ticks, el blockling nunca se movía.
        // La comprobación de isDone() se elimina — la distancia es suficiente para
        // controlar el ciclo de vida del goal.

        // Comportamiento esperado (histeresis):
        // - Arranca a seguir cuando la distancia es >= startDistance.
        // - Continúa siguiendo hasta volver a estar <= stopDistance.
        //
        // Así, con start=20 y stop=2, el blockling no te persigue dentro de 20
        // hasta que te alejas de verdad, y cuando ya empezó a seguirte se acerca
        // hasta la distancia de parada configurada.
        return blockling.distanceToSqr(owner) > (double) (stopDistance.getValue() * stopDistance.getValue());
    }

    @Override
    public void start()
    {
        super.start();

        timeToRecalcPath = 0;
        oldWaterCost = blockling.getPathfindingMalus(BlockPathTypes.WATER);
        blockling.setPathfindingMalus(BlockPathTypes.WATER, 0.0f);
    }

    @Override
    public void stop()
    {
        super.stop();

        owner = null;
        navigation.stop();
        blockling.setPathfindingMalus(BlockPathTypes.WATER, oldWaterCost);
    }

    @Override
    public void tick()
    {
        super.tick();

        blockling.getLookControl().setLookAt(owner, 10.0f, (float) blockling.getMaxHeadXRot());

        if (--timeToRecalcPath <= 0)
        {
            timeToRecalcPath = 10;

            if (!blockling.isLeashed() && !blockling.isPassenger())
            {
                // BUG 2 ELIMINADO — navigation.stop() antes de moveTo():
                // En 1.16 llamar stop() antes de moveTo() era inocuo porque isDone()
                // ya no se usaba como condición de salida crítica entre ticks.
                // En 1.20.1, stop() ponía isDone() = true y si canContinueToUse()
                // era evaluado justo en ese instante (o si moveTo() fallaba), el goal
                // moría prematuramente.
                //
                // moveTo() reemplaza el path interno directamente sin necesitar
                // un stop() explícito previo. Eliminamos stop() aquí.
                navigation.moveTo(owner, speedModifier);
            }
        }
    }
}
