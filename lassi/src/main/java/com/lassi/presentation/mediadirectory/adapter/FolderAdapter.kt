package com.lassi.presentation.mediadirectory.adapter

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.lassi.R
import com.lassi.common.extenstions.hide
import com.lassi.common.extenstions.inflate
import com.lassi.common.extenstions.loadImage
import com.lassi.common.extenstions.show
import com.lassi.common.utils.ImageUtils
import com.lassi.data.mediadirectory.Folder
import com.lassi.data.mediadirectory.FolderDetail
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.item_media.*

class FolderAdapter(
    private val onItemClick: (folder: FolderDetail) -> Unit
) : RecyclerView.Adapter<FolderAdapter.FolderViewHolder>() {

    private var folders = ArrayList<FolderDetail>()

    fun setList(folders: ArrayList<FolderDetail>?) {
        folders?.let {
            this.folders.clear()
            this.folders.addAll(it)
        }
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FolderViewHolder {
        return FolderViewHolder(parent.inflate(R.layout.item_media))
    }

    override fun getItemCount() = folders.size

    override fun onBindViewHolder(holder: FolderViewHolder, position: Int) {
        holder.bind(folders[position])
    }

    inner class FolderViewHolder(override val containerView: View) :
        RecyclerView.ViewHolder(containerView), LayoutContainer {
        fun bind(folder: FolderDetail) {
            with(folder) {
                tvFolderName.show()
                tvDuration.hide()
                ivFolderThumbnail.loadImage(this.placeHolder)
//                tvFolderName.text = String.format(
//                    tvFolderName.context.getString(R.string.directory_with_item_count),
//                    folderName,
//                    medias.size.toString()
//                )
                tvFolderName.text = this.folderName
                itemView.setOnClickListener {
                    onItemClick(folder)
                }
            }
        }
    }
}