package com.winlator.cmod;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.widget.SwitchCompat;
import android.net.Uri;
import android.app.Activity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.winlator.cmod.steam.SteamLoginActivity;
import com.winlator.cmod.steam.service.SteamService;
import com.winlator.cmod.steam.utils.PrefManager;
import com.winlator.cmod.core.FileUtils;

/**
 * Fragment showing store sign-in / sign-out for Steam, Epic, GOG, Amazon.
 */
public class StoresFragment extends Fragment {
    
    private LinearLayout layout;
    private static final int REQUEST_CODE_DEFAULT_PATH = 2001;
    private static final int REQUEST_CODE_STEAM_PATH = 2002;
    private static final int REQUEST_CODE_EPIC_PATH = 2003;
    private static final int REQUEST_CODE_GOG_PATH = 2004;
    private static final int REQUEST_CODE_AMAZON_PATH = 2005;


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (getActivity() != null && ((AppCompatActivity) getActivity()).getSupportActionBar() != null) {
            ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle(R.string.stores);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        PrefManager.INSTANCE.init(getContext());
        this.layout = new LinearLayout(getContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        int pad = dpToPx(20);
        layout.setPadding(pad, pad, pad, pad);

        android.widget.ScrollView scrollView = new android.widget.ScrollView(getContext());
        scrollView.addView(layout);
        return scrollView;
    }

    private void updateStores() {
        if (layout == null) return;
        layout.removeAllViews();

        addStoreRow(layout, "Steam",
                SteamService.Companion.isLoggedIn(),
                v -> {
                    if (SteamService.Companion.isLoggedIn()) {
                        SteamService.Companion.logOut();
                        updateStores();
                    } else {
                        startActivity(new Intent(getContext(), SteamLoginActivity.class));
                    }
                });

        boolean isEpicLoggedIn = com.winlator.cmod.epic.service.EpicAuthManager.isLoggedIn(getContext());
        addStoreRow(layout, "Epic Games", isEpicLoggedIn, v -> {
            if (isEpicLoggedIn) {
                com.winlator.cmod.epic.service.EpicAuthManager.logoutSync(getContext());
                updateStores();
            } else {
                startActivity(new Intent(getContext(), com.winlator.cmod.epic.ui.auth.EpicOAuthActivity.class));
            }
        });

        addStoreRow(layout, "GOG", false, v ->
                android.widget.Toast.makeText(getContext(), "GOG integration coming soon", android.widget.Toast.LENGTH_SHORT).show());

        addStoreRow(layout, "Amazon Games", false, v ->
                android.widget.Toast.makeText(getContext(), "Amazon Games integration coming soon", android.widget.Toast.LENGTH_SHORT).show());

        // Downloads section
        addHeader(layout, "DOWNLOADS");

        addToggleRow(layout, "Wi-Fi Only Downloads", PrefManager.INSTANCE.getDownloadOnWifiOnly(), (buttonView, isChecked) -> {
            PrefManager.INSTANCE.setDownloadOnWifiOnly(isChecked);
        });

        addToggleRow(layout, "Shared Downloads Folder", PrefManager.INSTANCE.getUseSingleDownloadFolder(), (buttonView, isChecked) -> {
            PrefManager.INSTANCE.setUseSingleDownloadFolder(isChecked);
            updateStores();
        });

        if (PrefManager.INSTANCE.getUseSingleDownloadFolder()) {
            addPathRow(layout, "Default Downloads Folder", PrefManager.INSTANCE.getDefaultDownloadFolder(), REQUEST_CODE_DEFAULT_PATH);
        } else {
            addPathRow(layout, "Steam Store Downloads Folder", PrefManager.INSTANCE.getSteamDownloadFolder(), REQUEST_CODE_STEAM_PATH);
            addPathRow(layout, "Epic Store Downloads Folder", PrefManager.INSTANCE.getEpicDownloadFolder(), REQUEST_CODE_EPIC_PATH);
            addPathRow(layout, "GOG Store Downloads Folder", PrefManager.INSTANCE.getGogDownloadFolder(), REQUEST_CODE_GOG_PATH);
            addPathRow(layout, "Amazon Store Downloads Folder", PrefManager.INSTANCE.getAmazonDownloadFolder(), REQUEST_CODE_AMAZON_PATH);
        }
    }

    private void addHeader(LinearLayout parent, String title) {
        TextView header = new TextView(getContext());
        header.setText(title);
        header.setTextSize(12);
        header.setTextColor(0xFF8B949E);
        header.setPadding(dpToPx(4), dpToPx(24), dpToPx(4), dpToPx(8));
        parent.addView(header);
    }

    private void addToggleRow(LinearLayout parent, String title, boolean isChecked, android.widget.CompoundButton.OnCheckedChangeListener listener) {
        LinearLayout row = new LinearLayout(getContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(dpToPx(4), dpToPx(12), dpToPx(4), dpToPx(12));
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);

        TextView nameView = new TextView(getContext());
        nameView.setText(title);
        nameView.setTextSize(14);
        nameView.setTextColor(0xFFE6EDF3);
        LinearLayout.LayoutParams nameParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        nameView.setLayoutParams(nameParams);
        row.addView(nameView);

        SwitchCompat switchView = new SwitchCompat(getContext());
        switchView.setChecked(isChecked);
        switchView.setOnCheckedChangeListener(listener);
        row.addView(switchView);

        parent.addView(row);

        View divider = new View(getContext());
        divider.setBackgroundColor(0xFF30363D);
        parent.addView(divider, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1));
    }

