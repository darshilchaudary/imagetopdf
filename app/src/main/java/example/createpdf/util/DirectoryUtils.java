package example.createpdf.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;
import android.preference.PreferenceManager;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import example.createpdf.R;

import static example.createpdf.util.Constants.STORAGE_LOCATION;
import static example.createpdf.util.Constants.pdfExtension;
import static example.createpdf.util.StringUtils.getDefaultStorageLocation;

public class DirectoryUtils {

    private final Context mContext;
    private final SharedPreferences mSharedPreferences;
    private ArrayList<String> mFilePaths;

    public DirectoryUtils(Context context) {
        mContext = context;
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
    }

    public ArrayList<File> searchPDF(String query) {
        ArrayList<File> searchResult = new ArrayList<>();
        final File[] files = getOrCreatePdfDirectory().listFiles();
        ArrayList<File> pdfs = searchPdfsFromPdfFolder(files);
        for (File pdf : pdfs) {
            String path = pdf.getPath();
            String[] fileName = path.split("/");
            String pdfName = fileName[fileName.length - 1].replace("pdf" , "");
            if (checkChar(query , pdfName) == 1) {
                searchResult.add(pdf);
            }
        }
        return searchResult;
    }

    private int checkChar(String query , String fileName) {
        query = query.toLowerCase();
        fileName = fileName.toLowerCase();
        Set<Character> q = new HashSet<>();
        Set<Character> f = new HashSet<>();
        for ( char c : query.toCharArray() ) {
            q.add(c);
        }
        for ( char c : fileName.toCharArray() ) {
            f.add(c);
        }

        if ( q.containsAll(f) || f.containsAll(q) )
            return 1;

        return 0;
    }

    public ArrayList<File> getPdfsFromPdfFolder(File[] files) {
        ArrayList<File> pdfFiles = new ArrayList<>();
        if (files == null)
            return pdfFiles;
        for (File file : files) {
            if (isPDFAndNotDirectory(file))
                pdfFiles.add(file);
        }
        return pdfFiles;
    }

    private ArrayList<File> searchPdfsFromPdfFolder(File[] files) {
        ArrayList<File> pdfFiles = getPdfsFromPdfFolder(files);
        for (File file : files) {
            if (file.isDirectory()) {
                for (File dirFiles : file.listFiles()) {
                    if (isPDFAndNotDirectory(dirFiles))
                        pdfFiles.add(dirFiles);
                }
            }
        }
        return pdfFiles;
    }

    private boolean isPDFAndNotDirectory(File file) {
        return !file.isDirectory() &&
                file.getName().endsWith(mContext.getString(R.string.pdf_ext));
    }

    public File getOrCreatePdfDirectory() {
        File folder = new File(mSharedPreferences.getString(STORAGE_LOCATION,
                getDefaultStorageLocation()));
        if (!folder.exists())
            folder.mkdir();
        return folder;
    }

    public ArrayList<File> getPdfFromOtherDirectories() {
        mFilePaths = new ArrayList<>();
        walkdir(getOrCreatePdfDirectory());
        ArrayList<File> files = new ArrayList<>();
        for (String path : mFilePaths)
            files.add(new File(path));
        return files;
    }

    public File getDirectory(String dirName) {
        File folder = new File(mSharedPreferences.getString(STORAGE_LOCATION,
                getDefaultStorageLocation()) + dirName);
        if (!folder.exists()) {
            return null;
        }
        return folder;
    }

    public ArrayList<String> getAllPDFsOnDevice() {
        mFilePaths = new ArrayList<>();
        walkdir(Environment.getExternalStorageDirectory());
        return mFilePaths;
    }

    private void walkdir(File dir) {
        File[] listFile = dir.listFiles();
        if (listFile != null) {
            for (File aListFile : listFile) {

                if (aListFile.isDirectory()) {
                    walkdir(aListFile);
                } else {
                    if (aListFile.getName().endsWith(pdfExtension)) {
                        //Do what ever u want
                        mFilePaths.add(aListFile.getAbsolutePath());
                    }
                }
            }
        }
    }
}
