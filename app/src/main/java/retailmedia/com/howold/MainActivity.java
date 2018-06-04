package retailmedia.com.howold;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.hardware.Camera;
import android.hardware.camera2.CameraDevice;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * author:guand
 * Creation time:2017/01/03 12:30
 * email:674025184@qq.com
 *
 * 功能：点击SLIENCE 对准摄像头注释5秒，出来照片之后，点击识别即可
 */
public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = "MainActivity";
    private static final int PICK_CODE = 0x110;
    private ImageView mPhoto;
    private Button mSlientTake;
    private Button mDetect;
    private TextView mTip;
    private View mWaitting;
    private String mCurrentPhotoStr;
    private Bitmap mPhotoImg;
    private Paint mPaint;
    //文件存储的路径
    public static String POTOPATH = Environment.getExternalStorageDirectory() + "/silent";
    private File file;

    private SurfaceView mySurfaceView;
    private SurfaceHolder myHolder;
    private Camera myCamera;
    private String path;
    private CameraDevice cameraDevice;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // 无title
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        //全屏
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initViews();
        mPaint = new Paint();
        File file = new File(POTOPATH);
        if (!file.exists()) {
            file.mkdirs();
        }
    }


    public void initViews() {
        mPhoto = (ImageView) findViewById(R.id.id_photo);
        mSlientTake = (Button) findViewById(R.id.id_getImage);
        mDetect = (Button) findViewById(R.id.id_Detect);
        mTip = (TextView) findViewById(R.id.id_tip);
        mWaitting = findViewById(R.id.id_waitting);
        mSlientTake.setOnClickListener(this);
        mDetect.setOnClickListener(this);

    }


    private static final int MSG_SUCCESS = 0x111;
    private static final int MSG_ERROR = 0x112;
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_SUCCESS:
                    mWaitting.setVisibility(View.GONE);
                    JSONObject rs = (JSONObject) msg.obj;
                    try {
                        prepareRsBitmap(rs);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    mPhoto.setImageBitmap(mPhotoImg);

                    break;
                case MSG_ERROR:
                    mWaitting.setVisibility(View.GONE);
                    String errorMsg = (String) msg.obj;
                    if (TextUtils.isEmpty(errorMsg)) {
                        mTip.setText("Error");
                    } else {
                        mTip.setText(errorMsg);
                    }
                    break;
            }
            super.handleMessage(msg);
        }
    };

    private void prepareRsBitmap(JSONObject rs) throws JSONException {
        Bitmap bitmap = Bitmap.createBitmap(mPhotoImg.getWidth(), mPhotoImg.getHeight(), mPhotoImg.getConfig());

        Canvas canvas = new Canvas(bitmap);

        canvas.drawBitmap(mPhotoImg, 0, 0, null);

        JSONArray faces = rs.getJSONArray("faces");

        int faceCount = faces.length();
        mTip.setText("发现几张脸:" + faceCount);
        for (int i = 0; i < faceCount; i++) {
            //单独拿到face对象
            JSONObject face = faces.getJSONObject(i);
            JSONObject posObj = face.getJSONObject("face_rectangle");
            float startX = (float) posObj.optDouble("left");
            float startY = (float) posObj.optDouble("top");
            float width = (float) posObj.optDouble("width");
            float height = (float) posObj.optDouble("height");

            System.out.println("startX:" + startX + ",startY:" + startY + ",width:" + width + ",height:" + height);

            mPaint.setColor(Color.YELLOW);
            mPaint.setStrokeWidth(5);

//            x:430.2222: ,y:618.24,w:481.8489,h:480.85333   X:w:表示检出的脸的宽度在图片中百分比
//            startX:410.0,startY:410.0,width:527.0,height:527.0


            canvas.drawLine(startX, startY, startX + width, startY, mPaint);
            canvas.drawLine(startX, startY + height, startX + width, startY + height, mPaint);
            canvas.drawLine(startX, startY, startX, startY + height, mPaint);
            canvas.drawLine(startX + width, startY, startX + width, startY + height, mPaint);


            //get age and gender
            int age = face.getJSONObject("attributes").getJSONObject("age").getInt("value");

            String gender = face.getJSONObject("attributes").getJSONObject("gender").getString("value");
            Log.e("canshu", "年龄:" + age + "," + "性别:" + gender);
            mTip.setText("年龄:" + age + "," + "性别:" + gender);
//            转换bitmap
            Bitmap ageBitmap = buildAgeBitmap(age, "Male".equals(gender));

            int ageWidth = ageBitmap.getWidth();
            int ageHeight = ageBitmap.getHeight();
            if (bitmap.getWidth() < mPhoto.getWidth() && bitmap.getHeight() < mPhoto.getHeight()) {

                float ratio = Math.max(bitmap.getWidth() * 1.0f / mPhoto.getWidth(),
                        bitmap.getHeight() * 1.0f / mPhoto.getHeight());
                ageBitmap = Bitmap.createScaledBitmap(ageBitmap, (int) (ageWidth * ratio), (int) (ageHeight * ratio), false);
            }

            canvas.drawBitmap(ageBitmap, startX + (width / 2 - ageBitmap.getWidth() / 2), startY - ageBitmap.getHeight(), null);

            mPhotoImg = bitmap;
        }
    }


    //
    private Bitmap buildAgeBitmap(int age, boolean isMale) {
        TextView tv = (TextView) mWaitting.findViewById(R.id.id_age_and_gender);
        tv.setText(age + "");
        if (isMale) {
            tv.setCompoundDrawablesWithIntrinsicBounds(getResources().getDrawable(R.drawable.male), null, null, null);
        } else {
            tv.setCompoundDrawablesWithIntrinsicBounds(getResources().getDrawable(R.drawable.female), null, null, null);
        }
        tv.setDrawingCacheEnabled(true);
        Bitmap bitmap = Bitmap.createBitmap(tv.getDrawingCache());
        tv.destroyDrawingCache();
        return bitmap;
    }


    public String getPictureFromFile() {
        //判断是否有sd卡
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            File[] files = new File(POTOPATH).listFiles();
            if (files.length != 0) {
                path = POTOPATH + File.separator + files[0].getName();
                Log.e("FDs", path);
                return path;
            }
        }
        return null;
    }

    private void showDialog(String title, String message, DialogInterface.OnClickListener onCancel, DialogInterface.OnClickListener onOK) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setNegativeButton("取消", onCancel)
                .setPositiveButton("确定", onOK)
                .create().show();
    }

    private static DialogInterface.OnClickListener emptyListener = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialogInterface, int i) {

        }
    };

    @Override
    public void onClick(View v) {
        if(v==null){
            Log.i(TAG, "v is null");
            return;
        }else {
//            Log.i(TAG, "Get:" + v.getId()+"  Silence:" + R.id.id_getImage+"  Detect:" + R.id.id_Detect);
        }

        switch (v.getId()) {
            case R.id.id_getImage:
//              倒计时
                CountDownTimer countDownTimer = new CountDownTimer(10000, 3000) {
                    @Override
                    public void onTick(long millisUntilFinished) {
                        Log.i(TAG, "启动拍照！");
                        startTakePhoto();
                        Log.i(TAG, "开始拍照！");
                    }

                    @Override
                    public void onFinish() {
                        Log.i(TAG, "拍照完成！");
                        mSlientTake.setEnabled(false);
                        try {
                            file = new File(getPictureFromFile());

                            if (path != null && !path.trim().equals("")) {
                                resizePhoto();
                            }
//                            //将图片显示到textView上面
                            Glide.with(MainActivity.this).load(file).asBitmap().into(mPhoto);
//                            btn_recongize_get.setEnabled(true);
                            mDetect.setEnabled(true);

                        } catch (Exception e) {
                            Log.i(TAG, "开始拍照！");
                            System.out.println(e.toString());
                        }
                    }
                }.start();
                break;
            case R.id.id_Detect:
                mWaitting.setVisibility(View.VISIBLE);
                FaceppDetect.detect(mPhotoImg, new FaceppDetect.CallBack() {
                    @Override
                    public void success(JSONObject result) {
                        Message msg = Message.obtain();
                        msg.what = MSG_SUCCESS;
                        msg.obj = result;
                        handler.sendMessage(msg);
                    }

                    @Override
                    public void error(Exception exception) {
                        Message msg = Message.obtain();
                        msg.what = MSG_ERROR;
                        msg.obj = exception.getMessage();
                        handler.sendMessage(msg);
                    }
                });
//                FaceDetectRealize faceDetectRealize=new FaceDetectRealize()
//                FaceppDetect.detect(mPhotoImg,faceDetectRealize);

                break;

        }
    }