    private void addPathRow(LinearLayout parent, String title, String currentPath, int requestCode) {
        LinearLayout container = new LinearLayout(getContext());
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(dpToPx(4), dpToPx(12), dpToPx(4), dpToPx(12));

        TextView titleView = new TextView(getContext());
        titleView.setText(title);
        titleView.setTextSize(14);
        titleView.setTextColor(0xFFE6EDF3);
        container.addView(titleView);

        LinearLayout row = new LinearLayout(getContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);
        row.setPadding(0, dpToPx(4), 0, 0);

        TextView pathView = new TextView(getContext());
        String displayPath = currentPath;
        if (displayPath == null || displayPath.isEmpty()) {
            displayPath = "Not set";
        } else {
            try {
                Uri uri = Uri.parse(displayPath);
                String decodedPath = FileUtils.getFilePathFromUri(getContext(), uri);
                if (decodedPath != null) displayPath = decodedPath;
            } catch (Exception e) {}
        }
        pathView.setText(displayPath);
        pathView.setTextSize(12);
        pathView.setTextColor(0xFF8B949E);
        LinearLayout.LayoutParams pathParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        pathView.setLayoutParams(pathParams);
        row.addView(pathView);

        TextView actionBtn = new TextView(getContext());
        actionBtn.setText("INSTALL"); // As requested: "By setting the default folder by pressing ( Install )"
        actionBtn.setTextSize(12);
        actionBtn.setTextColor(0xFF57CBDE);
        actionBtn.setPadding(dpToPx(12), dpToPx(6), dpToPx(12), dpToPx(6));
        actionBtn.setBackgroundColor(0x2057CBDE);
        actionBtn.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
            startActivityForResult(intent, requestCode);
        });
        row.addView(actionBtn);

        container.addView(row);
        parent.addView(container);

        View divider = new View(getContext());
        divider.setBackgroundColor(0xFF30363D);
        parent.addView(divider, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1));
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK && data != null) {
            Uri uri = data.getData();
            if (uri != null) {
                try {
                    getContext().getContentResolver().takePersistableUriPermission(
                            uri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    );
                } catch (SecurityException e) {}

                String uriString = uri.toString();
                switch (requestCode) {
                    case REQUEST_CODE_DEFAULT_PATH: PrefManager.INSTANCE.setDefaultDownloadFolder(uriString); break;
                    case REQUEST_CODE_STEAM_PATH: PrefManager.INSTANCE.setSteamDownloadFolder(uriString); break;
                    case REQUEST_CODE_EPIC_PATH: PrefManager.INSTANCE.setEpicDownloadFolder(uriString); break;
                    case REQUEST_CODE_GOG_PATH: PrefManager.INSTANCE.setGogDownloadFolder(uriString); break;
                    case REQUEST_CODE_AMAZON_PATH: PrefManager.INSTANCE.setAmazonDownloadFolder(uriString); break;
                }
                updateStores();
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        updateStores();
    }

    private void refreshView() {
        if (getView() != null) {
            getParentFragmentManager().beginTransaction()
                    .detach(this)
                    .attach(this)
                    .commit();
        }
    }

    private void addStoreRow(LinearLayout parent, String storeName, boolean isLoggedIn, View.OnClickListener onClick) {
        LinearLayout row = new LinearLayout(getContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(dpToPx(4), dpToPx(12), dpToPx(4), dpToPx(12));
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);

        // Store name
        TextView nameView = new TextView(getContext());
        nameView.setText(storeName);
        nameView.setTextSize(15);
        nameView.setTextColor(0xFFE6EDF3);
        LinearLayout.LayoutParams nameParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        nameView.setLayoutParams(nameParams);
        row.addView(nameView);

        // Status dot + text
        TextView statusView = new TextView(getContext());
        statusView.setText(isLoggedIn ? "● Signed In" : "○ Not Signed In");
        statusView.setTextSize(12);
        statusView.setTextColor(isLoggedIn ? 0xFF57CBDE : 0xFF8B949E);
        statusView.setPadding(0, 0, dpToPx(12), 0);
        row.addView(statusView);

        // Action button
        TextView actionBtn = new TextView(getContext());
        actionBtn.setText(isLoggedIn ? "SIGN OUT" : "SIGN IN");
        actionBtn.setTextSize(12);
        actionBtn.setTextColor(isLoggedIn ? 0xFFFF6B6B : 0xFF57CBDE);
        actionBtn.setPadding(dpToPx(12), dpToPx(6), dpToPx(12), dpToPx(6));
        actionBtn.setBackgroundColor(isLoggedIn ? 0x20FF6B6B : 0x2057CBDE);
        actionBtn.setOnClickListener(onClick);
        row.addView(actionBtn);

        parent.addView(row);

        // Thin divider
        View divider = new View(getContext());
        divider.setBackgroundColor(0xFF30363D);
        parent.addView(divider, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 1));
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }
}
