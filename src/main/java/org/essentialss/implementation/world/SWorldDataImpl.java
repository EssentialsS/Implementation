package org.essentialss.implementation.world;

import org.essentialss.api.utils.arrays.UnmodifiableCollection;
import org.essentialss.api.utils.validation.ValidationRules;
import org.essentialss.api.utils.validation.Validator;
import org.essentialss.api.world.SWorldData;
import org.essentialss.api.world.points.SPoint;
import org.essentialss.api.world.points.home.SHomeBuilder;
import org.essentialss.api.world.points.jail.SJailSpawnPointBuilder;
import org.essentialss.api.world.points.spawn.SSpawnPoint;
import org.essentialss.api.world.points.spawn.SSpawnPointBuilder;
import org.essentialss.api.world.points.spawn.SSpawnType;
import org.essentialss.api.world.points.warp.SWarp;
import org.essentialss.api.world.points.warp.SWarpBuilder;
import org.essentialss.implementation.events.point.register.RegisterPointPostEventImpl;
import org.essentialss.implementation.events.point.register.RegisterPointPreEventImpl;
import org.essentialss.implementation.world.points.spawn.SSpawnPointImpl;
import org.essentialss.implementation.world.points.warps.SWarpsImpl;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.event.Cause;
import org.spongepowered.api.event.Event;
import org.spongepowered.api.world.World;
import org.spongepowered.api.world.server.ServerWorld;
import org.spongepowered.configurate.ConfigurateException;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Optional;

public class SWorldDataImpl implements SWorldData {

    private final @Nullable ResourceKey key;
    private final @Nullable String id;
    private final Collection<SPoint> points = new LinkedHashSet<>();

    public SWorldDataImpl(@NotNull SWorldDataBuilder builder) {
        this.key = builder.worldKey();
        this.id = builder.worldId();
        if ((null == this.key) && (null == this.id)) {
            throw new IllegalArgumentException("World has not been specified");
        }
        if ((null != this.id) && Sponge.isServerAvailable()) {
            throw new IllegalStateException("Server can be used. Use ResourceKey");
        }
        this.points.addAll(new Validator<>(builder.points()).rule(ValidationRules.notNull()).validate());
    }

    @Override
    public @NotNull Optional<World<?, ?>> spongeWorld() {
        if ((null != this.key) && Sponge.isServerAvailable()) {
            return Sponge.server().worldManager().world(this.key).map(sWorld -> sWorld);
        }
        if ((null != this.id) && Sponge.isClientAvailable()) {
            return Sponge
                    .client()
                    .world()
                    .filter(world -> world.context().toString().equalsIgnoreCase(this.id))
                    .map(sWorld -> sWorld);
        }
        return Optional.empty();
    }

    @Override
    public Optional<ResourceKey> worldKey() {
        return Optional.empty();
    }

    @Override
    public void reloadFromConfig() throws ConfigurateException {
        SWorldDataSerializer.load(this);
    }

    @Override
    public void clearPoints() {
        this.points.clear();
    }

    @Override
    public void saveToConfig() throws ConfigurateException {
        SWorldDataSerializer.save(this);
    }

    @Override
    public @NotNull UnmodifiableCollection<SPoint> points() {
        LinkedList<SPoint> list = new LinkedList<>(this.points);
        if (list
                .parallelStream()
                .filter(point -> point instanceof SSpawnPoint)
                .noneMatch(point -> ((SSpawnPoint) point).types().contains(SSpawnType.MAIN_SPAWN))) {
            this.spongeWorld().ifPresent(sWorld -> {
                list.add(new SSpawnPointImpl(new SSpawnPointBuilder()
                                                     .setPoint(sWorld.properties().spawnPosition().toDouble())
                                                     .setSpawnTypes(SSpawnType.MAIN_SPAWN), this));
            });
        }
        return new UnmodifiableCollection<>(list);
    }

    @Override
    public boolean register(@NotNull SHomeBuilder builder, boolean runEvent, @Nullable Cause cause) {
        throw new RuntimeException("home is not implemented");
    }

    @Override
    public boolean register(@NotNull SSpawnPointBuilder builder, boolean runEvent, @Nullable Cause cause) {
        new Validator<>(builder.point()).notNull().validate();
        SPoint spawnPoint = new SSpawnPointImpl(builder, this);
        boolean register = this.register(spawnPoint, runEvent, cause);
        if (!register) {
            return false;
        }
        if (builder.spawnTypes().contains(SSpawnType.MAIN_SPAWN)) {
            //noinspection DataFlowIssue
            Optional<World<?, ?>> opWorld = this.spongeWorld();
            if (opWorld.isPresent()) {
                opWorld.get().properties().setSpawnPosition(builder.point().toInt());
                return true;
            }
            if (Sponge.isServerAvailable()) {
                Sponge
                        .server()
                        .worldManager()
                        .loadProperties(this.key)
                        .thenAccept(opProperties -> opProperties.ifPresent(
                                properties -> properties.setSpawnPosition(spawnPoint.position().toInt())));
            }
        }
        return true;
    }

    @Override
    public boolean register(@NotNull SWarpBuilder builder, boolean runEvent, @Nullable Cause cause) {
        new Validator<>(builder.name()).notNull().validate();

        Optional<SWarp> opWarp = this
                .warps()
                .parallelStream()
                .filter(warp -> warp.position().equals(builder.point()))
                .findAny();
        if (opWarp.isPresent()) {
            throw new IllegalArgumentException(
                    "Another warp (" + opWarp.get().identifier() + ") with that location has been found ");
        }
        //noinspection DataFlowIssue
        if (this.warp(builder.name()).isPresent()) {
            throw new IllegalArgumentException("Another warp has the name of " + builder.name());
        }
        return this.register(new SWarpsImpl(builder, this), runEvent, cause);
    }

    @Override
    public boolean register(@NotNull SJailSpawnPointBuilder builder, boolean runEvent, @Nullable Cause cause) {
        throw new RuntimeException("Jail is not implemented");
    }

    @Override
    public boolean isWorld(@NotNull World<?, ?> world) {
        if ((world instanceof ServerWorld) && ((ServerWorld) world).key().equals(this.key)) {
            return true;
        }
        return world.context().toString().equalsIgnoreCase(this.id);
    }

    private boolean register(@NotNull SPoint point, boolean runEvents, @Nullable Cause cause) {
        if (runEvents) {
            if (null == cause) {
                throw new IllegalArgumentException("Cause cannot be null when running events");
            }
            RegisterPointPreEventImpl preEvent = new RegisterPointPreEventImpl(point, cause);
            Sponge.eventManager().post(preEvent);
            if (preEvent.isCancelled()) {
                return false;
            }
        }
        boolean added = this.points.add(point);
        if (runEvents) {
            Event postEvent = new RegisterPointPostEventImpl(point, cause);
            Sponge.eventManager().post(postEvent);
        }
        return added;

    }

    @Override
    public @NotNull String identifier() {
        if (null != this.id) {
            return this.id;
        }
        //noinspection DataFlowIssue
        return this.key.formatted();
    }
}
