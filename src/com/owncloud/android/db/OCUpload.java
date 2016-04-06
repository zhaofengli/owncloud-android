/**
 *   ownCloud Android client application
 *
 *   @author LukeOwncloud
 *   @author masensio
 *   @author David A. Velasco
 *   Copyright (C) 2016 ownCloud Inc.
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License version 2,
 *   as published by the Free Software Foundation.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package com.owncloud.android.db;

import android.accounts.Account;
import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

import com.owncloud.android.authentication.AccountUtils;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.datamodel.UploadsStorageManager;
import com.owncloud.android.datamodel.UploadsStorageManager.UploadStatus;
import com.owncloud.android.files.services.FileUploader;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.operations.UploadFileOperation;
import com.owncloud.android.utils.MimetypeIconUtil;

import java.io.File;
import java.io.FileNotFoundException;

/**
 * Stores all information in order to start upload operations. PersistentUploadObject can
 * be stored persistently by {@link UploadsStorageManager}.
 * 
 */
public class OCUpload implements Parcelable {

    private static final String TAG = OCUpload.class.getSimpleName();

    private long mId;

    /**
     * Uri with the absolute path in the local file system to the file to be uploaded
     */
    private Uri mLocalUri;

    /**
     * Absolute path in the remote account to set to the uploaded file (not for its parent folder!)
     */
    private String mRemotePath;

    /**
     * Name of Owncloud account to upload file to.
     */
    private String mAccountName;

    /**
     * File size
     */
    private long mFileSize = 0;

    /**
     * Local action for upload. (0 - COPY, 1 - MOVE, 2 - FORGET)
     */
    private int mLocalAction;

    /**
     * Overwrite destination file?
     */
    private boolean mForceOverwrite;
    /**
     * Create destination folder?
     */
    private boolean mIsCreateRemoteFolder;
    /**
     * Status of upload (later, in_progress, ...).
     */
    private UploadStatus mUploadStatus;
    /**
     * Result from last upload operation. Can be null.
     */
    private UploadResult mLastResult;

    /**
     * Defines the origin of the upload; see constants CREATED_ in {@link UploadFileOperation}
     */
    private int mCreatedBy;

    /*
     * When the upload ended
     */
    private long mUploadEndTimeStamp;


    /**
     * Main constructor
     *
     * @param localPath         Absolute path in the local file system to the file to be uploaded.
     * @param remotePath        Absolute path in the remote account to set to the uploaded file.
     * @param accountName       Name of an ownCloud account to update the file to.
     */
    public OCUpload(String localPath, String remotePath, String accountName) {

        if (localPath == null) {
            throw new IllegalArgumentException("Local path must not be null");
        }
        setLocalUri(localPath);

        if (!mLocalUri.getLastPathSegment().startsWith(File.separator)) {
            throw new IllegalArgumentException("Local path must be an absolute path in the local file system");
        }
        if (remotePath == null || !remotePath.startsWith(OCFile.PATH_SEPARATOR)) {
            throw new IllegalArgumentException("Remote path must be an absolute path in the local file system");
        }
        if (accountName == null || accountName.length() < 1) {
            throw new IllegalArgumentException("Invalid account name");
        }
        resetData();

        mRemotePath = remotePath;
        mAccountName = accountName;
    }


    /**
     * Convenience constructor to reupload already existing {@link OCFile}s
     *
     * @param  ocFile           {@link OCFile} instance to update in the remote server.
     * @param  account          ownCloud {@link Account} where ocFile is contained.
     */
    public OCUpload(OCFile ocFile, Account account) {
        this(ocFile.getStoragePath(), ocFile.getRemotePath(), account.name);
    }


    /**
     * Reset all the fields to default values.
     */
    private void resetData() {
        mRemotePath = "";
        mLocalUri = null;
        mAccountName = "";
        mFileSize = -1;
        mId = -1;
        mLocalAction = FileUploader.LOCAL_BEHAVIOUR_COPY;
        mForceOverwrite = false;
        mIsCreateRemoteFolder = false;
        mUploadStatus = UploadStatus.UPLOAD_IN_PROGRESS;
        mLastResult = UploadResult.UNKNOWN;
        mCreatedBy = UploadFileOperation.CREATED_BY_USER;
    }

    // Getters & Setters
    public void setUploadId(long id) {
        mId = id;
    }
    public long getUploadId() {
        return mId;
    }

    /**
     * @return the uploadStatus
     */
    public UploadStatus getUploadStatus() {
        return mUploadStatus;
    }

    /**
     * Sets uploadStatus AND SETS lastResult = null;
     * @param uploadStatus the uploadStatus to set
     */
    public void setUploadStatus(UploadStatus uploadStatus) {
        this.mUploadStatus = uploadStatus;
        setLastResult(UploadResult.UNKNOWN);
    }

    /**
     * @return the lastResult
     */
    public UploadResult getLastResult() {
        return mLastResult;
    }

    /**
     * @param lastResult the lastResult to set
     */
    public void setLastResult(UploadResult lastResult) {
        this.mLastResult = ((lastResult != null) ? lastResult : UploadResult.UNKNOWN);
    }


    /**
     * @return the localPath
     */
    public Uri getLocalUri() {
        return mLocalUri;
    }

    public void setLocalUri(Uri localUri) {
        mLocalUri = localUri;
    }

    public void setLocalUri(String localPath) {
        mLocalUri = Uri.parse(localPath);
        if (mLocalUri.getScheme() == null) {
            mLocalUri = Uri.parse(ContentResolver.SCHEME_FILE + "://" + localPath);
        }
    }

