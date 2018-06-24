package ure;

import java.util.Iterator;

/**
 * Created by gilmore on 6/20/2018.
 *
 * A single xy grid cell of an Area
 *
 */
public class UCell implements UContainer {
    UREArea area;
    int x,y;
    URETerrain terrain;
    float sunBrightness;
    UCollection contents;
    boolean isSeen = false;

    public UCell(UREArea theArea, int thex, int they, URETerrain theTerrain) {
        contents = new UCollection(this);
        area = theArea;
        x = thex;
        y = they;
        setTerrain(theTerrain);
    }

    public void setTerrain(URETerrain theTerrain) {
        terrain = theTerrain;
        setSunBrightness(terrain.sunDefault);
    }
    public void setSunBrightness(float thebri) { sunBrightness = thebri; }

    public float sunBrightness() {
        return sunBrightness;
    }

    public void setSeen(boolean theseen) {
        isSeen = theseen;
    }
    public boolean isSeen() {
        return isSeen;
    }
    public URETerrain getTerrain() {
        return terrain;
    }

    public void addThing(UREThing thing) {
        contents.add(thing);
    }
    public void removeThing(UREThing thing) {
        contents.remove(thing);
        area.hearRemoveThing(thing);
    }
    public Iterator<UREThing> iterator() {
        return contents.iterator();
    }
    public int containerType() { return UContainer.TYPE_CELL; }
    public boolean willAcceptThing(UREThing thing) {
        if (terrain != null) {
            if (terrain.isPassable()) {
                return true;
            }
        }
        return false;
    }
    public int areaX() { return x; }
    public int areaY() { return y; }
    public UREArea area() { return area; }
}