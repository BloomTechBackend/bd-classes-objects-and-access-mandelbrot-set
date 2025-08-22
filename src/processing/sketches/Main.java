package processing.sketches;

import processing.core.PApplet;
import processing.event.MouseEvent;

public class Main extends PApplet {
    // Use double for all Mandelbrot calculations
    double zoom = 1.0;
    double centerX = 0.0;
    double centerY = 0.0;
    final double DEFAULT_MIN_RE = -2.0;
    final double DEFAULT_MAX_RE = 1.0;
    final double DEFAULT_MIN_IM = -1.5;
    final double DEFAULT_MAX_IM = 1.5;
    double minRe = DEFAULT_MIN_RE;
    double maxRe = DEFAULT_MAX_RE;
    double minIm = DEFAULT_MIN_IM;
    double maxIm = DEFAULT_MAX_IM;
    boolean isFullscreen = false;
    boolean isZoomingIn = false;
    boolean isZoomingOut = false;
    final double ZOOM_TARGET_X = -0.743643887037151;
    final double ZOOM_TARGET_Y = 0.131825904205330;
    final double ZOOM_MIN = 1.0;
    final double ZOOM_MAX = 1e12;
    double originalCenterX, originalCenterY, originalZoom;

    // --- Enhanced Mandelbrot Explorer State ---
    // Zoom paths: {centerX, centerY, pathType}
    final double[][] ZOOM_PATHS = {
        { -0.25, 0.0, 0 }, // Main Cardioid (Classic Mandelbrot)
        { -0.1011, 0.9563, 0 }, // Elephant Valley
        { -0.748, 0.1, 0 }, // Spiral
        { -1.25066, 0.02012, 0 }, // Minibrot
        { 0.001643721971153, 0.822467633298876, 0 }, // Triple Spiral Valley
        { -0.75, 0.0, 1 }, // Main Cardioid Spiral
        { -1.401155, 0.0, 2 } // Deep Valley (oscillate)
    };
    final String[] ZOOM_PATH_NAMES = {
        "Main Cardioid (Classic Mandelbrot)", "Elephant Valley", "Spiral", "Minibrot", "Triple Spiral", "Main Cardioid Spiral", "Deep Valley"
    };
    int currentPath = 0;
    int pathType = 0; // 0: static, 1: spiral, 2: oscillate
    double spiralAngle = 0;
    double oscillatePhase = 0;

    // Color schemes
    final String[] COLOR_SCHEME_NAMES = {"Classic", "Grayscale", "Psychedelic", "Fire", "Blue", "Green", "BW Invert"};
    int currentColorScheme = 0;
    float colorPhase = 0f;
    boolean colorAnimationEnabled = true;

    // Animation and detail
    double animationSpeed = 1.08;
    double interpSpeed = 0.08;
    double detailMultiplier = 1.0;
    boolean saveNextFrame = false;

    // For infinite spiral/oscillate paths
    double spiralCenterX = -0.75;
    double spiralCenterY = 0.0;
    double spiralAmplitude = 0.5;
    double oscillateCenterX = -1.401155;
    double oscillateCenterY = 0.0;
    double oscillateAmplitude = 0.1;
    // For infinite color options
    float colorHueFreq = 10f;
    float colorHueOffset = 0.95f;
    float colorSat = 0.7f;
    float colorBriBase = 0.7f;
    float colorBriAmp = 0.3f;
    float colorPhaseFreq = 2f;

    // Preset saving/loading
    static final int MAX_PRESETS = 10;
    double[][] savedPathPresets = new double[MAX_PRESETS][5]; // centerX, centerY, amplitude, type, zoom
    float[][] savedColorPresets = new float[MAX_PRESETS][6]; // hueFreq, hueOffset, sat, briBase, briAmp, phaseFreq
    int presetCount = 0;
    int lastLoadedPreset = -1;
    // Auto-explore
    boolean autoExplore = false;
    int autoExploreFrame = 0;
    static final int AUTO_EXPLORE_INTERVAL = 180; // frames
    boolean paused = false;
    boolean showUI = true;

    public void settings() {
        size(800, 600);
    }

    public void setup() {
        surface.setResizable(true);
        background(255);
        colorMode(HSB, 1.0f);
        Mandelbrot();
        noLoop();
        println("Controls: 's' to start zoom, 'e' to stop/reverse, 'r' to reset, 'f' to fullscreen.");
        originalZoom = 1.0;
        zoom = originalZoom;
        originalCenterX = centerX = (minRe + maxRe) / 2.0;
        originalCenterY = centerY = (minIm + maxIm) / 2.0;
    }

