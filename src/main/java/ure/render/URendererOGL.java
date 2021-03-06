package ure.render;

import org.joml.Matrix4f;
import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.*;
import org.lwjgl.opengl.*;
import ure.math.UColor;
import ure.sys.GLKey;
import ure.sys.Injector;
import ure.sys.UCommander;
import ure.ui.View;

import javax.inject.Inject;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryUtil.*;

public class URendererOGL implements URenderer {

    @Inject
    UCommander commander;

    private View rootView;
    private View context;

    //To store matrix data for uploading into OpenGLVille
    private FloatBuffer fb = BufferUtils.createFloatBuffer(16);
    private Matrix4f matrix = new Matrix4f();
    private Matrix4f dummyMatrix = new Matrix4f();

    private long window;

    private int screenWidth;
    private int screenHeight;

    private GLFWErrorCallback errorCallback;

    private int tris = 0;
    private int maxTris = 65536; // THIS CAN BE HIGHER IF NEEDED

    private float[] verts_pos = new float[3 * maxTris * 3];
    private float[] verts_col = new float[3 * maxTris * 4];
    private float[] verts_uv  = new float[3 * maxTris * 3];

    private int textureAtlas = -1;

    private KeyListener keyListener;

    private int frameCount = 0;
    private long lastUpdateTime = System.currentTimeMillis();

    private boolean[] keyState = new boolean[65536]; // Apparently in Java these are 16bit.

    public URendererOGL() {
        Injector.getAppComponent().inject(this);
    }

    // URenderer methods

    @Override
    public int getMousePosX(){
        DoubleBuffer xf = BufferUtils.createDoubleBuffer(1);
        DoubleBuffer yf = BufferUtils.createDoubleBuffer(1);
        glfwGetCursorPos(window, xf, yf);
        return (int)xf.get(0);
    }
    @Override
    public int getMousePosY(){
        DoubleBuffer xf = BufferUtils.createDoubleBuffer(1);
        DoubleBuffer yf = BufferUtils.createDoubleBuffer(1);
        glfwGetCursorPos(window, xf, yf);
        return (int)yf.get(0);
    }

    @Override
    public View getRootView() { return rootView; }
    @Override
    public void setRootView(View root) {
        rootView = root;
    }

    @Override
    public boolean windowShouldClose() {
        return glfwWindowShouldClose(window);
    }

    @Override
    public void pollEvents() {
        glfwPollEvents();
    }

    @Override
    public void setKeyListener(KeyListener listener) {
        this.keyListener = listener;
    }

