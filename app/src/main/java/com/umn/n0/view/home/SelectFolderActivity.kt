package com.umn.n0.view.home

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.os.Environment
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.umn.n0.databinding.ActivitySelectFolderBinding
import com.umn.n0.view.constant.AppBuild
import java.io.File

class SelectFolderActivity : AppCompatActivity() {

    companion object {

        const val DATA_EXTRA = "SelectFolderActivity_data_extra"
    }

    private val activityBinding by lazy { ActivitySelectFolderBinding.inflate(layoutInflater) }
    private val externalStorage: File = Environment.getExternalStorageDirectory()
    private val adapter = SelectFolderRecyclerViewAdapter(
        onItemClick = ::onItemClick
    )

    private fun onItemClick(file: File) {
        if (file.path == externalStorage.parent) {
            return
        } else {
            val path = "${activityBinding.textViewPath.text}"
            if (file.path == File(path).parent) {
                adapter.setItem(file)
                activityBinding.textViewPath.text = file.path
                return
            }
            AlertDialog.Builder(this)
                .setTitle("N0Render")
                .setMessage("\n${file.path}")
                .setPositiveButton("Open") { d, _ ->
                    adapter.setItem(file)
                    activityBinding.textViewPath.text = file.path
                    d.dismiss()
                }
                .setNegativeButton("Select") { d, _ ->
                    val i = Intent()
                    i.putExtra(DATA_EXTRA, file.path)
                    setResult(Activity.RESULT_OK, i)
                    d.dismiss()
                    finishAfterTransition()
                }
                .show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(activityBinding.root)

        val textViewPath = activityBinding.textViewPath
        textViewPath.text = externalStorage.path

        val rv = activityBinding.recyclerView
        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = adapter
        adapter.setItem(externalStorage)
    }

    override fun onBackPressed() {
        if (activityBinding.textViewPath.text == externalStorage.path) {
            super.onBackPressed()
        } else {
            val parentFile = File("${activityBinding.textViewPath.text}").parentFile
            if (parentFile != null) {
                adapter.setItem(parentFile)
                activityBinding.textViewPath.text = parentFile.path
            }
        }
    }
}