    public void draw() {
        background(255);
        Mandelbrot();
        if (showUI) {
            // Draw a marker at the current center
            float cx = map((float)centerX, (float)minRe, (float)maxRe, 0f, (float)width);
            float cy = map((float)centerY, (float)minIm, (float)maxIm, 0f, (float)height);
            stroke(0);
            strokeWeight(2);
            line(cx-8, cy, cx+8, cy);
            line(cx, cy-8, cx, cy+8);
            fill(0);
            noStroke();
            textSize(14);
            int detail = getDynamicDetail();
            text(String.format("Zoom: %.4e", zoom), 10, 30);
            text(String.format("Center: (%.8f, %.8f)", centerX, centerY), 10, 55);
            text(String.format("Detail: %d", detail), 10, 80);
            text(String.format("Path: %s", ZOOM_PATH_NAMES[currentPath]), 10, 110);
            text(String.format("Color: %s", COLOR_SCHEME_NAMES[currentColorScheme]), 10, 135);
            text(String.format("Speed: %.2f | Detail: %.2f", animationSpeed, detailMultiplier), 10, 160);
            text("s: start, e: reverse, r: reset, f: fullscreen", 10, 190);
            text("1-7: path, c: color, +/-: speed, []: detail, p: save", 10, 215);
            text("z: new spiral/oscillate path, v: new color", 10, 240);
            text("S: save preset, L: load preset, d: defaults, x: auto-explore", 10, 265);
            text(String.format("Auto-explore: %s", autoExplore ? "ON" : "OFF"), 10, 290);
            text(String.format("Color: freq=%.2f, off=%.2f, sat=%.2f, bri=%.2f, amp=%.2f, pf=%.2f", colorHueFreq, colorHueOffset, colorSat, colorBriBase, colorBriAmp, colorPhaseFreq), 10, 315);
            text(String.format("Path: cx=%.4f, cy=%.4f, amp=%.3f, type=%d, zoom=%.2e", centerX, centerY, (pathType==1?spiralAmplitude:oscillateAmplitude), pathType, zoom), 10, 340);
        }
        if (saveNextFrame) {
            saveFrame("mandelbrot-####.png");
            saveNextFrame = false;
        }
        if (!paused) {
            if (autoExplore) {
                autoExploreFrame++;
                if (autoExploreFrame >= AUTO_EXPLORE_INTERVAL) {
                    autoExploreFrame = 0;
                    // Randomize both color and path
                    colorHueFreq = random(5f, 30f);
                    colorHueOffset = random(0f, 1f);
                    colorSat = random(0.5f, 1f);
                    colorBriBase = random(0.5f, 0.9f);
                    colorBriAmp = random(0.1f, 0.5f);
                    colorPhaseFreq = random(1f, 6f);
                    colorPhase = 0f;
                    spiralCenterX = (float)random(-2.0f, 1.0f);
                    spiralCenterY = (float)random(-1.5f, 1.5f);
                    spiralAmplitude = (float)random(0.1f, 1.0f);
                    oscillateCenterX = (float)random(-2.0f, 1.0f);
                    oscillateCenterY = (float)random(-1.5f, 1.5f);
                    oscillateAmplitude = (float)random(0.05f, 0.5f);
                    if (pathType == 1) {
                        ZOOM_PATHS[5][0] = spiralCenterX;
                        ZOOM_PATHS[5][1] = spiralCenterY;
                    }
                    if (pathType == 2) {
                        ZOOM_PATHS[6][0] = oscillateCenterX;
                        ZOOM_PATHS[6][1] = oscillateCenterY;
                    }
                    redraw();
                }
            }
            if (colorAnimationEnabled) {
                colorPhase += 0.01f;
                if (colorPhase > 1f) colorPhase -= 1f;
            }
            // --- Restore zoom update logic ---
            if (isZoomingIn) {
                double interp = interpSpeed;
                pathType = (int)ZOOM_PATHS[currentPath][2];
                // Gently correct center toward main cardioid if on path 0
                if (currentPath == 0) {
                    centerX += (-0.25 - centerX) * 0.02;
                    centerY += (0.0 - centerY) * 0.02;
                }
                if (pathType == 1) { // Spiral in
                    spiralAngle += 0.04 * animationSpeed;
                    double r = 0.0005 / zoom + 0.5 * Math.exp(-zoom/1e6);
                    centerX = ZOOM_PATHS[currentPath][0] + r * Math.cos(spiralAngle);
                    centerY = ZOOM_PATHS[currentPath][1] + r * Math.sin(spiralAngle);
                } else if (pathType == 2) { // Oscillate
                    oscillatePhase += 0.04 * animationSpeed;
                    centerX = ZOOM_PATHS[currentPath][0] + 0.1 * Math.sin(oscillatePhase);
                    centerY = ZOOM_PATHS[currentPath][1] + 0.1 * Math.cos(oscillatePhase);
                } else if (currentPath != 0) { // Static for other paths
                    centerX += (ZOOM_PATHS[currentPath][0] - centerX) * interp;
                    centerY += (ZOOM_PATHS[currentPath][1] - centerY) * interp;
                }
                zoom *= animationSpeed;
                if (zoom >= 1e12) {
                    isZoomingIn = false;
                    noLoop();
                }
                redraw();
            } else if (isZoomingOut) {
                double interp = interpSpeed;
                centerX += (originalCenterX - centerX) * interp;
                centerY += (originalCenterY - centerY) * interp;
                zoom /= animationSpeed;
                if (zoom <= 1.0) {
                    zoom = 1.0;
                    centerX = originalCenterX;
                    centerY = originalCenterY;
                    isZoomingOut = false;
                    noLoop();
                }
                redraw();
            }
        }
    }

