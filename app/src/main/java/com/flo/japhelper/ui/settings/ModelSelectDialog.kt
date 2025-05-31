package com.flo.japhelper.ui.settings

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.flo.japhelper.model.ModelInfo
import com.flo.japhelper.R

class ModelSelectDialog(
    private val models: List<ModelInfo>,
    private val onModelSelected: (ModelInfo) -> Unit
) : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = getLayoutInflater().inflate(R.layout.dialog_model_select, null)
        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerView)

        recyclerView.layoutManager = GridLayoutManager(context, 2)

        recyclerView.adapter = ModelAdapter(models) { model ->
            onModelSelected(model)
            dismiss() // 👈 This line closes the dialog
        }

        return AlertDialog.Builder(requireContext())
            .setTitle("Select a Model")
            .setView(view)
            .setNegativeButton("Cancel", null)
            .create()
    }
}

class ModelAdapter(
    private val models: List<ModelInfo>,
    private val onModelSelected: (ModelInfo) -> Unit
) : RecyclerView.Adapter<ModelAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(R.id.modelName)
        val price: TextView = view.findViewById(R.id.modelPrice)

        init {
            view.setOnClickListener {
                onModelSelected(models[adapterPosition])
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.model_list_item, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount() = models.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val model = models[position]
        holder.name.text = model.name
        holder.price.text = "Prompt: ${model.pricing.prompt} | Completion: ${model.pricing.completion}"
    }
}