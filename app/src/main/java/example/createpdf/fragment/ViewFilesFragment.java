package example.createpdf.fragment;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.constraint.ConstraintLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RelativeLayout;

import java.io.File;
import java.util.ArrayList;
import java.util.Objects;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import example.createpdf.R;
import example.createpdf.activity.MainActivity;
import example.createpdf.adapter.ViewFilesAdapter;
import example.createpdf.interfaces.EmptyStateChangeListener;
import example.createpdf.interfaces.ItemSelectedListener;
import example.createpdf.util.DirectoryUtils;
import example.createpdf.util.FileSortUtils;
import example.createpdf.util.MergeHelper;
import example.createpdf.util.PopulateList;
import example.createpdf.util.ViewFilesDividerItemDecoration;

import static example.createpdf.util.Constants.BUNDLE_DATA;
import static example.createpdf.util.Constants.SORTING_INDEX;
import static example.createpdf.util.Constants.appName;
import static example.createpdf.util.DialogUtils.showFilesInfoDialog;
import static example.createpdf.util.FileSortUtils.NAME_INDEX;
import static example.createpdf.util.StringUtils.showSnackbar;

public class ViewFilesFragment extends Fragment
        implements SwipeRefreshLayout.OnRefreshListener,
        EmptyStateChangeListener,
        ItemSelectedListener {

    private static final int PERMISSION_REQUEST_WRITE_EXTERNAL_STORAGE_RESULT = 10;

    @BindView(R.id.getStarted)
    public Button getStarted;
    @BindView(R.id.filesRecyclerView)
    RecyclerView mViewFilesListRecyclerView;
    @BindView(R.id.swipe)
    SwipeRefreshLayout mSwipeView;
    @BindView(R.id.emptyStatusView)
    ConstraintLayout emptyView;
    @BindView(R.id.no_permissions_view)
    RelativeLayout noPermissionsLayout;

    private MenuItem mMenuItem;
    private Activity mActivity;
    private ViewFilesAdapter mViewFilesAdapter;

    private DirectoryUtils mDirectoryUtils;
    private SearchView mSearchView;
    private int mCurrentSortingIndex;
    private SharedPreferences mSharedPreferences;
    private boolean mIsChecked = false;
    private boolean mCheckBoxChanged = false;
    private AlertDialog.Builder mAlertDialogBuilder;

    private int mCountFiles;
    private MergeHelper mMergeHelper;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View root = inflater.inflate(R.layout.fragment_view_files, container, false);
        ButterKnife.bind(this, root);
        // Initialize variables
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(mActivity);
        mCurrentSortingIndex = mSharedPreferences.getInt(SORTING_INDEX, NAME_INDEX);
        mViewFilesAdapter = new ViewFilesAdapter(mActivity, null, this, this);
        mAlertDialogBuilder = new AlertDialog.Builder(mActivity)
                .setCancelable(true)
                .setNegativeButton(R.string.cancel, (dialogInterface, i) -> dialogInterface.dismiss());

        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(root.getContext());
        mViewFilesListRecyclerView.setLayoutManager(mLayoutManager);
        mViewFilesListRecyclerView.setAdapter(mViewFilesAdapter);
        mViewFilesListRecyclerView.addItemDecoration(new ViewFilesDividerItemDecoration(root.getContext()));
        mSwipeView.setOnRefreshListener(this);

        int dialogId;
        if (getArguments() != null) {
            dialogId = getArguments().getInt(BUNDLE_DATA);
            showFilesInfoDialog(mActivity, dialogId);
        }

        checkIfListEmpty();
        mMergeHelper = new MergeHelper(mActivity, mViewFilesAdapter);
        return root;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        if (!mCheckBoxChanged) {
            inflater.inflate(R.menu.activity_view_files_actions, menu);
            MenuItem item = menu.findItem(R.id.action_search);
            mMenuItem = menu.findItem(R.id.select_all);
            mSearchView = (SearchView) item.getActionView();
            mSearchView.setQueryHint(getString(R.string.search_hint));
            mSearchView.setSubmitButtonEnabled(true);
            mSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                @Override
                public boolean onQueryTextSubmit(String s) {
                    setDataForQueryChange(s);
                    mSearchView.clearFocus();
                    return true;
                }

                @Override
                public boolean onQueryTextChange(String s) {
                    setDataForQueryChange(s);
                    return true;
                }
            });
            mSearchView.setOnCloseListener(() -> {
                populatePdfList();
                return false;
            });
            mSearchView.setIconifiedByDefault(true);
        } else {
            inflater.inflate(R.menu.activity_view_files_actions_if_selected, menu);
            MenuItem item = menu.findItem(R.id.item_merge);
            item.setVisible(mCountFiles > 1); // Show Merge icon when two or more files was selected
        }
    }

    private void setDataForQueryChange(String s) {
        ArrayList<File> searchResult = mDirectoryUtils.searchPDF(s);
        mViewFilesAdapter.setData(searchResult);
        mViewFilesListRecyclerView.setAdapter(mViewFilesAdapter);
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
            case R.id.item_sort:
                displaySortDialog();
                break;
            case R.id.item_delete:
                if (mViewFilesAdapter.areItemsSelected())
                    deleteFiles();
                else
                    showSnackbar(mActivity, R.string.snackbar_no_pdfs_selected);
                break;
            case R.id.item_share:
                if (mViewFilesAdapter.areItemsSelected())
                    mViewFilesAdapter.shareFiles();
                else
                    showSnackbar(mActivity, R.string.snackbar_no_pdfs_selected);
                break;
            case R.id.select_all:
                if (mViewFilesAdapter.getItemCount() > 0) {
                    if (mIsChecked) {
                        mViewFilesAdapter.unCheckAll();
                        mMenuItem.setIcon(R.drawable.ic_check_box_outline_blank_24dp);
                    } else {
                        mViewFilesAdapter.checkAll();
                        mMenuItem.setIcon(R.drawable.ic_check_box_24dp);
                    }
                    mIsChecked = !mIsChecked;
                } else {
                    showSnackbar(mActivity, R.string.snackbar_no_pdfs_selected);
                }
                break;
            case R.id.item_merge:
                if (mViewFilesAdapter.getItemCount() > 1) {
                    mMergeHelper.mergeFiles();
                }
                break;
        }
        return true;
    }


    private void deleteFiles() {
        AlertDialog.Builder dialogAlert = new AlertDialog.Builder(mActivity)
                .setCancelable(true)
                .setNegativeButton(R.string.cancel, (dialogInterface, i) -> dialogInterface.dismiss())
                .setTitle(R.string.delete_alert)
                .setPositiveButton(R.string.yes, (dialog, which) -> {
                    mViewFilesAdapter.deleteFiles();
                    checkIfListEmpty();
                });
        dialogAlert.create().show();
    }

    private void checkIfListEmpty() {
        onRefresh();
        final File[] files = mDirectoryUtils.getOrCreatePdfDirectory().listFiles();
        int count = 0;

        if (files == null) {
            showNoPermissionsView();
            return;
        }

        for (File file : files)
            if (!file.isDirectory()) {
                count++;
                break;
            }
        if (count == 0)
            setEmptyStateVisible();
    }

    @Override
    public void onRefresh() {
        populatePdfList();
        mSwipeView.setRefreshing(false);
    }

    private void populatePdfList() {
        new PopulateList(mViewFilesAdapter, this,
                new DirectoryUtils(mActivity), mCurrentSortingIndex).execute();
    }

    private void displaySortDialog() {
        mAlertDialogBuilder.setTitle(R.string.sort_by_title)
                .setItems(R.array.sort_options, (dialog, which) -> {
                    ArrayList<File> allPdfs = mDirectoryUtils.getPdfFromOtherDirectories();
                    FileSortUtils.performSortOperation(which, allPdfs);
                    mViewFilesAdapter.setData(allPdfs);
                    mCurrentSortingIndex = which;
                    mSharedPreferences.edit().putInt(SORTING_INDEX, which).apply();
                });
        mAlertDialogBuilder.create().show();
    }

    @Override
    public void setEmptyStateVisible() {
        emptyView.setVisibility(View.VISIBLE);
        noPermissionsLayout.setVisibility(View.GONE);
    }

    @Override
    public void setEmptyStateInvisible() {
        emptyView.setVisibility(View.GONE);
        noPermissionsLayout.setVisibility(View.GONE);
    }

    @Override
    public void showNoPermissionsView() {
        emptyView.setVisibility(View.GONE);
        noPermissionsLayout.setVisibility(View.VISIBLE);
    }

    @Override
    public void hideNoPermissionsView() {
        noPermissionsLayout.setVisibility(View.GONE);
    }

    @OnClick(R.id.getStarted)
    public void loadHome() {
        Fragment fragment = new ImageToPdfFragment();
        FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
        fragmentManager.beginTransaction().replace(R.id.content, fragment).commit();
        if (mActivity instanceof MainActivity) {
            ((MainActivity) mActivity).setDefaultMenuSelected(0);
        }
    }

    @OnClick(R.id.provide_permissions)
    public void providePermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(new String[]{
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.CAMERA},
                    PERMISSION_REQUEST_WRITE_EXTERNAL_STORAGE_RESULT);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {

        if (grantResults.length < 1) {
            showSnackbar(mActivity, R.string.snackbar_insufficient_permissions);
            return;
        }
        switch (requestCode) {
            case PERMISSION_REQUEST_WRITE_EXTERNAL_STORAGE_RESULT: {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    showSnackbar(mActivity, R.string.snackbar_permissions_given);
                    onRefresh();
                } else
                    showSnackbar(mActivity, R.string.snackbar_insufficient_permissions);
            }
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mActivity = (Activity) context;
        mDirectoryUtils = new DirectoryUtils(mActivity);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void isSelected(Boolean isSelected, int countFiles) {
        AppCompatActivity activity = ((AppCompatActivity)
                Objects.requireNonNull(mActivity));
        ActionBar toolbar = activity.getSupportActionBar();
        mCountFiles = countFiles;
        if (toolbar != null) {
            if (countFiles == 0) {
                toolbar.setTitle(appName);
                if (mCheckBoxChanged) {
                    mCheckBoxChanged = false;
                    mIsChecked = false;
                    activity.invalidateOptionsMenu();
                }
            } else {
                toolbar.setTitle(String.valueOf(countFiles));
                if (!mCheckBoxChanged) {
                    mCheckBoxChanged = true;
                    mIsChecked = true;
                    activity.invalidateOptionsMenu();
                }
                if (countFiles == 1 || countFiles == 2)
                    activity.invalidateOptionsMenu();
            }
        }
    }

}