    public int getDynamicDetail() {
        // Increase detail much faster with zoom for deep zooms
        int detail = (int)(100 + Math.pow(zoom, 0.25) * 200 * detailMultiplier);
        return constrain(detail, 100, 10000);
    }

    public void Mandelbrot() {
        noStroke();
        double rangeRe = (maxRe - minRe) / zoom;
        double rangeIm = (maxIm - minIm) / zoom;
        double startRe = centerX - rangeRe/2.0;
        double startIm = centerY - rangeIm/2.0;
        int detail = getDynamicDetail();
        loadPixels();
        int pixelCount = width * height;
        for (int y = 0; y < height; y++) {
            double im = startIm + (y / (double)height) * rangeIm;
            for (int x = 0; x < width; x++) {
                int idx = y * width + x;
                if (idx >= pixelCount) continue;
                double re = startRe + (x / (double)width) * rangeRe;
                double zx = 0, zy = 0;
                int n = 0;
                double smooth = 0;
                boolean escaped = false;
                while (n < detail) {
                    double xtemp = zx*zx - zy*zy + re;
                    zy = 2*zx*zy + im;
                    zx = xtemp;
                    if (zx*zx + zy*zy >= 4.0) {
                        escaped = true;
                        break;
                    }
                    n++;
                }
                if (escaped) {
                    double log_zn = Math.log(zx*zx + zy*zy) / 2.0;
                    double nu = Math.log(log_zn / Math.log(2)) / Math.log(2);
                    smooth = n + 1 - nu;
                } else {
                    smooth = detail + Math.sqrt(zx*zx + zy*zy);
                }
                int c = getColor(n, smooth, detail, escaped);
                pixels[idx] = c;
            }
        }
        updatePixels();
    }

