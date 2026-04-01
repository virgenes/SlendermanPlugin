package me.dreamdevs.slender.api;

import me.dreamdevs.slender.api.inventory.handlers.ItemMenuListener;
import me.dreamdevs.slender.api.utils.Util;
import org.bukkit.plugin.java.JavaPlugin;

public class SlenderApi {

	public static JavaPlugin plugin;

	/** @deprecated LibsDisguises is no longer used. Internal disguise system is always active. */
	@Deprecated
	public static boolean isLibsDisguisedEnabled = false;

	public static void loadApi(JavaPlugin plugin) {
		SlenderApi.plugin = plugin;

		Util.sendPluginMessage("&aLoading SlendermanPlugin API...");

		ItemMenuListener.getInstance().register(plugin);

		Util.sendPluginMessage("&aInternal disguise system active.");
		Util.sendPluginMessage("&aSuccessfully registered SlendermanPlugin API!");
	}

}