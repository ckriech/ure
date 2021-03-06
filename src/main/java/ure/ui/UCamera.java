package ure.ui;

import ure.actors.UActor;
import ure.areas.UArea;
import ure.areas.UCell;
import ure.math.UColor;
import ure.math.USimplexNoise;
import ure.render.URenderer;
import ure.sys.Injector;
import ure.sys.UAnimator;
import ure.sys.UCommander;
import ure.terrain.UTerrain;
import ure.things.UThing;
import ure.ui.modals.UModal;
import ure.ui.particles.UParticle;

import javax.inject.Inject;
import java.util.*;

/**
 * A view pane into a UREArea
 *
 */

public class UCamera extends View implements UAnimator {

    @Inject
    UCommander commander;

    public UArea area;
    private URenderer renderer;
    float zoom = 1.0f;
    public int columns, rows;
    private int centerColumn, centerRow;

    public int leftEdge, topEdge, rightEdge, bottomEdge;
    private ULightcell lightcells[][];
    private HashSet<UActor> visibilitySources;

    private UModal modal;

    public static int PINSTYLE_NONE = 0;
    public static int PINSTYLE_SOFT = 1;
    public static int PINSTYLE_SCREENS = 2;
    public static int PINSTYLE_HARD = 3;

    public boolean rendering;

    private class UShadow {
        float start, end;
        public UShadow(float thes, float thee) {
            start = thes;
            end = thee;
        }
        public boolean contains(UShadow other) {
            if (start <= other.start && end >= other.end)
                return true;
            return false;
        }
        public void projectTile(int row, int col) {
            float topLeft = (float)col / ((float)row + 2f);
            float bottomRight = ((float)col + 1f) / ((float)row + 1f);
            start = topLeft;
            end = bottomRight;
        }
    }
    private class UShadowLine {
        LinkedList<UShadow> _shadows;
        public UShadowLine() {
            _shadows = new LinkedList<UShadow>();
        }
        public void add(UShadow shadow) {
            int index = 0;
            for (index = 0;index < _shadows.size();index++)
                if (_shadows.get(index).start >= shadow.start) break;
            UShadow overlappingPrevious = null;
            if (index > 0 && _shadows.get(index - 1).end > shadow.start)
                overlappingPrevious = _shadows.get(index - 1);
            UShadow overlappingNext = null;
            if (index < _shadows.size() && _shadows.get(index).start < shadow.end)
                overlappingNext = _shadows.get(index);
            if (overlappingNext != null)
                if (overlappingPrevious != null) {
                    overlappingPrevious.end = overlappingNext.end;
                    _shadows.remove(index);
                } else {
                    overlappingNext.start = shadow.start;
                }
                else {
                if (overlappingPrevious != null)
                    overlappingPrevious.end = shadow.end;
                else
                    if (index < _shadows.size())
                        _shadows.add(index, shadow);
                    else
                        _shadows.addLast(shadow);
            }
        }
        public boolean isInShadow(UShadow projection) {
            for (UShadow shadow : _shadows)
                if (shadow.contains(projection)) return true;
            return false;
        }
        public boolean isFullShadow() {
            if (_shadows.size() == 1) {
                for (UShadow shadow: _shadows)
                    if (shadow.start == 0f && shadow.end == 1f)
                        return true;
            }
            return false;
        }
    }

    public UCamera(URenderer theRenderer, int x, int y, int width, int height) {
        Injector.getAppComponent().inject(this);
        renderer = theRenderer;
        visibilitySources = new HashSet<>();
        setBounds(x, y, width, height);
        setupGrid();
        lightcells = new ULightcell[columns][rows];
        USimplexNoise noise = new USimplexNoise();
        float[] scales = new float[]{15f,22f};
        for (int col = 0; col<columns; col++) {
            for (int row = 0;row < rows;row++) {
                float cloud = noise.multi(col*2,row,scales) - 0.3f;
                if (cloud < 0.3f) cloud = 0f;
                lightcells[col][row] = new ULightcell(this, col, row, cloud);
            }
        }
    }

    public int getCenterColumn() {
        return centerColumn;
    }

    public int getCenterRow() {
        return centerRow;
    }

    public int cellWidth() {
        return commander.config.getGlyphWidth();
    }

    public int cellHeight() {
        return commander.config.getGlyphHeight();
    }

    public void moveTo(UArea theArea, int thex, int they) {
        boolean areachange = false;
        if (area != null && theArea != area) {
            areachange = true;
        }
        area = theArea;
        moveTo(thex,they);
        if (areachange) {
            renderer.render();
        }
    }

    public void moveTo(int thex, int they) {
        centerColumn = thex;
        centerRow = they;
        setupGrid();
    }

