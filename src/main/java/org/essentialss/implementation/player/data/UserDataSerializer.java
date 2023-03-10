package org.essentialss.implementation.player.data;

import net.kyori.adventure.text.Component;
import org.essentialss.api.config.value.CollectionConfigValue;
import org.essentialss.api.config.value.ConfigValue;
import org.essentialss.api.player.data.SGeneralUnloadedData;
import org.essentialss.api.world.points.OfflineLocation;
import org.essentialss.api.world.points.home.SHomeBuilder;
import org.essentialss.implementation.EssentialsSMain;
import org.essentialss.implementation.config.value.ListDefaultConfigValueImpl;
import org.essentialss.implementation.config.value.position.HomeConfigValue;
import org.essentialss.implementation.config.value.position.LocationConfigValue;
import org.essentialss.implementation.config.value.primitive.BooleanConfigValue;
import org.essentialss.implementation.config.value.simple.ComponentConfigValue;
import org.essentialss.implementation.config.value.simple.DateTimeConfigValue;
import org.spongepowered.api.Sponge;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.hocon.HoconConfigurationLoader;
import org.spongepowered.configurate.serialize.SerializationException;

import java.io.File;
import java.time.LocalDateTime;
import java.util.List;

final class UserDataSerializer {

    private static final ConfigValue<Boolean> CAN_LOOSE_ITEMS_WHEN_USED = new BooleanConfigValue(true, "inventory", "LooseItemsWhenUsed");
    private static final ConfigValue<Boolean> PREVENT_TELEPORT_REQUESTS = new BooleanConfigValue(false, "other", "BlockingTeleportRequests");
    private static final BooleanConfigValue IS_IN_JAIL = new BooleanConfigValue(false, "jail", "In");
    private static final DateTimeConfigValue RELEASED_FROM_JAIL = new DateTimeConfigValue("jail", "ReleasedOn");
    private static final ComponentConfigValue DISPLAY_NAME = new ComponentConfigValue("other", "DisplayName");

    private static final CollectionConfigValue<OfflineLocation> BACK_LOCATIONS = new ListDefaultConfigValueImpl<>(new LocationConfigValue("placement"),
                                                                                                                  "locations", "back");

    private static final CollectionConfigValue<SHomeBuilder> HOMES = new ListDefaultConfigValueImpl<>(new HomeConfigValue(), "homes");

    private UserDataSerializer() {
        throw new RuntimeException("Should not create");
    }

    @SuppressWarnings("DuplicateThrows")
    static void load(SGeneralUnloadedData userData) throws ConfigurateException, SerializationException {
        File folder = Sponge.configManager().pluginConfig(EssentialsSMain.plugin().container()).directory().toFile();
        File file = new File(folder, "data/players/" + userData.uuid() + ".conf");
        HoconConfigurationLoader loader = HoconConfigurationLoader.builder().file(file).build();
        CommentedConfigurationNode root = loader.load();

        boolean isInJail = IS_IN_JAIL.parseDefault(root);
        if (isInJail && (userData instanceof AbstractProfileData)) {
            AbstractProfileData apd = ((AbstractProfileData) userData);
            LocalDateTime releasedFromJailTime = RELEASED_FROM_JAIL.parse(root);
            apd.isInJail = true;
            apd.releaseFromJail = releasedFromJailTime;
        }

        Component displayName = DISPLAY_NAME.parse(root);
        userData.setDisplayName(displayName);

        List<OfflineLocation> backLocations = BACK_LOCATIONS.parse(root);
        userData.setBackTeleportLocations(backLocations);

        List<SHomeBuilder> homes = HOMES.parse(root);
        if (null != homes) {
            userData.setHomes(homes);
        }
    }

    @SuppressWarnings("DuplicateThrows")
    static void save(SGeneralUnloadedData userData) throws ConfigurateException, SerializationException {
        File folder = Sponge.configManager().pluginConfig(EssentialsSMain.plugin().container()).directory().toFile();
        File file = new File(folder, "data/players/" + userData.uuid() + ".conf");
        HoconConfigurationLoader loader = HoconConfigurationLoader.builder().file(file).build();
        CommentedConfigurationNode root = loader.createNode();
        IS_IN_JAIL.set(root, userData.isInJail());
        PREVENT_TELEPORT_REQUESTS.set(root, userData.isPreventingTeleportRequests());
        CAN_LOOSE_ITEMS_WHEN_USED.set(root, userData.canLooseItemsWhenUsed());
        RELEASED_FROM_JAIL.set(root, userData.releasedFromJailTime().orElse(null));
        DISPLAY_NAME.set(root, userData.hasSetDisplayName() ? userData.displayName() : null);
        BACK_LOCATIONS.set(root, userData.backTeleportLocations());
        //HOMES.set(root, userData.homes().stream().map(SHome::builder).collect(Collectors.toList()));

        loader.save(root);
    }
}
