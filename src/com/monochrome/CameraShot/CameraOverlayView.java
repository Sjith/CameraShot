package com.monochrome.CameraShot;

import android.content.Context;
import android.graphics.Canvas;
import android.view.View;

public class CameraOverlayView extends View {

	public CameraOverlayView(Context context) {
		super(context);
	}

	@Override
	protected void onDraw(Canvas canvas) {
		System.out.println("### OnDraw #######################");
		super.onDraw(canvas);
	}

}