//
//    class FaceDetectRealize implements FaceppDetect.CallBack{
//        @Override
//        public void success(JSONObject result) {
//
//        }
//
//        @Override
//        public void error(Exception exception) {
//
//        }
//    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        deleteAllFiles();
    }

    @Override
    protected void onPause() {
        super.onPause();
        deleteAllFiles();
    }

    /**
     * 删除文件夹下面所有的照片， 递归调用
     */
    private void deleteAllFiles() {
        File[] files = new File(POTOPATH).listFiles();
        if (files != null && file.length() != 0) {
            for (File f : files) {
                if (f.isFile()) {
                    try {
                        f.delete();
                        deleteAllFiles();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    if (f.exists()) {
                        try {
                            f.delete();
                            deleteAllFiles();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }

            }
        }
    }


    // 初始化surface
    @SuppressWarnings("deprecation")
    private void initSurface() {
        // 初始化surfaceview
        if (mySurfaceView == null && myHolder == null) {
            mySurfaceView = (SurfaceView) findViewById(R.id.camera_surfaceview);
            // 初始化surfaceholder
            myHolder = mySurfaceView.getHolder();
        }

    }

    /**
     * 开始拍照
     */
    private void startTakePhoto() {
        //初始化surface
        initSurface();
        //这里得开线程进行拍照，因为Activity还未显示完全的时候是无法进行拍照的，SurfacaView必须先显示
        new Thread() {
            @Override
            public void run() {
                super.run();
                //如果存在摄像头
                if (getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
                    //获取摄像头
                    Log.i(TAG,"获取摄像头！");
                    for (int i=0;i<3;i++){
                        if (openFacingFrontCamera()) {
                            Log.i(TAG, "进行对焦!");
                            //进行对焦
                            autoFocus();
                            Log.i(TAG, "对焦OK！");
                            break;
                        } else {
                            Log.i(TAG, "openCameraFailed");
                            try {
                                Thread.sleep(3000);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }

            }
        }.start();
    }

    // 自动对焦回调函数(空实现)
    private Camera.AutoFocusCallback myAutoFocus = new Camera.AutoFocusCallback() {
        @Override
        public void onAutoFocus(boolean success, Camera camera) {
//            camera.cancelAutoFocus();

        }
    };

    // 对焦并拍照
    private void autoFocus() {

        try {
            // 因为开启摄像头需要时间，这里让线程睡两秒
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // 自动对焦
//        myCamera.startPreview();
        myCamera.autoFocus(myAutoFocus);

        // 对焦后拍照
        myCamera.takePicture(null, null, myPicCallback);
    }

    // 拍照成功回调函数
    private Camera.PictureCallback myPicCallback = new Camera.PictureCallback() {

        @Override
        public void onPictureTaken(byte[] data, Camera camera) {


            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss")
                    .format(new Date());

            // 创建并保存图片文件
//            File pictureFile = new File(getDir(), "IMG_" + timeStamp + ".jpg");
            File file = new File(POTOPATH);
            file.mkdirs(); // 创建文件夹保存照片
            String filename = file.getPath() + File.separator + timeStamp + ".jpg";

            // 将得到的照片进行270°旋转，使其竖直
            Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
            Matrix matrix = new Matrix();
            matrix.preRotate(270);
            bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(),
                    bitmap.getHeight(), matrix, true);

            try {
                Toast.makeText(MainActivity.this, "开始拍照", Toast.LENGTH_SHORT)
                        .show();
                FileOutputStream fos = new FileOutputStream(filename);
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
                fos.close();
                Log.i(TAG, "获取照片成功");
                myCamera.stopPreview();
                myCamera.release();
                myCamera = null;
            } catch (Exception error) {
                Toast.makeText(MainActivity.this, "拍照失败", Toast.LENGTH_SHORT)
                        .show();

                Log.i(TAG, "保存照片失败" + error.toString());
                error.printStackTrace();
                myCamera.stopPreview();
                myCamera.release();
                myCamera = null;
            }
        }
    };

    // 得到前置摄像头
    private boolean openFacingFrontCamera() {
        int checkCallPhonePermission  = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA);
        if(checkCallPhonePermission != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_REQUEST_CODE);
            return false;
        }

        // 尝试开启前置摄像头
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        Log.i(TAG, "相机数：" +  Camera.getNumberOfCameras());
        for (int camIdx = 0, cameraCount = Camera.getNumberOfCameras(); camIdx < cameraCount; camIdx++) {
            Camera.getCameraInfo(camIdx, cameraInfo);
            if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                try {
                    Log.i(TAG, "tryToOpenCamera");
                    myCamera = Camera.open(camIdx);
                } catch (RuntimeException e) {
                    e.printStackTrace();
//                    return false;
                }
            }
        }

        // 如果开启前置失败（无前置）则开启后置
        if (myCamera == null) {
            for (int camIdx = 0, cameraCount = Camera.getNumberOfCameras(); camIdx < cameraCount; camIdx++) {
                Camera.getCameraInfo(camIdx, cameraInfo);
                if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                    try {
                        myCamera = Camera.open(camIdx);
                    } catch (RuntimeException e) {
//                        return false;
                    }
                }
            }
        }

        if (myCamera == null) {
            Log.i(TAG, "开启失败！");
//            return false;
        }

        try {
            // 这里的myCamera为已经初始化的Camera对象
            myCamera.setPreviewDisplay(myHolder);
            myCamera.startPreview();
        } catch (IOException e) {
            e.printStackTrace();
            myCamera.stopPreview();
            myCamera.release();
            myCamera = null;
            return false;
        }

        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == PICK_CODE) {
            if (data != null) {
                Uri url = data.getData();
                Cursor cursor = getContentResolver().query(url, null, null, null, null);
                cursor.moveToFirst();

                int idx = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
                mCurrentPhotoStr = cursor.getString(idx);
                cursor.close();
                resizePhoto();
                mPhoto.setImageBitmap(mPhotoImg);
//                mTip.setText("Click Detect ==>");
                mDetect.setVisibility(View.VISIBLE);
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    /**
     * 压缩图片
     */
    private void resizePhoto() {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, options);
        double ratio = Math.max(options.outWidth * 1.0d / 1024f, options.outHeight * 1.0d / 1024f);
        options.inSampleSize = (int) Math.ceil(ratio);
        options.inJustDecodeBounds = false;
        mPhotoImg = BitmapFactory.decodeFile(path, options);

    }

    private static final int CAMERA_REQUEST_CODE = 1;
    @Override
    public  void onRequestPermissionsResult(int requestCode,String[] permissions,int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_REQUEST_CODE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.e("权限：", "申请成功");
                openFacingFrontCamera();
            }
            else {
                //用户勾选了不再询问
                //提示用户手动打开权限
                if (!ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {
                    Toast.makeText(this, "相机权限已被禁止", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }
}
