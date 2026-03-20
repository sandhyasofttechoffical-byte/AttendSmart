package com.sandhyyasofttech.attendsmart.Utils;

import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.webkit.MimeTypeMap;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class ImageCompressor {

    private static final int MAX_WIDTH = 1024;
    private static final int MAX_HEIGHT = 1024;
    private static final int QUALITY = 80;

    public static byte[] compressImage(Context context, Uri imageUri) throws IOException {
        InputStream inputStream = context.getContentResolver().openInputStream(imageUri);
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(inputStream, null, options);
        inputStream.close();

        int imageWidth = options.outWidth;
        int imageHeight = options.outHeight;

        int sampleSize = 1;
        if (imageWidth > MAX_WIDTH || imageHeight > MAX_HEIGHT) {
            float widthRatio = (float) imageWidth / MAX_WIDTH;
            float heightRatio = (float) imageHeight / MAX_HEIGHT;
            sampleSize = (int) Math.min(widthRatio, heightRatio);
        }

        options.inJustDecodeBounds = false;
        options.inSampleSize = sampleSize;
        options.inPreferredConfig = Bitmap.Config.RGB_565;

        inputStream = context.getContentResolver().openInputStream(imageUri);
        Bitmap bitmap = BitmapFactory.decodeStream(inputStream, null, options);
        inputStream.close();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, QUALITY, baos);
        
        bitmap.recycle();
        
        return baos.toByteArray();
    }

    public static File saveCompressedImage(Context context, Uri imageUri, String fileName) throws IOException {
        byte[] compressedData = compressImage(context, imageUri);
        
        File cacheDir = context.getCacheDir();
        File imageFile = new File(cacheDir, fileName);
        
        FileOutputStream fos = new FileOutputStream(imageFile);
        fos.write(compressedData);
        fos.close();
        
        return imageFile;
    }

    public static String getMimeType(Context context, Uri uri) {
        String mimeType = null;
        if (uri.getScheme().equals(ContentResolver.SCHEME_CONTENT)) {
            ContentResolver cr = context.getContentResolver();
            mimeType = cr.getType(uri);
        } else {
            String fileExtension = MimeTypeMap.getFileExtensionFromUrl(uri.toString());
            mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileExtension.toLowerCase());
        }
        return mimeType;
    }
}