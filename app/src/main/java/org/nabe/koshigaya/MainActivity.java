package org.nabe.koshigaya;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Bundle;
import android.text.TextPaint;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.RelativeLayout;

import androidx.appcompat.app.AppCompatActivity;

class BaseView extends View {
    private final int x;
    private final int y;
    private final static String text = "Congratulation!!!";
    private final TextPaint paint;

    BaseView(Context context) {
        super(context, null);
        setBackgroundColor(MainActivity.backgroundColor);

        DisplayMetrics metrics = new DisplayMetrics();
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        wm.getDefaultDisplay().getMetrics(metrics);
        int width = metrics.widthPixels;
        int height = metrics.heightPixels;

        paint = new TextPaint();
        paint.setAntiAlias(true);
        paint.setColor(Color.WHITE);
        paint.setTextSize(24 * getResources().getDisplayMetrics().density);

        Rect bounds = new Rect();
        paint.getTextBounds(text, 0, text.length(), bounds);
        x = (width - bounds.width()) / 2;
        y = (height - bounds.height()) / 2;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawText(text, x, y, paint);
    }
}

public class MainActivity extends AppCompatActivity {

    public static int backgroundColor = Color.DKGRAY;

    @SuppressLint("SourceLockedOrientationActivity")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Not support landscape
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        // Programmatically layout the child views, forgot how to do this in xml

        // New RelativeLayout
        RelativeLayout relativeLayout = new RelativeLayout(this);

        // BaseView
        BaseView base = new BaseView(this);

        RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.MATCH_PARENT);
        lp.addRule(RelativeLayout.BELOW);

        base.setLayoutParams(lp);

        relativeLayout.addView(base);

        // Hexagon Game
        HexagonGame game = new HexagonGame(this);

        DisplayMetrics metrics = new DisplayMetrics();
        WindowManager wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        wm.getDefaultDisplay().getMetrics(metrics);

        int gameSize = metrics.widthPixels * 8 / 10;

        lp = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT);
        lp.height = gameSize;
        lp.width = gameSize;
        lp.addRule(RelativeLayout.CENTER_IN_PARENT);

        game.setLayoutParams(lp);

        relativeLayout.addView(game);

        Button startNewBtn = new Button(this);
        startNewBtn.setText("Start New");
        startNewBtn.setOnClickListener(view -> {
            game.startGame(false);
        });

        lp = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT);
        lp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        lp.addRule(RelativeLayout.ALIGN_PARENT_LEFT);

        startNewBtn.setLayoutParams(lp);
        relativeLayout.addView(startNewBtn);

        Button startNewBtnSolved = new Button(this);
        startNewBtnSolved.setText("Start New Solved");
        startNewBtnSolved.setOnClickListener(view -> {
            game.startGame(true);
        });

        lp = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT);
        lp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        lp.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);

        startNewBtnSolved.setLayoutParams(lp);
        relativeLayout.addView(startNewBtnSolved);

        Button restartBtn = new Button(this);
        restartBtn.setText("Restart");
        restartBtn.setOnClickListener(view -> {
            game.restartGame();
        });

        lp = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT);
        lp.addRule(RelativeLayout.ALIGN_PARENT_TOP);
        lp.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);

        restartBtn.setLayoutParams(lp);
        relativeLayout.addView(restartBtn);

        // Set the layout
        RelativeLayout.LayoutParams rlp = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.MATCH_PARENT);

        setContentView(relativeLayout, rlp);
    }
}
