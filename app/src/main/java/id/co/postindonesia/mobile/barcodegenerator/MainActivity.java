package id.co.postindonesia.mobile.barcodegenerator;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    public final static String APP_PATH_SD_CARD = "/downloads/posindonesia/";

    private EditText barcodeEditText;
    private ImageView barcodeImage;
    private Button barcodeButton;
    private Spinner barcodeList;

    private String[] listBarcodeType;

    private Activity activity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        activity = this;

        barcodeEditText = (EditText)findViewById(R.id.barcodeEditText);
        barcodeImage = (ImageView)findViewById(R.id.barcodeImageView);
        barcodeButton = (Button)findViewById(R.id.barcodeButton);
        barcodeList = (Spinner)findViewById(R.id.listBarcode);

        ArrayList<String> tempList = new ArrayList<>();
        tempList.add("" + BarcodeFormat.CODE_39);
        tempList.add("" + BarcodeFormat.CODE_93);
        tempList.add("" + BarcodeFormat.CODE_128);
        tempList.add("" + BarcodeFormat.EAN_8);
        tempList.add("" + BarcodeFormat.EAN_13);
        tempList.add("" + BarcodeFormat.QR_CODE);

        listBarcodeType = tempList.toArray(new String[tempList.size()]);

        ArrayAdapter<String> spinnerArrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, listBarcodeType); //selected item will look like a spinner set from XML
        spinnerArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        barcodeList.setAdapter(spinnerArrayAdapter);

        // barcode image
        final Bitmap[] barcodeBitmap = {null};

        barcodeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("barcode","barcode format : " + BarcodeFormat.CODE_128);
                try {
                    int pos = barcodeList.getSelectedItemPosition();
                    switch (pos){
                        case 0 :
                            barcodeBitmap[0] = encodeAsBitmap(barcodeEditText.getText().toString(), BarcodeFormat.CODE_39, 600, 300);
                            break;
                        case 1 :
                            barcodeBitmap[0] = encodeAsBitmap(barcodeEditText.getText().toString(), BarcodeFormat.CODE_93, 600, 300);
                            break;
                        case 2 :
                            barcodeBitmap[0] = encodeAsBitmap(barcodeEditText.getText().toString(), BarcodeFormat.CODE_128, 600, 300);
                            break;
                        case 3 :
                            barcodeBitmap[0] = encodeAsBitmap(barcodeEditText.getText().toString(), BarcodeFormat.EAN_8, 600, 300);
                            break;
                        case 4 :
                            barcodeBitmap[0] = encodeAsBitmap(barcodeEditText.getText().toString(), BarcodeFormat.EAN_13, 600, 300);
                            break;
                        default:
                            barcodeBitmap[0] = encodeAsBitmap(barcodeEditText.getText().toString(), BarcodeFormat.QR_CODE, 600, 300);
                            break;
                    }
                    barcodeImage.setImageBitmap(barcodeBitmap[0]);
                } catch (WriterException e) {
                    e.printStackTrace();
                }
            }
        });

        barcodeEditText.setOnEditorActionListener(new EditText.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    try {
                        barcodeBitmap[0] = encodeAsBitmap(barcodeEditText.getText().toString(), BarcodeFormat.CODE_39, 600, 300);
                        barcodeImage.setImageBitmap(barcodeBitmap[0]);
                    } catch (WriterException e) {
                        e.printStackTrace();
                    }
                }
                return true;
            }
        });

        barcodeImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new AlertDialog.Builder(activity)
                    .setTitle("Save image confirmation")
                    .setMessage("Apakah anda yakin untuk menyimpan gambar barcode ini?")
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            saveImageToExternalStorage(barcodeBitmap[0], "barcode");
                            Toast.makeText(getBaseContext(), "Gambar tersimpan pada filepath " + APP_PATH_SD_CARD, Toast.LENGTH_SHORT).show();
                        }
                    })
                    .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    }).show();
            }
        });
    }

    /**************************************************************
     * getting from com.google.zxing.client.android.encode.QRCodeEncoder
     *
     * See the sites below
     * http://code.google.com/p/zxing/
     * http://code.google.com/p/zxing/source/browse/trunk/android/src/com/google/zxing/client/android/encode/EncodeActivity.java
     * http://code.google.com/p/zxing/source/browse/trunk/android/src/com/google/zxing/client/android/encode/QRCodeEncoder.java
     */

    private static final int WHITE = 0xFFFFFFFF;
    private static final int BLACK = 0xFF000000;

    Bitmap encodeAsBitmap(String contents, BarcodeFormat format, int img_width, int img_height) throws WriterException {
        String contentsToEncode = contents;
        if (contentsToEncode == null) {
            return null;
        }
        Map<EncodeHintType, Object> hints = null;
        String encoding = guessAppropriateEncoding(contentsToEncode);
        if (encoding != null) {
            hints = new EnumMap<EncodeHintType, Object>(EncodeHintType.class);
            hints.put(EncodeHintType.CHARACTER_SET, encoding);
        }
        MultiFormatWriter writer = new MultiFormatWriter();
        BitMatrix result;
        try {
            result = writer.encode(contentsToEncode, format, img_width, img_height, hints);
        } catch (IllegalArgumentException iae) {
            // Unsupported format
            return null;
        }
        int width = result.getWidth();
        int height = result.getHeight();
        int[] pixels = new int[width * height];
        for (int y = 0; y < height; y++) {
            int offset = y * width;
            for (int x = 0; x < width; x++) {
                pixels[offset + x] = result.get(x, y) ? BLACK : WHITE;
            }
        }

        Bitmap bitmap = Bitmap.createBitmap(width, height,
                Bitmap.Config.ARGB_8888);
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
        return bitmap;
    }

    private static String guessAppropriateEncoding(CharSequence contents) {
        // Very crude at the moment
        for (int i = 0; i < contents.length(); i++) {
            if (contents.charAt(i) > 0xFF) {
                return "UTF-8";
            }
        }
        return null;
    }


    private String saveToInternalStorage(Bitmap bitmapImage, String barcodeName){
        ContextWrapper cw = new ContextWrapper(getApplicationContext());
        // path to /data/data/yourapp/app_data/imageDir
        File directory = cw.getDir("imageDir", Context.MODE_PRIVATE);
        // Create imageDir
        File mypath=new File(directory, "" + barcodeName + ".jpg");

        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(mypath);
            // Use the compress method on the BitMap object to write image to the OutputStream
            bitmapImage.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return directory.getAbsolutePath();
    }

    public boolean saveImageToExternalStorage(Bitmap image, String barcodeName) {
        String fullPath = Environment.getExternalStorageDirectory().getAbsolutePath() + APP_PATH_SD_CARD;

        try {
            File dir = new File(fullPath);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            OutputStream fOut = null;
            File file = new File(fullPath, "" + barcodeName + ".png");
            file.createNewFile();
            fOut = new FileOutputStream(file);

            // 100 means no compression, the lower you go, the stronger the compression
            image.compress(Bitmap.CompressFormat.PNG, 100, fOut);
            fOut.flush();
            fOut.close();

            MediaStore.Images.Media.insertImage(getContentResolver(), file.getAbsolutePath(), file.getName(), file.getName());

            return true;

        } catch (Exception e) {
            Log.e("saveToExternalStorage()", e.getMessage());
            return false;
        }
    }

}
