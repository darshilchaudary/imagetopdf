package example.createpdf.fragment;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.HashMap;

import butterknife.BindView;
import butterknife.ButterKnife;
import example.createpdf.R;
import example.createpdf.activity.MainActivity;
import example.createpdf.customviews.MyCardView;

import static example.createpdf.util.Constants.BUNDLE_DATA;
import static example.createpdf.util.Constants.PDF_TO_IMAGES;

public class HomeFragment extends Fragment implements View.OnClickListener {

    private Activity mActivity;
    @BindView(R.id.images_to_pdf)
    MyCardView imagesToPdf;
    @BindView(R.id.view_files)
    MyCardView viewFiles;
    @BindView(R.id.pdf_to_images)
    MyCardView mPdfToImages;

    private HashMap<Integer, Integer> mFragmentPositionMap;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootview = inflater.inflate(R.layout.fragment_home, container, false);
        ButterKnife.bind(this, rootview);
        fillMap();
        imagesToPdf.setOnClickListener(this);
        viewFiles.setOnClickListener(this);
        mPdfToImages.setOnClickListener(this);
        return rootview;
    }

    private void fillMap() {
        mFragmentPositionMap = new HashMap<>();
        mFragmentPositionMap.put(R.id.images_to_pdf, 1);
        mFragmentPositionMap.put(R.id.view_files, 3);
        mFragmentPositionMap.put(R.id.pdf_to_images, 11);

    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mActivity = (Activity) context;
    }

    private void setNavigationViewSelection(int index) {
        if (mActivity instanceof MainActivity)
            ((MainActivity) mActivity).setNavigationViewSelection(index);
    }

    @Override
    public void onClick(View v) {

        Fragment fragment = null;
        FragmentManager fragmentManager = getFragmentManager();
        Bundle bundle = new Bundle();
        highLightNavigationDrawerItem(v);

        switch (v.getId()) {
            case R.id.images_to_pdf:
                fragment = new ImageToPdfFragment();
                break;

            case R.id.pdf_to_images:
                fragment = new PdfToImageFragment();
                bundle.putString(BUNDLE_DATA, PDF_TO_IMAGES);
                fragment.setArguments(bundle);
                break;
            case R.id.view_files:
                fragment = new ViewFilesFragment();
                break;
        }

        try {
            if (fragment != null && fragmentManager != null)
                fragmentManager.beginTransaction().replace(R.id.content, fragment).commit();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private void highLightNavigationDrawerItem(View v) {
        if (mFragmentPositionMap.containsKey(v.getId())) {
            setNavigationViewSelection(mFragmentPositionMap.get(v.getId()));
        }
    }
}
