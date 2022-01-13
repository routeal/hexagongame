package org.nabe.koshigaya;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HexagonGame extends View {

    private final String TAG = "HexagonGame";

    // indexes for colors
    private final int YELLOW = 0;
    private final int RED = 1;
    private final int BLUE = 2;
    private final int GRAY = 3;

    // array of colors
    private final int[] colors = {Color.YELLOW, Color.MAGENTA, Color.CYAN, Color.GRAY};

    // size of hexagon i.e. distance from center
    private int mapRadius = 0;
    // square size of each point
    private int pointSize;
    // radius of each point circle
    private int pointRadius;
    // array of hexagon points
    private final Map<OffsetCoord, Hex> hexMap;
    // points for drawing path keeping the insertion order with LinkedHashSet
    private final List<Set<Hex>> pathPoints;
    // answer points, total 6 points,  2 of start and end points for 3 color paths
    private final List<List<Hex>> answerPoints;
    // private Hex[][] answerPoints; // can be two dimension
    // size of path in pixel
    private int strokeWidth;
    // current selected hex
    private Hex prevHex = null;
    // fadeout animation
    private final Animation animFadeOut;
    // timer to run waiting effect
    private Timer waitingTimer;

    public HexagonGame(Context context) {
        this(context, null);
    }

    public HexagonGame(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);

        setBackgroundColor(MainActivity.backgroundColor);

        // currently supports fixed radius only,
        // actually works with 3 or 4 but won't be tested
        mapRadius = 2;

        // saves the user selected hex points to draw the path
        pathPoints = new ArrayList<Set<Hex>>(colors.length);
        for (int i = 0; i < colors.length; i++) {
            pathPoints.add(new LinkedHashSet<>());
        }

        // contains two of start and end points for all color paths
        answerPoints = new ArrayList<List<Hex>>(colors.length);
        for (int i = 0; i < colors.length; i++) {
            answerPoints.add(new ArrayList<Hex>());
        }

        // creates a hex map based on the current radius
        hexMap = new HashMap<OffsetCoord, Hex>();
        for (int q = -mapRadius; q <= mapRadius; q++) {
            int r1 = Math.max(-mapRadius, -q - mapRadius);
            int r2 = Math.min(mapRadius, -q + mapRadius);
            for (int r = r1; r <= r2; r++) {
                Hex h = new Hex(q, r, -q - r, GRAY);
                OffsetCoord coord = OffsetCoord.roffsetFromCube(OffsetCoord.ODD, h);
                hexMap.put(coord, h);
            }
        }

        // animation
        animFadeOut = AnimationUtils.loadAnimation(context, R.anim.fade_out);
        animFadeOut.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                startGame(false);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });
    }

    // generates a new game
    // 1. reset the all hexagon points - hexMap and pathPoints
    // 2. generate the paris of random points for three colors - answerPoints
    // 3. verify that each pair can be connected without crossing and
    //    the three paths cover all hexagon points  - pathPoints
    void generateGame() {
        do {
            // reset path
            for (int i = 0; i < colors.length; i++) {
                pathPoints.get(i).clear();
            }

            // reset start and end points
            for (int i = 0; i < colors.length; i++) {
                answerPoints.get(i).clear();
            }

            // reset all hexagon points
            for (Map.Entry<OffsetCoord, Hex> entry : hexMap.entrySet()) {
                Hex h = entry.getValue();
                h.c = GRAY;
            }

            List<Hex> hexList = new ArrayList<Hex>(hexMap.values());

            // generate start and end points for all colors
            for (int c = 0; c < GRAY; c++) {
                Hex h1;
                Hex h2;

                do {
                    do {
                        do {
                            h1 = hexList.get(getRandomNumber(0, hexList.size()));
                        } while (h1.c != GRAY);
                        do {
                            h2 = hexList.get(getRandomNumber(0, hexList.size()));
                        } while (h2.c != GRAY);
                    } while (h2.equals(h1));
                } while (h2.isNeighbor(h1)); // avoid the neighbors

                h1.c = h2.c = c;

                // save the points for checking the answers
                answerPoints.get(c).add(h1);
                answerPoints.get(c).add(h2);
            }

            // set the colors of the generated answer points to hexMap
            for (int c = 0; c < GRAY; c++) {
                List<Hex> ans = answerPoints.get(c);
                for (Hex hex : ans) {
                    OffsetCoord coord = OffsetCoord.roffsetFromCube(OffsetCoord.ODD, hex);
                    Hex h = hexMap.get(coord);
                    if (h != null) {
                        h.c = c;
                    }
                }
            }

        } while (!IsCurrentAnswerPointsVerified()); // verified the generated answer points
    }

    // generateGame runs in a different thread since it takes time
    private class generateGameAsync implements Runnable {
        private final boolean show;

        generateGameAsync(boolean show) {
            this.show = show;
        }

        @Override
        public void run() {
            generateGame();

            if (!show) {
                resetGame();
            }

            waitingTimer.cancel();
            waitingTimer = null;

            postInvalidate();
        }
    }

    void startGame(boolean show) {
        TimerTask task = new TimerTask() {
            public void run() {
                postInvalidate();
            }
        };
        waitingTimer = new Timer();
        waitingTimer.schedule(task, 100, 250);

        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(new generateGameAsync(show));
    }

    void restartGame() {
        resetGame();
        invalidate();
    }

    void resetGame() {
        for (int c = 0; c < GRAY; c++) {
            clearPath(c, 0);
        }
    }

    @SuppressLint("DrawAllocation")
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // animation set alpha to 0
        setAlpha(1.0f);

        if (mapRadius < 2) return;

        if (waitingTimer != null) {
            for (Map.Entry<OffsetCoord, Hex> entry : hexMap.entrySet()) {
                OffsetCoord coord = entry.getKey();
                Hex h = entry.getValue();
                Point point = convCoordToPoint(coord, mapRadius, pointSize);
                Paint paint = new Paint();
                paint.setColor(colors[getRandomNumber(0, colors.length)]);
                paint.setStyle(Paint.Style.FILL);
                canvas.drawCircle(point.x, point.y, pointRadius, paint);
            }
            return;
        }

        // draws hex points
        for (Map.Entry<OffsetCoord, Hex> entry : hexMap.entrySet()) {
            OffsetCoord coord = entry.getKey();
            Hex h = entry.getValue();
            Point point = convCoordToPoint(coord, mapRadius, pointSize);
            Paint paint = new Paint();
            paint.setColor(colors[GRAY]);
            paint.setStyle(Paint.Style.FILL);
            canvas.drawCircle(point.x, point.y, pointRadius, paint);
        }

        // draws both start and end points on top of gray points
        for (int c = 0; c < GRAY; c++) {
            List<Hex> ans = answerPoints.get(c);
            for (Hex hex : ans) {
                OffsetCoord coord = OffsetCoord.roffsetFromCube(OffsetCoord.ODD, hex);
                Point point = convCoordToPoint(coord, mapRadius, pointSize);
                Paint paint = new Paint();
                paint.setColor(colors[c]);
                paint.setStyle(Paint.Style.FILL);
                canvas.drawCircle(point.x, point.y, pointRadius, paint);
            }
        }

        // draws paths
        for (int c = 0; c < GRAY; c++) {
            Path path = new Path();
            boolean first = true;
            for (Hex hex : pathPoints.get(c)) {
                OffsetCoord coord = OffsetCoord.roffsetFromCube(OffsetCoord.ODD, hex);
                Point point = convCoordToPoint(coord, mapRadius, pointSize);
                if (first) {
                    first = false;
                    path.moveTo(point.x, point.y);
                } else {
                    path.lineTo(point.x, point.y);
                }
            }
            Paint blueLinePaint = new Paint();
            blueLinePaint.setColor(colors[c]);
            blueLinePaint.setStyle(Paint.Style.STROKE);
            blueLinePaint.setStrokeJoin(Paint.Join.ROUND);
            blueLinePaint.setStrokeCap(Paint.Cap.ROUND);
            blueLinePaint.setStrokeWidth(strokeWidth);
            canvas.drawPath(path, blueLinePaint);
        }
    }

    void initHexSize(int canvasSize) {
        int hexSize = mapRadius * 2 + 1;

        pointSize = canvasSize / hexSize;

        pointRadius = (pointSize / 2) * 4 / 5;

        strokeWidth = pointRadius / 2;
    }

    // seems called twice so try to prevent 2nd one
    private boolean hasOnLayoutCalled = false;

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);

        if (hasOnLayoutCalled) return;

        hasOnLayoutCalled = true;

        // should be square
        initHexSize(right - left);

        // start here as soon as the layout is fixed
        startGame(false);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                startTouch(x, y);
                invalidate();
                break;
            case MotionEvent.ACTION_MOVE:
                moveTouch(x, y);
                invalidate();
                break;
            case MotionEvent.ACTION_UP:
                endTouch(x, y);
                invalidate();
                break;
        }

        return true;
    }

    void startTouch(float x, float y) {
        // get the mapped hex from the point
        Hex selectedHex = getSelectedHex(x, y);

        // return if none is mapped
        if (selectedHex == null) return;

        // reset the path if the starting point is selected again or if the end point is selected
        for (int c = 0; c < GRAY; c++) {
            List<Hex> ans = answerPoints.get(c);
            if (ans.get(0) == selectedHex || ans.get(1) == selectedHex) {
                Set<Hex> path = pathPoints.get(c);
                if (path.size() > 0) {
                    clearPath(c, 0);
                }
            }
        }

        // remember the selected point as prevHex and put it into the point array for drawing path
        for (int c = 0; c < GRAY; c++) {
            // get hex in path and remove it thereafter
            Hex hexInPath = getMidInPath(c, selectedHex);

            if (selectedHex.c == c || selectedHex.equals(hexInPath)) {
                printHex(selectedHex, "first selected:");
                selectedHex.c = c;
                prevHex = selectedHex;
                pathPoints.get(c).add(selectedHex);
                return;
            }
        }

        // do nothing since gray is selected as starting
        printHex(selectedHex, "none:");
        prevHex = null;
    }

    void moveTouch(float x, float y) {
        // not the first one selected
        // FIXME: not allowing to select the first one while moving
        if (prevHex == null) return;

        // get the mapped hex from the point
        Hex selectedHex = getSelectedHex(x, y);

        // return if none is mapped
        if (selectedHex == null) return;

        // return if the same is selected
        if (selectedHex.equals(prevHex)) return;

        //  only neighbor will be taken
        if (!prevHex.isNeighbor(selectedHex)) return;

        // if new gray point is selected
        if (selectedHex.c == GRAY) {
            for (int c = 0; c < GRAY; c++) {
                if (prevHex.c == c) {
                    if (isPathConnected(c)) return;
                    printHex(selectedHex, "added:");
                    selectedHex.c = c;
                    pathPoints.get(c).add(selectedHex);
                    prevHex = selectedHex;
                    break;
                }
            }
            return;
        }

        // must be heading to the end point
        if (prevHex.c != selectedHex.c) {
            return;
        }

        printHex(selectedHex, "added the end point:");
        pathPoints.get(prevHex.c).add(selectedHex);
    }

    void endTouch(float x, float y) {
        if (isGameEnd()) {
            Log.d(TAG, "Game ended");
            startAnimation(animFadeOut);
        }
    }

    // gets a mapped hex from x-y location
    Hex getSelectedHex(float x, float y) {
        // get offsetcord from the pixel points
        OffsetCoord coord = convPointToCoord(mapRadius, pointSize, (int) x, (int) y);
        return hexMap.get(coord);
    }

    // gets a hex in the middle of path from the hex position
    // and removes the path after the hex
    Hex getMidInPath(int c, Hex hex) {
        Set<Hex> path = pathPoints.get(c);
        if (path.size() == 0) return null;
        int i = 0;
        for (Hex h : path) {
            if (h.equals(hex)) {
                break;
            }
            i++;
        }
        // not in the path
        if (i == path.size()) return null;
        // last one
        if ((i + 1) == path.size()) return hex;
        // clear path after i
        clearPath(c, i);
        return hex;
    }

    // removes the path starting from the specified position
    void clearPath(int c, int start) {
        Set<Hex> path = pathPoints.get(c);
        // set GRAY i.e. mark deselected to hexes in the path
        // starting from start to end
        int i = 0;
        for (Iterator<Hex> iter = path.iterator(); iter.hasNext(); ) {
            Hex h = iter.next();
            if (i >= start) {
                h.c = GRAY;
                iter.remove();
            }
            i++;
        }
        // ensure the end points not to be erased
        List<Hex> ans = answerPoints.get(c);
        ans.get(0).c = c;
        ans.get(1).c = c;
    }

    // checks two of both start and end points are in the path
    boolean isPathConnected(int c) {
        Set<Hex> path = pathPoints.get(c);
        if (path.size() < 2) return false;
        Hex h1 = path.iterator().next();
        Hex h2 = null;
        for (Iterator<Hex> iter = path.iterator(); iter.hasNext(); ) {
            h2 = iter.next();
        }
        //Hex h2 = path.get(path.size() - 1);
        List<Hex> ans = answerPoints.get(c);
        return (ans.get(0) == h1 && ans.get(1) == h2) || (ans.get(0) == h2 && ans.get(1) == h1);
    }

    boolean isGameEnd() {
        for (int c = 0; c < GRAY; c++) {
            if (!isPathConnected(c)) {
                return false;
            }
        }
        int total = 0;
        for (int c = 0; c < GRAY; c++) {
            Set<Hex> path = pathPoints.get(c);
            total += path.size();
        }
        //Log.d(TAG,  "Path total=" + total + " hexMap size=" + hexMap.size());
        if (!(hexMap.size() == total)) {
            return false;
        }
        return true;
    }

    int getRandomNumber(int min, int max) {
        return (int) ((Math.random() * (max - min)) + min);
    }

    void printHex(Hex h, String msg) {
        String[] cstr = {"yellow", "red", "blue", "gray"};
        OffsetCoord coord = OffsetCoord.roffsetFromCube(OffsetCoord.ODD, h);
        Log.d(TAG, msg + " row=" + coord.row + " col=" + coord.col + " " + cstr[h.c]);
    }

    public Point convCoordToPoint(OffsetCoord coord, int mapRadius, int pointSize) {
        int offset = pointSize / 2;
        int x = offset + (coord.col + mapRadius) * pointSize + (coord.row & 1) * pointSize / 2;
        int y = offset + (coord.row + mapRadius) * pointSize;
        return new Point(x, y);
    }

    public OffsetCoord convPointToCoord(int mapRadius, int pointSize, int x, int y) {
        int offset = pointSize / 2;
        int row = y / pointSize;
        int col = (x - (row & 1) * offset) / pointSize;
        return new OffsetCoord(col - mapRadius, row - mapRadius);
    }

    // returns true if all the combination of the current start and end points
    // can be connected without crossing each other and cover all hexagon points
    //
    // findpath() finds a path connecting start and end. runs findpath() for each color
    // and verifies that the three paths run independently and cover all points
    boolean IsCurrentAnswerPointsVerified() {
        List<Boolean> reverse = new ArrayList<>();
        reverse.add(true);
        reverse.add(false);
        for (Boolean b : reverse) {
            for (int i = 0; i < GRAY; i++) {
                for (int j = 0; j < GRAY; j++) {
                    if (j == i) continue;
                    for (int k = 0; k < GRAY; k++) {
                        if (k == i || k == j) continue;
                        Hex start = answerPoints.get(i).get(b ? 1 : 0);
                        Hex end = answerPoints.get(i).get(b ? 0 : 1);
                        if (findPath(i, start, end)) {
                            start = answerPoints.get(j).get(b ? 1 : 0);
                            end = answerPoints.get(j).get(b ? 0 : 1);
                            if (findPath(j, start, end)) {
                                start = answerPoints.get(k).get(b ? 1 : 0);
                                end = answerPoints.get(k).get(b ? 0 : 1);
                                if (findPath(k, start, end)) {
                                    if (isGameEnd()) return true;
                                    resetGame();
                                }
                            }
                        }
                    }
                }
            }
        }
        return false;
    }

    // logic for path finding is referred from
    // https://www.redblobgames.com/pathfinding/a-star/introduction.html
    // simple breath first search is implemented here
    boolean findPath(int currentColor, Hex start, Hex end) {

        Queue<Hex> frontier = new ArrayDeque<>();
        frontier.add(start);

        Map<OffsetCoord, Hex> came_from = new HashMap<>();
        came_from.put(OffsetCoord.roffsetFromCube(OffsetCoord.ODD, start), null);

        boolean hasAnswer = false;

        while (!frontier.isEmpty()) {
            Hex current = frontier.poll();

            if (current == null) continue;

            if (current.equals(end)) {
                hasAnswer = true;
                break;
            }

            // searching all neighbors
            for (int direction = 0; direction < Hex.directions.size(); direction++) {
                // note that this next is not mapped
                Hex next = current.neighbor(direction);
                OffsetCoord coord = OffsetCoord.roffsetFromCube(OffsetCoord.ODD, next);
                // check this within the current radius
                if (Math.abs(coord.row) > mapRadius || Math.abs(coord.col) > mapRadius) continue;
                // get the mapped hex
                next = hexMap.get(coord);
                // not in the current hexagon map
                if (next == null) continue;
                // only gray (not selected) or own color
                if (!(next.c == GRAY || next.c == currentColor)) continue;

                if (came_from.get(coord) == null) {
                    frontier.add(next);
                    came_from.put(coord, current);
                }
            }
        }

        if (!hasAnswer) {
            return false;
        }

        Hex current = end;

        Set<Hex> path = pathPoints.get(currentColor);

        while (!current.equals(start)) {
            current.c = colors[currentColor];
            path.add(current);
            OffsetCoord coord = OffsetCoord.roffsetFromCube(OffsetCoord.ODD, current);
            current = came_from.get(coord);
            if (current == null) {
                // should be the start point
                break;
            }
        }
        path.add(start);

        return true;
    }

    boolean findPath2(int currentColor, Hex start, Hex end) {
        // findpath() can be improved, but tired
        return false;
    }

}
