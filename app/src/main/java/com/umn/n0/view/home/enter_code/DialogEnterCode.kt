package com.umn.n0.view.home.enter_code

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.Window
import android.widget.Button
import com.google.android.material.textfield.TextInputEditText
import com.umn.n0.R

class DialogEnterCode(
    context: Context,
    val onClickButtonDownload: (link: String) -> Unit = {},
) : Dialog(context) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestWindowFeature(Window.FEATURE_NO_TITLE)

        setContentView(R.layout.dialog_enter_code)

        findViewById<Button>(R.id.btn_download).setOnClickListener {
            val inputLink = findViewById<TextInputEditText>(R.id.et_input_code)
            val link = "${inputLink.text}"
            if (link.isNotEmpty()) {
                onClickButtonDownload.invoke(link)
                cancel()
            }
        }
    }
}