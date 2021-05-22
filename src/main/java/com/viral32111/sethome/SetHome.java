// The package this file is a part of
package com.viral32111.sethome;

// Import the base Bukkit plugin class
import org.bukkit.plugin.java.JavaPlugin;

// The main entry point class
public class SetHome extends JavaPlugin {

	// This event runs whenever the plugin is loaded
	@Override
	public void onEnable() {

		// Copy the default configuration into the plugin's folder
		saveDefaultConfig();

		// Print a startup message in the console
		getLogger().info( "Successfully loaded!" );

	}

	// This event runs whenever the plugin is unloaded
	@Override
	public void onDisable() {

		// Print a shutdown message in the console
		getLogger().info( "Goodbye." );

	}

}
