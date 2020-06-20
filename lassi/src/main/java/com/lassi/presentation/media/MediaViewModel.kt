package com.lassi.presentation.media

import androidx.lifecycle.MutableLiveData
import com.lassi.data.common.Response
import com.lassi.data.media.MiMedia
import com.lassi.data.mediadirectory.Folder
import com.lassi.data.mediadirectory.FolderDetail
import com.lassi.domain.media.MediaRepository
import com.lassi.presentation.common.LassiBaseViewModel
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers

class MediaViewModel(private val mediaRepository: MediaRepository) : LassiBaseViewModel() {
    var fetchMediaFromFolderLiveData = MutableLiveData<Response<ArrayList<MiMedia>>>()

    fun fetchMediaFromFolders(folderName : String?) {
        fetchMediaFromFolderLiveData.value = Response.Loading()
        folderName?.let { it ->
            mediaRepository.getAllMediaFromFolder(it)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    fetchMediaFromFolderLiveData.value = Response.Success(it)
                }, {
                    fetchMediaFromFolderLiveData.value = Response.Error(it)
                }).collect()
        }
    }
}