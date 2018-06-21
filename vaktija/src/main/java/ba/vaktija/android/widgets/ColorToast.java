package ba.vaktija.android.widgets;

import ba.vaktija.android.R;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.GradientDrawable.Orientation;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class ColorToast {
	
	Context context;
	String message;
	int color;
	
	Toast toast;
	
	@SuppressWarnings("deprecation")
	public ColorToast(Context context, String message, int color){
		this.color = color;
		this.message = message;
		this.context = context;
		
		View layout = LayoutInflater.from(context).inflate(R.layout.color_toast, null);

		LinearLayout root = (LinearLayout) layout.findViewById(R.id.toast_layout_root);
		root.setBackgroundColor(color);
		
		
		TextView text = (TextView) layout.findViewById(R.id.toast_text);
		text.setText(message);
		
		GradientDrawable colorDrawable = new GradientDrawable(Orientation.BOTTOM_TOP, new int[]{color, color});
		colorDrawable.setShape(GradientDrawable.RECTANGLE);
		colorDrawable.setCornerRadius(7f);
		
		layout.setBackgroundDrawable(colorDrawable);
		
		toast = new Toast(context.getApplicationContext());
		toast.setGravity(Gravity.TOP, 0, 200);
		toast.setDuration(Toast.LENGTH_SHORT);
		toast.setView(layout);
	}
	
	public void show(){
		toast.show();
	}
	
	public void cancel(){
		toast.cancel();
	}
}
