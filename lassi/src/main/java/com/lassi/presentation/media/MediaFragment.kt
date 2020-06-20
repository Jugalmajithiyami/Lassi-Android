package com.lassi.presentation.media

import android.graphics.PorterDuff
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.Menu
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.GridLayoutManager
import com.lassi.R
import com.lassi.common.extenstions.hide
import com.lassi.common.extenstions.show
import com.lassi.common.utils.CropUtils
import com.lassi.common.utils.KeyUtils.SELECTED_FOLDER
import com.lassi.data.common.Response
import com.lassi.data.media.MiMedia
import com.lassi.data.mediadirectory.FolderDetail
import com.lassi.domain.common.SafeObserver
import com.lassi.domain.media.LassiConfig
import com.lassi.domain.media.MediaType
import com.lassi.presentation.common.LassiBaseViewModelFragment
import com.lassi.presentation.common.decoration.GridSpacingItemDecoration
import com.lassi.presentation.media.adapter.MediaAdapter
import com.lassi.presentation.mediadirectory.FolderViewModel
import com.lassi.presentation.videopreview.VideoPreviewActivity
import kotlinx.android.synthetic.main.fragment_media_picker.*
import java.io.File

class MediaFragment : LassiBaseViewModelFragment<SelectedMediaViewModel>() {
    private val mediaAdapter by lazy { MediaAdapter(this::onItemClick) }
    private var folder: FolderDetail? = null
    private var mediaPickerConfig = LassiConfig.getConfig()
    val mediaViewModel: MediaViewModel by lazy {
        ViewModelProviders.of(this,MediaViewModelFactory(requireContext()))[MediaViewModel::class.java]
    }


    override fun getContentResource() = R.layout.fragment_media_picker

    companion object {
        fun getInstance(folder: FolderDetail): MediaFragment {
            val miMediaPickerFragment = MediaFragment()
            val args = Bundle().apply {
                putParcelable(SELECTED_FOLDER, folder)
            }
            miMediaPickerFragment.arguments = args
            return miMediaPickerFragment
        }
    }

    override fun initViews() {
        super.initViews()
//        mediaViewModel = ViewModelProviders.of(requireActivity()).get(MediaViewModel::class.java)

        setImageAdapter()
        progressBar.indeterminateDrawable.setColorFilter(
            mediaPickerConfig.progressBarColor,
            PorterDuff.Mode.MULTIPLY
        )
    }
    override fun initLiveDataObservers() {
        super.initLiveDataObservers()
        mediaViewModel.fetchMediaFromFolderLiveData.observe(
            viewLifecycleOwner,
            SafeObserver(this::handleFetchedMedia)
        )
    }
    private fun handleFetchedMedia(response: Response<ArrayList<MiMedia>>) {
        when (response) {
            is Response.Success -> {
                progressBar.hide()
                mediaAdapter.setList(response.item)
            }
            is Response.Loading -> progressBar.show()
            is Response.Error -> progressBar.hide()
        }
    }

    override fun getBundle() {
        super.getBundle()
        arguments?.let {
            folder = it.getParcelable(SELECTED_FOLDER)
        }
    }

    override fun buildViewModel(): SelectedMediaViewModel {

        return ViewModelProviders.of(requireActivity())[SelectedMediaViewModel::class.java]
    }

    private fun setImageAdapter() {
        rvMedia.layoutManager = GridLayoutManager(context, mediaPickerConfig.gridSize)
        rvMedia.adapter = mediaAdapter
        rvMedia.addItemDecoration(GridSpacingItemDecoration(mediaPickerConfig.gridSize, 10))
        folder?.folderName?.let { mediaViewModel.fetchMediaFromFolders(it) }
    }

    private fun onItemClick(selectedMedias: ArrayList<MiMedia>) {
        if (LassiConfig.getConfig().maxCount > 1) {
            viewModel.addAllSelectedMedia(selectedMedias)
        } else {
            if (LassiConfig.getConfig().mediaType == MediaType.IMAGE) {
                val uri = Uri.fromFile(File(selectedMedias[0].path))
                CropUtils.beginCrop(requireActivity(), uri)
            } else {
                VideoPreviewActivity.startVideoPreview(
                    activity,
                    selectedMedias[0].path!!
                )
            }
        }
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        val item = menu.findItem(R.id.menuCamera)
        if (item != null)
            item.isVisible = false
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }
}
