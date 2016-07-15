package com.ckt.eirot.openmouth;

import android.content.ContentResolver;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;
import com.google.android.gms.vision.face.Landmark;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private final String TAG = "OpenMouth";
    Button btnChoose;
    ImageView imgeFace;
    Bitmap mBitmap;
    TextView tvNose, tvBottomMouth, tvLeftClip, tvRightClip, tvOpenMouthRange;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Load the Image
        imgeFace = (ImageView) findViewById(R.id.img_face);

        tvNose = (TextView) findViewById(R.id.tv_nose);
        tvBottomMouth = (TextView) findViewById(R.id.tv_bottom_mouth);
        tvLeftClip = (TextView) findViewById(R.id.tv_left_clip);
        tvRightClip = (TextView) findViewById(R.id.tv_right_clip);
        tvOpenMouthRange = (TextView) findViewById(R.id.open_mouth_range);

        btnChoose = (Button) this.findViewById(R.id.button_choose);
        btnChoose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Read picture from gallery
                Intent intent = new Intent();
                intent.setAction(Intent.ACTION_GET_CONTENT);
                intent.setType("image/*");
                startActivityForResult(intent, 1);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(TAG, "onActivityResult: resultCode is :" + resultCode);
        if (requestCode == 1) {
            if (resultCode == RESULT_OK) {
                if (data != null) {
                    Uri uri = data.getData();
                    ContentResolver cr = this.getContentResolver();
                    try {
                        // 实例化一个Options对象
                        BitmapFactory.Options options = new BitmapFactory.Options();
                        options.inMutable=true;
                        // 从ContentResolver中获取到Uri的输入流
                        InputStream is = getContentResolver().openInputStream(uri);
                        // 通过这个Options对象,从输入流中读取图片的信息
                        mBitmap = BitmapFactory.decodeStream(is, null, options);
                        imgeFace.setImageBitmap(mBitmap);
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onResume() {
        detectFace(mBitmap);
        super.onResume();
    }

    /*
     * when the user presses the button load the image, process it for faces,
     * and draw a red rectangle over any faces it finds
     */
    private void detectFace(Bitmap mBitmap) {
        if (mBitmap == null) {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inMutable=true;
            mBitmap = BitmapFactory.decodeResource(getApplicationContext().getResources(),
                    R.drawable.default_image, options);
            Toast.makeText(this, "Read default image now first", Toast.LENGTH_LONG).show();
        }
        // Create a Paint object for drawing with
        Paint mRectPaint = new Paint();
        mRectPaint.setStrokeWidth(5);
        mRectPaint.setColor(Color.RED);
        mRectPaint.setStyle(Paint.Style.STROKE);

        Paint textPaint = new Paint( Paint.ANTI_ALIAS_FLAG);
        textPaint.setTextSize(40);
        textPaint.setColor( Color.MAGENTA);

        // Create a Canvas object for drawing on
        Bitmap tempBitmap = Bitmap.createBitmap(mBitmap.getWidth(), mBitmap.getHeight(), Bitmap.Config.RGB_565);
        Canvas tempCanvas = new Canvas(tempBitmap);
        tempCanvas.drawBitmap(mBitmap, 0, 0, null);

        //Create the Face Detector
        /*
         * Note: As this sample is simply detecting a face on a still frame, no tracking is necessary.
         * If you are detecting faces in video, or on a live preview from the camera, you should set
         * trackingEnabled on the faceDetector to ‘true’.
         */
        FaceDetector faceDetector = new
                FaceDetector.Builder(getApplicationContext())
                .setTrackingEnabled(false)
                .setLandmarkType(FaceDetector.ALL_LANDMARKS)
                .build();

         /*
          * check if our detector is operational before we use it.  If it isn’t, we may have to
          * wait for a download to complete, or let our users know that they need to find an
          * internet connection or clear some space on their device.
          */
        if(!faceDetector.isOperational()){
            new AlertDialog.Builder(this.getApplicationContext()).setMessage("Could not set up the face detector!").show();
            return;
        }

        // Detect the Faces
        Frame frame = new Frame.Builder().setBitmap(mBitmap).build();
        SparseArray<Face> faces = faceDetector.detect(frame);

        /**
         * Draws a small circle for each detected landmark, centered at the detected landmark position.
         *
         * Note that eye landmarks are defined to be the midpoint between the detected eye corner
         * positions, which tends to place the eye landmarks at the lower eyelid rather than at the
         * pupil position.
         */

        double viewWidth = tempCanvas.getWidth();
        double viewHeight = tempCanvas.getHeight();
        double imageWidth = mBitmap.getWidth();
        double imageHeight = mBitmap.getHeight();
        double scale = Math.min(viewWidth / imageWidth, viewHeight / imageHeight);
        Log.d(TAG, "--->> scale is : " + scale);

        Paint paint = new Paint();
        paint.setColor(Color.GREEN);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(5);

        for(int i=0; i<faces.size(); i++) {
            Face thisFace = faces.valueAt(i);
            float x1 = thisFace.getPosition().x;
            float y1 = thisFace.getPosition().y;
            float x2 = x1 + thisFace.getWidth();
            float y2 = y1 + thisFace.getHeight();
            float smile = thisFace.getIsSmilingProbability();

            List<Landmark> faceLandMark = thisFace.getLandmarks();
            /*
             * faceLandMark[0] 左眼坐标
             * faceLandMark[1] 右眼坐标
             * faceLandMark[2] 鼻子坐标
             * faceLandMark[3] 右脸颊坐标
             * faceLandMark[4] 左脸颊坐标
             * faceLandMark[5] 右嘴唇坐标
             * faceLandMark[6] 左嘴唇坐标
             * faceLandMark[7] 下嘴唇坐标
             */

            double landmark2_x = (int) thisFace.getLandmarks().get(2).getPosition().x * scale;
            double landmark2_y = (int) thisFace.getLandmarks().get(2).getPosition().y * scale;
            tvNose.setText("Base of nose coordinate(x,y) is : " + "  (" + landmark2_x + ", " + landmark2_y + ")");

            double landmark7_x = (int) thisFace.getLandmarks().get(7).getPosition().x * scale;
            double landmark7_y = (int) thisFace.getLandmarks().get(7).getPosition().y * scale;
            tvBottomMouth.setText("Bottom of mouth coordinate(x,y) is : " + "  (" + landmark7_x + ", " + landmark7_y + ")");

            double landmark6_x = (int) thisFace.getLandmarks().get(6).getPosition().x * scale;
            double landmark6_y = (int) thisFace.getLandmarks().get(6).getPosition().y * scale;
            tvLeftClip.setText("Left clip coordinate(x,y) is : " + "  (" + landmark6_x + ", " + landmark6_y + ")");

            double landmark5_x = (int) thisFace.getLandmarks().get(5).getPosition().x * scale;
            double landmark5_y = (int) thisFace.getLandmarks().get(5).getPosition().y * scale;
            tvRightClip.setText("Right clip coordinate(x,y) is : " + "  (" + landmark5_x + ", " + landmark5_y + ")");

            /*
             * judge the mouth open range[0,1]
             * 嘴巴张开幅度估算 = (下嘴唇坐标y - 鼻子坐标y) / (左右嘴唇均值坐标y - 鼻子坐标y + 常数C,下嘴唇相对于左右嘴唇均值y的偏移量) - 1
             */
            double scope = (landmark7_y - landmark2_y) / ((landmark5_y + landmark6_y) / 2 - landmark2_y +
                    ((landmark5_x - landmark6_x) / 5)) - 1;
            DecimalFormat df = new DecimalFormat(".#");
            String strScope = df.format(scope * 100);
            if (scope < 0) {
                tvOpenMouthRange.setText("嘟嘴了吧，么么哒");
            } else {
                tvOpenMouthRange.setText("嘴巴张开幅度: " + strScope + "%");
            }

            // Draw Rectangles on the Faces
            for (Landmark landmark : faceLandMark) {
                int cx = (int) (landmark.getPosition().x * scale);
                int cy = (int) (landmark.getPosition().y * scale);
                tempCanvas.drawCircle(cx, cy, 10, paint);
                Log.d(TAG, "--->> cx is : " + cx + "\n" + "--->> cy is : " + cy);
            }
            tempCanvas.drawRoundRect(new RectF(x1, y1, x2, y2), 2, 2, mRectPaint);
            tempCanvas.drawText("Smile point is : " + smile , x1 + 2, y1 + 80, textPaint);
        }
        imgeFace.setImageDrawable(new BitmapDrawable(getResources(),tempBitmap));
    }

}
