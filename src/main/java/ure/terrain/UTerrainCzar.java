package ure.terrain;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Set;

import com.fasterxml.jackson.databind.ObjectMapper;
import ure.sys.Injector;
import ure.sys.UCommander;

import javax.inject.Inject;

/**
 * Load and manage all the terrain types and dole them out as needed.
 *
 * Generally, your game will make one of these and use it for all terrain.  It loads terrain definitions
 * from terrain.json in your resources folder by default.  This json can specify any public property of
 * your terrain subclasses, including anything you add to a TerrainDeco.
 */

public class UTerrainCzar {

    private  HashMap<Character,UTerrain> terrains;
    private  HashMap<String,UTerrain> terrainsByName;

    @Inject
    ObjectMapper objectMapper;
    @Inject
    UCommander commander;

    public UTerrainCzar(String jsonfilename) {
        this();
        loadTerrains();
    }

    public UTerrainCzar() {
        Injector.getAppComponent().inject(this);
    }

    /**
     * Load terrain definitions from a json file and create sample instances.
     *
     * This is done on creation, but you can rerun it to load a new terrain set.
     *
     */
    public void loadTerrains() {
        terrains = new HashMap<>();
        terrainsByName = new HashMap<>();
        File jsonDir = new File(commander.config.getResourcePath() + "terrain/");
        ArrayList<File> files = new ArrayList<File>(Arrays.asList(jsonDir.listFiles()));
        for (File resourceFile : files) {
            String resourceName = resourceFile.getName();
            if (resourceName.endsWith(".json")) {
                System.out.println("TERRAINCZAR: loading " + resourceName);
                try {
                    InputStream inputStream = getClass().getResourceAsStream("/terrain/" + resourceName);
                    UTerrain[] terrainObjs = objectMapper.readValue(inputStream, UTerrain[].class);
                    for (UTerrain terrain : terrainObjs) {
                        terrain.initialize();
                        terrains.put(terrain.getFilechar(), terrain);
                        terrainsByName.put(terrain.getName(), terrain);
                    }
                } catch (IOException io) {
                    io.printStackTrace();
                }
            }
        }
    }

    /**
     * Get a new instance of a terrain type with the given textfile character ID.
     *
     * @param thechar
     * @return
     */
    public UTerrain getTerrainForFilechar(char thechar) {
        UTerrain terrain = terrains.get(thechar).makeClone();
        terrain.setID(commander.generateNewID(terrain));
        return terrain;
    }

    /**
     * Get a new instance of a named terrain type.
     *
     * @param name
     * @return
     */
    public UTerrain getTerrainByName(String name) {
        return (UTerrain)(getTerrainForFilechar(terrainsByName.get(name).getFilechar()));
    }

    public Set<String> getAllTerrains() {
        return terrainsByName.keySet();
    }
}