    int getColor(int n, double smooth, int detail, boolean escaped) {
        float t = (float)(smooth / detail);
        float time = colorPhase;
        // Infinite color mode: always use randomizable parameters for the default (and optionally others)
        if (currentColorScheme == 0) {
            float hue = (colorHueOffset + colorHueFreq * t + time) % 1.0f;
            float bri = escaped ? 1.0f : colorBriBase + colorBriAmp * (float)Math.sin(smooth/100f + colorPhaseFreq * time);
            return color(hue, colorSat, bri);
        }
        switch (currentColorScheme) {
            case 1: // Grayscale - dynamic oscillating gradient
                float g = 0.5f + 0.5f * (float)Math.sin(10 * smooth / detail + time * 6.28f);
                return color(g, 0, g);
            case 2: // Psychedelic - keep dynamic, add time-based cycling
                return color((float)((smooth*7 + time*5)%1.0), 1, escaped ? 1 : 0.7f + 0.3f * (float)Math.sin(smooth/100f + time*3));
            case 3: // Fire - flickering, animated flames
                float f = 0.7f + 0.3f * (float)Math.sin(15 * t + time * 12.56f);
                float fireHue = 0.05f + 0.1f * (float)Math.sin(8 * t + time * 8);
                return color(fireHue, 1, f);
            case 4: // Blue - animated blue/cyan gradient
                float b = 0.6f + 0.4f * (float)Math.sin(12 * t + time * 8);
                float blueHue = 0.55f + 0.15f * (float)Math.sin(6 * t + time * 4);
                return color(blueHue, 0.7f + 0.3f * (float)Math.cos(4 * t + time * 2), b);
            case 5: // Green - animated green/yellow gradient
                float gr = 0.6f + 0.4f * (float)Math.sin(10 * t + time * 7);
                float greenHue = 0.28f + 0.12f * (float)Math.sin(7 * t + time * 5);
                return color(greenHue, 0.8f + 0.2f * (float)Math.cos(3 * t + time * 2), gr);
            case 6: // BW Invert - high-frequency oscillation, checkerboard effect
                float bw = 0.5f + 0.5f * (float)Math.sin(20 * t + time * 10 + n * 0.1f);
                return color(0, 0, bw);
            default: // Classic - dynamic rainbow cycling
                float hue = (float)(0.95f + 10 * smooth / detail + time) % 1.0f;
                float bri = escaped ? 1.0f : 0.7f + 0.3f * (float)Math.sin(smooth/100f + time*2);
                return color(hue, 0.7f, bri);
        }
    }

    public void mouseClicked() {
        // Map mouse position to complex plane and set as new center
        double re = map((float)mouseX, 0f, (float)width, (float)minRe, (float)maxRe);
        double im = map((float)mouseY, 0f, (float)height, (float)minIm, (float)maxIm);
        centerX = re;
        centerY = im;
        redraw();
    }

    public void mouseWheel(MouseEvent event) {
        float e = event.getCount();
        float zoomFactor = 1.1f;
        if (e < 0) {
            zoom *= zoomFactor;
        } else {
            zoom /= zoomFactor;
        }
        redraw();
    }

