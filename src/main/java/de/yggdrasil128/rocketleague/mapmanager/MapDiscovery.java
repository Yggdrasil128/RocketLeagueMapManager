package de.yggdrasil128.rocketleague.mapmanager;

import de.yggdrasil128.rocketleague.mapmanager.config.RLMap;
import de.yggdrasil128.rocketleague.mapmanager.config.RLMapMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.HashMap;

public class MapDiscovery {
	private static MapDiscovery task = null;
	
	public synchronized static MapDiscovery get() {
		return task;
	}
	
	public synchronized static void start(RLMapManager rlMapManager) {
		if(task != null && !task.isDone()) {
			throw new IllegalStateException("Already running");
		}
		task = new MapDiscovery(rlMapManager);
	}
	
	private static Logger logger;
	
	private final RLMapManager rlMapManager;
	private final Thread thread;
	private int progress, progressTarget;
	private Throwable throwable = null;
	
	private MapDiscovery(RLMapManager rlMapManager) {
		if(logger == null) {
			logger = LoggerFactory.getLogger(MapDiscovery.class);
		}
		this.rlMapManager = rlMapManager;
		thread = new Thread(this::run);
		progress = 0;
		progressTarget = 0;
		thread.start();
	}
	
	public boolean isDone() {
		return !thread.isAlive();
	}
	
	public float getProgress() {
		if(progressTarget == 0) {
			return 0;
		}
		return (float) progress / progressTarget;
	}
	
	public Throwable getThrowable() {
		return throwable;
	}
	
	private void run() {
		logger.info("Starting Map Discovery");
		try {
			HashMap<Long, RLMap> maps = discoverMaps();
			progressTarget = maps.size();
			for(Long mapID : maps.keySet()) {
				RLMapMetadata mapMetadata = rlMapManager.getConfig().getMapMetadata(mapID);
				
				File imageFile = mapMetadata.getImageFile();
				if(imageFile != null && !imageFile.exists()) {
					mapMetadata.fetchFromWorkshop();
				}
				
				progress++;
			}
			
			rlMapManager.getConfig().save();
			
			rlMapManager.setMaps(maps);
		} catch(Exception e) {
			throwable = e;
			logger.error("Fatal error", e);
		}
		logger.info("Finished Map Discovery");
	}
	
	private HashMap<Long, RLMap> discoverMaps() {
		File workshopFolder = rlMapManager.getConfig().getWorkshopFolder();
		if(workshopFolder == null) {
			throw new IllegalStateException("Workshop folder doesn't exist");
		}
		
		HashMap<Long, RLMap> maps = new HashMap<>();
		
		File[] mapFolders = workshopFolder.listFiles();
		if(mapFolders == null) {
			return maps;
		}
		for(File mapFolder : mapFolders) {
			RLMap map = discoverMap(mapFolder);
			if(map != null) {
				maps.put(map.getID(), map);
			}
		}
		
		return maps;
	}
	
	private RLMap discoverMap(File mapFolder) {
		long id;
		try {
			id = Long.parseLong(mapFolder.getName());
		} catch(Exception e) {
			return null;
		}
		
		String[] files = mapFolder.list((dir, name) -> name.endsWith("udk"));
		if(files == null || files.length == 0) {
			return null;
		}
		
		File udkFile = new File(mapFolder, files[0]);
		
		return new RLMap(id, udkFile);
	}
}