    private void setupGrid() {
        float cellWidth = (float)cellWidth() * zoom;
        float cellHeight = (float)cellHeight() * zoom;
        columns = (int)(width / cellWidth) + 2;
        rows = (int)(height / cellHeight) + 2;
        System.out.println("cell: " + cellWidth + "," + cellHeight + "  cols: " + columns + " rows: " + rows);
        leftEdge = centerColumn - (columns / 2);
        topEdge = centerRow - (rows / 2);
        rightEdge = leftEdge + columns;
        bottomEdge = topEdge + rows;
    }

    public int getWidthInCells() { return columns; }
    public int getHeightInCells() { return rows; }

    void renderLights() {
        for (int i = 0; i< columns; i++) {
            for (int j = 0; j< rows; j++) {
                lightcells[i][j].wipe();
                lightcells[i][j].setSunBrightness(area.sunBrightnessAt(leftEdge +i, topEdge +j));
            }
        }
        renderSun();
        renderVisible();
        for (ULight light : area.lights()) {
            if (light.canTouch(this)) {
                projectLight(light);
            }
        }
    }

    void renderSun() {
        for (int col = 0; col < columns; col++) {
            for (int row = 0; row < rows; row++) {
                float sun = getSunBrightnessAt(col,row);
                if (sun < 0.1f) {
                    int litnear = 0;
                    for (int k = -1;k < 2;k++) {
                        for (int l = -1;l < 2;l++) {
                            if (getSunBrightnessAt(col + k, row + l) > 0.9f) {
                                litnear++;
                            }
                        }
                    }
                    if (litnear > 0) {
                        if (area.blocksLight(col + leftEdge, row + topEdge))
                            sun = 1f;
                        else
                            sun = 0.5f;
                    }
                }
                lightcells[col][row].setRenderedSun(sun);
            }
        }
    }
    float getSunBrightnessAt(int col, int row) {
        if (isValidCell(col, row))
            return lightcells[col][row].getSunBrightness();
        return 0f;
    }

    float cloudPatternAt(int cx, int cy) {
        while (cx >= getWidthInCells())
            cx = cx - getWidthInCells();
        while (cx < 0)
            cx = cx + getWidthInCells();
        while (cy >= getHeightInCells())
            cy = cy - getHeightInCells();
        while (cy < 0)
            cy = cy + getHeightInCells();
        return lightcells[cx][cy].cloudPattern();
    }

    void renderVisible() {
        for (int col = 0; col < columns; col++) {
            for (int row = 0; row < rows; row++) {
                setVisibilityAt(col, row, 0f);
            }
        }
        Iterator<UActor> players = visibilitySources.iterator();
        while (players.hasNext()) {
            renderVisibleFor(players.next());
        }

    }


    void renderVisibleFor(UActor actor) {
        for (int i=-1;i<2;i++) {
            for (int j=-1;j<2;j++) {
                int dx = (actor.areaX() - leftEdge) + i;
                int dy = (actor.areaY() - topEdge) + j;
                setVisibilityAt(dx, dy, 1.0f);
            }
        }
        projectVisibility(actor.areaX() - leftEdge, actor.areaY() - topEdge);
    }


    public float visibilityAt(int col, int row) {
        if (!commander.config.isVisibilityEnable())
            return 1.0f;
        if (!isValidCell(col, row))
            return 0f;
        return lightcells[col][row].visibility();
    }
    void setVisibilityAt(int col, int row, float vis) {
        if (isValidCell(col, row)) {
            lightcells[col][row].setVisibility(vis);
            if (vis > commander.config.getVisibilityThreshold() && lightAt(col,row).grayscale() > commander.config.getVisibilityThreshold())
                area.setSeen(col + leftEdge, row + topEdge);
        }
    }

    public void addVisibilitySource(UActor actor) {
        visibilitySources.add(actor);
    }
    public void removeVisibilitySource(UActor actor) {
        visibilitySources.remove(actor);
    }


    void projectVisibility(int ox, int oy) {
        projectLight(ox, oy, null, true);
    }
    void projectLight(ULight light) {
        projectLight(light.x - leftEdge, light.y - topEdge, light, false);
    }

