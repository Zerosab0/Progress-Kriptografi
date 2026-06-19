package com.example.kriptotugas1

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView

class VaultAdapter(private var entries: List<VaultEntry>) : RecyclerView.Adapter<VaultAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvService: TextView = view.findViewById(R.id.tvVaultService)
        val tvLevel: TextView = view.findViewById(R.id.tvVaultLevel)
        val tvUsername: TextView = view.findViewById(R.id.tvVaultUsername)
        val tvFingerprint: TextView = view.findViewById(R.id.tvVaultFingerprint)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_vault, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val entry = entries[position]
        holder.tvService.text = entry.serviceName
        holder.tvUsername.text = entry.username
        holder.tvLevel.text = entry.strengthLevel
        holder.tvFingerprint.text = entry.fingerprintSha256
        
        val context = holder.itemView.context
        val colorRes = when (entry.strengthLevel) {
            "Sangat kuat", "Kuat" -> R.color.success
            "Sedang" -> R.color.warning
            else -> R.color.danger
        }
        holder.tvLevel.setTextColor(ContextCompat.getColor(context, colorRes))
    }

    override fun getItemCount() = entries.size

    fun updateData(newEntries: List<VaultEntry>) {
        entries = newEntries
        notifyDataSetChanged()
    }
}
