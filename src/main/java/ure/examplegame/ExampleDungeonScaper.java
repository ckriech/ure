package ure.examplegame;

import ure.areas.UArea;
import ure.areas.ULandscaper;
import ure.math.UColor;
import ure.terrain.UTerrain;
import ure.terrain.UTerrainCzar;
import ure.things.UThingCzar;

import java.util.ArrayList;
import java.util.Collections;

public class ExampleDungeonScaper extends ULandscaper {


    class RoomStruct{
        int x, y, w, h, weight;
        int seen, start, exit;
        int entered, hit;
        int size;
        UColor forceColor = null;
        RoomStruct(int x_, int y_, int w_, int h_){
            x = x_; y = y_; w = w_; h = h_;
            weight = random.nextInt(255);

            seen = 0;
            start = 0;
            exit = 0;

            entered = 0; // 0 no, 1 pathfind, 2 branch
            hit = 0;
            size = 0;
        }
        void print(){
            System.out.println("ROOM: x: " + x + " y: " + y + " w: " + w + " h: " + h + " weight: " + weight + " size: " + size);
        }
    }

    class RoomNeighbor{
        int x, y;
        RoomNeighbor(int x_, int y_) {
            x = x_;
            y = y_;
        }
    }

    UArea area;
    int roomMultiplier = 5;
    int totalRooms = 0;
    int[][] roomPointers;
    float branchChance = .25f; // Keep in mind this is a chance per roomMultiplier size,
                                // meaning if a room has 4 connection points to another room, it has four chances to make this roll.
    ArrayList<RoomStruct> rooms = new ArrayList<RoomStruct>();
    int ys;
    int xs;
    void makeRoom(int xStart, int yStart){
        if(roomPointers[yStart][xStart] != -1) return;
        int w = Math.max(1, random.nextInt(5));
        int h = Math.max(1, random.nextInt(5));

        for(int x = 0; x < w; x++) {
            for(int y = 0; y < h; y++) {
                if(yStart + y >= ys || xStart + x >= xs || roomPointers[yStart + y][xStart + x] != -1){
                    h = y;
                }
            }
        }
        h = Math.max(1, h);
        for(int y = 0; y < h; y++) {
            for(int x = 0; x < w; x++) {
                if(yStart + y >= ys || xStart + x >= xs || roomPointers[yStart + y][xStart + x] != -1){
                    w = x;
                }
            }
        }
        w = Math.max(1, w);
        RoomStruct room = new RoomStruct(xStart, yStart, w, h);
        for(int x = 0; x < w; x++) {
            for(int y = 0; y < h; y++) {
                roomPointers[yStart + y][xStart + x] = totalRooms;
                room.size++;
            }
        }
        room.print();
        totalRooms++;
        rooms.add(room);
    }

    public ExampleDungeonScaper(UTerrainCzar theTerrainCzar, UThingCzar theThingCzar) {
        super(theTerrainCzar, theThingCzar);
    }

    void floodRoom(RoomStruct r, UColor c, boolean walls){
        //TODO: Perhaps add a system to add a bunch of random things to the rooms (furniture/decals/etc).
        if(r.entered == 0) return;
        int x, y;
        for(y = roomMultiplier * r.y; y < roomMultiplier * (r.y + r.h); y++){
            for(x = roomMultiplier * r.x; x < roomMultiplier * (r.x + r.w); x++){
                UTerrain t = area.terrainAt(x, y);
                if (t != null) {
                    if (x == roomMultiplier * r.x || y == roomMultiplier * r.y) {
                        //t.bgColor.set(0, 0, 0);
                        //t.fgColor.set(0, 0, 0);
                        //TODO: If not a door, make it a wall.
                        //  Currently the world starts off as walls, so it's not needed right now,
                        //  but things *CAN* cover existing doors, so we gotta fix that.
                        //TODO: Add right/bottom sides of room walls too when we do this without a wall flood fill.

                        //if(walls) area.setTerrain(x, y, "wall");
                    }else{
                        //t.bgColor.set(c.r, c.g, c.b);
                        //t.fgColor.set(c.r, c.g, c.b);
                        area.setTerrain(x, y, "floor");
                    }
                }
            }
        }
    }

    //TODO: SOMEONE FIX THIS.  MM:A
/*    void colorRoom(RoomStruct r, UColor c){
        if(r.entered == 0) return;
        int x, y;
        for(y = roomMultiplier * r.y; y < roomMultiplier * (r.y + r.h); y++){
            for(x = roomMultiplier * r.x; x < roomMultiplier * (r.x + r.w); x++){
                UTerrain t = area.terrainAt(x, y);
                if (t != null) {
                    if (x == roomMultiplier * r.x || y == roomMultiplier * r.y) {
                    }else{
                        t.bgColor.set(c.r, c.g, c.b);
                        //area.setTerrain(x, y, "floor");
                    }
                }
            }
        }
    }
*/
    void floodRooms(){
        for(RoomStruct r: rooms){
            float f = ((float)(r.weight & 255)) / 255.f;
            floodRoom(r, new UColor(f, f, f), true);
            //MM:A
            //if(r.forceColor != null) colorRoom(r, r.forceColor);
        }
    }

