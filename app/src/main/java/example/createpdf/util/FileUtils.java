package example.createpdf.util;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.preference.PreferenceManager;
import android.print.PageRange;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintDocumentInfo;
import android.print.PrintManager;
import android.provider.MediaStore;
import android.support.v4.content.FileProvider;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import example.createpdf.R;
import example.createpdf.database.DatabaseHelper;

import static example.createpdf.util.Constants.AUTHORITY_APP;
import static example.createpdf.util.Constants.PATH_SEPERATOR;
import static example.createpdf.util.Constants.STORAGE_LOCATION;
import static example.createpdf.util.Constants.pdfDirectory;
import static example.createpdf.util.Constants.pdfExtension;
import static example.createpdf.util.FileUriUtils.getUriRealPathAboveKitkat;
import static example.createpdf.util.FileUriUtils.isWhatsappImage;
import static example.createpdf.util.StringUtils.getDefaultStorageLocation;
import static example.createpdf.util.StringUtils.showSnackbar;

public class FileUtils {

    private final Activity mContext;
    private final SharedPreferences mSharedPreferences;

    public FileUtils(Activity context) {
        this.mContext = context;
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
    }

    public static String getFormattedDate(File file) {
        Date lastModDate = new Date(file.lastModified());
        String[] formatdate = lastModDate.toString().split(" ");
        String time = formatdate[3];
        String[] formattime =  time.split(":");
        String date = formattime[0] + ":" + formattime[1];
        return formatdate[0] + ", " + formatdate[1] + " " + formatdate[2] + " at " + date;
    }

    public static String getFormattedSize(File file) {
        return String.format("%.2f MB", (double) file.length() / (1024 * 1024));
    }

    public void printFile(final File file) {

        final PrintDocumentAdapter mPrintDocumentAdapter = new PrintDocumentAdapter() {

            @Override
            public void onWrite(PageRange[] pages,
                                ParcelFileDescriptor destination,
                                CancellationSignal cancellationSignal,
                                WriteResultCallback callback) {
                try {
                    InputStream input = new FileInputStream(file.getName());
                    OutputStream output = new FileOutputStream(destination.getFileDescriptor());

                    byte[] buf = new byte[1024];
                    int bytesRead;

                    while ((bytesRead = input.read(buf)) > 0)
                        output.write(buf, 0, bytesRead);

                    callback.onWriteFinished(new PageRange[]{PageRange.ALL_PAGES});

                    input.close();
                    output.close();

                } catch (Exception e) {
                }
            }

            @Override
            public void onLayout(PrintAttributes oldAttributes,
                                 PrintAttributes newAttributes,
                                 CancellationSignal cancellationSignal,
                                 LayoutResultCallback callback,
                                 Bundle extras) {

                if (cancellationSignal.isCanceled()) {
                    callback.onLayoutCancelled();
                    return;
                }
                PrintDocumentInfo pdi = new PrintDocumentInfo.Builder("myFile")
                        .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                        .build();

                callback.onLayoutFinished(pdi, true);
            }
        };

        PrintManager printManager = (PrintManager) mContext
                .getSystemService(Context.PRINT_SERVICE);
        String jobName = mContext.getString(R.string.app_name) + " Document";
        if (printManager != null) {
            printManager.print(jobName, mPrintDocumentAdapter, null);
            new DatabaseHelper(mContext).insertRecord(file.getAbsolutePath(), mContext.getString(R.string.printed));
        }
    }

    public void shareFile(File file) {
        Uri uri = FileProvider.getUriForFile(mContext, AUTHORITY_APP, file);
        ArrayList<Uri> uris = new ArrayList<>();
        uris.add(uri);
        shareFile(uris);
    }

    public void shareMultipleFiles(List<File> files) {
        ArrayList<Uri> uris = new ArrayList<>();
        for (File file: files) {
            Uri uri = FileProvider.getUriForFile(mContext, AUTHORITY_APP, file);
            uris.add(uri);
        }
        shareFile(uris);
    }

