package imageeffects2.br.imageefects2;

import android.app.ProgressDialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.media.effect.Effect;
import android.media.effect.EffectContext;
import android.media.effect.EffectFactory;
import android.opengl.GLES20;
import android.opengl.GLException;
import android.opengl.GLSurfaceView;
import android.opengl.GLUtils;
import android.os.Environment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.IntBuffer;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;
import javax.net.ssl.HttpsURLConnection;

public class MainActivity extends AppCompatActivity implements GLSurfaceView.Renderer {

    private GLSurfaceView mEffectView;
    private int[] mTextures = new int[2];
    private EffectContext mEffectContext;
    private Effect mEffect;
    private TextureRenderer mTexRenderer = new TextureRenderer();
    private int mImageWidth;
    private int mImageHeight;
    private boolean mInitialized = false;
    int mCurrentEffect;
    private volatile boolean saveFrame;

    private int rotation = 0;

    public Bitmap bmpMain, bmpChange;

    private ImageView imageView;

    public void setCurrentEffect(int effect) {
        mCurrentEffect = effect;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//		requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main);
        /**
         * Initialise the renderer and tell it to only render when Explicit
         * requested with the RENDERMODE_WHEN_DIRTY option
         */




        Bundle extras = getIntent().getExtras();
        byte[] byteArray = extras.getByteArray("picture");

        bmpMain = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length);

        bmpMain = BitmapFactory.decodeResource(getResources(), R.drawable.tigre);
        bmpChange = bmpMain;
        //ImageView image = (ImageView) findViewById(R.id.imageView1);


