package com.umn.n0.view.home

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.umn.n0.R
import com.umn.n0.databinding.ItemBinding
import java.io.File

class SelectFolderRecyclerViewAdapter(
    val onItemClick: (File) -> Unit = { },
) : RecyclerView.Adapter<SelectFolderRecyclerViewViewHolder>() {

    private val _items = mutableListOf<File>()

    override fun onCreateViewHolder(
        parent: ViewGroup, viewType: Int
    ): SelectFolderRecyclerViewViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item, parent, false)
        return SelectFolderRecyclerViewViewHolder(view)
    }

    override fun getItemCount() = _items.size

    override fun onBindViewHolder(holder: SelectFolderRecyclerViewViewHolder, position: Int) {
        val binding = ItemBinding.bind(holder.itemView)
        val item = _items[position]
        if (position == 0) {
            binding.folderName.text = "..."
        } else {
            binding.folderName.text = item.name
        }
        binding.root.setOnClickListener {
            onItemClick.invoke(item)
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun setItem(file: File) {
        _items.clear()
        val items: MutableList<File> = file.listFiles()
            ?.toMutableList()
            ?.sorted()
            ?.filter { it.isDirectory }
            ?.toMutableList()
            ?: mutableListOf()
        val parent = file.parentFile
        if (parent != null) {
            items.add(0, parent)
        }
        _items.addAll(items)
        notifyDataSetChanged()
    }
}