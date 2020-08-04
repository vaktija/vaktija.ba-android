package ba.vaktija.android.widgets;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.LinearLayout;

public class ClickConsumingLinearLayout extends LinearLayout {

    public ClickConsumingLinearLayout(Context context) {
        super(context);
    }

    public ClickConsumingLinearLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @SuppressLint("NewApi")
    public ClickConsumingLinearLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return !isEnabled();
    }
}
