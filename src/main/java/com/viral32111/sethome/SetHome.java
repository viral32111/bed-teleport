// The package this file is a part of
package com.viral32111.sethome;

// Import required classes
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.EventHandler;
import org.jetbrains.annotations.NotNull;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

// The main entry point class
public class SetHome extends JavaPlugin {

	// This event runs whenever the plugin is loaded
	@Override
	public void onEnable() {

		// Copy the default configuration into the plugin's folder
		saveDefaultConfig();

		// Setup event handlers
		getServer().getPluginManager().registerEvents( new SetHomeListener(), this );

		/*
		ConfigurationSection experienceConfig = getConfig().getConfigurationSection( "experience" );
		ConfigurationSection experienceScaleConfig = experienceConfig.getConfigurationSection( "scale" );
		ConfigurationSection bedConfig = getConfig().getConfigurationSection( "bed" );
		ConfigurationSection respawnAnchor = getConfig().getConfigurationSection( "respawn-anchor" );

		getLogger().info( "experience.enabled = " + experienceConfig.getBoolean( "enabled" ) );
		getLogger().info( "experience.amount = " + experienceConfig.getInt( "amount" ) );
		getLogger().info( "experience.scale.enabled = " + experienceScaleConfig.getBoolean( "enabled" ) );
		getLogger().info( "experience.scale.factor = " + experienceScaleConfig.getDouble( "factor" ) );
		getLogger().info( "bed.full-sleep = " + bedConfig.getBoolean( "full-sleep" ) );
		getLogger().info( "respawn-anchor.use-charge = " + respawnAnchor.getBoolean( "use-charge" ) );
		getLogger().info( "respawn-anchor.charge-amount = " + respawnAnchor.getInt( "charge-amount" ) );
		*/

		// Print a startup message in the console
		//getLogger().info( "Successfully loaded!" );

	}

	// This event runs whenever the plugin is unloaded
	@Override
	public void onDisable() {

		// Print a shutdown message in the console
		//getLogger().info( "Goodbye." );

	}

	// This event runs whenever a chat command is executed
	@Override
	public boolean onCommand( @NotNull CommandSender sender, @NotNull Command command, @NotNull String name, String[] arguments ) {

		// Do not continue if the console ran the command
		if ( !( sender instanceof Player ) ) {
			getLogger().info( "This command is not usable by the console." );
			return false;
		}

		Player player = ( Player ) sender;

		if ( command.getName().equalsIgnoreCase( "bed" ) && player.hasPermission( "sethome.bed" ) ) {
			player.sendMessage( "Teleporting to your bed..." );
			return true;
		}

		if ( command.getName().equalsIgnoreCase( "anchor" ) && player.hasPermission( "sethome.anchor" ) ) {
			player.sendMessage( "Teleporting to your respawn anchor..." );
			return true;
		}

		return false;
	}

}
