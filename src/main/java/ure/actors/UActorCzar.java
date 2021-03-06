package ure.actors;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import com.fasterxml.jackson.databind.ObjectMapper;
import ure.actors.behaviors.BehaviorDeserializer;
import ure.sys.Injector;
import ure.sys.UCommander;

import javax.inject.Inject;

public class UActorCzar {

    private HashMap<String,UActor> actorsByName;

    @Inject
    ObjectMapper objectMapper;
    @Inject
    UCommander commander;

    public BehaviorDeserializer behaviorDeserializer;

    public UActorCzar() {
        Injector.getAppComponent().inject(this);
    }

    public void loadActors() {
        behaviorDeserializer = new BehaviorDeserializer(objectMapper);
        actorsByName = new HashMap<>();
        File jsonDir = new File(commander.config.getResourcePath() + "actors/");
        ArrayList<File> files = new ArrayList<File>(Arrays.asList(jsonDir.listFiles()));
        for (File resourceFile : files) {
            String resourceName = resourceFile.getName();
            if (resourceName.endsWith(".json")) {
                System.out.println("ACTORCZAR: loading " + resourceName);
                try {
                    InputStream inputStream = getClass().getResourceAsStream("/actors/" + resourceName);
                    UActor[] actorObjs = objectMapper.readValue(inputStream, UActor[].class);
                    for (UActor actor : actorObjs) {
                        actor.initialize();
                        actorsByName.put(actor.getName(), actor);
                    }
                } catch (IOException io) {
                    io.printStackTrace();
                }
            }
        }
    }

    public UActor getActorByName(String name) {
        UActor clone = (UActor)actorsByName.get(name).makeClone();
        clone.initialize();
        clone.setID(commander.generateNewID(clone));
        return clone;
    }

    public String[] getActorsByTag(String tag, int level) {
        ArrayList<UActor> actors = new ArrayList<>();
        for (String actorname : actorsByName.keySet()) {
            UActor actor = actorsByName.get(actorname);
            if (actor.isTagAndLevel(tag, level)) {
                actors.add(actor);
            }
        }
        String[] names = new String[actors.size()];
        int i = 0;
        for (UActor actor: actors) {
            names[i] = actor.getName();
            i++;
        }
        return names;
    }
}