    void projectLight(int ox, int oy, ULight light, boolean projectVisibility) {
        projectToCell(ox, oy, light, projectVisibility, 1f);
        for (int octant=0;octant<8;octant++) {
            UShadowLine line = new UShadowLine();
            boolean fullShadow = false;
            int row = 0;
            boolean inFrame = true;
            while (inFrame) {
                row++;
                if (!isValidCell(ox + transformOctantCol(row, 0, octant),oy + transformOctantRow(row, 0, octant)))
                    inFrame = false;
                else {
                    boolean inRow = true;
                    for (int col = 0; col <= row; col++) {
                        int dy = oy + transformOctantRow(row, col, octant);
                        int dx = ox + transformOctantCol(row, col, octant);
                        if (!isValidCell(dx, dy))
                            inRow = false;
                        else {
                            if (fullShadow) {
                                // projectToCell(dx, dy, light, projectVisibility, 0f);
                            } else {
                                UShadow projection = new UShadow(0f, 0f);
                                projection.projectTile(row, col);
                                boolean visible = !line.isInShadow(projection);
                                if (visible) {
                                    projectToCell(dx, dy, light, projectVisibility, 1f);
                                    if (area.blocksLight(dx + leftEdge, dy + topEdge)) {
                                        line.add(projection);
                                        fullShadow = line.isFullShadow();
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        if (commander.config.isSmoothLightCones()) {
            for (int col = 0;col < columns;col++) {
                for (int row = 0;row < rows;row++) {
                    float v = visibilityAt(col, row);
                    if (v == 0f) {
                        int neigh = 0;
                        if (visibilityAt(col - 1, row) == 1f && !area.blocksLight(col + leftEdge - 1, row + topEdge))
                            neigh++;
                        if (visibilityAt(col + 1, row) == 1f && !area.blocksLight(col + leftEdge + 1, row + topEdge))
                            neigh++;
                        if (visibilityAt(col, row - 1) == 1f && !area.blocksLight(col + leftEdge, row + topEdge - 1))
                            neigh++;
                        if (visibilityAt(col, row + 1) == 1f && !area.blocksLight(col + leftEdge, row + topEdge + 1))
                            neigh++;
                        if (neigh > 2)
                            projectToCell(col, row, light, projectVisibility, 0.75f);
                        else if (neigh > 1)
                            projectToCell(col, row, light, projectVisibility, 0.6f);
                    }
                }
            }
        }

    }
    void projectToCell(int col, int row, ULight light, boolean projectVisibility, float intensity) {
        if (projectVisibility)
            setVisibilityAt(col, row, intensity);
        else {
            if (light.canTouch(col + leftEdge, row + topEdge)) {
                boolean blockedWallGlow = false;
                if (area.blocksLight(col+ leftEdge, row+ topEdge)) {
                    if (visibilityAt(light.x - leftEdge, light.y - topEdge) < 0.1f) {
                        blockedWallGlow = true;
                    }
                }
                if (!blockedWallGlow) {
                    receiveLight(col, row, light, intensity * light.intensityAtOffset((col + leftEdge) - light.x, (row + topEdge) - light.y));
                }
            }
        }
    }
    int transformOctantRow(int row, int col, int octant) {
        switch (octant) {
            case 0:
                return col;
            case 1:
                return row;
            case 2:
                return row;
            case 3:
                return col;
            case 4:
                return -col;
            case 5:
                return -row;
            case 6:
                return -row;
            case 7:
                return -col;
        }
        return 0;
    }
    int transformOctantCol(int row, int col, int octant) {
        switch (octant) {
            case 0:
                return -row;
            case 1:
                return -col;
            case 2:
                return col;
            case 3:
                return row;
            case 4:
                return row;
            case 5:
                return col;
            case 6:
                return -col;
            case 7:
                return -row;
        }
        return 0;
    }
    boolean isValidCell(int col, int row) {
        if (col >= 0 && row >= 0 && col < columns && row < rows)
            return true;
        return false;
    }

    void receiveLight(int col, int row, ULight source, float intensity) {
        if (isValidCell(col, row)) {
            lightcells[col][row].receiveLight(source, intensity);
        }
    }

    public UColor lightAt(int col, int row) {
        UColor total;
        if (!isValidCell(col,row))
            return UColor.COLOR_BLACK;
        if (!commander.config.isLightEnable()) {
            total = UColor.COLOR_WHITE;
        } else if (lightcells[col][row] == null) {
            System.out.println("WARNING!  nonexistent lightcell");
            total = UColor.COLOR_BLACK;
        } else {
            total = lightcells[col][row].light(commander.frameCounter);
            for (int i = -1;i < 2;i++) {
                for (int j = -1;j < 2;j++) {
                    UTerrain t = area.terrainAt(col + leftEdge + i, row + topEdge + j);
                    if (t != null)
                        if (t.isGlow())
                            total.addLights(t.getBgColor(), 0.5f);
                }
            }
        }
        return total;
    }

    public UTerrain terrainAt(int localCol, int localRow) {
        return area.terrainAt(localCol + leftEdge, localRow + topEdge);
    }

    public Iterator<UThing> thingsAt(int localCol, int localRow) {
        return area.thingsAt(localCol + leftEdge, localRow + topEdge);
    }
    public UActor actorAt(int localCol, int localRow) { return area.actorAt(localCol+ leftEdge,localRow+ topEdge); }
    public UCell cellAt(int localCol, int localRow) { return area.cellAt(localCol+ leftEdge, localRow+ topEdge); }

    @Override
    public void draw(URenderer renderer) {

        //if (modal != null)
        //    modal.renderImage();
        rendering = true;
        renderLights();

        int cellw = cellWidth();
        int cellh = cellHeight();
        int camw = getWidthInCells();
        int camh = getHeightInCells();

        // Render Cells.
        for (int col=0; col<camw; col++) {
            for (int row=0; row<camh; row++) {
                drawCell(renderer, col, row, cellw, cellh);
            }
        }
        for (int col=0; col<camw; col++) {
            for (int row=0; row<camh; row++) {
                drawCellActor(renderer, col, row, cellw, cellh);
            }
        }
        for (int col=0; col<camw; col++) {
            for (int row=0; row<camh; row++) {
                drawCellParticle(renderer, col, row, cellw, cellh);
            }
        }
        rendering = false;
    }

    private void drawCell(URenderer renderer, int col, int row, int cellw, int cellh) {
        float vis = visibilityAt(col,row);
        UColor light = lightAt(col,row);
        UTerrain t = terrainAt(col,row);
        if (t != null) {
            float tOpacity = vis;
            float tSaturation = 1f;
            if ((vis < commander.config.getVisibilityThreshold()) && area.seenCell(col + leftEdge, row + topEdge)) {
                tOpacity = commander.config.getSeenOpacity();
                tSaturation = commander.config.getSeenSaturation();
                if (commander.config.isSeenLightGray())
                    light = UColor.COLOR_GRAY;
            }
            UColor terrainLight = light;
            // TODO: clean up access to terrain obj here, wtf methodcalls
            if (t.isGlow())
                terrainLight.set(1f,1f,1f);
            // TODO: Use alpha here too?
            t.getBgColorBuffer().set(t.getBgColor().r, t.getBgColor().g, t.getBgColor().b);
            t.getBgColorBuffer().illuminateWith(terrainLight, tOpacity);
            t.getBgColorBuffer().desaturateBy(1f - tSaturation);
            renderer.drawRect(col * cellw, row * cellh, cellw, cellh, t.getBgColorBuffer());

            t.getFgColorBuffer().set(t.getFgColor().r, t.getFgColor().g, t.getFgColor().b);
            t.getFgColorBuffer().illuminateWith(terrainLight, tOpacity);
            t.getFgColorBuffer().desaturateBy(1f - tSaturation);
            renderer.drawGlyph(t.glyph(col+ leftEdge,row+ topEdge), col * cellw, row * cellh, t.getFgColorBuffer(), t.glyphOffsetX(), t.glyphOffsetY() + 2);
        } else {
            renderer.drawRect(col * cellw, row * cellh, cellw, cellh, commander.config.getCameraBgColor());
        }

        if (vis < commander.config.getVisibilityThreshold())
            return;
        Iterator<UThing> things = thingsAt(col,row);
        if (things != null) {
            while (things.hasNext()) {
                things.next().render(renderer, col * cellw, row * cellh, light, vis);
            }
        }
    }

    private void drawCellActor(URenderer renderer, int col, int row, int cellw, int cellh) {
        UActor actor = actorAt(col, row);
        if (actor == null) return;
        float vis = visibilityAt(col,row);
        if (vis < commander.config.getVisibilityThreshold()) return;

        UColor light = lightAt(col,row);
        actor.render(renderer, col*cellw, row*cellh, light, vis);
    }
    private void drawCellParticle(URenderer renderer, int col, int row, int cellw, int cellh) {
        UCell cell = cellAt(col,row);
        if (cell != null) {
            UParticle particle = cellAt(col, row).getParticle();
            if (particle != null) {
                UColor light = lightAt(col,row);
                float vis = visibilityAt(col,row);
                if (vis < commander.config.getVisibilityThreshold()) return;
                particle.render(renderer, col * cellw, row * cellh, light, vis);
            }
        }
    }

    public void animationTick() {
        float visThreshold;
        if (commander.config.isVisibilityEnable())
            visThreshold = commander.config.getVisibilityThreshold();
        else
            visThreshold = -1f;
        for (int col = leftEdge; col< rightEdge; col++) {
            for (int row = topEdge; row< bottomEdge; row++) {
                if (area.isValidXY(col,row) && lightcells[col- leftEdge][row- topEdge].visibility() > visThreshold)
                    area.cellAt(col,row).animationTick();
            }
        }
        draw(renderer);
    }

}