    /**
     * @return the remotePath
     */
    public String getRemotePath() {
        return mRemotePath;
    }

    /**
     * @param remotePath
     */
    public void setRemotePath(String remotePath) {
        mRemotePath = remotePath;
    }


    /**
     * @return  File size
     */
    public long getFileSize() {
        return mFileSize;
    }

    public void setFileSize(long fileSize) {
        mFileSize = fileSize;
    }


    /**
     * @return the mimeType
     */
    public String getMimeType() {
        if(mLocalUri != null) {
            return MimetypeIconUtil.getBestMimeTypeByFilename(mLocalUri.getLastPathSegment());
        }
        return null;
    }

    /**
     * @return the localAction
     */
    public int getLocalAction() {
        return mLocalAction;
    }

    /**
     * @param localAction the localAction to set
     */
    public void setLocalAction(int localAction) {
        this.mLocalAction = localAction;
    }

    /**
     * @return the forceOverwrite
     */
    public boolean isForceOverwrite() {
        return mForceOverwrite;
    }

    /**
     * @param forceOverwrite the forceOverwrite to set
     */
    public void setForceOverwrite(boolean forceOverwrite) {
        this.mForceOverwrite = forceOverwrite;
    }

    /**
     * @return the isCreateRemoteFolder
     */
    public boolean isCreateRemoteFolder() {
        return mIsCreateRemoteFolder;
    }

    /**
     * @param isCreateRemoteFolder the isCreateRemoteFolder to set
     */
    public void setCreateRemoteFolder(boolean isCreateRemoteFolder) {
        this.mIsCreateRemoteFolder = isCreateRemoteFolder;
    }

    /**
     * @return the accountName
     */
    public String getAccountName() {
        return mAccountName;
    }

    /**
     * Returns owncloud account as {@link Account} object.  
     */
    public Account getAccount(Context context) {
        return AccountUtils.getOwnCloudAccountByName(context, getAccountName());
    }

    public void setCreatedBy(int createdBy) {
        mCreatedBy = createdBy;
    }

    public int getCreadtedBy() {
        return mCreatedBy;
    }

    public void setUploadEndTimestamp(long uploadEndTimestamp) {
        mUploadEndTimeStamp = uploadEndTimestamp;
    }

    public long getUploadEndTimestamp(){
        return mUploadEndTimeStamp;
    }

    /**
     * For debugging purposes only.
     */
    public String toFormattedString() {
        try {
            String localPath = getLocalUri() != null ? String.valueOf(getLocalUri()) : "";
            return localPath + " status:" + getUploadStatus() + " result:" +
                    (getLastResult() == null ? "null" : getLastResult().getValue());
        } catch (NullPointerException e){
            Log_OC.d(TAG, "Exception " + e.toString() );
            return (e.toString());
        }
    }

    /**
     * Returns true when user is able to cancel this upload. That is, when
     * upload is currently in progress or scheduled for upload.
     */
    public  boolean userCanCancelUpload() {
        if (getUploadStatus() == UploadStatus.UPLOAD_IN_PROGRESS) {
            return true;
        }
        return false;
    }

    /**
     * Returns true when user can choose to retry this upload. That is, when
     * upload has failed for any reason.
     */
    public boolean userCanRetryUpload() {
        if (getUploadStatus() == UploadStatus.UPLOAD_FAILED) {
            return true;
        }
        return false;
    }


    /****
     *
     */
    public static final Parcelable.Creator<OCUpload> CREATOR = new Parcelable.Creator<OCUpload>() {

        @Override
        public OCUpload createFromParcel(Parcel source) {
            return new OCUpload(source);
        }

        @Override
        public OCUpload[] newArray(int size) {
            return new OCUpload[size];
        }
    };

    /**
     * Reconstruct from parcel
     *
     * @param source The source parcel
     */
    protected OCUpload(Parcel source) {
        readFromParcel(source);
    }

    public void readFromParcel(Parcel source) {
        mId = source.readLong();
        mLocalUri = Uri.parse(source.readString());
        mRemotePath = source.readString();
        mAccountName = source.readString();
        mLocalAction = source.readInt();
        mForceOverwrite = (source.readInt() == 1);
        mIsCreateRemoteFolder = (source.readInt() == 1);
        try {
            mUploadStatus = UploadStatus.valueOf(source.readString());
        } catch (IllegalArgumentException x) {
            mUploadStatus = UploadStatus.UPLOAD_IN_PROGRESS;
        }
        mUploadEndTimeStamp = source.readLong();
        try {
            mLastResult = UploadResult.valueOf(source.readString());
        } catch (IllegalArgumentException x) {
            mLastResult = UploadResult.UNKNOWN;
        }
        mCreatedBy = source.readInt();
    }


    @Override
    public int describeContents() {
        return this.hashCode();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(mId);
        dest.writeString(String.valueOf(mLocalUri));
        dest.writeString(mRemotePath);
        dest.writeString(mAccountName);
        dest.writeInt(mLocalAction);
        dest.writeInt(mForceOverwrite ? 1 : 0);
        dest.writeInt(mIsCreateRemoteFolder ? 1 : 0);
        dest.writeString(mUploadStatus.name());
        dest.writeLong(mUploadEndTimeStamp);
        dest.writeString(((mLastResult == null) ? "" : mLastResult.name()));
        dest.writeInt(mCreatedBy);
    }

    enum CanUploadFileNowStatus {NOW, LATER, FILE_GONE, ERROR};

}
