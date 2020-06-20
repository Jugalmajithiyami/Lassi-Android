package com.lassi.data.mediadirectory

import android.os.Parcel
import android.os.Parcelable

data class FolderDetail(var folderName: String?, var placeHolder: String?) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString(),
        parcel.readString()
    ) {
    }

    override fun writeToParcel(dest: Parcel?, flags: Int) {
        TODO("Not yet implemented")
    }

    override fun describeContents(): Int {
        TODO("Not yet implemented")
    }

    companion object CREATOR : Parcelable.Creator<FolderDetail> {
        override fun createFromParcel(parcel: Parcel): FolderDetail {
            return FolderDetail(parcel)
        }

        override fun newArray(size: Int): Array<FolderDetail?> {
            return arrayOfNulls(size)
        }
    }
}