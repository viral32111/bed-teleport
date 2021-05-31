package com.viral32111.sethome;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import io.papermc.paper.event.player.PlayerDeepSleepEvent;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.plugin.java.JavaPlugin;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.UUID;
import java.util.logging.Logger;

// https://papermc.io/javadocs/paper/1.16/io/papermc/paper/event/player/PlayerDeepSleepEvent.html
// https://papermc.io/javadocs/paper/1.16/org/bukkit/event/player/PlayerBedEnterEvent.html

public class SetHomeListener implements Listener {

	File dataFolder;
	Logger logger;

	public SetHomeListener( SetHome setHome ) {
		dataFolder = setHome.getDataFolder();
		logger = setHome.getLogger();
	}

	@EventHandler
	public void onPlayerBedEnter( PlayerBedEnterEvent event ) {

		PlayerBedEnterEvent.BedEnterResult bedResult = event.getBedEnterResult();

		if ( bedResult == PlayerBedEnterEvent.BedEnterResult.OBSTRUCTED || bedResult == PlayerBedEnterEvent.BedEnterResult.TOO_FAR_AWAY ) return;

		UUID playerID = event.getPlayer().getUniqueId();

		File dataFile = new File( dataFolder, "beds.yml" );

		if ( !dataFile.exists() ) {
			try {
				dataFile.createNewFile();
			} catch ( IOException exception ) {
				logger.severe( "An error occured trying to create the beds.yml file: " + exception.getMessage() );
			}
		};

		FileConfiguration dataConfig = new YamlConfiguration();

		try {
			dataConfig.load( dataFile );
		} catch ( Exception exception ) {
			logger.severe( "An error occured trying to load the beds.yml file: " + exception.getMessage() );
		}

		ConfigurationSection playerBedData = dataConfig.getConfigurationSection( playerID.toString() );
		if ( playerBedData == null ) {
			playerBedData = dataConfig.createSection( playerID.toString() );
			logger.info( "Creating bed location data section for " + event.getPlayer().getName() );
		}

		playerBedData.set( "x", event.getBed().getX() );
		playerBedData.set( "y", event.getBed().getY() );
		playerBedData.set( "z", event.getBed().getZ() );

		try {
			dataConfig.save( dataFile );
		} catch ( IOException exception ) {
			logger.severe( "An error occured trying to save the beds.yml file: " + exception.getMessage() );
		}

		logger.info( "Saved bed location data for " + event.getPlayer().getName() );

	}

}