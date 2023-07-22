package com.example.gitnotes

import android.content.Context
import android.graphics.Typeface
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView

class CustomSpinnerAdapter(context: Context, layoutId: Int, private val data: List<String>) :
    ArrayAdapter<String>(context, layoutId, data) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = super.getView(position, convertView, parent) as TextView
        styleTextView(view, position)
        return view
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = super.getDropDownView(position, convertView, parent) as TextView
        styleTextView(view, position)
        return view
    }

    private fun styleTextView(textView: TextView, position: Int) {
        textView.textSize = 20f
        if (position == 0) {
            textView.setTypeface(null, Typeface.ITALIC)
        } else {
            textView.setTypeface(null, Typeface.NORMAL)
        }
    }
}