    @Override
    public void initialize() {

        if (!glfwInit())
            throw new IllegalStateException("Unable to initialize GLFW");

        glfwSetErrorCallback(errorCallback = GLFWErrorCallback.createPrint(System.err));

        // Configure our window
        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 1);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 1);
        glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, 0);
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);

        screenWidth = commander.config.getScreenWidth();
        screenHeight = commander.config.getScreenHeight();

        window = glfwCreateWindow(screenWidth, screenHeight, "UREasonable example!", NULL, NULL);
        if ( window == NULL )
            throw new RuntimeException("Failed to create the GLFW window");

        glfwSetKeyCallback(window, new GLFWKeyCallback() {
            @Override
            public void invoke(long window, int key,
                               int scancode, int action, int mods) {
                if(key < 0 || key >= keyState.length) return; // Bail, oob key press.
                keyState[key] = action != GLFW_RELEASE;

                if (keyListener != null && (action == GLFW_PRESS || action == GLFW_REPEAT)) {
                    GLKey glkey = new GLKey(key, keyState[GLFW_KEY_LEFT_SHIFT] || keyState[GLFW_KEY_RIGHT_SHIFT],
                                                    keyState[GLFW_KEY_LEFT_CONTROL] || keyState[GLFW_KEY_RIGHT_CONTROL]);
                    keyListener.keyPressed(glkey);
                }
            }
        });


        glfwSetMouseButtonCallback(window, (new GLFWMouseButtonCallback() {

            @Override
            public void invoke(long window, int button, int action, int mods) {
                // button 0 - 7
                // I assume action is the same as GLFW_RELEASE like above.
            }

        }));
        glfwSetFramebufferSizeCallback(window,
                new GLFWFramebufferSizeCallback() {
                    @Override
                    public void invoke(long window, int w, int h) {
                        if (w > 0 && h > 0) {
                            resize(w, h);
                        }
                    }
                });

        GLFWVidMode vidmode = glfwGetVideoMode(glfwGetPrimaryMonitor());
        glfwSetWindowPos(window, (vidmode.width() - screenWidth) / 2, (vidmode.height() - screenHeight) / 2);

        glfwMakeContextCurrent(window);
        glfwSwapInterval(0);
        glfwShowWindow(window);

        // Get the actual framebuffer width/height in order to deal with different pixel densities
        int[] width = new int[1];
        int[] height = new int[1];
        glfwGetFramebufferSize(window, width, height);
        resize(width[0], height[0]);

        glfwWindowHint(GLFW_OPENGL_DEBUG_CONTEXT, GLFW_TRUE);
        GL.createCapabilities();
        GLUtil.setupDebugMessageCallback();

        //Lets quickly blank the screen.
        glViewport(0, 0, screenWidth, screenHeight);
        UColor windowBgColor = commander.config.getWindowBgColor();
        glClearColor(windowBgColor.fR(), windowBgColor.fG(), windowBgColor.fB(), 1.0f);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        glEnable(GL_TEXTURE_2D);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        //The 2d stuff we're doing doesn't need depth, or culling of faces.
        glDisable(GL_DEPTH_TEST);
        glDisable(GL_CULL_FACE);

        //Flush a blank image to the screen quickly.
        glfwSwapBuffers(window);

        FontTexture fontTexture = new FontTexture();
        textureAtlas = fontTexture.loadTexture("/font.png");
    }

    @Override
    public void render() {
        if (rootView != null) {
            render(rootView);
        }
        paintScreen();
    }

    public void render(View view) {
        context = view;
        view.draw(this);
        for (View child : view.children()) {
            render(child);
        }
    }

    @Override
    public void drawString(int x, int y, UColor col, String str){
        if (str == null) return;
        //TODO: HANDLE FONT CHANGES
        x += context.absoluteX();
        y += context.absoluteY();
        for(int i = 0; i < str.length(); i++){
            // TODO: pass in the size
            addQuad(x, y, glyphWidth(), glyphHeight(), col, str.charAt(i));
            x += 8;
        }
    }

    @Override
    public void drawGlyph(char glyph, int destx, int desty, UColor tint, int offX, int offY) {
        // Seems odd that we are taking a position and an offset here... maybe this could be simplified?
        destx += context.absoluteX();
        desty += context.absoluteY();
        addQuad(destx + offX, desty + offY, glyphWidth(), glyphHeight(), tint, glyph);
    }

    @Override
    public void drawGlyphOutline(char glyph, int destx, int desty, UColor tint, int offX, int offY) {
        destx += context.absoluteX();
        desty += context.absoluteY();
        for(int y = -1; y < 2; y += 1)
            for(int x = -1; x < 2; x += 1)
                if(x != 0 && y != 0)
                    addQuad(destx + offX + x, desty + offY + y, glyphWidth(), glyphHeight(), tint, glyph);
    }

    @Override
    public void drawRect(int x, int y, int w, int h, UColor col){
        x += context.absoluteX();
        y += context.absoluteY();
        addQuad(x, y, w, h, col);
    }

    @Override
    public void drawRectBorder(int x, int y, int w, int h, int borderThickness, UColor bgColor, UColor borderColor){
        x += context.absoluteX();
        y += context.absoluteY();
        addQuad(x, y, w, h, borderColor);
        addQuad(x + borderThickness, y + borderThickness, w - borderThickness * 2, h - borderThickness * 2, bgColor);
    }

    @Override
    public int glyphWidth() {
        return commander.config.getGlyphWidth();
    }

    @Override
    public int glyphHeight() {
        return commander.config.getGlyphHeight();
    }

    // internals

    public void paintScreen() {

        glViewport(0, 0, screenWidth, screenHeight);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        if (tris == 0) return; //Nothing to draw

        glLoadIdentity();

        glMatrixMode(GL_PROJECTION);
        glLoadMatrixf(matrix.get(fb));

        glMatrixMode(GL_MODELVIEW);
        glLoadMatrixf(dummyMatrix.get(fb));

        glBindTexture(GL_TEXTURE_2D, textureAtlas);

        FloatBuffer v = BufferUtils.createFloatBuffer(tris * 3 * 3);
        FloatBuffer c = BufferUtils.createFloatBuffer(tris * 3 * 4);
        FloatBuffer u = BufferUtils.createFloatBuffer(tris * 3 * 2);

        v.put(verts_pos, 0, v.capacity());
        c.put(verts_col, 0, c.capacity());
        u.put(verts_uv, 0, u.capacity());

        v.flip();
        c.flip();
        u.flip();

        GL11.glEnableClientState(GL11.GL_VERTEX_ARRAY);
        GL11.glEnableClientState(GL11.GL_COLOR_ARRAY);
        GL11.glEnableClientState(GL11.GL_TEXTURE_COORD_ARRAY);

        GL11.glVertexPointer(3, GL_FLOAT, 0, v);
        GL11.glColorPointer(4, GL_FLOAT, 0, c);
        GL11.glTexCoordPointer(2, GL_FLOAT, 0, u);


        GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, tris * 3);
        glFinish();

        glfwSwapBuffers(window);

        tris = 0;

        frameCount++;
        long now = System.currentTimeMillis();
        if (now - lastUpdateTime > 1000) {
            System.out.println("[ " + frameCount + " fps ]");
            frameCount = 0;
            lastUpdateTime = now;
        }
    }

    private void resize(int width, int height){
        screenWidth = width;
        screenHeight = height;
        matrix.setOrtho2D(0, 1400, 1000, 0);
    }

    private void destroy(){
        glfwDestroyWindow(window);
        glfwTerminate();
        errorCallback.free();
    }


    private void addQuad(int x, int y, int w, int h, UColor col){
        addQuad(x, y, w, h, col, 0.0f, 0.0f, 32.f / 1024.f, 32.f / 1024.f);
    }

    private void addQuad(int x, int y, int w, int h, UColor col, char glyph){
        float u = (float)(glyph % 16) / 32.0f + 0.00390625f;
        float v = (float)(glyph / 16) / 32.0f + 0.0078125f;
        addQuad(x, y, w, h, col, u, v, glyphWidth() / 1024.f, glyphHeight() / 1024.f);
    }

    // I don't think we'll have any triangle data for awhile/ever, so I'm just gonna do quads.
    private void addQuad(int x, int y, int w, int h, UColor col, float u, float v, float uw, float vh){
        //   1---2
        //   | / |
        //   3---4
        if(tris + 2 >= maxTris) return; // Should probably dump a message here, we're all full up!

        // Current indices for our data;
        int ip = 3 * 3 * tris;
        int ic = 4 * 3 * tris;
        int iu = 2 * 3 * tris;


        //pos, tri0
        //v1
        verts_pos[ip++] = x;
        verts_pos[ip++] = y;
        verts_pos[ip++] = 0;
        //v2
        verts_pos[ip++] = x + w;
        verts_pos[ip++] = y;
        verts_pos[ip++] = 0;
        //v3
        verts_pos[ip++] = x;
        verts_pos[ip++] = y + h;
        verts_pos[ip++] = 0;
        //pos, tri1
        //v2
        verts_pos[ip++] = x + w;
        verts_pos[ip++] = y;
        verts_pos[ip++] = 0;
        //v4
        verts_pos[ip++] = x + w;
        verts_pos[ip++] = y + h;
        verts_pos[ip++] = 0;
        //v3
        verts_pos[ip++] = x;
        verts_pos[ip++] = y + h;
        verts_pos[ip++] = 0;

        //cols;
        int i;
        for(i = 0; i < 3 * 2; i++){
            verts_col[ic++] = col.r;
            verts_col[ic++] = col.g;
            verts_col[ic++] = col.b;
            verts_col[ic++] = col.a;
        }

        //UVs, tri 0
        //v1
        verts_uv[iu++] = u;
        verts_uv[iu++] = v;
        //v2
        verts_uv[iu++] = u + uw;
        verts_uv[iu++] = v;
        //v3
        verts_uv[iu++] = u;
        verts_uv[iu++] = v + vh;
        //UVs, tri 1
        //v2
        verts_uv[iu++] = u + uw;
        verts_uv[iu++] = v;
        //v4
        verts_uv[iu++] = u + uw;
        verts_uv[iu++] = v + vh;
        //v3
        verts_uv[iu++] = u;
        verts_uv[iu++] = v + vh;


        tris += 2;
    }
}
