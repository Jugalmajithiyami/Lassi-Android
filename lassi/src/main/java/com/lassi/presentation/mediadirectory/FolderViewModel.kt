package com.lassi.presentation.mediadirectory

import androidx.lifecycle.MutableLiveData
import com.lassi.data.common.Response
import com.lassi.data.mediadirectory.Folder
import com.lassi.data.mediadirectory.FolderDetail
import com.lassi.domain.media.MediaRepository
import com.lassi.presentation.common.LassiBaseViewModel
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers

class FolderViewModel(
    private val mediaRepository: MediaRepository
) : LassiBaseViewModel() {
    var fetchMediaFolderLiveData = MutableLiveData<Response<ArrayList<FolderDetail>>>()

    fun fetchFolders() {
        fetchMediaFolderLiveData.value = Response.Loading()
//        mediaRepository.fetchFolders()
        mediaRepository.fetchFolderList()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                fetchMediaFolderLiveData.value = Response.Success(it)
            }, {
                fetchMediaFolderLiveData.value = Response.Error(it)
            }).collect()
    }
}