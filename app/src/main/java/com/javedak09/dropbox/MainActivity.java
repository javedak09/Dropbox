package com.javedak09.dropbox;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import java.io.IOException;
import java.util.List;

import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

import com.dropbox.sync.android.DbxAccount;
import com.dropbox.sync.android.DbxAccountManager;
import com.dropbox.sync.android.DbxFile;
import com.dropbox.sync.android.DbxFileInfo;
import com.dropbox.sync.android.DbxFileSystem;
import com.dropbox.sync.android.DbxPath;


public class MainActivity extends Activity {

    final static private String APP_KEY = "wcw6knr2a4j7qld";
    final static private String APP_SECRET = "1yz84nsarzeaydx";


    private static final int REQUEST_LINK_TO_DBX = 0;

    private DbxAccountManager mDbxAcctMgr;


    @BindView(R.id.test_output)
    TextView mTestOutput;

    @BindView(R.id.link_button)
    Button mLinkButton;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);


        mLinkButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                onClickLinkToDropbox();
            }
        });
        mDbxAcctMgr = DbxAccountManager.getInstance(getApplicationContext(), APP_KEY, APP_SECRET);

    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mDbxAcctMgr.hasLinkedAccount()) {
            showLinkedView();
            doDropboxTest();
        } else {
            showUnlinkedView();
        }
    }


    private void showLinkedView() {
        mLinkButton.setVisibility(View.GONE);
        mTestOutput.setVisibility(View.VISIBLE);
    }

    private void showUnlinkedView() {
        mLinkButton.setVisibility(View.VISIBLE);
        mTestOutput.setVisibility(View.GONE);
    }

    private void onClickLinkToDropbox() {
        mDbxAcctMgr.startLink((Activity) this, REQUEST_LINK_TO_DBX);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_LINK_TO_DBX) {
            if (resultCode == Activity.RESULT_OK) {
                doDropboxTest();
            } else {
                mTestOutput.setText("Link to Dropbox failed or was cancelled.");
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void doDropboxTest() {
        mTestOutput.setText("Dropbox Sync API Version " + DbxAccountManager.SDK_VERSION_NAME + "\n");
        try {
            final String TEST_DATA = "Hello Dropbox";
            final String TEST_FILE_NAME = "hello_dropbox.txt";
            DbxPath testPath = new DbxPath(DbxPath.ROOT, TEST_FILE_NAME);

            // Create DbxFileSystem for synchronized file access.
            DbxFileSystem dbxFs = DbxFileSystem.forAccount(mDbxAcctMgr.getLinkedAccount());

            // Print the contents of the root folder.  This will block until we can
            // sync metadata the first time.
            List<DbxFileInfo> infos = dbxFs.listFolder(DbxPath.ROOT);
            mTestOutput.append("\nContents of app folder:\n");
            for (DbxFileInfo info : infos) {
                mTestOutput.append("    " + info.path + ", " + info.modifiedTime + '\n');
            }

            // Create a test file only if it doesn't already exist.
            if (!dbxFs.exists(testPath)) {
                DbxFile testFile = dbxFs.create(testPath);
                try {
                    testFile.writeString(TEST_DATA);
                } finally {
                    testFile.close();
                }
                mTestOutput.append("\nCreated new file '" + testPath + "'.\n");
            }

            // Read and print the contents of test file.  Since we're not making
            // any attempt to wait for the latest version, this may print an
            // older cached version.  Use getSyncStatus() and/or a listener to
            // check for a new version.
            if (dbxFs.isFile(testPath)) {
                String resultData;
                DbxFile testFile = dbxFs.open(testPath);
                try {
                    resultData = testFile.readString();
                } finally {
                    testFile.close();
                }
                mTestOutput.append("\nRead file '" + testPath + "' and got data:\n    " + resultData);
            } else if (dbxFs.isFolder(testPath)) {
                mTestOutput.append("'" + testPath.toString() + "' is a folder.\n");
            }
        } catch (IOException e) {
            mTestOutput.setText("Dropbox test failed: " + e);
        }
    }
}