    ArrayList<RoomNeighbor> ajacentRooms(RoomStruct room){
        ArrayList<RoomNeighbor> neighbors = new ArrayList<RoomNeighbor>();
        int lastRoom = -1;
        int r;
        if (room.y + -1 > 0) for (int x = 0; x < room.w; x++) {
            if (room.x + x < 0 || room.x + x >= xs) continue;
            r = roomPointers[room.y - 1][room.x + x];
            if(r != lastRoom && r != -1) neighbors.add(new RoomNeighbor(room.x + x, room.y - 1));
        }
        if (room.y + room.h < ys) for (int x = 0; x < room.w; x++) {
            if (room.x + x < 0 || room.x + x >= xs) continue;
            r = roomPointers[room.y + room.h][room.x + x];
            if(r != lastRoom && r != -1) neighbors.add(new RoomNeighbor(room.x + x, room.y + room.h));
        }
        if (room.x + -1 > 0) for (int y = 0; y < room.h; y++) {
            if (room.y + y < 0 || room.y + y >= ys) continue;
            r = roomPointers[room.y + y][room.x - 1];
            if(r != lastRoom && r != -1) neighbors.add(new RoomNeighbor(room.x - 1, room.y + y));
        }
        if (room.x + room.w < xs) for (int y = 0; y < room.h; y++) {
            if (room.y + y < 0 || (room.y + y) >= ys) continue;
            r = roomPointers[room.y + y][room.x + room.w];
            if(r != lastRoom && r != -1) neighbors.add(new RoomNeighbor(room.x + room.w, room.y + y));
        }
        return neighbors;
    }

    void connect(RoomStruct s, RoomStruct e){
        ArrayList<RoomNeighbor> r = ajacentRooms(s);
        Collections.shuffle(r);
        s.entered = 1;
        e.entered = 1;
        for(RoomNeighbor n : r){
            if(rooms.get(roomPointers[n.y][n.x]) == e){
                if(n.x == s.x + s.w || n.x == s.x - 1){ // E / W door
                    int x = 0;
                    if(n.x == s.x - 1) x = 5;
                    int y = random.nextInt(3) + 2;
                    area.setTerrain(x + n.x * roomMultiplier, y + n.y * roomMultiplier, "door");
                }else{ // N / S
                    int y = 0;
                    if(n.y == s.y - 1) y = 5;
                    int x = random.nextInt(3) + 2;
                    area.setTerrain(x + n.x * roomMultiplier, y + n.y * roomMultiplier, "door");
                }
                return;
            }
        }
    }

    int maxDepth = 256; // Should never get above like 20 or so
    boolean pathFind(RoomStruct room, int depth){
        //System.out.println("Depth: " + depth);
        //floodRoom(room, new UColor(0.0f, 1.0f, 0.0f), false);
        room.hit = 1;
        ArrayList<RoomNeighbor> r = ajacentRooms(room);
        if(depth >= maxDepth) return false;

        boolean ok = true;
        while(ok){
            int max = -1;
            int p;
            RoomStruct next = null;
            for(RoomNeighbor n : r) {
                p = roomPointers[n.y][n.x];
                if (rooms.get(p).hit == 0 && rooms.get(p).weight > max) {
                    //System.out.println(rooms.get(p).weight);
                    max = rooms.get(p).weight;
                    next = rooms.get(p);
                }
            }
            if(next == null){
                return false; // We're exhausted!
            }
            if(next.exit != 0){
                connect(room, next);
                return true; // Ding ding!
            }
            if(pathFind(next, depth + 1)){
                //floodRoom(next, new UColor(1.0f, 1.0f, 0.0f), false);
                connect(room, next);
                return true;
            }
        }
        return false;
    }

    @Override
    public void buildArea(UArea area_) {
        //NOTE:  Ideally you'd want to create this as a divisor of roomMultiplier + 1 so the east/south walls can be walls.
        area = area_;
        fillRect(area, "wall", 0,0,area.xsize - 1,area.ysize - 1);

        ys = (area.ysize - 1) / roomMultiplier;
        xs = (area.xsize - 1) / roomMultiplier;
        roomPointers = new int[ys][xs];

        int x, y;
        for(y = 0; y < ys; y++)
            for(x = 0; x < xs; x++)
                roomPointers[y][x] = -1;


        for(y = 0; y < ys; y++)
            for(x = 0; x < xs; x++)
                makeRoom(x, y);


        RoomStruct start = rooms.get(roomPointers[1][1]);
        RoomStruct exit = rooms.get(roomPointers[ys - 1][xs - 1]);
        start.start = 1;
        exit.exit = 1;

        if(!pathFind(rooms.get(roomPointers[1][1]), 0)){
            System.out.println("Something happened, we couldn't connect start to exit.");
        }



        /*MM:A//Debug, color all rooms from start to exit, then the start and exit.
        for(RoomStruct room : rooms) if (room.entered == 1) room.forceColor = new UColor(.3f, .3f, .15f);
        start.forceColor = new UColor(.13f, .23f, .13f);
        exit.forceColor = new UColor(.23f, .13f, .13f);*/

        //Connect random rooms.
        ArrayList<RoomStruct> branches = new ArrayList<RoomStruct>();
        for(RoomStruct room : rooms) {
            if (room.entered == 1) branches.add(room);
        }
        for(int i = 0; i < branches.size(); i++){
            RoomStruct room = branches.get(i);
            ArrayList<RoomNeighbor> r = ajacentRooms(room);
            Collections.shuffle(r);
            for(RoomNeighbor n : r) {
                RoomStruct targ = rooms.get(roomPointers[n.y][n.x]);
                if(targ.entered == 0 && random.nextFloat() <= branchChance){
                    connect(room, targ);
                    targ.entered = 1;
                    branches.add(targ);
                    break;
                }
            }
        }
        floodRooms();

        //Clean up
        roomPointers = null;
        rooms = null;
    }
}