package example.createpdf.util;

import android.os.AsyncTask;

import com.itextpdf.text.Document;
import com.itextpdf.text.pdf.PdfCopy;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.PdfWriter;

import java.io.FileOutputStream;

import example.createpdf.interfaces.MergeFilesListener;

import static example.createpdf.util.Constants.pdfExtension;

public class MergePdf extends AsyncTask<String, Void, Void> {

    private String mFinPath;
    private Boolean mIsPDFMerged;
    private String mFilename;
    private boolean mIsPasswordProtected;
    private String mPassword, mMasterPwd;
    private final MergeFilesListener mMergeFilesListener;

    public MergePdf(String fileName, String homePath, boolean isPasswordProtected,
                    String password, MergeFilesListener mergeFilesListener, String masterpwd) {
        mFilename = fileName;
        mMergeFilesListener = mergeFilesListener;
        mFinPath = homePath;
        mIsPasswordProtected = isPasswordProtected;
        mPassword = password;
        mMasterPwd = masterpwd;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        mIsPDFMerged = false;
        mMergeFilesListener.mergeStarted();
    }

    @Override
    protected Void doInBackground(String... pdfpaths) {
        try {
            PdfReader pdfreader;
            Document document = new Document();
            mFilename = mFilename + pdfExtension;
            mFinPath = mFinPath + mFilename;
            PdfCopy copy = new PdfCopy(document, new FileOutputStream(mFinPath));
            if (mIsPasswordProtected) {
                copy.setEncryption(mPassword.getBytes(),
                        mMasterPwd.getBytes(),
                        PdfWriter.ALLOW_PRINTING | PdfWriter.ALLOW_COPY,
                        PdfWriter.ENCRYPTION_AES_128);
            }
            document.open();
            int numOfPages;
            for (String pdfPath : pdfpaths) {
                pdfreader = new PdfReader(pdfPath);
                numOfPages = pdfreader.getNumberOfPages();
                for (int page = 1; page <= numOfPages; page++) {
                    copy.addPage(copy.getImportedPage(pdfreader, page));
                }
            }
            mIsPDFMerged = true;
            document.close();
        } catch (Exception e) {
            mIsPDFMerged = false;
            e.printStackTrace();
        }
        return null;
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        super.onPostExecute(aVoid);
        mMergeFilesListener.resetValues(mIsPDFMerged, mFinPath);
    }
}