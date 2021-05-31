// The package this file is a part of
package com.viral32111.bedteleport;

// Import required classes
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

// The main entry point class
public class BedTeleport extends JavaPlugin {

	// Globals for config values
	boolean isExperienceEnabled;
	boolean isExperienceScaleEnabled;
	int experienceAmount;
	double experienceScaleFactor;

	// This calculates the amount of experience a player has
	// https://minecraft.fandom.com/wiki/Experience#Leveling_up
	private int getTotalExperienceForPlayer( Player player ) {
		int level = player.getLevel();
		double experience;

		if ( level <= 16 ) {
			experience = Math.pow( level, 2 ) + 6 * level;
		} else if ( level <= 31 ) {
			experience = 2.5 * Math.pow( level, 2 ) - 40.5 * level + 360;
		} else {
			experience = 4.5 * Math.pow( level, 2 ) - 162.5 * level + 2220;
		}

		if ( level <= 15 ) {
			experience += ( 2 * level + 7 ) * player.getExp();
		} else if ( level <= 30 ) {
			experience += ( 5 * level - 38 ) * player.getExp();
		} else {
			experience += ( 9 * level - 158 ) * player.getExp();
		}

		return ( int ) Math.ceil( experience );
	}

	// This event runs whenever the plugin is loaded
	@Override
	public void onEnable() {

		// Copy the default configuration
		saveDefaultConfig();

		// Get configuration values
		ConfigurationSection experienceConfig = getConfig().getConfigurationSection( "experience" );
		ConfigurationSection experienceScaleConfig = experienceConfig.getConfigurationSection( "scale" );
		isExperienceEnabled = experienceConfig.getBoolean( "enabled" );
		isExperienceScaleEnabled = experienceScaleConfig.getBoolean( "enabled" );
		experienceAmount = experienceConfig.getInt( "amount" );
		experienceScaleFactor = experienceScaleConfig.getDouble( "factor" );

	}

	// This event runs whenever a chat command is executed
	@Override
	public boolean onCommand( @NotNull CommandSender sender, @NotNull Command command, @NotNull String name, String[] arguments ) {

		// Do not continue if the console ran the command
		if ( !( sender instanceof Player ) ) {
			getLogger().info( "This command is not usable by the console." );
			return true;
		}

		// Cast sender to player
		Player player = ( Player ) sender;

		// Is this the /bed command?
		if ( command.getName().equalsIgnoreCase( "bed" ) && player.hasPermission( "bedteleport.bed" ) ) {

			// Don't continue if the player isn't in the Overworld
			if ( player.getWorld().getEnvironment() != World.Environment.NORMAL ) {
				player.sendMessage( "You can only return to your bed when in the Overworld." );
				return true;
			}

			// Get the player's bed spawn location
			Location respawnPointLocation = player.getBedSpawnLocation();

			// Don't continue if their bed is missing or obstructed
			if ( respawnPointLocation == null ) {
				player.sendMessage( "You have no bed or it is obstructed!" );
				getLogger().info( player.getName() + " attempted to teleport back to their bed, but it is missing or obstructed." );
				return true;
			}

			// If experience is enabled, don't continue if the player doesn't have enough experience, if they do then subtract it
			if ( isExperienceEnabled ) {
				int currentExperience = getTotalExperienceForPlayer( player );

				if ( isExperienceScaleEnabled ) {
					double distanceToBed = respawnPointLocation.distance( player.getLocation() );
					int experienceRequired = ( int ) Math.round( distanceToBed * experienceScaleFactor );

					if ( currentExperience < experienceRequired ) {
						player.sendMessage( "You do not have enough experience to teleport back to your bed!" );
						getLogger().info( player.getName() + " attempted to teleport back to their bed, but they do not have enough experience." );
						return true;
					} else {
						player.giveExp( -experienceRequired );
						player.playSound( player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f );
					}
				} else {
					if ( currentExperience < experienceAmount ) {
						player.sendMessage( "You do not have enough experience to teleport back to your bed!" );
						getLogger().info( player.getName() + " attempted to teleport back to their bed, but they do not have enough experience." );
						return true;
					} else {
						player.giveExp( -experienceAmount );
						player.playSound( player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f );
					}
				}
			}

			// Teleport the player to their bed
			player.teleport( respawnPointLocation );
			player.sendMessage( "You have teleported back to your bed." );
			getLogger().info( player.getName() + " has teleported back to their bed at [ " + respawnPointLocation.getX() + ", " + respawnPointLocation.getY() + ", " + respawnPointLocation.getZ() + " ]" );

			return true;

		}

		return false;
	}

}
