package ure.dagger;

import dagger.Component;
import ure.UCommander;
import ure.actions.UAction;
import ure.actors.UActorCzar;
import ure.areas.UArea;
import ure.areas.UCartographer;
import ure.areas.UCell;
import ure.behaviors.UBehavior;
import ure.examplegame.ExampleGame;
import ure.terrain.TerrainI;
import ure.terrain.UTerrainCzar;
import ure.things.ThingI;
import ure.things.UThingCzar;
import ure.ui.UCamera;

import javax.inject.Singleton;

@Singleton
@Component(modules =  { AppModule.class })
public interface AppComponent {
    void inject(UTerrainCzar czar);
    void inject(UThingCzar czar);
    void inject(UActorCzar czar);
    void inject(UCartographer cartographer);
    void inject(UCommander cmdr);
    void inject(ExampleGame game);
    void inject(UAction act);
    void inject(ThingI thingi);
    void inject(UArea uarea);
    void inject(UCamera cam);
    void inject(UCell cel);
    void inject(TerrainI terr);
    void inject(UBehavior behav);
}