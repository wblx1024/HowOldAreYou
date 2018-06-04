package retailmedia.com.howold;

import android.graphics.Bitmap;
import android.util.Log;

import com.megvii.cloud.http.CommonOperate;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;

/**
 * author:guand
 * Creation time:2017/01/03 16:46
 * email:674025184@qq.com
 */

public class FaceppDetect {

    public static final String KEY = "qWdYDF4s9R7fbj1PCPUFSfuSEBiLPWrF";
    public static final String SECRET = "x1w2D3e12pXKeGARJuNrpS4dhFlnVVhl";


    public interface CallBack {

        void success(JSONObject result);

        void error(Exception exception);
    }


    public static void detect(final Bitmap bm, final CallBack callBack) {

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    CommonOperate commonOperate = new CommonOperate(KEY, SECRET);
                    Bitmap bmSmall = Bitmap.createBitmap(bm, 0, 0, bm.getWidth(), bm.getHeight());
                    ByteArrayOutputStream stream = new ByteArrayOutputStream();
                    bmSmall.compress(Bitmap.CompressFormat.JPEG, 100, stream);
                    byte[] arrays = stream.toByteArray();
                    String content = new String(commonOperate.detectByte(arrays, 0, "gender,age").getContent());
                    //得到网络返回的数据
                    JSONObject jsonObject = new JSONObject(content);
                    Log.e("TAG", jsonObject.toString());

                    if (callBack != null) {
                        callBack.success(jsonObject);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    if (callBack != null) {
                        callBack.error(e);
                    }
                }

            }
        }).start();
    }


}
