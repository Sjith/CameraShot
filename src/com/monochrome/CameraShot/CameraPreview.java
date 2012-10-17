package com.monochrome.CameraShot;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.Date;
import java.util.List;

import jp.monochrome.Utility.DoubleImage;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.hardware.Camera;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.PreviewCallback;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback, Camera.PreviewCallback, PictureCallback, SensorEventListener {
	private SurfaceHolder holder;
	private Camera camera;
	private CameraOverlayView cov;
	private SensorManager sensorManager;

	int width = 640;
	int height = 480;
	double[] acceleration = null;

	CameraPreview(Context context, CameraOverlayView cov) {
		super(context);
		try {
			this.cov = cov;

			holder = getHolder();
			holder.addCallback(this);
			holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

			camera = Camera.open();

			// 加速度センサーのListnerを設定する。
			sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
			List<Sensor> sensors = sensorManager.getSensorList(Sensor.TYPE_ACCELEROMETER);
			if (sensors.size() > 0) {
				sensorManager.registerListener(this, sensors.get(0), SensorManager.SENSOR_DELAY_NORMAL);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		try {
			camera.setPreviewDisplay(holder);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
		try {
			camera.stopPreview();
			camera.setPreviewDisplay(holder);
			Camera.Parameters params = camera.getParameters();
			params.setPreviewSize(width, height);
			camera.setParameters(params);
			camera.setPreviewCallback((PreviewCallback) this);
			camera.startPreview();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		camera.stopPreview();
	}

	private long timeLastPhot = 0;

	@Override
	public void onPreviewFrame(byte[] data, Camera camera) {
		try {
			if (acceleration == null) return;

			long timeNow = new Date().getTime();
			if (timeNow > timeLastPhot + 100) {
				System.out.println("save photo start!!!");

				timeLastPhot = timeNow;

				DoubleImage image = new DoubleImage(width, height);
				decodeYUV420SP(image.data, data, width, height);

				// ファイルに保存
				if (true) {
					if (true) {
						int[] buffer = new int[width * height];
						for (int x = 0; x < width; x++) {
							for (int y = 0; y < height; y++) {
								int color = (int) (image.data[x][y] * 256.0);
								if (color < 0) color = 0;
								if (color > 255) color = 255;
								int index = y * width + x;
								buffer[index] = (0xff000000) | (color << 24) | (color << 16) | (color << 8) | (color);
							}
						}
						Bitmap bmp = Bitmap.createBitmap(buffer, width, height, Bitmap.Config.ARGB_8888);

						File dir = new File("/sdcard/SIFT");
						if (dir.exists() == false) {
							dir.mkdir();
						}
						FileOutputStream out = new FileOutputStream("/sdcard/SIFT/aaa_" + timeNow + ".jpg");
						bmp.compress(CompressFormat.JPEG, 100, out);
						out.flush();
						out.close();
					}

					if (true) {
						ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(new File("/sdcard/SIFT/aaa_" + timeNow + ".acc")));
						oos.writeObject(acceleration[0]);
						oos.writeObject(acceleration[1]);
						oos.writeObject(acceleration[2]);
						oos.flush();
						oos.close();
					}

					Camera.Parameters params = camera.getParameters();
					float focusLength = params.getFocalLength();
					float angleV = params.getVerticalViewAngle();
					float angleH = params.getHorizontalViewAngle();
					System.out.println("focusLength = " + focusLength);
					System.out.println("angle = " + angleV + ", " + angleH);
				}

				System.out.println("save photo end!!!");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void decodeYUV420SP(double[][] rgb, byte[] yuv420sp, int width, int height) {
		for (int j = 0, yp = 0; j < height; j++) {
			for (int i = 0; i < width; i++, yp++) {
				int y = (0xff & ((int) yuv420sp[yp])) - 16;
				if (y < 0) y = 0;
				rgb[i][j] = y / 255.0;
			}
		}
	}

	public void TakePhoto() throws Exception {
		camera.takePicture(null, null, this);
	}

	private int imageCounter = 0;

	@Override
	public void onPictureTaken(byte[] data, Camera arg1) {
		try {
			if (data != null) {
				if (true) {
					FileOutputStream fos = new FileOutputStream("/sdcard/image_" + imageCounter + ".jpg");
					fos.write(data);
					fos.flush();
					fos.close();
				}
				if (true) {
					ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(new File("/sdcard/image_" + imageCounter + ".acc")));
					oos.writeObject(acceleration[0]);
					oos.writeObject(acceleration[1]);
					oos.writeObject(acceleration[2]);
					oos.flush();
					oos.close();
				}
				imageCounter++;
				camera.startPreview();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void onAccuracyChanged(Sensor arg0, int arg1) {
	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		acceleration = new double[3];
		double x = (double) event.values[0];
		double y = (double) event.values[1];
		double z = (double) event.values[2];
		double len = Math.sqrt(x * x + y * y + z * z);
		acceleration[0] = y / len;
		acceleration[1] = -x / len;
		acceleration[2] = z / len;
		// System.out.println(tracker.acceleration_x + ", " + tracker.acceleration_y + ", " + tracker.acceleration_z);
	}
}