    public void keyPressed() {
        double panStep = 0.05 * (maxRe - minRe) / zoom;
        if (key == 's' || key == 'S') {
            isZoomingIn = true;
            isZoomingOut = false;
            spiralAngle = 0;
            oscillatePhase = 0;
            loop();
        }
        if (key == 'e' || key == 'E') {
            isZoomingOut = true;
            isZoomingIn = false;
            loop();
        }
        if (key == 'r' || key == 'R') {
            zoom = originalZoom;
            centerX = originalCenterX;
            centerY = originalCenterY;
            isZoomingIn = false;
            isZoomingOut = false;
            spiralAngle = 0;
            oscillatePhase = 0;
            redraw();
        }
        if (key == 'f' || key == 'F') {
            isFullscreen = !isFullscreen;
            if (isFullscreen) {
                surface.setSize(displayWidth, displayHeight);
            } else {
                surface.setSize(800, 600);
            }
            redraw();
        }
        if (key >= '1' && key <= '7') {
            currentPath = key - '1';
            spiralAngle = 0;
            oscillatePhase = 0;
            redraw();
        }
        if (key == 'c' || key == 'C') {
            currentColorScheme = (currentColorScheme + 1) % COLOR_SCHEME_NAMES.length;
            colorPhase = 0f; // Reset phase for color consistency
            redraw();
        }
        if (key == 'a' || key == 'A') {
            colorAnimationEnabled = !colorAnimationEnabled;
            redraw();
        }
        if (key == '+' || key == '=') {
            animationSpeed = Math.min(animationSpeed + 0.02, 1.5);
            interpSpeed = Math.min(interpSpeed + 0.01, 0.2);
        }
        if (key == '-') {
            animationSpeed = Math.max(animationSpeed - 0.02, 1.01);
            interpSpeed = Math.max(interpSpeed - 0.01, 0.01);
        }
        if (key == '[') {
            detailMultiplier = Math.max(detailMultiplier - 0.1, 0.2);
        }
        if (key == ']') {
            detailMultiplier = Math.min(detailMultiplier + 0.1, 5.0);
        }
        if (key == 'p' || key == 'P') {
            saveNextFrame = true;
            redraw();
        }
        if (key == 'z' || key == 'Z') {
            // Randomize spiral/oscillate path parameters
            spiralCenterX = (float)random(-2.0f, 1.0f);
            spiralCenterY = (float)random(-1.5f, 1.5f);
            spiralAmplitude = (float)random(0.1f, 1.0f);
            oscillateCenterX = (float)random(-2.0f, 1.0f);
            oscillateCenterY = (float)random(-1.5f, 1.5f);
            oscillateAmplitude = (float)random(0.05f, 0.5f);
            // If on spiral/oscillate path, update center
            if (pathType == 1) {
                ZOOM_PATHS[5][0] = spiralCenterX;
                ZOOM_PATHS[5][1] = spiralCenterY;
            }
            if (pathType == 2) {
                ZOOM_PATHS[6][0] = oscillateCenterX;
                ZOOM_PATHS[6][1] = oscillateCenterY;
            }
            redraw();
        }
        if (key == 'v' || key == 'V') {
            // Randomize color parameters for infinite color options
            colorHueFreq = random(5f, 30f);
            colorHueOffset = random(0f, 1f);
            colorSat = random(0.5f, 1f);
            colorBriBase = random(0.5f, 0.9f);
            colorBriAmp = random(0.1f, 0.5f);
            colorPhaseFreq = random(1f, 6f);
            colorPhase = 0f;
            redraw();
        }
        if (key == 'S') {
            // Save current color and path preset
            if (presetCount < MAX_PRESETS) {
                savedPathPresets[presetCount][0] = centerX;
                savedPathPresets[presetCount][1] = centerY;
                savedPathPresets[presetCount][2] = (pathType==1?spiralAmplitude:oscillateAmplitude);
                savedPathPresets[presetCount][3] = pathType;
                savedPathPresets[presetCount][4] = zoom;
                savedColorPresets[presetCount][0] = colorHueFreq;
                savedColorPresets[presetCount][1] = colorHueOffset;
                savedColorPresets[presetCount][2] = colorSat;
                savedColorPresets[presetCount][3] = colorBriBase;
                savedColorPresets[presetCount][4] = colorBriAmp;
                savedColorPresets[presetCount][5] = colorPhaseFreq;
                presetCount++;
            }
        }
        if (key == 'L') {
            // Load last saved preset
            if (presetCount > 0) {
                int idx = presetCount-1;
                centerX = savedPathPresets[idx][0];
                centerY = savedPathPresets[idx][1];
                if (savedPathPresets[idx][3] == 1) spiralAmplitude = savedPathPresets[idx][2];
                if (savedPathPresets[idx][3] == 2) oscillateAmplitude = savedPathPresets[idx][2];
                pathType = (int)savedPathPresets[idx][3];
                zoom = savedPathPresets[idx][4];
                colorHueFreq = savedColorPresets[idx][0];
                colorHueOffset = savedColorPresets[idx][1];
                colorSat = savedColorPresets[idx][2];
                colorBriBase = savedColorPresets[idx][3];
                colorBriAmp = savedColorPresets[idx][4];
                colorPhaseFreq = savedColorPresets[idx][5];
                colorPhase = 0f;
                redraw();
            }
        }
        if (key == 'd' || key == 'D') {
            // Reset to defaults
            colorHueFreq = 10f;
            colorHueOffset = 0.95f;
            colorSat = 0.7f;
            colorBriBase = 0.7f;
            colorBriAmp = 0.3f;
            colorPhaseFreq = 2f;
            spiralCenterX = -0.75;
            spiralCenterY = 0.0;
            spiralAmplitude = 0.5;
            oscillateCenterX = -1.401155;
            oscillateCenterY = 0.0;
            oscillateAmplitude = 0.1;
            colorPhase = 0f;
            redraw();
        }
        if (key == 'x' || key == 'X') {
            autoExplore = !autoExplore;
            autoExploreFrame = 0;
            redraw();
        }
        if (key == ' ') { // Spacebar to pause/resume
            paused = !paused;
            if (!paused) loop();
            else noLoop();
        }
        if (key == 'n' || key == 'N') { // Step-by-step zoom
            if (paused) {
                if (isZoomingIn || isZoomingOut) {
                    paused = false;
                    loop();
                    delay(30); // Let one frame render
                    paused = true;
                    noLoop();
                }
            }
        }
        if (key == 'u' || key == 'U') { // Toggle UI overlay
            showUI = !showUI;
            redraw();
        }
        if (keyCode == LEFT) centerX -= panStep;
        if (keyCode == RIGHT) centerX += panStep;
        if (keyCode == UP) centerY -= panStep;
        if (keyCode == DOWN) centerY += panStep;
        redraw();
    }

    public static void main(String... args) {
        PApplet.main("processing.sketches.Main");
    }
}
