package example.createpdf.adapter;

import android.app.Activity;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.balysv.materialripple.MaterialRippleLayout;

import java.io.File;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import butterknife.BindView;
import butterknife.ButterKnife;
import example.createpdf.R;
import example.createpdf.database.DatabaseHelper;
import example.createpdf.interfaces.DataSetChanged;
import example.createpdf.interfaces.EmptyStateChangeListener;
import example.createpdf.interfaces.ItemSelectedListener;
import example.createpdf.util.DirectoryUtils;
import example.createpdf.util.FileUtils;
import example.createpdf.util.PDFEncryptionUtility;
import example.createpdf.util.PDFUtils;
import example.createpdf.util.PopulateList;

import static example.createpdf.util.Constants.SORTING_INDEX;
import static example.createpdf.util.DialogUtils.createOverwriteDialog;
import static example.createpdf.util.FileSortUtils.NAME_INDEX;
import static example.createpdf.util.FileUtils.getFormattedDate;
import static example.createpdf.util.StringUtils.getSnackbarwithAction;
import static example.createpdf.util.StringUtils.showSnackbar;

public class ViewFilesAdapter extends RecyclerView.Adapter<ViewFilesAdapter.ViewFilesHolder>
        implements DataSetChanged, EmptyStateChangeListener {

    private final Activity mActivity;
    private final EmptyStateChangeListener mEmptyStateChangeListener;
    private final ItemSelectedListener mItemSelectedListener;

    private ArrayList<File> mFileList;
    private final ArrayList<Integer> mSelectedFiles;

    private final FileUtils mFileUtils;
    private final PDFUtils mPDFUtils;
    private final PDFEncryptionUtility mPDFEncryptionUtils;
    private final DatabaseHelper mDatabaseHelper;
    private final SharedPreferences mSharedPreferences;

    public ViewFilesAdapter(Activity activity,
                            ArrayList<File> feedItems,
                            EmptyStateChangeListener emptyStateChangeListener,
                            ItemSelectedListener itemSelectedListener) {
        this.mActivity = activity;
        this.mEmptyStateChangeListener = emptyStateChangeListener;
        this.mItemSelectedListener = itemSelectedListener;
        this.mFileList = feedItems;
        mSelectedFiles = new ArrayList<>();
        mFileUtils = new FileUtils(activity);
        mPDFUtils = new PDFUtils(activity);
        mPDFEncryptionUtils = new PDFEncryptionUtility(activity);
        mDatabaseHelper = new DatabaseHelper(mActivity);
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(mActivity);
    }

    @NonNull
    @Override
    public ViewFilesHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_file, parent, false);
        return new ViewFilesHolder(itemView, mItemSelectedListener);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewFilesHolder holder, final int pos) {
        final int position = holder.getAdapterPosition();
        final File file = mFileList.get(position);

        boolean isEncrypted = mPDFUtils.isPDFEncrypted(file.getPath());
        holder.mFilename.setText(file.getName());
        holder.mFilesize.setText(FileUtils.getFormattedSize(file));
        holder.mFiledate.setText(getFormattedDate(file));
        holder.checkBox.setChecked(mSelectedFiles.contains(position));
        holder.mEncryptionImage.setVisibility(isEncrypted ? View.VISIBLE : View.GONE);
        holder.mRipple.setOnClickListener(view -> {
            new MaterialDialog.Builder(mActivity)
                    .title(R.string.title)
                    .items(R.array.items)
                    .itemsIds(R.array.itemIds)
                    .itemsCallback((dialog, view1, which, text) -> performOperation(which, position, file))
                    .show();
            notifyDataSetChanged();
        });
    }

    private void performOperation(int index, int position, File file) {
        switch (index) {
            case 0:
                mFileUtils.openFile(file.getPath());
                break;

            case 1:
                deleteFile(file.getPath(), position);
                break;

            case 2:
                onRenameFileClick(position);
                break;

            case 3:
                mFileUtils.printFile(file);
                break;

            case 4:
                mFileUtils.shareFile(file);
                break;

            case 5:
                mPDFUtils.showDetails(file);
                break;

            case 6:
                mPDFEncryptionUtils.setPassword(file.getPath(), ViewFilesAdapter.this, mFileList);
                break;

            case 7:
                mPDFEncryptionUtils.removePassword(file.getPath(), ViewFilesAdapter.this, mFileList);
                break;
        }
    }

    public void checkAll() {
        mSelectedFiles.clear();
        for (int i = 0; i < mFileList.size(); i++)
            mSelectedFiles.add(i);
        notifyDataSetChanged();
    }

    public void unCheckAll() {
        mSelectedFiles.clear();
        notifyDataSetChanged();
    }

    public ArrayList<String> getSelectedFilePath() {
        ArrayList<String> filePathList = new ArrayList<>();
        for (int position : mSelectedFiles) {
            if (mFileList.size() > position)
                filePathList.add(mFileList.get(position).getPath());
        }
        return filePathList;
    }

    @Override
    public int getItemCount() {
        return mFileList == null ? 0 : mFileList.size();
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    public void setData(ArrayList<File> pdfFiles) {
        mFileList = pdfFiles;
        notifyDataSetChanged();
    }

    public boolean areItemsSelected() {
        return mSelectedFiles.size() > 0;
    }

    private void deleteFile(String name, int position) {

        if (position < 0 || position >= mFileList.size())
            return;

        AtomicInteger undoClicked = new AtomicInteger();
        final File fdelete = new File(name);
        mFileList.remove(position);
        notifyDataSetChanged();
        getSnackbarwithAction(mActivity, R.string.snackbar_file_deleted)
                .setAction(R.string.snackbar_undoAction, v -> {
                    if (mFileList.size() == 0) {
                        mEmptyStateChangeListener.setEmptyStateInvisible();
                    }
                    updateDataset();
                    undoClicked.set(1);
                }).addCallback(new Snackbar.Callback() {
                    @Override
                        public void onDismissed(Snackbar snackbar, int event) {
                            if (undoClicked.get() == 0) {
                            fdelete.delete();
                            mDatabaseHelper.insertRecord(fdelete.getAbsolutePath(),
                                    mActivity.getString(R.string.deleted));
                            }
                    }
                }).show();
        if (mFileList.size() == 0)
            mEmptyStateChangeListener.setEmptyStateVisible();
    }

    public void deleteFiles() {

        for (int position : mSelectedFiles) {

            if (position >= mFileList.size())
                continue;

            String fileName = mFileList.get(position).getPath();
            File fdelete = new File(fileName);
            mDatabaseHelper.insertRecord(fdelete.getAbsolutePath(), mActivity.getString(R.string.deleted));
            if (fdelete.exists() && !fdelete.delete())
                showSnackbar(mActivity, R.string.snackbar_file_not_deleted);
        }

        ArrayList<File> newList = new ArrayList<>();
        for (int position = 0; position < mFileList.size(); position++)
            if (!mSelectedFiles.contains(position))
                newList.add(mFileList.get(position));

        mSelectedFiles.clear();
        if (newList.size() == 0)
            mEmptyStateChangeListener.setEmptyStateVisible();

        setData(newList);
    }

    public void shareFiles() {
        ArrayList<File> files = new ArrayList<>();
        for (int position : mSelectedFiles) {
            if (mFileList.size() > position)
                files.add(mFileList.get(position));
        }
        mFileUtils.shareMultipleFiles(files);
    }

    private void onRenameFileClick(final int position) {
        new MaterialDialog.Builder(mActivity)
                .title(R.string.creating_pdf)
                .content(R.string.enter_file_name)
                .input(mActivity.getString(R.string.example), null, (dialog, input) -> {
                    if (input == null || input.toString().trim().isEmpty())
                        showSnackbar(mActivity, R.string.snackbar_name_not_blank);
                    else {
                        if (!mFileUtils.isFileExist(input + mActivity.getString(R.string.pdf_ext))) {
                            renameFile(position, input.toString());
                        } else {
                            MaterialDialog.Builder builder = createOverwriteDialog(mActivity);
                            builder.onPositive((dialog2, which) -> renameFile(position, input.toString()))
                                    .onNegative((dialog1, which) -> onRenameFileClick(position))
                                    .show();
                        }
                    }
                }).show();
    }

    private void renameFile(int position, String newName) {
        File oldfile = mFileList.get(position);
        String oldPath = oldfile.getPath();
        String newfilename = oldPath.substring(0, oldPath.lastIndexOf('/'))
                + "/" + newName + mActivity.getString(R.string.pdf_ext);
        File newfile = new File(newfilename);
        if (oldfile.renameTo(newfile)) {
            showSnackbar(mActivity, R.string.snackbar_file_renamed);
            mFileList.set(position, newfile);
            notifyDataSetChanged();
            mDatabaseHelper.insertRecord(newfilename, mActivity.getString(R.string.renamed));
        } else
            showSnackbar(mActivity, R.string.snackbar_file_not_renamed);
    }

    @Override
    public void updateDataset() {
        int index = mSharedPreferences.getInt(SORTING_INDEX, NAME_INDEX);
        new PopulateList(this, this,
                new DirectoryUtils(mActivity), index).execute();
    }

    @Override
    public void setEmptyStateVisible() {

    }

    @Override
    public void setEmptyStateInvisible() {

    }

    @Override
    public void showNoPermissionsView() {

    }

    @Override
    public void hideNoPermissionsView() {

    }

    public class ViewFilesHolder extends RecyclerView.ViewHolder {

        @BindView(R.id.fileRipple)
        MaterialRippleLayout mRipple;
        @BindView(R.id.fileName)
        TextView mFilename;
        @BindView(R.id.checkbox)
        CheckBox checkBox;
        @BindView(R.id.fileDate)
        TextView mFiledate;
        @BindView(R.id.fileSize)
        TextView mFilesize;
        @BindView(R.id.encryptionImage)
        ImageView mEncryptionImage;

        ViewFilesHolder(View itemView, ItemSelectedListener itemSelectedListener) {
            super(itemView);
            ButterKnife.bind(this, itemView);
            checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    if (!mSelectedFiles.contains(getAdapterPosition())) {
                        mSelectedFiles.add(getAdapterPosition());
                        itemSelectedListener.isSelected(true, mSelectedFiles.size());
                    }
                } else
                    mSelectedFiles.remove(Integer.valueOf(getAdapterPosition()));
                itemSelectedListener.isSelected(false, mSelectedFiles.size());
            });
        }
    }
}