        Button girarImagem = (Button) findViewById(R.id.girarImagem);
        girarImagem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (rotation < 270) {
                    rotation += 90;
                } else {
                    rotation = 0;
                }
                rotation = 0;
                saveBitmap(bmpChange);

            }
        });

        Button salvarImagemWeb = (Button) findViewById(R.id.salvarImagemWeb);
        salvarImagemWeb.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                uploadImage();
            }
        });


        Button pagamento = (Button) findViewById(R.id.pagamento);
        pagamento.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String send = "{\n" +
                        "   \"MerchantOrderId\":\"2014111703\",\n" +
                        "   \"Payment\":{\n" +
                        "     \"Type\":\"CreditCard\",\n" +
                        "     \"Amount\":15700,\n" +
                        "     \"Installments\":1,\n" +
                        "     \"SoftDescriptor\":\"123456789ABCD\",\n" +
                        "     \"CreditCard\":{\n" +
                        "         \"CardNumber\":\"4551870000000183\",\n" +
                        "         \"Holder\":\"Teste Holder\",\n" +
                        "         \"ExpirationDate\":\"12/2021\",\n" +
                        "         \"SecurityCode\":\"123\",\n" +
                        "         \"Brand\":\"Visa\"\n" +
                        "     }\n" +
                        "   }\n" +
                        "}";

                String metodo = "POST";

                //String url = "https://apisandbox.cieloecommerce.cielo.com.br/1/sales";

                Log.d("Cielo SDK", "Aqui 0");
                URL url = null;
                try {
                    Log.d("Cielo SDK", "Aqui 01");
                    url = new URL("https://apisandbox.cieloecommerce.cielo.com.br/1/sales");
                    Log.d("Cielo SDK", "Aqui 02");
                    efetuarPagamento(metodo,url,send);
                } catch (MalformedURLException e) {
                    Log.d("Cielo SDK", "Aqui erro 1");
                    e.printStackTrace();
                } catch (IOException e) {
                    Log.d("Cielo SDK", "Aqui erro 2");
                    e.printStackTrace();
                }


            }
        });


        imageView = (ImageView) findViewById(R.id.imageView2);

        mEffectView = (GLSurfaceView) findViewById(R.id.effectsview);
        mEffectView.setEGLContextClientVersion(2);
        mEffectView.setRenderer(this);
        mEffectView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);


        BitmapDrawable bd=(BitmapDrawable) ContextCompat.getDrawable(this, R.drawable.tigre);


        int height = bmpMain.getHeight();
        int width = bmpMain.getWidth();

        mEffectView.getLayoutParams().width = width;
        mEffectView.getLayoutParams().height = height;

        Log.d("inf_img",String.valueOf(width));
        Log.d("inf_img",String.valueOf(height));

        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int dWidth = size.x;
        int dHeight = size.y;

        //Log.d("inf_dis",String.valueOf(dWidth));
        //Log.d("inf_dis",String.valueOf(dHeight));


        mCurrentEffect = R.id.none;
    }

    private void efetuarPagamento(final String method, final URL url, final String body) throws IOException {
        Log.d("Cielo SDK", "Aqui 1");
        Thread thread = new Thread(new Runnable() {

            @Override
            public void run() {
                try  {
                    Log.d("Cielo SDK", "Aqui 2");
                    HttpsURLConnection connection = null;
                    connection = (HttpsURLConnection) url.openConnection();

                    connection.setRequestMethod(method);

                    connection.setRequestProperty("Accept", "application/json");
                    connection.setRequestProperty("Accept-Encoding", "gzip");
                    connection.setRequestProperty("Content-Type", "application/json");
                    connection.setRequestProperty("User-Agent", "CieloEcommerce/3.0 Android SDK");
                    connection.setRequestProperty("MerchantId", "03d9b59f-37f1-490f-8b45-7272fbca7773");
                    connection.setRequestProperty("MerchantKey", "XIGDULRKRCICNUXYNAWNZGURKHGRPWBCJTAQUODL");
                    connection.setRequestProperty("RequestId", UUID.randomUUID().toString());

                    connection.setDoInput(true);
                    connection.setUseCaches(false);

                    if (body != null) {
                        Log.d("Cielo SDK", "Request Body: " + body);

                        connection.setDoOutput(true);

                        DataOutputStream dataOutputStream = new DataOutputStream(connection.getOutputStream());

                        dataOutputStream.writeBytes(body);
                        dataOutputStream.flush();
                        dataOutputStream.close();
                    }

                    int statusCode = connection.getResponseCode();

        /* First, let's check if the request was successful or not.
        * If the Error Stream is null, then the request was successful.
        * If the Error Stream is not null, we CANNOT call the normal Stream.
        */
                    InputStream inputStream = connection.getErrorStream();
                    if (inputStream == null) {
                        // Since the Error Stream is null, it's safe to use the normal Stream
                        inputStream = connection.getInputStream();
                    }

                    BufferedReader responseReader = new BufferedReader(new InputStreamReader(inputStream));
                    StringBuilder responseBuilder = new StringBuilder();
                    String line;
                    String responseBody;

                    while ((line = responseReader.readLine()) != null) {
                        responseBuilder.append(line);
                    }

                    responseReader.close();
                    connection.disconnect();

                    responseBody = responseBuilder.toString();

                    Log.d("Cielo SDK", "Response Body: " + responseBody);
                } catch (Exception e) {
                    Log.d("Cielo SDK", "Aqui 3");
                    e.printStackTrace();
                }
            }
        });

        thread.start();

        //HttpResponse<String>


    }

    private void loadTextures() {
        // Generate textures
        GLES20.glGenTextures(2, mTextures, 0);

        // Load input bitmap
        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.tigre);

        bitmap = bmpMain;

        mImageWidth = bitmap.getWidth();
        mImageHeight = bitmap.getHeight();

        Log.d("inf_img",String.valueOf(mImageWidth));
        Log.d("inf_img",String.valueOf(mImageHeight));

        mTexRenderer.updateTextureSize(mImageWidth, mImageHeight);

        // Upload to texture
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextures[0]);
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);

        // Set texture parameters
        GLToolbox.initTexParams();
    }

    private void initEffect() {
        EffectFactory effectFactory = mEffectContext.getFactory();
        if (mEffect != null) {
            mEffect.release();
        }
        /**
         * Initialize the correct effect based on the selected menu/action item
         */
        switch (mCurrentEffect) {

            case R.id.none:
                break;

            case R.id.autofix:
                mEffect = effectFactory.createEffect(EffectFactory.EFFECT_AUTOFIX);
                mEffect.setParameter("scale", 0.5f);
                break;

            case R.id.bw:
                mEffect = effectFactory.createEffect(EffectFactory.EFFECT_BLACKWHITE);
                mEffect.setParameter("black", .1f);
                mEffect.setParameter("white", .7f);
                break;

            case R.id.brightness:
                mEffect = effectFactory.createEffect(EffectFactory.EFFECT_BRIGHTNESS);
                mEffect.setParameter("brightness", 2.0f);
                break;

            case R.id.contrast:
                mEffect = effectFactory.createEffect(EffectFactory.EFFECT_CONTRAST);
                mEffect.setParameter("contrast", 1.4f);
                break;

            case R.id.crossprocess:
                mEffect = effectFactory.createEffect(EffectFactory.EFFECT_CROSSPROCESS);
                break;

            case R.id.documentary:
                mEffect = effectFactory.createEffect(EffectFactory.EFFECT_DOCUMENTARY);
                break;

            case R.id.duotone:
                mEffect = effectFactory.createEffect(EffectFactory.EFFECT_DUOTONE);
                mEffect.setParameter("first_color", Color.YELLOW);
                mEffect.setParameter("second_color", Color.DKGRAY);
                break;

            case R.id.filllight:
                mEffect = effectFactory.createEffect(EffectFactory.EFFECT_FILLLIGHT);
                mEffect.setParameter("strength", .8f);
                break;

            case R.id.fisheye:
                mEffect = effectFactory.createEffect(EffectFactory.EFFECT_FISHEYE);
                mEffect.setParameter("scale", .5f);
                break;

            case R.id.flipvert:
                mEffect = effectFactory.createEffect(EffectFactory.EFFECT_FLIP);
                mEffect.setParameter("vertical", true);
                break;

            case R.id.fliphor:
                mEffect = effectFactory.createEffect(EffectFactory.EFFECT_FLIP);
                mEffect.setParameter("horizontal", true);
                break;

            case R.id.grain:
                mEffect = effectFactory.createEffect(EffectFactory.EFFECT_GRAIN);
                mEffect.setParameter("strength", 1.0f);
                break;

            case R.id.grayscale:
                mEffect = effectFactory.createEffect(EffectFactory.EFFECT_GRAYSCALE);
                break;

            case R.id.lomoish:
                mEffect = effectFactory.createEffect(EffectFactory.EFFECT_LOMOISH);
                break;

            case R.id.negative:
                mEffect = effectFactory.createEffect(EffectFactory.EFFECT_NEGATIVE);
                break;

            case R.id.posterize:
                mEffect = effectFactory.createEffect(EffectFactory.EFFECT_POSTERIZE);
                break;

            case R.id.rotate:
                mEffect = effectFactory.createEffect(EffectFactory.EFFECT_ROTATE);
                mEffect.setParameter("angle", 180);
                break;

            case R.id.saturate:
                mEffect = effectFactory.createEffect(EffectFactory.EFFECT_SATURATE);
                mEffect.setParameter("scale", .5f);
                break;

            case R.id.sepia:
                mEffect = effectFactory.createEffect(EffectFactory.EFFECT_SEPIA);
                break;

            case R.id.sharpen:
                mEffect = effectFactory.createEffect(EffectFactory.EFFECT_SHARPEN);
                break;

            case R.id.temperature:
                mEffect = effectFactory.createEffect(EffectFactory.EFFECT_TEMPERATURE);
                mEffect.setParameter("scale", .9f);
                break;

            case R.id.tint:
                mEffect = effectFactory.createEffect(EffectFactory.EFFECT_TINT);
                mEffect.setParameter("tint", Color.MAGENTA);
                break;

            case R.id.vignette:
                mEffect = effectFactory.createEffect(EffectFactory.EFFECT_VIGNETTE);
                mEffect.setParameter("scale", .5f);
                break;

            default:
                break;

        }
    }

    private void applyEffect() {
        mEffect.apply(mTextures[0], mImageWidth, mImageHeight, mTextures[1]);
    }

    private void renderResult() {
        if (mCurrentEffect != R.id.none) {
            // if no effect is chosen, just render the original bitmap
            mTexRenderer.renderTexture(mTextures[1]);
        } else {
            saveFrame=true;
            // render the result of applyEffect()
            mTexRenderer.renderTexture(mTextures[0]);
        }
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        if (!mInitialized) {
            // Only need to do this once
            mEffectContext = EffectContext.createWithCurrentGlContext();
            mTexRenderer.init();
            loadTextures();
            mInitialized = true;
        }
        if (mCurrentEffect != R.id.none) {
            // if an effect is chosen initialize it and apply it to the texture
            initEffect();
            applyEffect();
        }
        renderResult();
        if (saveFrame) {

            Bitmap bitmap = takeScreenshot(gl);

            saveBitmap(bitmap);
        }
    }

    private void saveBitmap(final Bitmap bitmap) {
        bmpChange = bitmap;
        String root = Environment.getExternalStorageDirectory().toString();
        File myDir = new File(root + "/saved_images");
        myDir.mkdirs();
        Random generator = new Random();
        int n = 10000;
        n = generator.nextInt(n);
        String fname = "Image-"+ n +".png";
        File file = new File (myDir, fname);
        if (file.exists ()) file.delete ();
        try {
            FileOutputStream out = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
            out.flush();
            out.close();
            Log.i("TAG", "Image SAVED=========="+file.getAbsolutePath());
            //imageView.setImageBitmap(bitmap);

            new Thread(new Runnable() {
                public void run() {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Matrix matrix = new Matrix();
                            matrix.postRotate(rotation);
                            Bitmap rotatet = Bitmap.createBitmap(bmpChange,0,0,bmpChange.getWidth(),bmpChange.getHeight(),matrix,true);
                            bmpChange = rotatet;
                            ImageView imageView2 = (ImageView) findViewById(R.id.imageView2);
                            imageView2.setImageBitmap(rotatet);
                        }
                    });

                }
            }).start();

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public Bitmap takeScreenshot(GL10 mGL) {
        final int mWidth = mEffectView.getWidth();
        final int mHeight = mEffectView.getHeight();
        IntBuffer ib = IntBuffer.allocate(mWidth * mHeight);
        IntBuffer ibt = IntBuffer.allocate(mWidth * mHeight);
        mGL.glReadPixels(0, 0, mWidth, mHeight, GL10.GL_RGBA, GL10.GL_UNSIGNED_BYTE, ib);

        // Convert upside down mirror-reversed image to right-side up normal
        // image.
        for (int i = 0; i < mHeight; i++) {
            for (int j = 0; j < mWidth; j++) {
                ibt.put((mHeight - i - 1) * mWidth + j, ib.get(i * mWidth + j));
            }
        }

        Bitmap mBitmap = Bitmap.createBitmap(mWidth, mHeight, Bitmap.Config.ARGB_8888);
        mBitmap.copyPixelsFromBuffer(ibt);
        return mBitmap;
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        if (mTexRenderer != null) {
            mTexRenderer.updateViewSize(width, height);
        }
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        setCurrentEffect(item.getItemId());
        mEffectView.requestRender();
        return true;
    }

    public String getStringImage(Bitmap bmp){
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bmp.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        byte[] imageBytes = baos.toByteArray();
        String encodedImage = Base64.encodeToString(imageBytes, Base64.DEFAULT);
        return encodedImage;
    }


    private void uploadImage(){
        //Showing the progress dialog
        final ProgressDialog loading = ProgressDialog.show(this,"Uploading...","Please wait...",false,false);
        StringRequest stringRequest = new StringRequest(Request.Method.POST, "http://192.168.0.52/temps/salvar_img/setAvaliacao.php",
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String s) {
                        //Disimissing the progress dialog
                        loading.dismiss();
                        //Showing toast message of the response
                        Toast.makeText(MainActivity.this, s , Toast.LENGTH_LONG).show();
                    }

                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError volleyError) {
                        //Dismissing the progress dialog
                        loading.dismiss();

                        //Showing toast
                        Toast.makeText(MainActivity.this, volleyError.getMessage().toString(), Toast.LENGTH_LONG).show();
                    }
                }){
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                //Converting Bitmap to String
                String image = getStringImage(bmpChange);


                //Creating parameters
                Map<String,String> params = new Hashtable<String, String>();

                //Adding parameters
                params.put("image", image);

                //returning parameters
                return params;
            }
        };

        //Creating a Request Queue
        RequestQueue requestQueue = Volley.newRequestQueue(this);

        //Adding request to the queue
        requestQueue.add(stringRequest);
    }
}