    private void shareFile(ArrayList<Uri> uris) {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_SEND_MULTIPLE);
        intent.putExtra(Intent.EXTRA_TEXT, mContext.getString(R.string.i_have_attached_pdfs_to_this_message));
        intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.setType("application/pdf");
        mContext.startActivity(Intent.createChooser(intent, "Sharing"));
    }

    public void openFile(String path) {
        File file = new File(path);
        Intent target = new Intent(Intent.ACTION_VIEW);
        target.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
        try {
            Uri uri = FileProvider.getUriForFile(mContext, AUTHORITY_APP, file);
            target.setDataAndType(uri, mContext.getString(R.string.pdf_type));
            target.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            openIntent(Intent.createChooser(target, mContext.getString(R.string.open_file)));
        } catch (Exception e) {
            showSnackbar(mContext, R.string.error_occurred);
        }
    }

    private int checkRepeat(String finalOutputFile, final ArrayList<File> mFile) {
        boolean flag = true;
        int append = 0;
        while (flag) {
            append++;
            String name = finalOutputFile.replace(mContext.getString(R.string.pdf_ext),
                    append + mContext.getString(R.string.pdf_ext));
            flag = mFile.contains(new File(name));
        }
        return append;
    }

    public String getUriRealPath(Uri uri) {
        String ret;

        if (uri == null)
            return null;

        if (isWhatsappImage(uri.getAuthority())) {
            ret = null;
        } else {
            ret = getUriRealPathAboveKitkat(mContext, uri);
        }
        return ret;
    }


    public boolean isFileExist(String mFileName) {
        String path = mSharedPreferences.getString(STORAGE_LOCATION,
                getDefaultStorageLocation()) + mFileName;
        File file = new File(path);
        return file.exists();
    }

    public String getFileName(Uri uri) {
        String fileName = null;
        String scheme = uri.getScheme();
        if (scheme == null)
            return null;
        if (scheme.equals("file"))
            fileName = uri.getLastPathSegment();
        else if (scheme.equals("content")) {
            Cursor cursor = mContext.getContentResolver().query(uri, null, null, null, null);
            if (cursor != null && cursor.getCount() != 0) {
                int columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME);
                cursor.moveToFirst();
                fileName = cursor.getString(columnIndex);
            }
            if (cursor != null)
                cursor.close();
        }
        return fileName;
    }

    public static String getFileName(String path) {
        return path.substring(path.lastIndexOf(PATH_SEPERATOR) + 1);
    }


    public static String getFileNameWithoutExtension(String path) {
        String p = path.substring(path.lastIndexOf(PATH_SEPERATOR) + 1);
        p = p.replace(pdfExtension, "");
        return p;
    }

    public static String getFileDirectoryPath(String path) {
        return path.substring(0, path.lastIndexOf(PATH_SEPERATOR) + 1);
    }

    public static String saveImage(String filename, Bitmap finalBitmap) {

        if (finalBitmap == null)
            return null;

        if (checkIfBitmapIsWhite(finalBitmap))
            return null;

        String root = Environment.getExternalStorageDirectory().toString();
        File myDir = new File(root + pdfDirectory);
        String fname = filename + ".png";

        File file = new File(myDir, fname);
        if (file.exists()) file.delete();
        try {
            FileOutputStream out = new FileOutputStream(file);
            finalBitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
            Log.v("saving", fname);
            out.flush();
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return myDir + "/" + fname;
    }

    private static boolean checkIfBitmapIsWhite(Bitmap bitmap) {

        if (bitmap == null)
            return true;

        int w = bitmap.getWidth();
        int h = bitmap.getHeight();
        for (int i =  0; i < w; i++) {
            for (int j = 0; j < h; j++) {
                int pixel =  bitmap.getPixel(i, j);
                if (pixel != Color.WHITE) {
                    return false;
                }
            }
        }
        return true;
    }

    public void openImage(String path) {
        File file = new File(path);
        Intent target = new Intent(Intent.ACTION_VIEW);
        target.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
        Uri uri = FileProvider.getUriForFile(mContext, AUTHORITY_APP, file);
        target.setDataAndType(uri,  "image/*");
        target.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        openIntent(Intent.createChooser(target, mContext.getString(R.string.open_file)));
    }

    private void openIntent(Intent intent) {
        try {
            mContext.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            showSnackbar(mContext, R.string.snackbar_no_pdf_app);
        }
    }

    public Intent getFileChooser() {
        String folderPath = Environment.getExternalStorageDirectory() + "/";
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_GET_CONTENT);
        Uri myUri = Uri.parse(folderPath);
        intent.setDataAndType(myUri, mContext.getString(R.string.pdf_type));
        return Intent.createChooser(intent, mContext.getString(R.string.merge_file_select));
    }

    String getUniqueFileName(String fileName, ArrayList<File> fileList) {
        String outputFileName = fileName;
        File file = new File(outputFileName);
        if (isFileExist(file.getName())) {
            int append = checkRepeat(outputFileName, fileList);
            outputFileName = outputFileName.replace(mContext.getString(R.string.pdf_ext),
                    append + mContext.getResources().getString(R.string.pdf_ext));
        }
        return outputFileName;
    }
}